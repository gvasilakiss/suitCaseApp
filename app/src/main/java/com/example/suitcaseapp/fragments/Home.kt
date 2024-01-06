package com.example.suitcaseapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.example.suitcaseapp.HolidayAdapter
import com.example.suitcaseapp.R
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Home : Fragment() {
    // Firebase authentication instance
    private lateinit var auth: FirebaseAuth
    // Navigation controller instance
    private lateinit var navControl: NavController
    // Firestore database instance
    private lateinit var firestore: FirebaseFirestore

    // Inflate the layout for this fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    // Called immediately after onCreateView has returned
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase and navigation controller
        init(view)
        // Setup the newHoliday button click listener
        addNewHoliday()
        // Setup the RecyclerView
        setupRecyclerView()
    }

    // Initialize Firebase and navigation controller
    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

    }

    // Empty function, can be used to show a menu
    private fun showMenu() {

    }

    // Setup the RecyclerView
    private fun setupRecyclerView() {
        // Initialize the RecyclerView
        val recyclerView = view?.findViewById<RecyclerView>(R.id.holidayRecyclerView)

        // Fetch the holiday data from Firestore
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val email = user.email
            if (email != null) {
                val query = firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION)

                // Configure the adapter options
                val options = FirestoreRecyclerOptions.Builder<HolidayDetails.Holiday>()
                    .setQuery(query, HolidayDetails.Holiday::class.java)
                    .build()

                // Initialize the adapter
                val adapter = HolidayAdapter(options)

                // Set the adapter on the RecyclerView
                recyclerView?.adapter = adapter

                // Start listening for Firestore updates
                adapter.startListening()
            }
        }
    }

    // Setup the add button click listener
    private fun addNewHoliday() {
        //on click button navigate to notesDetails
        val addButton = view?.findViewById<FloatingActionButton>(R.id.addButton) ?: return
        addButton.setOnClickListener {
            // Check if navControl is initialized and if the destination exists in the navigation graph
            navControl.navigate(R.id.action_homeFragment_to_notesDetails)
        }
    }

    companion object {
        // Constant for the name of the 'holidays' collection in Firestore
        private const val HOLIDAYS_COLLECTION = "holidays"
        // Constant for the name of the 'users' collection in Firestore
        private const val USERS_COLLECTION = "users"
    }
}