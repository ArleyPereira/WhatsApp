package com.apsmobile.whatsapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.databinding.FragmentRecoverPasswordBinding
import com.apsmobile.whatsapp.utils.*

class RecoverPasswordFragment : BaseFragment() {

    private var _binding: FragmentRecoverPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecoverPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initToolbar(binding.toolbar)

        // Ouvinte de clicks dos componentes de tela
        initClicks()

    }

    // Ouvinte de clicks dos componentes de tela
    private fun initClicks() {
        binding.btnRecover.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()

            if (email.isNotEmpty()) {

                hideKeyboard()

                binding.progressBar.visibility = View.VISIBLE

                sendEmail(email)
            } else {
                showBottomSheetInfo(R.string.inform_your_email)
            }
        }
    }

    // Envia link para o e-mail informado
    private fun sendEmail(email: String) {
        FirebaseHelper
            .getAuth()
            .sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    requireContext().toast(R.string.text_recover_password_fragment)
                } else {
                    showBottomSheetInfo(FirebaseHelper.validError(task.exception?.message!!))
                }
                binding.progressBar.visibility = View.GONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}