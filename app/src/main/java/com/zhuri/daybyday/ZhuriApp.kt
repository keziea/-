package com.zhuri.daybyday

import android.app.Application
import com.zhuri.daybyday.data.TaskRepository

class ZhuriApp : Application() {

    lateinit var repo: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = TaskRepository(this)
    }
}
