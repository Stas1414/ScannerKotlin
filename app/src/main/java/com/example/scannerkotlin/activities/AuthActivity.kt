package com.example.scannerkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.scannerkotlin.R
import com.example.scannerkotlin.service.UserService
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {

    private var userService: UserService? = null
    private val passwords: MutableList<String> = mutableListOf()
    private val users: MutableMap<String, String> = mutableMapOf()
    private val enteredPassword = StringBuilder()
    private lateinit var dotViews: List<ImageView>
    private val authScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        userService = UserService()
        dotViews = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )

        setupKeypadButtons()
        loadPasswords()
    }

    private fun loadPasswords() {
        authScope.launch {
            try {
                val passwordPairs = withContext(Dispatchers.IO) {
                    userService?.getPasswords()
                }

                passwordPairs?.forEach { (id, password) ->
                    Log.d("Entry", "Pass: $password, Id: $id")
                    passwords.add(password)
                    users[id] = password
                }
            } catch (e: Exception) {
                Log.e("AuthActivity", "Error loading passwords", e)
                Toast.makeText(this@AuthActivity, "Ошибка загрузки паролей", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupKeypadButtons() {
        val buttons = listOf(
            findViewById(R.id.btn1),
            findViewById(R.id.btn2),
            findViewById(R.id.btn3),
            findViewById(R.id.btn4),
            findViewById(R.id.btn5),
            findViewById(R.id.btn6),
            findViewById(R.id.btn7),
            findViewById(R.id.btn8),
            findViewById(R.id.btn9),
            findViewById<MaterialButton>(R.id.btn0)
        )

        buttons.forEach { button ->
            button.setOnClickListener {
                if (enteredPassword.length < 4) {
                    enteredPassword.append(button.text)
                    updatePasswordDots()

                    if (enteredPassword.length == 4) {
                        checkPassword()
                    }
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            if (enteredPassword.isNotEmpty()) {
                enteredPassword.deleteCharAt(enteredPassword.length - 1)
                updatePasswordDots()
            }
        }
    }

    private fun updatePasswordDots() {
        dotViews.forEachIndexed { index, imageView ->
            if (index < enteredPassword.length) {
                imageView.setImageResource(R.drawable.circle_filled)
                imageView.imageTintList = ContextCompat.getColorStateList(this, R.color.black)
            } else {
                imageView.setImageResource(R.drawable.circle_outline)
                imageView.imageTintList = ContextCompat.getColorStateList(this, R.color.black)
            }
        }
    }

    private fun checkPassword() {
        val password = enteredPassword.toString()

        if (passwords.contains(password)) {
            val userId = users.entries.find { it.value == password }?.key
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("userId", userId)
            }
            Log.d("AuthActivity", "userId: $userId")

            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
            enteredPassword.clear()
            updatePasswordDots()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authScope.cancel()
    }
}