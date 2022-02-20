package com.apsmobile.whatsapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LastMessages(
    val messageList: List<Message> = listOf()
) : Parcelable