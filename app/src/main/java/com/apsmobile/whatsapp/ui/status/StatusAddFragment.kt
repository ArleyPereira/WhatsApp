package com.apsmobile.whatsapp.ui.status

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.databinding.FragmentStatusAddBinding
import com.apsmobile.whatsapp.model.Message
import com.apsmobile.whatsapp.model.Status
import com.apsmobile.whatsapp.ui.chat.ChatSendImageFragmentArgs
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.showBottomSheetInfo
import com.apsmobile.whatsapp.utils.toast
import com.google.firebase.database.ServerValue
import com.techiness.progressdialoglibrary.ProgressDialog

class StatusAddFragment : Fragment() {

    private val args: StatusAddFragmentArgs by navArgs()

    private var _binding: FragmentStatusAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var progressDialog: ProgressDialog
    private var urlImage: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupera as informações passadas via argumentos
        getExtras()

        // Ouvinte de clicks no componentes
        configClicks()
    }

    // Ouvinte de clicks no componentes
    private fun configClicks() {
        binding.btnClose.setOnClickListener { findNavController().popBackStack() }

        binding.btnSendImage.setOnClickListener {
            saveImageProfile()
        }
    }

    // Recupera as informações passadas via argumentos
    private fun getExtras() {
        urlImage = args.urlImage

        binding.photoMsg.setImageBitmap(getBitmap(urlImage!!.toUri()))
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

    // Salva a imagem de perfil no firebase storage
    private fun saveImageProfile() {
        showDialogLoading(R.string.save_image_status_fragment)

        val idStatus = FirebaseHelper.getDatabase().push().key.toString()

        val storageReference = FirebaseHelper.getStorage()
            .child("images")
            .child("status")
            .child(FirebaseHelper.getIdUser())
            .child("${idStatus}.jpeg")

        val uploadTask = storageReference.putFile(Uri.parse(urlImage))
        uploadTask.addOnSuccessListener {
            storageReference.downloadUrl.addOnCompleteListener { task ->
                val status = Status(idStatus, task.result.toString())
                saveStatus(status)
            }
        }.addOnFailureListener(requireActivity()) {
            progressDialog.dismiss()
            showBottomSheetInfo(R.string.error_generic)
        }
    }

    private fun saveStatus(status: Status) {
        FirebaseHelper
            .getDatabase()
            .child("status")
            .child(FirebaseHelper.getIdUser())
            .child(status.id)
            .setValue(status).addOnCompleteListener {
                progressDialog.dismiss()
                findNavController().popBackStack()
            }
    }

    private fun showDialogLoading(message: Int) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(message))
        progressDialog.theme = ProgressDialog.THEME_DARK
        progressDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}