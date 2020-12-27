package com.rpljumat.ti_tipagent

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_peta_daftar_agen.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PetaDaftarAgen : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var conn = false

    private var lat = 0.toDouble()
    private var long = 0.toDouble()

    companion object{
        const val MY_PERMISSION_ACCESS_FINE_LOCATION = 1
        const val MY_PERMISSION_ACCESS_COARSE_LOCATION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peta_daftar_agen)

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

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        Places.initialize(this, "AIzaSyBQrHz10Ki-k3pj-NP6CpAxn5TXa_o55zw")
        initAutoCompleteFragment()

        back.setOnClickListener {
            finish()
        }

        done.setOnClickListener {
            val addr = choosen_agent_loc.text

            // Pass address data to registration page
            val alamatRegis = Intent(this, RegistrasiAgent::class.java)
            alamatRegis.putExtra("Alamat", "$addr")
            alamatRegis.putExtra("Lintang", "$lat")
            alamatRegis.putExtra("Bujur", "$long")
            setResult(Activity.RESULT_OK, alamatRegis)

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
                finish()
            }
        builder.show()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSION_ACCESS_FINE_LOCATION
            )
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                MY_PERMISSION_ACCESS_COARSE_LOCATION
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener {
                var latitude = it?.latitude
                var longitude = it?.longitude
                if(latitude == null || longitude == null){
                    latitude = -6.208763
                    longitude = 106.845599
                }
                val loc = LatLng(latitude, longitude)

                googleMap.moveCamera(
                    CameraUpdateFactory
                        .newLatLngZoom(loc, 15f)
                )

                googleMap.setOnMapClickListener { point: LatLng ->
                    googleMap.clear()
                    googleMap.addMarker(MarkerOptions().position(point))

                    lat = point.latitude
                    long = point.longitude
                    val addr = getAgentLoc(Pair(lat, long), this)
                    choosen_agent_loc.text = addr

                    done.visibility = View.VISIBLE
                }
            }
    }

    private fun initAutoCompleteFragment() {
        val autoCompleteFragment = supportFragmentManager
            .findFragmentById(R.id.search_field_loc) as AutocompleteSupportFragment
        autoCompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))
        autoCompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {

            }
            override fun onError(status: Status) {
//                TODO("Not yet implemented")
                Log.d("PetaPilihAgen", "$status")
            }
        })
    }
}