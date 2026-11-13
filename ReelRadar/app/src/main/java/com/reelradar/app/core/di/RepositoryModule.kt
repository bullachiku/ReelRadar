package com.reelradar.app.core.di

import com.reelradar.app.data.repository.AuthRepositoryImpl
import com.reelradar.app.data.repository.FriendsRepositoryImpl
import com.reelradar.app.data.repository.ReelRepositoryImpl
import com.reelradar.app.domain.repository.AuthRepository
import com.reelradar.app.domain.repository.FriendsRepository
import com.reelradar.app.domain.repository.ReelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindReelRepository(
        reelRepositoryImpl: ReelRepositoryImpl
    ): ReelRepository

    @Binds
    @Singleton
    abstract fun bindFriendsRepository(
        friendsRepositoryImpl: FriendsRepositoryImpl
    ): FriendsRepository
}
