package com.paul.nutritiontracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity

/**
 * Plays the animated intro (res/raw/intro.mp4) full-screen on the brand
 * background color, then hands off to MainActivity. Falls through
 * immediately to MainActivity if the video fails to load for any reason,
 * so a bad video never blocks the app from opening.
 */
class SplashActivity : ComponentActivity() {

    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.splashVideoView)
        val uri = Uri.parse("android.resource://$packageName/${R.raw.intro}")

        videoView.setVideoURI(uri)
        videoView.setOnCompletionListener { goToMain() }
        videoView.setOnErrorListener { _, _, _ -> goToMain(); true }
        videoView.start()
    }

    private fun goToMain() {
        if (navigated) return
        navigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
