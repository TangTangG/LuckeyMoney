package com.bedone.luckymoney

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo


class LuckyMoneyAccessibilityService : AccessibilityService() {
    private lateinit var catcher: LuckyMoneyCatcher

    private val wechatCatcher = LuckyMoneyCatcher("LauncherUI", "ChattingUI",
            "LuckyMoneyReceiveUI", "LuckyMoneyDetailUI"
    )
    private val weworkCatcher = LuckyMoneyCatcher("WwMainActivity",
            "MessageListActivity", "RedEnvelopeCollectorActivity",
            "RedEnvelopeDetailActivity"
    )
    private var currentActivity = ""

    override fun onInterrupt() {

    }

    override fun onCreate() {
        super.onCreate()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        if (Build.VERSION.SDK_INT >= 26) {
            val builder = Notification.Builder(this, getString(R.string.app_name))
            builder.setContentIntent(pendingIntent)
            val notification = builder.build()
            startForeground(1, notification)
        } else {
            val notify = Notification()
//            notify.setLatestEventInfo(this, "My title", "My content", pendingIntent)
            startForeground(1, notify)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        catcher = if ("com.tencent.mm" == event?.packageName.toString()) wechatCatcher else weworkCatcher
        setCurrentActivityInfo(event)
        catcher.catcher(event)
    }

    private fun setCurrentActivityInfo(event: AccessibilityEvent?) {
        if (event!!.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        currentActivity = try {
            val componentName = ComponentName(
                    event.packageName.toString(),
                    event.className.toString()
            )
            packageManager.getActivityInfo(componentName, 0)
            componentName.flattenToShortString()
        } catch (e: PackageManager.NameNotFoundException) {
            LogTool.e("get package info error.")
            ""
        }
        LogTool.i("currentAct:" + currentActivity)
    }

    inner class LuckyMoneyCatcher(val mainActivity: String,
                                  private val chattingActivity: String,
                                  private val receiveActivity: String,
                                  private val detailActivity: String) {

        private val wechatViewSelfCn = "查看红包"
        private val wechatViewOthersCn = "领取红包"
        private var rootNodeInfo: AccessibilityNodeInfo? = null

        private var workingState = -1

        private val PACKGE_CLICK = 0
        private val OPEN_CLICK = 1
        private val DO_BACK = 2
        private val handler: Handler = Handler(Looper.getMainLooper(), Handler.Callback {
            when (it.what) {
                PACKGE_CLICK ->
                    (it.obj as AccessibilityNodeInfo).performAction(AccessibilityNodeInfo.ACTION_CLICK)
                OPEN_CLICK -> performOpen()
                DO_BACK -> performBack()
            }
            true
        })

        private fun reset() {
            workingState = -1
        }

        private fun performOpen() {
            if (Build.VERSION.SDK_INT >= 24) {
                LogTool.d("try open ")
                val metrics: DisplayMetrics = resources.displayMetrics
                val dpi: Int = metrics.density.toInt()
                val path = Path()
                if (640 == dpi) {
                    path.moveTo(720f, 1575f)
                } else {
                    path.moveTo(540f, 1150f)
                }
                val builder = GestureDescription.Builder()
                val description = builder.addStroke(GestureDescription.StrokeDescription(path, 5, 1)).build()
                dispatchGesture(description, object : AccessibilityService.GestureResultCallback() {
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        LogTool.d("click canceled")
                        reset()
                    }

                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        LogTool.d("click complete")
                    }
                }, null)
            } else {
                //check

            }
        }

        fun catcher(event: AccessibilityEvent?) {
            if (event == null) {
                return
            }
            rootNodeInfo = rootInActiveWindow
            analysisNode()
        }

        private fun analysisNode() {
            if (rootNodeInfo == null) {
                return
            }
            if (workingState <= 0){
                val node = getTheLastNode(wechatViewOthersCn, wechatViewSelfCn)
                if (node != null && currentActivity.contains(chattingActivity)) {
                    val parent = node.parent
                    val bounds = Rect()
                    parent.getBoundsInScreen(bounds)
                    //visible
                    if (bounds.top >= 0) {
                        workingState = PACKGE_CLICK
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } else {
                        reset()
                    }
                }
            }
            if (workingState == PACKGE_CLICK) {
                if (currentActivity.contains(receiveActivity)) {
                    workingState = OPEN_CLICK
                    handler.sendEmptyMessage(OPEN_CLICK)
                } else if (currentActivity.contains(detailActivity)) {
                    workingState = -1
                    handler.sendEmptyMessageDelayed(DO_BACK, 36)
                }
            }

            if (workingState == OPEN_CLICK) {
                if (currentActivity.contains(detailActivity)) {
                    workingState = -1
                    handler.sendEmptyMessageDelayed(DO_BACK, 36)
                } else if(currentActivity.contains(chattingActivity)){
                    reset()
                }
            }
        }

        private fun performBack() {
            LogTool.d("do back")
            reset()
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        private fun hasAnyOfNodes(vararg texts: String): Boolean {
            return foreachNode(rootNodeInfo!!, action = {
                var result = false
                if ("android.widget.TextView" == it.className)
                    for (text in texts) {
                        result = text == it.text
                        if (result) {
                            break
                        }
                    }
                result
            })
        }

        private fun foreachNode(nodeInfo: AccessibilityNodeInfo,
                                action: (child: AccessibilityNodeInfo) -> Boolean): Boolean {
            val count = nodeInfo.childCount
            for (i in 0 until count) {
                val child = nodeInfo.getChild(i)
                val childCount = child.childCount
                if (childCount == 0) {
                    if (action(child)) {
                        return true
                    }
                } else {
                    if (foreachNode(child, action)) {
                        return true
                    }
                }
            }
            return false
        }

        private fun getTheLastNode(vararg texts: String): AccessibilityNodeInfo? {
            var bottom = 0
            var lastNode: AccessibilityNodeInfo? = null
            var tempNode: AccessibilityNodeInfo
            var nodes: List<AccessibilityNodeInfo>
            for (text in texts) {
                if (TextUtils.isEmpty(text)) {
                    continue
                }
                nodes = rootNodeInfo!!.findAccessibilityNodeInfosByText(text)

                if (nodes != null && !nodes.isEmpty()) {
                    tempNode = nodes.last()
                    if (tempNode == null) {
                        return null
                    }
                    val bounds = Rect()
                    tempNode.getBoundsInScreen(bounds)
                    if (bounds.bottom > bottom) {
                        bottom = bounds.bottom
                        lastNode = tempNode
                    }
                }

            }
            return lastNode
        }

        fun onDestroy(){
            handler.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        LogTool.d("destroy ")
        catcher.onDestroy()
        super.onDestroy()
    }
}
