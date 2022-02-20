package com.apsmobile.whatsapp.model

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
data class Status (
    val id: String = "",

    val urlImagem: String = "",

    @get:Exclude
    var idUser: String = ""
) : Parcelable