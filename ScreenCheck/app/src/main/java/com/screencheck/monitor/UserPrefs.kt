package com.screencheck.monitor

import android.content.Context

/**
 * Tiny wrapper around SharedPreferences for the onboarding fields.
 * Swap this for DataStore or a real backend once you're past the prototype stage.
 */
object UserPrefs {
    private const val FILE = "screencheck_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_AGE = "age"
    private const val KEY_LOCATION_OPT_IN = "location_opt_in"
    private const val KEY_ONBOARDED = "onboarded"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun saveOnboarding(context: Context, username: String, age: Int, locationOptIn: Boolean) {
        prefs(context).edit()
            .putString(KEY_USERNAME, username)
            .putInt(KEY_AGE, age)
            .putBoolean(KEY_LOCATION_OPT_IN, locationOptIn)
            .putBoolean(KEY_ONBOARDED, true)
            .apply()
    }

    fun isOnboarded(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDED, false)

    fun getUsername(context: Context): String =
        prefs(context).getString(KEY_USERNAME, "") ?: ""

    fun getAge(context: Context): Int =
        prefs(context).getInt(KEY_AGE, 0)

    fun getLocationOptIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCATION_OPT_IN, false)
}
