package org.tvheadend.data.entity

interface ChannelBaseInterface {
    var id: Int
    var name: String?
    var icon: String?
    var number: Int
    var numberMinor: Int
    var displayNumber: String?
}

interface ChannelInterface : ChannelBaseInterface {
    var eventId: Int
    var nextEventId: Int
    var tags: List<Int>?
    var connectionId: Int
    var serverOrder: Int
}