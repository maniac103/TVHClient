package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.ProgramWithChannel

class ProgramDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Program, ProgramWithChannel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val itemCount: Int
        get() {
            var count: Int
            runBlocking(Dispatchers.IO) {
                count = db.programDao.itemCountSync
            }
            return count
        }

    override fun addItem(item: Program) {
        ioScope.launch { db.programDao.insert(item) }
    }

    fun addItems(items: List<Program>) {
        ioScope.launch {
            db.programDao.insert(items)
        }
    }

    override fun updateItem(item: Program) {
        ioScope.launch { db.programDao.update(item) }
    }

    override fun removeItem(item: Program) {
        ioScope.launch { db.programDao.delete(item) }
    }

    fun removeItemsByTime(time: Long) {
        ioScope.launch { db.programDao.deleteProgramsByTime(time) }
    }

    fun removeItemById(id: Int) {
        ioScope.launch { db.programDao.deleteById(id) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> = db.programDao.itemCount

    override fun getLiveDataItems(): LiveData<List<ProgramWithChannel>> = db.programDao.loadPrograms()

    override fun getLiveDataItemById(id: Any): LiveData<ProgramWithChannel> =
        db.programDao.loadProgramById(id as Int)

    override fun getItemById(id: Any): ProgramWithChannel? = runBlocking(Dispatchers.IO) {
        db.programDao.loadProgramByIdSync(id as Int)
    }

    override fun getItems(): List<ProgramWithChannel> = runBlocking(Dispatchers.IO) {
        db.programDao.loadProgramsSync()
    }

    fun getLiveDataItemsFromTime(time: Long): LiveData<List<ProgramWithChannel>> =
        db.programDao.loadProgramsFromTime(time)

    fun getLiveDataItemByChannelIdAndTime(channelId: Int, time: Long): LiveData<List<ProgramWithChannel>> =
        db.programDao.loadProgramsFromChannelFromTime(channelId, time)

    fun getItemByChannelIdAndBetweenTime(channelId: Int, startTime: Long, endTime: Long): List<EpgProgram> = runBlocking(Dispatchers.IO) {
            db.programDao.loadEpgProgramsFromChannelBetweenTimeSync(channelId, startTime, endTime)
    }

    fun getLastItemByChannelId(channelId: Int): ProgramWithChannel? = runBlocking(Dispatchers.IO) {
        db.programDao.loadLastProgramFromChannelSync(channelId)
    }

    fun getItemsByChannelId(channelId: Int): List<ProgramWithChannel> = runBlocking(Dispatchers.IO) {
        val programs = mutableListOf<ProgramWithChannel>()
        val timeStep = 1000L * 3600 * 24 * 2
        val lastProgram = db.programDao.loadLastProgramFromChannelSync(channelId)
        val startTime = System.currentTimeMillis()
        val endTime = lastProgram?.stop ?: startTime

        // Load the programs in chunks to avoid a SQLiteBlobTooBigException
        for (time in startTime until endTime step timeStep) {
            programs += db.programDao.loadProgramsFromChannelBetweenTimeSync(channelId, time, time + timeStep)
        }
        programs
    }

    fun getDuplicatePrograms(channelId: Int): List<EpgProgram> = runBlocking(Dispatchers.IO) {
        db.programDao.loadDuplicateProgramsSync(channelId)
    }
}
