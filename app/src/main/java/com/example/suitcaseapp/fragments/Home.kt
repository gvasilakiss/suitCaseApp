package com.example.suitcaseapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.suitcaseapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class Home : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var firestore: FirebaseFirestore
    private lateinit var addButton: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        registerEvents()
        setupAddButton()
    }

    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    private fun registerEvents() {
        view?.findViewById<Button>(R.id.logOutBtn)?.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        auth.signOut()
        navControl.navigate(R.id.action_homeFragment_to_signInFragment)
    }
    private fun setupAddButton() {
        //on click button navigate to notesDetails
        val addButton = view?.findViewById<FloatingActionButton>(R.id.addButton) ?: return
        addButton.setOnClickListener {
            // Check if navControl is initialized and if the destination exists in the navigation graph
            navControl.navigate(R.id.action_homeFragment_to_notesDetails)
        }
//        val todoEditText = view?.findViewById<EditText>(R.id.todoEditText)
//
//        addButton.setOnClickListener {
//            val todoText = todoEditText?.text.toString().trim()
//
//            if (todoText.isNotEmpty()) {
//                addTodoToFirestore(todoText)
//                // Clear the EditText after adding the todo
//                todoEditText?.setText("")
//            } else {
//                Toast.makeText(context, "Empty fields are not allowed", Toast.LENGTH_SHORT).show()
//            }
//        }
    }


}