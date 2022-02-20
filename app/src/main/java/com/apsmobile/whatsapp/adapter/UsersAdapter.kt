package com.apsmobile.whatsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.model.LastMessages
import com.apsmobile.whatsapp.model.Message
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.GetMask
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class UsersAdapter(
    private val userList: List<User>,
    val userSelected: (User, Int) -> Unit?
) : RecyclerView.Adapter<UsersAdapter.MyViewHolder>() {

    private var usersFilter = userList.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.layout_talk, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val user = usersFilter[position]

        if (user.urlProfile.isNotEmpty()) {
            Picasso
                .get()
                .load(user.urlProfile)
                .into(holder.imgProfile)
        }

        holder.textNameUser.text = user.name

        val count = user.lastMessages?.messageList?.filter {
            !it.readTarget and (it.idUserTarget == FirebaseHelper.getIdUser())
        }

        if (count?.isNotEmpty() == true) { // Ùltima Mensagem não lida
            holder.textCountMessagesNotRead.text = count.size.toString()
            holder.textCountMessagesNotRead.visibility = View.VISIBLE
        } else { // Ùltima Mensagem lida
            holder.textCountMessagesNotRead.text = ""
            holder.textCountMessagesNotRead.visibility = View.INVISIBLE
        }

        val lastMessage = user.lastMessages?.messageList?.last()

        holder.lastMessage.text = lastMessage?.message

        if (lastMessage?.urlImage?.isNotEmpty() == true) {
            holder.lastMessage.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_photo_gallery,
                0,
                0,
                0
            )
            holder.lastMessage.text = "imagem"
        } else if (lastMessage?.contactShare != null) {
            holder.lastMessage.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_person,
                0,
                0,
                0
            )

            holder.lastMessage.text = lastMessage.contactShare?.name
        }

        if (lastMessage?.date != null) {
            holder.textHour.text = GetMask.getDate(lastMessage.date!!, 3)
        }

        holder.imgProfile.setOnClickListener { userSelected(user, 0) } // Abre dialog com foto
        holder.layoutProfile.setOnClickListener { userSelected(user, 1) } // Abre a conversa
    }

    // Recupera todas as mensagem, exibe a última e conta todas não lidas
    fun configLastMessages() {
        for (user in usersFilter) {
            FirebaseHelper
                .getDatabase()
                .child("talkRef")
                .child(FirebaseHelper.getIdUser())
                .child(user.id)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val idTalkRef = snapshot.getValue(String::class.java) as String
                        getLastMessage(idTalkRef, user.id)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
        }
    }

    // Recupera a última mensagem da conversa
    private fun getLastMessage(idTalkRef: String, userId: String) {
        FirebaseHelper
            .getDatabase()
            .child("messages")
            .child(idTalkRef)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messageList: MutableList<Message> = mutableListOf()

                    for (ds in snapshot.children) {
                        val message = ds.getValue(Message::class.java) as Message
                        messageList.add(message)
                    }

                    val users = usersFilter.filter { it.id == userId }
                    val currentUser = users[0]

                    val lastMessage = LastMessages(messageList)
                    currentUser.lastMessages = lastMessage

                    notifyItemChanged(usersFilter.indexOf(currentUser))
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    override fun getItemCount() = usersFilter.size

    fun searchUser(userName: String): Boolean {
        usersFilter.clear()
        usersFilter.addAll(userList.filter { user ->
            user.name.contains(userName, true)
        })

        notifyDataSetChanged()
        return usersFilter.isNotEmpty()
    }

    fun clearSearch() {
        usersFilter = userList.toMutableList()
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProfile: CircleImageView = itemView.findViewById(R.id.imgProfile)
        val textNameUser: TextView = itemView.findViewById(R.id.textNameUser)
        val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        val textHour: TextView = itemView.findViewById(R.id.textHour)
        val layoutProfile: ConstraintLayout = itemView.findViewById(R.id.layoutProfile)
        val textCountMessagesNotRead: TextView =
            itemView.findViewById(R.id.textCountMessagesNotRead)
    }

}