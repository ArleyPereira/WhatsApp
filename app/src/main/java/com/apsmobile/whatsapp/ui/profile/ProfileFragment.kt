package com.apsmobile.whatsapp.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.databinding.BottomSheetSelectImageProfileBinding
import com.apsmobile.whatsapp.databinding.FragmentProfileBinding
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.squareup.picasso.Picasso
import com.techiness.progressdialoglibrary.ProgressDialog
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var user: User

    private lateinit var progressDialog: ProgressDialog

    private var imageProfile: String? = null
    private var currentPhotoPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initToolbar(binding.toolbar)

        // Ouvinte de clicks
        configClicks()

        // Recupera dados do perfil no Firebase
        getProfile()
    }

    // Ouvinte de clicks
    private fun configClicks() {
        binding.btnSave.setOnClickListener { validateData() }

        binding.fabChangeImgProfile.setOnClickListener { showBottomSheetImageProfile() }
    }

    // Valida se as informacoes foram preenchidas
    private fun validateData() {
        val name = binding.edtName.text.toString().trim()
        val phone = binding.edtPhone.text.toString().trim()

        if (name.isNotEmpty()) {
            if(name.isFullName()){
                if (phone.isNotEmpty()) {

                    hideKeyboard()

                    showDialogLoading(R.string.message_save_profile_perfil_fragment)

                    user.name = name
                    user.phone = phone

                    if (imageProfile == null) {
                        saveProfile()
                    } else {
                        saveImageProfile()
                    }

                } else {
                    showBottomSheetInfo(R.string.text_phone_empty_profile_fragment)
                }
            }else {
                showBottomSheetInfo(R.string.text_full_name_empty_profile_fragment)
            }
        } else {
            showBottomSheetInfo(R.string.text_name_empty_profile_fragment)
        }

    }

    // Salva a imagem de perfil no firebase storage
    private fun saveImageProfile() {
        val storageReference = FirebaseHelper.getStorage()
            .child("images")
            .child("profile")
            .child("${FirebaseHelper.getIdUser()}.jpeg")

        val uploadTask = storageReference.putFile(Uri.parse(imageProfile))
        uploadTask.addOnSuccessListener {
            storageReference.downloadUrl.addOnCompleteListener { task ->
                user.urlProfile = task.result.toString()
                saveProfile()
            }
        }.addOnFailureListener(requireActivity()) {
            progressDialog.dismiss()
            showBottomSheetInfo(R.string.error_generic)
        }
    }

    // Recupera dados do perfil no Firebase
    private fun getProfile() {
        showDialogLoading(R.string.get_profile_perfil_fragment)

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
                    progressDialog.dismiss()
                    requireContext().toast(R.string.error_generic)
                }

            })
    }

    // Exibe as informações do perfil nos componentes
    private fun configProfile() {
        Picasso
            .get()
            .load(user.urlProfile)
            .into(binding.imgProfile)

        binding.edtName.setText(user.name)
        binding.edtPhone.setText(user.phone)
        binding.edtEmail.setText(user.email)

        progressDialog.dismiss()
    }

    // Salva os dados do usuário no firestore
    private fun saveProfile() {
        FirebaseHelper
            .getDatabase()
            .child("users")
            .child(user.id)
            .setValue(user)
            .addOnCompleteListener {
                progressDialog.dismiss()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
            }
    }

    private fun showDialogLoading(message: Int) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(message))
        progressDialog.theme = ProgressDialog.THEME_DARK
        progressDialog.show()
    }

    // Exibe Bottom Sheet Dialog para carregar imagem de perfil
    private fun showBottomSheetImageProfile() {
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

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {

            val imageSelected = result.data!!.data
            imageProfile = imageSelected.toString()

            if (imageSelected != null) {
                binding.imgProfile.setImageBitmap(getBitmap(imageSelected))
            }

        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = File(currentPhotoPath!!)
            binding.imgProfile.setImageURI(Uri.fromFile(file))

            imageProfile = file.toURI().toString()
        }
    }

    private fun getBitmap(caminhoUri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, caminhoUri)
            } else {
                val source =
                    ImageDecoder.createSource(requireActivity().contentResolver, caminhoUri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}