import json
import os
import shutil
import tempfile
from pathlib import Path

import imageio_ffmpeg
import requests
import whisper
from fastapi import FastAPI, File, Form, HTTPException, UploadFile

app = FastAPI(title="Lingua Tycoon Speech Service", version="1.0.0")
_model = None

# Whisper shells out to ffmpeg. The Python dependency makes the service
# self-contained on Windows and on machines without a system ffmpeg install.
_ffmpeg_source = Path(imageio_ffmpeg.get_ffmpeg_exe())
_ffmpeg_dir = Path(tempfile.gettempdir()) / "lingua-tycoon-ffmpeg"
_ffmpeg_dir.mkdir(exist_ok=True)
_ffmpeg = _ffmpeg_dir / ("ffmpeg.exe" if os.name == "nt" else "ffmpeg")
if not _ffmpeg.exists() or _ffmpeg.stat().st_size != _ffmpeg_source.stat().st_size:
    shutil.copyfile(_ffmpeg_source, _ffmpeg)
os.environ["PATH"] = f"{_ffmpeg_dir}{os.pathsep}{os.environ.get('PATH', '')}"


def model():
    global _model
    if _model is None:
        _model = whisper.load_model(os.getenv("WHISPER_MODEL", "base"))
    return _model


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/api/lesson")
async def lesson(
    audio: UploadFile = File(...),
    native_language: str = Form(...),
    target_language: str = Form(...),
    prompt: str = Form(...),
):
    suffix = Path(audio.filename or "speech.m4a").suffix or ".m4a"
    with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
        tmp.write(await audio.read())
        path = tmp.name
    try:
        transcript = model().transcribe(path, language=None, fp16=False)["text"].strip()
        evaluation_prompt = f"""You are a supportive {target_language} speaking tutor.
The learner's native language is {native_language}.
Task: {prompt}
Transcript: {transcript}
Return only JSON with integer score (0-100) and concise feedback. Judge relevance, clarity, grammar, and vocabulary. Do not penalize accent because only a transcript is available."""
        response = requests.post(
            f"{os.getenv('OLLAMA_URL', 'http://127.0.0.1:11434').rstrip('/')}/api/generate",
            json={"model": os.getenv("OLLAMA_MODEL", "qwen2.5vl:3b"), "prompt": evaluation_prompt, "stream": False, "format": "json"},
            timeout=120,
        )
        response.raise_for_status()
        evaluation = json.loads(response.json()["response"])
        return {"transcript": transcript, "score": int(evaluation.get("score", 0)), "feedback": str(evaluation.get("feedback", "Keep practicing!"))}
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    finally:
        Path(path).unlink(missing_ok=True)
