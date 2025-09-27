package net.solvetheriddle.roundtimer

import android.content.Context

object AppContext {
    lateinit var INSTANCE: Context
        private set

    fun initialize(context: Context) {
        INSTANCE = context.applicationContext
    }
}
