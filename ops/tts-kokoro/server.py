"""Local text-to-speech service for PrepAI, wrapping Kokoro (ONNX, CPU).

Expects text that is already in spoken form (units and symbols written as words).
Binds to localhost only; put nginx in front of it for anything public.
"""
import hashlib
import io
import os
import threading
from pathlib import Path

import soundfile as sf
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from kokoro_onnx import Kokoro
from pydantic import BaseModel, Field

MODEL_DIR = Path(os.environ.get("KOKORO_MODEL_DIR", Path.home() / "kokoro" / "models"))
CACHE_DIR = Path(os.environ.get("TTS_CACHE_DIR", Path.home() / "kokoro" / "cache"))
DEFAULT_VOICE = os.environ.get("TTS_DEFAULT_VOICE", "af_heart")
ALLOWED_ORIGIN = os.environ.get("TTS_ALLOWED_ORIGIN", "")
MAX_CHARS = 2000

CACHE_DIR.mkdir(parents=True, exist_ok=True)
kokoro = Kokoro(str(MODEL_DIR / "kokoro-v1.0.onnx"), str(MODEL_DIR / "voices-v1.0.bin"))
lock = threading.Lock()

app = FastAPI(title="PrepAI Kokoro TTS")
if ALLOWED_ORIGIN:
    app.add_middleware(CORSMiddleware, allow_origins=[ALLOWED_ORIGIN], allow_methods=["POST", "GET"], allow_headers=["*"])


class SpeechRequest(BaseModel):
    text: str = Field(min_length=1, max_length=MAX_CHARS)
    voice: str = DEFAULT_VOICE
    speed: float = Field(default=1.0, ge=0.5, le=2.0)
    lang: str = "en-us"


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "voices": len(kokoro.get_voices()), "cached": sum(1 for _ in CACHE_DIR.glob("*.wav"))}


@app.get("/voices")
def voices() -> list[str]:
    return sorted(kokoro.get_voices())


@app.post("/tts")
def synthesize(req: SpeechRequest) -> Response:
    if req.voice not in kokoro.get_voices():
        raise HTTPException(status_code=400, detail=f"unknown voice {req.voice}")
    key = hashlib.sha256(f"{req.voice}|{req.speed}|{req.lang}|{req.text}".encode()).hexdigest()
    cached = CACHE_DIR / f"{key}.wav"
    if cached.exists():
        return Response(cached.read_bytes(), media_type="audio/wav", headers={"X-Cache": "hit"})
    with lock:
        audio, rate = kokoro.create(req.text, voice=req.voice, speed=req.speed, lang=req.lang)
    buffer = io.BytesIO()
    sf.write(buffer, audio, rate, format="WAV")
    cached.write_bytes(buffer.getvalue())
    return Response(buffer.getvalue(), media_type="audio/wav", headers={"X-Cache": "miss"})
