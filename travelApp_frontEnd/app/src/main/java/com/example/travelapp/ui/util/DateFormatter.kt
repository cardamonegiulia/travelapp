package com.example.travelapp.ui.util

/**
 * Converte una data ISO ricevuta dal backend
 * 2026-06-01T10:00:00 -> 01/06/2026
 * 2026-06-01 -> 01/06/2026
 * Se il formato non è riconosciuto restituisce il valore originale.
 */
fun formattaData(data: String?): String {
    if (data.isNullOrBlank()) {
        return "---"
    }

    return try {
        val parteData = data.substringBefore("T")
        val parti = parteData.split("-")

        if (parti.size == 3) {
            "${parti[2]}/${parti[1]}/${parti[0]}"
        } else {
            data
        }
    } catch (_: Exception) {
        data
    }
}

/**
 * Converte una data ISO mostrando anche l'ora.
 * 2026-06-01T10:30:00 -> 01/06/2026 · 10:30
 */
fun formattaDataOra(data: String?): String {
    if (data.isNullOrBlank()) {
        return "---"
    }

    return try {
        val dataFormattata = formattaData(data)

        if ("T" !in data) {
            return dataFormattata
        }

        val parteOra = data.substringAfter("T")
        val componentiOra = parteOra.split(":")

        if (componentiOra.size >= 2) {
            "$dataFormattata · ${componentiOra[0]}:${componentiOra[1]}"
        } else {
            dataFormattata
        }
    } catch (_: Exception) {
        data
    }
}

/**
 * Formatta un intervallo.
 * 2026-06-01T10:00:00 + 2026-06-07T18:00:00
 * ->
 * 01/06/2026 - 07/06/2026
 */
fun formattaIntervalloDate(
    dataInizio: String?,
    dataFine: String?
): String {
    return "${formattaData(dataInizio)} - ${formattaData(dataFine)}"
}