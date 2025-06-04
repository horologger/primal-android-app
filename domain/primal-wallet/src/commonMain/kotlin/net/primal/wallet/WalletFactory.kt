package net.primal.wallet

interface WalletApiFactory {
    suspend fun createOrNull(userId: String): WalletApi?
}
