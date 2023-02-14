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
import com.google.android.gms.maps.model.*
import org.json.JSONObject

class CommanderMapActivity : AppCompatActivity(), OnMapReadyCallback ,SendGoalDialog.ListSelectedListener{
    private lateinit var shell:KShell
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityCommanderMapBinding
    private lateinit var detector: GestureDetectorCompat
    private var markerArray = mutableListOf<Marker>()
    private var state = InductionKonidae.STATE_OPERATOR
    private var gLat = 0.0
    private var gLong = 0.0
    private var recipientName = "NAME"

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
        //マップのセットアップ
        mMap.uiSettings.isZoomControlsEnabled=true
        mMap.uiSettings.isMapToolbarEnabled=true
        mMap.uiSettings.isCompassEnabled=true
        checkPermission()
        mMap.isMyLocationEnabled = true
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.setOnMapLongClickListener{
            mMap.addMarker(MarkerOptions().draggable(true).position(it).title("Goal"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(it))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(18.0f),3000,null)
        }
        mMap.setOnMarkerClickListener{
            run {
                Toast.makeText(this, "${it.position.latitude},${it.position.longitude} is selected", Toast.LENGTH_SHORT).show()
                val dialog = SendGoalDialog(markerArray)
                dialog.listSelectedListener = this

                dialog.show(supportFragmentManager,"title")
            }
            true
        }
    }

    private fun removeAllMarkers(){ //マーカーを全削除する関数
        for(marker in markerArray){
            try{
                println(marker.title)
                marker.remove()
            }
            catch(e:Exception){
                print("なにかしら問題が発生したんだえ")
            }
        }
        markerArray = mutableListOf()
    }

    override fun onListSelected(userName: String) {
        super.onListSelected(userName)
        Toast.makeText(this, "${userName}が選択されました", Toast.LENGTH_SHORT).show()
        recipientName = userName
        communicateServer(SET_GOAL)
    }
    private fun communicateServer(sendCommand:Int){//サーバー通信をする関数。渡されたコマンドによって動作内容を変える。
        val queue= Volley.newRequestQueue(this)
        var url="https://script.google.com/macros/s/AKfycbysbL-F44yfNugcDVajCX3-U7vdrFqe_GL8U8z0p1FmP9Z1P0Gg_PQJuKOuN92KSAFd/exec?"
        url += when (sendCommand) {
            UPDATE_MAP -> {//マップ更新
                "comm=$UPDATE_MAP"+"&name=${intent.getStringExtra("USERNAME")}&${makeExtraQuery(UPDATE_MAP)}"
            }
            SET_GOAL -> {
                "comm=$SET_GOAL"+"&name=${intent.getStringExtra("USERNAME")}&${makeExtraQuery(SET_GOAL)}"
            }
            else -> {
                return
            }
        }
        println(url)
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
                if(command.toInt()== UPDATE_MAP){//帰ってきたコマンドがマップ更新のとき
                    val retJson = JSONObject(payload)
                    val keys: Iterator<String> = retJson.keys()
                    removeAllMarkers()//一旦マップ上のマーカーを全消去
                    while(keys.hasNext()){
                        val key = keys.next()
                        val json = retJson.getJSONObject(key)
                        try{
                            val currentPosition = LatLng(json.getDouble("Latitude"),json.getDouble("Longitude"))
                            val usrName = json.getString("UserName")
                            val status = json.getInt("Status")
                            val option = createMarkOptionForStatus(currentPosition,usrName,status)
                            val marker = mMap.addMarker(option)
                            markerArray.add(marker!!)//マップにマーカーを追加しつつ、あとで一括管理をするためにリストに格納。
                        }catch (e:java.lang.Exception){
                            println("It does not Double!")
                        }
                    }
                }
                if(command.toInt()== SET_GOAL){
                    print(payload)
                }
            },
            {})
        queue.add(stringRequest)
    }
    private fun createMarkOptionForStatus(position:LatLng,usrName:String,status:Int): MarkerOptions {
        val returnOption = MarkerOptions().position(position).title(usrName)
        if(status.and(InductionKonidae.STATE_CONIDAE)!=0){
            returnOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        }
        if(status.and(InductionKonidae.STATE_NET_ERR)==0){
            returnOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        }
        return returnOption
    }
    private fun makeExtraQuery(sendCommand: Int):String{//コマンドによってクエリに追加する内容を変える関数。
        var sendQuery = "lat=${shell.nowLat.toString()}&long=${shell.nowLon.toString()}&state=$state"
        if(sendCommand== SET_GOAL){
            sendQuery+="&recipient=$recipientName&gLat=$gLat&gLong=$gLong&gUser=NONE"
        }
        return sendQuery

    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            detector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }
    fun communicate(){
        Toast.makeText(this, "更新中...", Toast.LENGTH_SHORT).show()
        communicateServer(UPDATE_MAP)
        Toast.makeText(this,"更新が完了しました！",Toast.LENGTH_SHORT).show()
    }

    private class MyGestureListener(mapAct:CommanderMapActivity):
        GestureDetector.SimpleOnGestureListener() {
        private var activity = mapAct
        override fun onDoubleTap(e: MotionEvent): Boolean {
            activity.communicate()
            return super.onDoubleTap(e)
        }
    }
    companion object{
        private const val UPDATE_MAP = 4
        private const val SET_GOAL = 8
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