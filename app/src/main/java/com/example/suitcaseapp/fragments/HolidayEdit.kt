package com.example.suitcaseapp.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
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
import java.util.UUID


class HolidayEdit : Fragment() {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val HOLIDAYS_COLLECTION = "holidays"
        private const val IMAGES_FOLDER = "images"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var holidayTitleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var checkBox: CheckBox
    private lateinit var holidayImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var dateCreatedTextView: EditText
    private var imageUri: Uri? = null
    private var currentImageUrl: String? = null
    private lateinit var mapFragment: SupportMapFragment


    private lateinit var imageResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_holiday_edit, container, false)
    }

    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        holidayTitleEditText = view.findViewById(R.id.holidayTitleEditText)
        descriptionEditText = view.findViewById(R.id.holidayDescriptionEditText)
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
            // log to console
            Log.d(TAG, "sendSMSButton clicked")
            val holidayTitle = holidayTitleEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val dateCreated = dateCreatedTextView.text.toString()

            // Format the SMS content
            val formattedMessage =
                "Holiday Details:\n\nTitle: $holidayTitle\n\nDescription: $description\n\nDate: $dateCreated"

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

    private fun loadHoliday(email: String, id: String) {
        firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION)
            .document(id).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val holiday = document.toObject(HolidayDetails.Holiday::class.java)
                    holidayTitleEditText.setText(holiday?.title)
                    descriptionEditText.setText(holiday?.lines?.joinToString("\n"))
                    checkBox.isChecked = holiday?.isPurchased ?: false
                    dateCreatedTextView.setText(holiday?.dateCreated)

                    val imageUrl = holiday?.imageUrl
                    if (!imageUrl.isNullOrEmpty()) {
                        currentImageUrl = imageUrl // Store the current imageUrl
                        Picasso.get().load(imageUrl).into(holidayImageView)
                    }

                    // Get the latitude and longitude from the Firestore document
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")

                    // If the latitude and longitude are not null, display the location on the map
                    if (latitude != null && longitude != null) {
                        mapFragment.getMapAsync { googleMap ->
                            val location = LatLng(latitude, longitude)
                            googleMap.addMarker(
                                MarkerOptions().position(location)
                                    .title(holiday?.title + " Location")
                            )
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
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

    private fun setupSaveButton(email: String, id: String) {
        saveButton.setOnClickListener {
            val title = holidayTitleEditText.text.toString()
            val lines = descriptionEditText.text.toString().split("\n")
            val isPurchased = checkBox.isChecked

            // Use the current imageUrl if no new image has been selected
            val imageUrl = if (imageUri != null) null else currentImageUrl

            val updatedHoliday = HolidayDetails.Holiday(
                title,
                lines,
                imageUrl = imageUrl,
                dateCreated = dateCreatedTextView.text.toString(),
                isPurchased = isPurchased
            )

            if (imageUri != null) {
                uploadImageAndSaveHoliday(email, id, updatedHoliday)
            } else {
                updateHoliday(email, id, updatedHoliday)
            }
        }
    }

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
                // TODO: Show error message to the user
            }
        }
    }

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
                                        navControl.navigateUp() // Go back to the previous screen
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error deleting document", e)
                                        // TODO: Show error message to the user
                                    }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "get failed with ", exception)
                            // TODO: Show error message to the user
                        }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun updateHoliday(email: String, id: String, holiday: HolidayDetails.Holiday) {
        firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION)
            .document(id).set(holiday)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully updated!")
                navControl.navigateUp() // Go back to the previous screen
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating document", e)
                // TODO: Show error message to the user
            }
    }
}