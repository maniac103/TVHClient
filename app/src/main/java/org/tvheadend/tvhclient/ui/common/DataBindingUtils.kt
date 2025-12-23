package org.tvheadend.tvhclient.ui.common

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.tvheadend.data.entity.ProgramInterface
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.getIconUrl
import org.tvheadend.tvhclient.util.isInDarkMode
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import org.tvheadend.data.entity.RecordingInterface
import org.tvheadend.tvhclient.util.extensions.prefs

// Constants required for the date calculation
private const val ONE_DAY = 1000 * 3600 * 24
private const val TWO_DAYS = 1000 * 3600 * 24 * 2
private const val SIX_DAYS = 1000 * 3600 * 24 * 6

@BindingAdapter("marginStart")
fun setLayoutWidth(view: View, increaseMargin: Boolean) {
    val layoutParams = view.layoutParams
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val marginStart = (if (increaseMargin)
            view.context.resources.getDimension(R.dimen.dp_80)
        else
            view.context.resources.getDimension(R.dimen.dp_16)).toInt()

        layoutParams.marginStart = marginStart
        view.layoutParams = layoutParams
    }
}

@BindingAdapter("layoutWidth")
fun setLayoutWidth(view: View, width: Int) {
    val layoutParams = view.layoutParams as RecyclerView.LayoutParams
    layoutParams.width = width
    view.layoutParams = layoutParams
}

@BindingAdapter("startStopTextStart", "startStopTextStop")
fun setStartStopText(view: TextView, start: Long, stop: Long) {
    val df = if (view.context.prefs.localizedTimeFormat) {
        // Show the date as defined with the currently active locale.
        // For the date display the short version will be used
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, getLocale(view.context.resources))
    } else {
        // Show the date using the default format like 31.07.2013
        SimpleDateFormat("HH:mm", Locale.US)
    }

    val startText = df.format(start)
    val stopText = df.format(stop)
    view.text = "$startText - $stopText"
}

@BindingAdapter("seriesInfoText")
fun setSeriesInfoText(view: TextView, program: ProgramInterface?) {
    val context = view.context
    val season = context.resources.getString(R.string.season)
    val episode = context.resources.getString(R.string.episode)
    val part = context.resources.getString(R.string.part)

    var seriesInfo = ""
    if (program != null) {
        if (!program.episodeOnscreen.isNullOrEmpty()) {
            seriesInfo = program.episodeOnscreen ?: ""
        } else {
            if (program.seasonNumber > 0) {
                seriesInfo += String.format(Locale.getDefault(), "%s %02d",
                        season.lowercase(), program.seasonNumber)
            }
            if (program.episodeNumber > 0) {
                if (seriesInfo.isNotEmpty()) {
                    seriesInfo += ", "
                }
                seriesInfo += String.format(Locale.getDefault(), "%s %02d",
                        episode.lowercase(), program.episodeNumber)
            }
            if (program.partNumber > 0) {
                if (seriesInfo.isNotEmpty()) {
                    seriesInfo += ", "
                }
                seriesInfo += String.format(Locale.getDefault(), "%s %d",
                        part.lowercase(), program.partNumber)
            }
            if (seriesInfo.isNotEmpty()) {
                seriesInfo = seriesInfo.substring(0, 1).uppercase() + seriesInfo.substring(1)
            }
        }
    }
    view.isVisible = seriesInfo.isNotEmpty()
    view.text = seriesInfo
}

@BindingAdapter("contentTypeText")
fun setContentTypeText(view: TextView, contentType: Int) {
    val ret = SparseArray<String>()
    val context = view.context

    var s = context.resources.getStringArray(R.array.pr_content_type0)
    for (i in s.indices) {
        ret.append(i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type1)
    for (i in s.indices) {
        ret.append(0x10 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type2)
    for (i in s.indices) {
        ret.append(0x20 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type3)
    for (i in s.indices) {
        ret.append(0x30 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type4)
    for (i in s.indices) {
        ret.append(0x40 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type5)
    for (i in s.indices) {
        ret.append(0x50 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type6)
    for (i in s.indices) {
        ret.append(0x60 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type7)
    for (i in s.indices) {
        ret.append(0x70 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type8)
    for (i in s.indices) {
        ret.append(0x80 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type9)
    for (i in s.indices) {
        ret.append(0x90 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type10)
    for (i in s.indices) {
        ret.append(0xa0 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type11)
    for (i in s.indices) {
        ret.append(0xb0 + i, s[i])
    }
    val contentTypeText = ret.get(contentType, context.getString(R.string.no_data))
    view.isVisible = contentTypeText.isNotEmpty()
    view.text = contentTypeText
}

@BindingAdapter("priorityText")
fun setPriorityText(view: TextView, priority: Int) {
    val priorityNames = view.context.resources.getStringArray(R.array.dvr_priority_names)
    when (priority) {
        in 0..4 -> view.text = priorityNames[priority]
        6 -> view.text = priorityNames[5]
        else -> view.text = ""
    }
}

@BindingAdapter("dataSizeText", "dataSizeVisible")
fun setDataSizeText(view: TextView, recording: RecordingInterface?, visible: Boolean) {
    if (visible
            && recording != null
            && (!recording.isScheduled || recording.isScheduled && recording.isRecording)) {
        view.isVisible = true
        if (recording.dataSize > 1048576) {
            view.text = view.context.getString(R.string.data_size, recording.dataSize / 1048576, "MB")
        } else {
            view.text = view.context.getString(R.string.data_size, recording.dataSize / 1024, "KB")
        }
    } else {
        view.isVisible = false
    }
}

@BindingAdapter("dataErrorText", "dataErrorVisible")
fun setDataErrorText(view: TextView, recording: RecordingInterface?, visible: Boolean) {
    if (visible
            && recording != null
            && !recording.dataErrors.isNullOrEmpty()
            && (!recording.isScheduled || recording.isScheduled && recording.isRecording)) {
        view.isVisible = true
        view.text = view.context.getString(R.string.data_errors, recording.dataErrors ?: "0")
    } else {
        view.isVisible = false
    }
}

@BindingAdapter("subscriptionErrorText", "subscriptionErrorVisible")
fun setSubscriptionErrorText(view: TextView, recording: RecordingInterface?, visible: Boolean) {
    if (visible
            && recording != null
            && !recording.isScheduled
            && !recording.subscriptionError.isNullOrEmpty()) {
        view.isVisible = true
        view.text = view.context.getString(R.string.subscription_error, recording.subscriptionError)
    } else {
        view.isVisible = false
    }
}

@BindingAdapter("streamErrorText", "streamErrorVisible")
fun setStreamErrorText(view: TextView, recording: RecordingInterface?, visible: Boolean) {
    if (visible
            && recording != null
            && !recording.isScheduled
            && !recording.streamErrors.isNullOrEmpty()) {
        view.isVisible = true
        view.text = view.context.getString(R.string.stream_errors, recording.streamErrors)
    } else {
        view.isVisible = false
    }
}

@BindingAdapter("disabledText", "htspVersion")
fun setDisabledText(view: TextView, recording: RecordingInterface?, htspVersion: Int) {
    if (recording == null || !recording.isScheduled) {
        view.isVisible = false
    } else {
        setDisabledText(view, recording.isEnabled, htspVersion)
    }
}

@BindingAdapter("disabledText", "htspVersion")
fun setDisabledText(view: TextView, isEnabled: Boolean, htspVersion: Int) {
    view.isVisible = htspVersion >= 19 && !isEnabled
    view.setText(if (isEnabled) R.string.recording_enabled else R.string.recording_disabled)
}

@BindingAdapter("duplicateText", "htspVersion")
fun setDuplicateText(view: TextView, recording: RecordingInterface?, htspVersion: Int) {
    if (recording == null || !recording.isScheduled) {
        view.isVisible = false
    } else {
        view.isVisible = htspVersion >= 33 && recording.duplicate != 0
        view.setText(R.string.duplicate_recording)
    }
}

@BindingAdapter("failedReasonText")
fun setFailedReasonText(view: TextView, recording: RecordingInterface?) {
    val context = view.context
    var failedReasonText = ""

    if (recording != null) {
        when {
            recording.isAborted -> failedReasonText = context.resources.getString(R.string.recording_canceled)
            recording.isMissed -> failedReasonText = context.resources.getString(R.string.recording_time_missed)
            recording.isFailed -> failedReasonText = context.resources.getString(R.string.recording_file_invalid)
            recording.isFileMissing -> failedReasonText = context.resources.getString(R.string.recording_file_missing)
        }
    }

    view.isVisible = failedReasonText.isNotEmpty() && recording != null && !recording.isCompleted
    view.text = failedReasonText
}

@BindingAdapter("optionalColoredText")
fun setOptionalDescriptionText(view: TextView, text: String?) {
    view.isVisible = !text.isNullOrEmpty()
    if (text.isNullOrEmpty()) return

    if (text.contains("[COLOR ") && text.contains("[/COLOR]")) {
        val builder = SpannableStringBuilder()
        builder.append(text.substringBefore("[COLOR ", ""))

        val textArray = text.split("[COLOR").toTypedArray()
        textArray.forEach { str ->
            val colorName = str.substringBefore("]", "").trim()
            val coloredText = SpannableString(str.substringAfter("]", "").substringBefore("[/COLOR]", ""))
            val remainingText = str.substringAfter("[/COLOR]", "")

            val colorId = view.resources.getIdentifier(colorName, "color", view.context.packageName)
            if (colorId > 0) {
                coloredText.setSpan(ForegroundColorSpan(ContextCompat.getColor(view.context, colorId)), 0, coloredText.length, 0)
            }
            builder.append(coloredText)
            builder.append(remainingText)
        }
        view.setText(builder, TextView.BufferType.SPANNABLE)
    } else {
        view.text = text
    }
}

@BindingAdapter("optionalText")
fun setOptionalText(view: TextView, text: String?) {
    view.isVisible = !text.isNullOrEmpty()
    view.text = text
}

@BindingAdapter("stateIcon")
fun setStateIcon(view: ImageView, recording: RecordingInterface?) {
    var drawable: Drawable? = null
    if (recording != null) {
        when {
            recording.isFailed -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_error_small)
            recording.isCompleted -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_success_small)
            recording.isMissed -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_error_small)
            recording.isRecording -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_rec_small)
            recording.isScheduled -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_schedule_small)
        }
    }

    view.isVisible = drawable != null
    view.setImageDrawable(drawable)
}

@BindingAdapter("recordingStateText")
fun setRecordingStateText(view: TextView, recording: RecordingInterface?) {
    val stateTextResId = when {
        recording == null -> 0
        recording.isFailed -> R.string.recording_state_failed
        recording.isCompleted -> R.string.recording_state_completed
        recording.isMissed -> R.string.recording_state_missed
        recording.isRecording -> R.string.recording_state_recording
        recording.isScheduled -> R.string.recording_state_scheduled
        else -> 0
    }
    view.isVisible = stateTextResId != 0
    if (stateTextResId != 0) {
        view.setText(stateTextResId)
    }
}

@BindingAdapter("iconUrl", "iconVisibility")
fun setChannelIcon(view: ImageView, iconUrl: String?, visible: Boolean) {
    if (visible) {
        setChannelIcon(view, iconUrl)
    } else {
        view.isVisible = false
    }
}

/**
 * Loads the given program image via Glide into the image view
 *
 * @param view The view where the icon and visibility shall be applied to
 * @param url  The url of the channel icon
 */
@BindingAdapter("programImage", "programImageVisibility")
fun setProgramImage(view: ImageView, url: String?, visible: Boolean) {
    if (url.isNullOrEmpty() || !visible) {
        view.isVisible = false
    } else {
        Picasso.get()
                .load(url)
                .into(view, object : Callback {
                    override fun onSuccess() {
                        view.isVisible = true
                    }

                    override fun onError(e: Exception) {
                        Timber.d("Could not load image $url")
                        view.isVisible = false
                    }
                })
    }
}

/**
 * Loads the given channel icon via Glide into the image view
 *
 * @param view    The view where the icon and visibility shall be applied to
 * @param iconUrl The url of the channel icon
 */
@BindingAdapter("iconUrl")
fun setChannelIcon(view: ImageView, iconUrl: String?) {
    if (iconUrl.isNullOrEmpty()) {
        //Timber.d("Channel icon '$iconUrl' is empty or null, hiding icon")
        view.isVisible = false
    } else {
        val url = getIconUrl(view.context, iconUrl)
        //Timber.d("Channel icon '$iconUrl' is not empty, loading icon from url '$url'")

        Picasso.get().cancelRequest(view)
        Picasso.get()
                .load(url)
                .into(view, object : Callback {
                    override fun onSuccess() {
                        //Timber.d("Successfully loaded channel icon from url '$url'")
                        view.isVisible = true
                    }

                    override fun onError(e: Exception) {
                        //Timber.d("Error loading channel icon from url '$url'")
                        view.isVisible = false
                    }
                })
    }
}

@BindingAdapter("channelNumber")
fun setChannelNumber(view: TextView, number: String?) {
    if (!number.isNullOrEmpty()) {
        if (number.endsWith(".0")) {
            view.text = number.substringBefore(".")
        } else {
            view.text = number
        }
    }
}

@BindingAdapter("iconName", "iconUrl", "iconVisibility")
fun setChannelName(view: TextView, name: String?, iconUrl: String?, visible: Boolean) {
    if (visible) {
        setChannelName(view, name, iconUrl)
    } else {
        view.isVisible = false
    }
}

/**
 * Shows the channel name in the view if no channel icon exists.
 *
 * @param view    The view where the text and visibility shall be applied to
 * @param name    The name of the channel
 * @param iconUrl The url to the channel icon
 */
@BindingAdapter("iconName", "iconUrl")
fun setChannelName(view: TextView, name: String?, iconUrl: String?) {
    view.text = if (!name.isNullOrEmpty()) name else view.context.getString(R.string.all_channels)

    if (iconUrl.isNullOrEmpty()) {
        view.isVisible = true
    } else {
        val url = getIconUrl(view.context, iconUrl)
        Picasso.get()
                .load(url).fetch(object : Callback {
                    override fun onSuccess() {
                        view.isVisible = false
                    }

                    override fun onError(e: Exception) {
                        view.isVisible = true
                    }
                })
    }
}

/**
 * Set the correct indication when the dual pane mode is active If the item is selected
 * the the arrow will be shown, otherwise only a vertical separation line is displayed.
 *
 * @param view       The view where the theme and background image shall be applied to
 * @param isSelected Determines if the background image shall show a selected state or not
 */
@BindingAdapter("backgroundImage")
fun setDualPaneBackground(view: ImageView, isSelected: Boolean) {
    if (isSelected) {
        val icon = if (view.context.isInDarkMode()) R.drawable.dual_pane_selector_active_dark else R.drawable.dual_pane_selector_active_light // FIXME: via theme
        view.setBackgroundResource(icon)
    } else {
        val icon = R.drawable.dual_pane_selector_inactive
        view.setBackgroundResource(icon)
    }
}

/**
 * Converts the given number representing the days into a string with the
 * short names for the days. This string is assigned to the given view.
 *
 * @param view       The view where the short names for the days shall be shown
 * @param daysOfWeek The number representing the days of the week
 */
@BindingAdapter("daysText")
fun getDaysOfWeekText(view: TextView, daysOfWeek: Long) {
    val daysOfWeekList = view.context.resources.getStringArray(R.array.day_short_names)
    val text = StringBuilder()
    for (i in 0..6) {
        val s = if (daysOfWeek shr i and 1 == 1L) daysOfWeekList[i] else ""
        if (text.isNotEmpty() && s.isNotEmpty()) {
            text.append(", ")
        }
        text.append(s)
    }
    view.text = text.toString()
}

/**
 * Converts the given time in milliseconds into a default readable time
 * format, or if set by the preferences, into a localized time format
 *
 * @param view The view where the readable time shall be shown
 * @param time The time in milliseconds
 */
@BindingAdapter("timeText")
fun setLocalizedTime(view: TextView, time: Long) {
    if (time < 0) {
        view.text = view.context.getString(R.string.any)
        return
    }

    val localizedTime = if (view.context.prefs.localizedTimeFormat) {
        // Show the date as defined with the currently active locale.
        // For the date display the short version will be used
        val df = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, getLocale(view.context.resources))
        df.format(time)
    } else {
        // Show the date using the default format like 31.07.2013
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.format(time)
    }
    view.text = localizedTime
}

@BindingAdapter("dateText")
fun setLocalizedDate(view: TextView, date: Long) {
    if (date < 0) {
        view.text = view.context.getString(R.string.any)
        return
    }

    var localizedDate: String

    val dateDiff = date/ONE_DAY - System.currentTimeMillis()/ONE_DAY

    when (dateDiff.toInt()) {
        0 -> localizedDate = view.context.getString(R.string.today)
        1 -> localizedDate = view.context.getString(R.string.tomorrow)
        -1 -> localizedDate = view.context.getString(R.string.yesterday)
        -6, -5, -4, -3, -2, 2, 3, 4, 5, 6 -> {
            // show matching day of week
            val sdf = SimpleDateFormat("EEEE", getLocale(view.context.resources))
            localizedDate = sdf.format(date)
        }
        else -> {
            localizedDate = if (view.context.prefs.localizedTimeFormat) {
                // Show the date as defined with the currently active locale.
                // For the date display the short version will be used
                val df = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, getLocale(view.context.resources))
                df.format(date)
            } else {
                // Show the date using the default format like 31.07.2013
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US)
                sdf.format(date)
            }
        }
    }
    view.text = localizedDate
}

/**
 * Calculates the genre color from the given content type and sets it as the
 * background color of the given view
 *
 * @param view            The view that displays the genre color as a background
 * @param contentType     The content type to calculate the color from
 * @param showGenreColors True to show the color, false otherwise
 * @param offset          Positive offset from 0 to 100 to increase the transparency of the color
 */
@SuppressLint("ResourceAsColor")
@BindingAdapter("genreColor", "showGenreColor", "genreColorAlphaOffset", "genreColorItemName")
fun setGenreColor(view: TextView, contentType: Int, showGenreColors: Boolean, offset: Int, itemName: String?) {
    val context = view.context

    if (showGenreColors) {
        var color = ContextCompat.getColor(view.context, android.R.color.transparent)
        if (contentType >= 0) {
            // Get the genre color from the content type
            color = R.color.EPG_OTHER
            var type = contentType / 16 - 1
            type = if (type < 0) 0 else type

            Timber.d("Received content type $contentType ${if (itemName != null) " for $itemName" else ""}, final color id is ${contentType / 16}")
            when (type) {
                0 -> color = ContextCompat.getColor(view.context, R.color.EPG_MOVIES)
                1 -> color = ContextCompat.getColor(view.context, R.color.EPG_NEWS)
                2 -> color = ContextCompat.getColor(view.context, R.color.EPG_SHOWS)
                3 -> color = ContextCompat.getColor(view.context, R.color.EPG_SPORTS)
                4 -> color = ContextCompat.getColor(view.context, R.color.EPG_CHILD)
                5 -> color = ContextCompat.getColor(view.context, R.color.EPG_MUSIC)
                6 -> color = ContextCompat.getColor(view.context, R.color.EPG_ARTS)
                7 -> color = ContextCompat.getColor(view.context, R.color.EPG_SOCIAL)
                8 -> color = ContextCompat.getColor(view.context, R.color.EPG_SCIENCE)
                9 -> color = ContextCompat.getColor(view.context, R.color.EPG_HOBBY)
                10 -> color = ContextCompat.getColor(view.context, R.color.EPG_SPECIAL)
            }

            // Get the color with the desired alpha value
            var alpha = ((context.prefs.genreColorTransparencyPercent - offset).toFloat() / 100.0f * 255.0f).toInt()
            if (alpha < 0) {
                alpha = 0
            }
            color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }

        view.setBackgroundColor(color)
        view.isVisible = true
    } else {
        view.isInvisible = true
    }
}

@BindingAdapter("activeIcon")
fun setConnectionActiveIcon(view: ImageView, isActive: Boolean) {
    // Set the active / inactive icon depending on the selection status
    view.setImageResource(if (isActive) R.drawable.item_active else R.drawable.item_not_active)
}
