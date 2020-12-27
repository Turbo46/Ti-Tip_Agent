package com.rpljumat.ti_tipagent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_user_info.*

class UserInfo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        back.setOnClickListener {
            finish()
        }

        logout_btn.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            auth.signOut()
        }
    }
}