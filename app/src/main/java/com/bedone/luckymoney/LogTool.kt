package com.bedone.luckymoney

import android.util.Log

/**
 * Created by CaiGao on 2018/2/21.
 */
class LogTool {

    companion object {

        private val TAG = "tang-info"

        fun i(msg: String) {
            Log.i(TAG, msg)
        }

        fun v(msg: String) {
            Log.v(TAG, msg)
        }

        fun d(msg: String) {
            Log.d(TAG, msg)
        }

        fun w(msg: String) {
            Log.w(TAG, msg)
        }

        fun e(msg: String) {
            Log.e(TAG, msg)
        }
    }
}