package com.notchflow.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.media.MediaMetadata
import android.content.ComponentName

class Pixel10DynamicIslandService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var islandView: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    
    private lateinit var islandContainer: FrameLayout
    private lateinit var islandIcon: ImageView
    private lateinit var islandTitle: TextView
    private lateinit var islandText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isExpanded = false
    private var hideRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        this.serviceInfo = info

        setupDynamicIsland()
        setupMediaListener()
    }

    private fun setupDynamicIsland() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        islandView = LayoutInflater.from(this).inflate(R.layout.dynamic_island_layout, null)

        islandContainer = islandView.findViewById(R.id.island_container)
        islandIcon = islandView.findViewById(R.id.island_icon)
        islandTitle = islandView.findViewById(R.id.island_title)
        islandText = islandView.findViewById(R.id.island_text)

        // Configuración para que flote sobre todo y se centre arriba
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutParams.y = 20 // Ajuste fino para el Pixel 10

        windowManager.addView(islandView, layoutParams)

        // Al tocar la isla, se contrae
        islandContainer.setOnClickListener {
            if (isExpanded) contractIsland()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val text = event.text.joinToString(" ")
            if (text.isNotEmpty()) {
                showNotification(text)
            }
        }
    }

    private fun showNotification(message: String) {
        islandTitle.text = "Nueva Notificación"
        islandText.text = message
        
        expandIsland()

        // Ocultar automáticamente después de 4 segundos
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = Runnable { contractIsland() }
        handler.postDelayed(hideRunnable!!, 4000)
    }

    private fun setupMediaListener() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, Pixel10DynamicIslandService::class.java)
            
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers ->
                if (controllers.isNotEmpty()) {
                    val controller = controllers[0]
                    controller.registerCallback(object : android.media.session.MediaController.Callback() {
                        override fun onMetadataChanged(metadata: MediaMetadata?) {
                            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                            if (title != null) {
                                islandTitle.text = "Reproduciendo"
                                islandText.text = "$title - $artist"
                                expandIsland()
                            }
                        }
                        
                        override fun onPlaybackStateChanged(state: PlaybackState?) {
                            if (state?.state == PlaybackState.STATE_PAUSED) {
                                contractIsland()
                            }
                        }
                    })
                }
            }, componentName)
        } catch (e: SecurityException) {
            // Faltan permisos de Notificaciones (NotificationListener)
        }
    }

    private fun expandIsland() {
        if (isExpanded) return
        isExpanded = true
        
        islandIcon.visibility = View.VISIBLE
        islandTitle.visibility = View.VISIBLE
        islandText.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(islandContainer.width, 800) // Ancho expandido
        animator.duration = 300
        animator.addUpdateListener { animation ->
            val layoutParams = islandContainer.layoutParams
            layoutParams.width = animation.animatedValue as Int
            islandContainer.layoutParams = layoutParams
        }
        animator.start()
    }

    private fun contractIsland() {
        if (!isExpanded) return
        isExpanded = false

        islandTitle.visibility = View.GONE
        islandText.visibility = View.GONE
        islandIcon.visibility = View.GONE

        val animator = ValueAnimator.ofInt(islandContainer.width, WindowManager.LayoutParams.WRAP_CONTENT)
        animator.duration = 300
        animator.addUpdateListener { animation ->
            val layoutParams = islandContainer.layoutParams
            layoutParams.width = animation.animatedValue as Int
            islandContainer.layoutParams = layoutParams
        }
        animator.start()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (::windowManager.isInitialized && ::islandView.isInitialized) {
            windowManager.removeView(islandView)
        }
    }
}
