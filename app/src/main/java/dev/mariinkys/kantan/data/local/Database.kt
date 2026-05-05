package dev.mariinkys.kantan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.mariinkys.kantan.data.local.dao.FavoriteDao
import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.data.local.entity.FavoriteEntity
import dev.mariinkys.kantan.data.local.entity.KanjiEntity
import dev.mariinkys.kantan.data.local.entity.TermEntity
import dev.mariinkys.kantan.data.local.entity.TermFtsEntity

@Database(
    entities = [TermEntity::class, TermFtsEntity::class, KanjiEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KantanDatabase : RoomDatabase() {
    abstract fun termDao(): TermDao
    abstract fun kanjiDao(): KanjiDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "kantan.db"
        
    }
}