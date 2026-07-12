package com.beatsandbeyond.video_editor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.beatsandbeyond.video_editor.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity in B&B Video Editor.
 *
 * This is a "shell" Activity — it provides only the navigation host container.
 * All UI logic, business logic, and state management lives in Fragments and ViewModels.
 *
 * Responsibilities:
 * - Inflate the nav host layout
 * - Set up the NavController
 * - Configure edge-to-edge display
 *
 * Future: Handle deep links, back press customization, and system UI visibility changes.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge rendering for immersive video editor experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
    }
}