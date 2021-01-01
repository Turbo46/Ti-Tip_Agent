package com.rpljumat.ti_tipagent

import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Constraints
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.layout_new_goods_param.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Dashboard : AppCompatActivity() {
    private var conn = false

    private var agentId = "null"
    private var containerId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

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
        agentId = currentUser?.uid.toString()

        val db = FirebaseFirestore.getInstance()
        db.collection("goods").get()
            .addOnSuccessListener {
                settle(it)
            }
            .addOnFailureListener {

            }

        user_icon.setOnClickListener {
            val agentInfo = Intent(this, AgentInfo::class.java)
            startActivity(agentInfo)
        }

        goods_history_btn_dashboard.setOnClickListener {
            val riwayatTitipan = Intent(this, RiwayatTitipan::class.java)
            riwayatTitipan.putExtra("Agent ID", agentId)
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

            val attCnt = container_need_attention.childCount
            var i = 2
            while(i < attCnt) {
                val item = container_need_attention.getChildAt(i)
                item.visibility = if(expanded) View.GONE else View.VISIBLE
                i++
            }
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

            val activeCnt = container_active.childCount
            var i = 3
            while(i < activeCnt) {
                val item = container_active.getChildAt(i)
                item.visibility = if(expanded) View.GONE else View.VISIBLE
                i++
            }
        }

        more_active.setOnClickListener {
            val titipanBerjalanAgent = Intent(this, TitipanBerjalanAgent::class.java)
            titipanBerjalanAgent.putExtra("Agent ID", agentId)
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
        val builder = AlertDialog.Builder(this)
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
        var prevIdAtt = title_need_attention.id
        var prevIdActive = title_active.id
        var attCnt = 0
        var activeCnt = 0

        CoroutineScope(Dispatchers.Main).launch {
            for(good in goods) {
                val goodId = good.id
                val data = good.data
                val goodAgentId = data["agentId"].toString()
                val goodAgentDest = data["agentDest"].toString()
                val status = (data["status"] as Long).toInt()

                when(status) {
                    REJECTED, RETURNED, EXPIRED -> continue
                    AWAITING_PINDAH_TITIP_DEST -> {
                        if(goodAgentDest != agentId) continue
                    }
                    else -> {
                        if(goodAgentId != agentId) continue
                    }
                }

                val nama = data["nama"].toString()
                val uId = data["userId"].toString()
                val userFullName = getUserFullName(uId)
                val estPrice = (data["estPrice"] as Long).toInt()
                val grocery = data["grocery"] as Boolean
                val fragile = data["fragile"] as Boolean
                val pindahTitipPrice = when(status) {
                    AWAITING_PINDAH_TITIP_ORG, AWAITING_PINDAH_TITIP_DEST -> {
                        (data["pindahTitipPrice"] as Long).toInt()
                    }
                    else -> 0
                }
                val ts = (data["ts"] as Long).toDT()
                val exp = (data["exp"] as Long).toDT()

                when(status) {
                    NEW_GOODS, AWAITING_PINDAH_TITIP_ORG, AWAITING_PINDAH_TITIP_DEST,
                    REQ_RETURN -> {
                        containerId = createTitipanItemBg(prevIdAtt, ATT_BLOCK)
                        prevIdAtt = containerId
                        attCnt++
                    }
                    STORED -> {
                        if(activeCnt == 5) continue
                        containerId = createTitipanItemBg(prevIdActive, ACTIVE_BLOCK)
                        prevIdActive = containerId
                        activeCnt++
                    }
                    else -> return@launch
                }

                var prevIdInner = createTitipanItemTitle(nama)
                prevIdInner = createTitipanItemStatus(prevIdInner, status)
                prevIdInner = createTitipanItemUserFullName(prevIdInner, userFullName)
                prevIdInner = createTitipanItemFragileGrocery(prevIdInner, fragile, grocery)

                when(status) {
                    AWAITING_PINDAH_TITIP_ORG -> {
                        prevIdInner = createTitipanItemPTLoc(prevIdInner, goodAgentDest, status)
                    }
                    AWAITING_PINDAH_TITIP_DEST -> {
                        prevIdInner = createTitipanItemPTLoc(prevIdInner, goodAgentId, status)
                    }
                }

                if(status != NEW_GOODS) {
                    val length = (data["length"] as Double).toFloat()
                    val width = (data["width"] as Double).toFloat()
                    val height = (data["height"] as Double).toFloat()
                    val weight = (data["weight"] as Double).toFloat()
                    prevIdInner =
                        createTitipanItemDimWeight(prevIdInner, length, width, height, weight)
                }

                when(status) {
                    NEW_GOODS, REQ_RETURN -> prevIdInner =
                        createTitipanItemCost(prevIdInner, estPrice, status)
                    AWAITING_PINDAH_TITIP_ORG -> prevIdInner =
                        createTitipanItemCost(prevIdInner, pindahTitipPrice, status)
                }

                if(status != REQ_RETURN) prevIdInner = createTitipanItemTs(prevIdInner, ts, status)
                prevIdInner = createTitipanItemExp(prevIdInner, exp)

                // Action menus
                createTitipanItemActions(prevIdInner, goodId, nama, status, userFullName,
                    grocery, fragile, goodAgentId, goodAgentDest, estPrice, pindahTitipPrice)
            }

            setAttCnt(attCnt)
            if(attCnt > 0) addMarginBottomAtt(prevIdAtt)
            if(activeCnt > 0) addMarginBottomActive(prevIdActive)
        }
    }

    private fun createTitipanItemBg(prevId: Int, block: Int): Int {
        val container = ConstraintLayout(this)
        container.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.MATCH_PARENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        container.id = id
        container.setBackgroundResource(R.color.frame_front_bg)

        if(block == ATT_BLOCK) container_need_attention.addView(container)
        else container_active.addView(container)

        val constraintSet = ConstraintSet()
        val parentCL = if(block == ATT_BLOCK) container_need_attention else container_active
        constraintSet.clone(parentCL)
        constraintSet.centerHorizontally(id,
            parentCL.id, ConstraintSet.LEFT, 8f.toPx(),
            parentCL.id, ConstraintSet.RIGHT, 8f.toPx(),
            0.5f)
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.applyTo(parentCL)

        return id
    }

    private fun createTitipanItemTitle(nama: String): Int {
        val titleTextView = TextView(this)
        titleTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        titleTextView.id = id

        titleTextView.text = nama
        titleTextView.setTextColor(black)

        titleTextView.textSize = 16f
        titleTextView.setTypeface(titleTextView.typeface, Typeface.BOLD)

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(titleTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.START, containerId, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, containerId, ConstraintSet.TOP, 8f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createTitipanItemStatus(prevId: Int, status: Int): Int {
        val statusTextView = TextView(this)
        statusTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        statusTextView.id = id

        val pair = status.getStatusInfo()
        val text = pair.first
        val color = pair.second
        statusTextView.text = text
        statusTextView.setTextColor(ContextCompat.getColor(this, color))

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(statusTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.START, containerId, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createTitipanItemUserFullName(prevId: Int, userFullName: String): Int {
        val fullNameTextView = TextView(this)
        fullNameTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        fullNameTextView.id = id

        val text = "Penitip: $userFullName"
        fullNameTextView.text = text
        fullNameTextView.setTextColor(black)

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(fullNameTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.START, containerId, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createTitipanItemFragileGrocery(prevId: Int,
                                                fragile: Boolean, grocery: Boolean): Int {

        val container = findViewById<ConstraintLayout>(containerId)

        // Fragile part
        val fragileTextView = TextView(this)
        fragileTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val fragileId = View.generateViewId()
        fragileTextView.id = fragileId
        fragileTextView.setTextColor(if(fragile) red else black)

        val fragileText = if(fragile) "Pecah belah" else "Tidak pecah belah"
        fragileTextView.text = fragileText

        container.addView(fragileTextView)

        // Separator
        val separTextView = TextView(this)
        separTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val separId = View.generateViewId()
        separTextView.id = separId

        separTextView.text = "/"
        separTextView.setTextColor(black)

        container.addView(separTextView)

        // Grocery part
        val groceryTextView = TextView(this)
        groceryTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val groceryId = View.generateViewId()
        groceryTextView.id = groceryId
        groceryTextView.setTextColor(if(grocery) red else black)

        val groceryText = if(grocery) "Basah" else "Tidak basah"
        groceryTextView.text = groceryText

        container.addView(groceryTextView)

        val constraintLayout = ConstraintSet()
        constraintLayout.clone(container)
        constraintLayout.connect(
            fragileId, ConstraintSet.START, containerId, ConstraintSet.START, 8f.toPx())
        constraintLayout.connect(
            fragileId, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintLayout.connect(
            separId, ConstraintSet.START, fragileId, ConstraintSet.END, 2f.toPx())
        constraintLayout.centerVertically(separId, fragileId)
        constraintLayout.connect(
            groceryId, ConstraintSet.START, separId, ConstraintSet.END, 2f.toPx())
        constraintLayout.centerVertically(groceryId, separId)
        constraintLayout.applyTo(container)

        return fragileId
    }

    private suspend fun createTitipanItemPTLoc(prevId: Int, agentId: String, status: Int): Int {
        val locTextView = TextView(this)
        locTextView.layoutParams = Constraints.LayoutParams(
            0,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        locTextView.id = id

        val agentDestName = getAgentName(agentId)
        val agentDestCoords = getAgentCoords(agentId)
        val agentDestLoc = getAgentLoc(agentDestCoords, this)
        val text = when(status) {
            AWAITING_PINDAH_TITIP_ORG -> "Tujuan:\n$agentDestName\n$agentDestLoc"
            AWAITING_PINDAH_TITIP_DEST -> "Pengirim:\n$agentDestName\n$agentDestLoc"
            else -> ""
        }
        locTextView.text = text
        locTextView.setTextColor(black)

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(locTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.centerHorizontally(id, containerId, ConstraintSet.LEFT, 8f.toPx(),
            containerId, ConstraintSet.RIGHT, 8f.toPx(), 0.5f)
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createTitipanItemDimWeight(prevId: Int, length: Float,
                                           width: Float, height: Float, weight: Float): Int {

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

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(dimWeightTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.START, containerId, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createTitipanItemCost(prevId: Int, amount: Int, status: Int): Int {
        val costTextView = TextView(this)
        costTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        costTextView.id = id

        val text = when(status) {
            NEW_GOODS -> "Biaya penitipan: Rp$amount"
            AWAITING_PINDAH_TITIP_ORG -> "Biaya Pindah Titip: Rp$amount"
            REQ_RETURN -> "Biaya pengembalian: Rp$amount"
            else -> ""
        }
        costTextView.text = text
        costTextView.setTextColor(black)

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(costTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.START, containerId, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createTitipanItemTs(prevId: Int, ts: String, status: Int): Int {
        val tsTextView = TextView(this)
        tsTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        tsTextView.id = id

        val text = if(status == NEW_GOODS) "Diajukan pada: $ts" else "Dititipkan pada $ts"
        tsTextView.text = text
        tsTextView.setTextColor(black)

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(tsTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.START, containerId, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createTitipanItemExp(prevId: Int, exp: String): Int {
        val expTextView = TextView(this)
        expTextView.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        expTextView.id = id

        val text = "Kadaluarsa pada $exp"
        expTextView.text = text
        expTextView.setTextColor(red)

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(expTextView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.START, containerId, ConstraintSet.START, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 4f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createTitipanItemActions(prevId: Int, goodId: String, nama: String, status: Int,
                                         userFullName: String, grocery: Boolean, fragile: Boolean,
                                         agentId: String, agentDest: String,
                                         estPrice: Int, pindahTitipPrice: Int) {

        if(status == STORED || status == REJECTED || status == RETURNED || status == EXPIRED)
            return

        val positiveId = createButtonPositive(prevId, goodId, nama, status, userFullName,
            grocery, fragile, agentId, agentDest, estPrice, pindahTitipPrice)
        if(status == NEW_GOODS) createButtonNegativeNewGoods(positiveId, goodId, nama)
    }

    private fun createButtonPositive(prevId: Int, goodId: String, nama: String, status: Int,
                                     userFullName: String, grocery: Boolean, fragile: Boolean,
                                     agentId: String, agentDest: String,
                                     estPrice: Int, pindahTitipPrice: Int): Int {

        val button = Button(this)
        button.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        button.id = id

        button.setBackgroundResource(R.color.btn_green)
        button.setPadding(0, 0, 0, 0)

        val text = when(status) {
            NEW_GOODS -> "Terima"
            AWAITING_PINDAH_TITIP_ORG -> "Sudah dikirim"
            AWAITING_PINDAH_TITIP_DEST -> "Sudah diterima"
            REQ_RETURN -> "Kembalikan"
            else -> ""
        }
        button.text = text
        button.setTextColor(black)
        button.setTypeface(button.typeface, Typeface.BOLD_ITALIC)
        button.textSize = 14f

        button.setOnClickListener {
            buttonPositiveClickListener(goodId, nama, status, userFullName, grocery, fragile,
                agentId, agentDest, estPrice, pindahTitipPrice)
        }

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(button)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.END, containerId, ConstraintSet.END, 8f.toPx())
        constraintSet.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.connect(
            id, ConstraintSet.BOTTOM, containerId, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSet.applyTo(container)

        return id
    }

    private fun createButtonNegativeNewGoods(positiveId: Int, goodId: String, nama: String) {
        val button = Button(this)
        button.layoutParams = Constraints.LayoutParams(
            Constraints.LayoutParams.WRAP_CONTENT,
            Constraints.LayoutParams.WRAP_CONTENT
        )

        val id = View.generateViewId()
        button.id = id

        button.setBackgroundResource(R.color.btn_pink)

        val text = "Tolak"
        button.text = text
        button.setTextColor(black)
        button.setTypeface(button.typeface, Typeface.BOLD_ITALIC)
        button.textSize = 14f

        button.setOnClickListener {
            buttonNegativeClickListener(goodId, nama)
        }

        val container = findViewById<ConstraintLayout>(containerId)
        container.addView(button)

        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.connect(id, ConstraintSet.END, positiveId, ConstraintSet.START, 8f.toPx())
        constraintSet.centerVertically(id, positiveId)
        constraintSet.applyTo(container)
    }

    private fun buttonPositiveClickListener(goodId: String, namaTitipan: String, status: Int,
                                            userFullName: String,
                                            grocery: Boolean, fragile: Boolean,
                                            agentId: String, agentDest: String,
                                            estPrice: Int, pindahTitipPrice: Int) {

        val builder = AlertDialog.Builder(this)

        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("goods").document(goodId)

        val notifMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifBuilder = NotificationCompat.Builder(this, "Ti-Tip Agent")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        when(status) {
            NEW_GOODS ->
                builder.setTitle("Parameter titipan")
                    .setMessage("Ukur dimensi dan timbang berat titipan, lalu masukkan di sini")
                    .setView(R.layout.layout_new_goods_param)
                    .setPositiveButton("Terima titipan") { dialogInterface: DialogInterface,
                                                           _: Int ->
                        val dialog = dialogInterface as Dialog
                        val lengthTextView = dialog.titipan_baru_param_length
                        val widthTextView = dialog.titipan_baru_param_width
                        val heightTextView = dialog.titipan_baru_param_height
                        val weightTextView = dialog.titipan_baru_param_weight

                        val length = lengthTextView.text.toString().toFloat()
                        val width = widthTextView.text.toString().toFloat()
                        val height = heightTextView.text.toString().toFloat()
                        val weight = weightTextView.text.toString().toFloat()

                        val currDT = System.currentTimeMillis()
                        val expDays = if(grocery) 1 else 7
                        val expDT = currDT + expDays * MS_SEHARI

                        val data = hashMapOf<String, Any>(
                            "length" to length,
                            "width" to width,
                            "height" to height,
                            "weight" to weight,
                            "ts" to currDT,
                            "exp" to expDT,
                            "estPrice" to 0,
                            "status" to STORED
                        )
                        doc.update(data)
                            .addOnSuccessListener {
                                finish()
                                val konfirmasiAgent = Intent(this, KonfirmasiAgent::class.java)
                                konfirmasiAgent
                                    .putExtra("Nama Titipan", namaTitipan)
                                    .putExtra("Nama Penitip", userFullName)
                                    .putExtra("Fragile", fragile)
                                    .putExtra("Grocery", grocery)
                                    .putExtra("Panjang", length)
                                    .putExtra("Lebar", width)
                                    .putExtra("Tinggi", height)
                                    .putExtra("Berat", weight)
                                startActivity(konfirmasiAgent)
                            }
                            .addOnFailureListener {

                            }
                    }
                    .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

                    }
                    .show()
            AWAITING_PINDAH_TITIP_ORG -> builder.setTitle("Konfirmasi pengiriman")
                .setMessage("Pastikan barang sudah benar-benar dikirim ke alamat penerima")
                .setPositiveButton("Konfirmasi") { _: DialogInterface, _: Int ->
                    doc.update("status", AWAITING_PINDAH_TITIP_DEST)
                        .addOnSuccessListener {
                            // Show notification
                            CoroutineScope(Dispatchers.Main).launch {
                                val agentDestName = getAgentName(agentDest)
                                notifBuilder.setContentTitle("Pindah Titip berhasil!")
                                    .setContentText(
                                        "Titipan $namaTitipan telah dikirim ke agen $agentDestName")
                                notifMgr.notify(0, notifBuilder.build())
                            }

                            // Refresh the Dashboard
                            finish()
                            startActivity(intent)
                        }
                        .addOnFailureListener {

                        }
                }
                .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

                }
                .show()
            AWAITING_PINDAH_TITIP_DEST -> builder.setTitle("Konfirmasi penerimaan")
                .setMessage("Pastikan barang sudah benar-benar diterima dari alamat penerima")
                .setPositiveButton("Konfirmasi") { _: DialogInterface, _: Int ->
                    val data = hashMapOf(
                        "agentId" to agentDest,
                        "agentDest" to FieldValue.delete(),
                        "estPrice" to estPrice + pindahTitipPrice,
                        "pindahTitipPrice" to FieldValue.delete(),
                        "status" to STORED
                    )
                    doc.update(data)
                        .addOnSuccessListener {
                            // Show notification
                            CoroutineScope(Dispatchers.Main).launch {
                                val agentName = getAgentName(agentId)
                                notifBuilder.setContentTitle("Penerimaan Pindah Titip berhasil!")
                                    .setContentText(
                                        "Titipan $namaTitipan telah diterima dari agen $agentName")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setAutoCancel(true)
                                notifMgr.notify(0, notifBuilder.build())
                            }

                            // Refresh the Dashboard
                            finish()
                            startActivity(intent)
                        }
                        .addOnFailureListener {

                        }
                }
                .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

                }
                .show()
            REQ_RETURN -> builder.setTitle("Konfirmasi pengembalian")
                .setMessage(
                    "Pastikan barang telah dikembalikan ke penitip dan uang titipan telah diterima")
                .setPositiveButton("Konfirmasi") { _: DialogInterface, _: Int ->

                    val data = hashMapOf(
                        "ts" to FieldValue.delete(),
                        "returnTs" to System.currentTimeMillis(),
                        "status" to RETURNED
                    )
                    doc.update(data)
                        .addOnSuccessListener {
                            // Show notification
                            notifBuilder.setContentTitle("Pengembalian titipan berhasil!")
                                .setContentText("Titipan $namaTitipan berhasil dikembalikan!")
                            notifMgr.notify(0, notifBuilder.build())

                            // Refresh the Dashboard
                            finish()
                            startActivity(intent)
                        }
                        .addOnFailureListener {

                        }
                }
                .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

                }
                .show()
            else -> return
        }

    }

    private fun buttonNegativeClickListener(goodId: String, namaTitipan: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Konfirmasi untuk menolak titipan ini?")
            .setPositiveButton("Konfirmasi") { _: DialogInterface, _: Int ->
                val db = FirebaseFirestore.getInstance()
                db.collection("goods").document(goodId)
                    .update("status", REJECTED)
                    .addOnSuccessListener {
                        // Show notification
                        val notifMgr = getSystemService(Context.NOTIFICATION_SERVICE)
                                as NotificationManager
                        val notifBuilder = NotificationCompat.Builder(this, "Ti-Tip Agent")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Pengembalian titipan berhasil!")
                            .setContentText("Titipan $namaTitipan berhasil dikembalikan!")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                        notifMgr.notify(0, notifBuilder.build())

                        // Refresh the Dashboard
                        finish()
                        startActivity(intent)
                    }
                    .addOnFailureListener {

                    }
            }
            .setNegativeButton("Batal") { _: DialogInterface, _: Int ->

            }
            .show()
    }

    private fun setAttCnt(cnt: Int) {
        val text = "Perlu Perhatian ($cnt)"
        title_need_attention.text = text
    }

    private fun addMarginBottomAtt(lastIdAtt: Int) {
        val constraintSetAtt = ConstraintSet()
        constraintSetAtt.clone(container_need_attention)
        constraintSetAtt.connect(lastIdAtt, ConstraintSet.BOTTOM,
            container_need_attention.id, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSetAtt.applyTo(container_need_attention)
    }

    private fun addMarginBottomActive(lastIdActive: Int) {
        val constraintSetActive = ConstraintSet()
        constraintSetActive.clone(container_active)
        constraintSetActive.connect(lastIdActive, ConstraintSet.BOTTOM,
            container_active.id, ConstraintSet.BOTTOM, 8f.toPx())
        constraintSetActive.applyTo(container_active)
    }

}