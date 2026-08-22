package com.dicypruss.hangcy.history

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class RejectedCallStore private constructor(context: Context) {
    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val lock = Any()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun load(): List<RejectedCall> = synchronized(lock) {
        readUnlocked()
    }

    fun append(call: RejectedCall) {
        synchronized(lock) {
            val next = (listOf(call) + readUnlocked()).take(MAX_ENTRIES)
            writeUnlocked(next)
        }
        listeners.forEach { it() }
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun readUnlocked(): List<RejectedCall> {
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        RejectedCall(
                            atMillis = obj.getLong(KEY_AT),
                            number = obj.optString(KEY_NUMBER, ""),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeUnlocked(calls: List<RejectedCall>) {
        val array = JSONArray()
        calls.forEach { call ->
            array.put(
                JSONObject()
                    .put(KEY_AT, call.atMillis)
                    .put(KEY_NUMBER, call.number),
            )
        }
        file.writeText(array.toString())
    }

    companion object {
        private const val FILE_NAME = "rejected_calls.json"
        private const val MAX_ENTRIES = 100
        private const val KEY_AT = "atMillis"
        private const val KEY_NUMBER = "number"

        @Volatile
        private var instance: RejectedCallStore? = null

        fun get(context: Context): RejectedCallStore {
            return instance ?: synchronized(this) {
                instance ?: RejectedCallStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
