package org.tvheadend.tvhclient.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.R
import timber.log.Timber
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Converts the given url into a unique hash value.
 *
 * @param url The url that shall be converted
 * @return The hash value or the url or an empty string if an error occurred
 */
fun convertUrlToHashString(url: String?): String {
    if (url.isNullOrEmpty()) return ""
    try {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(url.toByteArray())
        val messageDigest = digest.digest()
        val hexString = StringBuilder()
        for (md in messageDigest) {
            hexString.append(Integer.toHexString(0xFF and md.toInt()))
        }
        return hexString.toString()
    } catch (e: NoSuchAlgorithmException) {
        Timber.e(e, "No algorithm was found to handle MD5")
    }
    return ""
}

/**
 * Returns the url to the file of the channel icon.
 *
 * @param context Context
 * @param url The url to the icon
 * @return A string of the url to the local icon file
 */
fun getIconUrl(context: Context, url: String?): String {
    // Replace all occurrences of + with the utf-8 value
    val urlEncoded = url?.replace("\\+", "%2b") ?: ""
    return "file://" + context.cacheDir + "/" + convertUrlToHashString(urlEncoded) + ".png"
}

/**
 * Returns the id of the theme that is currently set in the settings.
 *
 * @param context Context
 * @return Id of the light or dark theme
 */
fun getThemeId(context: Context): Int {
    return R.style.CustomTheme
}

fun Context.isInDarkMode() =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES