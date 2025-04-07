package com.example.scannerkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scannerkotlin.R
import com.example.scannerkotlin.service.UserService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

@Suppress("DEPRECATION")
class AuthActivity : AppCompatActivity() {

    private var userId: String? = null
    private var userService: UserService? = null
    private var passwords: MutableList<String> = mutableListOf()
    private val users: MutableMap<String, String> = mutableMapOf<String, String>()
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var inputLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        userService = UserService()


        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        inputLayout = findViewById(R.id.passwordInputLayout)


        userService?.getPasswords { passwordPairs ->
            passwordPairs?.forEach { (id, password) ->
                passwords.add(password)
                users[id] = password
            }
        }

        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                inputLayout.error = null
                inputLayout.isPasswordVisibilityToggleEnabled = true
            }
        }

        passwordEditText.setOnClickListener {
            inputLayout.error = null
            inputLayout.isPasswordVisibilityToggleEnabled = true
        }
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            val enteredPassword = passwordEditText.text.toString().trim()

            if (enteredPassword.isEmpty()) {
                showError("Введите пароль")
                return@setOnClickListener
            }

            if (passwords.contains(enteredPassword)) {
                userId = users.entries.find { it.value == enteredPassword }?.key
                val intent: Intent = Intent(this, MainActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
                finish()
            } else {
                showError("Неверный пароль")
            }
        }
    }

    private fun showError(message: String) {
        inputLayout.error = message
        passwordEditText.requestFocus()
    }
}