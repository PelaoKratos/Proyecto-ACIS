package com.example.proyecto_acis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.proyecto_acis.ui.navigation.AcisApp
import com.example.proyecto_acis.ui.theme.ProyectoACISTheme
import com.example.proyecto_acis.ui.viewmodel.CatastroViewModel
import com.example.proyecto_acis.ui.viewmodel.ClimaViewModel
import com.example.proyecto_acis.ui.viewmodel.OrdenesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ordenesViewModel = ViewModelProvider(this)[OrdenesViewModel::class.java]
        val catastroViewModel = ViewModelProvider(this)[CatastroViewModel::class.java]
        val climaViewModel = ViewModelProvider(this)[ClimaViewModel::class.java]
        setContent {
            ProyectoACISTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AcisApp(viewModel = ordenesViewModel, catastroViewModel = catastroViewModel,
                        climaViewModel = climaViewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
