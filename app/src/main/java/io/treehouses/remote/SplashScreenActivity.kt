package io.treehouses.remote

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import io.treehouses.remote.InitialActivity

class SplashScreenActivity : AppCompatActivity() {
    var logoAnimation: Animation? = null
    var textAnimation: Animation? = null
    var logo: ImageView? = null
    var logoText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this@SplashScreenActivity)
        nightMode()
        if (preferences.getBoolean("splashScreen", true)) {
            setContentView(R.layout.activity_splash_screen)
            logo = findViewById(R.id.splash_logo)
            logoAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_logo_anim)
            logo?.animation = logoAnimation
            logoText = findViewById(R.id.logo_text)
            textAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_text_anim)
            logoText?.animation = textAnimation
            Handler().postDelayed({
                val intent = Intent(this@SplashScreenActivity, InitialActivity::class.java)
                startActivity(intent)
                finish()
            }, SPLASH_TIME_OUT.toLong())
        } else {
            val intent = Intent(this@SplashScreenActivity, InitialActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    companion object {
        private const val SPLASH_TIME_OUT = 2000
    }

    fun nightMode() {
        val preference = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getString("dark_mode", "Follow System")
        val options = listOf(*resources.getStringArray(R.array.dark_mode_options))
        val optionsCode = resources.getStringArray(R.array.led_options_commands)
        val selected_mode = options.indexOf(preference)
        when (selected_mode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}