package net.primal.android.user.wallet

import javax.inject.Inject
import net.primal.android.user.accounts.UserAccountsStore
import net.primal.android.user.domain.WalletPreference
import net.primal.android.wallet.domain.WalletKycLevel
import net.primal.core.networking.nwc.NwcClientFactory
import net.primal.wallet.NwcWalletApiImpl
import net.primal.wallet.PrimalWalletApiImpl
import net.primal.wallet.WalletApiFactory

class WalletApiFactoryImpl @Inject constructor(
    private val accountsStore: UserAccountsStore,
) : WalletApiFactory {

    override suspend fun createOrNull(userId: String): net.primal.wallet.WalletApi? {
        val userAccount = accountsStore.findByIdOrNull(userId = userId)
        val walletPreference = userAccount?.walletPreference

        return when (walletPreference) {
            WalletPreference.NostrWalletConnect -> {
                val nwcData = userAccount.nostrWallet ?: return null
                val nwcApi = NwcClientFactory.createNwcApiClient(nwcData)
                NwcWalletApiImpl(nwcApi)
            }

            WalletPreference.PrimalWallet, null -> {
                if (userAccount?.primalWallet?.kycLevel != WalletKycLevel.None) {
                    PrimalWalletApiImpl()
                } else {
                    null
                }
            }

            WalletPreference.Undefined -> null
        }
    }
}
