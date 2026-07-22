# Lingua Tycoon

An Android speaking-practice game. Record a lesson, transcribe it with local Whisper, receive an Ollama evaluation, earn XP, upgrade a business, and get hints from Milo.

## Run the speech service

Prerequisites: Python 3.11+ and [Ollama](https://ollama.com/) with `qwen2.5vl:3b` installed (or set `OLLAMA_MODEL` to another local model). The Python environment includes its own ffmpeg binary.

```powershell
cd server
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
ollama pull qwen2.5vl:3b
uvicorn main:app --host 0.0.0.0 --port 8000
```

The Android emulator reaches the service at `http://10.0.2.2:8000`. For a physical phone, enter the computer's LAN URL (for example `http://192.168.1.20:8000`) under **Languages & connection**.

## Build the APK

Set `JAVA_HOME` to Android Studio's bundled JBR, create `local.properties` containing your Android SDK path, then run `gradlew assembleDebug` (or the installed Gradle binary until the wrapper is generated). The APK is produced under `app/build/outputs/apk/debug/`.

## Release updates

Enter `owner/repository` in app settings. The app checks GitHub's latest release and opens its APK asset. Release tags should match the Android `versionName`, prefixed with `v` if desired.

## Privacy

Account data and progress stay on the device. Recorded audio is sent only to the configured speech-service URL and temporary server audio is deleted after evaluation.
