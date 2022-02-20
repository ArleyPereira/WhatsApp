package com.apsmobile.whatsapp.model

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
data class Talk(
    var id: String = "",
    var idSourceUser: String = "",
    var idTargetUser: String = "",
    var deleted: Int = 0, // caso 0 deleta apenas do usuário logado, caso 1 deleta dos dois usuários
    var lastMessage: Message? = null,
    @get:Exclude
    var countMessagesNotRead: Int = 0
) : Parcelable