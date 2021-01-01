package com.rpljumat.ti_tipagent

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
import kotlinx.android.synthetic.main.activity_login_agent.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginAgent : AppCompatActivity() {
    private var conn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_agent)

        // Get current user
        val auth = FirebaseAuth.getInstance()
        var currentUser = auth.currentUser

        // If already logged in, go to Dashboard
        if(currentUser != null){
            val dashboard = Intent(this, Dashboard::class.java)
            startActivity(dashboard)
            finish()
        }

        // Executes when the login button is clicked
        login_btn.setOnClickListener {
            val email = email_text.text.toString()
            val pass = password_text.text.toString()

            // Checks email and password are filled
            if(email.isEmpty()){
                Toast.makeText(this@LoginAgent, "Email tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(pass.isEmpty()){
                Toast.makeText(this@LoginAgent, "Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Authenticate
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    currentUser = auth.currentUser
                    val agentId = currentUser!!.uid

                    // Check if logged in into agent account
                    CoroutineScope(Dispatchers.Main).launch {
                        val db = FirebaseFirestore.getInstance()
                        val agentDoc = db.collection("agent").document(agentId).get().await()
                        // Logout immediately if logged in into user account
                        if(!agentDoc.exists()) {
                            auth.signOut()
                            email_text.text.clear()
                            password_text.text.clear()
                            Toast.makeText(this@LoginAgent,"Anda bukan agen!", Toast.LENGTH_SHORT)
                                .show()
                            return@launch
                        }
                        // Proceed if logged in into agent account
                        Toast.makeText(this@LoginAgent, "Login berhasil", Toast.LENGTH_SHORT).show()
                        val dashboard = Intent(applicationContext, Dashboard::class.java)
                        startActivity(dashboard)
                        finish()
                    }
                }
                .addOnFailureListener {
                    // Check internet connection first
                    CoroutineScope(Dispatchers.Main).launch {
                        withContext(Dispatchers.Default) {
                            checkNetworkConnection()
                        }
                        // If no connection, alert
                        if(!conn) {
                            alertNoConnection()
                            return@launch
                        }
                    }
                    Toast.makeText(this@LoginAgent, "Login gagal", Toast.LENGTH_SHORT).show()
                }
        }

        regis_btn.setOnClickListener {
            val regis = Intent(this, RegistrasiAgent::class.java)
            startActivity(regis)
            finish()
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