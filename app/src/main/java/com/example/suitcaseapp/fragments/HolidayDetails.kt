package com.example.suitcaseapp.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.navigation.NavController
import androidx.navigation.Navigation
import android.widget.CheckBox
import com.example.suitcaseapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HolidayDetails : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private var imageView: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_holiday_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageView)
        init(view)
        setupAddButton()
    }

    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    private fun setupAddButton() {
        val notesTitle = view?.findViewById<EditText>(R.id.notesTitle)
        val notesDescription = view?.findViewById<EditText>(R.id.notesDescription)
        val saveButton = view?.findViewById<AppCompatImageButton>(R.id.SaveNotes)
        val selectImageButton = view?.findViewById<Button>(R.id.selectImageButton)
        val checkBox = view?.findViewById<CheckBox>(R.id.chkItemsPurchased)

        val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                imageUri = result.data?.data
                imageView?.setImageURI(imageUri)
            }
        }

        selectImageButton?.setOnClickListener {
            // Start an activity to select an image, and save the result in imageUri
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            getContent.launch(intent)
        }

        saveButton?.setOnClickListener {
            val title = notesTitle?.text.toString()
            val description = notesDescription?.text.toString()
            val isPurchased = checkBox?.isChecked ?: false // Get the state of the checkbox

            if (title.isNotEmpty() && description.isNotEmpty()) {
                addToFirestore(title, description, imageUri,getTime(), isPurchased)
                // clear the fields
                notesTitle?.setText("")
                notesDescription?.setText("")
                imageView?.setImageDrawable(null)
                navControl.navigate(R.id.action_notesDetails_to_homeFragment)
            } else {
                // Show an error message for empty fields
                Toast.makeText(context, "Fields cant be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Data class representing a Holiday
    data class Holiday(
        val title: String, // The title of the holiday
        val lines: List<String>, // The lines of description for the holiday
        val imageUrl: String?, // The URL of the image for the holiday (nullable)
        val dateCreated: String, // The date the holiday was created
        val isPurchased: Boolean // Whether the holiday has been purchased
    )

    // Function to add a holiday to Firestore
    private fun addToFirestore(notesTitle: String, description: String, imageUri: Uri?, dateCreated: String, isPurchased: Boolean) {
        // Get the current user
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val email = user.email
            if (email != null) {
                // Split the description by newlines to create a list of lines
                val lines = description.split("\n")

                // If there's an image, upload it to Firebase Storage
                if (imageUri != null) {
                    val storageRef = storage.reference
                    val imageRef = storageRef.child(IMAGES_FOLDER).child(imageUri.lastPathSegment!!)
                    val uploadTask = imageRef.putFile(imageUri)
                    uploadTask.continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let {
                                throw it
                            }
                        }
                        // Get the download URL for the uploaded image
                        imageRef.downloadUrl
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // If the upload was successful, create a Holiday object and save it to Firestore
                            val downloadUri = task.result
                            val holiday = Holiday(notesTitle, lines, downloadUri.toString(), dateCreated, isPurchased)

                            saveHolidayToFirestore(email, holiday)
                        } else {
                            // Handle failure to upload the image
                            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // If there's no image, create a Holiday object with a null imageUrl and save it to Firestore
                    val holiday = Holiday(notesTitle, lines, null, dateCreated, isPurchased)

                    saveHolidayToFirestore(email, holiday)
                }
            } else {
                // Handle the case where the user's email is null
                Toast.makeText(context, "User email is null", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
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