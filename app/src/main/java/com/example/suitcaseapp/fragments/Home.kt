package com.example.suitcaseapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.suitcaseapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.RecyclerView
import com.example.suitcaseapp.HolidayAdapter


class Home : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var firestore: FirebaseFirestore

    private lateinit var recyclerView: RecyclerView
    private lateinit var menuBtn: ImageButton
    private lateinit var holidayAdapter: HolidayAdapter



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        setupAddButton()
        //registerEvents()
    }

    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    private fun showMenu() {

    }

    private fun setupRecyclerView() {

    }

//    private fun registerEvents() {
//        view?.findViewById<Button>(R.id.logOutBtn)?.setOnClickListener {
//            logoutUser()
//        }

//    private fun logoutUser() {
//        auth.signOut()
//        navControl.navigate(R.id.action_homeFragment_to_signInFragment)
//    }
    private fun setupAddButton() {
        //on click button navigate to notesDetails
        val addButton = view?.findViewById<FloatingActionButton>(R.id.addButton) ?: return
        addButton.setOnClickListener {
            // Check if navControl is initialized and if the destination exists in the navigation graph
            navControl.navigate(R.id.action_homeFragment_to_notesDetails)
        }
    }
}