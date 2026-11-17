package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.BotanicalReport
import com.example.model.BenefitsSection
import com.example.model.CandidateSpecies
import com.example.model.ChatMessage
import com.example.model.ConfidenceTier
import com.example.model.CultivationSection
import com.example.model.EcologySection
import com.example.model.HabitatSection
import com.example.model.IdentitySection
import com.example.model.MedicinalSection
import com.example.model.PhysicalSection
import com.example.model.PlantHealthSection
import com.example.model.SafetyLevel
import com.example.model.SafetySection
import com.example.model.UsesSection
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

class PlantAiRepository {

    private val reportSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "isPlantRecognized" to Schema.boolean("True if image shows a botanical specimen, plant part, mushroom/bryophyte/algae-like flora, or plant material; false if non-plant object or unrecognizable."),
                "confidenceTier" to Schema.enumeration(
                    values = listOf("HIGH", "MODERATE", "UNCERTAIN", "NON_PLANT"),
                    description = "HIGH only when clear diagnostic features match a specific species; MODERATE when genus/species is plausible; UNCERTAIN when blurry or partial; NON_PLANT if no plant is visible."
                ),
                "confidenceRationale" to Schema.string("Plain-language explanation of certainty or uncertainty without fabricating scientific probability percentages."),
                "summary" to Schema.string("Concise identification summary."),
                "safetyLevel" to Schema.enumeration(
                    values = listOf("SAFE", "CAUTION", "TOXIC", "UNKNOWN"),
                    description = "Highest relevant safety risk level."
                ),
                "safetyHeadline" to Schema.string("Immediate safety warning or reassurance visible at top of report. If uncertain, strictly warn not to ingest."),
                "visibleCharacteristics" to Schema.array(Schema.string(), "Morphological traits directly visible in the submitted image(s)."),
                "missingInformation" to Schema.array(Schema.string(), "Additional angles, structures (e.g. flowers, bark, underside of leaves), or context needed to narrow identification."),
                "alternativeCandidates" to Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "commonName" to Schema.string(),
                            "scientificName" to Schema.string(),
                            "rationale" to Schema.string()
                        )
                    ),
                    description = "Alternative look-alike or candidate species when identification is uncertain."
                ),
                "identity" to Schema.obj(
                    properties = mapOf(
                        "commonName" to Schema.string(),
                        "scientificName" to Schema.string(),
                        "synonyms" to Schema.array(Schema.string()),
                        "kingdom" to Schema.string(),
                        "phylum" to Schema.string(),
                        "plantClass" to Schema.string(),
                        "order" to Schema.string(),
                        "family" to Schema.string(),
                        "genus" to Schema.string(),
                        "species" to Schema.string(),
                        "regionalNames" to Schema.array(Schema.string()),
                        "taxonomicNotes" to Schema.string()
                    )
                ),
                "physical" to Schema.obj(
                    properties = mapOf(
                        "observedInPhoto" to Schema.string("Traits strictly observed in the uploaded photo(s)."),
                        "generalAppearance" to Schema.string(),
                        "leafMorphology" to Schema.string(),
                        "flowersAndSeason" to Schema.string(),
                        "stemAndBark" to Schema.string(),
                        "roots" to Schema.string(),
                        "fruitsAndSeeds" to Schema.string(),
                        "heightAndSpread" to Schema.string(),
                        "growthRateAndLifespan" to Schema.string(),
                        "distinguishingTraits" to Schema.string()
                    )
                ),
                "habitat" to Schema.obj(
                    properties = mapOf(
                        "nativeRange" to Schema.string(),
                        "countriesAndRegions" to Schema.string(),
                        "introducedRange" to Schema.string(),
                        "climateAndTemperature" to Schema.string(),
                        "soilRequirements" to Schema.string(),
                        "waterAndHumidity" to Schema.string(),
                        "sunlightAndAltitude" to Schema.string(),
                        "seasonalAndEcologicalRole" to Schema.string()
                    )
                ),
                "uses" to Schema.obj(
                    properties = mapOf(
                        "foodAndNutrition" to Schema.string(),
                        "agricultureAndHorticulture" to Schema.string(),
                        "traditionalUses" to Schema.string(),
                        "medicinalResearch" to Schema.string(),
                        "timberCraftsAndMaterials" to Schema.string(),
                        "cosmeticsAndLandscaping" to Schema.string(),
                        "restorationAndWildlife" to Schema.string(),
                        "culturalAndScientific" to Schema.string()
                    )
                ),
                "benefits" to Schema.obj(
                    properties = mapOf(
                        "establishedScientific" to Schema.string(),
                        "preliminaryResearch" to Schema.string(),
                        "traditionalClaims" to Schema.string(),
                        "nutritionalProfile" to Schema.string(),
                        "agriculturalAndEcological" to Schema.string()
                    )
                ),
                "safety" to Schema.obj(
                    properties = mapOf(
                        "humanToxicity" to Schema.string(),
                        "petsAndLivestockToxicity" to Schema.string(),
                        "toxicPartsAndCompounds" to Schema.string(),
                        "skinAndAllergyRisks" to Schema.string(),
                        "ingestionAndPreparationRisks" to Schema.string(),
                        "invasivenessAndPests" to Schema.string(),
                        "misidentificationRisks" to Schema.string(),
                        "medicationInteractions" to Schema.string(),
                        "contraindications" to Schema.string()
                    )
                ),
                "medicinal" to Schema.obj(
                    properties = mapOf(
                        "historicalUses" to Schema.string(),
                        "partsStudied" to Schema.string(),
                        "activeCompounds" to Schema.string(),
                        "biologicalMechanisms" to Schema.string(),
                        "labAndAnimalEvidence" to Schema.string(),
                        "humanClinicalEvidence" to Schema.string(),
                        "establishedTherapeutics" to Schema.string(),
                        "contraindicationsAndGaps" to Schema.string()
                    )
                ),
                "cultivation" to Schema.obj(
                    properties = mapOf(
                        "adaptedLocationNote" to Schema.string(),
                        "plantingAndPropagation" to Schema.string(),
                        "soilAndFertilizer" to Schema.string(),
                        "wateringAndLight" to Schema.string(),
                        "pruningAndFlowering" to Schema.string(),
                        "pestsAndTroubleshooting" to Schema.string(),
                        "harvestingAndSustainability" to Schema.string()
                    )
                ),
                "ecology" to Schema.obj(
                    properties = mapOf(
                        "pollinatorsAndWildlife" to Schema.string(),
                        "ecosystemAndSoilWater" to Schema.string(),
                        "carbonAndClimate" to Schema.string(),
                        "conservationStatus" to Schema.string(),
                        "threatsAndInvasivePotential" to Schema.string(),
                        "responsibleHarvesting" to Schema.string()
                    )
                ),
                "plantHealth" to Schema.obj(
                    properties = mapOf(
                        "overallHealthAssessment" to Schema.string("Summary of observed plant vigor vs visible distress."),
                        "visibleDiseaseSymptoms" to Schema.string("Fungal, bacterial, or viral symptoms visible in photo or common in this species."),
                        "pestIndicators" to Schema.string("Visible insect/mite damage or key pests affecting this taxon."),
                        "nutrientAndEnvironmentalStress" to Schema.string("Chlorosis, scorching, overwatering, root stress, or deficit signs."),
                        "diagnosticConfidenceNotes" to Schema.string("Limitations of visual photo pathology and recommended close-ups."),
                        "targetedTreatmentPlan" to Schema.string("Integrated pest/disease management steps and organic/cultural interventions."),
                        "preventionAndMonitoring" to Schema.string("Sanitation, airflow, watering schedule, and quarantine recommendations.")
                    )
                ),
                "interestingFacts" to Schema.array(Schema.string())
            )
        )
    }

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FLASH,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reportSchema
                temperature = 0.25f
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FLASH,
            generationConfig = generationConfig {
                temperature = 0.4f
            }
        )
    }

    suspend fun identifyPlant(
        bitmaps: List<Bitmap>,
        regionContext: String,
        habitatContext: String
    ): BotanicalReport {
        require(bitmaps.isNotEmpty()) { "At least one photograph is required for identification." }

        val promptText = buildString {
            appendLine("You are FloraMind AI, a rigorous botanical taxonomist and plant safety specialist.")
            appendLine("Analyze the submitted plant photograph(s) carefully.")
            if (regionContext.isNotBlank()) {
                appendLine("User reported country/region: $regionContext")
            }
            if (habitatContext.isNotBlank()) {
                appendLine("User reported habitat / observation notes: $habitatContext")
            }
            appendLine()
            appendLine("STRICT SCIENTIFIC & SAFETY RULES:")
            appendLine("1. Examine leaf shape, arrangement, margins, venation, flowers, stems/bark, fruits/seeds, growth habit, and visible habitat clues.")
            appendLine("2. If the image does not show a recognizable plant, set isPlantRecognized=false, confidenceTier='NON_PLANT', explain politely, and ask for a suitable botanical photo.")
            appendLine("3. If the image is blurry, shows only a tiny fragment, or matches multiple species, set confidenceTier='UNCERTAIN', list alternativeCandidates, explain what is missing in missingInformation, and clearly warn users NOT to consume or use it medicinally.")
            appendLine("4. Never claim certainty when unsupported. Never fabricate calibrated probability numbers.")
            appendLine("5. In physical.observedInPhoto, describe ONLY what is directly visible in the photograph(s), separate from general species traits.")
            appendLine("6. In uses/benefits/medicinal, clearly distinguish established clinical/scientific evidence from traditional uses and preliminary lab/animal research. Never invent uses for unsupported categories—explicitly write 'Not documented or not applicable.' Never provide unsafe medicinal dosages.")
            appendLine("7. Never invent journal citations, URLs, or study titles. (External database verification will be executed separately.)")
            appendLine("8. Adapt cultivation advice to the user's stated region ($regionContext) when provided; otherwise state general climate zones and note regional variation.")
            appendLine("9. In plantHealth, carefully inspect leaves, stems, and fruit for lesions, chlorosis, honeydew, stippling, fungal spots, or pests; clearly note if foliage looks healthy or if photo resolution limits pathology diagnosis.")
        }

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(
                    content {
                        bitmaps.forEach { bmp -> image(bmp) }
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text
                ?: throw IllegalStateException("Gemini returned an empty identification response.")
            parseStructuredReport(rawJson)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Plant identification failed", e)
            throw e
        }
    }

    suspend fun generateSpeciesDossier(
        scientificName: String,
        commonName: String?,
        regionContext: String
    ): BotanicalReport {
        val promptText = buildString {
            appendLine("You are FloraMind AI, a botanical research encyclopedia.")
            appendLine("Generate a comprehensive, scientifically grounded 11-section botanical report for:")
            appendLine("Scientific Name: $scientificName")
            if (!commonName.isNullOrBlank()) appendLine("Common Name: $commonName")
            if (regionContext.isNotBlank()) appendLine("User Region Context: $regionContext")
            appendLine("Set isPlantRecognized=true and confidenceTier='HIGH' (or 'MODERATE' if only a genus was provided).")
            appendLine("In physical.observedInPhoto, state: 'Encyclopedia lookup — no user photograph submitted.'")
            appendLine("Separate established scientific evidence from traditional claims and preliminary studies. Never invent citations or URLs.")
        }

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(promptText)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("Gemini returned an empty botanical dossier.")
            parseStructuredReport(rawJson)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Dossier generation failed", e)
            throw e
        }
    }

    fun streamAskFloraMind(
        question: String,
        activeReport: BotanicalReport?,
        history: List<ChatMessage>,
        advancedBotanyMode: Boolean,
        userRegion: String
    ): Flow<String> = flow {
        val systemInstruction = buildString {
            appendLine("You are Ask FloraMind, a trustworthy botanical research assistant inside FloraMind AI.")
            appendLine("Tone & depth: ${if (advancedBotanyMode) "Advanced scientific botanical terminology with concise definitions." else "Clear, beginner-friendly plain language explaining any botanical terms used."}")
            if (userRegion.isNotBlank()) {
                appendLine("User region/climate context: $userRegion")
            }
            appendLine("Safety rules: Never recommend eating or tasting an unidentified plant. Distinguish traditional medicine from clinical evidence. Highlight poisonous look-alikes when relevant. Never fabricate citations or study URLs.")
            if (activeReport != null) {
                appendLine()
                appendLine("CURRENT PLANT CONTEXT:")
                appendLine("Common Name: ${activeReport.identity.commonName}")
                appendLine("Scientific Name: ${activeReport.identity.scientificName}")
                appendLine("Family: ${activeReport.identity.family}")
                appendLine("Confidence: ${activeReport.confidenceTier.name} — ${activeReport.confidenceRationale}")
                appendLine("Safety Level: ${activeReport.safetyLevel.name} — ${activeReport.safetyHeadline}")
                appendLine("Toxic Parts/Risks: ${activeReport.safety.toxicPartsAndCompounds} ${activeReport.safety.misidentificationRisks}")
                appendLine("Habitat/Care: ${activeReport.habitat.climateAndTemperature} ${activeReport.cultivation.plantingAndPropagation}")
                appendLine("Medicinal Evidence: ${activeReport.medicinal.humanClinicalEvidence} | Gaps: ${activeReport.medicinal.contraindicationsAndGaps}")
            }
        }

        val recentHistory = history.takeLast(8).map { msg ->
            content(if (msg.isUser) "user" else "model") {
                text(msg.text)
            }
        }

        val chat = chatModel.startChat(
            history = listOf(
                content("user") { text("System context:\n$systemInstruction") },
                content("model") { text("Understood. I will answer accurately using the botanical safety rules and plant context.") }
            ) + recentHistory
        )

        withTimeout(60_000L) {
            chat.sendMessageStream(question).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    emit(piece)
                }
            }
        }
    }

    private fun parseStructuredReport(rawJson: String): BotanicalReport {
        val root = JSONObject(rawJson)
        fun JSONObject?.s(key: String, def: String = ""): String = this?.optString(key, def) ?: def
        fun JSONObject?.arr(key: String): List<String> {
            val a = this?.optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).mapNotNull { i -> a.optString(i).takeIf { it.isNotBlank() } }
        }

        val idObj = root.optJSONObject("identity")
        val physObj = root.optJSONObject("physical")
        val habObj = root.optJSONObject("habitat")
        val usesObj = root.optJSONObject("uses")
        val benObj = root.optJSONObject("benefits")
        val safeObj = root.optJSONObject("safety")
        val medObj = root.optJSONObject("medicinal")
        val cultObj = root.optJSONObject("cultivation")
        val ecoObj = root.optJSONObject("ecology")
        val healthObj = root.optJSONObject("plantHealth")

        val candidatesArr = root.optJSONArray("alternativeCandidates") ?: JSONArray()
        val candidates = (0 until candidatesArr.length()).mapNotNull { idx ->
            val obj = candidatesArr.optJSONObject(idx) ?: return@mapNotNull null
            CandidateSpecies(
                commonName = obj.s("commonName"),
                scientificName = obj.s("scientificName"),
                rationale = obj.s("rationale")
            )
        }

        return BotanicalReport(
            isPlantRecognized = root.optBoolean("isPlantRecognized", true),
            confidenceTier = ConfidenceTier.fromString(root.s("confidenceTier")),
            confidenceRationale = root.s("confidenceRationale"),
            summary = root.s("summary"),
            safetyLevel = SafetyLevel.fromString(root.s("safetyLevel")),
            safetyHeadline = root.s("safetyHeadline"),
            visibleCharacteristics = root.arr("visibleCharacteristics"),
            missingInformation = root.arr("missingInformation"),
            alternativeCandidates = candidates,
            identity = IdentitySection(
                commonName = idObj.s("commonName", "Unidentified Specimen"),
                scientificName = idObj.s("scientificName"),
                synonyms = idObj.arr("synonyms"),
                kingdom = idObj.s("kingdom", "Plantae"),
                phylum = idObj.s("phylum"),
                plantClass = idObj.s("plantClass"),
                order = idObj.s("order"),
                family = idObj.s("family"),
                genus = idObj.s("genus"),
                species = idObj.s("species"),
                regionalNames = idObj.arr("regionalNames"),
                taxonomicNotes = idObj.s("taxonomicNotes")
            ),
            physical = PhysicalSection(
                observedInPhoto = physObj.s("observedInPhoto"),
                generalAppearance = physObj.s("generalAppearance"),
                leafMorphology = physObj.s("leafMorphology"),
                flowersAndSeason = physObj.s("flowersAndSeason"),
                stemAndBark = physObj.s("stemAndBark"),
                roots = physObj.s("roots"),
                fruitsAndSeeds = physObj.s("fruitsAndSeeds"),
                heightAndSpread = physObj.s("heightAndSpread"),
                growthRateAndLifespan = physObj.s("growthRateAndLifespan"),
                distinguishingTraits = physObj.s("distinguishingTraits")
            ),
            habitat = HabitatSection(
                nativeRange = habObj.s("nativeRange"),
                countriesAndRegions = habObj.s("countriesAndRegions"),
                introducedRange = habObj.s("introducedRange"),
                climateAndTemperature = habObj.s("climateAndTemperature"),
                soilRequirements = habObj.s("soilRequirements"),
                waterAndHumidity = habObj.s("waterAndHumidity"),
                sunlightAndAltitude = habObj.s("sunlightAndAltitude"),
                seasonalAndEcologicalRole = habObj.s("seasonalAndEcologicalRole")
            ),
            uses = UsesSection(
                foodAndNutrition = usesObj.s("foodAndNutrition"),
                agricultureAndHorticulture = usesObj.s("agricultureAndHorticulture"),
                traditionalUses = usesObj.s("traditionalUses"),
                medicinalResearch = usesObj.s("medicinalResearch"),
                timberCraftsAndMaterials = usesObj.s("timberCraftsAndMaterials"),
                cosmeticsAndLandscaping = usesObj.s("cosmeticsAndLandscaping"),
                restorationAndWildlife = usesObj.s("restorationAndWildlife"),
                culturalAndScientific = usesObj.s("culturalAndScientific")
            ),
            benefits = BenefitsSection(
                establishedScientific = benObj.s("establishedScientific"),
                preliminaryResearch = benObj.s("preliminaryResearch"),
                traditionalClaims = benObj.s("traditionalClaims"),
                nutritionalProfile = benObj.s("nutritionalProfile"),
                agriculturalAndEcological = benObj.s("agriculturalAndEcological")
            ),
            safety = SafetySection(
                humanToxicity = safeObj.s("humanToxicity"),
                petsAndLivestockToxicity = safeObj.s("petsAndLivestockToxicity"),
                toxicPartsAndCompounds = safeObj.s("toxicPartsAndCompounds"),
                skinAndAllergyRisks = safeObj.s("skinAndAllergyRisks"),
                ingestionAndPreparationRisks = safeObj.s("ingestionAndPreparationRisks"),
                invasivenessAndPests = safeObj.s("invasivenessAndPests"),
                misidentificationRisks = safeObj.s("misidentificationRisks"),
                medicationInteractions = safeObj.s("medicationInteractions"),
                contraindications = safeObj.s("contraindications")
            ),
            medicinal = MedicinalSection(
                historicalUses = medObj.s("historicalUses"),
                partsStudied = medObj.s("partsStudied"),
                activeCompounds = medObj.s("activeCompounds"),
                biologicalMechanisms = medObj.s("biologicalMechanisms"),
                labAndAnimalEvidence = medObj.s("labAndAnimalEvidence"),
                humanClinicalEvidence = medObj.s("humanClinicalEvidence"),
                establishedTherapeutics = medObj.s("establishedTherapeutics"),
                contraindicationsAndGaps = medObj.s("contraindicationsAndGaps")
            ),
            cultivation = CultivationSection(
                adaptedLocationNote = cultObj.s("adaptedLocationNote"),
                plantingAndPropagation = cultObj.s("plantingAndPropagation"),
                soilAndFertilizer = cultObj.s("soilAndFertilizer"),
                wateringAndLight = cultObj.s("wateringAndLight"),
                pruningAndFlowering = cultObj.s("pruningAndFlowering"),
                pestsAndTroubleshooting = cultObj.s("pestsAndTroubleshooting"),
                harvestingAndSustainability = cultObj.s("harvestingAndSustainability")
            ),
            ecology = EcologySection(
                pollinatorsAndWildlife = ecoObj.s("pollinatorsAndWildlife"),
                ecosystemAndSoilWater = ecoObj.s("ecosystemAndSoilWater"),
                carbonAndClimate = ecoObj.s("carbonAndClimate"),
                conservationStatus = ecoObj.s("conservationStatus"),
                threatsAndInvasivePotential = ecoObj.s("threatsAndInvasivePotential"),
                responsibleHarvesting = ecoObj.s("responsibleHarvesting")
            ),
            plantHealth = PlantHealthSection(
                overallHealthAssessment = healthObj.s("overallHealthAssessment"),
                visibleDiseaseSymptoms = healthObj.s("visibleDiseaseSymptoms"),
                pestIndicators = healthObj.s("pestIndicators"),
                nutrientAndEnvironmentalStress = healthObj.s("nutrientAndEnvironmentalStress"),
                diagnosticConfidenceNotes = healthObj.s("diagnosticConfidenceNotes"),
                targetedTreatmentPlan = healthObj.s("targetedTreatmentPlan"),
                preventionAndMonitoring = healthObj.s("preventionAndMonitoring")
            ),
            interestingFacts = root.arr("interestingFacts")
        )
    }

    companion object {
        private const val TAG = "PlantAiRepo"
        const val MODEL_FLASH = "gemini-3.8-flash"
    }
}
