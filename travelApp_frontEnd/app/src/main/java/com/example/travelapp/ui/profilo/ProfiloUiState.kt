package com.example.travelapp.ui.profilo

import com.example.travelapp.domain.model.Utente

/**
 * Stato completo della schermata Profilo.
 */
data class ProfiloUiState(

    /*
     * ID dell'utente recuperato dal backend.
     *
     * Serve anche per salvare preferenze come
     * il tema chiaro/scuro.
     */
    val id: Long? = null,

    val isLoading: Boolean = false,

    val utente: Utente? = null,

    val name: String = "",

    val email: String = "",

    /*
     * VIAGGIATORE
     * ORGANIZZATORE
     * ADMIN
     */
    val ruolo: String = "VIAGGIATORE",

    val avatarUrl: String? = null,

    val isDarkModeEnabled: Boolean = false,

    val isPhotoUploading: Boolean = false,

    val photoMessage: String? = null,

    val errorMessage: String? = null
)