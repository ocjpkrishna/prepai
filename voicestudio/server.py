"""VoiceStudio: the local text-to-speech server PrepAI's speech module calls (spec 4.5).

    POST /synthesize  {"text": "...", "language": "en-IN", "voice": "en-IN-default"}  ->  audio/wav
    GET  /voices                                                                      ->  [{id, name, language}]

It runs Piper, a free offline neural voice, on the CPU. Female voices only. Models are read from
VOICESTUDIO_MODELS (default ~/voicestudio-models); see README.md. It listens on 127.0.0.1 only.
"""
import json
import os
import re
import threading
import wave
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from io import BytesIO
from pathlib import Path

from piper import PiperVoice, SynthesisConfig

MODELS_DIR = Path(os.environ.get("VOICESTUDIO_MODELS", Path.home() / "voicestudio-models"))
HOST = os.environ.get("VOICESTUDIO_HOST", "127.0.0.1")
PORT = int(os.environ.get("VOICESTUDIO_PORT", "5050"))
MAX_TEXT = 1000
SPEAKING_PACE = float(os.environ.get("VOICESTUDIO_PACE", "1.08"))  # above 1.0 is slower, clearer for teaching

# voice id -> (model file, language, display name)
VOICES = {
    "en-IN-default": ("en_GB-jenny_dioco-medium", "en-IN", "Jenny (warm, female)"),
    "en-IN-amy": ("en_US-amy-medium", "en-IN", "Amy (clear, female)"),
    "hi-IN-default": ("hi_IN-priyamvada-medium", "hi-IN", "Priyamvada (female)"),
}
DEFAULT_BY_LANGUAGE = {"en-IN": "en-IN-default", "hi-IN": "hi-IN-default"}

_loaded: dict[str, PiperVoice] = {}
_synth_lock = threading.Lock()


def speakable(text: str) -> str:
    """Small clean-ups so symbols and subscripts are read the way a teacher says them."""
    text = re.sub(r"\b([A-Za-z]) sub ([A-Za-z0-9])\b", r"\1 \2", text)  # "u sub y" -> "u y"
    text = text.replace("^2", " squared").replace("^3", " cubed")
    return re.sub(r"\s+", " ", text).strip()


def voice_for(voice_id: str | None, language: str) -> PiperVoice:
    key = voice_id if voice_id in VOICES else DEFAULT_BY_LANGUAGE.get(language, "en-IN-default")
    if key not in _loaded:
        _loaded[key] = PiperVoice.load(MODELS_DIR / f"{VOICES[key][0]}.onnx")
    return _loaded[key]


def synthesize(text: str, language: str, voice_id: str | None) -> bytes:
    voice = voice_for(voice_id, language)
    out = BytesIO()
    with _synth_lock, wave.open(out, "wb") as wav:
        voice.synthesize_wav(speakable(text), wav, syn_config=SynthesisConfig(length_scale=SPEAKING_PACE))
    return out.getvalue()


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != "/voices":
            return self._send(404, b"not found", "text/plain")
        voices = [{"id": key, "name": name, "language": language} for key, (_, language, name) in VOICES.items()]
        self._send(200, json.dumps(voices).encode(), "application/json")

    def do_POST(self):
        if self.path != "/synthesize":
            return self._send(404, b"not found", "text/plain")
        try:
            body = json.loads(self._read_body())
            text = str(body["text"])[:MAX_TEXT]
            wav = synthesize(text, body.get("language", "en-IN"), body.get("voice"))
        except (KeyError, ValueError):
            return self._send(400, b"bad request", "text/plain")
        except Exception as failure:  # a broken model must answer 500, not kill the server
            print("synthesis failed:", failure, flush=True)
            return self._send(500, b"synthesis failed", "text/plain")
        self._send(200, wav, "audio/wav")

    def _read_body(self) -> bytes:
        """The request body, whether it comes with a Content-Length or in chunks (Java clients may chunk)."""
        if "chunked" in self.headers.get("Transfer-Encoding", "").lower():
            body = b""
            while True:
                size = int(self.rfile.readline().split(b";")[0].strip() or b"0", 16)
                if size == 0:
                    self.rfile.readline()
                    return body
                body += self.rfile.read(size)
                self.rfile.readline()
        return self.rfile.read(int(self.headers.get("Content-Length", 0)))

    def _send(self, status: int, body: bytes, content_type: str):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        print(format % args, flush=True)


if __name__ == "__main__":
    print(f"VoiceStudio on http://{HOST}:{PORT}, models in {MODELS_DIR}", flush=True)
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
