package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Ignore

data class EpgProgram(
    @ColumnInfo(name = "id")
    override var eventId: Int = 0,
    @ColumnInfo(name = "channel_id")
    override var channelId: Int = 0,
    override var start: Long = 0,
    override var stop: Long = 0,
    override var title: String? = null,
    override var subtitle: String? = null,
    @ColumnInfo(name = "content_type")
    override var contentType: Int = 0,
    @ColumnInfo(name = "connection_id")
    var connectionId: Int = 0,
) : ProgramBaseInterface {
    val duration: Int
        get() = ((stop - start) / 1000 / 60).toInt()
}