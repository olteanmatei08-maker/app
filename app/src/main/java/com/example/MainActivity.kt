package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ShoppingScreen
import com.example.ui.ShoppingViewModel
import com.example.ui.theme.ClujShopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClujShopTheme {
                val viewModel: ShoppingViewModel = viewModel()
                ShoppingScreen(viewModel = viewModel)
            }
        }
    }
}
