package com.example.travelapp.ui.profilo

import com.example.travelapp.domain.model.Utente


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
)