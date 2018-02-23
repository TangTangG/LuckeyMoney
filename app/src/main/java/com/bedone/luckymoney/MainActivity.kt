package com.bedone.luckymoney

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.provider.Settings;
import android.widget.Toast
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.widget.TextView


class MainActivity : Activity(), AccessibilityManager.AccessibilityStateChangeListener {
    private val accessibilityManager: AccessibilityManager by lazy {
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        accessibilityManager.addAccessibilityStateChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        updateServiceState()
    }

    fun configPlugin(view: View) {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.open_failed_tip, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun isServiceEnabled(): Boolean {
        val accessibilityServices = accessibilityManager.
                getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return accessibilityServices.any {
            it.id == packageName + "/com.bedone.luckymoney.LuckyMoneyAccessibilityService"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        accessibilityManager.removeAccessibilityStateChangeListener(this)
    }

    /**
     * This method in some os may do not valid.
     * So update service state in when resume.
     */
    override fun onAccessibilityStateChanged(enabled: Boolean) {
        LogTool.i("state changed " + enabled)
        updateServiceState()
    }

    private fun updateServiceState() {
        val configPlugin = findViewById<TextView>(R.id.config_plugin)
        if (isServiceEnabled()) {
            configPlugin?.text = getString(R.string.close_btn)
        } else {
            configPlugin?.text = getString(R.string.open_btn)
        }
    }
}
