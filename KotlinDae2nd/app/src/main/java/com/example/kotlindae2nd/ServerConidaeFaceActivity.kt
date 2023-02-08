package com.example.kotlindae2nd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.kotlindae2nd.databinding.ActivityServerConidaeFaceBinding
import org.json.JSONObject

class ServerConidaeFaceActivity : AppCompatActivity(),InductionKonidae.StateListener {

    private lateinit var binding:ActivityServerConidaeFaceBinding
    private lateinit var key:BluetoothKommunication
    private lateinit var conidae:InductionKonidae

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerConidaeFaceBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        key = BluetoothKommunication(intent.getStringExtra("DEVICE_NAME")!!,this)
        conidae = InductionKonidae(key,this)
        conidae.statelistener = this
        conidae.state += InductionKonidae.STATE_CONIDAE//stateをコニダエとして登録。
        val faceThread = Thread{
            while (true){
                Thread.sleep(20000)
                serverDrive()
            }
        }
        faceThread.start()
    }
    private fun serverDrive(){
        val queue=Volley.newRequestQueue(this)
        var url="https://script.google.com/macros/s/AKfycbzIcRtXuKZvURu9HowEQSTGKcgEMN6Mg_cSIYxiXkXB6frjdV00zXOd5E-yQy4SKDJg/exec?"
        url += "comm=2&name=${intent.getStringExtra("USERNAME")}&${conidae.getStatusForQuery()}"
        val stringRequest= StringRequest(
            Request.Method.GET,url,
            { response ->println(response)
                //レスポンスは"コマンド番号#ペイロード"の形で文字列として返ってくる。
                val responseList = response.split("#")
                val command = responseList[0]
                val payload = responseList[1]
                if(command.toInt()==2){
                    val goal = payload.split(",")
                    conidae.drive(goal[0].toDouble(),goal[1].toDouble())
                }

            },
            {})
        queue.add(stringRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        conidae.finish()
    }
    override fun onStateChanged(state: Int){
        if(state.and(InductionKonidae.STATE_REVERSE)!=0){
            nothingFace()
        }
        else if(state.and(InductionKonidae.STATE_CONNECTION_ERR)!=0){
            troubleFace()
        }
        else{
            normalFace()
        }
    }
    //それぞれの表情について，changeFace()を呼び出す．
    fun fightFace(){
        changeFace(R.drawable.fightleft,R.drawable.fightright,R.drawable.fight)
    }
    private fun normalFace(){
        changeFace(R.drawable.nomalleft,R.drawable.nomalright,R.drawable.smile)
    }
    private fun troubleFace(){
        changeFace(R.drawable.troubleleft,R.drawable.troubleright,R.drawable.fight)
    }
    fun smileFace(){
        changeFace(R.drawable.smileright,R.drawable.smileleft,R.drawable.smile)
    }
    private fun nothingFace(){
        changeFace(R.drawable.nothingleft,R.drawable.nothingright,R.drawable.fight)
    }
    fun surpriseFace(){
        changeFace(R.drawable.nomalleft,R.drawable.nomalright,R.drawable.surprise)
    }
    private fun changeFace(left:Int, right:Int, mouth:Int){
        val rightEye = binding.imgRightEye
        val leftEye = binding.imgLeftEye
        val mMouth = binding.imgMouth
        runOnUiThread(kotlinx.coroutines.Runnable {
            rightEye.setImageResource(right)
            leftEye.setImageResource(left)
            mMouth.setImageResource(mouth)
        })
    }

}