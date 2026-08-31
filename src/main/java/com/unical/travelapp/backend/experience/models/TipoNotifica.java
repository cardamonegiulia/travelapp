package com.unical.travelapp.backend.experience.models;

/**
 * Tipi di notifica in-app.
 *
 * <p>Il nome e' persistito come stringa (vedi {@link Notifica}): aggiungere un valore in
 * mezzo non deve poter cambiare il significato delle righe gia' salvate.
 */
public enum TipoNotifica {

    /** Invito a recensire un viaggio appena concluso, generato dal job giornaliero. */
    INVITO_RECENSIONE
}
