package com.apsmobile.whatsapp.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apsmobile.whatsapp.MainGraphDirections
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.adapter.UsersAdapter
import com.apsmobile.whatsapp.databinding.DialogShowProfileUserBinding
import com.apsmobile.whatsapp.databinding.FragmentTalksBinding
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.showBottomSheetInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class TalksFragment : Fragment() {

    private var _binding: FragmentTalksBinding? = null
    private val binding get() = _binding!!

    private var talksRef: DatabaseReference? = null
    private lateinit var eventListener: ValueEventListener

    private var usersAdapter: UsersAdapter? = null
    private var userList: MutableList<User> = mutableListOf()

    private lateinit var dialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTalksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Recupera todas as referências das conversas do usuário
        getIdsUsers()

        // Ouvinte de clicks
        configClicks()
    }

    // Ouvinte de clicks
    private fun configClicks() {
        binding.fabContacts.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_contactsFragment)
        }

        binding.swipe.setOnRefreshListener {
            getIdsUsers()
        }
    }

    // Recupera os ids dos usuárarios com qual o usuário logado tem uma conversa
    private fun getIdsUsers() {
        talksRef = FirebaseHelper
            .getDatabase()
            .child("talkRef")
            .child(FirebaseHelper.getIdUser())
        eventListener = talksRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val idsUsers: MutableList<String> = mutableListOf()
                    for (ds in snapshot.children) {
                        idsUsers.add(ds.key.toString())
                    }

                    // Recupera os usuários de todas as conversas que o usuário logado tem
                    getUsers(idsUsers)
                } else {
                    binding.textInfo.text = getText(R.string.talks_list_empty_talks_fragment)
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showBottomSheetInfo(R.string.error_generic)
            }
        })
    }

    // Recupera os usuários de todas as conversas que o usuário logado tem
    private fun getUsers(idsUsers: List<String>) {
        for (userId in idsUsers) {
            FirebaseHelper
                .getDatabase()
                .child("users")
                .child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java) as User
                        userList.add(user)

                        if (userList.size == idsUsers.size) {
                            binding.progressBar.visibility = View.GONE
                            binding.textInfo.text = ""

                            initAdapter()

                            if (binding.swipe.isRefreshing) binding.swipe.isRefreshing = false
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheetInfo(R.string.error_generic)
                    }
                })
        }
    }

    // Exibe bottom sheet com informações do perfil
    private fun showDialogProfile(user: User) {
        val binding: DialogShowProfileUserBinding = DialogShowProfileUserBinding
            .inflate(LayoutInflater.from(context))

        binding.textNameUser.text = user.name

        binding.btnChat.setOnClickListener {
            dialog.dismiss()

            val action = MainGraphDirections
                .actionGlobalChatBetweenUsersFragment(user)

            findNavController().navigate(action)
        }

        if (user.urlProfile.isNotEmpty()) {
            Picasso
                .get()
                .load(user.urlProfile)
                .into(binding.imgProfile)
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding.root)

        dialog = builder.create()
        dialog.show()
    }

    fun searchUser(userName: String) {
        if (usersAdapter != null) {
            if (userName.isNotEmpty() && userName.isNotBlank()) {
                usersAdapter!!.searchUser(userName)
            } else {
                usersAdapter!!.clearSearch()
            }
        }
    }

    // Inicia as configurações do adapter
    private fun initAdapter() {
        binding.rvTalks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTalks.setHasFixedSize(true)
        usersAdapter = UsersAdapter(userList) { user, acess ->
            if (acess == 1) { // Acessa a conversa
                val action = MainGraphDirections
                    .actionGlobalChatBetweenUsersFragment(user)

                findNavController().navigate(action)
            } else { // Exibe dialog de perfil
                showDialogProfile(user)
            }
        }
        binding.rvTalks.adapter = usersAdapter
        usersAdapter?.configLastMessages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (talksRef != null) talksRef!!.removeEventListener(eventListener)
        _binding = null
    }

}