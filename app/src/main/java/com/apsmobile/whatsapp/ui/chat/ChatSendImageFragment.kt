package com.apsmobile.whatsapp.ui.chat

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
import com.apsmobile.whatsapp.databinding.FragmentChatSendImageBinding
import com.apsmobile.whatsapp.model.Message
import com.apsmobile.whatsapp.model.Talk
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.showBottomSheetInfo
import com.google.firebase.database.ServerValue
import com.techiness.progressdialoglibrary.ProgressDialog

class ChatSendImageFragment : Fragment() {

    private var _binding: FragmentChatSendImageBinding? = null
    private val binding get() = _binding!!

    private val args: ChatSendImageFragmentArgs by navArgs()

    private lateinit var progressDialog: ProgressDialog

    private var talk: Talk? = null
    private var urlImage: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatSendImageBinding.inflate(inflater, container, false)
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
            if (urlImage != null) {
                saveImageProfile()
            }
        }
    }

    // Recupera as informações passadas via argumentos
    private fun getExtras() {
        urlImage = args.urlImage
        talk = args.talk

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
        showDialogLoading(R.string.message_save_image_fragment)

        val storageReference = FirebaseHelper.getStorage()
            .child("images")
            .child("talks")
            .child(talk!!.id)
            .child("${FirebaseHelper.getDatabase().push().key}.jpeg")

        val uploadTask = storageReference.putFile(Uri.parse(urlImage))
        uploadTask.addOnSuccessListener {
            storageReference.downloadUrl.addOnCompleteListener { task ->
                saveMessage(task.result.toString())
            }
        }.addOnFailureListener(requireActivity()) {
            progressDialog.dismiss()
            showBottomSheetInfo(R.string.error_generic)
        }
    }

    private fun saveMessage(urlImage: String) {
        val message = Message(
            id = FirebaseHelper.getDatabase().push().key.toString(),
            idUserTarget = talk!!.idTargetUser,
            urlImage = urlImage
        )

        val talkRef = FirebaseHelper
            .getDatabase()
            .child("talks")
            .child(talk!!.id)
        talkRef.setValue(talk)

        val messageRef = FirebaseHelper
            .getDatabase()
            .child("messages")
            .child(talk!!.id)
            .child(message.id)
        messageRef.setValue(message)

        val messageUpdate = messageRef
            .child("date")
        messageUpdate.setValue(ServerValue.TIMESTAMP)

        progressDialog.dismiss()
        findNavController().popBackStack()
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