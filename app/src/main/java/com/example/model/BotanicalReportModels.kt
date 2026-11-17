package com.example.model

enum class ConfidenceTier {
    HIGH,
    MODERATE,
    UNCERTAIN,
    NON_PLANT;

    companion object {
        fun fromString(value: String?): ConfidenceTier =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNCERTAIN
    }
}

enum class SafetyLevel {
    SAFE,
    CAUTION,
    TOXIC,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): SafetyLevel =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class EvidenceQuality {
    STRONG,
    MODERATE,
    LIMITED,
    INSUFFICIENT;

    companion object {
        fun fromString(value: String?): EvidenceQuality =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LIMITED
    }
}

enum class SourceAuthorityTier {
    PRIMARY_DATABASE,
    PEER_REVIEWED_LITERATURE,
    SUPPLEMENTARY_ENCYCLOPEDIA
}

data class CandidateSpecies(
    val commonName: String = "",
    val scientificName: String = "",
    val rationale: String = ""
)

data class ResearchSourceRecord(
    val sourceId: String = "",
    val sourceName: String = "",
    val authorityTier: String = SourceAuthorityTier.PRIMARY_DATABASE.name,
    val retrieved: Boolean = false,
    val statusLabel: String = "",
    val acceptedScientificName: String = "",
    val taxonomicStatus: String = "",
    val family: String = "",
    val synonyms: List<String> = emptyList(),
    val summary: String = "",
    val url: String = "",
    val publicationDate: String = ""
)

data class DeepResearchBundle(
    val retrievedAtIso: String = "",
    val evidenceQuality: EvidenceQuality = EvidenceQuality.LIMITED,
    val evidenceRationale: String = "",
    val discrepanciesNote: String = "",
    val sources: List<ResearchSourceRecord> = emptyList()
)

data class IdentitySection(
    val commonName: String = "",
    val scientificName: String = "",
    val synonyms: List<String> = emptyList(),
    val kingdom: String = "Plantae",
    val phylum: String = "",
    val plantClass: String = "",
    val order: String = "",
    val family: String = "",
    val genus: String = "",
    val species: String = "",
    val regionalNames: List<String> = emptyList(),
    val taxonomicNotes: String = ""
)

data class PhysicalSection(
    val observedInPhoto: String = "",
    val generalAppearance: String = "",
    val leafMorphology: String = "",
    val flowersAndSeason: String = "",
    val stemAndBark: String = "",
    val roots: String = "",
    val fruitsAndSeeds: String = "",
    val heightAndSpread: String = "",
    val growthRateAndLifespan: String = "",
    val distinguishingTraits: String = ""
)

data class HabitatSection(
    val nativeRange: String = "",
    val countriesAndRegions: String = "",
    val introducedRange: String = "",
    val climateAndTemperature: String = "",
    val soilRequirements: String = "",
    val waterAndHumidity: String = "",
    val sunlightAndAltitude: String = "",
    val seasonalAndEcologicalRole: String = ""
)

data class UsesSection(
    val foodAndNutrition: String = "",
    val agricultureAndHorticulture: String = "",
    val traditionalUses: String = "",
    val medicinalResearch: String = "",
    val timberCraftsAndMaterials: String = "",
    val cosmeticsAndLandscaping: String = "",
    val restorationAndWildlife: String = "",
    val culturalAndScientific: String = ""
)

data class BenefitsSection(
    val establishedScientific: String = "",
    val preliminaryResearch: String = "",
    val traditionalClaims: String = "",
    val nutritionalProfile: String = "",
    val agriculturalAndEcological: String = ""
)

data class SafetySection(
    val humanToxicity: String = "",
    val petsAndLivestockToxicity: String = "",
    val toxicPartsAndCompounds: String = "",
    val skinAndAllergyRisks: String = "",
    val ingestionAndPreparationRisks: String = "",
    val invasivenessAndPests: String = "",
    val misidentificationRisks: String = "",
    val medicationInteractions: String = "",
    val contraindications: String = ""
)

data class MedicinalSection(
    val historicalUses: String = "",
    val partsStudied: String = "",
    val activeCompounds: String = "",
    val biologicalMechanisms: String = "",
    val labAndAnimalEvidence: String = "",
    val humanClinicalEvidence: String = "",
    val establishedTherapeutics: String = "",
    val contraindicationsAndGaps: String = ""
)

data class CultivationSection(
    val adaptedLocationNote: String = "",
    val plantingAndPropagation: String = "",
    val soilAndFertilizer: String = "",
    val wateringAndLight: String = "",
    val pruningAndFlowering: String = "",
    val pestsAndTroubleshooting: String = "",
    val harvestingAndSustainability: String = ""
)

data class EcologySection(
    val pollinatorsAndWildlife: String = "",
    val ecosystemAndSoilWater: String = "",
    val carbonAndClimate: String = "",
    val conservationStatus: String = "",
    val threatsAndInvasivePotential: String = "",
    val responsibleHarvesting: String = ""
)

data class PlantHealthSection(
    val overallHealthAssessment: String = "",
    val visibleDiseaseSymptoms: String = "",
    val pestIndicators: String = "",
    val nutrientAndEnvironmentalStress: String = "",
    val diagnosticConfidenceNotes: String = "",
    val targetedTreatmentPlan: String = "",
    val preventionAndMonitoring: String = ""
)

data class BotanicalReport(
    val isPlantRecognized: Boolean = true,
    val confidenceTier: ConfidenceTier = ConfidenceTier.MODERATE,
    val confidenceRationale: String = "",
    val summary: String = "",
    val safetyLevel: SafetyLevel = SafetyLevel.UNKNOWN,
    val safetyHeadline: String = "",
    val visibleCharacteristics: List<String> = emptyList(),
    val missingInformation: List<String> = emptyList(),
    val alternativeCandidates: List<CandidateSpecies> = emptyList(),
    val identity: IdentitySection = IdentitySection(),
    val physical: PhysicalSection = PhysicalSection(),
    val habitat: HabitatSection = HabitatSection(),
    val uses: UsesSection = UsesSection(),
    val benefits: BenefitsSection = BenefitsSection(),
    val safety: SafetySection = SafetySection(),
    val medicinal: MedicinalSection = MedicinalSection(),
    val cultivation: CultivationSection = CultivationSection(),
    val ecology: EcologySection = EcologySection(),
    val plantHealth: PlantHealthSection = PlantHealthSection(),
    val interestingFacts: List<String> = emptyList(),
    val aiDisclaimerNote: String = "Morphological identification and narrative explanations are generated by AI (Gemini) and should be cross-checked against primary botanical authorities.",
    val deepResearch: DeepResearchBundle = DeepResearchBundle()
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "isPlantRecognized" to isPlantRecognized,
        "confidenceTier" to confidenceTier.name,
        "confidenceRationale" to confidenceRationale.take(1500),
        "summary" to summary.take(2000),
        "safetyLevel" to safetyLevel.name,
        "safetyHeadline" to safetyHeadline.take(600),
        "visibleCharacteristics" to visibleCharacteristics.take(12),
        "missingInformation" to missingInformation.take(10),
        "alternativeCandidates" to alternativeCandidates.take(5).map {
            mapOf(
                "commonName" to it.commonName,
                "scientificName" to it.scientificName,
                "rationale" to it.rationale
            )
        },
        "identity" to mapOf(
            "commonName" to identity.commonName,
            "scientificName" to identity.scientificName,
            "synonyms" to identity.synonyms.take(10),
            "kingdom" to identity.kingdom,
            "phylum" to identity.phylum,
            "plantClass" to identity.plantClass,
            "order" to identity.order,
            "family" to identity.family,
            "genus" to identity.genus,
            "species" to identity.species,
            "regionalNames" to identity.regionalNames.take(12),
            "taxonomicNotes" to identity.taxonomicNotes
        ),
        "physical" to mapOf(
            "observedInPhoto" to physical.observedInPhoto,
            "generalAppearance" to physical.generalAppearance,
            "leafMorphology" to physical.leafMorphology,
            "flowersAndSeason" to physical.flowersAndSeason,
            "stemAndBark" to physical.stemAndBark,
            "roots" to physical.roots,
            "fruitsAndSeeds" to physical.fruitsAndSeeds,
            "heightAndSpread" to physical.heightAndSpread,
            "growthRateAndLifespan" to physical.growthRateAndLifespan,
            "distinguishingTraits" to physical.distinguishingTraits
        ),
        "habitat" to mapOf(
            "nativeRange" to habitat.nativeRange,
            "countriesAndRegions" to habitat.countriesAndRegions,
            "introducedRange" to habitat.introducedRange,
            "climateAndTemperature" to habitat.climateAndTemperature,
            "soilRequirements" to habitat.soilRequirements,
            "waterAndHumidity" to habitat.waterAndHumidity,
            "sunlightAndAltitude" to habitat.sunlightAndAltitude,
            "seasonalAndEcologicalRole" to habitat.seasonalAndEcologicalRole
        ),
        "uses" to mapOf(
            "foodAndNutrition" to uses.foodAndNutrition,
            "agricultureAndHorticulture" to uses.agricultureAndHorticulture,
            "traditionalUses" to uses.traditionalUses,
            "medicinalResearch" to uses.medicinalResearch,
            "timberCraftsAndMaterials" to uses.timberCraftsAndMaterials,
            "cosmeticsAndLandscaping" to uses.cosmeticsAndLandscaping,
            "restorationAndWildlife" to uses.restorationAndWildlife,
            "culturalAndScientific" to uses.culturalAndScientific
        ),
        "benefits" to mapOf(
            "establishedScientific" to benefits.establishedScientific,
            "preliminaryResearch" to benefits.preliminaryResearch,
            "traditionalClaims" to benefits.traditionalClaims,
            "nutritionalProfile" to benefits.nutritionalProfile,
            "agriculturalAndEcological" to benefits.agriculturalAndEcological
        ),
        "safety" to mapOf(
            "humanToxicity" to safety.humanToxicity,
            "petsAndLivestockToxicity" to safety.petsAndLivestockToxicity,
            "toxicPartsAndCompounds" to safety.toxicPartsAndCompounds,
            "skinAndAllergyRisks" to safety.skinAndAllergyRisks,
            "ingestionAndPreparationRisks" to safety.ingestionAndPreparationRisks,
            "invasivenessAndPests" to safety.invasivenessAndPests,
            "misidentificationRisks" to safety.misidentificationRisks,
            "medicationInteractions" to safety.medicationInteractions,
            "contraindications" to safety.contraindications
        ),
        "medicinal" to mapOf(
            "historicalUses" to medicinal.historicalUses,
            "partsStudied" to medicinal.partsStudied,
            "activeCompounds" to medicinal.activeCompounds,
            "biologicalMechanisms" to medicinal.biologicalMechanisms,
            "labAndAnimalEvidence" to medicinal.labAndAnimalEvidence,
            "humanClinicalEvidence" to medicinal.humanClinicalEvidence,
            "establishedTherapeutics" to medicinal.establishedTherapeutics,
            "contraindicationsAndGaps" to medicinal.contraindicationsAndGaps
        ),
        "cultivation" to mapOf(
            "adaptedLocationNote" to cultivation.adaptedLocationNote,
            "plantingAndPropagation" to cultivation.plantingAndPropagation,
            "soilAndFertilizer" to cultivation.soilAndFertilizer,
            "wateringAndLight" to cultivation.wateringAndLight,
            "pruningAndFlowering" to cultivation.pruningAndFlowering,
            "pestsAndTroubleshooting" to cultivation.pestsAndTroubleshooting,
            "harvestingAndSustainability" to cultivation.harvestingAndSustainability
        ),
        "ecology" to mapOf(
            "pollinatorsAndWildlife" to ecology.pollinatorsAndWildlife,
            "ecosystemAndSoilWater" to ecology.ecosystemAndSoilWater,
            "carbonAndClimate" to ecology.carbonAndClimate,
            "conservationStatus" to ecology.conservationStatus,
            "threatsAndInvasivePotential" to ecology.threatsAndInvasivePotential,
            "responsibleHarvesting" to ecology.responsibleHarvesting
        ),
        "plantHealth" to mapOf(
            "overallHealthAssessment" to plantHealth.overallHealthAssessment,
            "visibleDiseaseSymptoms" to plantHealth.visibleDiseaseSymptoms,
            "pestIndicators" to plantHealth.pestIndicators,
            "nutrientAndEnvironmentalStress" to plantHealth.nutrientAndEnvironmentalStress,
            "diagnosticConfidenceNotes" to plantHealth.diagnosticConfidenceNotes,
            "targetedTreatmentPlan" to plantHealth.targetedTreatmentPlan,
            "preventionAndMonitoring" to plantHealth.preventionAndMonitoring
        ),
        "interestingFacts" to interestingFacts.take(10),
        "aiDisclaimerNote" to aiDisclaimerNote,
        "deepResearch" to mapOf(
            "retrievedAtIso" to deepResearch.retrievedAtIso,
            "evidenceQuality" to deepResearch.evidenceQuality.name,
            "evidenceRationale" to deepResearch.evidenceRationale,
            "discrepanciesNote" to deepResearch.discrepanciesNote,
            "sources" to deepResearch.sources.take(12).map { src ->
                mapOf(
                    "sourceId" to src.sourceId,
                    "sourceName" to src.sourceName,
                    "authorityTier" to src.authorityTier,
                    "retrieved" to src.retrieved,
                    "statusLabel" to src.statusLabel,
                    "acceptedScientificName" to src.acceptedScientificName,
                    "taxonomicStatus" to src.taxonomicStatus,
                    "family" to src.family,
                    "synonyms" to src.synonyms.take(6),
                    "summary" to src.summary.take(700),
                    "url" to src.url,
                    "publicationDate" to src.publicationDate
                )
            }
        )
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(raw: Map<String, Any?>?): BotanicalReport {
            if (raw == null) return BotanicalReport()
            fun Map<String, Any?>?.str(key: String, def: String = ""): String =
                (this?.get(key) as? String) ?: def

            fun Map<String, Any?>?.strList(key: String): List<String> =
                (this?.get(key) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            fun Map<String, Any?>?.sub(key: String): Map<String, Any?>? =
                this?.get(key) as? Map<String, Any?>

            val idMap = raw.sub("identity")
            val physMap = raw.sub("physical")
            val habMap = raw.sub("habitat")
            val usesMap = raw.sub("uses")
            val benMap = raw.sub("benefits")
            val safeMap = raw.sub("safety")
            val medMap = raw.sub("medicinal")
            val cultMap = raw.sub("cultivation")
            val ecoMap = raw.sub("ecology")
            val healthMap = raw.sub("plantHealth")
            val drMap = raw.sub("deepResearch")

            val candidates = (raw["alternativeCandidates"] as? List<*>)?.mapNotNull { item ->
                val m = item as? Map<String, Any?> ?: return@mapNotNull null
                CandidateSpecies(
                    commonName = m.str("commonName"),
                    scientificName = m.str("scientificName"),
                    rationale = m.str("rationale")
                )
            } ?: emptyList()

            val drSources = (drMap?.get("sources") as? List<*>)?.mapNotNull { item ->
                val m = item as? Map<String, Any?> ?: return@mapNotNull null
                ResearchSourceRecord(
                    sourceId = m.str("sourceId"),
                    sourceName = m.str("sourceName"),
                    authorityTier = m.str("authorityTier", SourceAuthorityTier.PRIMARY_DATABASE.name),
                    retrieved = (m["retrieved"] as? Boolean) ?: false,
                    statusLabel = m.str("statusLabel"),
                    acceptedScientificName = m.str("acceptedScientificName"),
                    taxonomicStatus = m.str("taxonomicStatus"),
                    family = m.str("family"),
                    synonyms = m.strList("synonyms"),
                    summary = m.str("summary"),
                    url = m.str("url"),
                    publicationDate = m.str("publicationDate")
                )
            } ?: emptyList()

            return BotanicalReport(
                isPlantRecognized = (raw["isPlantRecognized"] as? Boolean) ?: true,
                confidenceTier = ConfidenceTier.fromString(raw.str("confidenceTier")),
                confidenceRationale = raw.str("confidenceRationale"),
                summary = raw.str("summary"),
                safetyLevel = SafetyLevel.fromString(raw.str("safetyLevel")),
                safetyHeadline = raw.str("safetyHeadline"),
                visibleCharacteristics = raw.strList("visibleCharacteristics"),
                missingInformation = raw.strList("missingInformation"),
                alternativeCandidates = candidates,
                identity = IdentitySection(
                    commonName = idMap.str("commonName"),
                    scientificName = idMap.str("scientificName"),
                    synonyms = idMap.strList("synonyms"),
                    kingdom = idMap.str("kingdom", "Plantae"),
                    phylum = idMap.str("phylum"),
                    plantClass = idMap.str("plantClass"),
                    order = idMap.str("order"),
                    family = idMap.str("family"),
                    genus = idMap.str("genus"),
                    species = idMap.str("species"),
                    regionalNames = idMap.strList("regionalNames"),
                    taxonomicNotes = idMap.str("taxonomicNotes")
                ),
                physical = PhysicalSection(
                    observedInPhoto = physMap.str("observedInPhoto"),
                    generalAppearance = physMap.str("generalAppearance"),
                    leafMorphology = physMap.str("leafMorphology"),
                    flowersAndSeason = physMap.str("flowersAndSeason"),
                    stemAndBark = physMap.str("stemAndBark"),
                    roots = physMap.str("roots"),
                    fruitsAndSeeds = physMap.str("fruitsAndSeeds"),
                    heightAndSpread = physMap.str("heightAndSpread"),
                    growthRateAndLifespan = physMap.str("growthRateAndLifespan"),
                    distinguishingTraits = physMap.str("distinguishingTraits")
                ),
                habitat = HabitatSection(
                    nativeRange = habMap.str("nativeRange"),
                    countriesAndRegions = habMap.str("countriesAndRegions"),
                    introducedRange = habMap.str("introducedRange"),
                    climateAndTemperature = habMap.str("climateAndTemperature"),
                    soilRequirements = habMap.str("soilRequirements"),
                    waterAndHumidity = habMap.str("waterAndHumidity"),
                    sunlightAndAltitude = habMap.str("sunlightAndAltitude"),
                    seasonalAndEcologicalRole = habMap.str("seasonalAndEcologicalRole")
                ),
                uses = UsesSection(
                    foodAndNutrition = usesMap.str("foodAndNutrition"),
                    agricultureAndHorticulture = usesMap.str("agricultureAndHorticulture"),
                    traditionalUses = usesMap.str("traditionalUses"),
                    medicinalResearch = usesMap.str("medicinalResearch"),
                    timberCraftsAndMaterials = usesMap.str("timberCraftsAndMaterials"),
                    cosmeticsAndLandscaping = usesMap.str("cosmeticsAndLandscaping"),
                    restorationAndWildlife = usesMap.str("restorationAndWildlife"),
                    culturalAndScientific = usesMap.str("culturalAndScientific")
                ),
                benefits = BenefitsSection(
                    establishedScientific = benMap.str("establishedScientific"),
                    preliminaryResearch = benMap.str("preliminaryResearch"),
                    traditionalClaims = benMap.str("traditionalClaims"),
                    nutritionalProfile = benMap.str("nutritionalProfile"),
                    agriculturalAndEcological = benMap.str("agriculturalAndEcological")
                ),
                safety = SafetySection(
                    humanToxicity = safeMap.str("humanToxicity"),
                    petsAndLivestockToxicity = safeMap.str("petsAndLivestockToxicity"),
                    toxicPartsAndCompounds = safeMap.str("toxicPartsAndCompounds"),
                    skinAndAllergyRisks = safeMap.str("skinAndAllergyRisks"),
                    ingestionAndPreparationRisks = safeMap.str("ingestionAndPreparationRisks"),
                    invasivenessAndPests = safeMap.str("invasivenessAndPests"),
                    misidentificationRisks = safeMap.str("misidentificationRisks"),
                    medicationInteractions = safeMap.str("medicationInteractions"),
                    contraindications = safeMap.str("contraindications")
                ),
                medicinal = MedicinalSection(
                    historicalUses = medMap.str("historicalUses"),
                    partsStudied = medMap.str("partsStudied"),
                    activeCompounds = medMap.str("activeCompounds"),
                    biologicalMechanisms = medMap.str("biologicalMechanisms"),
                    labAndAnimalEvidence = medMap.str("labAndAnimalEvidence"),
                    humanClinicalEvidence = medMap.str("humanClinicalEvidence"),
                    establishedTherapeutics = medMap.str("establishedTherapeutics"),
                    contraindicationsAndGaps = medMap.str("contraindicationsAndGaps")
                ),
                cultivation = CultivationSection(
                    adaptedLocationNote = cultMap.str("adaptedLocationNote"),
                    plantingAndPropagation = cultMap.str("plantingAndPropagation"),
                    soilAndFertilizer = cultMap.str("soilAndFertilizer"),
                    wateringAndLight = cultMap.str("wateringAndLight"),
                    pruningAndFlowering = cultMap.str("pruningAndFlowering"),
                    pestsAndTroubleshooting = cultMap.str("pestsAndTroubleshooting"),
                    harvestingAndSustainability = cultMap.str("harvestingAndSustainability")
                ),
                ecology = EcologySection(
                    pollinatorsAndWildlife = ecoMap.str("pollinatorsAndWildlife"),
                    ecosystemAndSoilWater = ecoMap.str("ecosystemAndSoilWater"),
                    carbonAndClimate = ecoMap.str("carbonAndClimate"),
                    conservationStatus = ecoMap.str("conservationStatus"),
                    threatsAndInvasivePotential = ecoMap.str("threatsAndInvasivePotential"),
                    responsibleHarvesting = ecoMap.str("responsibleHarvesting")
                ),
                plantHealth = PlantHealthSection(
                    overallHealthAssessment = healthMap.str("overallHealthAssessment"),
                    visibleDiseaseSymptoms = healthMap.str("visibleDiseaseSymptoms"),
                    pestIndicators = healthMap.str("pestIndicators"),
                    nutrientAndEnvironmentalStress = healthMap.str("nutrientAndEnvironmentalStress"),
                    diagnosticConfidenceNotes = healthMap.str("diagnosticConfidenceNotes"),
                    targetedTreatmentPlan = healthMap.str("targetedTreatmentPlan"),
                    preventionAndMonitoring = healthMap.str("preventionAndMonitoring")
                ),
                interestingFacts = raw.strList("interestingFacts"),
                aiDisclaimerNote = raw.str(
                    "aiDisclaimerNote",
                    "AI morphological synthesis — verify against primary botanical sources."
                ),
                deepResearch = DeepResearchBundle(
                    retrievedAtIso = drMap.str("retrievedAtIso"),
                    evidenceQuality = EvidenceQuality.fromString(drMap.str("evidenceQuality")),
                    evidenceRationale = drMap.str("evidenceRationale"),
                    discrepanciesNote = drMap.str("discrepanciesNote"),
                    sources = drSources
                )
            )
        }
    }
}

data class ChatMessage(
    val id: String = "",
    val isUser: Boolean = true,
    val text: String = "",
    val isStreaming: Boolean = false
)
