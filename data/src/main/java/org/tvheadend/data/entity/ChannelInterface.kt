package org.tvheadend.data.entity

interface ChannelInterface {
    var id: Int
    var number: Int
    var numberMinor: Int
    var name: String?
    var icon: String?
    var eventId: Int
    var nextEventId: Int
    var tags: List<Int>?
    var connectionId: Int
    var displayNumber: String?
    var serverOrder: Int
}