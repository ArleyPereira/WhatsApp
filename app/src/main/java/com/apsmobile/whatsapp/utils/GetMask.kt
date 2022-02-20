package com.apsmobile.whatsapp.utils

import java.text.*
import java.util.*

class GetMask {

    companion object {

        const val DIA_MES: Int = 1
        const val DIA_MES_HORA: Int = 2
        const val HORA_MINUTO: Int = 3

        fun getValor(valor: Double): String {
            val nf: NumberFormat = DecimalFormat(
                "#,##0.00",
                DecimalFormatSymbols(Locale("pt", "BR"))
            )
            return nf.format(valor)
        }

        fun getDate(time: Long, tipo: Int): String {

            // 1 -> dia/mes (26 outubro)
            // 2 -> dia/mes hora (26/10 às 07:45)
            // 3 -> hora:minuto (07:45)

            val dateFormat = DateFormat.getDateTimeInstance()
            val netDate = Date(time)
            dateFormat.format(netDate)

            val dia = SimpleDateFormat("dd", Locale.ROOT).format(netDate)
            var mes = SimpleDateFormat("MM", Locale.ROOT).format(netDate)

            val hora = SimpleDateFormat("HH", Locale.ROOT).format(netDate)
            val minuto = SimpleDateFormat("mm", Locale.ROOT).format(netDate)

            if(tipo == DIA_MES){
                mes = when (mes) {
                    "01" -> "janeiro"
                    "02" -> "fevereiro"
                    "03" -> "março"
                    "04" -> "abril"
                    "05" -> "maio"
                    "06" -> "junho"
                    "07" -> "julho"
                    "08" -> "agosto"
                    "09" -> "setembro"
                    "10" -> "outubro"
                    "11" -> "novembro"
                    "12" -> "novembro"
                    else -> ""
                }
            }

            val diaMes = StringBuilder()
                .append(dia)
                .append(" ")
                .append(mes)

            val diaMesHora = StringBuilder()
                .append(dia)
                .append("/")
                .append(mes)
                .append(" às ")
                .append(hora)
                .append(":")
                .append(minuto)

            val horaMinuto = "$hora:$minuto"

            return when(tipo){
                DIA_MES -> diaMes.toString()
                DIA_MES_HORA -> diaMesHora.toString()
                HORA_MINUTO -> horaMinuto
                else -> {
                    ""
                }
            }

        }

    }

}