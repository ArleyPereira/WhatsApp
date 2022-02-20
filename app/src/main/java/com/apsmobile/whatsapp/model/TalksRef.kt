package com.apsmobile.whatsapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TalksRef (
    var talks: List<String> = listOf()
) : Parcelable