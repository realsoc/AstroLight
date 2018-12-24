package com.falouvaleu.astrolight

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.falouvaleu.astrolight.NotificationController.Companion.FOREGROUND_NOTIFICATION_ID

class LightService: Service() {
    private var view: View? = null
    private lateinit var notificationController: NotificationController

    private var brightness: Int
        get() = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        set(value) {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationController = NotificationControllerImpl(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return super.onStartCommand(intent, flags, startId)
        when(intent.action) {
            ENABLE_LIGHT -> {
                startLight()
                notificationController.bindServiceToNotification(this, NotificationController.Type.LIGHT_ACTIVE)
            }
            DISABLE_LIGHT -> {
                stopLight(startId)
                notificationController.cancelNotification(FOREGROUND_NOTIFICATION_ID)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startLight() {
        lightOn = true
        val windowManager = baseContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_PHONE else
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val flags = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        } else {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }

        val layoutParams = WindowManager.LayoutParams(layoutType).apply {
            width = MATCH_PARENT
            height = MATCH_PARENT
            format = PixelFormat.RGBA_8888
            this.flags = flags
        }

        view = View(this).apply {
            setBackgroundColor(Color.RED)
            alpha = 0.65F
        }

        windowManager.addView(view, layoutParams)
        windowManager.defaultDisplay

        val newBrightness = otherBrightness
        otherBrightness = brightness
        brightness = newBrightness
    }

    private fun stopLight(startId: Int) {
        lightOn = false
        brightness = otherBrightness

        if(view != null) {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view)
            view = null
        }
        stopForeground(true)
        stopSelf(startId)
    }

    companion object {
        const val ENABLE_LIGHT = "ENABLE_LIGHT"
        const val DISABLE_LIGHT = "DISABLE_LIGHT"

        const val BRIGHTNESS_PREFERENCE_KEY = "BRIGHTNESS_PREFERENCE"

        var lightOn = false
        var otherBrightness: Int = 20

        fun request(action: String) {
            val intent = Intent(AstroLightApplication.instance, LightService::class.java)
            intent.action = action

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AstroLightApplication.instance.startForegroundService(intent)
            } else {
                AstroLightApplication.instance.startService(intent)
            }
        }

    }
}
