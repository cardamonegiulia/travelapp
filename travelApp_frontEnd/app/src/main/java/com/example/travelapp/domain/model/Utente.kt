package com.example.travelapp.domain.model

data class Utente(
    val id: Long,
    val nome: String,
    val cognome: String,
    val email: String,
    val ruolo: String?,
    val tema: String?,
    val fotoProfiloUrl: String?
) {
    val nomeCompleto: String get() = listOf(nome, cognome).filter { it.isNotBlank() }.joinToString(" ")
}
