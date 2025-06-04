package net.primal.wallet

import net.primal.core.networking.nwc.NwcApi
import net.primal.core.networking.nwc.NwcResult
import net.primal.core.utils.MSATS_IN_SATS

class NwcWalletApiImpl(
    private val nwcApi: NwcApi,
) : WalletApi {
    override suspend fun getBalance(userId: String): Long {
        return when (val result = nwcApi.getBalance()) {
            is NwcResult.Success -> result.result.balance / MSATS_IN_SATS
            is NwcResult.Failure -> throw result.error
        }
    }
}
