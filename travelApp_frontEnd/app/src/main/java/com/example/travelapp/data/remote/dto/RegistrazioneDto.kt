package com.example.travelapp.data.remote.dto

data class RegistrazioneRequest(
    val nome: String,
    val cognome: String,
    val email: String,
    val password: String,
    val ruolo: String
)

data class UtenteResponse(
    val id: Long,
    val nome: String,
    val cognome: String,
    val email: String,
    val ruolo: String,
    val tema: String?
)