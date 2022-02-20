package com.apsmobile.whatsapp.model

import android.os.Parcelable
import com.apsmobile.whatsapp.utils.FirebaseHelper
import kotlinx.parcelize.Parcelize

@Parcelize
data class Message(
    var id: String = "",
    var idUserSource: String = FirebaseHelper.getIdUser(),
    var idUserTarget: String = "",
    var date: Long? = null,
    var message: String = "",
    var urlImage: String = "",
    var readSource: Boolean = true,
    var readTarget: Boolean = false,
    var contactShare: ContactShare? = null
) : Parcelable