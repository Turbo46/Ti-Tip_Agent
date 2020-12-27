package com.rpljumat.ti_tipagent

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.activity_login_agent.email_text
import kotlinx.android.synthetic.main.activity_login_agent.regis_btn
import kotlinx.android.synthetic.main.activity_registrasi__agent.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistrasiAgent : AppCompatActivity() {

    var conn = false

    private var coords = GeoPoint(0.toDouble(), 0.toDouble())

    companion object {
        const val ADDRESS_DATA = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrasi__agent)

        sel_loc_btn_regis.setOnClickListener {
            val peta = Intent(this, PetaDaftarAgen::class.java)
            startActivityForResult(peta, ADDRESS_DATA)
        }

        login_regis_btn.setOnClickListener {
            val login = Intent(this, LoginAgent::class.java)
            startActivity(login)
            finish()
        }

        regis_btn.setOnClickListener {
            val agentName = agent_name_text.text.toString()
            val pj = fullname_text.text.toString()
            val username = username_text.text.toString()
            val nik = nik_text.text.toString()
            val phone = nope_text.text.toString()
            val email = email_text.text.toString()
            val pass = pass_text.text.toString()
            val alamat = sel_loc_regis.text.toString()

            when {
                agentName.isEmpty() -> {
                    Toast.makeText(this@RegistrasiAgent,
                        "Nama agen belum diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                pj.isEmpty() -> {
                    Toast.makeText(this@RegistrasiAgent,
                        "Nama penanggung jawab belum diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                username.isEmpty() -> {
                    Toast.makeText(this@RegistrasiAgent,
                        "Username belum diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                nik.isEmpty() -> {
                    Toast.makeText(this@RegistrasiAgent,
                        "NIK belum diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                phone.isEmpty() -> {
                    Toast.makeText(this@RegistrasiAgent,
                        "No. telepon belum diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    Toast.makeText(this@RegistrasiAgent,
                        "Email belum diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                pass.isEmpty() -> {
                    Toast.makeText(this@RegistrasiAgent,
                        "Password belum diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                alamat.isEmpty() -> {
                    Toast.makeText(this@RegistrasiAgent,
                        "Alamat belum dipilih!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    val agentId = auth.uid ?: ""
                    val db = FirebaseFirestore.getInstance()
                    val agent = Agent(agentName, pj, username, nik, phone, coords)

                    db.collection("agent")
                        .document(agentId)
                        .set(agent)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@RegistrasiAgent,
                                "Pendaftaran berhasil",
                                Toast.LENGTH_SHORT
                            ).show()
                            val dashboard = Intent(this, Dashboard::class.java)
                            startActivity(dashboard)
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this@RegistrasiAgent,
                                "Pendaftaran gagal",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    // Check internet connection first
                    CoroutineScope(Dispatchers.Main).launch {
                        withContext(Dispatchers.Default) {
                            checkNetworkConnection()
                        }
                        if(!conn) {
                            alertNoConnection()
                            return@launch
                        }
                    }
                    Toast.makeText(this@RegistrasiAgent, "Pendaftaran gagal", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == ADDRESS_DATA) {
            if(resultCode == Activity.RESULT_OK) {
                sel_loc_regis.text = data!!.getStringExtra("Alamat")
                val lat = data.getDoubleExtra("Lintang", -6.208763)
                val long = data.getDoubleExtra("Bujur", 106.845599)
                coords = GeoPoint(lat, long)
            }
        }
    }

    private fun checkNetworkConnection() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()
        cm.registerNetworkCallback(
            builder.build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    conn = true
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    conn = false
                }
            }
        )
    }

    private fun alertNoConnection() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tidak ada koneksi!")
            .setMessage("Pastikan Wi-Fi atau data seluler telah dinyalakan, lalu coba lagi")
            .setPositiveButton("Kembali") { _: DialogInterface, _: Int ->

            }
        builder.show()
    }
}