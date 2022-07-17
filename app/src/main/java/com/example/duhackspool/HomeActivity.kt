package com.example.duhackspool

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.example.duhackspool.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)

        if (!LoginManager.loggedIn) finish()

        binding.nightMode.setOnClickListener {
            day()
        }

        binding.dayMode.setOnClickListener {
            night()
        }

        binding.dayMode.visibility = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) View.VISIBLE else View.INVISIBLE
        binding.nightMode.visibility = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) View.VISIBLE else View.INVISIBLE

        if (binding.dayMode.visibility == View.INVISIBLE && binding.nightMode.visibility == View.INVISIBLE) binding.dayMode.visibility = View.VISIBLE

        setContentView(binding.root)

        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.nameTxt.text = LoginManager.name


        binding.rideBtn.setOnClickListener {
            startActivity(Intent(this@HomeActivity, RideActivity::class.java))
        }

        if (LoginManager.isDriver) {
            binding.driveBtn.visibility = View.VISIBLE
            binding.driveBtn.setOnClickListener {
                startActivity(Intent(this@HomeActivity, DriveActivity::class.java))
            }
        } else {
            binding.driveBtn.visibility = View.GONE
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