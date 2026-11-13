package com.example.model

data class KochiZone(
    val name: String,
    val pinCode: String,
    val latitude: Double,
    val longitude: Double
)

object KochiServiceArea {

    const val CITY_NAME = "Kochi"
    const val STATE_NAME = "Kerala"
    const val PIN_PREFIX = "682"

    val supportedZones: List<KochiZone> = listOf(
        KochiZone("MG Road / Ernakulam", "682016", 9.9710, 76.2830),
        KochiZone("Edappally", "682024", 10.0261, 76.3125),
        KochiZone("Kakkanad", "682030", 10.0159, 76.3419),
        KochiZone("Kaloor", "682017", 9.9949, 76.2921),
        KochiZone("Panampilly Nagar", "682036", 9.9619, 76.2990),
        KochiZone("Kadavanthra", "682020", 9.9673, 76.3005),
        KochiZone("Palarivattom", "682025", 10.0014, 76.3067),
        KochiZone("Vyttila", "682019", 9.9679, 76.3183),
        KochiZone("Fort Kochi", "682001", 9.9658, 76.2421),
        KochiZone("Aluva", "682101", 10.1004, 76.3570),
        KochiZone("Thrippunithura", "682301", 9.9487, 76.3464),
        KochiZone("Kalamassery", "682033", 10.0531, 76.3228)
    )

    private val kochiKeywords = setOf(
        "kochi",
        "cochin",
        "ernakulam",
        "edappally",
        "kakkanad",
        "kaloor",
        "panampilly",
        "kadavanthra",
        "palarivattom",
        "vyttila",
        "fort kochi",
        "mattancherry",
        "aluva",
        "thrippunithura",
        "tripunithura",
        "kalamassery",
        "marine drive",
        "thevara",
        "vennala",
        "thrikkakara",
        "maradu",
        "infopark",
        "kundannoor",
        "cheranalloor",
        "elamakkara",
        "pachalam",
        "vaduthala",
        "ravipuram",
        "thammanam",
        "petta"
    )

    private val outsideCities = setOf(
        "bengaluru",
        "bangalore",
        "chennai",
        "mumbai",
        "delhi",
        "new delhi",
        "hyderabad",
        "pune",
        "kolkata",
        "thiruvananthapuram",
        "trivandrum",
        "kozhikode",
        "calicut",
        "thrissur",
        "trichur",
        "kottayam",
        "kollam",
        "palakkad",
        "kannur",
        "alappuzha",
        "alleppey",
        "malappuram",
        "kasaragod",
        "idukki",
        "pathanamthitta",
        "wayanad"
    )

    private val sixDigitPinRegex = Regex("\\b(\\d{6})\\b")
    private val kochiPinRegex = Regex("^682\\d{3}$")

    fun isKochiPinCode(pinCode: String): Boolean {
        return kochiPinRegex.matches(pinCode.trim())
    }

    fun isKochiAddress(address: DeliveryAddress?): Boolean {
        if (address == null) return false
        return isKochiAddress(address.fullAddress, address.landmark)
    }

    fun isKochiAddress(fullAddress: String, landmark: String = ""): Boolean {
        val combined = "$fullAddress $landmark".trim().lowercase()
        if (combined.isBlank()) return false

        val pins = sixDigitPinRegex.findAll(combined).map { it.groupValues[1] }.toList()
        if (pins.isNotEmpty()) {
            // Every 6-digit postal PIN found must belong to Kochi / Ernakulam (682xxx)
            if (pins.any { !isKochiPinCode(it) }) return false
            return true
        }

        val mentionsOutsideCity = outsideCities.any { city ->
            Regex("\\b${Regex.escape(city)}\\b").containsMatchIn(combined)
        }
        if (mentionsOutsideCity) return false

        return kochiKeywords.any { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(combined)
        }
    }
}
