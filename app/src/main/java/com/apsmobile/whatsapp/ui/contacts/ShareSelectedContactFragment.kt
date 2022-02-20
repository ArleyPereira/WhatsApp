package com.apsmobile.whatsapp.ui.contacts

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.adapter.ContactsAdapter
import com.apsmobile.whatsapp.databinding.FragmentShareSelectedContactBinding
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.BaseFragment
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.initToolbar
import com.ferfalk.simplesearchview.SimpleSearchView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class ShareSelectedContactFragment : BaseFragment() {

    private val args: ShareSelectedContactFragmentArgs by navArgs()

    private var _binding: FragmentShareSelectedContactBinding? = null
    private val binding get() = _binding!!

    private lateinit var contactsAdapter: ContactsAdapter

    private val userList: MutableList<User> = mutableListOf()

    private lateinit var valueEventListener: ValueEventListener
    private var contactsRef: DatabaseReference? = null

    private lateinit var idUserSelected: String
    companion object {
        val REQUEST_KEY = "CONTACT_SELECTED"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShareSelectedContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicia as configurações da toolbar
        initToolbar(binding.toolbar)

        // Recupera o objeto usuário selecionado
        getExtra()

        // Configura searchView
        configSearchView()
    }

    // Recupera o objeto usuário selecionado
    private fun getExtra() {
        idUserSelected = args.idUserSelected

        // Recupera contato do Firebase
        getContacts()
    }

    // Configura searchView
    private fun configSearchView() {
        binding.searchView.setOnQueryTextListener(object : SimpleSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                hideKeyboard()
                contactsAdapter.search(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isNotEmpty()) {
                    contactsAdapter.search(newText)
                } else {
                    contactsAdapter.clearSearch()
                }
                return true
            }

            override fun onQueryTextCleared(): Boolean {
                return false
            }

        })

        binding.searchView.setOnSearchViewListener(object : SimpleSearchView.SearchViewListener {
            override fun onSearchViewClosed() {
                contactsAdapter.clearSearch()
            }

            override fun onSearchViewClosedAnimation() {
            }

            override fun onSearchViewShown() {
            }

            override fun onSearchViewShownAnimation() {
            }

        })

    }

    // Recupera contato do Firebase
    private fun getContacts() {
        contactsRef = FirebaseHelper
            .getDatabase()
            .child("users")
        valueEventListener = contactsRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    userList.clear()
                    for (ds in snapshot.children) {
                        val user = ds.getValue(User::class.java) as User

                        if (user.id != FirebaseHelper.getIdUser() &&
                                user.id != idUserSelected)
                            userList.add(user)

                    }
                    binding.textInfo.text = ""
                } else {
                    binding.textInfo.text = getString(R.string.list_empty_contacts_fragment)
                }

                binding.toolbar.subtitle =
                    getString(R.string.count_contacts_fragment, userList.size.toString())
                binding.progressBar.visibility = View.GONE

                // Inicia as configurações do adapter
                initAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    // Inicia as configurações do adapter
    private fun initAdapter() {
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.setHasFixedSize(true)

        contactsAdapter = ContactsAdapter(userList) { user ->
            val bundle = Bundle()
            bundle.putParcelable(REQUEST_KEY, user)
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundle)
            findNavController().popBackStack()
        }

        binding.rvContacts.adapter = contactsAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_contacts, menu)
        val item = menu.findItem(R.id.action_search)
        binding.searchView.setMenuItem(item)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (contactsRef != null) contactsRef?.removeEventListener(valueEventListener)
    }

}