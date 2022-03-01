package com.example.todolist2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import com.example.todolist2.databinding.ActivitySplashScreenBinding

class SplashScreen : AppCompatActivity() {
    private lateinit var binding:ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val anim = AnimationUtils.loadAnimation(this,R.anim.fadein)
        anim.duration = 1500
        binding.todoTitleSplash.startAnimation(anim)
        Handler().postDelayed({
            startActivity(Intent(this@SplashScreen,MainActivity::class.java))
            finish()
        },2000)
    }
}