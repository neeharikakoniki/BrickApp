package com.brick.earthquaketracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.brick.earthquaketracker.ui.list.EarthquakeListViewModel
import com.brick.earthquaketracker.ui.navigation.QuakesNavHost
import com.brick.earthquaketracker.ui.theme.BrickAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val listViewModel: EarthquakeListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BrickAppTheme {
                val navController = rememberNavController()
                QuakesNavHost(
                    navController = navController,
                    listViewModel = listViewModel,
                )
            }
        }
    }
}
