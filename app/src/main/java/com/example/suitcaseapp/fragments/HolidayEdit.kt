package com.example.suitcaseapp.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.suitcaseapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import java.util.Locale
import java.util.UUID

/**
 * This fragment is used to edit the details of a holiday.
 * It allows the user to update the title, description, price, and image of the holiday.
 * It also provides the option to delete the holiday.
 */
@Suppress("DEPRECATION")
class HolidayEdit : Fragment() {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val HOLIDAYS_COLLECTION = "holidays"
        private const val IMAGES_FOLDER = "images"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var navControl: NavController
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private lateinit var holidayTitleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var checkBox: CheckBox
    private lateinit var holidayImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var dateCreatedTextView: EditText
    private lateinit var priceEditText: EditText
    private var imageUri: Uri? = null
    private var currentImageUrl: String? = null
    private lateinit var mapFragment: SupportMapFragment
    private var holidayAddress: String? = null

    private lateinit var imageResultLauncher: ActivityResultLauncher<Intent>

    /**
     * Inflates the layout for this fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_holiday_edit, container, false)
    }

    /**
     * Initializes the UI elements and sets up the click listeners
     */
    private fun init(view: View) {
        navControl = Navigation.findNavController(view)

        holidayTitleEditText = view.findViewById(R.id.holidayTitleEditText)
        descriptionEditText = view.findViewById(R.id.holidayDescriptionEditText)
        priceEditText = view.findViewById(R.id.priceEditText)
        checkBox = view.findViewById(R.id.chkItemsPurchased)
        holidayImageView = view.findViewById(R.id.imageView)
        selectImageButton = view.findViewById(R.id.selectImageButton)
        saveButton = view.findViewById(R.id.saveHolidayButton)
        deleteButton = view.findViewById(R.id.deleteHolidayButton)
        dateCreatedTextView = view.findViewById(R.id.holidayDateCreatedEditText)

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imageResultLauncher.launch(intent)
        }

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        val sendSmsButton: AppCompatImageButton = view.findViewById(R.id.sendSMSButton)
        sendSmsButton.setOnClickListener {
            val holidayTitle = holidayTitleEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val dateCreated = dateCreatedTextView.text.toString()
            val price = priceEditText.text.toString()

            // Format the SMS content
            val formattedMessage =
                "Holiday Details:\n\nTitle: $holidayTitle\n\nDescription: $description\n\nPrice: $price\n\nDate: $dateCreated\n\nLocation: $holidayAddress"

            // Create an EditText where the user can enter the phone number
            val phoneNumberEditText = EditText(requireContext())
            phoneNumberEditText.hint = "Enter phone number"

            // Show dialog to enter phone number
            AlertDialog.Builder(requireContext())
                .setTitle("Enter Phone Number")
                .setView(phoneNumberEditText)
                .setPositiveButton("OK") { _, _ ->
                    val phoneNumber = phoneNumberEditText.text.toString()
                    if (phoneNumber.isNotBlank()) {
                        // Check if the phone number starts with "+44"
                        if (phoneNumber.matches("^\\+44.*$".toRegex())) {
                            // Show confirmation dialog
                            AlertDialog.Builder(requireContext())
                                .setTitle("Send SMS")
                                .setMessage("Are you sure you want to send this SMS?")
                                .setPositiveButton("Yes") { _, _ ->
                                    sendSms(formattedMessage, phoneNumber)
                                }
                                .setNegativeButton("No", null)
                                .show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Phone number must start with +44",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Please enter a phone number",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /**
     * Called immediately after onCreateView() has returned, but before any saved state has been restored in to the view.
     * This is useful for when Fragment views are created from code and not inflated from a layout.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)

        val itemId = arguments?.getString("itemId")

        itemId?.let { id ->
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                val email = user.email
                if (email != null) {
                    loadHoliday(email, id)
                    setupSaveButton(email, id)
                    setupImageResultLauncher()
                    setupDeleteButton(email, id)
                }
            }
        }
    }

    /**
     * Sends an SMS with the holiday details using Firebase Functions Twilio extension
     */
    private fun sendSms(message: String, to: String) {
        val db = Firebase.firestore
        val data = hashMapOf(
            "body" to message,
            "to" to to
        )
        db.collection("messages")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(requireContext(), "SMS sent", Toast.LENGTH_SHORT).show()
                Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show()
                Log.w("Firestore", "Error adding document", e)
            }
    }

    /**
     * Loads the holiday details from Firestore
     */
    private fun loadHoliday(email: String, id: String) {
        firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION)
            .document(id).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val holiday = document.toObject(HolidayDetails.Holiday::class.java)
                    holidayTitleEditText.setText(holiday?.title)
                    descriptionEditText.setText(holiday?.lines?.joinToString("\n"))
                    checkBox.isChecked = holiday?.purchased ?: false
                    priceEditText.setText(holiday?.price.toString())
                    dateCreatedTextView.setText(holiday?.dateCreated)

                    val imageUrl = holiday?.imageUrl
                    if (!imageUrl.isNullOrEmpty()) {
                        currentImageUrl = imageUrl // Store the current imageUrl
                        Picasso.get().load(imageUrl).into(holidayImageView)
                    }

                    // Get the location as a GeoPoint from the Firestore document
                    val locationGeoPoint = document.getGeoPoint("location")

                    // If the locationGeoPoint is not null, convert it to an address
                    if (locationGeoPoint != null) {
                        val geocoder = context?.let { Geocoder(it, Locale.getDefault()) }
                        val addresses = geocoder?.getFromLocation(
                            locationGeoPoint.latitude,
                            locationGeoPoint.longitude,
                            1
                        )
                        holidayAddress = addresses?.get(0)?.getAddressLine(0)

                        mapFragment.getMapAsync { googleMap ->
                            val location =
                                LatLng(locationGeoPoint.latitude, locationGeoPoint.longitude)
                            googleMap.addMarker(
                                MarkerOptions().position(location)
                                    .title(holiday?.title + " Location")
                            )
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    location,
                                    5f
                                )
                            ) // Set zoom level to 5
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("Failed to load holiday")
                    .setPositiveButton("OK", null)
                    .show()
            }
    }

    /**
     * Sets up the save button to update the holiday details in Firestore
     */
    private fun setupSaveButton(email: String, id: String) {
        saveButton.setOnClickListener {
            val title = holidayTitleEditText.text.toString()
            val lines = descriptionEditText.text.toString().split("\n")
            val price = priceEditText.text.toString().toDouble()
            val purchased = checkBox.isChecked

            // Use the current imageUrl if no new image has been selected
            val imageUrl = if (imageUri != null) null else currentImageUrl

            firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION)
                .document(id).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val originalLocation = document.getGeoPoint("location")

                        val updatedHoliday = HolidayDetails.Holiday(
                            title,
                            lines,
                            imageUrl = imageUrl,
                            dateCreated = dateCreatedTextView.text.toString(),
                            purchased = purchased,
                            location = originalLocation!!,
                            price = price
                        )

                        if (imageUri != null) {
                            uploadImageAndSaveHoliday(email, id, updatedHoliday)
                        } else {
                            updateHoliday(email, id, updatedHoliday)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                    Toast.makeText(requireContext(), "Failed to update holiday", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    /**
     * Uploads the selected image to Firebase Storage and updates the holiday details in Firestore
     */
    private fun uploadImageAndSaveHoliday(
        email: String,
        id: String,
        holiday: HolidayDetails.Holiday
    ) {
        val imageRef = storage.reference.child("$IMAGES_FOLDER/${UUID.randomUUID()}")
        val uploadTask = imageRef.putFile(imageUri!!)

        uploadTask.continueWithTask(com.google.android.gms.tasks.Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation imageRef.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                holiday.imageUrl = downloadUri.toString()

                updateHoliday(email, id, holiday)
            } else {
                // Handle failures
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("Failed to upload image")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    /**
     * Sets up the image result launcher to handle the result of the image selection intent
     */
    private fun setupImageResultLauncher() {
        imageResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    imageUri = data?.data
                    holidayImageView.setImageURI(imageUri)
                }
            }
    }

    /**
     * Sets up the delete button to delete the holiday from Firestore
     */
    private fun setupDeleteButton(email: String, id: String) {
        deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Holiday")
                .setMessage("Are you sure you want to delete this holiday?")
                .setPositiveButton("Yes") { _, _ ->
                    // Get the holiday document
                    val holidayRef = firestore.collection(USERS_COLLECTION).document(email)
                        .collection(HOLIDAYS_COLLECTION).document(id)

                    holidayRef.get()
                        .addOnSuccessListener { document ->
                            if (document != null) {
                                val holiday = document.toObject(HolidayDetails.Holiday::class.java)

                                // If the holiday has an image, delete it from Firebase Storage
                                val imageUrl = holiday?.imageUrl
                                if (!imageUrl.isNullOrEmpty()) {
                                    val imageRef =
                                        FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                                    imageRef.delete()
                                }

                                // Delete the holiday document
                                holidayRef.delete()
                                    .addOnSuccessListener {
                                        Log.d(TAG, "DocumentSnapshot successfully deleted!")
                                        Toast.makeText(
                                            requireContext(),
                                            "Holiday deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navControl.navigateUp() // Go back to the previous screen
                                    }
                                    .addOnFailureListener { e ->
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Error")
                                            .setMessage("Failed to delete holiday")
                                            .setPositiveButton("OK", null)
                                            .show()
                                        Log.w(TAG, "Error deleting document", e)
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Error")
                                            .setMessage(getString(R.string.failed_to_delete_holiday))
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "get failed with ", exception)
                            AlertDialog.Builder(requireContext())
                                .setTitle("Error")
                                .setMessage("Failed to delete holiday" + exception.message)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    /**
     * Updates the holiday details in Firestore
     */
    private fun updateHoliday(email: String, id: String, holiday: HolidayDetails.Holiday) {
        firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION)
            .document(id).set(holiday)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully updated!")
                Toast.makeText(requireContext(), "Holiday updated", Toast.LENGTH_SHORT).show()
                navControl.navigateUp() // Go back to the previous screen
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating document", e)
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(getString(R.string.failed_to_update_holiday) + e.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
    }
}