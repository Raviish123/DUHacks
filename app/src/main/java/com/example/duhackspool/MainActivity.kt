package com.example.duhackspool

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.example.duhackspool.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            supportActionBar!!.hide()
        } catch (e: NullPointerException) {

        }
    }

    override fun onResume() {
        super.onResume()

        if (LoginManager.loggedIn) {
            binding.frontPageBtn1.text = "Proceed"
            binding.frontPageBtn1.setOnClickListener {
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
            }
            binding.frontPageBtn2.visibility = View.INVISIBLE
        } else {
            binding.frontPageBtn1.text = "Login"
            binding.frontPageBtn1.setOnClickListener {
//                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            }
            binding.frontPageBtn2.text = "Sign Up"
            binding.frontPageBtn2.setOnClickListener {
                startActivity(Intent(this@MainActivity, SignUpActivity::class.java))
            }
        }
    }
}