package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import java.util.*
import kotlin.math.floor

@Entity(tableName = "channels", primaryKeys = ["id", "connection_id"])
data class Channel(
    @ColumnInfo(name = "id")
    override var id: Int = 0,                        // u32   required   ID of channel.
    @ColumnInfo(name = "number")
    override var number: Int = 0,                    // u32   required   Channel number, 0 means not configured.
    @ColumnInfo(name = "number_minor")
    override var numberMinor: Int = 0,               // u32   optional   Minor channel number (Added in version 13).
    @ColumnInfo(name = "name")
    override var name: String? = null,               // str   required   Name of channel.
    @ColumnInfo(name = "icon")
    override var icon: String? = null,               // str   optional   URL to an icon representative for the channel
    @ColumnInfo(name = "event_id")
    override var eventId: Int = 0,                   // u32   optional   ID of the current event on this channel.
    @ColumnInfo(name = "next_event_id")
    override var nextEventId: Int = 0,               // u32   optional   ID of the next event on the channel.
    @Ignore
    override var tags: List<Int>? = null,           // u32[]  optional   Tags this channel is mapped to.

    @ColumnInfo(name = "connection_id")
    override var connectionId: Int = 0,
    @ColumnInfo(name = "display_number")
    override var displayNumber: String? = null,
    @ColumnInfo(name = "server_order")
    override var serverOrder: Int = 0,
) : ChannelInterface