package com.apsmobile.whatsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.model.Status
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class MyStatusAdapter(
    private val statusList: List<Status>,
    val statusSelected: (Status, View) -> Unit
) : RecyclerView.Adapter<MyStatusAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_item_my_status, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val status = statusList[position]

        Picasso
            .get()
            .load(status.urlImagem)
            .into(holder.imgStatus)

        holder.btnOption.setOnClickListener { statusSelected(status, holder.btnOption) }
    }

    override fun getItemCount() = statusList.size

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgStatus: CircleImageView = itemView.findViewById(R.id.imgStatus)
        val btnOption: ImageView = itemView.findViewById(R.id.btnOption)
    }

}