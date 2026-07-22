package com.linguatycoon.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var state: AppState
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    private val navy = Color.rgb(20, 27, 52)
    private val muted = Color.rgb(100, 108, 130)
    private val purple = Color.rgb(103, 80, 255)
    private val purpleDark = Color.rgb(74, 55, 214)
    private val purpleSoft = Color.rgb(237, 234, 255)
    private val mint = Color.rgb(28, 184, 160)
    private val gold = Color.rgb(247, 174, 53)
    private val background = Color.rgb(247, 248, 252)
    private val surface = Color.WHITE
    private val border = Color.rgb(229, 232, 241)
    private val danger = Color.rgb(221, 74, 92)
    private val prompt = "Introduce yourself and describe what you do for work."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = navy
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        state = AppState(this)
        if (state.loggedIn) showHome() else showAuth()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, radius: Int = 18, strokeColor: Int? = null, strokeWidth: Int = 1) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != null) setStroke(dp(strokeWidth), strokeColor)
        }

    private fun gradient(vararg colors: Int, radius: Int = 24) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        colors
    ).apply { cornerRadius = dp(radius).toFloat() }

    private fun text(
        value: String,
        size: Float = 15f,
        color: Int = navy,
        weight: Int = Typeface.NORMAL,
        gravityValue: Int = Gravity.START
    ) = TextView(this).apply {
        this.text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create("sans-serif", weight)
        gravity = gravityValue
        includeFontPadding = false
        setLineSpacing(0f, 1.14f)
    }

    private fun spacer(height: Int) = Space(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun row(gap: Int = 10) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        if (gap > 0) setPadding(0, 0, 0, 0)
    }

    private fun card(padding: Int = 20, radius: Int = 22): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        background = shape(surface, radius, border)
        elevation = dp(2).toFloat()
    }

    private fun addCard(root: LinearLayout, view: View, top: Int = 12) {
        root.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
        })
    }

    private fun brandHeader(showBack: Boolean = false, onBack: (() -> Unit)? = null): LinearLayout {
        val header = row()
        if (showBack) {
            header.addView(text("‹", 35f, navy, Typeface.NORMAL, Gravity.CENTER).apply {
                background = shape(surface, 16, border)
                setOnClickListener { onBack?.invoke() }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            header.addView(spacer(12), LinearLayout.LayoutParams(dp(12), 1))
        }
        header.addView(text("L", 20f, Color.WHITE, Typeface.BOLD, Gravity.CENTER).apply {
            background = gradient(purple, purpleDark, radius = 14)
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(text("LINGUA TYCOON", 13f, navy, Typeface.BOLD).apply {
            letterSpacing = .11f
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) })
        return header
    }

    private fun screen(
        eyebrow: String,
        title: String,
        subtitle: String,
        showBack: Boolean = false,
        onBack: (() -> Unit)? = null
    ): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(36))
        }
        content.addView(brandHeader(showBack, onBack))
        content.addView(spacer(if (showBack) 28 else 42))
        content.addView(text(eyebrow.uppercase(), 12f, purple, Typeface.BOLD).apply { letterSpacing = .13f })
        content.addView(spacer(8))
        content.addView(text(title, 32f, navy, Typeface.BOLD))
        if (subtitle.isNotBlank()) {
            content.addView(spacer(10))
            content.addView(text(subtitle, 16f, muted).apply { maxWidth = dp(520) })
        }
        content.addView(spacer(18))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(this@MainActivity.background)
            addView(content)
        }
        setContentView(scroll)
        content.alpha = 0f
        content.translationY = dp(8).toFloat()
        content.animate().alpha(1f).translationY(0f).setDuration(220).start()
        return content
    }

    private fun field(label: String, hint: String, password: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint
            textSize = 16f
            setTextColor(navy)
            setHintTextColor(Color.rgb(154, 160, 178))
            background = shape(Color.rgb(250, 250, 253), 16, border)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            minHeight = dp(54)
            inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else InputType.TYPE_CLASS_TEXT
            contentDescription = label
        }
    }

    private fun labeledField(container: LinearLayout, label: String, input: EditText) {
        container.addView(text(label, 13f, navy, Typeface.BOLD))
        container.addView(spacer(8))
        container.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
        container.addView(spacer(16))
    }

    private fun primaryButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 16f
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        gravity = Gravity.CENTER
        background = gradient(purple, purpleDark, radius = 17)
        elevation = dp(3).toFloat()
        minHeight = dp(56)
        setPadding(dp(18), dp(16), dp(18), dp(16))
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun secondaryButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 15f
        setTextColor(navy)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        gravity = Gravity.CENTER
        background = shape(surface, 17, border)
        minHeight = dp(54)
        setPadding(dp(16), dp(15), dp(16), dp(15))
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun pill(label: String, color: Int = purple, fill: Int = purpleSoft) = text(label, 12f, color, Typeface.BOLD, Gravity.CENTER).apply {
        background = shape(fill, 50)
        setPadding(dp(12), dp(7), dp(12), dp(7))
    }

    private fun sectionTitle(title: String, action: String? = null): LinearLayout {
        return row().apply {
            addView(text(title, 19f, navy, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (action != null) addView(text(action, 13f, purple, Typeface.BOLD))
        }
    }

    private fun showAuth() {
        val root = screen(
            "Language learning, reimagined",
            "Speak boldly.\nBuild something.",
            "Real conversation practice meets a world that grows with every lesson."
        )
        val trust = row().apply {
            background = shape(Color.rgb(237, 249, 246), 50)
            setPadding(dp(13), dp(9), dp(13), dp(9))
            addView(text("●", 10f, mint, Typeface.BOLD))
            addView(text("  Private by default  •  Powered by local AI", 12f, Color.rgb(42, 115, 103), Typeface.BOLD))
        }
        root.addView(trust, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val form = card(20)
        form.addView(text("Your account", 20f, navy, Typeface.BOLD))
        form.addView(spacer(5))
        form.addView(text("Continue your journey or start fresh.", 14f, muted))
        form.addView(spacer(20))
        val name = field("Username", "Username")
        val pass = field("Password", "Password", true)
        labeledField(form, "USERNAME", name)
        labeledField(form, "PASSWORD", pass)
        val status = text("", 13f, danger, Typeface.BOLD)
        form.addView(primaryButton("Create account") {
            if (name.text.length < 2 || pass.text.length < 4) {
                status.text = "Use a username and a password of at least 4 characters."
            } else {
                state.register(name.text.toString(), pass.text.toString())
                showLanguages()
            }
        })
        form.addView(spacer(10))
        form.addView(secondaryButton("I already have an account") {
            if (state.login(name.text.toString(), pass.text.toString())) {
                state.loggedIn = true
                showHome()
            } else status.text = "Those details do not match this device's account."
        })
        form.addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        addCard(root, form, 24)
    }

    private fun languageSpinner(languages: Array<String>, selected: String): Spinner {
        return Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages)
            setSelection(languages.indexOf(selected).coerceAtLeast(0))
            background = shape(Color.rgb(250, 250, 253), 16, border)
            setPadding(dp(14), 0, dp(14), 0)
        }
    }

    private fun showLanguages() {
        val root = screen(
            "Personalize your journey",
            "Choose your languages",
            "We will tailor lesson prompts and feedback to your language pair.",
            state.lessons > 0,
            { showSettings() }
        )
        root.addView(pill("STEP 1 OF 1"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val languages = arrayOf("English", "Spanish", "French", "German", "Italian", "Dutch", "Portuguese", "Japanese")
        val native = languageSpinner(languages, state.nativeLanguage)
        val target = languageSpinner(languages, state.targetLanguage)
        val selection = card()
        selection.addView(text("I already speak", 13f, muted, Typeface.BOLD))
        selection.addView(spacer(8))
        selection.addView(native, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)))
        selection.addView(spacer(20))
        selection.addView(text("I want to master", 13f, muted, Typeface.BOLD))
        selection.addView(spacer(8))
        selection.addView(target, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)))
        selection.addView(spacer(22))
        selection.addView(primaryButton("Start my journey  →") {
            if (native.selectedItem == target.selectedItem) {
                Toast.makeText(this, "Choose two different languages", Toast.LENGTH_SHORT).show()
            } else {
                state.nativeLanguage = native.selectedItem.toString()
                state.targetLanguage = target.selectedItem.toString()
                showHome()
            }
        })
        addCard(root, selection, 20)
    }

    private fun stat(value: String, label: String, accent: Int): LinearLayout {
        return card(14, 18).apply {
            gravity = Gravity.CENTER
            addView(text(value, 21f, accent, Typeface.BOLD, Gravity.CENTER))
            addView(spacer(4))
            addView(text(label.uppercase(), 10f, muted, Typeface.BOLD, Gravity.CENTER).apply { letterSpacing = .1f })
        }
    }

    private fun actionCard(icon: String, title: String, subtitle: String, tint: Int, action: () -> Unit): LinearLayout {
        return card(17, 20).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            val top = row()
            top.addView(text(icon, 24f, tint, Typeface.BOLD, Gravity.CENTER).apply {
                background = shape(if (tint == gold) Color.rgb(255, 247, 230) else purpleSoft, 15)
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            val labels = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(title, 16f, navy, Typeface.BOLD))
                addView(spacer(4))
                addView(text(subtitle, 12f, muted))
            }
            top.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) })
            top.addView(text("→", 21f, muted, Typeface.NORMAL, Gravity.CENTER))
            addView(top)
        }
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(36))
        }
        val top = brandHeader()
        top.addView(text(state.username.take(1).uppercase(), 15f, purple, Typeface.BOLD, Gravity.CENTER).apply {
            background = shape(purpleSoft, 50)
        }, LinearLayout.LayoutParams(dp(40), dp(40)))
        root.addView(top)
        root.addView(spacer(34))
        root.addView(text("GOOD TO SEE YOU", 11f, purple, Typeface.BOLD).apply { letterSpacing = .13f })
        root.addView(spacer(7))
        root.addView(text("Ready to speak, ${state.username}?", 29f, navy, Typeface.BOLD))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(22))
            background = gradient(Color.rgb(105, 82, 255), Color.rgb(72, 52, 205), radius = 25)
            elevation = dp(5).toFloat()
            addView(text("TODAY'S SPEAKING MISSION", 11f, Color.rgb(218, 211, 255), Typeface.BOLD).apply { letterSpacing = .12f })
            addView(spacer(10))
            addView(text("Introduce yourself", 24f, Color.WHITE, Typeface.BOLD))
            addView(spacer(7))
            addView(text("Practice a natural ${state.targetLanguage} introduction and receive instant feedback.", 14f, Color.rgb(235, 232, 255)))
            addView(spacer(20))
            addView(text("Start lesson   →", 15f, purpleDark, Typeface.BOLD, Gravity.CENTER).apply {
                background = shape(Color.WHITE, 16)
                setPadding(dp(18), dp(14), dp(18), dp(14))
                setOnClickListener { showLesson() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))
        }
        addCard(root, hero, 22)

        val stats = row()
        stats.addView(stat(state.xp.toString(), "XP", purple), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        stats.addView(spacer(9), LinearLayout.LayoutParams(dp(9), 1))
        stats.addView(stat(state.lessons.toString(), "Lessons", mint), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        stats.addView(spacer(9), LinearLayout.LayoutParams(dp(9), 1))
        stats.addView(stat("Lv ${state.businessLevel}", "Business", gold), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addCard(root, stats, 14)

        root.addView(spacer(28))
        root.addView(sectionTitle("Build your world"))
        val activities = row()
        activities.addView(actionCard("↗", "Business", "Grow your empire", gold) { showBusiness() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        activities.addView(spacer(10), LinearLayout.LayoutParams(dp(10), 1))
        activities.addView(actionCard("✦", "Ask Milo", "Get a smart hint", purple) { showPetHint() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addCard(root, activities, 12)

        root.addView(spacer(28))
        root.addView(sectionTitle("Tools"))
        root.addView(spacer(12))
        root.addView(secondaryButton("Languages & connection") { showSettings() })
        root.addView(spacer(10))
        root.addView(secondaryButton("Check for app update") { checkUpdate(root) })
        root.addView(spacer(16))
        root.addView(text("Log out", 14f, muted, Typeface.BOLD, Gravity.CENTER).apply {
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setOnClickListener { state.loggedIn = false; showAuth() }
        })

        val scroll = ScrollView(this).apply { setBackgroundColor(this@MainActivity.background); addView(root) }
        setContentView(scroll)
        root.alpha = 0f
        root.animate().alpha(1f).setDuration(220).start()
    }

    private fun showLesson() {
        val root = screen("Speaking mission", "Introduce yourself", "A short, focused conversation exercise.", true) {
            stopRecording(); showHome()
        }
        val tags = row()
        tags.addView(pill("●  SPEAKING", mint, Color.rgb(231, 249, 245)))
        tags.addView(spacer(8), LinearLayout.LayoutParams(dp(8), 1))
        tags.addView(pill("ABOUT 2 MIN", muted, Color.rgb(238, 240, 245)))
        root.addView(tags)

        val promptCard = card(22)
        promptCard.addView(text("YOUR PROMPT", 11f, purple, Typeface.BOLD).apply { letterSpacing = .12f })
        promptCard.addView(spacer(13))
        promptCard.addView(text("“$prompt”", 22f, navy, Typeface.BOLD))
        promptCard.addView(spacer(14))
        promptCard.addView(text("Speak naturally in ${state.targetLanguage}. Pauses are completely okay.", 14f, muted))
        addCard(root, promptCard, 18)

        val recorderCard = card(22)
        recorderCard.gravity = Gravity.CENTER_HORIZONTAL
        val record = text("●", 40f, Color.WHITE, Typeface.BOLD, Gravity.CENTER).apply {
            background = gradient(purple, purpleDark, radius = 50)
            elevation = dp(6).toFloat()
        }
        recorderCard.addView(record, LinearLayout.LayoutParams(dp(92), dp(92)))
        recorderCard.addView(spacer(14))
        val recordLabel = text("Tap to start recording", 17f, navy, Typeface.BOLD, Gravity.CENTER)
        recorderCard.addView(recordLabel)
        recorderCard.addView(spacer(6))
        val status = text("Your recording stays private and goes only to your configured AI service.", 13f, muted, Typeface.NORMAL, Gravity.CENTER)
        recorderCard.addView(status)
        record.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7)
                status.text = "Microphone permission granted? Tap once more to begin."
            } else if (recorder == null) {
                startRecording()
                record.background = gradient(Color.rgb(238, 88, 105), Color.rgb(198, 54, 76), radius = 50)
                record.text = "■"
                recordLabel.text = "Listening… tap to finish"
                status.text = "Take your time. Speak clearly and naturally."
            } else {
                stopRecording()
                record.isEnabled = false
                record.alpha = .55f
                record.text = "↑"
                recordLabel.text = "Uploading your speech"
                status.text = "Whisper is transcribing, then Ollama will prepare feedback…"
                submitLesson(root)
            }
        }
        addCard(root, recorderCard, 14)
        root.addView(spacer(14))
        root.addView(secondaryButton("Leave lesson") { stopRecording(); showHome() })
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
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
    }

    private fun submitLesson(root: LinearLayout) {
        val file = audioFile ?: return
        thread {
            runCatching { ApiClient.evaluate(state.apiUrl, file, state.nativeLanguage, state.targetLanguage, prompt) }
                .onSuccess { result -> runOnUiThread {
                    val score = result.optInt("score", 0).coerceIn(0, 100)
                    val earned = 10 + score / 10
                    state.xp += earned
                    state.lessons += 1
                    showLessonResult(result, score, earned)
                }}
                .onFailure { error -> runOnUiThread {
                    val errorCard = card()
                    errorCard.addView(text("We couldn't finish that lesson", 18f, danger, Typeface.BOLD))
                    errorCard.addView(spacer(8))
                    errorCard.addView(text(error.message ?: "Unknown service error", 13f, muted))
                    errorCard.addView(spacer(14))
                    errorCard.addView(primaryButton("Record again") { showLesson() })
                    addCard(root, errorCard, 14)
                }}
        }
    }

    private fun showLessonResult(result: JSONObject, score: Int, earned: Int) {
        val root = screen("Lesson complete", "That was a strong step.", "Your progress has been saved automatically.", true) { showHome() }
        val scoreCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(25), dp(24), dp(25))
            background = gradient(Color.rgb(27, 190, 165), Color.rgb(14, 142, 128), radius = 24)
            addView(text("$score", 54f, Color.WHITE, Typeface.BOLD, Gravity.CENTER))
            addView(text("SPEAKING SCORE", 11f, Color.rgb(214, 255, 247), Typeface.BOLD, Gravity.CENTER).apply { letterSpacing = .13f })
            addView(spacer(12))
            addView(pill("+$earned XP", Color.WHITE, Color.argb(45, 255, 255, 255)))
        }
        addCard(root, scoreCard, 4)

        val feedback = card()
        feedback.addView(text("Coach feedback", 19f, navy, Typeface.BOLD))
        feedback.addView(spacer(10))
        feedback.addView(text(result.optString("feedback", "Keep practicing!"), 15f, muted))
        addCard(root, feedback, 14)

        val transcript = card()
        transcript.addView(text("Whisper transcript", 13f, purple, Typeface.BOLD))
        transcript.addView(spacer(9))
        transcript.addView(text(result.optString("transcript", "No speech detected."), 15f, navy))
        addCard(root, transcript, 12)
        root.addView(spacer(16))
        root.addView(primaryButton("Continue to dashboard  →") { showHome() })
    }

    private fun showBusiness() {
        val names = arrayOf("No business yet", "Street cart", "Corner café", "Language studio", "Global academy")
        val current = names[state.businessLevel.coerceAtMost(names.lastIndex)]
        val price = 50 * (state.businessLevel + 1)
        val root = screen("Your language empire", current, "Turn speaking practice into steady progress.", true) { showHome() }

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(24), dp(22), dp(24))
            background = gradient(Color.rgb(255, 190, 80), Color.rgb(239, 141, 45), radius = 24)
            addView(text("◆", 42f, Color.WHITE, Typeface.BOLD, Gravity.CENTER))
            addView(spacer(8))
            addView(text(current, 23f, Color.WHITE, Typeface.BOLD, Gravity.CENTER))
            addView(spacer(6))
            addView(text("BUSINESS LEVEL ${state.businessLevel}", 11f, Color.rgb(255, 244, 220), Typeface.BOLD, Gravity.CENTER).apply { letterSpacing = .12f })
        }
        addCard(root, hero, 2)

        val upgrade = card()
        val head = row()
        head.addView(text("Next milestone", 18f, navy, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(pill("${state.xp} XP", gold, Color.rgb(255, 247, 230)))
        upgrade.addView(head)
        upgrade.addView(spacer(14))
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = price
            this.progress = state.xp.coerceAtMost(price)
            progressTintList = android.content.res.ColorStateList.valueOf(gold)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(238, 239, 244))
        }
        upgrade.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)))
        upgrade.addView(spacer(10))
        upgrade.addView(text(if (state.businessLevel >= names.lastIndex) "You built the full empire." else "$price XP required for the next upgrade", 13f, muted))
        upgrade.addView(spacer(18))
        upgrade.addView(primaryButton(if (state.businessLevel == 0) "Buy my first business" else "Upgrade business") {
            when {
                state.businessLevel >= names.lastIndex -> Toast.makeText(this, "Your business is fully upgraded!", Toast.LENGTH_SHORT).show()
                state.xp < price -> Toast.makeText(this, "Earn ${price - state.xp} more XP", Toast.LENGTH_SHORT).show()
                else -> { state.xp -= price; state.businessLevel += 1; showBusiness() }
            }
        })
        addCard(root, upgrade, 14)
    }

    private fun showPetHint() {
        val hints = arrayOf(
            "Short daily practice beats one long weekly session.",
            "Repeat the corrected sentence aloud three times.",
            "Describe the objects around you in ${state.targetLanguage}.",
            "Do not chase perfection—keep the conversation moving."
        )
        val root = screen("Your learning companion", "Milo has a tip", "Small habits create fluent speakers.", true) { showHome() }
        val petCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(30), dp(24), dp(28))
            background = gradient(Color.rgb(240, 236, 255), Color.rgb(225, 246, 242), radius = 26)
            addView(text("🦊", 66f, navy, Typeface.NORMAL, Gravity.CENTER))
            addView(spacer(12))
            addView(pill("MILO • LANGUAGE COACH", purple, Color.WHITE))
            addView(spacer(20))
            addView(text("“${hints[(state.lessons + state.xp) % hints.size]}”", 21f, navy, Typeface.BOLD, Gravity.CENTER))
        }
        addCard(root, petCard, 4)
        root.addView(spacer(16))
        root.addView(primaryButton("Got it, Milo") { showHome() })
    }

    private fun showSettings() {
        val root = screen("Preferences", "Settings", "Keep your local AI connection and update source in one place.", true) { showHome() }
        val connection = card()
        connection.addView(text("AI connection", 19f, navy, Typeface.BOLD))
        connection.addView(spacer(6))
        connection.addView(text("Whisper transcription and Ollama feedback", 13f, muted))
        connection.addView(spacer(18))
        val api = field("Service URL", "http://10.0.2.2:8000").apply { setText(state.apiUrl) }
        labeledField(connection, "SERVICE URL", api)
        addCard(root, connection, 2)

        val updates = card()
        updates.addView(text("App updates", 19f, navy, Typeface.BOLD))
        updates.addView(spacer(6))
        updates.addView(text("GitHub repository used for APK releases", 13f, muted))
        updates.addView(spacer(18))
        val repo = field("GitHub repository", "owner/repository").apply { setText(state.githubRepository) }
        labeledField(updates, "GITHUB REPOSITORY", repo)
        addCard(root, updates, 12)

        root.addView(spacer(16))
        root.addView(primaryButton("Save settings") {
            state.apiUrl = api.text.toString()
            state.githubRepository = repo.text.toString()
            showHome()
        })
        root.addView(spacer(10))
        root.addView(secondaryButton("Change language pair") { showLanguages() })
    }

    private fun checkUpdate(root: LinearLayout) {
        if (state.githubRepository.isBlank()) {
            Toast.makeText(this, "Set a GitHub repository in Settings", Toast.LENGTH_LONG).show()
            return
        }
        val statusCard = card(16, 18)
        val status = text("Checking GitHub Releases…", 14f, navy, Typeface.BOLD)
        statusCard.addView(status)
        addCard(root, statusCard, 12)
        thread {
            runCatching { ApiClient.latestRelease(state.githubRepository) }
                .onSuccess { release -> runOnUiThread {
                    val tag = release.optString("tag_name").removePrefix("v")
                    if (tag == BuildConfig.VERSION_NAME) {
                        status.text = "✓  You are running the latest version (${BuildConfig.VERSION_NAME})."
                        status.setTextColor(mint)
                    } else {
                        status.text = "Version $tag is ready to download."
                        val assets = release.optJSONArray("assets")
                        var apkUrl = release.optString("html_url")
                        if (assets != null) for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.optString("name").endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                        val finalUrl = apkUrl
                        statusCard.addView(spacer(12))
                        statusCard.addView(primaryButton("Open secure download") {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
                        })
                    }
                }}
                .onFailure { error -> runOnUiThread {
                    status.text = "Update check failed: ${error.message}"
                    status.setTextColor(danger)
                }}
        }
    }
}
