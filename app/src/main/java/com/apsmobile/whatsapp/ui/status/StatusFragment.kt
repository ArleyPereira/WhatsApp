package com.apsmobile.whatsapp.ui.status

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apsmobile.whatsapp.MainGraphDirections
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.adapter.StatusAdapter
import com.apsmobile.whatsapp.databinding.FragmentStatusBinding
import com.apsmobile.whatsapp.model.Status
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.squareup.picasso.Picasso

class StatusFragment : Fragment() {

    private var _binding: FragmentStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var user: User

    private var statusList: MutableList<Status> = mutableListOf()
    private var usersList: MutableList<User> = mutableListOf()
    private var idsUsers: MutableList<String> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ouvinte de cliques
        configClicks()
    }

    override fun onResume() {
        super.onResume()

        // Recupera dados do perfil no Firebase
        getProfile()

        // Recupera os ids dos usuários
        getIdsUsersStatus()
    }

    // Ouvinte de cliques
    private fun configClicks() {
        binding.textStatus.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_statusListFragment)
        }

        binding.imgAddStatus.setOnClickListener { checkPermissionGallery() }
    }

    private fun checkPermissionGallery() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                openGallery()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                requireContext().toast(R.string.permission_denied)
            }
        }
        showDialogPermission(
            permissionlistener, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            R.string.gallery_permission_denied
        )
    }

    // Recupera os ids dos usuários
    private fun getIdsUsersStatus() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseHelper
            .getDatabase()
            .child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children) {
                        val idUser = ds.key.toString()
                        if (idUser != FirebaseHelper.getIdUser() && !idsUsers.contains(idUser)) {
                            idsUsers.add(idUser)
                        }
                    }

                    if (idsUsers.isNotEmpty()) {
                        // Recupera dados dos usuários dos status
                        getProfileUsersStatus()
                    } else {
                        binding.progressBar.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    requireContext().toast(R.string.error_generic)
                }

            })
    }

    // Recupera dados dos usuários dos status
    private fun getProfileUsersStatus() {
        usersList.clear()
        for (idUser in idsUsers) {
            FirebaseHelper
                .getDatabase()
                .child("users")
                .child(idUser)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java) as User

                        if (!usersList.contains(user)) {
                            usersList.add(user)
                        }

                        if (usersList.size == idsUsers.size) {
                            // Associa as informações do usuário com os status recuperados
                            associateData()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        binding.progressBar.visibility = View.GONE
                        requireContext().toast(R.string.error_generic)
                    }

                })
        }
    }

    // Associa as informações do usuário com os status recuperados
    private fun associateData() {
        val userListAltered: MutableList<User> = mutableListOf()
        val statusListAltered: MutableList<Status> = mutableListOf()

        for (user in usersList) {
            for (status in statusList) {
                if (status.idUser == user.id) {
                    statusListAltered.add(status)
                }
            }

            user.statusList = statusListAltered
            userListAltered.add(user)
            statusListAltered.clear()
        }

        // Inicia as configurações do adapter
        configAdapter(userListAltered)

        binding.progressBar.visibility = View.GONE
    }

    // Inicia as configurações do adapter
    private fun configAdapter(userList: List<User>) {
        binding.rvStatus.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStatus.setHasFixedSize(true)
        binding.rvStatus.adapter = StatusAdapter(userList.reversed()) { user ->
            val action = MainGraphDirections
                .actionGlobalViewStatusFragment(user)
            findNavController().navigate(action)
        }
    }

    // Recupera dados do perfil no Firebase
    private fun getProfile() {
        FirebaseHelper
            .getDatabase()
            .child("users")
            .child(FirebaseHelper.getIdUser())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java) as User
                    configProfile()
                }

                override fun onCancelled(error: DatabaseError) {
                    requireContext().toast(R.string.error_generic)
                }

            })
    }

    // Exibe as informações do perfil nos componentes
    private fun configProfile() {
        if (user.urlProfile.isNotEmpty()) {
            Picasso
                .get()
                .load(user.urlProfile)
                .into(binding.imgPhotoUser)
        } else {
            binding.imgPhotoUser.setImageResource(R.drawable.ic_user_round)
        }

        binding.progressBar.visibility = View.GONE
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val urlImage = result.data!!.data.toString()

            val action = MainGraphDirections
                .actionGlobalStatusAddFragment(urlImage)

            findNavController().navigate(action)
        }
    }

    private fun showDialogPermission(
        permissionListener: PermissionListener,
        permissions: Array<String>,
        msg: Int
    ) {
        TedPermission.create()
            .setPermissionListener(permissionListener)
            .setDeniedTitle(R.string.permission_denied)
            .setDeniedMessage(msg)
            .setDeniedCloseButtonText("Não")
            .setGotoSettingButtonText("Sim")
            .setPermissions(*permissions)
            .check()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}