package com.apsmobile.whatsapp.model

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
        var id: String = "",
        var name: String = "",
        var email: String = "",
        var phone: String = "",
        var urlProfile: String = "",
        @get:Exclude
        var password: String = "",
        @get:Exclude
        var statusList: List<Status> = listOf(),
        @get:Exclude
        var lastMessages: LastMessages? = null
) : Parcelable