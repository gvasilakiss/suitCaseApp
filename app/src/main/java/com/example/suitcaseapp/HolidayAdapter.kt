package com.example.suitcaseapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.suitcaseapp.fragments.HolidayDetails
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

// Define the adapter for the RecyclerView. This adapter is responsible for creating
// the view holders and binding data to them.
class HolidayAdapter(options: FirestoreRecyclerOptions<HolidayDetails.Holiday>)
    : FirestoreRecyclerAdapter<HolidayDetails.Holiday, HolidayAdapter.HolidayViewHolder>(options) {

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

    // Inflate the item layout and create a ViewHolder. This method is called when the RecyclerView
    // needs a new ViewHolder to represent an item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolidayViewHolder {
        // Inflate the item layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_holiday_item, parent, false)
        // Create and return a ViewHolder
        return HolidayViewHolder(view)
    }

    // Bind data to the item. This method is called to update the contents of the ViewHolder
    // to reflect an item in the position 'position' in the data set.
    override fun onBindViewHolder(holder: HolidayViewHolder, position: Int, model: HolidayDetails.Holiday) {
        // Set the title of the holiday
        holder.titleTextView.text = model.title
        // Set the first line of the description of the holiday
        holder.descriptionTextView.text = model.lines[0]
        // Set the date the holiday was created
        holder.dateCreatedTextView.text = model.dateCreated

        holder.itemView.setOnClickListener {
            // Get the ID of the clicked item
            val itemId = snapshots.getSnapshot(position).id

            // Create a bundle to hold the arguments
            val bundle = Bundle().apply {
                putString("itemId", itemId)
            }

            // Navigate to the details screen and pass the arguments
            holder.itemView.findNavController().navigate(R.id.action_homeFragment_to_holidayEdit, bundle)
        }
    }
}