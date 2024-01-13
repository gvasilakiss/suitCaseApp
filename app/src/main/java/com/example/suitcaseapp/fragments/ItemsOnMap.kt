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

class ItemsOnMap : Fragment(), OnMapReadyCallback {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleMap: GoogleMap
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_items_on_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun init() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val email = user.email
            if (email != null) {
                fetchAndDisplayMarkers(email)
            }
        }
    }

    private fun fetchAndDisplayMarkers(email: String) {
        firestore.collection(USERS_COLLECTION).document(email)
            .collection(HOLIDAYS_COLLECTION)
            .get()
            .addOnSuccessListener { result ->
                handleFirestoreSuccess(result)
            }
            .addOnFailureListener { exception ->
                handleFirestoreFailure(exception)
            }
    }

    private fun handleFirestoreSuccess(result: QuerySnapshot) {
        for (document in result) {
            val latitude = document.getDouble("latitude")
            val longitude = document.getDouble("longitude")
            val title = document.getString("title") ?: "No Title"
            if (latitude != null && longitude != null) {
                val location = LatLng(latitude, longitude)
                googleMap.addMarker(MarkerOptions().position(location).title("$title Location"))
            }
        }
        if (result.documents.isNotEmpty()) {
            val firstLocation = LatLng(
                result.documents.first().getDouble("latitude") ?: 0.0,
                result.documents.first().getDouble("longitude") ?: 0.0
            )
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    firstLocation,
                    1f // Set zoom level to 1
                )
            )
        }
    }

    private fun handleFirestoreFailure(exception: Exception) {
        Log.e(TAG, "Error getting documents", exception)
    }

    companion object {
        private const val TAG = "ItemsOnMap"
        private const val USERS_COLLECTION = "users"
        private const val HOLIDAYS_COLLECTION = "holidays"
    }
}
