package com.example.suitcaseapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.suitcaseapp.R
import com.example.suitcaseapp.databinding.FragmentSignInBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class SignIn : Fragment() {

    private lateinit var navController: NavController
    private lateinit var mAuth: FirebaseAuth
    private lateinit var binding: FragmentSignInBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)

        // Check if user is already logged in
        val currentUser = mAuth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            navController.navigate(R.id.action_signInFragment_to_homeFragment)
            return
        }

        binding.signUpBtn.setOnClickListener {
            navController.navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        // Redirect to forgot password page if user clicks on "Forgot password?"
        binding.forgotPasswordRedirect.setOnClickListener {
            navController.navigate(R.id.action_signInFragment_to_resetPass)
        }

        binding.signInBtn.setOnClickListener {
            val email = binding.signInEmailEditText.text.toString().trim()
            val pass = binding.signInPasswordText.text.toString()

            if (isValidEmail(email) && isValidPassword(pass))
                loginUser(email, pass)
            else
                Toast.makeText(context, getString(R.string.invalid_email_or_password), Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex()) //Email should be in the correct format
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6 //Password should have at least 6 characters
    }

    private fun loginUser(email: String, pass: String) {
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { signInTask ->
            if (signInTask.isSuccessful) {
                val user = mAuth.currentUser
                if (user != null && user.isEmailVerified) {
                    navController.navigate(R.id.action_signInFragment_to_homeFragment)
                } else {
                    Toast.makeText(context,
                        getString(R.string.email_is_not_verified), Toast.LENGTH_SHORT).show()
                    mAuth.signOut()
                }
            } else {
                when (val exception = signInTask.exception) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        Toast.makeText(context, getString(R.string.invalid_email_or_password), Toast.LENGTH_SHORT).show()
                    }

                    is FirebaseNetworkException -> {
                        Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                    }

                    else -> {
                        Toast.makeText(context, exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun init(view: View) {
        navController = Navigation.findNavController(view)
        mAuth = FirebaseAuth.getInstance()
    }
}