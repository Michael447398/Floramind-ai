package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.core.BotanicalConstants
import com.example.model.AppConfig
import com.example.model.BotanicalReport
import com.example.model.ConfidenceTier
import com.example.model.CuratedPlant
import com.example.model.DeepResearchBundle
import com.example.model.EvidenceQuality
import com.example.model.IdentitySection
import com.example.model.SafetyLevel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FloraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleReport(common: String = "Neem Tree", scientific: String = "Azadirachta indica"): BotanicalReport =
        BotanicalReport(
            isPlantRecognized = true,
            confidenceTier = ConfidenceTier.HIGH,
            confidenceRationale = "Pinnate leaves with serrated margins observed.",
            summary = "Fast-growing mahogany family tree widely cultivated in Nigeria and South Asia.",
            safetyLevel = SafetyLevel.CAUTION,
            safetyHeadline = "Seed oil concentrated extracts can be toxic if ingested.",
            identity = IdentitySection(
                commonName = common,
                scientificName = scientific,
                family = "Meliaceae",
                genus = "Azadirachta",
                species = "indica"
            ),
            deepResearch = DeepResearchBundle(
                retrievedAtIso = "2026-04-12T10:00:00Z",
                evidenceQuality = EvidenceQuality.STRONG,
                evidenceRationale = "Verified across GBIF & Kew POWO."
            )
        )

    @Test
    fun userProfileAndScanCrud_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FloraRepository(firestore)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(uid, "Alice Botanist", ALICE_EMAIL, null)
        }
        assertTrue("Profile creation failed: ${profileRes.exceptionOrNull()}", profileRes.isSuccess)
        assertEquals(uid, profileRes.getOrThrow().userId)

        val scanRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveScanRecord(
                report = sampleReport(),
                regionContext = "West Africa (Nigeria / Ghana)",
                habitatContext = "Roadside tree",
                thumbnailBase64 = null
            )
        }
        assertTrue("Scan creation failed: ${scanRes.exceptionOrNull()}", scanRes.isSuccess)
        val scanId = scanRes.getOrThrow()

        val scans = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeScans(uid).first { list -> list.any { it.id == scanId } }
        }
        assertTrue(scans.any { it.id == scanId && it.scientificName == "Azadirachta indica" })

        val savePlantRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.savePlantToCollection(
                report = sampleReport(),
                scanId = scanId,
                folderName = "Medicinal Trees",
                tags = listOf("nigeria", "agroforestry"),
                notes = "Found near Ibadan farm edge."
            )
        }
        assertTrue("Save plant failed: ${savePlantRes.exceptionOrNull()}", savePlantRes.isSuccess)
        val plantId = savePlantRes.getOrThrow()

        val savedList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeSavedPlants(uid).first { list -> list.any { it.id == plantId } }
        }
        assertTrue(savedList.any { it.id == plantId && it.folderName == "Medicinal Trees" })
    }

    @Test
    fun observeScans_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FloraRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile(aliceUid, "Alice", ALICE_EMAIL, null).getOrThrow()
            aliceRepo.saveScanRecord(sampleReport(), "Global", "", null).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FloraRepository(firestore)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeScans(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException for cross-user scan read, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    @Test
    fun observeScans_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FloraRepository(firestore)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeScans("some_user_uid").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException for unauthenticated read, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    @Test
    fun feedbackAndAdminCms_nonOwnerDenied_verifiedOwnerAllowed() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val userRepo = FloraRepository(firestore)

        // Regular user can submit feedback report
        val fbRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            userRepo.submitFeedbackReport(
                category = "misidentification",
                plantName = "Cassava",
                message = "Look-alike leaf margin note ${UUID.randomUUID()}",
                userEmail = ALICE_EMAIL
            )
        }
        assertTrue("Feedback submission failed: ${fbRes.exceptionOrNull()}", fbRes.isSuccess)

        // Regular user CANNOT modify global app_config or curated_plants
        val forbiddenConfig = withTimeout(DEFAULT_TIMEOUT_MS) {
            userRepo.saveAppConfigAdmin(AppConfig(id = "global", dailyScanLimit = 99))
        }
        assertTrue(forbiddenConfig.isFailure)
        val forbiddenFsEx = forbiddenConfig.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, forbiddenFsEx?.code)

        // Verified owner admin CAN publish curated plants and app_config
        val ownerUid = signInTestUser(BotanicalConstants.OWNER_ADMIN_EMAIL, markEmailVerified = true)
        val ownerRepo = FloraRepository(firestore)
        val slug = "moringa_test_${UUID.randomUUID().toString().take(8)}"
        val cmsRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.upsertCuratedPlantAdmin(
                plant = CuratedPlant(
                    id = slug,
                    commonName = "Moringa",
                    scientificName = "Moringa oleifera",
                    family = "Moringaceae",
                    category = "Trees & Agroforestry",
                    region = "Nigeria / West Africa",
                    safetyLevel = SafetyLevel.SAFE.name,
                    summary = "Nutrient-dense agroforestry tree.",
                    status = "published"
                ),
                changeSummary = "Initial verified owner seed"
            )
        }
        assertTrue("Owner CMS upsert failed: ${cmsRes.exceptionOrNull()}", cmsRes.isSuccess)

        val configRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveAppConfigAdmin(AppConfig(id = "global", updatedBy = ownerUid, dailyScanLimit = 30L))
        }
        assertTrue("Owner AppConfig write failed: ${configRes.exceptionOrNull()}", configRes.isSuccess)

        // Cleanup curated test doc so repeated runs stay clean
        withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.deleteCuratedPlantAdmin(slug).getOrThrow()
        }
    }

    @Test
    fun freemiumEntitlement_freeQuotaCapAndIdempotency_enforced() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FloraRepository(firestore)

        val initial = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserEntitlement(uid).getOrThrow()
        }
        assertEquals("FREE", initial.planType)
        assertEquals(0L, initial.freeIdentificationsUsed)
        assertEquals(10L, initial.freeIdentificationsLimit)

        // Consume all 10 identifications; verify duplicate requestId does not double-charge
        for (i in 1..10) {
            val reqId = "req_free_scan_$i"
            val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.consumeIdentificationUseAtomic(uid, reqId).getOrThrow()
            }
            assertEquals(i.toLong(), updated.freeIdentificationsUsed)

            val idempotentRetry = withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.consumeIdentificationUseAtomic(uid, reqId).getOrThrow()
            }
            assertEquals(i.toLong(), idempotentRetry.freeIdentificationsUsed)
        }

        // 11th unique identification MUST be rejected with QuotaExhaustedException
        val eleventh = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.consumeIdentificationUseAtomic(uid, "req_free_scan_11")
        }
        assertTrue(eleventh.isFailure)
        assertTrue(
            "Expected QuotaExhaustedException, got ${eleventh.exceptionOrNull()}",
            eleventh.exceptionOrNull() is com.example.model.QuotaExhaustedException
        )
    }

    private companion object {
        const val ALICE_EMAIL = "alice_botany@test.com"
        const val BOB_EMAIL = "bob_botany@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
