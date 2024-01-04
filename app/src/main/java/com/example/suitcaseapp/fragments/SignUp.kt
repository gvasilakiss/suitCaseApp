package com.example.suitcaseapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.suitcaseapp.R
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.suitcaseapp.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class SignUp : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var binding: FragmentSignUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        registerEvents()
    }

    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()

        val signUpButton = view.findViewById<Button>(R.id.loginBtn)
        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                createUser(email, password)
            } else {
                // Show an error message for empty fields
            }
        }

        // Redirect to login page if user clicks on "Already have an account?"
        val loginRedirect = view.findViewById<TextView>(R.id.nextBtn)
        loginRedirect.setOnClickListener {
        }
    }

    private fun registerEvents() {

        // Redirect to login page if user clicks on Login button"
        binding.loginBtn.setOnClickListener {
            navControl.navigate(R.id.action_signUpFragment_to_signInFragment)
        }

        binding.signUpButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()){
                createUser(email, password)
            } else {
                // Show an error message for empty fields
            }
        }
    }
    // Create a new user with email and password - check if email and password are valid
    private fun createUser(email: String, password: String) {
        if (isValidEmail(email) && isValidPassword(password)) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.sendEmailVerification()?.addOnCompleteListener { emailTask -> // Send verification email
                            if (emailTask.isSuccessful) {
                                Toast.makeText(context, "Registered successfully and Verification email sent.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Registered successfully but Failed to send verification email.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        navControl.navigate(R.id.action_signUpFragment_to_homeFragment)
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthUserCollisionException) {
                            // Email already exists
                            Toast.makeText(context, "Email already exists.", Toast.LENGTH_SHORT).show()
                        } else {
                            // Other authentication failures
                            Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        } else {
            Toast.makeText(context, "Invalid email or password format", Toast.LENGTH_SHORT).show()
        }
    }



    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex()) // Email should match the email pattern
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6 // Password should have at least 6 characters
    }


}