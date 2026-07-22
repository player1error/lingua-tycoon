package com.linguatycoon.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var state: AppState
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val purple = Color.rgb(108, 77, 255)
    private val ink = Color.rgb(31, 27, 51)
    private val prompt = "Introduce yourself and describe what you do for work."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = AppState(this)
        if (state.loggedIn) showHome() else showAuth()
    }

    private fun page(title: String): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 32)
            setBackgroundColor(Color.rgb(247, 245, 255))
        }
        root.addView(TextView(this).apply {
            text = title; textSize = 30f; setTextColor(ink); setPadding(0, 0, 0, 24)
        })
        setContentView(ScrollView(this).apply { addView(root) })
        return root
    }

    private fun input(hint: String, password: Boolean = false) = EditText(this).apply {
        this.hint = hint
        if (password) inputType = 0x81
        setPadding(20, 18, 20, 18)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.WHITE); setBackgroundColor(purple)
        setOnClickListener { action() }
    }

    private fun note(textValue: String) = TextView(this).apply {
        text = textValue; textSize = 16f; setTextColor(ink); setPadding(0, 14, 0, 14)
    }

    private fun showAuth() {
        val root = page("Lingua Tycoon")
        root.addView(note("Speak a language. Build an empire."))
        val name = input("Username")
        val pass = input("Password", true)
        val status = note("")
        root.addView(name); root.addView(pass)
        root.addView(button("Register") {
            if (name.text.length < 2 || pass.text.length < 4) status.text = "Use a username and a password of at least 4 characters."
            else { state.register(name.text.toString(), pass.text.toString()); showLanguages() }
        })
        root.addView(button("Log in") {
            if (state.login(name.text.toString(), pass.text.toString())) { state.loggedIn = true; showHome() }
            else status.text = "Those details do not match this device's account."
        })
        root.addView(status)
    }

    private fun showLanguages() {
        val root = page("Choose your languages")
        val languages = arrayOf("English", "Spanish", "French", "German", "Italian", "Dutch", "Portuguese", "Japanese")
        val native = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages) }
        val target = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages); setSelection(1) }
        root.addView(note("I speak")); root.addView(native)
        root.addView(note("I want to learn")); root.addView(target)
        root.addView(button("Save and continue") {
            if (native.selectedItem == target.selectedItem) Toast.makeText(this, "Choose two different languages", Toast.LENGTH_SHORT).show()
            else { state.nativeLanguage = native.selectedItem.toString(); state.targetLanguage = target.selectedItem.toString(); showHome() }
        })
    }

    private fun showHome() {
        val root = page("Welcome, ${state.username}")
        root.addView(note("${state.nativeLanguage} → ${state.targetLanguage}\n${state.xp} XP • ${state.lessons} lessons • Business level ${state.businessLevel}"))
        root.addView(button("Start speaking lesson") { showLesson() })
        root.addView(button("Business") { showBusiness() })
        root.addView(button("Pet hint") { showPetHint() })
        root.addView(button("Languages & connection") { showSettings() })
        root.addView(button("Check for APK update") { checkUpdate(root) })
        root.addView(button("Log out") { state.loggedIn = false; showAuth() })
    }

    private fun showLesson() {
        val root = page("Speaking lesson")
        root.addView(note("Speak in ${state.targetLanguage}:\n\n$prompt"))
        val status = note("Tap record when ready.")
        val record = button("Start recording") {}
        record.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7)
                status.text = "Microphone permission granted? Tap record again."
            } else if (recorder == null) {
                startRecording(); record.text = "Stop recording"; status.text = "Listening…"
            } else {
                stopRecording(); record.visibility = View.GONE; status.text = "Uploading speech to Whisper…"
                submitLesson(status, root)
            }
        }
        root.addView(record); root.addView(status)
        root.addView(button("Back") { stopRecording(); showHome() })
    }

    private fun startRecording() {
        audioFile = File(cacheDir, "lesson-${System.currentTimeMillis()}.m4a")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(audioFile!!.absolutePath)
            prepare(); start()
        }
    }

    private fun stopRecording() {
        recorder?.runCatching { stop() }
        recorder?.release(); recorder = null
    }

    private fun submitLesson(status: TextView, root: LinearLayout) {
        val file = audioFile ?: return
        thread {
            runCatching { ApiClient.evaluate(state.apiUrl, file, state.nativeLanguage, state.targetLanguage, prompt) }
                .onSuccess { result -> runOnUiThread {
                    val score = result.optInt("score", 0).coerceIn(0, 100)
                    val earned = (10 + score / 10)
                    state.xp += earned; state.lessons += 1
                    status.text = "Whisper transcript:\n${result.optString("transcript")}\n\nOllama feedback (${score}/100):\n${result.optString("feedback")}\n\n+$earned XP — progress saved"
                    root.addView(button("Continue") { showHome() })
                }}
                .onFailure { error -> runOnUiThread {
                    status.text = "Could not finish the lesson: ${error.message}\n\nCheck the service URL in settings and ensure Whisper and Ollama are running."
                    root.addView(button("Retry upload") { status.text = "Retrying…"; submitLesson(status, root) })
                }}
        }
    }

    private fun showBusiness() {
        val root = page("Your business")
        val names = arrayOf("Street cart", "Corner café", "Language studio", "Global academy")
        val current = names[state.businessLevel.coerceAtMost(names.lastIndex)]
        val price = 50 * (state.businessLevel + 1)
        root.addView(note("Current: $current\nBalance: ${state.xp} XP\nNext upgrade: $price XP"))
        root.addView(button(if (state.businessLevel == 0) "Buy business" else "Upgrade business") {
            if (state.businessLevel >= names.lastIndex) Toast.makeText(this, "Your business is fully upgraded!", Toast.LENGTH_SHORT).show()
            else if (state.xp < price) Toast.makeText(this, "Earn ${price - state.xp} more XP", Toast.LENGTH_SHORT).show()
            else { state.xp -= price; state.businessLevel += 1; showBusiness() }
        })
        root.addView(button("Back") { showHome() })
    }

    private fun showPetHint() {
        val hints = arrayOf(
            "🐶 Short daily practice beats one long weekly session.",
            "🐱 Repeat the corrected sentence aloud three times.",
            "🦊 Describe objects around you in ${state.targetLanguage}.",
            "🐼 Don't chase perfection—keep the conversation moving."
        )
        val root = page("Milo's hint")
        root.addView(TextView(this).apply { text = hints[(state.lessons + state.xp) % hints.size]; textSize = 22f; gravity = Gravity.CENTER; setPadding(10, 50, 10, 50) })
        root.addView(button("Thanks, Milo!") { showHome() })
    }

    private fun showSettings() {
        val root = page("Settings")
        val api = input("Service URL").apply { setText(state.apiUrl) }
        val repo = input("GitHub owner/repository").apply { setText(state.githubRepository) }
        root.addView(note("Whisper + Ollama service URL")); root.addView(api)
        root.addView(note("GitHub repository for APK updates")); root.addView(repo)
        root.addView(button("Save") { state.apiUrl = api.text.toString(); state.githubRepository = repo.text.toString(); showHome() })
        root.addView(button("Change languages") { showLanguages() })
    }

    private fun checkUpdate(root: LinearLayout) {
        if (state.githubRepository.isBlank()) { Toast.makeText(this, "Set a GitHub repository in Settings", Toast.LENGTH_LONG).show(); return }
        val status = note("Checking GitHub releases…"); root.addView(status)
        thread {
            runCatching { ApiClient.latestRelease(state.githubRepository) }
                .onSuccess { release -> runOnUiThread {
                    val tag = release.optString("tag_name").removePrefix("v")
                    if (tag == BuildConfig.VERSION_NAME) status.text = "You're up to date (${BuildConfig.VERSION_NAME})."
                    else {
                        status.text = "Version $tag is available."
                        val assets = release.optJSONArray("assets")
                        var apkUrl = release.optString("html_url")
                        if (assets != null) for (i in 0 until assets.length()) {
                            val a = assets.getJSONObject(i)
                            if (a.optString("name").endsWith(".apk")) { apkUrl = a.optString("browser_download_url"); break }
                        }
                        root.addView(button("Open download") { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))) })
                    }
                }}
                .onFailure { runOnUiThread { status.text = "Update check failed: ${it.message}" } }
        }
    }
}
