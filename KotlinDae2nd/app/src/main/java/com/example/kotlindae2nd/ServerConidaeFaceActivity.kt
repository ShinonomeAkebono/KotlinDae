package com.example.kotlindae2nd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlindae2nd.databinding.ActivityServerConidaeFaceBinding

class ServerConidaeFaceActivity : AppCompatActivity() {

    private lateinit var binding:ActivityServerConidaeFaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerConidaeFaceBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val faceThread = Thread{
            for (i in 0..3) {
                fightFace()
                Thread.sleep(1000)
                normalFace()
                Thread.sleep(1000)
                troubleFace()
                Thread.sleep(1000)
                smileFace()
                Thread.sleep(1000)
                nothingFace()
                Thread.sleep(1000)
                surpriseFace()
                Thread.sleep(1000)
            }
        }
        faceThread.start()
    }
    //それぞれの表情について，changeFace()を呼び出す．
    fun fightFace(){
        changeFace(R.drawable.fightleft,R.drawable.fightright,R.drawable.fight)
    }
    fun normalFace(){
        changeFace(R.drawable.nomalleft,R.drawable.nomalright,R.drawable.smile)
    }
    fun troubleFace(){
        changeFace(R.drawable.troubleleft,R.drawable.troubleright,R.drawable.fight)
    }
    fun smileFace(){
        changeFace(R.drawable.smileright,R.drawable.smileleft,R.drawable.smile)
    }
    fun nothingFace(){
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