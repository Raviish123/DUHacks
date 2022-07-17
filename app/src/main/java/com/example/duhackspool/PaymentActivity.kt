package com.example.duhackspool

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.example.duhackspool.databinding.ActivityPaymentBinding

class PaymentActivity : AppCompatActivity() {

    private var paymentAmount = 0.0

    private lateinit var binding: ActivityPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityPaymentBinding.inflate(layoutInflater)

        binding.nightMode.setOnClickListener {
            day()
        }

        binding.dayMode.setOnClickListener {
            night()
        }

        binding.dayMode.visibility = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) View.VISIBLE else View.INVISIBLE
        binding.nightMode.visibility = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) View.VISIBLE else View.INVISIBLE

        setContentView(binding.root)


        paymentAmount = intent.getDoubleExtra("paymentAmount", 0.0)

        val isDriver = intent.getBooleanExtra("isDriver", false)

        if (isDriver) {
            binding.textView2.text = "You have received a total of"
        } else {
            binding.textView2.text = "You are required to pay a total of"
        }

        binding.paymentText.text = "₹${paymentAmount}"

        binding.continueBtn.setOnClickListener {
            finish()
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