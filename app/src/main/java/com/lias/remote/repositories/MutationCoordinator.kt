// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/MutationCoordinator.kt
// Version: 14.0.0
//
// Purpose:
//   Coordinate concurrent REST mutations and bulk synchronization.
//
// Guarantees:
//   1. Mutations affecting the same logical resource are serialized.
//   2. Every mutation advances a global revision before AND after I/O.
//   3. refreshAll() can detect that its snapshot became stale while
//      network requests were in flight.
//   4. SSE-driven authoritative changes can invalidate an in-progress
//      bulk refresh.
// ====================================================================

package com.lias.remote.repositories

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutationCoordinator {

    private val revision =
        AtomicLong(0L)

    private val resourceMutexes =
        ConcurrentHashMap<String, Mutex>()

    fun revision(): Long =
        revision.get()

    fun markExternalChange(): Long =
        revision.incrementAndGet()

    fun snapshotIsCurrent(
        snapshotRevision: Long
    ): Boolean =
        revision.get() ==
            snapshotRevision

    suspend fun <T> mutate(
        resourceKey: String,
        block: suspend () -> T
    ): T {

        val mutex =
            resourceMutexes.computeIfAbsent(
                resourceKey
            ) {
                Mutex()
            }

        return mutex.withLock {

            /*
             * Invalidates any refresh that started before this
             * mutation began.
             */
            revision.incrementAndGet()

            try {
                block()
            } finally {

                /*
                 * Also invalidates any refresh that began while the
                 * server mutation was in flight.
                 */
                revision.incrementAndGet()

                /*
                 * A mutex with no waiter can safely be discarded.
                 *
                 * Even if removal races, computeIfAbsent simply gives
                 * a future operation a fresh mutex.
                 */
                if (
                    !mutex.isLocked
                ) {
                    resourceMutexes.remove(
                        resourceKey,
                        mutex
                    )
                }
            }
        }
    }
}
