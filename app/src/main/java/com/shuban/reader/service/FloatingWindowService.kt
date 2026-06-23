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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
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

    private fun createLayoutParams(focusable: Boolean = false): WindowManager.LayoutParams {
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            if (focusable) flags else flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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

        setupDragListener(floatingView!!, params)
        setupDragListener(minimizedView!!, params)

        windowManager.addView(floatingView, params)
        isExpanded = true
    }

    private fun updateFocusable(focusable: Boolean) {
        val params = floatingParams ?: return
        val view = if (isExpanded) floatingView else minimizedView ?: return

        val flags = params.flags
        if (focusable) {
            params.flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
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

        val etInputText = view.findViewById<EditText>(R.id.et_input_text)
        val etQuestion = view.findViewById<EditText>(R.id.et_question)
        val btnSend = view.findViewById<Button>(R.id.btn_send)
        val tvChatResponse = view.findViewById<TextView>(R.id.tv_chat_response)
        val tvSummary = view.findViewById<TextView>(R.id.tv_summary)
        val btnAnalyze = view.findViewById<Button>(R.id.btn_analyze)
        val characterContainer = view.findViewById<LinearLayout>(R.id.character_container)
        val btnCharacter = view.findViewById<Button>(R.id.btn_character)
        val btnMinimize = view.findViewById<View>(R.id.btn_minimize)
        val btnClose = view.findViewById<View>(R.id.btn_close)

        etInputText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateFocusable(true)
                showKeyboard(etInputText)
            } else {
                hideKeyboard()
                updateFocusable(false)
            }
        }

        etQuestion.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateFocusable(true)
                showKeyboard(etQuestion)
            } else {
                hideKeyboard()
                updateFocusable(false)
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                hideKeyboard()
                updateFocusable(false)
                etInputText.clearFocus()
                etQuestion.clearFocus()

                when (tab?.position) {
                    0 -> {
                        layoutChat.visibility = View.VISIBLE
                        layoutSummary.visibility = View.GONE
                        layoutCharacters.visibility = View.GONE
                    }
                    1 -> {
                        layoutChat.visibility = View.GONE
                        layoutSummary.visibility = View.VISIBLE
                        layoutCharacters.visibility = View.GONE
                    }
                    2 -> {
                        layoutChat.visibility = View.GONE
                        layoutSummary.visibility = View.GONE
                        layoutCharacters.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btnSend.setOnClickListener {
            val text = etInputText.text.toString()
            val question = etQuestion.text.toString()
            if (text.isNotEmpty() && question.isNotEmpty()) {
                val response = aiEngine.chat(text, question)
                tvChatResponse.text = response
                etQuestion.text.clear()
            }
            hideKeyboard()
            updateFocusable(false)
            etInputText.clearFocus()
            etQuestion.clearFocus()
        }

        btnAnalyze.setOnClickListener {
            hideKeyboard()
            updateFocusable(false)
            etInputText.clearFocus()
            etQuestion.clearFocus()

            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                val summary = aiEngine.summarize(text)
                tvSummary.text = summary
            }
        }

        btnCharacter.setOnClickListener {
            hideKeyboard()
            updateFocusable(false)
            etInputText.clearFocus()
            etQuestion.clearFocus()

            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                val characters = aiEngine.extractCharacters(text)
                characterContainer.removeAllViews()
                for (character in characters) {
                    val characterView = LayoutInflater.from(view.context)
                        .inflate(R.layout.item_character, characterContainer, false)
                    characterView.findViewById<TextView>(R.id.tv_character_name).text = character.name
                    characterView.findViewById<TextView>(R.id.tv_character_role).text = character.role
                    characterView.findViewById<TextView>(R.id.tv_character_relation).text = character.relation
                    characterContainer.addView(characterView)
                }
            }
        }

        btnMinimize.setOnClickListener {
            hideKeyboard()
            updateFocusable(false)
            etInputText.clearFocus()
            etQuestion.clearFocus()
            toggleWindowState()
        }

        btnClose.setOnClickListener {
            stopSelf()
        }
    }

    private fun setupMinimizedWindow(view: View) {
        view.setOnClickListener {
            toggleWindowState()
        }
    }

    private fun toggleWindowState() {
        hideKeyboard()
        updateFocusable(false)

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

    private fun setupDragListener(view: View, params: WindowManager.LayoutParams) {
        val titleBar = view.findViewById<View>(R.id.btn_minimize)?.parent as? View ?: view
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        titleBar.setOnTouchListener { _, event ->
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
                        updateFocusable(false)
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
