package com.floribert.autosaveflopro.di

import android.content.Context
import androidx.room.Room
import com.floribert.autosaveflopro.data.local.AppDatabase
import com.floribert.autosaveflopro.data.local.ContactDao
import com.floribert.autosaveflopro.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()
}
