package org.tvheadend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.data.entity.SeriesRecordingEntity

@Dao
internal interface SeriesRecordingDao {

    @get:Query("SELECT COUNT (*) FROM series_recordings AS rec " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM series_recordings AS rec " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " ORDER BY rec.start, rec.title ASC")
    fun loadAllRecordings(): LiveData<List<SeriesRecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    fun loadRecordingById(id: String): LiveData<SeriesRecordingEntity>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    fun loadRecordingByIdSync(id: String): SeriesRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recording: SeriesRecordingEntity)

    @Update
    fun update(recording: SeriesRecordingEntity)

    @Delete
    fun delete(recording: SeriesRecordingEntity)

    @Query("DELETE FROM series_recordings " +
            " WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)" +
            " AND id = :id ")
    fun deleteById(id: String)

    @Query("DELETE FROM series_recordings")
    fun deleteAll()

    companion object {

        const val RECORDING_BASE_QUERY = "SELECT DISTINCT " +
                "rec.id, rec.enabled, rec.name, rec.min_duration, rec.max_duration, rec.retention, " +
                "rec.days_of_week, rec.priority, rec.approx_time, rec.start, rec.start_window, " +
                "rec.start_extra, rec.stop_extra, rec.title, rec.fulltext, rec.directory, rec.channel_id, " +
                "rec.owner, rec.creator, rec.dup_detect, rec.removal, rec.max_count, rec.connection_id, " +
                "c.name AS channel_name, " +
                "c.icon AS channel_icon " +
                "FROM series_recordings AS rec " +
                "LEFT JOIN channels AS c ON  c.id = rec.channel_id "

        const val CONNECTION_IS_ACTIVE = " rec.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
