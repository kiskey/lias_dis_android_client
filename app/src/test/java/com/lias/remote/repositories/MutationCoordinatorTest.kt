// ====================================================================
// File: app/src/test/java/com/lias/remote/repositories/MutationCoordinatorTest.kt
// Version: 14.0.0
//
// Purpose:
//   Regression tests for mutation/refresh ordering.
//
// These are JVM-only coroutine tests and do not require Android UI.
// ====================================================================

package com.lias.remote.repositories

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class MutationCoordinatorTest {

    @Test
    fun `mutation invalidates snapshot that started before mutation`() =
        runBlocking {

            val coordinator =
                MutationCoordinator()

            val snapshotRevision =
                coordinator.revision()

            coordinator.mutate(
                "policy:test"
            ) {
                Unit
            }

            assertFalse(
                coordinator.snapshotIsCurrent(
                    snapshotRevision
                )
            )
        }

    @Test
    fun `mutation invalidates snapshot started during mutation`() =
        runBlocking {

            val coordinator =
                MutationCoordinator()

            val mutationStarted =
                CompletableDeferred<Unit>()

            val allowMutationToFinish =
                CompletableDeferred<Unit>()

            val mutation =
                async {

                    coordinator.mutate(
                        "schedule:test"
                    ) {

                        mutationStarted.complete(
                            Unit
                        )

                        allowMutationToFinish.await()
                    }
                }

            mutationStarted.await()

            /*
             * This mimics refreshAll beginning while the HTTP mutation
             * is still in flight.
             */
            val refreshRevision =
                coordinator.revision()

            allowMutationToFinish.complete(
                Unit
            )

            mutation.await()

            assertFalse(
                coordinator.snapshotIsCurrent(
                    refreshRevision
                )
            )
        }

    @Test
    fun `same resource mutations are serialized`() =
        runBlocking {

            val coordinator =
                MutationCoordinator()

            val active =
                AtomicInteger(0)

            val maxConcurrent =
                AtomicInteger(0)

            val first =
                async {

                    coordinator.mutate(
                        "device:abc"
                    ) {

                        val current =
                            active.incrementAndGet()

                        maxConcurrent.updateAndGet {
                            previous ->

                            maxOf(
                                previous,
                                current
                            )
                        }

                        delay(
                            50
                        )

                        active.decrementAndGet()
                    }
                }

            val second =
                async {

                    coordinator.mutate(
                        "device:abc"
                    ) {

                        val current =
                            active.incrementAndGet()

                        maxConcurrent.updateAndGet {
                            previous ->

                            maxOf(
                                previous,
                                current
                            )
                        }

                        delay(
                            20
                        )

                        active.decrementAndGet()
                    }
                }

            first.await()
            second.await()

            assertEquals(
                1,
                maxConcurrent.get()
            )
        }

    @Test
    fun `different resources may mutate concurrently`() =
        runBlocking {

            val coordinator =
                MutationCoordinator()

            val bothEntered =
                CompletableDeferred<Unit>()

            val entered =
                AtomicInteger(0)

            val first =
                async {

                    coordinator.mutate(
                        "device:a"
                    ) {

                        if (
                            entered.incrementAndGet() ==
                            2
                        ) {
                            bothEntered.complete(
                                Unit
                            )
                        }

                        bothEntered.await()
                    }
                }

            val second =
                async {

                    coordinator.mutate(
                        "device:b"
                    ) {

                        if (
                            entered.incrementAndGet() ==
                            2
                        ) {
                            bothEntered.complete(
                                Unit
                            )
                        }

                        bothEntered.await()
                    }
                }

            first.await()
            second.await()

            assertEquals(
                2,
                entered.get()
            )
        }

    @Test
    fun `external event invalidates bulk snapshot`() {

        val coordinator =
            MutationCoordinator()

        val before =
            coordinator.revision()

        coordinator.markExternalChange()

        assertFalse(
            coordinator.snapshotIsCurrent(
                before
            )
        )

        assertTrue(
            coordinator.snapshotIsCurrent(
                coordinator.revision()
            )
        )
    }
}
