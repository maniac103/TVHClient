package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.data.entity.SeriesRecordingWithChannel
import java.util.*

class SeriesRecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<SeriesRecording, SeriesRecordingWithChannel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.insert(item) }
    }

    override fun updateItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.update(item) }
    }

    override fun removeItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.delete(item) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> = db.seriesRecordingDao.itemCount

    override fun getLiveDataItems(): LiveData<List<SeriesRecordingWithChannel>> = db.seriesRecordingDao.loadAllRecordings()

    override fun getLiveDataItemById(id: Any): LiveData<SeriesRecordingWithChannel> =
        db.seriesRecordingDao.loadRecordingById(id as String)

    override fun getItemById(id: Any): SeriesRecordingWithChannel? = id
        .takeIf { it is String && it.isNotEmpty() }
        ?.let {
            runBlocking(Dispatchers.IO) {
                db.seriesRecordingDao.loadRecordingByIdSync(id as String)
            }
        }

    override fun getItems(): List<SeriesRecordingWithChannel> {
        return ArrayList()
    }
}
