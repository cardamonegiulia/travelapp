package com.example.travelapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.travelapp.data.remote.ApiClient

class TravelApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .callFactory { ApiClient.getHttpClient(this) }
            .crossfade(true)
            .build()
}
