package net.primal.wallet

interface WalletApi {
    suspend fun getBalance(userId: String): Long
}
