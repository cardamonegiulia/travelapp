package com.example.travelapp.ui.profilo

import com.example.travelapp.domain.model.Utente

/**
 * Stato completo della schermata Profilo con dati utente,
 * caricamento ed eventuali messaggi di errore.
 */
data class ProfiloUiState(
    val id: Long? = null,
    val isLoading: Boolean = false,
    val utente: Utente? = null,
    val name: String = "",
    val email: String = "",
    val ruolo: String = "VIAGGIATORE",
    val avatarUrl: String? = null,
    val isDarkModeEnabled: Boolean = false,
    val isPhotoUploading: Boolean = false,
    val photoMessage: String? = null,
    val errorMessage: String? = null
) {
    fun conProfilo(utente: Utente): ProfiloUiState {
        val nomeCompleto = listOf(
            utente.nome,
            utente.cognome
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return copy(
            id = utente.id,
            isLoading = false,
            utente = utente,

            name = if (nomeCompleto.isNotBlank()) {
                nomeCompleto
            } else {
                utente.email
            },

            email = utente.email,
            ruolo = utente.ruolo ?: "VIAGGIATORE",
            avatarUrl = utente.fotoProfiloUrl,

            isDarkModeEnabled =
                utente.tema?.equals(
                    "SCURO",
                    ignoreCase = true
                ) ?: isDarkModeEnabled,

            errorMessage = null
        )
    }
}