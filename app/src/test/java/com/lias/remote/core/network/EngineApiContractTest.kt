package com.lias.remote.core.network

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.IdentityTier
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EngineApiContractTest {

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private lateinit var server: MockWebServer
    private lateinit var client: LiasApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            LiasApiClient(OkHttpClient()).apply {
                baseUrl = server.url("/").toString()
            }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `capabilities retain known features and ignore future additions`() {
        val capabilities =
            json.decodeFromString<LiasCapabilitiesResponse>(
                """
                {
                  "api_version":"v1",
                  "schema_version":2,
                  "min_client_api_version":"v1",
                  "public_device_key":"pdid",
                  "response_compatibility":"additive",
                  "features":["snapshot_v1","identity_candidate_queue","future_feature"],
                  "upstream":{"reachable":true,"legacy_mode":false},
                  "future_top_level":{"safe":true}
                }
                """.trimIndent()
            )

        assertTrue(capabilities.supports(EngineFeatures.SNAPSHOT))
        assertTrue(
            capabilities.supports(
                EngineFeatures.IDENTITY_CANDIDATE_QUEUE
            )
        )
        assertEquals("pdid", capabilities.publicDeviceKey)
        assertTrue(capabilities.upstream.reachable)
    }

    @Test
    fun `device identity additions preserve pdid as public key`() {
        val device =
            json.decodeFromString<Device>(
                """
                {
                  "device_id":"internal-immutable-id",
                  "pdid":"pdid_public",
                  "identity_tier":"bia",
                  "identity_assurance":"verified",
                  "identity_probability":1.0,
                  "identity_ambiguous":false,
                  "future_field":true
                }
                """.trimIndent()
            )

        assertEquals("pdid_public", device.pdid)
        assertEquals("internal-immutable-id", device.deviceID)
        assertEquals("bia", device.identityTier)
        assertEquals(
            IdentityTier.BIA,
            IdentityTier.fromWireValue(device.identityTier)
        )
        assertEquals("verified", device.identityAssurance)
        assertFalse(device.identityAmbiguous)
    }

    @Test
    fun `candidate decision includes stale decision guards`() {
        val request =
            IdentityCandidateDecisionRequest(
                expectedSourcePdid = "pdid_source",
                expectedTargetPdid = "pdid_target",
                expectedUpdatedAt = "2026-08-09T12:00:00Z",
                decisionNote = "Reviewed on Android"
            )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("expected_source_pdid"))
        assertTrue(encoded.contains("expected_target_pdid"))
        assertTrue(encoded.contains("expected_updated_at"))
        assertFalse(encoded.contains("device_id"))
    }

    @Test
    fun `identity candidate retains evidence and never treats score as proof`() {
        val candidate =
            json.decodeFromString<IdentityCandidateDetail>(
                """
                {
                  "id":42,
                  "source_pdid":"pdid_source",
                  "target_pdid":"pdid_target",
                  "probability":0.83,
                  "ambiguous":true,
                  "status":"pending",
                  "factors":[{"kind":"dhcp_client_id","likelihood_ratio":5.2,"matched":true}],
                  "conflicts":[{"kind":"simultaneous_online","likelihood_ratio":0.1,"matched":false}],
                  "created_at":"2026-08-09T19:00:00Z",
                  "updated_at":"2026-08-09T20:02:00Z"
                }
                """.trimIndent()
            )

        assertEquals(83, candidate.scorePercent)
        assertTrue(candidate.ambiguous)
        assertEquals(1, candidate.factors.size)
        assertEquals(1, candidate.conflicts.size)
        assertEquals("pdid_target", candidate.targetPdid)
    }

    @Test
    fun `snapshot request sends etag and decodes authoritative status maps`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setHeader("ETag", "\"rev-8\"")
                    .setBody(
                        """
                        {
                          "revision":8,
                          "devices":[],
                          "tags":[],
                          "policies":[],
                          "schedules":[],
                          "users":[],
                          "device_effective_statuses":{},
                          "tag_effective_statuses":{}
                        }
                        """.trimIndent()
                    )
            )

            val result = client.getSnapshot("\"rev-7\"")
            val request = server.takeRequest()

            assertEquals("\"rev-7\"", request.getHeader("If-None-Match"))
            assertTrue(result is ApiResult.Success)

            val fetch =
                (result as ApiResult.Success<SnapshotFetchResult>).data as
                    SnapshotFetchResult.Modified

            assertEquals(8L, fetch.snapshot.revision)
            assertEquals("\"rev-8\"", fetch.etag)
            assertTrue(fetch.snapshot.deviceEffectiveStatuses.isEmpty())
        }

    @Test
    fun `snapshot 304 is a successful not modified result`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(304)
            )

            val result = client.getSnapshot("\"rev-8\"")

            assertTrue(result is ApiResult.Success)
            assertTrue(
                (result as ApiResult.Success<SnapshotFetchResult>).data is
                    SnapshotFetchResult.NotModified
            )
        }

    @Test
    fun `identity endpoint catalog retains pdid routes`() {
        assertEquals(
            "/api/v1/devices/pdid_1/identity",
            Endpoints.deviceIdentity("pdid_1")
        )
        assertEquals(
            "/api/v1/identity/candidates/42/confirm",
            Endpoints.identityCandidateDecision(42L, "confirm")
        )
        assertEquals(
            "/api/v1/devices/pdid%2Fwith%20space/identity",
            Endpoints.deviceIdentity("pdid/with space")
        )
    }
}
