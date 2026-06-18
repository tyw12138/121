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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingWindow()
    }

    private fun showFloatingWindow() {
        // 创建悬浮窗布局参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        // 创建展开状态的悬浮窗
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
        setupFloatingWindow(floatingView!!)

        // 创建最小化状态的悬浮窗
        minimizedView = LayoutInflater.from(this).inflate(R.layout.floating_window_minimized, null)
        setupMinimizedWindow(minimizedView!!)

        // 添加触摸监听
        setupTouchListener(floatingView!!, params)
        setupTouchListener(minimizedView!!, params)

        // 默认显示展开状态
        windowManager.addView(floatingView, params)
        isExpanded = true
    }

    private fun setupFloatingWindow(view: View) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val contentContainer = view.findViewById<FrameLayout>(R.id.content_container)
        val layoutChat = view.findViewById<LinearLayout>(R.id.layout_chat)
        val layoutSummary = view.findViewById<LinearLayout>(R.id.layout_summary)
        val layoutCharacters = view.findViewById<LinearLayout>(R.id.layout_characters)

        val etInputText = view.findViewById<EditText>(R.id.et_input_text)
        val etQuestion = view.findViewById<EditText>(R.id.et_question)
        val btnSend = view.findViewById<Button>(R.id.btn_send)
        val tvSummary = view.findViewById<TextView>(R.id.tv_summary)
        val btnAnalyze = view.findViewById<Button>(R.id.btn_analyze)
        val characterContainer = view.findViewById<LinearLayout>(R.id.character_container)
        val btnCharacter = view.findViewById<Button>(R.id.btn_character)
        val btnMinimize = view.findViewById<View>(R.id.btn_minimize)

        // Tab 切换
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
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

        // 发送按钮
        btnSend.setOnClickListener {
            val text = etInputText.text.toString()
            val question = etQuestion.text.toString()
            if (text.isNotEmpty() && question.isNotEmpty()) {
                val response = aiEngine.chat(text, question)
                etInputText.setText(response)
                etQuestion.text.clear()
            }
        }

        // 分析剧情按钮
        btnAnalyze.setOnClickListener {
            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                val summary = aiEngine.summarize(text)
                tvSummary.text = summary
            }
        }

        // 人物关系按钮
        btnCharacter.setOnClickListener {
            val text = etInputText.text.toString()
            if (text.isNotEmpty()) {
                val characters = aiEngine.extractCharacters(text)
                characterContainer.removeAllViews()
                for (character in characters) {
                    val characterView = LayoutInflater.from(this)
                        .inflate(R.layout.item_character, characterContainer, false)
                    characterView.findViewById<TextView>(R.id.tv_character_name).text = character.name
                    characterView.findViewById<TextView>(R.id.tv_character_role).text = character.role
                    characterView.findViewById<TextView>(R.id.tv_character_relation).text = character.relation
                    characterContainer.addView(characterView)
                }
            }
        }

        // 最小化按钮
        btnMinimize.setOnClickListener {
            toggleWindowState()
        }
    }

    private fun setupMinimizedWindow(view: View) {
        view.setOnClickListener {
            toggleWindowState()
        }
    }

    private fun toggleWindowState() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        if (isExpanded) {
            // 切换到最小化状态
            windowManager.removeView(floatingView)
            windowManager.addView(minimizedView, params)
            isExpanded = false
        } else {
            // 切换到展开状态
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

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun removeFloatingWindow() {
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