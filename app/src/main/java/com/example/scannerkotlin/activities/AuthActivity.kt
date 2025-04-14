package com.example.scannerkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.scannerkotlin.R

import com.example.scannerkotlin.utils.SessionManager
import com.example.scannerkotlin.service.UserService
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {

    private var userService: UserService? = null

    private val users: MutableMap<String, String> = mutableMapOf()
    private val enteredPassword = StringBuilder()
    private lateinit var dotViews: List<ImageView>

    private val authScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (SessionManager.isUserLoggedIn()) {
            Log.i("AuthActivity", "User already logged in (userId: ${SessionManager.getUserId()}). Redirecting to MainActivity.")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

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


                users.clear()


                passwordPairs?.forEach { (id, password) ->
                    Log.d("AuthActivity", "Loading Pass: ***, Id: $id")

                    users[id] = password
                }
                Log.i("AuthActivity", "Passwords loaded successfully for ${users.size} users.")
            } catch (e: Exception) {

                Log.e("AuthActivity", "Error loading passwords", e)
                Toast.makeText(this@AuthActivity, "Ошибка загрузки данных для входа. Попробуйте позже.", Toast.LENGTH_LONG).show()

            }
        }
    }

    private fun setupKeypadButtons() {

        val buttonIds = listOf(
            R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6,
            R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btn0
        )


        buttonIds.forEach { buttonId ->
            findViewById<MaterialButton>(buttonId).setOnClickListener { button ->
                if (enteredPassword.length < 4) {
                    enteredPassword.append((button as MaterialButton).text)
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
            val isFilled = index < enteredPassword.length
            val drawableRes = if (isFilled) R.drawable.circle_filled else R.drawable.circle_outline

            val colorRes = R.color.black

            imageView.setImageResource(drawableRes)
            imageView.imageTintList = ContextCompat.getColorStateList(this, colorRes)
        }
    }


    private fun checkPassword() {
        val enteredPass = enteredPassword.toString()


        val userId = users.entries.find { it.value == enteredPass }?.key

        if (userId != null) {

            Log.i("AuthActivity", "Password correct for userId: $userId. Saving session.")


            SessionManager.saveSession(userId)


            val intent = Intent(this, MainActivity::class.java)

            startActivity(intent)
            finish()
        } else {

            Log.w("AuthActivity", "Incorrect password entered.")
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