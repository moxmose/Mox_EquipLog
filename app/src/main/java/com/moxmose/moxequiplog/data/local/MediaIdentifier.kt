package com.moxmose.moxequiplog.data.local

/**
 * Rappresenta un identificatore univoco per un media, che può essere
 * un'icona predefinita (identificata da un nome/chiave) o una foto
 * dalla galleria (identificata da un URI).
 */
sealed class MediaIdentifier {
    /**
     * Rappresenta un'icona predefinita.
     * @param name La chiave univoca dell'icona (es. "ic_bike").
     */
    data class Icon(val name: String) : MediaIdentifier()

    /**
     * Rappresenta una foto salvata.
     * @param uri L'URI della foto.
     */
    data class Photo(val uri: String) : MediaIdentifier()
}
