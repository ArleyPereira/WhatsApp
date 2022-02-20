package com.apsmobile.whatsapp.ui.auth

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.databinding.FragmentLoginBinding
import com.apsmobile.whatsapp.utils.BaseFragment
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.showBottomSheetInfo

class LoginFragment : BaseFragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ouvinte de clicks dos componentes de tela
        initClicks()
    }

    // Ouvinte de clicks dos componentes de tela
    private fun initClicks() {
        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (email.isNotEmpty()) {
                if (password.isNotEmpty()) {
                    hideKeyboard()

                    binding.progressBar.visibility = View.VISIBLE

                    loginApp(email, password)
                } else {
                    showBottomSheetInfo(R.string.inform_your_password)
                }
            } else {
                showBottomSheetInfo(R.string.inform_your_email)
            }
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.btnRecoverPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_recoverPasswordFragment)
        }

        // Exibe / Oculta senha da EditText
        showHidePassword()
    }

    // Efetua login no app pelo firebase autentication
    private fun loginApp(email: String, password: String) {
        FirebaseHelper.getAuth().signInWithEmailAndPassword(
            email, password
        ).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                findNavController().navigate(R.id.action_global_homeFragment)
            } else {
                binding.progressBar.visibility = View.GONE

                showBottomSheetInfo(
                    FirebaseHelper.validError(task.exception?.message.toString())
                )
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}