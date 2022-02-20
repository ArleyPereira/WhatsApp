package com.apsmobile.whatsapp.utils

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.apsmobile.whatsapp.R
import com.apsmobile.whatsapp.databinding.LayoutInfoBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.*

fun Fragment.initToolbar(toolbar: Toolbar, HomeAsUpEnabled: Boolean = true) {
    (activity as AppCompatActivity).setSupportActionBar(toolbar)
    (activity as AppCompatActivity).title = ""
    (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(HomeAsUpEnabled)
    toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
    toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"))
}

fun String.validCPF(): Boolean {
    val CPF = this

    if (CPF == "00000000000" ||
        CPF == "11111111111" ||
        CPF == "22222222222" || CPF == "33333333333" ||
        CPF == "44444444444" || CPF == "55555555555" ||
        CPF == "66666666666" || CPF == "77777777777" ||
        CPF == "88888888888" || CPF == "99999999999" ||
        CPF.length != 11
    ) return false

    val dig10: Char
    val dig11: Char
    var sm: Int
    var i: Int
    var r: Int
    var num: Int
    var peso: Int

    return try {
        sm = 0
        peso = 10
        i = 0

        while (i < 9) {
            num = (CPF[i] - 48).code
            sm += num * peso
            peso -= 1
            i++
        }

        r = 11 - sm % 11
        dig10 = if (r == 10 || r == 11) '0' else (r + 48).toChar()
        sm = 0
        peso = 11
        i = 0

        while (i < 10) {
            num = (CPF[i] - 48).code
            sm += num * peso
            peso -= 1
            i++
        }

        r = 11 - sm % 11
        dig11 = if (r == 10 || r == 11) '0' else (r + 48).toChar()
        dig10 == CPF[9] && dig11 == CPF[10]
    } catch (erro: InputMismatchException) {
        false
    }

}

fun String.isFullName() = this.trim().split(" ").size > 1

fun String.isEmail() = this.matches(Regex(".+@.+\\..+"))

fun Context.toast(resource: Int): Toast = Toast
    .makeText(this, resource, Toast.LENGTH_SHORT)
    .apply { show() }

fun Fragment.showBottomSheetInfo(resource: Int) {
    val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialog)
    val bottomSheetBinding: LayoutInfoBottomSheetBinding =
        LayoutInfoBottomSheetBinding.inflate(layoutInflater, null, false)

    bottomSheetBinding.textMsgInfo.text = getString(resource)
    bottomSheetBinding.btnClose.setOnClickListener { bottomSheetDialog.dismiss() }

    bottomSheetDialog.setContentView(bottomSheetBinding.root)
    bottomSheetDialog.show()
}