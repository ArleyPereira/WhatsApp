package com.apsmobile.whatsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.model.Status
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class StatusAdapter(
    private val userLst: List<User>,
    val userSelected: (User) -> Unit?
) : RecyclerView.Adapter<StatusAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_item_public_status, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val user = userLst[position]

        // Recupera os status dos usuários
        getStatus(user, holder)

        holder.textNameUser.text = user.name

        if (user.urlProfile.isNotEmpty()) {
            Picasso
                .get()
                .load(user.urlProfile)
                .into(holder.imgProfile)
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_user_round)
        }

        holder.itemView.setOnClickListener { userSelected(user) }
    }

    // Recupera os status dos usuários
    private fun getStatus(user: User, holder: MyViewHolder) {
        FirebaseHelper
            .getDatabase()
            .child("status")
            .child(user.id)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val statusList: MutableList<Status> = mutableListOf()

                    for (ds in snapshot.children) {
                        val status = ds.getValue(Status::class.java) as Status
                        statusList.add(status)
                    }

                    user.statusList = statusList
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }

    override fun getItemCount() = userLst.size

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProfile: CircleImageView = itemView.findViewById(R.id.imgProfile)
        val textNameUser: TextView = itemView.findViewById(R.id.textNameUser)
        val textHour: TextView = itemView.findViewById(R.id.textHour)
    }

}