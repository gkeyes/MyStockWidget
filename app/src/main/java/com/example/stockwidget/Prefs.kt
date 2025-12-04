package com.example.stockwidget

import android.content.Context

object Prefs {
    private const val PREF_NAME = "stock_widget_prefs"
    private const val KEY_CODES = "saved_codes"
    private const val DEFAULT_CODES = "sh000001,sh600519,sz300750,sh601127,sz000002"

    fun getSavedCodes(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CODES, DEFAULT_CODES) ?: DEFAULT_CODES
    }

    fun saveCodes(context: Context, codes: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CODES, codes)
            .apply()
    }
}
