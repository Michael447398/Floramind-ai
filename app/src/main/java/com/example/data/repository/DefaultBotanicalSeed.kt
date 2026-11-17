package com.example.data.repository

import com.example.model.CuratedPlant

object DefaultBotanicalSeed {
    fun starterCuratedPlants(authorId: String): List<CuratedPlant> = listOf(
        CuratedPlant(
            id = "moringa_oleifera",
            authorId = authorId,
            commonName = "Moringa (Drumstick Tree)",
            scientificName = "Moringa oleifera",
            family = "Moringaceae",
            category = "Food & Crops",
            region = "West Africa (Nigeria / Ghana) & South Asia",
            safetyLevel = "CAUTION",
            summary = "Fast-growing, drought-tolerant deciduous tree widely cultivated across northern and southern Nigeria (known locally as Zogale, Okwe Oyibo, and Ewe Ile) for nutrient-dense leaves and pods.",
            safetyNotice = "Leaves and young pods are widely eaten cooked, whereas root bark contains toxic alkaloids (spirochin) and should be avoided, especially during pregnancy.",
            habitat = "Semi-arid to tropical lowlands; thrives in well-drained sandy or loamy soils from sea level to 1,200 m.",
            uses = "Culinary greens, seed oil (ben oil), seed-powder water clarification, agroforestry windbreaks, and livestock fodder.",
            medicinalEvidence = "Preliminary human and animal studies investigate leaf powder for glycemic modulation and micronutrient support; clinical evidence remains moderate/limited and should not replace prescribed therapy.",
            careGuide = "Propagate from seeds or hardwood stem cuttings. Needs full sun and minimal watering once established; avoid waterlogged clay.",
            sources = listOf(
                "https://powo.science.kew.org/taxon/urn:lsid:ipni.org:names:584736-1",
                "https://www.gbif.org/species/3054185"
            ),
            featured = true,
            status = "published",
            revisionNumber = 1L
        ),
        CuratedPlant(
            id = "azadirachta_indica",
            authorId = authorId,
            commonName = "Neem Tree (Dogonyaro)",
            scientificName = "Azadirachta indica",
            family = "Meliaceae",
            category = "Trees & Agroforestry",
            region = "West Africa (Nigeria / Ghana) & Indian Subcontinent",
            safetyLevel = "CAUTION",
            summary = "Evergreen mahogany-family tree prized across Africa and Asia for shade, drought resilience, and azadirachtin-rich seeds used in integrated pest management.",
            safetyNotice = "Neem seed oil is toxic if ingested—especially to infants and young children, where it can cause severe metabolic acidosis and encephalopathy.",
            habitat = "Tropical and subtropical dry to sub-humid savannas; tolerates heat up to 45°C and nutrient-poor soils.",
            uses = "Botanical biopesticide (azadirachtin), urban shade trees, twigs for oral hygiene, and traditional decoctions.",
            medicinalEvidence = "Active limonoids such as azadirachtin and nimbin show strong insect-growth-regulating properties; internal clinical safety of concentrated extracts is limited.",
            careGuide = "Plant in full sun with good drainage. Drought-hardy once established; prune lightly to shape canopy.",
            sources = listOf(
                "https://powo.science.kew.org/taxon/urn:lsid:ipni.org:names:577492-1",
                "https://www.gbif.org/species/3190474"
            ),
            featured = true,
            status = "published",
            revisionNumber = 1L
        ),
        CuratedPlant(
            id = "aloe_vera",
            authorId = authorId,
            commonName = "Aloe Vera",
            scientificName = "Aloe vera",
            family = "Asphodelaceae",
            category = "Succulents & Cacti",
            region = "Global / Arid & Subtropical",
            safetyLevel = "CAUTION",
            summary = "Stemless rosette succulent storing clear mucilaginous gel inside thick serrated leaves.",
            safetyNotice = "Yellow latex (aloin) directly beneath the leaf rind is a strong irritant laxative and is toxic to pets (dogs/cats) and unsafe for internal use during pregnancy.",
            habitat = "Arid rocky slopes and frost-free Mediterranean or tropical gardens; widely grown indoors worldwide.",
            uses = "Topical cosmetic gel for minor skin soothing, ornamental xeriscaping.",
            medicinalEvidence = "Topical gel has supportive evidence for mild superficial burns and skin hydration; oral whole-leaf latex preparations carry safety concerns.",
            careGuide = "Bright indirect to full sun, gritty cactus soil, let soil dry completely between waterings.",
            sources = listOf(
                "https://powo.science.kew.org/taxon/urn:lsid:ipni.org:names:529980-1",
                "https://www.gbif.org/species/2777724"
            ),
            featured = false,
            status = "published",
            revisionNumber = 1L
        ),
        CuratedPlant(
            id = "digitalis_purpurea",
            authorId = authorId,
            commonName = "Common Foxglove",
            scientificName = "Digitalis purpurea",
            family = "Plantaginaceae",
            category = "Toxic & Look-Alikes",
            region = "Europe & Introduced Temperate Regions",
            safetyLevel = "TOXIC",
            summary = "Biennial herbaceous plant bearing tall spikes of tubular spotted flowers; first-year basal rosette leaves are sometimes dangerously confused with edible comfrey or borage.",
            safetyNotice = "HIGHLY TOXIC: All plant parts contain potent cardiac glycosides (digitoxin, digoxin). Ingestion can cause fatal cardiac arrhythmias in humans, pets, and livestock.",
            habitat = "Woodland clearings, acidic heathlands, and temperate garden borders.",
            uses = "Ornamental horticulture and pharmaceutical origin of standardized cardiac glycoside medicines (digoxin).",
            medicinalEvidence = "Purified digoxin is a clinically established prescription cardiac medication with a narrow therapeutic index; raw plant ingestion is life-threatening.",
            careGuide = "Grow only where children and grazing animals cannot access; prefers partial shade and moist acidic humus.",
            sources = listOf(
                "https://powo.science.kew.org/taxon/urn:lsid:ipni.org:names:802124-1",
                "https://www.gbif.org/species/5414995"
            ),
            featured = false,
            status = "published",
            revisionNumber = 1L
        ),
        CuratedPlant(
            id = "vernoria_amygdalina",
            authorId = authorId,
            commonName = "Bitter Leaf",
            scientificName = "Gymnanthemum amygdalinum",
            family = "Asteraceae",
            category = "Medicinal & Herbs",
            region = "West Africa (Nigeria / Cameroon) & Tropical Africa",
            safetyLevel = "SAFE",
            summary = "Small shrub native to tropical Africa (known as Ewuro in Yoruba, Onugbu in Igbo, Shuwaka in Hausa), prized as a washed culinary vegetable and traditional botanical.",
            safetyNotice = "Widely consumed after thorough washing to reduce bitter sesquiterpene lactones; unverified concentrated extracts should be used cautiously alongside diabetes or blood-pressure medications.",
            habitat = "Tropical forest edges, humid savannas, and home gardens across sub-Saharan Africa.",
            uses = "Washed leafy culinary vegetable (Ofe Onugbu, Ewuro soup), traditional herbal infusions, and garden hedging.",
            medicinalEvidence = "Contains vernodalin and luteolin flavonoids studied in laboratory and preliminary metabolic research; clinical trials remain limited.",
            careGuide = "Easily propagated from semi-hardwood stem cuttings during rainy season; thrives in fertile moist loam with full to partial sun.",
            sources = listOf(
                "https://powo.science.kew.org/taxon/urn:lsid:ipni.org:names:77099394-1",
                "https://www.gbif.org/species/5402029"
            ),
            featured = true,
            status = "published",
            revisionNumber = 1L
        )
    )
}
