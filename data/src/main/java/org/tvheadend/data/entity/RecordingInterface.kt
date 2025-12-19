package org.tvheadend.data.entity

interface RecordingInterface {
    var id: Int
    var channelId: Int
    var start: Long
    var stop: Long
    var startExtra: Long
    var stopExtra: Long
    var retention: Long
    var priority: Int
    var eventId: Int
    var autorecId: String?
    var timerecId: String?
    var contentType: Int
    var title: String?
    var subtitle: String?
    var summary: String?
    var description: String?
    var state: String?
    var error: String?
    var owner: String?
    var creator: String?
    var subscriptionError: String?
    var streamErrors: String?
    var dataErrors: String?
    var path: String?
    var dataSize: Long
    var isEnabled: Boolean
    var duplicate: Int
    var episode: String?
    var comment: String?
    var image: String?
    var fanartImage: String?
    var copyrightYear: Int
    var removal: Int
    var files: List<String>?
    var connectionId: Int
    var duration: Int

    val isCompleted: Boolean
    val isRecording: Boolean
    val isScheduled: Boolean
    val isFailed: Boolean
    val isMissed: Boolean
    val isAborted: Boolean
    val isFileMissing: Boolean
}