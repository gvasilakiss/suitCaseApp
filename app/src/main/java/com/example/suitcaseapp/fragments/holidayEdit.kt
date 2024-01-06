package com.example.suitcaseapp.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.suitcaseapp.R
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import java.util.UUID


class holidayEdit : Fragment() {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val HOLIDAYS_COLLECTION = "holidays"
        private const val IMAGES_FOLDER = "images"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var functions : FirebaseFunctions

    private lateinit var holidayTitleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var checkBox: CheckBox
    private lateinit var holidayImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var dateCreatedTextView: EditText
    private var imageUri: Uri? = null

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
        functions = Firebase.functions

        functions.getHttpsCallable("europe-west2-sendSMS")

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

        val sendSmsButton: AppCompatImageButton = view.findViewById(R.id.sendSMSButton)
        sendSmsButton.setOnClickListener {
            // log to console
            Log.d(TAG, "sendSMSButton clicked")
            val holidayDetails = "Title: ${holidayTitleEditText.text}, Description: ${descriptionEditText.text}, Date: ${dateCreatedTextView.text}"
            Log.d(TAG, "holidayDetails: $holidayDetails")
            sendSms(holidayDetails, "+4407736781146") // Replace with the recipient's phone number
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
        val functions = Firebase.functions
        val data = hashMapOf(
            "message" to message,
            "to" to to
        )
        functions
            .getHttpsCallable("europe-west2-sendSMS") // Make sure this matches the name of your Firebase function
            .call(data)
            .continueWith { task ->
                if (task.isSuccessful) {
                    // Handle the result if there is one
                    val result = task.result?.data
                    Log.d("FirebaseFunctions", "Function result: $result")
                } else {
                    // Log all exceptions
                    val e = task.exception
                    Log.e("FirebaseFunctions", "Error calling function", e)
                }
            }
    }

    private fun loadHoliday(email: String, id: String) {
        firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION).document(id).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val holiday = document.toObject(HolidayDetails.Holiday::class.java)
                    holidayTitleEditText.setText(holiday?.title)
                    descriptionEditText.setText(holiday?.lines?.joinToString("\n"))
                    checkBox.isChecked = holiday?.isPurchased ?: false
                    dateCreatedTextView.setText(holiday?.dateCreated)

                    val imageUrl = holiday?.imageUrl
                    if (!imageUrl.isNullOrEmpty()) {
                        Picasso.get().load(imageUrl).into(holidayImageView)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
                // TODO: Show error message to the user
            }
    }

    private fun setupSaveButton(email: String, id: String) {
        saveButton.setOnClickListener {
            val title = holidayTitleEditText.text.toString()
            val lines = descriptionEditText.text.toString().split("\n")
            val isPurchased = checkBox.isChecked

            val updatedHoliday = HolidayDetails.Holiday(title, lines, imageUrl = null, dateCreated = dateCreatedTextView.text.toString(), isPurchased = isPurchased)

            if (imageUri != null) {
                uploadImageAndSaveHoliday(email, id, updatedHoliday)
            } else {
                updateHoliday(email, id, updatedHoliday)
            }
        }
    }

    private fun uploadImageAndSaveHoliday(email: String, id: String, holiday: HolidayDetails.Holiday) {
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
        imageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                    firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION).document(id).delete()
                        .addOnSuccessListener {
                            Log.d(TAG, "DocumentSnapshot successfully deleted!")
                            navControl.navigateUp() // Go back to the previous screen
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error deleting document", e)
                            // TODO: Show error message to the user
                        }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun updateHoliday(email: String, id: String, holiday: HolidayDetails.Holiday) {
        firestore.collection(USERS_COLLECTION).document(email).collection(HOLIDAYS_COLLECTION).document(id).set(holiday)
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