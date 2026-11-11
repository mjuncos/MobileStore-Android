package com.mobilestore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobilestore.data.ServiceLocator
import com.mobilestore.ui.navigation.AppNavigation
import com.mobilestore.ui.theme.MobileStoreTheme

/**
 * Unica Activity de la aplicacion. Las cuatro pantallas se gestionan con
 * Navigation Compose, tal como habilita el punto 3.1 del enunciado.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val productRepository = ServiceLocator.productRepository(applicationContext)
        val cartRepository = ServiceLocator.cartRepository(applicationContext)

        setContent {
            MobileStoreTheme {
                AppNavigation(
                    productRepository = productRepository,
                    cartRepository = cartRepository
                )
            }
        }
    }
}
