package com.example.suitcaseapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * This is the main activity of the application.
 * It is the entry point of the application.
 */
class MainActivity : AppCompatActivity() {

    /**
     * This method is called when the activity is starting.
     * It is where most initialization happens.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down
     * then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the user interface layout for this activity.
        // The layout file is defined in the project res/layout/activity_main.xml file.
        setContentView(R.layout.activity_main)
    }
}