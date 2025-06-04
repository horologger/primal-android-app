package net.primal.wallet

class PrimalWalletApiImpl() : WalletApi {
    override suspend fun getBalance(userId: String): Long {
//        val queryResult = primalApiClient.query(
//            message = PrimalCacheFilter(
//                primalVerb = PrimalVerb.WALLET.id,
//                optionsJson = buildWalletOptionsJson(
//                    userId = userId,
//                    walletVerb = WalletOperationVerb.BALANCE,
//                    requestBody = BalanceRequestBody(subWallet = SubWallet.Open),
//                    nostrNotary = nostrNotary,
//                ),
//            ),
//        )
//
//        return queryResult.findPrimalEvent(NostrEventKind.PrimalWalletBalance)
//            ?.takeContentOrNull<BalanceResponse>()
//            ?: throw NetworkException("Missing or invalid content in response.")
        return 5L
    }
}
