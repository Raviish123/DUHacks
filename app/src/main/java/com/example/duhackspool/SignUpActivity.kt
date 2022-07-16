package com.example.duhackspool

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.duhackspool.databinding.ActivitySignUpBinding
import com.google.android.material.snackbar.Snackbar


class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (LoginManager.loggedIn) finish()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.signUpBtn.setOnClickListener {
            if (binding.nameField.text.toString() != "" && binding.editTextTextEmailAddress.text.toString() != "" && binding.editTextPhone.toString() != "") {
                if (!Patterns.EMAIL_ADDRESS.matcher(binding.editTextTextEmailAddress.text).matches()) {
                    Snackbar.make(findViewById(android.R.id.content), "Enter Valid Email Address.", Snackbar.LENGTH_SHORT).show()
                } else {
                    LoginManager.name = binding.nameField.text.toString()
                    LoginManager.isDriver = binding.switch1.isChecked
                    LoginManager.emailAddress = binding.editTextTextEmailAddress.text.toString()
                    LoginManager.phoneNumber = binding.editTextPhone.text.toString()
                    LoginManager.password = binding.editTextPassword.text.toString()
                    LoginManager.loggedIn = true
                    startActivity(Intent(this@SignUpActivity, HomeActivity::class.java))
                    finish()
                }
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Fill in all fields.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}