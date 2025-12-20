package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingWithChannel

class RecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Recording, RecordingWithChannel> {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: Recording) {
        scope.launch { db.recordingDao.insert(item) }
    }

    fun addItems(items: List<Recording>) {
        val itemsCopy = ArrayList(items)
        scope.launch { db.recordingDao.insert(itemsCopy) }
    }

    override fun updateItem(item: Recording) {
        scope.launch { db.recordingDao.update(item) }
    }

    override fun removeItem(item: Recording) {
        scope.launch { db.recordingDao.delete(item) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return MutableLiveData()
    }

    override fun getLiveDataItems(): LiveData<List<RecordingWithChannel>> =
        db.recordingDao.loadRecordings()

    override fun getLiveDataItemById(id: Any): LiveData<RecordingWithChannel> =
        db.recordingDao.loadRecordingById(id as Int)

    fun getLiveDataItemsByChannelId(channelId: Int): LiveData<List<RecordingWithChannel>> =
        db.recordingDao.loadRecordingsByChannelId(channelId)

    fun getCompletedRecordings(sortOrder: Int): LiveData<List<RecordingWithChannel>> =
        db.recordingDao.loadCompletedRecordings(sortOrder)

    fun getScheduledRecordings(hideDuplicates: Boolean): LiveData<List<RecordingWithChannel>> = if (hideDuplicates) {
            db.recordingDao.loadUniqueScheduledRecordings()
        } else {
            db.recordingDao.loadScheduledRecordings()
        }

    fun getFailedRecordings(): LiveData<List<RecordingWithChannel>> =
        db.recordingDao.loadFailedRecordings()

    fun getRemovedRecordings(): LiveData<List<RecordingWithChannel>> =
        db.recordingDao.loadRemovedRecordings()

    fun getLiveDataCountByType(type: String): LiveData<Int> {
        return when (type) {
            "completed" -> db.recordingDao.completedRecordingCount
            "scheduled" -> db.recordingDao.scheduledRecordingCount
            "running" -> db.recordingDao.runningRecordingCount
            "failed" -> db.recordingDao.failedRecordingCount
            "removed" -> db.recordingDao.removedRecordingCount
            else -> MutableLiveData()
        }
    }

    override fun getItemById(id: Any): RecordingWithChannel? = id
        .takeIf { it is Int && it > 0 }
        ?.let {
            runBlocking(Dispatchers.IO) {
                db.recordingDao.loadRecordingByIdSync(it as Int)
            }
        }

    override fun getItems(): List<RecordingWithChannel> {
        return ArrayList()
    }

    fun getItemByEventId(id: Int): RecordingWithChannel? = id
        .takeIf { id > 0 }
        ?.let {
            runBlocking(Dispatchers.IO) {
                db.recordingDao.loadRecordingByEventIdSync(id)
            }
        }

    fun removeAndAddItems(items: ArrayList<Recording>) {
        scope.launch {
            db.recordingDao.deleteAll()
            db.recordingDao.insert(items)
        }
    }
}
