package dev.mariinkys.kantan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.mariinkys.kantan.data.repository.CustomListsRepositoryImpl
import dev.mariinkys.kantan.data.repository.DictionaryRepositoryImpl
import dev.mariinkys.kantan.data.repository.FavoritesRepositoryImpl
import dev.mariinkys.kantan.domain.repository.CustomListsRepository
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import dev.mariinkys.kantan.domain.repository.FavoritesRepository
import javax.inject.Singleton

// Despite what the IDE might say this is being used, the app will not compile without it (:

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDictionaryRepository(
        impl: DictionaryRepositoryImpl
    ): DictionaryRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindCustomListsRepository(impl: CustomListsRepositoryImpl): CustomListsRepository
}