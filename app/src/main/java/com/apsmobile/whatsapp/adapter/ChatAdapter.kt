package com.apsmobile.whatsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.model.ContactShare
import com.apsmobile.whatsapp.model.Message
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.squareup.picasso.Picasso

class ChatAdapter(
    private val messageList: List<Message>,
    val contactShare: (ContactShare) -> Unit?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Mensagens de textos de quem envia e de quem recebe
    private val TYPE_MESSAGE_SENDER = 0
    private val TYPE_MESSAGE_RECIPIENT = 1

    // Mensagens de imagens de quem envia e de quem recebe
    private val TYPE_MESSAGE_IMAGE_SENDER = 2
    private val TYPE_MESSAGE_IMAGE_RECIPIENT = 3

    // Mensagens de contatos compartilhadores
    private val TYPE_MESSAGE_CONTACT_SENDER = 4
    private val TYPE_MESSAGE_CONTACT_RECIPIENT = 5

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        val itemView: RecyclerView.ViewHolder

        if (viewType != TYPE_MESSAGE_CONTACT_SENDER && viewType != TYPE_MESSAGE_CONTACT_RECIPIENT) {
            view = when (viewType) {
                TYPE_MESSAGE_SENDER -> { // Mensagem enviada de texto
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_item_sender, parent, false)
                }
                TYPE_MESSAGE_IMAGE_SENDER -> { // Mensagem enviada de imagem
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_item_img_sender, parent, false)
                }
                TYPE_MESSAGE_RECIPIENT -> { // Mensagem recebida de texto
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_item_recipient, parent, false)
                }
                TYPE_MESSAGE_CONTACT_SENDER -> {
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_share_selected_contact_send, parent, false)
                }
                TYPE_MESSAGE_CONTACT_RECIPIENT -> {
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_share_selected_contact_recipient, parent, false)
                }
                else -> { // Mensagem recebida de imagem
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_item_img_recipient, parent, false)
                }
            }
            itemView = MyViewHolder(view)
        } else {
            view = if (viewType == TYPE_MESSAGE_CONTACT_SENDER) {
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_share_selected_contact_send, parent, false)
            } else {
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_share_selected_contact_recipient, parent, false)
            }
            itemView = ContactShareViewHolder(view)
        }
        return itemView
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]

        if (holder.itemViewType != TYPE_MESSAGE_CONTACT_SENDER &&
            holder.itemViewType != TYPE_MESSAGE_CONTACT_RECIPIENT
        ) { // Mensagem normal

            val myHolder = holder as MyViewHolder

            if (message.message.isNotEmpty()) {
                myHolder.textMsg.text = message.message
            }

            if (message.urlImage.isNotEmpty()) {
                Picasso
                    .get()
                    .load(message.urlImage)
                    .into(myHolder.photoMsg)
            }

        } else { // Contato compartilhado
            val myHolder = holder as ContactShareViewHolder

            Picasso
                .get()
                .load(message.contactShare?.urlProfile)
                .into(myHolder.imgUser)

            myHolder.textName.text = message.contactShare?.name

            myHolder.itemView.setOnClickListener {
                message.contactShare?.let { it ->
                    contactShare(it)
                }
            }
        }
    }

    override fun getItemCount() = messageList.size

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return when {
            message.idUserSource == FirebaseHelper.getIdUser() -> { // Enviada
                return if (message.urlImage.isNotEmpty() && message.message.isEmpty()) { // Mensagem apenas imagem
                    TYPE_MESSAGE_IMAGE_SENDER
                } else if (message.contactShare != null) {
                    TYPE_MESSAGE_CONTACT_SENDER
                } else { // Mensagem de texto
                    TYPE_MESSAGE_SENDER
                }
            }
            message.idUserSource != FirebaseHelper.getIdUser() -> { // Recebido
                return if (message.urlImage.isNotEmpty() && message.message.isEmpty()) { // Mensagem apenas imagem
                    TYPE_MESSAGE_IMAGE_RECIPIENT
                } else if (message.contactShare != null) {
                    TYPE_MESSAGE_CONTACT_RECIPIENT
                } else { // Mensagem de texto
                    TYPE_MESSAGE_RECIPIENT
                }
            }
            else -> {
                0
            }
        }
    }

    // Infla o layout de mensagem normal
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMsg: TextView = itemView.findViewById(R.id.textMsg)
        val photoMsg: ImageView = itemView.findViewById(R.id.photoMsg)
    }

    // Infla o layout do layout de contato compartilhado
    class ContactShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.textName)
        val imgUser: ImageView = itemView.findViewById(R.id.imgUser)
    }

}