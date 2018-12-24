package com.falouvaleu.astrolight

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import com.falouvaleu.astrolight.LightService.Companion.BRIGHTNESS_PREFERENCE_KEY
import com.falouvaleu.astrolight.LightService.Companion.DISABLE_LIGHT
import com.falouvaleu.astrolight.LightService.Companion.ENABLE_LIGHT
import kotlinx.android.synthetic.main.activity_main.*
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat

class LightActivity : AppCompatActivity(), View.OnClickListener {

    private var operationInProgress: (() -> Unit)? = null
    var brightness: Int
        get() = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        set(value) {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttonStar.setOnClickListener(this)
        seekBarBrightness.max = 255
        seekBarBrightness.progress = PreferenceManager.getDefaultSharedPreferences(this).getInt(BRIGHTNESS_PREFERENCE_KEY, 50)
        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (LightService.lightOn) {
                    brightness = progress
                } else {
                    LightService.otherBrightness = progress
                }
                PreferenceManager.getDefaultSharedPreferences(this@LightActivity).edit().putInt(BRIGHTNESS_PREFERENCE_KEY, progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        setUINightMode(LightService.lightOn)
        processIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when(v.id) {
            buttonStar.id -> {
                nightMode(!LightService.lightOn)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            CODE_MANAGE_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                    operationInProgress?.let { it() }
                }
                operationInProgress = null
            }
            CODE_WRITE_SETTINGS_PERMISSION -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this)) {
                    operationInProgress?.let { it() }
                }
                operationInProgress = null
            }
        }

        if (resultCode == Activity.RESULT_OK) {
            data?.action?.let { LightService.request(it) }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            CODE_MANAGE_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                    operationInProgress?.let { it() }
                }
                operationInProgress = null
            }
        }
    }

    private fun processIntent(intent: Intent?) {
        when(intent?.action) {
            DISABLE_LIGHT -> nightMode(false)
            ENABLE_LIGHT -> nightMode(true)
        }
    }

    private fun runWriteSettingSafeOperation(operation: () -> Unit) {
        val permission: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(this)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        }
        if (permission) {
            operation()
        } else {
            operationInProgress = operation
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, CODE_WRITE_SETTINGS_PERMISSION)
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_SETTINGS),
                    CODE_WRITE_SETTINGS_PERMISSION
                )
            }
        }
    }

    private fun runManageOverlaySafeOperation(operation: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            operationInProgress = operation
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, CODE_MANAGE_OVERLAY_PERMISSION)
        } else {
            operation()
        }
    }

    private fun nightMode(nightMode: Boolean) {
        val action = if (nightMode) ENABLE_LIGHT else DISABLE_LIGHT
        setUINightMode(nightMode)
        runManageOverlaySafeOperation {
            runWriteSettingSafeOperation {
                LightService.request(action)
            }
        }
    }

    private fun setUINightMode(nightMode: Boolean) {
        if (nightMode) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    companion object {
        const val CODE_WRITE_SETTINGS_PERMISSION = 1
        const val CODE_MANAGE_OVERLAY_PERMISSION = 2
    }
}
