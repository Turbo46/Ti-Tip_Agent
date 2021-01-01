package com.rpljumat.ti_tipagent

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Constraints
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_riwayat_titipan.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RiwayatTitipan : AppCompatActivity() {

    private var conn = false

    private lateinit var itemContainer: ConstraintLayout
    private var prevId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_titipan)

        itemContainer = container_history

        back.setOnClickListener {
            finish()
        }

        CoroutineScope(Dispatchers.Main).launch {
            // Check internet connection first
            withContext(Dispatchers.Default) {
                checkNetworkConnection()
            }
            if (!conn) {
                alertNoConnection()
                return@launch
            }

            val db = FirebaseFirestore.getInstance()
            val goodsCollection = db.collection("goods")
            val agentId = intent.getStringExtra("Agent ID")!!
            val allHistoryStatus =
                mutableListOf(STORED, REJECTED, AWAITING_PINDAH_TITIP_DEST, RETURNED, EXPIRED)
            val query = goodsCollection.whereIn("status", allHistoryStatus).get().await()
            val documents = query.documents
            for((itemCnt, document) in documents.withIndex()) {
                val id = document.id
                val data = document.data!!
                val status = (data["status"] as Long).toInt()
                val namaTitipan = data["nama"].toString()
                val uid = data["userId"].toString()
                val userFullName = getUserFullName(uid)
                val goodAgentId = data["agentId"].toString()
                val goodAgentDest = data["agentDest"].toString()
                val agentDestName =
                    if(status == AWAITING_PINDAH_TITIP_DEST) getAgentName(goodAgentDest) else ""
                val fragile = data["fragile"] as Boolean
                val grocery = data["grocery"] as Boolean
                val ts = (
                    if(status == RETURNED) data["returnTs"] as Long
                    else data["ts"] as Long)
                    .toDT()
                val documentHist = goodsCollection.document(id)
                    .collection("agentHist").document("agentHist").get().await()
                val histData = documentHist.data!!

                when(status) {
                    STORED -> {
                        if(goodAgentId == agentId) continue // Should not be stored here
                        if(!isThisAgentInvolved(histData, agentId)) continue
                    }
                    REJECTED -> {
                        if(goodAgentId != agentId) continue
                    }
                    AWAITING_PINDAH_TITIP_DEST -> {
                        if(goodAgentDest == agentId) continue // Should not be stored here
                        if(!isThisAgentInvolved(histData, agentId)) continue
                    }
                    RETURNED, EXPIRED -> {
                        if(goodAgentId != agentId && !isThisAgentInvolved(histData, agentId))
                            continue
                    }
                }

                createHistoryBg(itemCnt)
                createHistoryTitle(namaTitipan)
                createHistoryStatus(status, goodAgentId == agentId)
                createHistoryUserFullName(userFullName)
                if(status == AWAITING_PINDAH_TITIP_DEST) createHistoryAgentDest(agentDestName)
                createHistoryFragileGrocery(fragile, grocery)
                createHistoryTs(ts)
            }
            addMarginBottom()
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
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    private fun isThisAgentInvolved(histData: MutableMap<String, Any>, agentId: String): Boolean {
        val len = histData.size
        histData.remove(len.toString())
        return histData.containsValue(agentId)
    }

    private fun createHistoryBg(itemCnt: Int) {
        val prevItemContainerId = itemContainer.id
        itemContainer = ConstraintLayout(this)

        itemContainer.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.MATCH_PARENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        itemContainer.id = id
        itemContainer.setBackgroundResource(R.color.frame_front_bg)
        container_history.addView(itemContainer)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container_history)
        constraintSet.centerHorizontally(id,
            container_history.id, ConstraintSet.LEFT, 8f.toPx(),
            container_history.id, ConstraintSet.RIGHT, 8f.toPx(),
            0.5f)
        constraintSet.connect(id, ConstraintSet.TOP,
            prevItemContainerId, if(itemCnt == 0) ConstraintSet.TOP else ConstraintSet.BOTTOM,
            8f.toPx())
        constraintSet.applyTo(container_history)
    }

    private fun createHistoryTitle(namaTitipan: String) {
        val titleTextView = TextView(this)
        titleTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        titleTextView.id = id

        titleTextView.text = namaTitipan
        titleTextView.setTextColor(black)

        titleTextView.textSize = 16f
        titleTextView.setTypeface(titleTextView.typeface, Typeface.BOLD)

        itemContainer.addView(titleTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(itemContainer)
        constraintSet.connect(id, ConstraintSet.START,
            itemContainer.id, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, itemContainer.id, ConstraintSet.TOP, 8f.toPx())
        constraintSet.applyTo(itemContainer)

        prevId = id
    }

    private fun createHistoryStatus(status: Int, isActionedOnThisAgent: Boolean) {
        val statusTextView = TextView(this)
        statusTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        statusTextView.id = id

        val text = when(status) {
            STORED, AWAITING_PINDAH_TITIP_DEST -> "Pindah Titip"
            REJECTED -> "Dibatalkan"
            RETURNED -> {
                if(isActionedOnThisAgent) "Dikembalikan"
                else "Pindah Titip"
            }
            EXPIRED -> {
                if(isActionedOnThisAgent) "Kadaluarsa"
                else "Pindah Titip"
            }
            else -> ""
        }
        statusTextView.text = text

        val statRed = R.color.stat_red
        val statOrange = R.color.stat_orange
        val statGreen = R.color.stat_green
        val color = when(status) {
            STORED, AWAITING_PINDAH_TITIP_DEST -> statOrange
            REJECTED -> statRed
            RETURNED -> {
                if(isActionedOnThisAgent) statGreen
                else statOrange
            }
            EXPIRED -> {
                if(isActionedOnThisAgent) statRed
                else statOrange
            }
            else -> -1
        }
        statusTextView.setTextColor(ContextCompat.getColor(this, color))

        itemContainer.addView(statusTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(itemContainer)
        constraintSet.connect(id, ConstraintSet.END, itemContainer.id, ConstraintSet.END, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, itemContainer.id, ConstraintSet.TOP, 8f.toPx())
        constraintSet.applyTo(itemContainer)
    }

    private fun createHistoryUserFullName(userFullName: String) {
        val userFullNameTextView = TextView(this)
        userFullNameTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        userFullNameTextView.id = id

        val text = "Nama penitip: $userFullName"
        userFullNameTextView.text = text
        userFullNameTextView.setTextColor(black)

        itemContainer.addView(userFullNameTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(itemContainer)
        constraintSet.connect(id, ConstraintSet.START,
            itemContainer.id, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.applyTo(itemContainer)

        prevId = id
    }

    private fun createHistoryAgentDest(agentDestName: String) {
        val agentDestTextView = TextView(this)
        agentDestTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        agentDestTextView.id = id

        val text = "Di-Pindah Titip kepada: $agentDestName"
        agentDestTextView.text = text
        agentDestTextView.setTextColor(black)

        itemContainer.addView(agentDestTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(itemContainer)
        constraintSet.connect(id, ConstraintSet.START,
            itemContainer.id, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.applyTo(itemContainer)

        prevId = id
    }

    private fun createHistoryFragileGrocery(fragile: Boolean, grocery: Boolean) {
        val fragileGroceryTextView = TextView(this)
        fragileGroceryTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        fragileGroceryTextView.id = id

        val groceryText = if(grocery) "Basah" else "Tidak basah"
        val fragileText = if(fragile) "Pecah belah" else "Tidak pecah belah"
        val text = "$groceryText / $fragileText"
        fragileGroceryTextView.text = text
        fragileGroceryTextView.setTextColor(black)

        itemContainer.addView(fragileGroceryTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(itemContainer)
        constraintSet.connect(id, ConstraintSet.START,
            itemContainer.id, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(itemContainer)

        prevId = id
    }

    private fun createHistoryTs(ts: String) {
        val tsTextView = TextView(this)
        tsTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        tsTextView.id = id

        val text = "Dititipkan pada: $ts"
        tsTextView.text = text
        tsTextView.setTextColor(black)

        itemContainer.addView(tsTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(itemContainer)
        constraintSet.connect(id, ConstraintSet.START,
            itemContainer.id, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(itemContainer)

        prevId = id
    }

    private fun addMarginBottom() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(container_history)
        constraintSet.connect(itemContainer.id, ConstraintSet.BOTTOM,
            container_history.id, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.applyTo(container_history)
    }
}