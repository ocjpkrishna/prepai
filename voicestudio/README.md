# VoiceStudio

The local text-to-speech server the backend's `speech` module calls (`prepai.voicestudio.api-url`, port 5050).
It runs [Piper](https://github.com/OHF-Voice/piper1-gpl), a free offline neural voice, on the CPU. Female voices only.

## Install (no root needed)
```
python3 -m venv ~/voicestudio-venv
~/voicestudio-venv/bin/pip install -r voicestudio/requirements.txt
mkdir -p ~/voicestudio-models && cd ~/voicestudio-models
B=https://huggingface.co/rhasspy/piper-voices/resolve/main
for f in en/en_GB/jenny_dioco/medium/en_GB-jenny_dioco-medium en/en_US/amy/medium/en_US-amy-medium \
         hi/hi_IN/priyamvada/medium/hi_IN-priyamvada-medium; do
  for ext in onnx onnx.json; do curl -sLO "$B/$f.$ext"; done
done
```

## Run
```
~/voicestudio-venv/bin/python voicestudio/server.py          # listens on 127.0.0.1:5050
```
Settings (environment): `VOICESTUDIO_MODELS`, `VOICESTUDIO_HOST`, `VOICESTUDIO_PORT`, `VOICESTUDIO_PACE` (1.08; higher is slower).

## Voices
| id | voice |
|----|-------|
| `en-IN-default` | Jenny, warm British female |
| `en-IN-amy` | Amy, clear US female |
| `hi-IN-default` | Priyamvada, Hindi female |

## Gotchas
- Memory: each loaded voice is about 100 MB; a voice loads on first use.
- It speaks what it is given. Narration should be written to be heard (see `prompts/system-prompt.txt`).
- To swap in a paid voice (Azure, ElevenLabs), keep the same two endpoints; the backend does not change.
