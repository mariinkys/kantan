package dev.mariinkys.kantan.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.mariinkys.kantan.data.local.KantanDatabase
import dev.mariinkys.kantan.data.local.dao.FavoriteDao
import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KantanDatabase =
        Room.databaseBuilder(context, KantanDatabase::class.java, KantanDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideTermDao(db: KantanDatabase): TermDao = db.termDao()

    @Provides
    fun provideKanjiDao(db: KantanDatabase): KanjiDao = db.kanjiDao()

    @Provides
    fun provideFavoriteDao(db: KantanDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}