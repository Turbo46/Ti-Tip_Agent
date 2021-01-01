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
import kotlinx.android.synthetic.main.activity_titipan_berjalan_agent.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TitipanBerjalanAgent : AppCompatActivity() {

    private var conn = false

    private lateinit var itemContainer: ConstraintLayout
    private var prevId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_titipan_berjalan_agent)

        tombol_kembali.setOnClickListener {
            finish()
        }

        CoroutineScope(Dispatchers.Main).launch {
            // Check internet connection first
            withContext(Dispatchers.Default) {
                checkNetworkConnection()
            }
            if(!conn) {
                alertNoConnection()
                return@launch
            }

            val db = FirebaseFirestore.getInstance()
            val agentId = intent.getStringExtra("Agent ID")!!
            val query = db.collection("goods")
                .whereEqualTo("agentId", agentId)
                .whereEqualTo("status", STORED)
                .get().await()
            val documents = query.documents
            for((itemCnt, document) in documents.withIndex()) {
                val data = document.data!!
                val namaTitipan = data["nama"].toString()
                val uid = data["userId"].toString()
                val userFullName = getUserFullName(uid)
                val length = (data["length"] as Double).toFloat()
                val width = (data["width"] as Double).toFloat()
                val height = (data["height"] as Double).toFloat()
                val weight = (data["weight"] as Double).toFloat()
                val fragile = data["fragile"] as Boolean
                val grocery = data["grocery"] as Boolean
                val ts = (data["ts"] as Long).toDT()
                val exp = (data["exp"] as Long).toDT()

                createRunningBg(itemCnt)
                createRunningTitle(namaTitipan)
                createRunningStatus()
                createRunningUserFullName(userFullName)
                createRunningDimWeight(length, width, height, weight)
                createRunningFragileGrocery(fragile, grocery)
                createRunningTs(ts)
                createRunningExp(exp)
            }

            val docsLen = documents.size
            if(docsLen > 0) addMarginBottom()
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

    private fun createRunningBg(itemCnt: Int) {
        val prevItemContainerId = if(itemCnt == 0) titip_berjalan_text.id else itemContainer.id
        itemContainer = ConstraintLayout(this)

        itemContainer.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.MATCH_PARENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        itemContainer.id = id
        itemContainer.setBackgroundResource(R.color.frame_front_bg)
        container_titipan_berjalan.addView(itemContainer)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container_titipan_berjalan)
        constraintSet.centerHorizontally(id,
            container_titipan_berjalan.id, ConstraintSet.LEFT, 8f.toPx(),
            container_titipan_berjalan.id, ConstraintSet.RIGHT, 8f.toPx(),
            0.5f)
        constraintSet.connect(id, ConstraintSet.TOP,
            prevItemContainerId, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.applyTo(container_titipan_berjalan)
    }

    private fun createRunningTitle(namaTitipan: String) {
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

    private fun createRunningStatus() {
        val statusTextView = TextView(this)
        statusTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        statusTextView.id = id

        // Obviously STORED
        val pair = STORED.getStatusInfo()
        val text = pair.first
        val color = pair.second
        statusTextView.text = text
        statusTextView.setTextColor(ContextCompat.getColor(this, color))

        itemContainer.addView(statusTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(itemContainer)
        constraintSet.connect(id, ConstraintSet.END, itemContainer.id, ConstraintSet.END, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, itemContainer.id, ConstraintSet.TOP, 8f.toPx())
        constraintSet.applyTo(itemContainer)
    }

    private fun createRunningUserFullName(userFullName: String) {
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

    private fun createRunningDimWeight(length: Float, width: Float, height: Float, weight: Float) {
        val dimWeightTextView = TextView(this)
        dimWeightTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        dimWeightTextView.id = id

        val text = "Dimensi/berat: ${length}cm x ${width}cm x ${height}cm / ${weight}kg"
        dimWeightTextView.text = text
        dimWeightTextView.setTextColor(black)

        itemContainer.addView(dimWeightTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(itemContainer)
        constraintSet.connect(id, ConstraintSet.START,
            itemContainer.id, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(itemContainer)

        prevId = id
    }

    private fun createRunningFragileGrocery(fragile: Boolean, grocery: Boolean) {
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

    private fun createRunningTs(ts: String) {
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

    private fun createRunningExp(exp: String) {
        val expTextView = TextView(this)
        expTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        expTextView.id = id

        val text = "Kadaluarsa pada: $exp"
        expTextView.text = text
        expTextView.setTextColor(red)

        itemContainer.addView(expTextView)

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
        constraintSet.clone(container_titipan_berjalan)
        constraintSet.connect(itemContainer.id, ConstraintSet.BOTTOM,
            container_titipan_berjalan.id, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.applyTo(container_titipan_berjalan)
    }
}