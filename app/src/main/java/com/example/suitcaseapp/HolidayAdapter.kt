package com.example.suitcaseapp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.suitcaseapp.fragments.HolidayDetails
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.storage.FirebaseStorage

/**
 * Define the adapter for the RecyclerView. This adapter is responsible for creating
 * the view holders and binding data to them.
 */
class HolidayAdapter(options: FirestoreRecyclerOptions<HolidayDetails.Holiday>) :
    FirestoreRecyclerAdapter<HolidayDetails.Holiday, HolidayAdapter.HolidayViewHolder>(options) {

    // ViewHolder class. Each instance represents a single item in the list.
    // It contains the views that will be filled with the data from a single object from the data set.
    class HolidayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Reference to the title TextView in the item layout
        val titleTextView: TextView = view.findViewById(R.id.holidayTitle)

        // Reference to the description TextView in the item layout
        val descriptionTextView: TextView = view.findViewById(R.id.holidayDescription)

        // Reference to the date created TextView in the item layout
        val dateCreatedTextView: TextView = view.findViewById(R.id.holidayTimestamp)
    }

    /**
     * Deletes an item at the specified position in the RecyclerView.
     *
     * @param position The adapter position of the item to be deleted.
     * @param context The context in which the function is called. Used to create dialogs.
     * @param viewHolder The ViewHolder of the swiped item. Used to revert the swipe action if the user cancels the deletion.
     */
    fun deleteItem(position: Int, context: Context, viewHolder: RecyclerView.ViewHolder) {
        // Get the holiday at the specified position
        val holiday = snapshots.getSnapshot(position).toObject(HolidayDetails.Holiday::class.java)

        // Create a confirmation dialog before deletion
        AlertDialog.Builder(context)
            .setTitle("Delete Holiday")
            .setMessage("Are you sure you want to delete this holiday?")
            .setPositiveButton("Yes") { _, _ ->
                // Delete the image from Firestore Storage if it exists
                val imageUrl = holiday?.imageUrl
                if (!imageUrl.isNullOrEmpty()) {
                    // Get a reference to the image in Firebase Storage
                    val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)

                    // Delete the image
                    imageRef.delete().addOnSuccessListener {
                        // Image deleted successfully, now delete the document from Firestore
                        deleteDocument(position)

                        // Show a confirmation dialog after deletion
                        AlertDialog.Builder(context)
                            .setTitle("Deleted")
                            .setMessage("Holiday deleted successfully.")
                            .setPositiveButton("OK", null)
                            .show()
                    }.addOnFailureListener {
                        // Handle failure to delete image
                        AlertDialog.Builder(context)
                            .setTitle("Error")
                            .setMessage("Failed to delete holiday.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                } else {
                    // No image associated with the holiday, delete the document from Firestore
                    deleteDocument(position)

                    // Show a confirmation dialog after deletion
                    AlertDialog.Builder(context)
                        .setTitle("Deleted")
                        .setMessage("Holiday deleted successfully.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("No") { _, _ ->
                // User cancelled the deletion, revert the swipe action
                notifyItemChanged(viewHolder.adapterPosition)
            }
            .show()
    }

    /**
     * Deletes the document at the specified position in the RecyclerView.
     *
     * @param position The adapter position of the document to be deleted.
     */
    private fun deleteDocument(position: Int) {
        // Get the reference of the Firestore document at the specified position
        val documentReference = snapshots.getSnapshot(position).reference

        // Delete the document from Firestore
        documentReference.delete().addOnSuccessListener {
            // Notify the adapter that the item at the specified position has been removed
            notifyItemRemoved(position)
        }.addOnFailureListener {
            // Handle failure to delete document
        }
    }


    // Inflate the item layout and create a ViewHolder. This method is called when the RecyclerView
    // needs a new ViewHolder to represent an item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolidayViewHolder {
        // Inflate the item layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_holiday_item, parent, false)
        // Create and return a ViewHolder
        return HolidayViewHolder(view)
    }

    // Bind data to the item. This method is called to update the contents of the ViewHolder
    // to reflect an item in the position 'position' in the data set.
    override fun onBindViewHolder(
        holder: HolidayViewHolder,
        position: Int,
        model: HolidayDetails.Holiday
    ) {
        // Set the title of the holiday
        holder.titleTextView.text = model.title
        // Set the first line of the description of the holiday
        holder.descriptionTextView.text = model.lines[0]
        // Set the date the holiday was created
        holder.dateCreatedTextView.text = model.dateCreated

        // Set a click listener to navigate to the details screen when an item is clicked
        holder.itemView.setOnClickListener {
            // Get the ID of the clicked item
            val itemId = snapshots.getSnapshot(position).id

            // Create a bundle to hold the arguments
            val bundle = Bundle().apply {
                putString("itemId", itemId)
            }

            // Navigate to the details screen and pass the arguments
            holder.itemView.findNavController()
                .navigate(R.id.action_homeFragment_to_holidayEdit, bundle)
        }
    }
}
