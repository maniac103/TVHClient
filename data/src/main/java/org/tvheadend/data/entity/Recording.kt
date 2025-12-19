package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import java.util.*

@Entity(tableName = "recordings", primaryKeys = ["id", "connection_id"])
data class Recording(

        override var id: Int = 0,                            // u32   required   ID of dvrEntry.
        @ColumnInfo(name = "channel_id")
        override var channelId: Int = 0,                     // u32   optional   Channel of dvrEntry.
        override var start: Long = Calendar.getInstance().timeInMillis,                     // s64   required   Time of when this entry was scheduled to start recording.
        override var stop: Long = Calendar.getInstance().timeInMillis + (30 * 60 * 1000).toLong(),             // s64   required   Time of when this entry was scheduled to stop recording.
        @ColumnInfo(name = "start_extra")
        override var startExtra: Long = 1,                   // s64   required   Extra start time (pre-time) in minutes (Added in version 13).
        @ColumnInfo(name = "stop_extra")
        override var stopExtra: Long = 15,                   // s64   required   Extra stop time (post-time) in minutes (Added in version 13).
        override var retention: Long = 0,                    // s64   required   DVR Entry retention time in days (Added in version 13).
        override var priority: Int = 2,                      // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set) (Added in version 13).
        @ColumnInfo(name = "event_id")
        override var eventId: Int = 0,                       // u32   optional   Associated EPG Event ID (Added in version 13).
        @ColumnInfo(name = "autorec_id")
        override var autorecId: String? = "",                 // str   optional   Associated Autorec UUID (Added in version 13).
        @ColumnInfo(name = "timerec_id")
        override var timerecId: String? = "",                 // str   optional   Associated Timerec UUID (Added in version 18).
        @ColumnInfo(name = "content_type")
        override var contentType: Int = 0,                   // u32   optional   Content Type (like in the DVB standard) (Added in version 13).
        override var title: String? = null,                  // str   optional   Title of recording
        override var subtitle: String? = null,               // str   optional   Subtitle of recording (Added in version 20).
        override var summary: String? = null,                // str   optional   Short description of the recording (Added in version 6).
        override var description: String? = null,            // str   optional   Long description of the recording.
        override var state: String? = null,                  // str   required   Recording state
        override var error: String? = null,                  // str   optional   Plain english error description (e.g. "Aborted by user").
        override var owner: String? = null,                  // str   optional   Name of the entry owner (Added in version 18).
        override var creator: String? = null,                // str   optional   Name of the entry creator (Added in version 18).
        @ColumnInfo(name = "subscription_error")
        override var subscriptionError: String? = null,      // str   optional   Subscription error string (Added in version 20).
        @ColumnInfo(name = "stream_errors")
        override var streamErrors: String? = null,           // str   optional   Number of recording errors (Added in version 20).
        @ColumnInfo(name = "data_errors")
        override var dataErrors: String? = null,             // str   optional   Number of stream data errors (Added in version 20).
        override var path: String? = null,                   // str   optional   Recording path for playback.
        @ColumnInfo(name = "data_size")
        override var dataSize: Long = 0,                     // s64   optional   Actual file size of the last recordings (Added in version 21).
        @ColumnInfo(name = "enabled")
        override var isEnabled: Boolean = true,              // u32   optional   Enabled flag (Added in version 23).
        override var duplicate: Int = 0,                     // u32   optional   Duplicate flag (Added in version 33).
        override var episode: String? = null,                // str   optional   Episode (Added in version 18).
        override var comment: String? = null,                // str   optional   Comment (Added in version 18).
        override var image: String? = null,                  // str   optional   Artwork for a recording
        @ColumnInfo(name = "fanart_image")
        override var fanartImage: String? = null,            // str   optional   Fanbased artwork for a recording (Added in version 33)
        @ColumnInfo(name = "copyright_year")
        override var copyrightYear: Int = 0,                 // str   optional   The copyright year (Added in version 33)
        override var removal: Int = 0,                       // u32   optional   Number of days to keep recorded files (Added in version 32)

        @Ignore
        override var files: List<String>? = null,            // msg   optional   All recorded files for playback (Added in version 21).

        @ColumnInfo(name = "connection_id")
        override var connectionId: Int = 0,

        override var duration: Int = 0
) : RecordingInterface {

    override val isCompleted: Boolean
        get() = error == null && state == "completed"

    override val isRecording: Boolean
        get() = error == null && state == "recording"

    override val isScheduled: Boolean
        get() = error == null && state == "scheduled"

    override val isFailed: Boolean
        get() = state == "invalid"

    override val isMissed: Boolean
        get() = state == "missed"

    override val isAborted: Boolean
        get() = error == "Aborted by user" && state == "completed"

    override val isFileMissing: Boolean
        get() = error == "File missing" && state == "completed"
}
