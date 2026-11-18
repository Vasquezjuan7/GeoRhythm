package com.example.georhythm.data

import java.text.Normalizer

object PoiSearch {

    fun normalize(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase().trim()
    }

    fun matches(query: String, target: String): Boolean {
        if (query.isBlank()) return true
        val nq = normalize(query)
        val nt = normalize(target)
        return nt.contains(nq)
    }

    fun filterPlaces(places: List<PlaceOfInterest>, query: String, category: PoiCategory? = null): List<PlaceOfInterest> {
        return places.filter { poi ->
            matches(query, poi.name) || matches(query, poi.description)
        }
    }
}
