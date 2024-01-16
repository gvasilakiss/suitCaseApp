package com.example.suitcaseapp.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.suitcaseapp.R
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HolidayDetails : Fragment() {
    // Firebase authentication instance
    private lateinit var auth: FirebaseAuth

    // Navigation controller instance
    private lateinit var navControl: NavController

    // Firestore database instance
    private lateinit var firestore: FirebaseFirestore

    // Firebase storage instance
    private lateinit var storage: FirebaseStorage

    // Uri for the selected image
    private var imageUri: Uri? = null

    // ImageView for displaying the selected image
    private var imageView: ImageView? = null

    // Map fragment instance
    private lateinit var mapFragment: SupportMapFragment

    // Selected location
    private var selectedLocation: LatLng? = null

    // UI components
    private val titleEditText by lazy { view?.findViewById<EditText>(R.id.notesTitle) }
    private val descriptionEditText by lazy { view?.findViewById<EditText>(R.id.notesDescription) }
    private val saveButton by lazy { view?.findViewById<AppCompatImageButton>(R.id.SaveNotes) }
    private val selectImageButton by lazy { view?.findViewById<Button>(R.id.selectImageButton) }
    private val checkBox by lazy { view?.findViewById<CheckBox>(R.id.chkItemsPurchased) }

    // Inflate the layout for this fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_holiday_details, container, false)
        mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment
        return view
    }

    // Initialize the views and setup the add button
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageView)
        init(view)
        setupAddButton()
        setupMap()
    }

    // Initialize Firebase and Navigation instances
    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    // Setup the map
    private fun setupMap() {
        mapFragment.getMapAsync { googleMap ->
            googleMap.setOnMapClickListener { latLng ->
                // Clear all markers
                googleMap.clear()
                // Add a marker at the clicked location
                googleMap.addMarker(MarkerOptions().position(latLng).title("Item Location"))
                // Store the clicked location
                selectedLocation = latLng
            }
        }
    }

    // Setup the add button and its functionality
    private fun setupAddButton() {
        // Register for activity result to get the selected image
        val getContent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // Get the Uri of the selected image
                    imageUri = result.data?.data
                    // Set the selected image to the ImageView
                    imageView?.setImageURI(imageUri)
                }
            }

        // Set click listener for the select image button
        selectImageButton?.setOnClickListener {
            // Create an intent to pick an image
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            // Launch the intent
            getContent.launch(intent)
        }

        // Inside your setupAddButton function
        saveButton?.setOnClickListener {
            val location = selectedLocation
            if (location != null) {
                // Rest of your logic when location is available
                val title = titleEditText?.text.toString()
                val description = descriptionEditText?.text.toString()
                val isPurchased = checkBox?.isChecked ?: false

                if (title.isNotEmpty() && description.isNotEmpty()) {
                    // Add the holiday to Firestore
                    addToFirestore(
                        title,
                        description,
                        imageUri,
                        getTime(),
                        isPurchased,
                        location.latitude,
                        location.longitude
                    )
                    // Clear the fields
                    titleEditText?.setText("")
                    descriptionEditText?.setText("")
                    imageView?.setImageDrawable(null)
                    // Navigate back to the home fragment
                    navControl.navigate(R.id.action_notesDetails_to_homeFragment)
                } else {
                    Toast.makeText(context, "Fields can't be empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Handle the case where the location is null
                Toast.makeText(context, "Please select a location on the map", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Data class representing a Holiday
    data class Holiday(
        val title: String = "", // The title of the holiday
        val lines: List<String> = listOf(), // The lines of description for the holiday
        var imageUrl: String? = null, // The URL of the image for the holiday (nullable)
        val dateCreated: String = "", // The date the holiday was created
        val purchased: Boolean = false, // Whether the holiday has been purchased
        val latitude: Double = 1.0, // The latitude of the location where the holiday was created
        val longitude: Double = 1.0 // The longitude of the location where the holiday was created
    )

    // Function to add a holiday to Firestore
    private fun addToFirestore(
        holidayTitle: String,
        description: String,
        imageUri: Uri?,
        dateCreated: String,
        purchased: Boolean,
        latitude: Double,
        longitude: Double
    ) {
        // Get the current user
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val email = user.email
            if (email != null) {
                // Split the description by newlines to create a list of lines
                val lines = description.split("\n")

                // If there's an image, upload it to Firebase Storage
                if (imageUri != null) {
                    uploadImageToStorage(imageUri) { imageUrl ->
                        // Create a Holiday object and save it to Firestore
                        val holiday = Holiday(
                            holidayTitle,
                            lines,
                            imageUrl,
                            dateCreated,
                            purchased,
                            latitude,
                            longitude
                        )
                        saveHolidayToFirestore(email, holiday)
                    }
                } else {
                    // If there's no image, create a Holiday object with a null imageUrl and save it to Firestore
                    val holiday = Holiday(
                        holidayTitle,
                        lines,
                        null,
                        dateCreated,
                        purchased,
                        latitude,
                        longitude
                    )
                    saveHolidayToFirestore(email, holiday)
                }
            } else {
                // Handle the case where the user's email is null
                Toast.makeText(context, "User email is null", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to upload an image to Firebase Storage and get the download URL
    private fun uploadImageToStorage(imageUri: Uri, onSuccess: (imageUrl: String) -> Unit) {
        val storageRef = storage.reference
        val imageRef = storageRef.child(IMAGES_FOLDER).child(imageUri.lastPathSegment!!)
        val uploadTask = imageRef.putFile(imageUri)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    Toast.makeText(
                        context,
                        "Failed to upload image: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            // Get the download URL for the uploaded image
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // If the upload was successful, pass the download URL to the onSuccess function
                val downloadUri = task.result
                onSuccess(downloadUri.toString())
            } else {
                // Handle failure to upload the image
                Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Save the holiday item to Firestore
    private fun saveHolidayToFirestore(email: String, holiday: Holiday) {
        firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION)
            .add(holiday)
            .addOnSuccessListener {
                Toast.makeText(context, "Holiday saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save holiday: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    // Get the current date and time
    private fun getTime(): String {
        return SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).format(Date())
    }

    companion object {
        // Constant for the name of the 'holidays' collection in Firestore
        private const val HOLIDAYS_COLLECTION = "holidays"

        // Constant for the name of the 'users' collection in Firestore
        private const val USERS_COLLECTION = "users"

        // Constant for the name of the 'images' folder where images are stored
        private const val IMAGES_FOLDER = "images"
    }
}