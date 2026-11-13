package com.example.data

import androidx.annotation.DrawableRes
import com.example.R
import com.example.model.MedicalStore
import com.example.model.MedicineCategory
import com.example.model.MedicineGroup
import com.example.model.MedicineOffering

object PharmacyCatalog {

    val stores: List<MedicalStore> = listOf(
        MedicalStore(
            id = "store_apollo_care",
            name = "Apollo Care Pharmacy",
            neighborhood = "MG Road, Ernakulam, Kochi",
            distanceKm = 0.8,
            etaMinutes = 18,
            rating = 4.9,
            reviewCount = 412,
            isOpen24Hours = true,
            licenseNumber = "KL-EKM-20419",
            deliveryFee = 15.0,
            minimumOrder = 99.0,
            specialties = listOf("24x7 Express", "Cold Chain", "Prescription Verified"),
            accentTag = "Fastest in Kochi",
            promoHeadline = "20% OFF up to ₹80 • Code KOCHI20"
        ),
        MedicalStore(
            id = "store_greencross_24x7",
            name = "GreenCross 24x7 Medical",
            neighborhood = "Edappally Junction, Kochi",
            distanceKm = 1.4,
            etaMinutes = 24,
            rating = 4.8,
            reviewCount = 289,
            isOpen24Hours = true,
            licenseNumber = "KL-EKM-18902",
            deliveryFee = 0.0,
            minimumOrder = 149.0,
            specialties = listOf("Zero Delivery Fee", "Chronic Care", "Cardiac Stock"),
            accentTag = "Free Kochi delivery",
            promoHeadline = "₹0 Delivery Fee • Flat ₹50 OFF CARE50"
        ),
        MedicalStore(
            id = "store_medplus_express",
            name = "MedPlus Express Chemist",
            neighborhood = "Kakkanad InfoPark Rd, Kochi",
            distanceKm = 1.9,
            etaMinutes = 28,
            rating = 4.7,
            reviewCount = 530,
            isOpen24Hours = false,
            licenseNumber = "KL-EKM-31104",
            deliveryFee = 12.0,
            minimumOrder = 79.0,
            specialties = listOf("Lowest MRP Discounts", "Generics", "First Aid"),
            accentTag = "Best prices",
            promoHeadline = "Up to 20% MRP savings + MEDFREE"
        ),
        MedicalStore(
            id = "store_citycare_apothecary",
            name = "CityCare Apothecary",
            neighborhood = "Panampilly Nagar, Kochi",
            distanceKm = 2.3,
            etaMinutes = 32,
            rating = 4.8,
            reviewCount = 194,
            isOpen24Hours = false,
            licenseNumber = "KL-EKM-09871",
            deliveryFee = 20.0,
            minimumOrder = 100.0,
            specialties = listOf("Pediatric Care", "Surgical", "Nebulizers"),
            accentTag = "Rare stock",
            promoHeadline = "Free delivery above ₹99 • Code MEDFREE"
        ),
        MedicalStore(
            id = "store_wellspring_health",
            name = "WellSpring Health Mart",
            neighborhood = "Kaloor - Kadavanthra Rd, Kochi",
            distanceKm = 3.1,
            etaMinutes = 38,
            rating = 4.6,
            reviewCount = 165,
            isOpen24Hours = true,
            licenseNumber = "KL-EKM-44520",
            deliveryFee = 10.0,
            minimumOrder = 120.0,
            specialties = listOf("Vitamins & Nutrition", "Diabetes Care", "24x7 Night Desk"),
            accentTag = "Wellness hub",
            promoHeadline = "Flat ₹50 OFF ₹249+ • Code CARE50"
        )
    )

    val offerings: List<MedicineOffering> = listOf(
        MedicineOffering(
            id = "med_dolo_650",
            storeId = "store_apollo_care",
            storeName = "Apollo Care Pharmacy",
            name = "Dolo 650 Tablet",
            genericName = "Paracetamol IP 650mg",
            dosage = "650mg • 15 tablets",
            category = MedicineCategory.PAIN_FEVER,
            price = 32.0,
            mrp = 36.0,
            stockCount = 42,
            requiresPrescription = false,
            manufacturer = "Micro Labs",
            description = "Fever reducer and mild-to-moderate pain relief tablets.",
            imageRes = R.drawable.img_med_tablets
        ),
        MedicineOffering(
            id = "med_dolo_650",
            storeId = "store_greencross_24x7",
            storeName = "GreenCross 24x7 Medical",
            name = "Dolo 650 Tablet",
            genericName = "Paracetamol IP 650mg",
            dosage = "650mg • 15 tablets",
            category = MedicineCategory.PAIN_FEVER,
            price = 33.5,
            mrp = 36.0,
            stockCount = 19,
            requiresPrescription = false,
            manufacturer = "Micro Labs",
            description = "Fever reducer and mild-to-moderate pain relief tablets.",
            imageRes = R.drawable.img_med_tablets
        ),
        MedicineOffering(
            id = "med_dolo_650",
            storeId = "store_medplus_express",
            storeName = "MedPlus Express Chemist",
            name = "Dolo 650 Tablet",
            genericName = "Paracetamol IP 650mg",
            dosage = "650mg • 15 tablets",
            category = MedicineCategory.PAIN_FEVER,
            price = 29.0,
            mrp = 36.0,
            stockCount = 64,
            requiresPrescription = false,
            manufacturer = "Micro Labs",
            description = "Fever reducer and mild-to-moderate pain relief tablets.",
            imageRes = R.drawable.img_med_tablets
        ),
        MedicineOffering(
            id = "med_augmentin_625",
            storeId = "store_apollo_care",
            storeName = "Apollo Care Pharmacy",
            name = "Augmentin 625 Duo",
            genericName = "Amoxycillin 500mg + Clavulanic Acid 125mg",
            dosage = "625mg • 10 tablets",
            category = MedicineCategory.ANTIBIOTICS_RX,
            price = 188.0,
            mrp = 223.0,
            stockCount = 18,
            requiresPrescription = true,
            manufacturer = "GSK Pharmaceuticals",
            description = "Broad-spectrum antibiotic for bacterial infections. Pharmacist verification required.",
            imageRes = R.drawable.img_med_antibiotic
        ),
        MedicineOffering(
            id = "med_augmentin_625",
            storeId = "store_citycare_apothecary",
            storeName = "CityCare Apothecary",
            name = "Augmentin 625 Duo",
            genericName = "Amoxycillin 500mg + Clavulanic Acid 125mg",
            dosage = "625mg • 10 tablets",
            category = MedicineCategory.ANTIBIOTICS_RX,
            price = 195.0,
            mrp = 223.0,
            stockCount = 11,
            requiresPrescription = true,
            manufacturer = "GSK Pharmaceuticals",
            description = "Broad-spectrum antibiotic for bacterial infections. Pharmacist verification required.",
            imageRes = R.drawable.img_med_antibiotic
        ),
        MedicineOffering(
            id = "med_metformin_500",
            storeId = "store_greencross_24x7",
            storeName = "GreenCross 24x7 Medical",
            name = "Glycomet-GP 1 SR",
            genericName = "Glimepiride 1mg + Metformin 500mg",
            dosage = "500mg SR • 15 tablets",
            category = MedicineCategory.CARDIAC_DIABETES,
            price = 132.0,
            mrp = 158.0,
            stockCount = 28,
            requiresPrescription = true,
            manufacturer = "USV Ltd",
            description = "Sustained-release glycemic management tablets for type-2 diabetes.",
            imageRes = R.drawable.img_med_cardiac
        ),
        MedicineOffering(
            id = "med_metformin_500",
            storeId = "store_wellspring_health",
            storeName = "WellSpring Health Mart",
            name = "Glycomet-GP 1 SR",
            genericName = "Glimepiride 1mg + Metformin 500mg",
            dosage = "500mg SR • 15 tablets",
            category = MedicineCategory.CARDIAC_DIABETES,
            price = 128.0,
            mrp = 158.0,
            stockCount = 35,
            requiresPrescription = true,
            manufacturer = "USV Ltd",
            description = "Sustained-release glycemic management tablets for type-2 diabetes.",
            imageRes = R.drawable.img_med_cardiac
        ),
        MedicineOffering(
            id = "med_atorva_20",
            storeId = "store_greencross_24x7",
            storeName = "GreenCross 24x7 Medical",
            name = "Atorva 20 Tablet",
            genericName = "Atorvastatin 20mg",
            dosage = "20mg • 15 tablets",
            category = MedicineCategory.CARDIAC_DIABETES,
            price = 196.0,
            mrp = 232.0,
            stockCount = 22,
            requiresPrescription = true,
            manufacturer = "Zydus Cadila",
            description = "Lipid-lowering statin for cholesterol and cardiovascular care.",
            imageRes = R.drawable.img_med_cardiac
        ),
        MedicineOffering(
            id = "med_montair_lc",
            storeId = "store_apollo_care",
            storeName = "Apollo Care Pharmacy",
            name = "Montair LC Tablet",
            genericName = "Montelukast 10mg + Levocetirizine 5mg",
            dosage = "15 tablets",
            category = MedicineCategory.ALLERGY_COLD,
            price = 212.0,
            mrp = 248.0,
            stockCount = 26,
            requiresPrescription = true,
            manufacturer = "Cipla",
            description = "Relieves seasonal allergic rhinitis, sneezing, and nasal congestion.",
            imageRes = R.drawable.img_med_allergy
        ),
        MedicineOffering(
            id = "med_montair_lc",
            storeId = "store_medplus_express",
            storeName = "MedPlus Express Chemist",
            name = "Montair LC Tablet",
            genericName = "Montelukast 10mg + Levocetirizine 5mg",
            dosage = "15 tablets",
            category = MedicineCategory.ALLERGY_COLD,
            price = 198.0,
            mrp = 248.0,
            stockCount = 31,
            requiresPrescription = true,
            manufacturer = "Cipla",
            description = "Relieves seasonal allergic rhinitis, sneezing, and nasal congestion.",
            imageRes = R.drawable.img_med_allergy
        ),
        MedicineOffering(
            id = "med_cetirizine_10",
            storeId = "store_medplus_express",
            storeName = "MedPlus Express Chemist",
            name = "Okacet 10 Tablet",
            genericName = "Cetirizine 10mg",
            dosage = "10mg • 10 tablets",
            category = MedicineCategory.ALLERGY_COLD,
            price = 18.0,
            mrp = 22.0,
            stockCount = 75,
            requiresPrescription = false,
            manufacturer = "Cipla",
            description = "Fast non-drowsy relief from itching, hives, and allergy symptoms.",
            imageRes = R.drawable.img_med_allergy
        ),
        MedicineOffering(
            id = "med_shelcal_500",
            storeId = "store_wellspring_health",
            storeName = "WellSpring Health Mart",
            name = "Shelcal 500 + Vitamin D3",
            genericName = "Elemental Calcium 500mg + Cholecalciferol 250 IU",
            dosage = "15 tablets",
            category = MedicineCategory.VITAMINS,
            price = 118.0,
            mrp = 144.0,
            stockCount = 40,
            requiresPrescription = false,
            manufacturer = "Torrent Pharma",
            description = "Daily bone strength and vitamin D3 supplementation.",
            imageRes = R.drawable.img_med_vitamins
        ),
        MedicineOffering(
            id = "med_neurobion_forte",
            storeId = "store_apollo_care",
            storeName = "Apollo Care Pharmacy",
            name = "Neurobion Forte",
            genericName = "Vitamin B-Complex + B12",
            dosage = "30 tablets",
            category = MedicineCategory.VITAMINS,
            price = 38.0,
            mrp = 44.0,
            stockCount = 52,
            requiresPrescription = false,
            manufacturer = "Procter & Gamble Health",
            description = "Supports nerve health and daily B-vitamin nourishment.",
            imageRes = R.drawable.img_med_vitamins
        ),
        MedicineOffering(
            id = "med_digital_thermometer",
            storeId = "store_citycare_apothecary",
            storeName = "CityCare Apothecary",
            name = "FlexiTip Digital Thermometer",
            genericName = "Clinical Digital Sensor",
            dosage = "1 unit • Waterproof",
            category = MedicineCategory.FIRST_AID,
            price = 215.0,
            mrp = 260.0,
            stockCount = 14,
            requiresPrescription = false,
            manufacturer = "Dr. Morepen",
            description = "10-second fast fever readout with fever alarm and memory recall.",
            imageRes = R.drawable.img_med_firstaid
        ),
        MedicineOffering(
            id = "med_antiseptic_kit",
            storeId = "store_medplus_express",
            storeName = "MedPlus Express Chemist",
            name = "Sterile First Aid Dressing Kit",
            genericName = "Antiseptic Solution + Gauze + Bandages",
            dosage = "12-piece compact pack",
            category = MedicineCategory.FIRST_AID,
            price = 145.0,
            mrp = 175.0,
            stockCount = 29,
            requiresPrescription = false,
            manufacturer = "Hansaplast Care",
            description = "Complete wound-care dressing kit for home and travel.",
            imageRes = R.drawable.img_med_firstaid
        ),
        MedicineOffering(
            id = "med_nebulizer_mask",
            storeId = "store_citycare_apothecary",
            storeName = "CityCare Apothecary",
            name = "Adult & Child Nebulizer Mask Set",
            genericName = "Medical-Grade Tubing & Chamber",
            dosage = "1 set",
            category = MedicineCategory.FIRST_AID,
            price = 165.0,
            mrp = 199.0,
            stockCount = 9,
            requiresPrescription = false,
            manufacturer = "Omron Healthcare",
            description = "Replacement aerosol therapy masks compatible with standard compressors.",
            imageRes = R.drawable.img_med_firstaid
        )
    )

    private val storeByIdMap: Map<String, MedicalStore> = stores.associateBy { it.id }
    private val offeringsByStoreMap: Map<String, List<MedicineOffering>> = offerings.groupBy { it.storeId }
    private val offeringByKeyMap: Map<String, MedicineOffering> = offerings.associateBy { "${it.storeId}__${it.id}" }
    private val imageByMedicineIdMap: Map<String, Int> = offerings.associate { it.id to it.imageRes }
    private val mrpByMedicineIdMap: Map<String, Double> = offerings.associate { it.id to it.mrp }
    private val open24hStoreIds: Set<String> = stores.filter { it.isOpen24Hours }.map { it.id }.toSet()

    fun storeById(storeId: String): MedicalStore? = storeByIdMap[storeId]

    fun offeringsForStore(storeId: String): List<MedicineOffering> = offeringsByStoreMap[storeId].orEmpty()

    fun offeringFor(storeId: String, medicineId: String): MedicineOffering? =
        offeringByKeyMap["${storeId}__$medicineId"]

    fun isStoreOpen24h(storeId: String): Boolean = storeId in open24hStoreIds

    fun mrpForMedicine(medicineId: String): Double? = mrpByMedicineIdMap[medicineId]

    @DrawableRes
    fun imageForMedicine(medicineId: String): Int =
        imageByMedicineIdMap[medicineId] ?: R.drawable.img_med_tablets

    fun recommendedAddOns(
        storeId: String,
        excludeMedicineIds: Set<String>,
        limit: Int = 5
    ): List<MedicineOffering> {
        val sameStoreOtc = offeringsForStore(storeId)
            .filter { !it.requiresPrescription && it.id !in excludeMedicineIds }
        val fallbackOtc = offerings
            .filter { !it.requiresPrescription && it.id !in excludeMedicineIds && it.storeId != storeId }
            .distinctBy { it.id }
        return (sameStoreOtc + fallbackOtc).take(limit)
    }

    fun searchOfferings(query: String, category: MedicineCategory): List<MedicineOffering> {
        val q = query.trim().lowercase()
        return offerings.filter { item ->
            val categoryOk = category == MedicineCategory.ALL || item.category == category
            val queryOk = q.isEmpty() ||
                item.name.contains(q, ignoreCase = true) ||
                item.genericName.contains(q, ignoreCase = true) ||
                item.storeName.contains(q, ignoreCase = true) ||
                item.category.label.contains(q, ignoreCase = true)
            categoryOk && queryOk
        }
    }

    fun distinctMedicines(query: String = "", category: MedicineCategory = MedicineCategory.ALL): List<MedicineGroup> {
        return searchOfferings(query, category)
            .groupBy { it.id }
            .values
            .map { group ->
                val first = group.first()
                MedicineGroup(
                    id = first.id,
                    name = first.name,
                    genericName = first.genericName,
                    dosage = first.dosage,
                    category = first.category,
                    requiresPrescription = first.requiresPrescription,
                    manufacturer = first.manufacturer,
                    description = first.description,
                    imageRes = first.imageRes,
                    storeOfferings = group.sortedBy { it.price }
                )
            }
    }
}
