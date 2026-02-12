package com.arduia.expense.ui.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import com.arduia.expense.ui.MainActivity

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: LoginViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()
                LaunchedEffect(uiState.success) {
                    if (uiState.success) {
                        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
                LoginScreen(viewModel = viewModel)
            }
        }
    }
}
