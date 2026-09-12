package com.floribert.autosaveflopro.notification

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.floribert.autosaveflopro.data.model.DetectedContact
import com.floribert.autosaveflopro.data.model.DetectionStatus
import com.floribert.autosaveflopro.data.repository.ContactRepository
import com.floribert.autosaveflopro.data.repository.LicenseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutoScanAccessibilityService : AccessibilityService() {

    @Inject lateinit var repository: ContactRepository
    @Inject lateinit var licenseRepository: LicenseRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var lastScrollTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!licenseRepository.isAutoScanEnabled() || !licenseRepository.isLicenseActive()) return

        val packageName = event.packageName?.toString() ?: ""
        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            val rootNode = rootInActiveWindow ?: return
            
            // Background scan for numbers
            serviceScope.launch(Dispatchers.Default) {
                scanNode(rootNode, packageName)
            }

            // Enhanced Auto-Scroll: Only if the user is in the main chat list
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED || 
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                val now = System.currentTimeMillis()
                if (now - lastScrollTime > 5000) { // Increased to 5s for better stability
                    if (isChatListVisible(rootNode)) {
                        attemptAutoScroll(rootNode)
                        lastScrollTime = now
                    }
                }
            }
        }
    }

    private fun isChatListVisible(rootNode: AccessibilityNodeInfo): Boolean {
        // WhatsApp chat list often has a specific ID or structure
        // We look for any scrollable list that isn't a keyboard
        return findScrollableNode(rootNode) != null
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable && (node.className?.contains("ListView") == true || 
            node.className?.contains("RecyclerView") == true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val result = findScrollableNode(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private fun attemptAutoScroll(rootNode: AccessibilityNodeInfo) {
        val scrollableNode = findScrollableNode(rootNode)
        scrollableNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    private fun scanNode(node: AccessibilityNodeInfo?, source: String) {
        if (node == null) return
        
        // 1. Skip editable fields (Typing area)
        if (node.isEditable || node.className?.contains("EditText") == true) return

        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        // Regex for phone numbers (Universal + Local)
        val phoneRegex = Regex("""(\+?\d{8,15})|(\b0[67]\d{8}\b)""")
        val match = phoneRegex.find(text) ?: phoneRegex.find(contentDesc)
        
        if (match != null) {
            val rawNumber = match.value.replace(Regex("""[\s-]"""), "")
            val normalizedNumber = if (rawNumber.startsWith("0") && rawNumber.length == 10) {
                "255" + rawNumber.substring(1)
            } else rawNumber

            if (normalizedNumber.length >= 8) {
                val detectedName = findNameInParent(node)
                saveIfNew(normalizedNumber, detectedName, source)
            }
        }

        // 2. Recursive scan children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            scanNode(child, source)
        }
    }

    private fun findNameInParent(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        val nameRegex = Regex("""[~@%]\s*([\w\s]+)""")
        
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i) ?: continue
            val cText = child.text?.toString() ?: ""
            val cDesc = child.contentDescription?.toString() ?: ""
            
            val match = nameRegex.find(cText) ?: nameRegex.find(cDesc)
            if (match != null) return match.groupValues[1].trim()
        }
        return null
    }

    private fun saveIfNew(number: String, name: String?, source: String) {
        serviceScope.launch {
            if (!repository.isAlreadyRecorded(number) && !repository.isNumberInContacts(number)) {
                val contact = DetectedContact(
                    phoneNumber = number,
                    senderName = name,
                    sourceApp = if (source.contains("w4b")) "WA Business Auto" else "WhatsApp Auto",
                    detectedAtTimestamp = System.currentTimeMillis(),
                    status = DetectionStatus.PENDING
                )
                repository.saveDetectedContact(contact)
                Log.d("AutoScan", "Auto-detected: $number as $name")
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
