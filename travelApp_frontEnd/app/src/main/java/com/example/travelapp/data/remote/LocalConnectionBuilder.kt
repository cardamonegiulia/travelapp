package com.example.travelapp.data.remote

import android.annotation.SuppressLint
import android.net.Uri
import net.openid.appauth.connectivity.ConnectionBuilder
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

object LocalConnectionBuilder : ConnectionBuilder {

    @SuppressLint("BadHostnameVerifier", "TrustAllX509TrustManager")
    override fun openConnection(uri: Uri): HttpURLConnection {
        val url = URL(uri.toString())
        val connection = url.openConnection() as HttpURLConnection

        if (connection is HttpsURLConnection) {
            connection.hostnameVerifier = object : HostnameVerifier {
                override fun verify(hostname: String?, session: SSLSession?): Boolean = true
            }
        }

        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        return connection
    }
}