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

class ResetPass : Fragment() {

    private lateinit var navController: NavController
    private lateinit var mAuth: FirebaseAuth
    private lateinit var binding: FragmentResetPassBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentResetPassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)

        binding.emailResetText.setOnClickListener {
            val email = binding.emailResetText.text.toString().trim()

            if (isValidEmail(email))
                resetPass(email)
            else
                Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex()) //Email should be in the correct format
    }

    private fun resetPass(email: String) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Email sent to reset your password", Toast.LENGTH_SHORT).show()
                    navController.navigate(R.id.action_resetPass_to_signInFragment)
                } else {
                    Toast.makeText(context, "Email not sent", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun init(view: View) {
        navController = Navigation.findNavController(view)
        mAuth = FirebaseAuth.getInstance()
    }
}