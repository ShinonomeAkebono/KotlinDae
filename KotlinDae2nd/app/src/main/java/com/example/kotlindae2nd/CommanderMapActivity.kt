package com.example.kotlindae2nd

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GestureDetectorCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.kotlindae2nd.databinding.ActivityCommanderMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject

class CommanderMapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var shell:KShell
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityCommanderMapBinding
    private lateinit var detector: GestureDetectorCompat
    private var state = InductionKonidae.STATE_OPERATOR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommanderMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        shell = KShell(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        detector = GestureDetectorCompat(this,MyGestureListener(this))
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
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled=true
        mMap.uiSettings.isMapToolbarEnabled=true
        mMap.uiSettings.isCompassEnabled=true
        checkPermission()
        mMap.isMyLocationEnabled = true
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

    }
    private fun refreshMap(){
        val queue= Volley.newRequestQueue(this)
        var url="https://script.google.com/macros/s/AKfycbysbL-F44yfNugcDVajCX3-U7vdrFqe_GL8U8z0p1FmP9Z1P0Gg_PQJuKOuN92KSAFd/exec?"
        url += "comm=4&name=${intent.getStringExtra("USERNAME")}&${makeQuery()}"
        print(url)
        val stringRequest= StringRequest(
            Request.Method.GET,url,
            { response ->println(response)
                //レスポンスは"コマンド番号#ペイロード"の形で文字列として返ってくる。
                val responseList = response.split("#")
                val command = responseList[0]
                val payload = responseList[1]
                if(command.toInt()==2){
                    print(response)
                }
                if(command.toInt()==4){
                    val retJson = JSONObject(payload)
                    val keys: Iterator<String> = retJson.keys()
                    while(keys.hasNext()){
                        val key = keys.next()
                        val json = retJson.getJSONObject(key)
                        try{
                            val currentPosition = LatLng(json.getDouble("Latitude"),json.getDouble("Longitude"))
                            val option = MarkerOptions().position(currentPosition).title(json.getString("UserName"))
                            mMap.addMarker(option)
                        }catch (e:java.lang.Exception){
                            print("It does not Double!")
                        }
                    }
                }
            },
            {})
        queue.add(stringRequest)
    }
    private fun makeQuery():String{
        return "lat=${shell.nowLat.toString()}&long=${shell.nowLon.toString()}&state=$state"
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            detector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }
    fun refresh(){
        Toast.makeText(this, "更新中...", Toast.LENGTH_SHORT).show()
        refreshMap()
    }
    private class MyGestureListener(mapAct:CommanderMapActivity):
        GestureDetector.SimpleOnGestureListener() {
        private var activity = mapAct
        override fun onDoubleTap(e: MotionEvent): Boolean {
            activity.refresh()
            return super.onDoubleTap(e)
        }
    }
    companion object{
        private const val REQUEST_CODE_PERMISSIONS = 90
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).toTypedArray()
    }
    private fun checkPermission(){//パーミッションの関数。基本コピペ
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this as Activity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
}