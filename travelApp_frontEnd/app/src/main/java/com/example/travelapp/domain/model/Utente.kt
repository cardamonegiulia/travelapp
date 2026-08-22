package com.example.travelapp.domain.model

/** Utente così come serve alla UI (niente annotazioni di rete o di persistenza). */
data class Utente(
    val id: Long,
    val nome: String,
    val cognome: String,
    val email: String,
    val ruolo: String?,
    val tema: String?,
    /** Url completo da cui scaricare la foto profilo; `null` se l'utente non ne ha una. */
    val fotoProfiloUrl: String?
) {
    val nomeCompleto: String get() = listOf(nome, cognome).filter { it.isNotBlank() }.joinToString(" ")
}
