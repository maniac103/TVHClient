package org.tvheadend.data.entity

interface SeriesRecordingInterface {
    var id: String
    var isEnabled: Boolean
    var name: String?
    var minDuration: Int
    var maxDuration: Int
    var retention: Int
    var daysOfWeek: Int
    var priority: Int
    var approxTime: Int
    var start: Long
    var startWindow: Long
    var startExtra: Long
    var stopExtra: Long
    var title: String?
    var fulltext: Int
    var directory: String?
    var channelId: Int
    var owner: String?
    var creator: String?
    var dupDetect: Int
    var removal: Int
    var maxCount: Int
    var connectionId: Int

    val duration: Int
    val startTimeInMillis: Long
    val startWindowTimeInMillis: Long
}