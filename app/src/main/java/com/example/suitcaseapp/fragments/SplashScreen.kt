package com.example.suitcaseapp.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.suitcaseapp.R
import com.google.firebase.auth.FirebaseAuth

/**
 * This fragment is used to display a splash screen when the app is launched.
 * It checks if the user is signed in and navigates to the appropriate screen.
 */
@SuppressLint("CustomSplashScreen")
class SplashScreen : Fragment() {

    // Firebase authentication instance
    private lateinit var auth: FirebaseAuth

    // Navigation controller instance
    private lateinit var navController: NavController

    /**
     * Inflates the layout for this fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    /**
     * Called immediately after onCreateView has returned
     * Initializes Firebase and navigation controller, starts the fade-in animation, and navigates to the appropriate screen after a delay
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase and navigation controller
        auth = FirebaseAuth.getInstance()
        navController = Navigation.findNavController(view)

        // Start the fade-in animation
        val logoImageView: ImageView = view.findViewById(R.id.imageView)
        val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        logoImageView.startAnimation(fadeInAnimation)

        // Navigate to the appropriate screen after a delay
        Handler(Looper.myLooper()!!).postDelayed({
            if (auth.currentUser != null) {
                // User is signed in
                // Redirect to home page
                navController.navigate(R.id.action_splashFragment_to_homeFragment)
            } else {
                // User is not signed in
                // Redirect to login page
                navController.navigate(R.id.action_splashFragment_to_signInFragment)
            }
        }, 100)
    }
}