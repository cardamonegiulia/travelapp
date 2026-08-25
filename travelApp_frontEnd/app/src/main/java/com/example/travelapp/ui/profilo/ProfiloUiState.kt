package com.example.travelapp.ui.profilo

import com.example.travelapp.domain.model.Utente

/**
 * Stato completo della schermata Profilo con dati utente,
 * caricamento ed eventuali messaggi di errore.
 */
data class ProfiloUiState(
    val isLoading: Boolean = false,
    val utente: Utente? = null,
    val name: String = "",
    val email: String = "",
    val ruolo: String = "VIAGGIATORE", // "VIAGGIATORE", "ORGANIZZATORE", "ADMIN"
    val avatarUrl: String? = null,
    val isDarkModeEnabled: Boolean = false,
    val isPhotoUploading: Boolean = false,
    val photoMessage: String? = null,
    val errorMessage: String? = null
)
