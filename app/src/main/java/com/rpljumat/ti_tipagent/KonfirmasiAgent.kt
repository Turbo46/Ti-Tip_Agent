package com.rpljumat.ti_tipagent

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.NotificationCompat
import kotlinx.android.synthetic.main.activity_konfirmasi_agent.*

class KonfirmasiAgent : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_konfirmasi_agent)

        val namaTitipan = intent.getStringExtra("Nama Titipan")
        val namaPenitip = intent.getStringExtra("Nama Penitip")
        val fragile = intent.getBooleanExtra("Fragile", false)
        val grocery = intent.getBooleanExtra("Grocery", false)
        val length = intent.getFloatExtra("Panjang", 0f)
        val width = intent.getFloatExtra("Lebar", 0f)
        val height = intent.getFloatExtra("Tinggi", 0f)
        val weight = intent.getFloatExtra("Berat", 0f)
        val dimWeightText = "${length}cm x ${width}cm x ${height}cm / ${weight}kg"

        nama_titipan.text = namaTitipan
        nama_penitip.text = namaPenitip
        grocery_konfirmasi.text = if(grocery) "Barang basah" else "Tidak basah"
        grocery_konfirmasi.setTextColor(if(grocery) red else black)
        fragile_konfirmasi.text = if(fragile) "Barang pecah belah" else "Tidak pecah belah"
        fragile_konfirmasi.setTextColor(if(fragile) red else black)
        dim_weight.text = dimWeightText

        // Show notification
        val notifMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifBuilder = NotificationCompat.Builder(this, "Ti-Tip Agent")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        notifBuilder.setContentTitle("Penerimaan titipan berhasil!")
            .setContentText("Titipan $namaTitipan telah diterima")
        notifMgr.notify(0, notifBuilder.build())

        tombol_konfirmasi.setOnClickListener {
            finish()
            val dashboard = Intent(this, Dashboard::class.java)
            startActivity(dashboard)
        }
    }
}