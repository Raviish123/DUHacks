package com.example.duhackspool

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.example.duhackspool.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (LoginManager.loggedIn) finish()

        binding = ActivityLoginBinding.inflate(layoutInflater)

        binding.nightMode.setOnClickListener {
            day()
        }

        binding.dayMode.setOnClickListener {
            night()
        }

        binding.dayMode.visibility = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) View.VISIBLE else View.INVISIBLE
        binding.nightMode.visibility = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) View.VISIBLE else View.INVISIBLE

        setContentView(binding.root)

        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        binding.loginBtn.setOnClickListener {
            if (binding.nameField.text.toString() != "" && binding.editTextPassword.text.toString() != "") {
                LoginManager.name = binding.nameField.text.toString()
                LoginManager.password = binding.editTextPassword.text.toString()
                LoginManager.loggedIn = true
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                finish()
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Fill in all fields.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun night() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        delegate.applyDayNight()
        binding.nightMode.visibility = View.VISIBLE
        binding.dayMode.visibility = View.INVISIBLE
    }

    private fun day() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        delegate.applyDayNight()
        binding.dayMode.visibility = View.VISIBLE
        binding.nightMode.visibility = View.INVISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}