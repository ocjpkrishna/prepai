#!/usr/bin/env python3
"""Serves the built PrepAI UI and proxies /tts-api/* to the local Kokoro service.

Same origin for the browser (no CORS, no public TTS port); the TTS service stays on localhost.
Usage: python3 serve-ui.py --dir <repo>/frontend/dist/frontend/browser --port 4300
"""
import argparse
import functools
import http.server
import urllib.error
import urllib.request

PREFIX = "/tts-api/"
ALLOWED = {("GET", "/health"), ("GET", "/voices"), ("POST", "/tts")}
MAX_BODY = 10_000
TIMEOUT_SECONDS = 120


class Handler(http.server.SimpleHTTPRequestHandler):
    upstream = "http://127.0.0.1:5051"

    def do_GET(self) -> None:
        self.proxy("GET") if self.path.startswith(PREFIX) else super().do_GET()

    def do_POST(self) -> None:
        self.proxy("POST") if self.path.startswith(PREFIX) else self.send_error(404)

    def proxy(self, method: str) -> None:
        path = self.path[len(PREFIX) - 1:].split("?")[0]
        length = int(self.headers.get("Content-Length") or 0)
        if (method, path) not in ALLOWED or length > MAX_BODY:
            self.send_error(404 if (method, path) not in ALLOWED else 413)
            return
        body = self.rfile.read(length) if length else None
        request = urllib.request.Request(self.upstream + path, data=body, method=method)
        request.add_header("Content-Type", self.headers.get("Content-Type", "application/json"))
        try:
            with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
                self.relay(response.status, response.headers, response.read())
        except urllib.error.HTTPError as error:
            self.relay(error.code, error.headers, error.read())
        except (urllib.error.URLError, TimeoutError):
            self.send_error(502, "TTS service is not reachable")

    def relay(self, status: int, headers, data: bytes) -> None:
        self.send_response(status)
        for name in ("Content-Type", "X-Cache"):
            if headers.get(name):
                self.send_header(name, headers[name])
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", required=True)
    parser.add_argument("--port", type=int, default=4300)
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--tts", default="http://127.0.0.1:5051")
    args = parser.parse_args()
    Handler.upstream = args.tts
    handler = functools.partial(Handler, directory=args.dir)
    with http.server.ThreadingHTTPServer((args.host, args.port), handler) as server:
        print(f"UI on http://{args.host}:{args.port}/  (TTS proxy to {args.tts})")
        server.serve_forever()


if __name__ == "__main__":
    main()
