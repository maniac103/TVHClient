package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ChannelWithProgram
import org.tvheadend.data.entity.EpgChannel
import timber.log.Timber

class ChannelDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Channel, Channel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: Channel) {
        ioScope.launch { db.channelDao.insert(item) }
    }

    fun addItems(items: List<Channel>) {
        val itemsCopy = ArrayList(items)
        ioScope.launch { db.channelDao.insert(itemsCopy) }
    }

    override fun updateItem(item: Channel) {
        ioScope.launch { db.channelDao.update(item) }
    }

    override fun removeItem(item: Channel) {
        ioScope.launch { db.channelDao.delete(item) }
    }

    fun removeItemById(id: Int) {
        ioScope.launch { db.channelDao.deleteById(id) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.channelDao.itemCount
    }

    override fun getLiveDataItems(): LiveData<List<Channel>> {
        return MutableLiveData()
    }

    override fun getLiveDataItemById(id: Any): LiveData<Channel> {
        return MutableLiveData()
    }

    override fun getItemById(id: Any): Channel? = runBlocking(Dispatchers.IO) {
        db.channelDao.loadChannelByIdSync(id as Int)
    }

    fun getChannels(sortOrder: Int = 0): List<Channel> = runBlocking(Dispatchers.IO) {
        db.channelDao.loadAllChannelsSync(sortOrder)
    }

    override fun getItems(): List<Channel> {
        return getChannels()
    }

    fun getItemByIdWithPrograms(id: Int, selectedTime: Long): ChannelWithProgram? = runBlocking(Dispatchers.IO) {
        db.channelDao.loadChannelByIdWithProgramsSync(id, selectedTime)
    }

    fun getAllEpgChannels(channelSortOrder: Int, tagIds: List<Int>): LiveData<List<EpgChannel>> {
        Timber.d("Loading epg channels with sort order $channelSortOrder and ${tagIds.size} tags")
        return if (tagIds.isEmpty()) {
            db.channelDao.loadAllEpgChannels(channelSortOrder)
        } else {
            db.channelDao.loadAllEpgChannelsByTag(channelSortOrder, tagIds)
        }
    }

    fun getAllChannelsByTime(selectedTime: Long, channelSortOrder: Int, tagIds: List<Int>): LiveData<List<ChannelWithProgram>> {
        Timber.d("Loading channels from time $selectedTime with sort order $channelSortOrder and ${tagIds.size} tags")
        return if (tagIds.isEmpty()) {
            db.channelDao.loadAllChannelsByTime(selectedTime, channelSortOrder)
        } else {
            db.channelDao.loadAllChannelsByTimeAndTag(selectedTime, channelSortOrder, tagIds)
        }
    }
}
