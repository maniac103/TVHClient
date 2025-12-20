package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class ProgramWithChannel(
    @Embedded
    val base: Program,

    @ColumnInfo(name = "channel_name")
    var channelName: String? = null,
    @ColumnInfo(name = "channel_icon")
    var channelIcon: String? = null
) : ProgramInterface by base