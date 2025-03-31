package net.primal.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import net.primal.domain.EventZap
import net.primal.domain.model.NostrEventAction
import net.primal.domain.model.NostrEventStats
import net.primal.domain.model.NostrEventUserStats

interface EventRepository {
    fun pagedEventZaps(userId: String, eventId: String): Flow<PagingData<EventZap>>
    fun observeEventStats(eventIds: List<String>): Flow<List<NostrEventStats>>
    fun observeUserEventStatus(eventIds: List<String>, userId: String): Flow<List<NostrEventUserStats>>
    suspend fun fetchEventActions(eventId: String, kind: Int): List<NostrEventAction>
    suspend fun fetchEventZaps(
        userId: String,
        eventId: String,
        limit: Int,
    )
    suspend fun findEventStats(eventId: String)
    suspend fun upsertEventStats(stats: NostrEventStats)

    suspend fun findUserStats(eventId: String, userId: String)
    suspend fun upsertUserStats(stats: NostrEventUserStats)

    suspend fun insertZap(zap: EventZap)
    suspend fun deleteZap(
        noteId: String,
        senderId: String,
        receiverId: String,
        timestamp: Long,
    )
}
