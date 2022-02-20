package com.apsmobile.whatsapp.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.apsmobile.whatsapp.MainGraphDirections
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.adapter.ChatAdapter
import com.apsmobile.whatsapp.databinding.BottomSheetChatBinding
import com.apsmobile.whatsapp.databinding.FragmentChatBetweenUsersBinding
import com.apsmobile.whatsapp.model.ContactShare
import com.apsmobile.whatsapp.model.Message
import com.apsmobile.whatsapp.model.Talk
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.initToolbar
import com.apsmobile.whatsapp.utils.showBottomSheetInfo
import com.apsmobile.whatsapp.utils.toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.apsmobile.whatsapp.ui.contacts.ShareSelectedContactFragment

class ChatBetweenUsersFragment : Fragment() {

    private var _binding: FragmentChatBetweenUsersBinding? = null
    private val binding get() = _binding!!

    private val args: ChatBetweenUsersFragmentArgs by navArgs()
    private lateinit var user: User
    private var contactShareSelected: User? = null

    private var messageList: MutableList<Message> = mutableListOf()
    private val talkRefList: MutableList<String> = mutableListOf()

    private var talk: Talk? = null

    private lateinit var chatAdapter: ChatAdapter

    private lateinit var childEventListener: ChildEventListener
    private var messageRef: DatabaseReference? = null

    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBetweenUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Inicia as configurações da toolbar
        initToolbar(binding.toolbar)

        // Recupera o objeto usuário selecionado
        getExtra()

        // Inicia componentes de tela
        initComponents()

        // Ouvinte de clicks
        configClicks()

        // Recupera a referência da conversa com o usuário selecionado
        getTalkRef()

        // Recupera o usuário selecionado para compartilhamento
        listenerContactShare()
    }

    // Ouvinte de clicks
    private fun configClicks() {
        binding.fabSend.setOnClickListener { sendMessage() }

        binding.imgOption.setOnClickListener { showBottomSheetInfo() }
    }

    // Salva a mensagem no firestore
    private fun sendMessage() {
        if (binding.edtMessage.text.isNotEmpty() || contactShareSelected != null) {
            var messageSent = binding.edtMessage.text.toString().trim()

            var contactShare: ContactShare? = null

            // Caso algum contato tenha sido selecionado o objeto será criado sem mensagem
            if (contactShareSelected != null) {
                messageSent = ""

                contactShare = ContactShare(
                    contactShareSelected!!.id,
                    contactShareSelected!!.name,
                    contactShareSelected!!.urlProfile
                )
            }

            binding.edtMessage.text.clear()

            val message = Message(
                id = FirebaseHelper.getDatabase().push().key.toString(),
                idUserTarget = user.id,
                message = messageSent,
                urlImage = "",
                contactShare = contactShare
            )

            val talkRef = FirebaseHelper
                .getDatabase()
                .child("talks")
                .child(talk!!.id)
            talkRef.setValue(talk)

            // Salva a última menasgem no firebase
            saveLastMessage(message.id)

            val messageRef = FirebaseHelper
                .getDatabase()
                .child("messages")
                .child(talk!!.id)
                .child(message.id)
            messageRef.setValue(message)

            val messageUpdate = messageRef
                .child("date")
            messageUpdate.setValue(ServerValue.TIMESTAMP)

            talk?.id?.let { if (!talkRefList.contains(talk?.id)) talkRefList.add(it) }
            FirebaseHelper
                .getDatabase()
                .child("talkRef")
                .child(FirebaseHelper.getIdUser())
                .child(user.id)
                .setValue(talk!!.id)

            FirebaseHelper
                .getDatabase()
                .child("talkRef")
                .child(user.id)
                .child(FirebaseHelper.getIdUser())
                .setValue(talk!!.id)
        }
    }

    // Salva a última menasgem no firebase
    private fun saveLastMessage(idLastMessage: String) {
        val messageRef1 = FirebaseHelper
            .getDatabase()
            .child("lastMessage")
            .child(FirebaseHelper.getIdUser())
            .child(user.id)
        messageRef1.setValue(idLastMessage)

        val messageRef2 = FirebaseHelper
            .getDatabase()
            .child("lastMessage")
            .child(user.id)
            .child(FirebaseHelper.getIdUser())
        messageRef2.setValue(idLastMessage)
    }

    // Recupera a referência da conversa com o usuário selecionado
    private fun getTalkRef() {
        FirebaseHelper
            .getDatabase()
            .child("talkRef")
            .child(FirebaseHelper.getIdUser())
            .child(user.id)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val talkId = snapshot.getValue(String::class.java) as String

                        // Recupera os dados da conversa
                        getTalk(talkId)

                        // Inicia as configurações do adapter
                        initAdapter()
                    } else {
                        binding.progressBar.visibility = View.GONE

                        talk = Talk(
                            id = FirebaseHelper.getDatabase().push().key.toString(),
                            idSourceUser = FirebaseHelper.getIdUser(),
                            idTargetUser = user.id
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheetInfo(R.string.error_generic)
                }
            })
    }

    // Recupera os dados da conversa
    private fun getTalk(talkId: String) {
        FirebaseHelper
            .getDatabase()
            .child("talks")
            .child(talkId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    talk = snapshot.getValue(Talk::class.java) as Talk

                    // Recupera as mensagens da conversa
                    getMessages(talkId)
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheetInfo(R.string.error_generic)
                }
            })
    }

    // Recupera as mensagens da conversa
    private fun getMessages(talkId: String) {
        messageRef = FirebaseHelper
            .getDatabase()
            .child("messages")
            .child(talkId)
        childEventListener = messageRef!!.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java) as Message

                if (message.idUserTarget == FirebaseHelper.getIdUser() && !message.readTarget) {
                    // Marcar as menagens como lida
                    setMessagesIsRead(message)
                }

                binding.rvChat.scrollToPosition(chatAdapter.itemCount)

                if (!messageList.contains(message)) messageList.add(message)
                chatAdapter.notifyItemInserted(messageList.size)

                binding.progressBar.visibility = View.GONE
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java) as Message
                messageList[messageList.size - 1] = message
                chatAdapter.notifyItemChanged(messageList.size)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    // Marcar as menagens como lida
    private fun setMessagesIsRead(message: Message) {
        val childUpdates = hashMapOf("readTarget" to true)

        FirebaseHelper
            .getDatabase()
            .child("messages")
            .child(talk!!.id)
            .child(message.id)
            .updateChildren(childUpdates as Map<String, Any>)
    }

    // Exibe bottom sheet para compartilhar ( Imagens e Contatos )
    private fun showBottomSheetInfo() {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialog)
        val bottomSheetBinding: BottomSheetChatBinding =
            BottomSheetChatBinding.inflate(layoutInflater, null, false)

        bottomSheetBinding.btnGallery.setOnClickListener {
            bottomSheetDialog.dismiss()
            checkPermissionGallery()
        }

        bottomSheetBinding.btnCamera.setOnClickListener {
            bottomSheetDialog.dismiss()
            checkPermissionCamera()
        }

        bottomSheetBinding.btnContact.setOnClickListener {
            bottomSheetDialog.dismiss()

            val action = ChatBetweenUsersFragmentDirections
                .actionChatBetweenUsersFragmentToShareSelectedContactFragment(user.id)

            findNavController().navigate(action)
        }

        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        bottomSheetDialog.show()
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

            val action = talk?.let {
                ChatBetweenUsersFragmentDirections
                    .actionChatBetweenUsersFragmentToChatSendImageFragment(urlImage, it)
            }

            if (action != null) {
                findNavController().navigate(action)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = File(currentPhotoPath!!)

            val urlImage = Uri.fromFile(file).toString()

            val action = talk?.let {
                ChatBetweenUsersFragmentDirections
                    .actionChatBetweenUsersFragmentToChatSendImageFragment(urlImage, it)
            }

            if (action != null) {
                findNavController().navigate(action)
            }
        }
    }

    // Recupera o usuário selecionado para compartilhamento
    private fun listenerContactShare() {
        parentFragmentManager.setFragmentResultListener(ShareSelectedContactFragment.REQUEST_KEY,
            this,
            { key, bundle ->
                contactShareSelected =
                    bundle.getParcelable(ShareSelectedContactFragment.REQUEST_KEY)

                if (contactShareSelected != null) {
                    sendMessage()
                }
            })
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

    override fun onDestroy() {
        super.onDestroy()
        if (messageRef != null) messageRef?.removeEventListener(childEventListener)
    }

    // Inicia as configurações do adapter
    private fun initAdapter() {
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.setHasFixedSize(true)
        chatAdapter = ChatAdapter(messageList) { contactShare ->
            getUser(contactShare.id)
        }
        binding.rvChat.adapter = chatAdapter

        binding.rvChat.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                binding.rvChat.postDelayed({
                    binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }, 100)
            }
        }
    }

    // Recupera os dados do usuários compartilhado clicado
    private fun getUser(idUser: String) {
        FirebaseHelper
            .getDatabase()
            .child("users")
            .child(idUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java) as User

                    val action = MainGraphDirections
                        .actionGlobalChatBetweenUsersFragment(user)

                    findNavController().navigate(action)
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheetInfo(R.string.error_generic)
                }
            })
    }

    // Recupera o objeto usuário selecionado
    private fun getExtra() {
        user = args.user

        configProfile()
    }

    // Inicia componentes de tela
    private fun initComponents() {
        binding.edtMessage.addTextChangedListener { text: Editable? ->
            if (text.toString().isNotEmpty()) {
                binding.fabSend.setImageResource(R.drawable.ic_send)
            } else {
                binding.fabSend.setImageResource(R.drawable.ic_mic)
            }
        }
    }

    // Exibe as informações do perfil nos componentes
    private fun configProfile() {
        Picasso
            .get()
            .load(user.urlProfile)
            .into(binding.imgProfile)

        binding.titleToolbar.text = user.name
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_chat_user, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

}