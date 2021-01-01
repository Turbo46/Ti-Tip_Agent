package com.rpljumat.ti_tipagent

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.activity_agent_info.*
import kotlinx.android.synthetic.main.layout_dlg_change_email.*
import kotlinx.android.synthetic.main.layout_dlg_change_pass.*
import kotlinx.android.synthetic.main.layout_dlg_change_phone.*
import kotlinx.android.synthetic.main.layout_dlg_change_responsible.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgentInfo : AppCompatActivity() {
    var conn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agent_info)

        lateinit var auth: FirebaseAuth
        lateinit var currAgent: FirebaseUser
        lateinit var email: String
        lateinit var doc: DocumentReference

        CoroutineScope(Dispatchers.Main).launch {
            // Check internet connection first
            withContext(Dispatchers.Default) {
                checkNetworkConnection()
            }
            if (!conn) {
                alertNoConnection(false)
                return@launch
            }

            auth = FirebaseAuth.getInstance()
            currAgent = auth.currentUser!!
            val agentId = currAgent.uid
            email = currAgent.email!!
            val emailMaskRule = Regex("^(.{3}).+(.{3}@.+)")
            val maskedEmail = email.replace(emailMaskRule, "$1***$2")
            email_info.text = maskedEmail

            val db = FirebaseFirestore.getInstance()
            doc = db.collection("agent").document(agentId)
            doc.get()
                .addOnSuccessListener {
                    val data = it.data!!
                    val agentName = data["agentName"].toString()
                    val responsiblePerson = data["responsiblePerson"].toString()
                    val nik = data["nik"].toString()
                    val responsibleText = "$responsiblePerson - $nik"
                    val pos = data["pos"] as GeoPoint
                    val coords = Pair(pos.latitude, pos.longitude)
                    val agentLoc = getAgentLoc(coords, this@AgentInfo)
                    val phone = data["phone"].toString()
                    val phoneMaskRule = Regex("^(\\d{4})\\d+(\\d{3})")
                    val maskedPhone = phone.replace(phoneMaskRule, "$1***$2")

                    agent_name_info.text = agentName
                    agent_loc_info.text = agentLoc
                    responsible_agent_info.text = responsibleText
                    phone_info.text = maskedPhone
                }
                .addOnFailureListener {

                }
        }

        back.setOnClickListener {
            finish()
        }

        change_responsible_btn.setOnClickListener {
            // Create dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Permintaan pengubahan penanggung jawab")
                .setView(R.layout.layout_dlg_change_responsible)
                .setPositiveButton("Ubah") { dialogInterface: DialogInterface, _: Int ->
                    CoroutineScope(Dispatchers.Main).launch {
                        // Check internet connection first
                        withContext(Dispatchers.Default) {
                            checkNetworkConnection()
                        }
                        if (!conn) {
                            dialogInterface.dismiss()
                            alertNoConnection(true)
                            return@launch
                        }

                        val dialog = dialogInterface as Dialog
                        val newPerson = dialog.new_responsible_person.text.toString()
                        val newNik = dialog.new_responsible_nik.text.toString()
                        val currPass = dialog.responsible_curr_pass.text.toString()

                        val credential = EmailAuthProvider.getCredential(email, currPass)
                        currAgent.reauthenticate(credential)
                            .addOnSuccessListener {
                                val data = hashMapOf<String, Any>(
                                    "responsiblePerson" to newPerson,
                                    "nik" to newNik
                                )
                                doc.update(data)
                                    .addOnSuccessListener {
                                        val newResponsibleText = "$newPerson - $newNik"
                                        responsible_agent_info.text = newResponsibleText
                                        Toast.makeText(this@AgentInfo, "Penanggung jawab " +
                                            "berhasil diubah!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@AgentInfo, "Perubahan penanggung " +
                                            "jawab gagal!", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@AgentInfo, "Sandi tidak cocok\n" +
                                    "Perubahan penanggung jawab gagal!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

                }
                .show()
        }

        change_email_btn.setOnClickListener {
            // Create dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Permintaan pengubahan email")
                .setView(R.layout.layout_dlg_change_email)
                .setPositiveButton("Ubah") { dialogInterface: DialogInterface, _: Int ->
                    CoroutineScope(Dispatchers.Main).launch {
                        // Check internet connection first
                        withContext(Dispatchers.Default) {
                            checkNetworkConnection()
                        }
                        if (!conn) {
                            dialogInterface.dismiss()
                            alertNoConnection(true)
                            return@launch
                        }

                        val dialog = dialogInterface as Dialog
                        val newEmail = dialog.new_email.text.toString()
                        val newEmailMaskRule = Regex("^(.{3}).+(.{3}@.+)")
                        val maskedNewEmail = newEmail.replace(newEmailMaskRule, "$1***$2")
                        val currPass = dialog.email_curr_pass.text.toString()

                        val credential = EmailAuthProvider.getCredential(email, currPass)
                        currAgent.reauthenticate(credential)
                            .addOnSuccessListener {
                                currAgent.updateEmail(newEmail)
                                    .addOnSuccessListener {
                                        email = newEmail
                                        email_info.text = maskedNewEmail
                                        Toast.makeText(this@AgentInfo,
                                            "Email berhasil diubah!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@AgentInfo, "Perubahan email gagal!",
                                            Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@AgentInfo, "Sandi tidak cocok\n" +
                                    "Perubahan email gagal!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

                }
                .show()
        }

        change_phone_btn.setOnClickListener {
            // Create dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Permintaan pengubahan nomor telepon")
                .setView(R.layout.layout_dlg_change_phone)
                .setPositiveButton("Ubah") { dialogInterface: DialogInterface, _: Int ->
                    CoroutineScope(Dispatchers.Main).launch {
                        // Check internet connection first
                        withContext(Dispatchers.Default) {
                            checkNetworkConnection()
                        }
                        if (!conn) {
                            dialogInterface.dismiss()
                            alertNoConnection(true)
                            return@launch
                        }

                        val dialog = dialogInterface as Dialog
                        val newPhone = dialog.new_phone.text.toString()
                        val newPhoneMaskRule = Regex("^(\\d{4})\\d+(\\d{3})")
                        val maskedNewPhone = newPhone.replace(newPhoneMaskRule, "$1***$2")
                        val currPass = dialog.phone_curr_pass.text.toString()

                        val credential = EmailAuthProvider.getCredential(email, currPass)
                        currAgent.reauthenticate(credential)
                            .addOnSuccessListener {
                                doc.update("phone", newPhone)
                                    .addOnSuccessListener {
                                        phone_info.text = maskedNewPhone
                                        Toast.makeText(this@AgentInfo,
                                            "Perubahan nomor telepon berhasil!", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@AgentInfo,
                                            "Perubahan nomor telepon gagal!", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@AgentInfo, "Sandi tidak cocok\n" +
                                    "Perubahan email gagal!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

                }
                .show()
        }

        change_pass_btn.setOnClickListener {
            // Create dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Permintaan pengubahan kata sandi")
                .setView(R.layout.layout_dlg_change_pass)
                .setPositiveButton("Ubah") { dialogInterface: DialogInterface, _: Int ->
                    CoroutineScope(Dispatchers.Main).launch {
                        // Check internet connection first
                        withContext(Dispatchers.Default) {
                            checkNetworkConnection()
                        }
                        if (!conn) {
                            dialogInterface.dismiss()
                            alertNoConnection(true)
                            return@launch
                        }

                        val dialog = dialogInterface as Dialog
                        val currPass = dialog.pass_curr_pass.text.toString()
                        val newPass = dialog.new_pass.text.toString()
                        val confirmPass = dialog.confirm_pass.text.toString()

                        if(newPass != confirmPass) {
                            Toast.makeText(this@AgentInfo, "Sandi baru tidak sama dengan " +
                                "konfirmasi\nPerubahan kata sandi gagal!", Toast.LENGTH_SHORT)
                                .show()
                            return@launch
                        }

                        val credential = EmailAuthProvider.getCredential(email, currPass)
                        currAgent.reauthenticate(credential)
                            .addOnSuccessListener {
                                currAgent.updatePassword(newPass)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@AgentInfo,
                                            "Sandi berhasil diubah!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@AgentInfo, "Perubahan kata sandi gagal!",
                                            Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@AgentInfo, "Sandi lama tidak cocok\n" +
                                    "Perubahan kata sandi gagal!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

                }
                .show()
        }

        logout_btn.setOnClickListener {
            // Create dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Konfirmasi keluar")
                .setMessage("Yakin untuk keluar?")
                .setPositiveButton("Ya") { _: DialogInterface, _: Int ->
                    auth.signOut()

                    // Close all child activities and refresh the dashboard
                    finishAffinity()
                    val login = Intent(applicationContext, LoginAgent::class.java)
                    startActivity(login)
                }
                .setNegativeButton("Tidak") { _: DialogInterface, _: Int ->

                }
                .show()
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

    private fun alertNoConnection(isOpenedBefore: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tidak ada koneksi!")
            .setMessage("Pastikan Wi-Fi atau data seluler telah dinyalakan, lalu coba lagi")
            .setPositiveButton("Kembali") { _: DialogInterface, _: Int ->
                if(!isOpenedBefore) finish()
            }
            .setOnCancelListener {
                if(!isOpenedBefore) finish()
            }
            .show()
    }
}