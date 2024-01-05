package com.example.suitcaseapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.suitcaseapp.fragments.HolidayDetails
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

// Define the adapter for the RecyclerView
class HolidayAdapter(options: FirestoreRecyclerOptions<HolidayDetails.Holiday>)
    : FirestoreRecyclerAdapter<HolidayDetails.Holiday, HolidayAdapter.HolidayViewHolder>(options) {

    // ViewHolder class
    class HolidayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.holidayTitle)
        val descriptionTextView: TextView = view.findViewById(R.id.holidayDescription)
        val dateCreatedTextView: TextView = view.findViewById(R.id.holidayTimestamp)
    }

    // Inflate the item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolidayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_holiday_item, parent, false)
        return HolidayViewHolder(view)
    }

    // Bind data to the item
    override fun onBindViewHolder(holder: HolidayViewHolder, position: Int, model: HolidayDetails.Holiday) {
        holder.titleTextView.text = model.title
        holder.descriptionTextView.text = model.lines.joinToString("\n")
        holder.dateCreatedTextView.text = model.dateCreated
    }
}