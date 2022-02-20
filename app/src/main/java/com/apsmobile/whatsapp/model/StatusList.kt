package com.apsmobile.whatsapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StatusList (
    val list: List<Status> = listOf()
) : Parcelable