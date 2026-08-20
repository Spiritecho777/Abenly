package com.example.abenly

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.abenly.AppNavigation

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstancesState: Bundle?) {
        super.onCreate(savedInstancesState)
        setContent {
            AppNavigation()
        }
    }
}