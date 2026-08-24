package com.example.travelapp.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.travelapp.R

@Composable
fun CaricamentoLottie(
    modifier: Modifier = Modifier,
    dimensione: Dp = 48.dp,
    @RawRes animazione: Int = R.raw.loading
) {
    val composizione by rememberLottieComposition(LottieCompositionSpec.RawRes(animazione))
    val progresso by animateLottieCompositionAsState(
        composizione,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composizione,
        progress = { progresso },
        modifier = modifier.size(dimensione)
    )
}
