package com.apsmobile.whatsapp.ui.auth

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
import android.provider.MediaStore.Images.Media.getBitmap
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.databinding.BottomSheetSelectImageProfileBinding
import com.apsmobile.whatsapp.databinding.FragmentRegisterBinding
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RegisterFragment : BaseFragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private var imageProfile: String? = null
    private var currentPhotoPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initToolbar(binding.toolbar)

        // Ouvinte de cliques
        listeners()
    }

    // Ouvinte de cliques
    private fun listeners() {
        binding.btnRegister.setOnClickListener {
            validateData()
        }

        binding.imgSelectPhotoUser.setOnClickListener {
            showBottomSheetImageProfile()
        }

        // Exibe / Oculta senha da EditText
        showHidePassword()
    }

    // Exibe / Oculta senha da EditText
    private fun showHidePassword() {
        var showPassword = true
        binding.ivShowHidePassword.setOnClickListener {
            if (showPassword) {
                binding.edtPassword.transformationMethod = null
                binding.ivShowHidePassword.setImageResource(R.drawable.ic_hide_password)
            } else {
                binding.edtPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.ivShowHidePassword.setImageResource(R.drawable.ic_show_password)
            }

            // Posiciona o cursor no final da EditText
            binding.edtPassword.setSelection(binding.edtPassword.length())

            showPassword = !showPassword
        }

        binding.edtPassword.addTextChangedListener { charSequence ->
            if (charSequence?.isEmpty() == true) {
                binding.ivShowHidePassword.visibility = View.GONE
            } else {
                binding.ivShowHidePassword.visibility = View.VISIBLE
            }
        }
    }

    // Valida se as informacoes foram preenchidas
    private fun validateData() {
        val name = binding.edtName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim().lowercase()
        val password = binding.edtPassword.text.toString().trim()
        val phone = binding.edtPhone.text.toString().trim()

        if (imageProfile != null) {
            if (name.isNotEmpty()) {
                if (name.isFullName()) {
                    if (email.isNotEmpty()) {
                        if (password.isNotEmpty()) {
                            if (phone.isNotEmpty()) {

                                hideKeyboard()

                                binding.progressBar.visibility = View.VISIBLE

                                val user = User(
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    password = password
                                )

                                createAccount(user)

                            } else {
                                showBottomSheetInfo(R.string.text_phone_empty_register_fragment)
                            }
                        } else {
                            showBottomSheetInfo(R.string.text_password_empty_register_fragment)
                        }
                    } else {
                        showBottomSheetInfo(R.string.text_email_empty_register_fragment)
                    }
                } else {
                    showBottomSheetInfo(R.string.text_full_name_empty_register_fragment)
                }
            } else {
                showBottomSheetInfo(R.string.text_name_empty_register_fragment)
            }
        } else {
            hideKeyboard()
            showBottomSheetInfo(R.string.image_profile_empty_register_fragment)
        }

    }

    // Salva dados do usuário no firestore
    private fun saveProfile(user: User) {
        FirebaseHelper
            .getDatabase()
            .child("users")
            .child(user.id)
            .setValue(user).addOnSuccessListener {
                findNavController().navigate(R.id.action_global_homeFragment)
                requireContext().toast(R.string.sucess_user_register_register_fragment)
            }.addOnFailureListener(requireActivity()) {
                binding.progressBar.visibility = View.GONE
                showBottomSheetInfo(R.string.error_generic)
            }
    }

    // Salva a imagem de perfil no firebase storage
    private fun saveImageProfile(user: User) {
        val storageReference = FirebaseHelper.getStorage()
            .child("images")
            .child("profile")
            .child("${FirebaseHelper.getIdUser()}.jpeg")

        val uploadTask = storageReference.putFile(Uri.parse(imageProfile))
        uploadTask.addOnSuccessListener {
            storageReference.downloadUrl.addOnCompleteListener { task ->
                user.urlProfile = task.result.toString()
                saveProfile(user)
            }
        }.addOnFailureListener(requireActivity()) {
            binding.progressBar.visibility = View.GONE
            showBottomSheetInfo(R.string.error_generic)
        }
    }

    // Salva usuario no firebase autentication
    private fun createAccount(user: User) {
        FirebaseHelper.getAuth().createUserWithEmailAndPassword(
            user.email, user.password
        ).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                // Recupera o ID do cadastro
                user.id = FirebaseHelper.getAuth().currentUser!!.uid

                saveImageProfile(user)
            } else {
                binding.progressBar.visibility = View.GONE
                showBottomSheetInfo(FirebaseHelper.validError(task.exception?.message.toString()))
            }
        }
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
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageSelected = result.data!!.data
            imageProfile = imageSelected.toString()

            if (imageSelected != null) {
                binding.imgPhotoUser.setImageBitmap(getBitmap(imageSelected))
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = File(currentPhotoPath!!)
            binding.imgPhotoUser.setImageURI(Uri.fromFile(file))

            imageProfile = file.toURI().toString()
        }
    }

    private fun getBitmap(caminhoUri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            bitmap = if (Build.VERSION.SDK_INT < 28) {
                getBitmap(requireActivity().contentResolver, caminhoUri)
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