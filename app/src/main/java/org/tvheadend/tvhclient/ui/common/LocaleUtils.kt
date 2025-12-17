package org.tvheadend.tvhclient.ui.common

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import java.util.*
import androidx.core.content.edit
import org.tvheadend.tvhclient.util.extensions.prefs


private const val SELECTED_LANGUAGE = "language"

fun onAttach(context: Context): Context {
    val lang = getPersistedData(context, Locale.getDefault().language)
    return setLocale(context, lang)
}

private fun setLocale(context: Context, language: String): Context {
    persist(context, language)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        updateResources(context, language)
    } else {
        updateResourcesLegacy(context, language)
    }
}

private fun getPersistedData(context: Context, defaultLanguage: String): String {
    return context.prefs.getString(SELECTED_LANGUAGE, defaultLanguage) ?: defaultLanguage
}

private fun persist(context: Context, language: String) {
    context.prefs.edit {
        putString(SELECTED_LANGUAGE, language)
    }
}

@TargetApi(Build.VERSION_CODES.N)
private fun updateResources(context: Context, language: String): Context {
    val locale = Locale(language)
    Locale.setDefault(locale)

    val configuration = context.resources.configuration
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)

    return context.createConfigurationContext(configuration)
}

private fun updateResourcesLegacy(context: Context, language: String): Context {
    val locale = Locale(language)
    Locale.setDefault(locale)
    val resources = context.resources
    val configuration = resources.configuration

    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    return context.createConfigurationContext(configuration)
}

fun getLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
    }
}