package org.tvheadend.tvhclient.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.getLocale
import kotlin.enums.enumEntries

class Preferences(context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val res = context.applicationContext.resources

    val languageCode get() = prefs.getString("language", getLocale(res).language)!!.substring(0, 2)
    val startScreenMenuId = getStringAsLong("start_screen", R.string.pref_default_start_screen)

    val debugModeEnabled get() = getBool("debug_mode_enabled", R.bool.pref_default_debug_mode_enabled)
    val navigationHistoryEnabled get() = getBool("navigation_history_enabled", R.bool.pref_default_navigation_history_enabled)
    val internalPlayerForChannelsEnabled get() = getBool("internal_player_for_channels_enabled", R.bool.pref_default_internal_player_enabled)
    val internalPlayerForRecordingsEnabled get() = getBool("internal_player_for_recordings_enabled", R.bool.pref_default_internal_player_enabled)
    val deleteAllRecordingsMenuEnabled get() = getBool("delete_all_recordings_menu_enabled", R.bool.pref_default_delete_all_recordings_menu_enabled)

    val notificationsEnabled get() = getBool("notifications_enabled", R.bool.pref_default_notifications_enabled)
    val notificationLeadTimeMinutes get() = getStringAsInt("notification_lead_time", R.string.pref_default_notification_lead_time)
    val notificationRunningRecordingCountEnabled get() = getBool("notify_running_recording_count_enabled", R.bool.pref_default_notify_running_recording_count_enabled)
    val notificationLowStorageEnabled get() = getBool("notify_low_storage_space_enabled", R.bool.pref_default_notify_low_storage_space_enabled)
    val notificationLowStorageThresholdGb get() = getStringAsInt("low_storage_space_threshold", R.string.pref_default_low_storage_space_threshold)

    val showRecordingFileStatus get() = getBool("show_recording_file_status_enabled", R.bool.pref_default_show_recording_file_status_enabled)
    val localizedTimeFormat get() = getBool("localized_date_time_format_enabled", R.bool.pref_default_localized_date_time_format_enabled)

    val multiChannelTagsEnabled get() = getBool("multiple_channel_tags_enabled", R.bool.pref_default_multiple_channel_tags_enabled)
    val channelTagMenuEnabled get() = getBool("channel_tag_menu_enabled", R.bool.pref_default_channel_tag_menu_enabled)
    val channelTagIconsEnabled get() = getBool("channel_tag_icons_enabled", R.bool.pref_default_channel_tag_icons_enabled)
    val showAllChannelTagsEnabled get() = getBool("empty_channel_tags_enabled", R.bool.pref_default_empty_channel_tags_enabled)

    val genreColorsForChannelsEnabled get() = getBool("genre_colors_for_channels_enabled", R.bool.pref_default_genre_colors_for_channels_enabled)
    val genreColorsForProgramsEnabled get() = getBool("genre_colors_for_programs_enabled", R.bool.pref_default_genre_colors_for_programs_enabled)
    val genreColorsForProgramGuideEnabled get() = getBool("genre_colors_for_program_guide_enabled", R.bool.pref_default_genre_colors_for_program_guide_enabled)
    val genreColorsForRecordingsEnabled get() = getBool("genre_colors_for_recordings_enabled", R.bool.pref_default_genre_colors_for_recordings_enabled)
    val genreColorTransparencyPercent get() = prefs.getInt("genre_color_transparency", res.getString(R.string.pref_default_genre_color_transparency).toInt())

    val channelIconAction get() = getStringAsEnum<IconAction>("channel_icon_action", R.string.pref_default_channel_icon_action)
    val channelSortOrder get() = getStringAsEnum<ChannelSortOrder>("channel_sort_order", R.string.pref_default_channel_sort_order)
    val completedRecordingSortOrder get() = getStringAsEnum<ChannelSortOrder>("completed_recording_sort_order", R.string.pref_default_completed_recording_sort_order)

    val showChannelNumbersEnabled get() = getBool("channel_number_enabled", R.bool.pref_default_channel_number_enabled)
    val showChannelNameEnabled get() = getBool("channel_name_enabled", R.bool.pref_default_channel_name_enabled)
    val showProgramProgressEnabled get() = getBool("program_progressbar_enabled", R.bool.pref_default_program_progressbar_enabled)
    val showProgramSubtitleEnabled get() = getBool("program_subtitle_enabled", R.bool.pref_default_program_subtitle_enabled)
    val showProgramArtworkEnabled get() = getBool("program_artwork_enabled", R.bool.pref_default_program_artwork_enabled)

    val timeshiftEnabled get() = getBool("timeshift_enabled", R.bool.pref_default_timeshift_enabled)
    val hideDuplicateScheduledRecordings get() = getBool("hide_duplicate_scheduled_recordings_enabled", R.bool.pref_default_hide_duplicate_scheduled_recordings_enabled)

    val connectionTimeoutMs get() = getStringAsInt("connection_timeout", R.string.pref_default_connection_timeout) * 1000
    val maxEpgTimeSeconds get() = getStringAsLong("epg_max_time", R.string.pref_default_epg_max_time)
    val epgDaysToShow get() = getStringAsInt("days_of_epg_data", R.string.pref_default_days_of_epg_data)
    val epgHoursPerScreen get() = getStringAsInt("hours_of_epg_data_per_screen", R.string.pref_default_hours_of_epg_data_per_screen)

    val audioTunnelingEnabled get() = getBool("audio_tunneling_enabled", R.bool.pref_default_audio_tunneling_enabled)
    val forceAspectRatioForSdContent get() = getBool("force_aspect_ratio_for_sd_content_enabled", R.bool.pref_default_force_aspect_ratio_for_sd_content_enabled)
    val bufferPlaybackMs get() = getStringAsInt("buffer_playback_ms", R.string.pref_default_buffer_playback_ms)

    fun channelSortOrderLiveData() = EnumLiveData(prefs, res, "channel_sort_order", R.string.pref_default_channel_icon_action, ChannelSortOrder.entries)
    fun completedRecordingSortOrderLiveData() = EnumLiveData(prefs, res, "completed_recording_sort_order", R.string.pref_default_completed_recording_sort_order, ChannelSortOrder.entries)
    fun genreColorsForChannelsLiveData() = BooleanLiveData(prefs, res, "genre_colors_for_channels_enabled", R.bool.pref_default_genre_colors_for_channels_enabled)
    fun genreColorsForProgramsLiveData() = BooleanLiveData(prefs, res, "genre_colors_for_programs_enabled", R.bool.pref_default_genre_colors_for_programs_enabled)
    fun genreColorsForProgramGuideLiveData() = BooleanLiveData(prefs, res, "genre_colors_for_program_guide_enabled", R.bool.pref_default_genre_colors_for_program_guide_enabled)
    fun genreColorsForRecordingsLiveData() = BooleanLiveData(prefs, res, "genre_colors_for_recordings_enabled", R.bool.pref_default_genre_colors_for_recordings_enabled)
    fun showNextProgramTitleLiveData() = BooleanLiveData(prefs, res, "next_program_title_enabled", R.bool.pref_default_next_program_title_enabled)
    fun showProgramSubtitleLiveData() = BooleanLiveData(prefs, res, "program_subtitle_enabled", R.bool.pref_default_program_subtitle_enabled)
    fun showProgramProgressLiveData() = BooleanLiveData(prefs, res, "program_progressbar_enabled", R.bool.pref_default_program_progressbar_enabled)
    fun showChannelNameLiveData() = BooleanLiveData(prefs, res, "channel_name_enabled", R.bool.pref_default_channel_name_enabled)
    fun showChannelNumbersLiveData() = BooleanLiveData(prefs, res, "channel_number_enabled", R.bool.pref_default_channel_number_enabled)
    fun showProgramArtworkLiveData() = BooleanLiveData(prefs, res, "program_artwork_enabled", R.bool.pref_default_program_artwork_enabled)
    fun showAllChannelTagsLiveData() = BooleanLiveData(prefs, res, "empty_channel_tags_enabled", R.bool.pref_default_empty_channel_tags_enabled)
    fun epgDaysToShowLiveData() = IntLiveData(prefs, res, "days_of_epg_data", R.string.pref_default_days_of_epg_data)
    fun epgHoursPerScreenLiveData() = IntLiveData(prefs, res, "hours_of_epg_data_per_screen", R.string.pref_default_hours_of_epg_data_per_screen)
    fun hideDuplicateScheduledRecordingsLiveData() = BooleanLiveData(prefs, res, "hide_duplicate_scheduled_recordings_enabled", R.bool.pref_default_hide_duplicate_scheduled_recordings_enabled)
    fun notificationRunningRecordingCountLiveData() = BooleanLiveData(prefs, res, "notify_running_recording_count_enabled", R.bool.pref_default_notify_running_recording_count_enabled)
    fun notificationLowStorageEnabledLiveData() = BooleanLiveData(prefs, res, "notify_low_storage_space_enabled", R.bool.pref_default_notify_low_storage_space_enabled)
    fun notificationLowStorageThresholdGbLiveData() = IntLiveData(prefs, res, "low_storage_space_threshold", R.string.pref_default_low_storage_space_threshold)

    private fun getBool(key: String, defaultResId: Int) = prefs.getBoolean(key, res.getBoolean(defaultResId))
    private fun getStringAsInt(key: String, defaultResId: Int) = prefs.getString(key, res.getString(defaultResId))!!.toInt()
    private fun getStringAsLong(key: String, defaultResId: Int) = prefs.getString(key, res.getString(defaultResId))!!.toLong()
    private inline fun<reified T : Enum<T>> getStringAsEnum(key: String, defaultResId: Int) = getStringAsInt(key, defaultResId).let { enumEntries<T>()[it] }

    enum class IconAction {
        DoNothing,
        Play,
        CastOrPlay
    }

    enum class ChannelSortOrder {
        ServerAsc,
        ServerDesc,
        ChannelIdAsc,
        ChannelIdDesc,
        ChannelNameAsc,
        ChannelNameDesc,
        ChannelNumberAsc,
        ChannelNumberDesc
    }

    abstract class PreferenceLiveData<PT, OT>(val prefs: SharedPreferences,
                                         val key: String,
                                         val defaultValue: PT) : LiveData<OT>(), SharedPreferences.OnSharedPreferenceChangeListener {
        abstract fun getValueFromPreferences(key: String, defaultValue: PT): OT

        override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
            if (key == this.key) {
                value = getValueFromPreferences(key, defaultValue)
            }
        }

        override fun onActive() {
            super.onActive()
            value = getValueFromPreferences(key, defaultValue)
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onInactive() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            super.onInactive()
        }
    }

    class BooleanLiveData(prefs: SharedPreferences, res: Resources, key: String, defaultValueResId: Int) :
        PreferenceLiveData<Boolean, Boolean>(prefs, key, res.getBoolean(defaultValueResId)) {
        override fun getValueFromPreferences(key: String, defaultValue: Boolean) = prefs.getBoolean(key, defaultValue)
    }

    class IntLiveData(prefs: SharedPreferences, res: Resources, key: String, defaultValueResId: Int) :
        PreferenceLiveData<String, Int>(prefs, key, res.getString(defaultValueResId)) {
        override fun getValueFromPreferences(key: String, defaultValue: String) = prefs.getString(key, defaultValue)!!.toInt()
    }

    class EnumLiveData<T : Enum<T>>(prefs: SharedPreferences, res: Resources, key: String, defaultValueResId: Int, private val entries: List<T>) :
        PreferenceLiveData<String, T>(prefs, key, res.getString(defaultValueResId)) {
        override fun getValueFromPreferences(key: String, defaultValue: String) =
            prefs.getString(key, defaultValue)!!.toInt().let { entries[it] }
    }
}