package com.riis.kotlin_simulatordemo
import androidx.room.*

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.serialization.Serializable

data class Controls(
    var lh: Int,
    var lv: Int,
    var rh: Int,
    var rv: Int
)
@Serializable
@Entity
data class DroneData(
    @PrimaryKey var t: Long,
    var id: String,
    var x: Double,
    var y: Double,
    var z: Double,
    val vX: Double,
    val vY: Double,
    val vZ: Double,
    val roll: Double,
    val pitch: Double,
    val yaw: Double,
    var lh: Int,
    var lv: Int,
    var rh: Int,
    var rv: Int
)

@Dao
interface DroneDataDao {
    @Query("SELECT * FROM Course")
    suspend fun getAll(): List<DroneData>

    @Insert
    suspend fun insertAll(Courses: List<DroneData>)

    @Delete
    suspend fun delete(Course: DroneData)
}

@Database(entities = [DroneData::class], version = 1)
abstract class DroneDataDatabase : RoomDatabase() {
    abstract fun quoteDao(): DroneDataDao

    companion object {
        private var INSTANCE: DroneDataDatabase? = null
        fun getDatabase(context: Context): DroneDataDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE =
                        Room.databaseBuilder(context, DroneDataDatabase::class.java, "drone_database")
                            .createFromAsset("positions.db")
                            .build()
                }
            }
            return INSTANCE!!
        }
    }
}