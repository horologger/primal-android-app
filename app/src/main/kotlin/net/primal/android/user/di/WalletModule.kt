package net.primal.android.user.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.primal.android.user.wallet.WalletApiFactoryImpl
import net.primal.wallet.WalletApiFactory

@Module
@InstallIn(SingletonComponent::class)
class WalletModule {

    @Provides
    fun bindWalletApiFactory(impl: WalletApiFactoryImpl): WalletApiFactory = impl
}
