package com.apsmobile.whatsapp.ui.status

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apsmobile.whatsapp.MainGraphDirections
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.adapter.MyStatusAdapter
import com.apsmobile.whatsapp.databinding.BottomSheetChatBinding
import com.apsmobile.whatsapp.databinding.BottomSheetSelectImageProfileBinding
import com.apsmobile.whatsapp.databinding.DialogDeleteBinding
import com.apsmobile.whatsapp.databinding.FragmentStatusListBinding
import com.apsmobile.whatsapp.model.Status
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.initToolbar
import com.apsmobile.whatsapp.utils.toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.techiness.progressdialoglibrary.ProgressDialog
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class StatusListFragment : Fragment() {

    private var _binding: FragmentStatusListBinding? = null
    private val binding get() = _binding!!

    private lateinit var valueEventListener: ValueEventListener
    private var statusRef: DatabaseReference? = null

    private lateinit var progressDialog: ProgressDialog
    private lateinit var dialog: AlertDialog

    private var currentPhotoPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentStatusListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initToolbar(binding.toolbar)

        // Ouvinte de cliques
        configClicks()

        // Recupera os status do firebase
        getStatus()
    }

    // Ouvinte de cliques
    private fun configClicks() {
        binding.fabAddStatus.setOnClickListener { showBottomSheetImageStatus() }
    }

    // Recupera os status do firebase
    private fun getStatus() {
        showDialogLoading(R.string.loading_status_fragment)

        statusRef = FirebaseHelper
            .getDatabase()
            .child("status")
            .child(FirebaseHelper.getIdUser())
        valueEventListener = statusRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val statusList: MutableList<Status> = mutableListOf()

                for (ds in snapshot.children) {
                    val status = ds.getValue(Status::class.java) as Status
                    statusList.add(status)
                }

                // Inicia as configurações do adapter
                configAdapter(statusList)
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
                requireContext().toast(R.string.error_generic)
            }

        })
    }

    // Inicia as configurações do adapter
    private fun configAdapter(statusList: List<Status>) {
        binding.rvStatus.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStatus.setHasFixedSize(true)
        binding.rvStatus.adapter = MyStatusAdapter(statusList.reversed()) { status, view ->
            showMenuOption(status, view)
        }

        progressDialog.dismiss()
    }

    // Navegação Entre Fragments
    private fun showMenuOption(status: Status, view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.status_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            showDialogDelete(status)
            true
        }
        popupMenu.show()
    }

    // Exibe dialog para confirmar exclusão
    private fun showDialogDelete(status: Status) {
        val binding: DialogDeleteBinding = DialogDeleteBinding
            .inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        builder.setView(binding.root)

        binding.btnDelete.setOnClickListener {
            dialog.dismiss()
            deleteStatus(status)
        }

        binding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog = builder.create()
        dialog.show()
    }

    // Delete status do Firebase
    private fun deleteStatus(status: Status) {
        showDialogLoading(R.string.loading_delete_status_list_fragment)

        FirebaseHelper
            .getDatabase()
            .child("status")
            .child(FirebaseHelper.getIdUser())
            .child(status.id)
            .removeValue()

        FirebaseHelper.getStorage()
            .child("images")
            .child("status")
            .child(FirebaseHelper.getIdUser())
            .child("${status.id}.jpeg")
            .delete()

        progressDialog.dismiss()
    }

    private fun showDialogLoading(message: Int) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(message))
        progressDialog.theme = ProgressDialog.THEME_DARK
        progressDialog.show()
    }

    // Exibe Bottom Sheet para escolher imagem ( Câmera e Galeria )
    private fun showBottomSheetImageStatus() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialog)
        val sheetBinding: BottomSheetSelectImageProfileBinding =
            BottomSheetSelectImageProfileBinding.inflate(layoutInflater, null, false)

        sheetBinding.btnGallery.setOnClickListener {
            dialog.dismiss()
            checkPermissionGallery()
        }

        sheetBinding.btnCamera.setOnClickListener {
            dialog.dismiss()
            checkPermissionCamera()
        }

        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    // Solicita permissções de acesso a galeria
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

    // Solicita permissções de acesso a câmera
    private fun checkPermissionCamera() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                openCamera()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                requireContext().toast(R.string.permission_denied)
            }
        }
        showDialogPermission(
            permissionlistener, arrayOf(Manifest.permission.CAMERA),
            R.string.camera_permission_denied
        )
    }

    // Abre a galeria do dispositivo do usuário
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // Abre a câmera do dispositivo do usuário
    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (ex: IOException) {
            requireContext().toast(R.string.camera_error)
        }

        if (photoFile != null) {
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "com.apsmobile.whatsapp.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            cameraLauncher.launch(takePictureIntent)
        }
    }

    // Cria um arquivo foto no dispositivo
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale("pt", "BR")).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        currentPhotoPath = image.absolutePath
        return image
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val urlImage = result.data!!.data.toString()

            val action = MainGraphDirections
                .actionGlobalStatusAddFragment(urlImage)

            findNavController().navigate(action)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = File(currentPhotoPath!!)

            val urlImage = Uri.fromFile(file).toString()

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
        if (statusRef != null) statusRef?.removeEventListener(valueEventListener)
    }


}