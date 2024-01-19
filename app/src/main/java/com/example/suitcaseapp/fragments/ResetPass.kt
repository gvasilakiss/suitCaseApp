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
import com.example.suitcaseapp.databinding.FragmentResetPassBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * This fragment is used for resetting the user's password.
 * It allows the user to enter their email and request a password reset link.
 */
class ResetPass : Fragment() {

    // Navigation controller instance
    private lateinit var navController: NavController

    // Firebase authentication instance
    private lateinit var mAuth: FirebaseAuth

    // Binding instance for this fragment
    private lateinit var binding: FragmentResetPassBinding

    /**
     * Inflates the layout for this fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentResetPassBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called immediately after onCreateView has returned
     * Initializes Firebase and navigation controller, and sets up the event handlers
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize variables
        init(view)

        // Set click listener for reset password button
        binding.emailResetText.setOnClickListener {
            val email = binding.emailResetText.text.toString().trim()

            // Check if email is valid
            if (isValidEmail(email))
                resetPass(email)
            else
                Toast.makeText(
                    context,
                    getString(R.string.invalid_email_format),
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    /**
     * Checks if the given email is valid
     */
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex()) //Email should be in the correct format
    }

    /**
     * Sends a password reset email to the given email
     */
    private fun resetPass(email: String) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        context,
                        getString(R.string.email_sent_to_reset_your_password), Toast.LENGTH_SHORT
                    )
                        .show()
                    navController.navigate(R.id.action_resetPass_to_signInFragment)
                } else {
                    Toast.makeText(context, getString(R.string.email_not_sent), Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    /**
     * Initializes Firebase and navigation controller
     */
    private fun init(view: View) {
        navController = Navigation.findNavController(view)
        mAuth = FirebaseAuth.getInstance()
    }
}