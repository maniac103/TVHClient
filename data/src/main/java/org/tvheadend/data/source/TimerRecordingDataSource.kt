package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.data.entity.TimerRecordingWithChannel

class TimerRecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<TimerRecording, TimerRecordingWithChannel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.insert(item) }
    }

    override fun updateItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.update(item) }
    }

    override fun removeItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.delete(item) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> = db.timerRecordingDao.itemCount

    override fun getLiveDataItems(): LiveData<List<TimerRecordingWithChannel>> =
        db.timerRecordingDao.loadAllRecordings()

    override fun getLiveDataItemById(id: Any): LiveData<TimerRecordingWithChannel?> =
        db.timerRecordingDao.loadRecordingById(id as String)

    override fun getItemById(id: Any): TimerRecordingWithChannel? = id
        .takeIf { it is String && it.isNotEmpty() }
        ?.let {
            runBlocking(Dispatchers.IO) {
                db.timerRecordingDao.loadRecordingByIdSync(it as String)
            }
        }

    override fun getItems(): List<TimerRecordingWithChannel> {
        return ArrayList()
    }
}
