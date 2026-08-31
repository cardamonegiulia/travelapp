package com.example.travelapp.ui.profilo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.DatiToken
import com.example.travelapp.data.remote.GestoreSessione
import com.example.travelapp.data.repository.UtenteRepository
import com.example.travelapp.domain.model.Utente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfiloViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        UtenteRepository(application)

    private val _state =
        MutableStateFlow(ProfiloUiState())

    val state: StateFlow<ProfiloUiState> =
        _state.asStateFlow()

    init {
        caricaProfilo()
    }

    /*
     * Recupera prima i dati disponibili nel JWT
     * e successivamente il profilo completo dal backend.
     *
     * I dati del backend restano la fonte autorevole
     * per id, foto profilo e tema.
     */
    fun caricaProfilo() {

        _state.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {

            /*
             * Fallback locale dal token.
             */
            GestoreSessione
                .datiUtenteDalToken(
                    getApplication()
                )
                ?.let { dati ->

                    _state.update {
                        it.conToken(dati)
                    }
                }

            /*
             * Profilo reale dal backend.
             */
            repository
                .caricaProfilo()
                .onSuccess { utente ->

                    _state.update {

                        it.conProfilo(
                            utente
                        ).copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { errore ->

                    /*
                     * Se il backend fallisce manteniamo
                     * comunque i dati recuperati dal token.
                     */
                    val datiToken =
                        GestoreSessione
                            .datiUtenteDalToken(
                                getApplication()
                            )

                    if (datiToken != null) {

                        _state.update {

                            it.conToken(
                                datiToken
                            ).copy(
                                isLoading = false,
                                errorMessage = null
                            )
                        }

                    } else {

                        _state.update {

                            it.copy(
                                isLoading = false,
                                errorMessage =
                                    errore.message
                                        ?: "Errore nel caricamento del profilo"
                            )
                        }
                    }
                }
        }
    }

    /*
     * ============================================================
     * FOTO PROFILO
     * ============================================================
     */

    fun cambiaFotoProfilo(
        uri: Uri
    ) {

        val precedente =
            _state.value.avatarUrl

        _state.update {

            it.copy(
                avatarUrl =
                    uri.toString(),
                isPhotoUploading =
                    true,
                photoMessage =
                    null
            )
        }

        viewModelScope.launch {

            repository
                .impostaFotoProfilo(uri)
                .onSuccess { utente ->

                    _state.update {

                        it.conProfilo(
                            utente
                        ).copy(
                            isPhotoUploading =
                                false,
                            photoMessage =
                                "Foto profilo aggiornata"
                        )
                    }
                }
                .onFailure { errore ->

                    _state.update {

                        it.copy(
                            avatarUrl =
                                precedente,
                            isPhotoUploading =
                                false,
                            photoMessage =
                                errore.message
                                    ?: "Caricamento non riuscito"
                        )
                    }
                }
        }
    }

    fun rimuoviFotoProfilo() {

        val precedente =
            _state.value.avatarUrl

        _state.update {

            it.copy(
                avatarUrl = null,
                isPhotoUploading = true,
                photoMessage = null
            )
        }

        viewModelScope.launch {

            repository
                .rimuoviFotoProfilo()
                .onSuccess {

                    _state.update {

                        it.copy(
                            isPhotoUploading =
                                false,
                            photoMessage =
                                "Foto profilo rimossa"
                        )
                    }
                }
                .onFailure { errore ->

                    _state.update {

                        it.copy(
                            avatarUrl =
                                precedente,
                            isPhotoUploading =
                                false,
                            photoMessage =
                                errore.message
                                    ?: "Rimozione non riuscita"
                        )
                    }
                }
        }
    }

    /*
     * ============================================================
     * DARK MODE
     * ============================================================
     */

    fun cambiaTemaScuro(
        attivo: Boolean
    ) {

        val precedente =
            _state.value.isDarkModeEnabled

        val id =
            _state.value.id

        /*
         * Aggiornamento immediato della UI.
         */
        _state.update {

            it.copy(
                isDarkModeEnabled =
                    attivo
            )
        }

        /*
         * Se il backend non ci ha ancora restituito
         * l'id dell'utente non possiamo salvare il tema.
         */
        if (id == null) {
            return
        }

        viewModelScope.launch {

            repository
                .aggiornaTema(
                    id,
                    attivo
                )
                .onFailure {

                    /*
                     * Rollback in caso di errore.
                     */
                    _state.update {

                        it.copy(
                            isDarkModeEnabled =
                                precedente
                        )
                    }
                }
        }
    }

    /*
     * ============================================================
     * MESSAGGI
     * ============================================================
     */

    fun messaggioMostrato() {

        _state.update {

            it.copy(
                photoMessage =
                    null
            )
        }
    }

    /*
     * ============================================================
     * MAPPING PROFILO BACKEND
     * ============================================================
     */

    private fun ProfiloUiState.conProfilo(
        utente: Utente
    ) = copy(

        id =
            utente.id,

        name =
            utente.nomeCompleto,

        email =
            utente.email,

        ruolo =
            utente.ruolo
                ?: ruolo,

        avatarUrl =
            utente.fotoProfiloUrl,

        isDarkModeEnabled =
            utente.tema
                ?.equals(
                    "SCURO",
                    ignoreCase = true
                )
                ?: isDarkModeEnabled
    )

    /*
     * ============================================================
     * MAPPING JWT
     * ============================================================
     *
     * Il token non contiene foto o tema.
     * Questi campi rimangono quindi invariati fino
     * alla risposta del backend.
     */

    private fun ProfiloUiState.conToken(
        dati: DatiToken
    ) = copy(

        name =
            dati.nomeCompleto
                .ifBlank {
                    name
                },

        email =
            dati.email
                .ifBlank {
                    email
                },

        ruolo =
            dati.ruolo
    )
}