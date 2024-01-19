package com.example.suitcaseapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.suitcaseapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

/**
 * This fragment displays items on a Google Map.
 * It fetches the items from a Firestore collection and adds them as markers on the map.
 */
class ItemsOnMap : Fragment(), OnMapReadyCallback {

    // Firestore instance
    private lateinit var firestore: FirebaseFirestore

    // Google Map instance
    private lateinit var googleMap: GoogleMap

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    /**
     * Inflates the layout for this fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_items_on_map, container, false)
    }

    /**
     * Called immediately after onCreateView has returned
     * Initializes Firebase and Google Map, and sets up the map
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase and Google Map
        init()

        // Get the SupportMapFragment and request notification when the map is ready to be used
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Initializes Firebase Authentication and Firestore
     */
    private fun init() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    /**
     * Manipulates the map once available
     * Fetches and displays markers when the map is ready
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Get the current user
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val email = user.email
            if (email != null) {
                // Fetch and display markers
                fetchAndDisplayMarkers(email)
            }
        }
    }

    /**
     * Fetches markers from Firestore and displays them on the map
     */
    private fun fetchAndDisplayMarkers(email: String) {
        firestore.collection(USERS_COLLECTION).document(email)
            .collection(HOLIDAYS_COLLECTION)
            .get()
            .addOnSuccessListener { result ->
                // Handle successful fetch
                handleFirestoreSuccess(result)
            }
            .addOnFailureListener { exception ->
                // Handle failed fetch
                handleFirestoreFailure(exception)
            }
    }

    /**
     * Handles successful fetch from Firestore
     * Adds markers to the map for each document in the result
     */
    private fun handleFirestoreSuccess(result: QuerySnapshot) {
        for (document in result) {
            val locationGeoPoint = document.getGeoPoint("location")
            val title = document.getString("title") ?: "No Title"
            if (locationGeoPoint != null) {
                val location = LatLng(locationGeoPoint.latitude, locationGeoPoint.longitude)
                googleMap.addMarker(MarkerOptions().position(location).title("$title Location"))
            }
        }
        if (result.documents.isNotEmpty()) {
            val firstLocationGeoPoint = result.documents.first().getGeoPoint("location")
            if (firstLocationGeoPoint != null) {
                val firstLocation =
                    LatLng(firstLocationGeoPoint.latitude, firstLocationGeoPoint.longitude)
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        firstLocation,
                        1f // Set zoom level to 1
                    )
                )
            }
        }
    }

    /**
     * Handles failed fetch from Firestore
     * Logs the exception
     */
    private fun handleFirestoreFailure(exception: Exception) {
        Log.e(TAG, "Error getting documents", exception)
    }

    companion object {
        // Tag for logging
        private const val TAG = "ItemsOnMap"

        // Firestore collection names
        private const val USERS_COLLECTION = "users"
        private const val HOLIDAYS_COLLECTION = "holidays"
    }
}