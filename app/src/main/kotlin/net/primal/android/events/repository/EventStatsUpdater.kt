package net.primal.android.events.repository

import kotlin.time.Duration.Companion.milliseconds
import net.primal.android.events.db.EventStats
import net.primal.android.events.db.EventUserStats
import net.primal.android.events.db.EventZap
import net.primal.android.profile.repository.ProfileRepository
import net.primal.android.wallet.utils.CurrencyConversionUtils.toBtc

class EventStatsUpdater(
    val eventId: String,
    val userId: String,
    val eventAuthorId: String,
    val profileRepository: ProfileRepository,
    val eventRepository: EventRepository,
) {

    private val timestamp: Long = System.currentTimeMillis().milliseconds.inWholeSeconds

    private suspend fun eventStats(): EventStats =
        eventRepository.findEventStats(eventId) ?: EventStats(eventId = eventId)

    private suspend fun eventUserStats(): EventUserStats =
        eventRepository.findUserStats(eventId, userId) ?: EventUserStats(eventId = eventId, userId = userId)

    suspend fun increaseLikeStats() {
        val stats = eventStats()
        val userStats = eventUserStats()
        eventRepository.upsertEventStats(stats.copy(likes = stats.likes + 1))
        eventRepository.upsertUserStats(userStats.copy(liked = true))
    }

    suspend fun increaseRepostStats() {
        val stats = eventStats()
        val userStats = eventUserStats()
        eventRepository.upsertEventStats(stats.copy(reposts = stats.reposts + 1))
        eventRepository.upsertUserStats(userStats.copy(reposted = true))
    }

    suspend fun increaseZapStats(amountInSats: Int, zapComment: String) {
        val stats = eventStats()
        val userStats = eventUserStats()
        val zapSender = profileRepository.findProfileDataOrNull(profileId = userId)

        eventRepository.upsertEventStats(
            stats.copy(
                zaps = stats.zaps + 1,
                satsZapped = stats.satsZapped + amountInSats,
            ),
        )
        eventRepository.upsertUserStats(userStats.copy(zapped = true))

        eventRepository.insertZap(
            EventZap(
                zapSenderId = userId,
                zapReceiverId = eventAuthorId,
                eventId = eventId,
                zapRequestAt = timestamp,
                zapReceiptAt = timestamp,
                zapSenderAvatarCdnImage = zapSender?.avatarCdnImage,
                zapSenderHandle = zapSender?.handle,
                zapSenderDisplayName = zapSender?.displayName,
                zapSenderInternetIdentifier = zapSender?.internetIdentifier,
                zapSenderPrimalLegendProfile = zapSender?.primalPremiumInfo?.legendProfile,
                amountInBtc = amountInSats.toBtc(),
                message = zapComment,
            ),
        )
    }

    suspend fun revertStats() {
        val stats = eventStats()
        val userStats = eventUserStats()
        eventRepository.upsertEventStats(stats)
        eventRepository.upsertUserStats(userStats)
        eventRepository.deleteZap(eventId, userId, eventAuthorId, timestamp)
    }
}
