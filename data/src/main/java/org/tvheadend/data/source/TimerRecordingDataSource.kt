package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.TimerRecordingWithChannel

class TimerRecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<TimerRecordingWithChannel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: TimerRecordingWithChannel) {
        ioScope.launch { db.timerRecordingDao.insert(item.base) }
    }

    override fun updateItem(item: TimerRecordingWithChannel) {
        ioScope.launch { db.timerRecordingDao.update(item.base) }
    }

    override fun removeItem(item: TimerRecordingWithChannel) {
        ioScope.launch { db.timerRecordingDao.delete(item.base) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> = db.timerRecordingDao.itemCount

    override fun getLiveDataItems(): LiveData<List<TimerRecordingWithChannel>> =
        db.timerRecordingDao.loadAllRecordings()

    override fun getLiveDataItemById(id: Any): LiveData<TimerRecordingWithChannel> =
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
