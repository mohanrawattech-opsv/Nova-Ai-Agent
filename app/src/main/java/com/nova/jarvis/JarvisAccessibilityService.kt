package com.nova.jarvis

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val action = pendingAction ?: return
        pendingAction = null
        val root = rootInActiveWindow ?: return
        when (action.first) {
            "click" -> clickNode(root, action.second)
            "type" -> typeIntoFocused(root, action.second)
        }
    }
    override fun onInterrupt() {}

    private fun clickNode(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString()?.contains(text, true) == true || node.contentDescription?.toString()?.contains(text, true) == true) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            node.parent?.let { if (it.isClickable && it.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true }
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { if (clickNode(it, text)) return true }
        return false
    }

    private fun typeIntoFocused(node: AccessibilityNodeInfo, value: String): Boolean {
        if (node.isFocused && (node.isEditable || node.className?.toString()?.contains("EditText") == true)) {
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            })
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { if (typeIntoFocused(it, value)) return true }
        return false
    }

    fun captureScreen() {
        if (Build.VERSION.SDK_INT < 30) return
        takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                val buffer = result.hardwareBuffer
                val bitmap = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                buffer.close()
                if (bitmap != null) saveBitmap(bitmap)
            }
            override fun onFailure(errorCode: Int) { }
        })
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "NovaScreenshot_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NovaJARVIS")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    companion object {
        @Volatile private var instance: JarvisAccessibilityService? = null
        @Volatile private var pendingAction: Pair<String, String>? = null
        fun isEnabled(context: android.content.Context): Boolean {
            val expected = context.packageName + "/" + JarvisAccessibilityService::class.java.name
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return enabled.split(':').any { it.equals(expected, true) }
        }
        fun requestClick(text: String) { pendingAction = "click" to text; instance?.onAccessibilityEvent(null) }
        fun requestType(text: String) { pendingAction = "type" to text; instance?.onAccessibilityEvent(null) }
        fun requestScreenshot() { instance?.captureScreen() }
    }
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { instance = null; super.onDestroy() }
}
