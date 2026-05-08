package com.moxmose.moxequiplog.utils

import android.content.Context

interface ResourceProvider {
    fun getString(id: Int): String
    fun getString(id: Int, vararg args: Any): String
}

class AndroidResourceProvider(private val context: Context) : ResourceProvider {
    override fun getString(id: Int): String = context.getString(id)
    override fun getString(id: Int, vararg args: Any): String = context.getString(id, *args)
}
