package com.rpljumat.ti_tipagent

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.coroutines.*

class Dashboard : AppCompatActivity() {
    private lateinit var activityContext: Dashboard
    var conn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        activityContext = this

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

        expand_need_attention.tag = R.drawable.ic_collapse
        expand_active.tag = R.drawable.ic_collapse

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val agentId = currentUser?.uid

        val db = FirebaseFirestore.getInstance()
        db.collection("goods").get()
            .addOnSuccessListener {
                settle(it)
            }
            .addOnFailureListener {

            }

        user_icon.setOnClickListener {

        }

        goods_history_btn_dashboard.setOnClickListener {
            val riwayatTitipan = Intent(this, RiwayatTitipan::class.java)
            startActivity(riwayatTitipan)
        }

        expand_need_attention.setOnClickListener {
            val status = expand_need_attention.tag as Int
            val expanded = (status == R.drawable.ic_collapse)

            if(expanded) {
                expand_need_attention.setImageDrawable(resources.getDrawable(R.drawable.ic_expand, null))
                expand_need_attention.tag = R.drawable.ic_expand
            }
            else {
                expand_need_attention.setImageDrawable(resources.getDrawable(R.drawable.ic_collapse, null))
                expand_need_attention.tag = R.drawable.ic_collapse
            }

            val histCnt = container_need_attention.childCount
            var i = 2
        }

        expand_active.setOnClickListener {
            val status = expand_active.tag as Int
            val expanded = (status == R.drawable.ic_collapse)

            if(expanded) {
                expand_active.setImageDrawable(resources.getDrawable(R.drawable.ic_expand, null))
                expand_active.tag = R.drawable.ic_expand
            }
            else {
                expand_active.setImageDrawable(resources.getDrawable(R.drawable.ic_collapse, null))
                expand_active.tag = R.drawable.ic_collapse
            }

            val histCnt = container_active.childCount
            var i = 3
        }

        more_active.setOnClickListener {
            val titipanBerjalanAgent = Intent(this, TitipanBerjalanAgent::class.java)
            startActivity(titipanBerjalanAgent)
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
        val builder = AlertDialog.Builder(activityContext)
        builder.setTitle("Tidak ada koneksi!")
            .setMessage("Pastikan Wi-Fi atau data seluler telah dinyalakan, lalu coba lagi")
            .setPositiveButton("Coba lagi") { _: DialogInterface, _: Int ->
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.Default) {
                        checkNetworkConnection()
                    }
                    if(!conn){
                        alertNoConnection()
                    }
                }
            }
            .setCancelable(false)
        builder.show()
    }

    private fun settle(goods: QuerySnapshot) {
        CoroutineScope(Dispatchers.Main).launch {
            var prevIdAtt = title_need_attention.id
            var prevIdActive = title_active.id
            var attCnt = 0
            var activeCnt = 0

            for(good in goods) {
                val data = good.data
                val status = (data["status"] as Long).toInt()
                val nama = data["nama"] as String
                val uId = data["uId"] as String
                val estPrice = (data["estPrice"] as Long).toInt()


            }
        }
    }

}