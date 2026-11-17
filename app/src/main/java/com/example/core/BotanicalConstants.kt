package com.example.core

object BotanicalConstants {
    const val APP_VERSION = "1.0.0"
    const val SCHEMA_VERSION = 1
    const val OWNER_ADMIN_EMAIL = "michaeligwe430@gmail.com"
    const val DEFAULT_DAILY_SCAN_LIMIT = 25
    const val FREE_IDENTIFICATION_LIMIT = 10
    const val MAX_PHOTOS_PER_SCAN = 4

    // Configurable Google Play Console / Backend Product IDs
    const val PRODUCT_ID_MONTHLY = "floramind_premium_monthly"
    const val PRODUCT_ID_ANNUAL = "floramind_premium_annual"
    const val PRODUCT_ID_LIFETIME = "floramind_lifetime_premium"

    const val BILLING_TERMS_SUMMARY =
        "Monthly and Annual subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period. Manage or cancel anytime in Google Play Subscriptions. Lifetime Premium is a single one-time payment granting permanent premium access for your account while FloraMind AI remains operational under the Terms of Service. There is no time-based free trial; Free accounts receive 10 successful identifications."

    const val SAFETY_DISCLAIMER =
        "Scientific Safety Disclaimer: Automated plant identification and AI-generated botanical summaries can be incorrect, especially for look-alike species, juvenile leaves, or partial photographs. Never eat, taste, forage, or apply any plant for medicinal treatment based solely on automated identification. Consult a qualified botanist, agricultural extension expert, or poison control center when safety is uncertain."

    val POPULAR_CATEGORIES = listOf(
        "All",
        "Medicinal & Herbs",
        "Trees & Agroforestry",
        "Food & Crops",
        "Ornamental & Houseplants",
        "Toxic & Look-Alikes",
        "Succulents & Cacti",
        "Ferns & Mosses"
    )

    val GLOBAL_REGIONS = listOf(
        "Global",
        "West Africa (Nigeria / Ghana)",
        "East & Southern Africa",
        "North America",
        "South America",
        "Europe",
        "South & Southeast Asia",
        "East Asia",
        "Oceania"
    )

    const val PRIVACY_POLICY_TEXT = """
FloraMind AI Privacy Policy (Effective 2026)

1. Information We Collect
• Google Account Identity: When you sign in with Google, Firebase Authentication provides your display name, email address, and unique user ID to isolate your private botanical workspace.
• Plant Observations & Scans: Photographs you capture or upload are processed via Firebase AI Logic (Gemini) to identify plants. Full-resolution images remain locally on your device. If thumbnail sync is enabled in Settings, a compressed thumbnail (<= 150 KB) is saved with your scan report in Cloud Firestore so you can recognize plants across devices.
• Personal Notes & Collections: Saved plants, folders, tags, and field notes are stored under your private user path in Cloud Firestore.

2. Data Isolation & Retention Controls
• Zero Cross-User Access: Firestore Security Rules strictly enforce that only your authenticated account can read, edit, or delete your personal scans, saved plants, and folders.
• Granular Deletion: You can delete compressed thumbnails individually while keeping scientific reports, delete individual scan records, or delete your entire account and all cloud/local data at any time from Profile & Settings.

3. Third-Party Scientific Queries
• When Deep Research verifies a plant name, FloraMind AI sends only the candidate botanical species name (never your personal identity or photographs) to public scientific endpoints (GBIF, Kew POWO, World Flora Online, NCBI PubMed, and Wikipedia).

4. Contact & Governance
• Administrator tools are restricted strictly to verified owner credentials and do not expose user passwords (authentication is handled solely by Google Sign-In).
"""

    const val TERMS_OF_SERVICE_TEXT = """
FloraMind AI Terms of Service

1. Educational & Research Purpose
FloraMind AI is a botanical research and discovery aid. It is NOT a medical diagnostic tool, clinical prescription engine, or poison-safety guarantee.

2. Assumption of Risk & Safety Boundaries
• Never ingest, brew, taste, or apply any wild or cultivated plant based solely on an automated identification.
• Many edible, culinary, and medicinal plants closely resemble toxic species. Always verify physical specimens with certified local botanists, herbaria, or agricultural extension specialists.

3. Fair Usage & Quotas
• To maintain reliable AI performance for users worldwide, daily identification quotas and file size limits apply and may be adjusted by the application owner via live configuration.

4. Intellectual Property & Open Scientific Data
• External taxonomic and literature links reference GBIF, Royal Botanic Gardens Kew (POWO), World Flora Online, NCBI PubMed, and Wikimedia under their respective open-data licenses.
"""
}
