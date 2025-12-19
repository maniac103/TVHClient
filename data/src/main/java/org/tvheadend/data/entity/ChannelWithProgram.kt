package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Ignore
import java.util.Date
import kotlin.math.floor

data class ChannelWithProgram(
    @Embedded
    val base: Channel,

    @ColumnInfo(name = "program_id")
    var programId: Int = 0,
    @ColumnInfo(name = "program_title")
    var programTitle: String? = null,
    @ColumnInfo(name = "program_subtitle")
    var programSubtitle: String? = null,
    @ColumnInfo(name = "program_start")
    var programStart: Long = 0,
    @ColumnInfo(name = "program_stop")
    var programStop: Long = 0,
    @ColumnInfo(name = "program_content_type")
    var programContentType: Int = 0,
    @ColumnInfo(name = "next_program_id")
    var nextProgramId: Int = 0,
    @ColumnInfo(name = "next_program_title")
    var nextProgramTitle: String? = null,
) : ChannelInterface by base {
    val duration: Int
        get() = ((programStop - programStart) / 1000 / 60).toInt()

    val progress: Int
        get() {
            var percentage = 0.0
            // Get the start and end times to calculate the progress.
            val durationTime = (programStop - programStart).toDouble()
            val elapsedTime = (Date().time - programStart).toDouble()
            // Show the progress as a percentage
            if (durationTime > 0) {
                percentage = elapsedTime / durationTime
            }
            return floor(percentage * 100).toInt()
        }
}
