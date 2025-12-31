package org.tvheadend.tvhclient.service.htsp

import android.content.Intent
import org.tvheadend.data.entity.*
import org.tvheadend.htsp.HtspMessage
import timber.log.Timber
import java.util.Date
import java.util.TimeZone

private fun HtspMessage.getNonEmptyString(key: String, fallback: String?): String? =
    getString(key)?.takeIf { it.isNotEmpty() } ?: fallback
private fun HtspMessage.getTrimmedNonEmptyString(key: String, fallback: String?): String? =
    getString(key)?.takeIf { it.isNotEmpty() }?.trim() ?: fallback
private fun HtspMessage.getPositiveInteger(key: String, fallback: Int): Int =
    getInteger(key, 0).takeIf { it > 0 } ?: fallback
private fun HtspMessage.getPositiveLong(key: String, fallback: Long): Long =
    getLong(key, 0L).takeIf { it > 0 } ?: fallback

fun convertMessageToChannelTagModel(tag: ChannelTag, msg: HtspMessage, channels: List<Channel>): ChannelTag {
    val members = if (msg.containsKey("members")) msg.getIntegerList("members") else tag.members
    return ChannelTag(
        tagId = msg.getInteger("tagId", tag.tagId),
        tagName = msg.getString("tagName", tag.tagName),
        tagIndex = msg.getPositiveInteger("tagIndex", tag.tagIndex),
        tagIcon = msg.getNonEmptyString("tagIcon", tag.tagIcon),
        tagTitledIcon = msg.getPositiveInteger("tagTitledIcon", tag.tagTitledIcon),
        members = if (msg.containsKey("members")) msg.getIntegerList("members") else tag.members,
        connectionId = tag.connectionId,
        isSelected = tag.isSelected,
        channelCount = members?.filter { id -> channels.any { it.id == id } }?.size ?: 0,
    )
}

fun convertMessageToChannelModel(channel: Channel, msg: HtspMessage): Channel {
    val number = msg.getInteger("channelNumber", channel.number)
    val numberMinor = if (msg.containsKey("channelNumber")) msg.getInteger("channelNumberMinor", 0) else channel.numberMinor

    return Channel(
        id = msg.getInteger("channelId", channel.id),
        number = number,
        numberMinor = numberMinor,
        name = msg.getString("channelName", channel.name),
        icon = msg.getNonEmptyString("channelIcon", channel.icon),
        eventId = msg.getPositiveInteger("eventId", channel.eventId),
        nextEventId = msg.getPositiveInteger("nextEventId", channel.nextEventId),
        tags = if (msg.containsKey("tags")) msg.getIntegerList("tags") else channel.tags,
        connectionId = channel.connectionId,
        displayNumber = "$number.$numberMinor",
        serverOrder = channel.serverOrder
    )
}

fun convertMessageToRecordingModel(recording: Recording, msg: HtspMessage): Recording {
    val start = msg.getLong("start", recording.start / 1000)
    val stop = msg.getLong("stop", recording.stop / 1000)
    return Recording(
        id = msg.getInteger("id", recording.id),
        channelId = msg.getPositiveInteger("channel", recording.channelId),
        start = start * 1000, // The message value is in seconds, convert to milliseconds
        stop = stop * 1000, // The message value is in seconds, convert to milliseconds
        startExtra = msg.getLong("startExtra", recording.startExtra),
        stopExtra = msg.getLong("stopExtra", recording.stopExtra),
        retention = msg.getLong("retention", recording.retention),
        priority = msg.getInteger("priority", recording.priority),
        eventId = msg.getPositiveInteger("eventId", recording.eventId),
        autorecId = msg.getNonEmptyString("autorecId", recording.autorecId),
        timerecId = msg.getNonEmptyString("timerecId", recording.timerecId),
        contentType = msg.getPositiveInteger("contentType", recording.contentType),
        title = msg.getTrimmedNonEmptyString("title", recording.title),
        subtitle = msg.getTrimmedNonEmptyString("subtitle", recording.subtitle),
        summary = msg.getTrimmedNonEmptyString("summary", recording.summary),
        description = msg.getTrimmedNonEmptyString("description", recording.description),
        state = msg.getString("state", recording.state),
        error = msg.getNonEmptyString("error", recording.error),
        owner = msg.getNonEmptyString("owner", recording.owner),
        creator = msg.getNonEmptyString("creator", recording.creator),
        subscriptionError = msg.getNonEmptyString("subscriptionError", recording.subscriptionError),
        streamErrors = msg.getNonEmptyString("streamErrors", recording.streamErrors),
        dataErrors = msg.getNonEmptyString("dataErrors", recording.dataErrors),
        path = msg.getNonEmptyString("path", recording.path),
        dataSize = msg.getPositiveLong("dataSize", recording.dataSize),
        isEnabled = msg.getInteger("enabled", if (recording.isEnabled) 1 else 0) == 1,
        duplicate = msg.getInteger("duplicate", recording.duplicate),
        episode = recording.episode, // FIXME: doesn't seem to actually exist?
        comment = msg.getString("comment", recording.comment),
        image = msg.getNonEmptyString("image", recording.image),
        fanartImage = msg.getNonEmptyString("fanart_image", recording.fanartImage),
        copyrightYear = msg.getPositiveInteger("copyright_year", recording.copyrightYear),
        removal = msg.getPositiveInteger("removal", recording.removal),
        files = recording.files, // TODO
        connectionId = recording.connectionId,
        duration = ((stop - start) / 60).toInt()
    )
}

fun convertMessageToProgramModel(program: Program, msg: HtspMessage) = Program(
    eventId = msg.getInteger("eventId", program.eventId),
    channelId = msg.getInteger("channelId", program.channelId),
    // The message value is in seconds, convert to milliseconds
    start = msg.getLong("start", program.start / 1000) * 1000,
    // The message value is in seconds, convert to milliseconds
    stop = msg.getLong("stop", program.stop / 1000) * 1000,
    title = msg.getTrimmedNonEmptyString("title", program.title),
    subtitle = msg.getTrimmedNonEmptyString("subtitle", program.subtitle),
    summary = msg.getTrimmedNonEmptyString("summary", program.summary),
    description = msg.getTrimmedNonEmptyString("description", program.description),
    credits = if (msg.containsKey("credits")) msg.getArrayList("credits").joinToString(",") else program.credits,
    category = if (msg.containsKey("category")) msg.getArrayList("category").joinToString (",") else program.category,
    keyword = if (msg.containsKey("keyword")) msg.getArrayList("keyword").joinToString (",") else program.keyword,
    serieslinkId = msg.getPositiveInteger("seriesLinkId", program.serieslinkId),
    episodeId = msg.getPositiveInteger("episodeId", program.episodeId),
    seasonId = msg.getPositiveInteger("seasonId", program.seasonId),
    brandId = msg.getPositiveInteger("brandId", program.brandId),
    contentType = msg.getPositiveInteger("contentType", program.contentType),
    ageRating = msg.getPositiveInteger("ageRating", program.ageRating),
    starRating = msg.getPositiveInteger("starRating", program.starRating),
    copyrightYear = msg.getPositiveInteger("copyright_year", program.copyrightYear),
    firstAired = msg.getPositiveLong("firstAired", program.firstAired),
    seasonNumber = msg.getPositiveInteger("seasonNumber", program.seasonNumber),
    seasonCount = msg.getPositiveInteger("seasonCount", program.seasonCount),
    episodeNumber = msg.getPositiveInteger("episodeNumber", program.episodeNumber),
    episodeCount = msg.getPositiveInteger("episodeCount", program.episodeCount),
    partNumber = msg.getPositiveInteger("partNumber", program.partNumber),
    partCount = msg.getPositiveInteger("partCount", program.partCount),
    episodeOnscreen = msg.getTrimmedNonEmptyString("episodeOnscreen", program.episodeOnscreen),
    image = msg.getNonEmptyString("image", program.image),
    dvrId = msg.getPositiveInteger("dvrId", program.dvrId),
    nextEventId = msg.getPositiveInteger("nextEventId", program.nextEventId),
    serieslinkUri = msg.getNonEmptyString("serieslinkUri", program.serieslinkUri),
    episodeUri = msg.getNonEmptyString("episodeUri", program.episodeUri),
    modifiedTime = System.currentTimeMillis(),
    connectionId = program.connectionId
)

fun convertMessageToSeriesRecordingModel(seriesRecording: SeriesRecording, msg: HtspMessage) = SeriesRecording(
    id = msg.getString("id", seriesRecording.id),
    isEnabled = msg.getInteger("enabled", if (seriesRecording.isEnabled) 1 else 0) == 1,
    name = msg.getString("name", seriesRecording.name),
    minDuration = msg.getInteger("minDuration", seriesRecording.minDuration),
    maxDuration = msg.getInteger("maxDuration", seriesRecording.maxDuration),
    retention = msg.getInteger("retention", seriesRecording.retention),
    daysOfWeek = msg.getInteger("daysOfWeek", seriesRecording.daysOfWeek),
    priority = msg.getInteger("priority", seriesRecording.priority),
    approxTime = msg.getInteger("approxTime", seriesRecording.approxTime),
    start = msg.getLong("start", seriesRecording.start),
    startWindow = msg.getLong("startWindow", seriesRecording.startWindow),
    startExtra = msg.getLong("startExtra", seriesRecording.startExtra),
    stopExtra = msg.getLong("stopExtra", seriesRecording.stopExtra),
    title = msg.getTrimmedNonEmptyString("title", seriesRecording.title),
    fulltext = msg.getInteger("fulltext", seriesRecording.fulltext),
    directory = msg.getNonEmptyString("directory", seriesRecording.directory),
    channelId = msg.getPositiveInteger("channel", seriesRecording.channelId),
    owner = msg.getNonEmptyString("owner", seriesRecording.owner),
    creator = msg.getNonEmptyString("creator", seriesRecording.creator),
    dupDetect = msg.getPositiveInteger("dupDetect", seriesRecording.dupDetect),
    removal = msg.getPositiveInteger("removal", seriesRecording.removal),
    maxCount = msg.getPositiveInteger("maxCount", seriesRecording.maxCount),
    connectionId = seriesRecording.connectionId
)

fun convertMessageToTimerRecordingModel(timerRecording: TimerRecording, msg: HtspMessage) = TimerRecording(
    id = msg.getString("id", timerRecording.id),
    title = msg.getString("title", timerRecording.title)?.trim(),
    directory = msg.getNonEmptyString("directory", timerRecording.directory),
    isEnabled = msg.getInteger("enabled", if (timerRecording.isEnabled) 1 else 0) == 1,
    name = msg.getString("name", timerRecording.name)?.trim(),
    configName = msg.getString("configName", timerRecording.configName),
    channelId = msg.getInteger("channel", timerRecording.channelId),
    daysOfWeek = msg.getInteger("daysOfWeek", timerRecording.daysOfWeek),
    priority = msg.getInteger("priority", timerRecording.priority),
    start = msg.getLong("start", timerRecording.start),
    stop = msg.getLong("stop", timerRecording.stop),
    retention = msg.getInteger("retention", timerRecording.retention),
    owner = msg.getNonEmptyString("owner", timerRecording.owner),
    creator = msg.getNonEmptyString("creator", timerRecording.creator),
    removal = msg.getPositiveInteger("removal", timerRecording.removal),
    connectionId = timerRecording.connectionId
)

fun convertMessageToServerStatusModel(serverStatus: ServerStatus, msg: HtspMessage) = serverStatus.copy(
    htspVersion = msg.getInteger("htspversion", serverStatus.htspVersion),
    serverName = msg.getString("servername", serverStatus.serverName),
    serverVersion = msg.getString("serverversion", serverStatus.serverVersion),
    webroot = msg.getString("webroot", serverStatus.webroot) ?: "",
)

fun convertIntentToAutorecMessage(intent: Intent, htspVersion: Int): HtspMessage {
    val enabled = intent.getIntExtra("enabled", 1).toLong()
    val title = intent.getStringExtra("title")
    val fulltext = intent.getStringExtra("fulltext")
    val directory = intent.getStringExtra("directory")
    val name = intent.getStringExtra("name")
    val configName = intent.getStringExtra("configName")
    val channelId = intent.getIntExtra("channelId", 0).toLong()
    val minDuration = intent.getIntExtra("minDuration", 0).toLong()
    val maxDuration = intent.getIntExtra("maxDuration", 0).toLong()
    val daysOfWeek = intent.getIntExtra("daysOfWeek", 127).toLong()
    val priority = intent.getIntExtra("priority", 2).toLong()
    val start = intent.getLongExtra("start", -1)
    val startWindow = intent.getLongExtra("startWindow", -1)
    val startExtra = intent.getLongExtra("startExtra", 0)
    val stopExtra = intent.getLongExtra("stopExtra", 0)
    val dupDetect = intent.getIntExtra("dupDetect", 0).toLong()
    val comment = intent.getStringExtra("comment")

    val request = HtspMessage()
    if (htspVersion >= 19) {
        request["enabled"] = enabled
    }
    request["title"] = title
    if (fulltext != null && htspVersion >= 20) {
        request["fulltext"] = fulltext
    }
    if (directory != null) {
        request["directory"] = directory
    }
    if (name != null) {
        request["name"] = name
    }
    if (configName != null) {
        request["configName"] = configName
    }
    // Don't add the channel id if none was given.
    // Assume the user wants to record on all channels
    if (channelId > 0) {
        request["channelId"] = channelId
    }
    // Minimal duration in seconds (0 = Any)
    request["minDuration"] = minDuration
    // Maximal duration in seconds (0 = Any)
    request["maxDuration"] = maxDuration
    request["daysOfWeek"] = daysOfWeek
    request["priority"] = priority

    // Minutes from midnight (up to 24*60) (window +- 15 minutes) (Obsoleted from version 18)
    // Do not send the value if the default of -1 (no time specified) was set
    if (start >= 0 && htspVersion < 18) {
        request["approxTime"] = start
    }
    // Minutes from midnight (up to 24*60) for the start of the time window.
    // Do not send the value if the default of -1 (no time specified) was set
    if (start >= 0 && htspVersion >= 18) {
        request["start"] = start
    }
    // Minutes from midnight (up to 24*60) for the end of the time window (including, cross-noon allowed).
    // Do not send the value if the default of -1 (no time specified) was set
    if (startWindow >= 0 && htspVersion >= 18) {
        request["startWindow"] = startWindow
    }
    request["startExtra"] = startExtra
    request["stopExtra"] = stopExtra

    if (htspVersion >= 20) {
        request["dupDetect"] = dupDetect
    }
    if (comment != null) {
        request["comment"] = comment
    }
    return request
}

fun convertIntentToDvrMessage(intent: Intent, htspVersion: Int): HtspMessage {
    val eventId = intent.getIntExtra("eventId", 0).toLong()
    val channelId = intent.getIntExtra("channelId", 0).toLong()
    val start = intent.getLongExtra("start", 0)
    val stop = intent.getLongExtra("stop", 0)
    val retention = intent.getLongExtra("retention", 0)
    val priority = intent.getIntExtra("priority", 2).toLong()
    val startExtra = intent.getLongExtra("startExtra", 0)
    val stopExtra = intent.getLongExtra("stopExtra", 0)
    val title = intent.getStringExtra("title")
    val subtitle = intent.getStringExtra("subtitle")
    val description = intent.getStringExtra("description")
    val configName = intent.getStringExtra("configName")
    val enabled = intent.getIntExtra("enabled", 1).toLong()
    // Controls that certain fields will only be added when the recording
    // is only scheduled and not being recorded
    val isRecording = intent.getBooleanExtra("isRecording", false)

    val request = HtspMessage()
    // If the eventId is set then an existing program from the program guide
    // shall be recorded. The server will then ignore the other fields
    // automatically.
    if (eventId > 0) {
        request["eventId"] = eventId
    }
    if (channelId > 0 && htspVersion >= 22) {
        request["channelId"] = channelId
    }
    if (!isRecording && start > 0) {
        request["start"] = start
    }
    if (stop > 0) {
        request["stop"] = stop
    }
    if (!isRecording && retention > 0) {
        request["retention"] = retention
    }
    if (!isRecording && priority > 0) {
        request["priority"] = priority
    }
    if (!isRecording && startExtra > 0) {
        request["startExtra"] = startExtra
    }
    if (stopExtra > 0) {
        request["stopExtra"] = stopExtra
    }
    // Only add the text fields if no event id was given
    if (eventId == 0L) {
        if (title != null) {
            request["title"] = title
        }
        if (subtitle != null && htspVersion >= 21) {
            request["subtitle"] = subtitle
        }
        if (description != null) {
            request["description"] = description
        }
    }
    if (configName != null) {
        request["configName"] = configName
    }
    if (htspVersion >= 23) {
        request["enabled"] = enabled
    }
    return request
}

fun convertIntentToTimerecMessage(intent: Intent, htspVersion: Int): HtspMessage {
    val enabled = intent.getIntExtra("enabled", 1).toLong()
    val title = intent.getStringExtra("title")
    val directory = intent.getStringExtra("directory")
    val name = intent.getStringExtra("name")
    val configName = intent.getStringExtra("configName")
    val channelId = intent.getIntExtra("channelId", 0).toLong()
    val daysOfWeek = intent.getIntExtra("daysOfWeek", 0).toLong()
    val priority = intent.getIntExtra("priority", 2).toLong()
    val start = intent.getLongExtra("start", -1)
    val stop = intent.getLongExtra("stop", -1)
    val retention = intent.getIntExtra("retention", -1).toLong()
    val comment = intent.getStringExtra("comment")

    val request = HtspMessage()
    if (htspVersion >= 19) {
        request["enabled"] = enabled
    }
    request["title"] = title
    if (directory != null) {
        request["directory"] = directory
    }
    if (name != null) {
        request["name"] = name
    }
    if (configName != null) {
        request["configName"] = configName
    }
    if (channelId > 0) {
        request["channelId"] = channelId
    }
    request["daysOfWeek"] = daysOfWeek
    request["priority"] = priority

    if (start >= 0) {
        request["start"] = start
    }
    if (stop >= 0) {
        request["stop"] = stop
    }
    if (retention > 0) {
        request["retention"] = retention
    }
    if (comment != null) {
        request["comment"] = comment
    }
    return request
}

fun convertIntentToEventMessage(intent: Intent): HtspMessage {
    val eventId = intent.getIntExtra("eventId", 0)
    val channelId = intent.getIntExtra("channelId", 0)
    val numFollowing = intent.getIntExtra("numFollowing", 0)
    val maxTime = intent.getLongExtra("maxTime", 0)

    val request = HtspMessage()
    request["method"] = "getEvents"
    if (eventId > 0) {
        request["eventId"] = eventId
    }
    if (channelId > 0) {
        request["channelId"] = channelId
    }
    if (numFollowing > 0) {
        request["numFollowing"] = numFollowing
    }
    if (maxTime > 0) {
        request["maxTime"] = maxTime
    }
    return request
}

fun convertIntentToEpgQueryMessage(intent: Intent): HtspMessage {
    val query = intent.getStringExtra("query")
    val channelId = intent.getIntExtra("channelId", 0).toLong()
    val tagId = intent.getIntExtra("tagId", 0).toLong()
    val contentType = intent.getIntExtra("contentType", 0)
    val minDuration = intent.getIntExtra("minduration", 0)
    val maxDuration = intent.getIntExtra("maxduration", 0)
    val language = intent.getStringExtra("language")
    val full = intent.getBooleanExtra("full", false)

    val request = HtspMessage()
    request["method"] = "epgQuery"
    request["query"] = query

    if (channelId > 0) {
        request["channelId"] = channelId
    }
    if (tagId > 0) {
        request["tagId"] = tagId
    }
    if (contentType > 0) {
        request["contentType"] = contentType
    }
    if (minDuration > 0) {
        request["minDuration"] = minDuration
    }
    if (maxDuration > 0) {
        request["maxDuration"] = maxDuration
    }
    if (language != null) {
        request["language"] = language
    }
    request["full"] = full
    return request
}

// Current timezone and date
val daylightSavingOffset: Int
    get() {
        val timeZone = TimeZone.getDefault()
        val nowDate = Date()
        val offsetFromUtc = timeZone.getOffset(nowDate.time)
        Timber.d("Offset from UTC is $offsetFromUtc")

        if (timeZone.useDaylightTime()) {
            Timber.d("Daylight saving is used")
            val dstOffset = timeZone.dstSavings
            if (timeZone.inDaylightTime(nowDate)) {
                Timber.d("Daylight saving offset is $dstOffset")
                return dstOffset
            }
        }
        Timber.d("Daylight saving is not used")
        return 0
    }

