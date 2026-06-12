package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R // Pense à vérifier que ton package de R est correct

@Composable
fun LynxLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo_lynx),
        contentDescription = "Logo Lynx",
        modifier = modifier.aspectRatio(1f)
    )
}