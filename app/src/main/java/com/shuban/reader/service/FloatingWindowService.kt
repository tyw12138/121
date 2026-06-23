package com.shuban.reader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.android.material.tabs.TabLayout
import com.shuban.reader.R
import com.shuban.reader.ai.MockAIEngine

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var minimizedView: View? = null
    private var isExpanded = true
    private var floatingParams: WindowManager.LayoutParams? = null

    private val aiEngine = MockAIEngine()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            showFloatingWindow()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingWindow()
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
    }

    private fun showFloatingWindow() {
        val params = createLayoutParams()
        floatingParams = params

        val themedContext = ContextThemeWrapper(this, R.style.Theme_ShuBan)

        floatingView = LayoutInflater.from(themedContext).inflate(R.layout.floating_window, null)
        setupFloatingWindow(floatingView!!)

        minimizedView = LayoutInflater.from(themedContext).inflate(R.layout.floating_window_minimized, null)
        setupMinimizedWindow(minimizedView!!)

        setupTouchListener(floatingView!!, params)
        setupMinimizedTouchListener(minimizedView!!, params)

        windowManager.addView(floatingView, params)
        isExpanded = true
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(floatingView?.windowToken, 0)
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, 0)
    }

    private fun setupFloatingWindow(view: View) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val layoutChat = view.findViewById<LinearLayout>(R.id.layout_chat)
        val layoutSummary = view.findViewById<LinearLayout>(R.id.layout_summary)
        val layoutCharacters = view.findViewById<LinearLayout>(R.id.layout_characters)
        val layoutRecommend = view.findViewById<LinearLayout>(R.id.layout_recommend)

        val etInputText = view.findViewById<EditText>(R.id.et_input_text)
        val spinnerCharacter = view.findViewById<Spinner>(R.id.spinner_character)
        val etQuestion = view.findViewById<EditText>(R.id.et_question)
        val btnSend = view.findViewById<Button>(R.id.btn_send)
        val tvChatResponse = view.findViewById<TextView>(R.id.tv_chat_response)
        val tvSummary = view.findViewById<TextView>(R.id.tv_summary)
        val btnAnalyze = view.findViewById<Button>(R.id.btn_analyze)
        val btnEmotion = view.findViewById<Button>(R.id.btn_emotion)
        val btnTimeline = view.findViewById<Button>(R.id.btn_timeline)
        val characterContainer = view.findViewById<LinearLayout>(R.id.character_container)
        val btnCharacter = view.findViewById<Button>(R.id.btn_character)
        val btnRelation = view.findViewById<Button>(R.id.btn_relation)
        val recommendContainer = view.findViewById<LinearLayout>(R.id.recommend_container)
        val btnRecommend = view.findViewById<Button>(R.id.btn_recommend)
        val btnMinimize = view.findViewById<View>(R.id.btn_minimize)
        val btnClose = view.findViewById<View>(R.id.btn_close)

        // 角色选择 Spinner
        val characters = aiEngine.getAvailableCharacters()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, characters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCharacter.adapter = adapter

        etInputText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showKeyboard(etInputText)
        }

        etQuestion.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showKeyboard(etQuestion)
        }

        // Tab 切换
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                hideKeyboard()
                etInputText.clearFocus()
                etQuestion.clearFocus()

                layoutChat.visibility = View.GONE
                layoutSummary.visibility = View.GONE
                layoutCharacters.visibility = View.GONE
                layoutRecommend.visibility = View.GONE

                when (tab?.position) {
                    0 -> layoutChat.visibility = View.VISIBLE
                    1 -> layoutSummary.visibility = View.VISIBLE
                    2 -> layoutCharacters.visibility = View.VISIBLE
                    3 -> layoutRecommend.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 对话发送
        btnSend.setOnClickListener {
            val text = etInputText.text.toString()
            val question = etQuestion.text.toString()
            val selectedCharacter = spinnerCharacter.selectedItem?.toString() ?: ""

            if (text.isNotEmpty() && question.isNotEmpty()) {
                val response = if (selectedCharacter.isNotEmpty()) {
                    "【$selectedCharacter 说】\n${aiEngine.chatWithCharacter(selectedCharacter, question)}"
                } else {
                    aiEngine.chat(text, question)
                }
                tvChatResponse.text = response
                etQuestion.text.clear()
            }
            hideKeyboard()
            etInputText.clearFocus()
            etQuestion.clearFocus()
        }

        // 剧情摘要
        btnAnalyze.setOnClickListener {
            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                tvSummary.text = aiEngine.summarize(text)
            }
            hideKeyboard()
            etInputText.clearFocus()
        }

        // 情感分析
        btnEmotion.setOnClickListener {
            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                tvSummary.text = aiEngine.analyzeEmotion(text)
            }
            hideKeyboard()
            etInputText.clearFocus()
        }

        // 时间线
        btnTimeline.setOnClickListener {
            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                val timeline = aiEngine.extractTimeline(text)
                val sb = StringBuilder("【剧情时间线】\n\n")
                for (event in timeline) {
                    val emotionIcon = when (event.emotion) {
                        "平静" -> "🟢"
                        "紧张" -> "🔴"
                        "坚定" -> "🔵"
                        "激烈" -> "🟠"
                        "震撼" -> "⚡"
                        "转折" -> "🔄"
                        else -> "⚪"
                    }
                    sb.append("$emotionIcon ${event.chapter}：${event.event}\n\n")
                }
                tvSummary.text = sb.toString()
            }
            hideKeyboard()
            etInputText.clearFocus()
        }

        // 人物卡片
        btnCharacter.setOnClickListener {
            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                val characterList = aiEngine.extractCharacters(text)
                characterContainer.removeAllViews()
                for (character in characterList) {
                    val characterView = LayoutInflater.from(view.context)
                        .inflate(R.layout.item_character, characterContainer, false)
                    characterView.findViewById<TextView>(R.id.tv_character_avatar).text = character.avatar
                    characterView.findViewById<TextView>(R.id.tv_character_name).text = character.name
                    characterView.findViewById<TextView>(R.id.tv_character_role).text = character.role
                    characterView.findViewById<TextView>(R.id.tv_character_personality).text = character.personality
                    characterView.findViewById<TextView>(R.id.tv_character_relation).text = character.relation
                    characterContainer.addView(characterView)
                }
            }
            hideKeyboard()
            etInputText.clearFocus()
        }

        // 关系图谱
        btnRelation.setOnClickListener {
            val relations = aiEngine.getCharacterRelations()
            characterContainer.removeAllViews()
            val header = TextView(view.context).apply {
                text = "【人物关系图谱】\n"
                setTextColor(resources.getColor(R.color.primary, null))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8, 8, 8, 4)
            }
            characterContainer.addView(header)

            for (relation in relations) {
                val tv = TextView(view.context).apply {
                    text = "🔗 $relation"
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    textSize = 12f
                    setPadding(12, 4, 8, 4)
                }
                characterContainer.addView(tv)
            }
            hideKeyboard()
            etInputText.clearFocus()
        }

        // 推荐
        btnRecommend.setOnClickListener {
            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                val recommendations = aiEngine.recommend(text)
                recommendContainer.removeAllViews()
                for (book in recommendations) {
                    val bookView = LayoutInflater.from(view.context)
                        .inflate(R.layout.item_book_recommendation, recommendContainer, false)
                    bookView.findViewById<TextView>(R.id.tv_book_title).text = book.title
                    bookView.findViewById<TextView>(R.id.tv_book_author).text = book.author
                    bookView.findViewById<TextView>(R.id.tv_book_genre).text = book.genre
                    bookView.findViewById<TextView>(R.id.tv_book_reason).text = book.reason
                    bookView.findViewById<TextView>(R.id.tv_match_score).text = "${book.matchScore}%匹配"
                    recommendContainer.addView(bookView)
                }
            }
            hideKeyboard()
            etInputText.clearFocus()
        }

        btnMinimize.setOnClickListener {
            hideKeyboard()
            etInputText.clearFocus()
            etQuestion.clearFocus()
            toggleWindowState()
        }

        btnClose.setOnClickListener {
            stopSelf()
        }
    }

    private fun setupMinimizedWindow(view: View) {
        // 点击展开逻辑在 setupMinimizedTouchListener 中处理
    }

    private fun setupMinimizedTouchListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleWindowState()
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleWindowState() {
        hideKeyboard()

        val params = createLayoutParams()
        floatingParams = params

        if (isExpanded) {
            windowManager.removeView(floatingView)
            windowManager.addView(minimizedView, params)
            isExpanded = false
        } else {
            windowManager.removeView(minimizedView)
            windowManager.addView(floatingView, params)
            isExpanded = true
        }
    }

    private fun setupTouchListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                        isDragging = true
                        hideKeyboard()
                    }
                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_OUTSIDE -> {
                    hideKeyboard()
                    true
                }
                else -> false
            }
        }
    }

    private fun removeFloatingWindow() {
        hideKeyboard()
        floatingView?.let {
            if (it.isShown) {
                windowManager.removeView(it)
            }
        }
        minimizedView?.let {
            if (it.isShown) {
                windowManager.removeView(it)
            }
        }
        floatingView = null
        minimizedView = null
        floatingParams = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "书伴服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "书伴悬浮窗服务运行中"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("书伴")
            .setContentText("AI 阅读助手运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "shuban_floating_window"
        private const val NOTIFICATION_ID = 1
    }
}
