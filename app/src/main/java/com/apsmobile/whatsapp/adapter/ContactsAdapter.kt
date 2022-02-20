package com.apsmobile.whatsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.model.User
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ContactsAdapter(
    private val userList: List<User>,
    val userSelected: (User) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.MyViewHolder>() {

    private var usersFilter = userList.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.layout_contact, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val user = usersFilter[position]

        Picasso
            .get()
            .load(user.urlProfile)
            .into(holder.imgProfile)

        holder.txtNameUser.text = user.name

        holder.itemView.setOnClickListener { userSelected(user) }
    }

    override fun getItemCount() = usersFilter.size

    fun search(query: String): Boolean {
        usersFilter.clear()
        usersFilter.addAll(userList.filter { user ->
            user.name.contains(query, true)
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
        val txtNameUser: TextView = itemView.findViewById(R.id.txtNameUser)
    }

}