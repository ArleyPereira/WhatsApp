package com.apsmobile.whatsapp.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.databinding.FragmentViewStatusBinding
import com.apsmobile.whatsapp.model.Status
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.showBottomSheetInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import jp.shts.android.storiesprogressview.StoriesProgressView

class ViewStatusFragment : Fragment(), StoriesProgressView.StoriesListener {

    private var counter = 0

    private val statusList: MutableList<Status> = mutableListOf()

    private lateinit var storiesProgressView: StoriesProgressView

    private val args: ViewStatusFragmentArgs by navArgs()

    private var _binding: FragmentViewStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupera as informações passadas via argumentos
        getExtras()
    }

    // Recupera as informações passadas via argumentos
    private fun getExtras() {
        user = args.user

        // Recupera os status do usuário selecionado
        getStatus()
    }

    // Recupera os status do usuário selecionado
    private fun getStatus() {
        FirebaseHelper
            .getDatabase()
            .child("status")
            .child(user.id)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children) {
                        val status = ds.getValue(Status::class.java) as Status
                        statusList.add(status)
                    }

                    // Configurações iniciais do Stories Progress View()
                    configStoriesProgressView()
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheetInfo(R.string.error_generic)
                }

            })
    }

    // Configurações iniciais do Stories Progress View()
    private fun configStoriesProgressView() {
        storiesProgressView = binding.stories
        storiesProgressView.setStoriesCount(statusList.size)
        storiesProgressView.setStoryDuration(3000L)
        storiesProgressView.setStoriesListener(this)
        storiesProgressView.startStories(counter)

        Picasso
            .get()
            .load(statusList[counter].urlImagem)
            .into(binding.imageStatus)

        binding.back.setOnClickListener { storiesProgressView.reverse() }
        binding.next.setOnClickListener { storiesProgressView.skip() }
    }

    override fun onNext() {
        Picasso
            .get()
            .load(statusList[++counter].urlImagem)
            .into(binding.imageStatus)
    }

    override fun onPrev() {
        if (counter - 1 < 0) return
        Picasso
            .get()
            .load(statusList[--counter].urlImagem)
            .into(binding.imageStatus)
    }

    override fun onComplete() {
        findNavController().popBackStack()
    }

    override fun onDestroy() {
        storiesProgressView.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}