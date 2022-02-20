package com.apsmobile.whatsapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactShare(
    var id: String = "",
    var name: String = "",
    var urlProfile: String = ""
) : Parcelable