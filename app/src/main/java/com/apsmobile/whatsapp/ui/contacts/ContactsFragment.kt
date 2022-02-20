package com.apsmobile.whatsapp.ui.contacts

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apsmobile.whatsapp.MainGraphDirections
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.adapter.ContactsAdapter
import com.apsmobile.whatsapp.databinding.FragmentContactsBinding
import com.apsmobile.whatsapp.model.User
import com.apsmobile.whatsapp.utils.BaseFragment
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.apsmobile.whatsapp.utils.initToolbar
import com.ferfalk.simplesearchview.SimpleSearchView
import com.google.firebase.database.*

class ContactsFragment : BaseFragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private lateinit var contactsAdapter: ContactsAdapter

    private val userList: MutableList<User> = mutableListOf()

    private lateinit var valueEventListener: ValueEventListener
    private var contactsRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicia as configurações da toolbar
        initToolbar(binding.toolbar)

        // Recupera contato do Firebase
        getContacts()

        // Configura searchView
        configSearchView()
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

                        if (user.id != FirebaseHelper.getIdUser())
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
            val action = MainGraphDirections
                .actionGlobalChatBetweenUsersFragment(user)
            findNavController().navigate(action)
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