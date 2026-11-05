package com.pleos.mate.app.di

import android.content.Context
import com.pleos.mate.data.PetProfileDataSource
import com.pleos.mate.feature.growth.data.GrowthDataSource
import com.pleos.mate.feature.journey.data.JourneyDataSource
import com.pleos.mate.data.local.CompanionPreferences
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.growth.data.local.GrowthRepository
import com.pleos.mate.feature.journey.data.local.JourneyDraftStore
import com.pleos.mate.feature.journey.data.local.JourneyRepository
import com.pleos.mate.feature.journey.data.local.JourneyStore
import com.pleos.mate.feature.journey.data.vehicle.PleosJourneyObservationFactory
import com.pleos.mate.feature.journey.domain.JourneyObservationFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDataModule {
    @Provides
    @Singleton
    fun companionPreferences(@ApplicationContext context: Context) = CompanionPreferences(context)

    @Provides
    fun profileDataSource(preferences: CompanionPreferences): PetProfileDataSource = preferences

    @Provides
    fun growthDataSource(store: GrowthStore): GrowthDataSource = store

    @Provides
    fun journeyDataSource(store: JourneyStore): JourneyDataSource = store

    @Provides
    @Singleton
    fun growthStore(@ApplicationContext context: Context) = GrowthStore(context.filesDir)

    @Provides
    @Singleton
    fun growthRepository(store: GrowthDataSource) = GrowthRepository(store)

    @Provides
    @Singleton
    fun journeyStore(@ApplicationContext context: Context) = JourneyStore(context.filesDir)

    @Provides
    @Singleton
    fun journeyRepository(store: JourneyDataSource) = JourneyRepository(store)

    @Provides
    @Singleton
    fun journeyDraftStore(@ApplicationContext context: Context) = JourneyDraftStore(context)
}

@Module
@InstallIn(ViewModelComponent::class)
object CompanionSessionModule {
    @Provides
    @ViewModelScoped
    fun companionSession(
        @ApplicationContext context: Context,
        store: GrowthRepository,
        preferences: CompanionPreferences,
        profile: PetProfileDataSource
    ) = CompanionSession(context, store, preferences, profile)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class JourneyObservationModule {
    @Binds
    @Singleton
    abstract fun journeyObservationFactory(
        implementation: PleosJourneyObservationFactory
    ): JourneyObservationFactory
}
