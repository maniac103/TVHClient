package org.tvheadend.data.entity

interface TimerRecordingInterface {
    var id: String
    var title: String?
    var directory: String?
    var isEnabled: Boolean
    var name: String?
    var configName: String?
    var channelId: Int
    var daysOfWeek: Int
    var priority: Int
    var start: Long
    var stop: Long
    var retention: Int
    var owner: String?
    var creator: String?
    var removal: Int
    var connectionId: Int

    val duration: Int
    val startTimeInMillis: Long
    val stopTimeInMillis: Long
}