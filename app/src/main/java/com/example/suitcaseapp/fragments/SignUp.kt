package com.example.suitcaseapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.suitcaseapp.R
import com.example.suitcaseapp.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

/**
 * This fragment is used for user registration.
 * It allows the user to sign up with an email and password.
 */
class SignUp : Fragment() {

    // Firebase authentication instance
    private lateinit var auth: FirebaseAuth

    // Navigation controller instance
    private lateinit var navControl: NavController

    // Binding instance for this fragment
    private lateinit var binding: FragmentSignUpBinding

    /**
     * Inflates the layout for this fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called immediately after onCreateView has returned
     * Initializes Firebase and navigation controller, and sets up the event handlers
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        registerEvents()
    }

    /**
     * Initializes Firebase and navigation controller
     */
    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
    }

    /**
     * Sets up the event handlers for the buttons
     */
    private fun registerEvents() {

        // Redirect to login page if user clicks on Login button
        binding.loginBtn.setOnClickListener {
            navControl.navigate(R.id.action_signUpFragment_to_signInFragment)
        }

        // Create a new user if user clicks on Sign Up button
        binding.signUpButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                createUser(email, password)
            } else {
                // Show an error message for empty fields
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(getString(R.string.please_fill_in_all_the_fields))
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    /**
     * Creates a new user with the given email and password
     * Checks if the email and password are valid before creating the user
     */
    private fun createUser(email: String, password: String) {
        if (isValidEmail(email) && isValidPassword(password)) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { emailTask -> // Send verification email
                                if (emailTask.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        getString(R.string.registered_successfully_and_verification_email_sent),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        getString(R.string.registered_successfully_but_failed_to_send_verification_email),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        navControl.navigate(R.id.action_signUpFragment_to_homeFragment)
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthUserCollisionException) {
                            // Email already exists
                            Toast.makeText(
                                context,
                                getString(R.string.email_already_exists), Toast.LENGTH_SHORT
                            )
                                .show()
                        } else {
                            // Other authentication failures
                            Toast.makeText(
                                context,
                                getString(R.string.authentication_failed), Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
        } else {
            Toast.makeText(
                context,
                getString(R.string.invalid_email_or_password_format), Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Checks if the given email is valid
     */
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex()) // Email should match the email pattern
    }

    /**
     * Checks if the given password is valid
     */
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6 // Password should have at least 6 characters
    }
}