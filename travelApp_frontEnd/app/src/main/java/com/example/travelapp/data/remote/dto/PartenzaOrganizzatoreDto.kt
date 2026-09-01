package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.PartenzaOrganizzatore
import com.example.travelapp.domain.model.PrenotatoPartenza

data class PartenzaOrganizzatoreDto(
    val disponibilitaId: Long,
    val dataInizio: String? = null,
    val dataFine: String? = null,
    val postiDisponibili: Int? = null,
    val numeroPrenotazioni: Long = 0,
    val partecipantiTotali: Long = 0
)

fun PartenzaOrganizzatoreDto.toDomain(): PartenzaOrganizzatore =
    PartenzaOrganizzatore(
        disponibilitaId = disponibilitaId,
        dataInizio = dataInizio,
        dataFine = dataFine,
        postiDisponibili = postiDisponibili,
        numeroPrenotazioni = numeroPrenotazioni,
        partecipantiTotali = partecipantiTotali
    )

/**
 * La stessa prenotazione letta dal lato di chi vende: qui interessa chi e' il viaggiatore,
 * non se il viaggio e' recensibile.
 */
fun PrenotazioneDto.toPrenotatoPartenza(): PrenotatoPartenza =
    PrenotatoPartenza(
        prenotazioneId = id,
        nome = nomeViaggiatore,
        cognome = cognomeViaggiatore,
        numeroPartecipanti = numeroPartecipanti,
        prezzoTotale = prezzoTotale,
        statoPrenotazione = statoPrenotazione,
        statoPagamento = statoPagamento,
        dataPrenotazione = dataPrenotazione
    )
