package com.example.suitcaseapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.suitcaseapp.fragments.HolidayDetails

class HolidayAdapter(private var holidayList: MutableList<HolidayDetails.Holiday>) : RecyclerView.Adapter<HolidayAdapter.HolidayViewHolder>() {

    class HolidayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.holidayTitle)
        val description: TextView = itemView.findViewById(R.id.holidaDescription)
        val timestamp: TextView = itemView.findViewById(R.id.holidayTimestamp)

        fun bind(holiday: HolidayDetails.Holiday) {
            title.text = holiday.title
            description.text = holiday.lines.joinToString(separator = "\n")
            timestamp.text = holiday.dateCreated
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolidayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_holiday_item, parent, false)
        return HolidayViewHolder(view)
    }

    override fun onBindViewHolder(holder: HolidayViewHolder, position: Int) {
        holder.bind(holidayList[position])
    }

    override fun getItemCount() = holidayList.size

    fun updateData(newHolidayList: MutableList<HolidayDetails.Holiday>) {
        holidayList = newHolidayList
        notifyDataSetChanged()
    }
}