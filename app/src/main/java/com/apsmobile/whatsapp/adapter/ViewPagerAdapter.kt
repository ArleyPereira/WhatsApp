package com.apsmobile.whatsapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.apsmobile.whatsapp.ui.call.CallsFragment
import com.apsmobile.whatsapp.ui.chat.TalksFragment
import com.apsmobile.whatsapp.ui.status.StatusFragment

class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    private val talksFragment = TalksFragment()
    private val statusFragment = StatusFragment()
    private val callsFragment = CallsFragment()

    // Retorna o número de fragments adicionados pela fun addFragment()
    override fun getItemCount() = 3

    // Cria os fragments pelas posições
    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> talksFragment
            1 -> statusFragment
            else -> callsFragment
        }

    fun searchUser(userName: String) {
        talksFragment.searchUser(userName)
    }

}