package com.example.todolist2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.example.todolist2.R

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        var todoSplash = findViewById<TextView>(R.id.todoSplash)
        var anim = AnimationUtils.loadAnimation(this,R.anim.fadein)
        anim.duration = 1500
        todoSplash.startAnimation(anim)
        Handler().postDelayed({
            startActivity(Intent(this@SplashScreen,MainActivity2::class.java))
            finish()
        },2000)
    }
}