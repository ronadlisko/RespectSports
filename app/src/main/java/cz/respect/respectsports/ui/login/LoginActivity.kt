package cz.respect.respectsports.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cz.respect.respectsports.MainActivity
import cz.respect.respectsports.R
import cz.respect.respectsports.databinding.ActivityLoginBinding
import java.lang.Exception


class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    private var loginByUser: Boolean = false
    private var tokenChecked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MY_INFO", "LOGIN ACTIVITY STARTED")

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading

        fun startLoading() {
            username.isEnabled = false
            password.isEnabled = false
            login.isEnabled = false
            loading.visibility = View.VISIBLE
        }

        fun startTokenLoading() {
                startLoading()
                tokenChecked = true
        }

        fun startButtonLoginLoading() {
            if (username.length() > 0 && password.length() > 4) {
                startLoading()
                loginByUser = true
                loginViewModel.login(username.text.toString(), password.text.toString())
            }
        }

        fun endLoginLoading() {
            username.isEnabled = true
            password.isEnabled = true
            login.isEnabled = true
            loading.visibility = View.INVISIBLE
        }

        loginViewModel = ViewModelProvider(this)
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.message.observe(this, Observer {
            Log.i("MY_INFO", "OBSERVING DONE")
            endLoginLoading()
            showResultMessage(it)
        })

        fun finalLogin(id:String?, name:String?, token:String?) {
            val loginResult =
                LoginResult(success = LoggedInUserView(displayName = name!!))
            Log.i("MY_INFO", "RESULT OBTAINED")

            if (loginResult.error != null) {
                endLoginLoading()
                showLoginFailed(loginResult.error!!)
            }

            if (loginResult.success != null) {
                endLoginLoading()
                updateUiWithUser(loginResult.success)

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("username", name)
                intent.putExtra("userId", id)
                intent.putExtra("userToken", token)
                startActivity(intent)
                finish()
            }
        }

        loginViewModel.loggedUser?.observe(this@LoginActivity, Observer {
            Log.i("MY_INFO", "LOGGED USER OBTAINED")
            if (it != null) {

                if (loginByUser) {
                    Log.i("MY_INFO", "LOGIN BY BUTTON")
                    finalLogin(it.id,it.name,it.token)

                }

                else if (!tokenChecked) {
                    startTokenLoading()
                    Log.i("MY_INFO", "TOKEN CHECK")
                    try {
                        loginViewModel.checkToken(it.token!!)
                        loginViewModel.loginResult.observe(this) {
                            Log.i("MY_INFO", "LOGIN RESULT OBTAINED")
                            finalLogin(loginViewModel.loggedUser.value!!.id, loginViewModel.loggedUser.value!!.name, loginViewModel.loggedUser.value!!.token)
                        }
                    }
                    catch (exception: Exception) {
                        Log.i("MY_INFO", "EXCEPTION")
                    }
                }
            }

            //Complete and destroy login activity once successful
            //finish()
        })



        /*
        loginViewModel.loggedUserValidToken.observe(this) {
            if (it. != null) {
                /*
            val resultIntent = Intent(this, MainActivity::class.java)
            resultIntent.putExtra("username", it.success?.displayName)
            setResult(Activity.RESULT_OK, resultIntent)
             */
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("username", it.name)
                intent.putExtra("userId", it.id)
                intent.putExtra("userToken", it.token)
                startActivity(intent)
                finish()
            }
        }

         */

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }



            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        startButtonLoginLoading()
                }
                false
            }

            login.setOnClickListener {
                startButtonLoginLoading()
            }

        }

    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val userWelcomeStart = getString(R.string.user_welcome_start)
        val userWelcomeEnd = getString(R.string.user_welcome_end)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$userWelcomeStart $displayName $userWelcomeEnd",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }


    private fun showResultMessage(apiResponseString:String) {
        Toast.makeText(
            this,
            apiResponseString,
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}