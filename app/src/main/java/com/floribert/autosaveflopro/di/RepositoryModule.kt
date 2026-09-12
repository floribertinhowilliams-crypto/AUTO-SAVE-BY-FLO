package com.floribert.autosaveflopro.di

import com.floribert.autosaveflopro.data.repository.ContactRepository
import com.floribert.autosaveflopro.data.repository.ContactRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
}
