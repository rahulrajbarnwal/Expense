package com.icit.expense.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.icit.expense.data.local.dao.FeedbackDao
import com.icit.expense.data.mapper.toDomain
import com.icit.expense.data.mapper.toEntity
import com.icit.expense.domain.model.Feedback
import com.icit.expense.domain.model.FeedbackResult
import com.icit.expense.domain.model.FeedbackStatus
import com.icit.expense.domain.repository.FeedbackRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FeedbackRepositoryImpl(
    private val feedbackDao: FeedbackDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FeedbackRepository {

    private val TAG = "FeedbackRepo"

    // Top-level collection so leadership can read every user's messages in one place.
    // Firestore rules for this collection are at the bottom of this file.
    private val feedbackCollection get() = firestore.collection(COLLECTION)

    override fun getSubmittedFeedback(): Flow<List<Feedback>> {
        return feedbackDao.getSubmittedFeedback().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun loadDraft(): Feedback? = feedbackDao.getDraft()?.toDomain()

    override suspend fun saveDraft(feedback: Feedback): Long {
        val now = System.currentTimeMillis()
        val draft = feedback.copy(
            status = FeedbackStatus.DRAFT,
            createdAt = if (feedback.createdAt == 0L) now else feedback.createdAt,
            updatedAt = now
        )
        return feedbackDao.upsertFeedback(draft.toEntity())
    }

    override suspend fun discardDraft() {
        feedbackDao.deleteDrafts()
    }

    override suspend fun submitFeedback(feedback: Feedback): FeedbackResult {
        val now = System.currentTimeMillis()
        val user = auth.currentUser
        val queued = feedback.copy(
            status = FeedbackStatus.PENDING,
            userId = user?.uid,
            userEmail = user?.email,
            createdAt = if (feedback.createdAt == 0L) now else feedback.createdAt,
            updatedAt = now,
            lastError = null
        )
        // Persist before the network call so nothing is lost if the process dies mid-send.
        val localId = feedbackDao.upsertFeedback(queued.toEntity())
        return push(queued.copy(id = localId))
    }

    override suspend fun retryUnsent(): Int {
        val unsent = feedbackDao.getUnsent()
        if (unsent.isEmpty()) return 0

        Log.d(TAG, "Retrying ${unsent.size} unsent message(s)")
        return unsent.count { push(it.toDomain()) is FeedbackResult.Sent }
    }

    override suspend fun deleteFeedback(id: Long) {
        feedbackDao.deleteById(id)
    }

    override suspend fun clearAllFeedback() {
        feedbackDao.deleteAllFeedback()
        Log.d(TAG, "Locally cleared all feedback")
    }

    /**
     * Writes [feedback] to Firestore and records the outcome locally.
     *
     * Offline is not a failure: Firestore never settles the write without a server ack, so the timeout
     * is what tells us we are offline. The message stays PENDING and [retryUnsent] picks it up later.
     */
    private suspend fun push(feedback: Feedback): FeedbackResult {
        val documentId = feedback.remoteId ?: buildDocumentId(feedback)
        var backoffMs = INITIAL_RETRY_DELAY_MS

        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    feedbackCollection.document(documentId).set(feedback.toRemoteMap(documentId)).await()
                }
                feedbackDao.upsertFeedback(
                    feedback.copy(
                        status = FeedbackStatus.SENT,
                        remoteId = documentId,
                        lastError = null,
                        updatedAt = System.currentTimeMillis()
                    ).toEntity()
                )
                Log.d(TAG, "Feedback ${feedback.id} sent as $documentId")
                return FeedbackResult.Sent(feedback.id, documentId)
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Feedback ${feedback.id} timed out, keeping it queued")
                feedbackDao.upsertFeedback(
                    feedback.copy(
                        status = FeedbackStatus.PENDING,
                        lastError = OFFLINE_REASON,
                        updatedAt = System.currentTimeMillis()
                    ).toEntity()
                )
                return FeedbackResult.Queued(feedback.id, OFFLINE_REASON)
            } catch (e: CancellationException) {
                throw e // The caller's scope went away — not our failure to record.
            } catch (e: Exception) {
                Log.e(TAG, "Feedback ${feedback.id} attempt ${attempt + 1} failed: ${e.message}", e)
                if (attempt == MAX_ATTEMPTS - 1) {
                    val reason = e.message ?: "Unknown error"
                    feedbackDao.upsertFeedback(
                        feedback.copy(
                            status = FeedbackStatus.FAILED,
                            lastError = reason,
                            updatedAt = System.currentTimeMillis()
                        ).toEntity()
                    )
                    return FeedbackResult.Error(feedback.id, reason)
                }
                delay(backoffMs)
                backoffMs *= 2
            }
        }
        // Unreachable: the last attempt always returns.
        return FeedbackResult.Error(feedback.id, "Unknown error")
    }

    /** Stable per-message id so a retry overwrites its own document instead of creating duplicates. */
    private fun buildDocumentId(feedback: Feedback): String =
        "${feedback.userId ?: ANONYMOUS_USER}_${feedback.createdAt}_${feedback.id}"

    private fun Feedback.toRemoteMap(documentId: String): Map<String, Any?> = mapOf(
        "messageId" to documentId,
        "subject" to subject,
        "message" to message,
        "category" to category.name,
        "userId" to (userId ?: ANONYMOUS_USER),
        "userEmail" to userEmail,
        "createdAt" to createdAt,
        "submittedAt" to System.currentTimeMillis()
    )

    private companion object {
        const val COLLECTION = "feedback"
        const val ANONYMOUS_USER = "anonymous"
        const val MAX_ATTEMPTS = 3
        const val INITIAL_RETRY_DELAY_MS = 1_000L
        const val NETWORK_TIMEOUT_MS = 10_000L
        const val OFFLINE_REASON = "No connection — saved and will be sent automatically"
    }
}

/*
Firestore rules for the feedback collection: anyone signed in may create, nobody may read
their way into other people's messages. Leadership reads it from the console/admin SDK.

rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /feedback/{messageId} {
      allow create: if request.auth != null;
      allow read, update, delete: if false;
    }
  }
}

NOTE: users who tapped "Skip" on the login screen have no FirebaseAuth session, so this rule
rejects their writes (they land as FAILED with the permission error shown in history). Pick one:
  - enable Firebase Anonymous Auth and sign them in on skip, or
  - relax the rule to `allow create: if true;` and rate-limit/moderate server-side.
*/
