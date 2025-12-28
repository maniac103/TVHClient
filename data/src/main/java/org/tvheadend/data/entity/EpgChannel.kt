package org.tvheadend.data.entity

import androidx.room.ColumnInfo

data class EpgChannel(
    @ColumnInfo(name = "id")
    override var id: Int = 0,
    @ColumnInfo(name = "name")
    override var name: String? = null,
    @ColumnInfo(name = "icon")
    override var icon: String? = null,
    @ColumnInfo(name = "number")
    override var number: Int = 0,
    @ColumnInfo(name = "number_minor")
    override var numberMinor: Int = 0,
    @ColumnInfo(name = "display_number")
    override var displayNumber: String? = null
) : ChannelBaseInterface