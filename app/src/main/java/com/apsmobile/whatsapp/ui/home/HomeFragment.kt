package com.apsmobile.whatsapp.ui.home

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.adapter.ViewPagerAdapter
import com.apsmobile.whatsapp.databinding.FragmentHomeBinding
import com.apsmobile.whatsapp.utils.BaseFragment
import com.apsmobile.whatsapp.utils.FirebaseHelper
import com.ferfalk.simplesearchview.SimpleSearchView
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        // Configura as Tabs Layout
        configTabsLayout()

        // Configura searchView
        configSearchView()
    }

    // Configura searchView
    private fun configSearchView() {
        binding.searchView.setOnQueryTextListener(object : SimpleSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewPagerAdapter.searchUser(newText)
                return true
            }

            override fun onQueryTextCleared(): Boolean {
                return false
            }
        })

        binding.searchView.setOnSearchViewListener(object : SimpleSearchView.SearchViewListener {
            override fun onSearchViewClosed() {
                viewPagerAdapter.searchUser("")
            }

            override fun onSearchViewClosedAnimation() {
            }

            override fun onSearchViewShown() {
            }

            override fun onSearchViewShownAnimation() {
            }

        })

    }

    // Configura as Tabs Layout
    private fun configTabsLayout() {
        viewPagerAdapter = ViewPagerAdapter(requireActivity())
        binding.viewPager.adapter = viewPagerAdapter

        binding.viewPager.offscreenPageLimit = 3

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.title_tab_talks)
                1 -> getString(R.string.title_tab_status)
                else -> getString(R.string.title_tab_calls)
            }
        }.attach()

        binding.tabs.elevation = 0f

        binding.searchView.setTabLayout(binding.tabs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_novo_grupo -> {
                Toast.makeText(requireContext(), "Novo grupo", Toast.LENGTH_SHORT).show()
            }
            R.id.menu_config -> {
                findNavController().navigate(R.id.action_homeFragment_to_perfilFragment)
            }
            else -> {
                FirebaseHelper.getAuth().signOut()
                findNavController().navigate(R.id.action_homeFragment_to_navigation)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        val item = menu.findItem(R.id.action_search)
        binding.searchView.setMenuItem(item)
        super.onCreateOptionsMenu(menu, inflater)
    }

}