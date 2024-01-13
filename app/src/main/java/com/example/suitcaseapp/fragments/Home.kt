package com.example.suitcaseapp.fragments

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
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

        // Setup the showMap button click listener
        setupShowMapButton(view)

        // Setup the menu button click listener
        setupMenuButton(view)
    }

    // Initialize Firebase and navigation controller
    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    // Setup the RecyclerView
    private fun setupRecyclerView() {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.holidayRecyclerView)

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val email = user.email
            if (email != null) {
                val query = firestore.collection(USERS_COLLECTION).document(email)
                    .collection(HOLIDAYS_COLLECTION)

                val options = FirestoreRecyclerOptions.Builder<HolidayDetails.Holiday>()
                    .setQuery(query, HolidayDetails.Holiday::class.java)
                    .build()

                val adapter = HolidayAdapter(options)

                recyclerView?.adapter = adapter

                // Setup swipe-to-delete functionality
                setupSwipeToDelete(recyclerView, adapter)

                // Start listening for Firestore updates
                adapter.startListening()
            }
        }
    }

    // Setup swipe-to-delete functionality
    private fun setupSwipeToDelete(recyclerView: RecyclerView?, adapter: HolidayAdapter) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                TODO("Not yet implemented")
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Swipe-to-delete action
                val position = viewHolder.adapterPosition
                adapter.deleteItem(position, requireContext())
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // Call default implementation first
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )

                // Check if swiping to the left (negative dX)
                val isSwipeLeft = dX < 0

                // Get the background color for the swipe
                val backgroundColor = Color.parseColor("#FF0000")

                // Set the bounds for the background
                val backgroundBounds = Rect(
                    viewHolder.itemView.right + dX.toInt(),
                    viewHolder.itemView.top,
                    viewHolder.itemView.right,
                    viewHolder.itemView.bottom
                )

                // Set the paint for the background
                val backgroundPaint = Paint().apply {
                    color = backgroundColor
                }

                // Draw the background
                c.drawRect(backgroundBounds, backgroundPaint)

                // Get the delete icon drawable
                val deleteIconDrawable =
                    ContextCompat.getDrawable(requireContext(), R.drawable.baseline_delete_24)

                // Calculate the position for the delete icon
                val iconMargin =
                    (viewHolder.itemView.height - deleteIconDrawable!!.intrinsicHeight) / 2
                val iconLeft =
                    viewHolder.itemView.right - iconMargin - deleteIconDrawable.intrinsicWidth + dX.toInt()
                val iconRight = viewHolder.itemView.right - iconMargin + dX.toInt()
                val iconTop =
                    viewHolder.itemView.top + (viewHolder.itemView.height - deleteIconDrawable.intrinsicHeight) / 2
                val iconBottom = iconTop + deleteIconDrawable.intrinsicHeight

                // Set the bounds for the delete icon
                deleteIconDrawable.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                // Draw the delete icon
                deleteIconDrawable.draw(c)

                // Hide the delete icon when swiping is completed
                if (!isCurrentlyActive) {
                    deleteIconDrawable.alpha = 0
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // Setup the add button click listener
    private fun addNewHoliday() {
        val addButton = view?.findViewById<FloatingActionButton>(R.id.addButton) ?: return
        addButton.setOnClickListener {
            navControl.navigate(R.id.action_homeFragment_to_notesDetails)
        }
    }

    // Setup the showMap button click listener
    private fun setupShowMapButton(view: View) {
        val showMapButton = view.findViewById<ImageButton>(R.id.showMap)
        showMapButton.setOnClickListener {
            navControl.navigate(R.id.action_homeFragment_to_itemsOnMap)
        }
    }

    // Setup the menu button click listener
    private fun setupMenuButton(view: View) {
        val menuButton = view.findViewById<ImageButton>(R.id.menuHome)
        menuButton.setOnClickListener {
            logoutUser()
        }
    }

    // Logout the user and navigate to the sign-in screen
    private fun logoutUser() {
        auth.signOut()
        navControl.navigate(R.id.action_homeFragment_to_signInFragment)
    }

    companion object {
        private const val HOLIDAYS_COLLECTION = "holidays"
        private const val USERS_COLLECTION = "users"
    }
}