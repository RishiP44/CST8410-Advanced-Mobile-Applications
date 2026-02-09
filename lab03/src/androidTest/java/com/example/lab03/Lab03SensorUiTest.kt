package com.example.lab03

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class Lab03SensorUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lightField_isVisible() {
        Thread.sleep(1000)
        composeRule.onNodeWithTag("light_text").assertIsDisplayed()
    }

    @Test
    fun accelerometerField_isVisible() {
        Thread.sleep(1000)
        composeRule.onNodeWithTag("accel_text").assertIsDisplayed()
    }

    @Test
    fun stepsField_isVisible() {
        Thread.sleep(1000)
        composeRule.onNodeWithTag("steps_text").assertIsDisplayed()
    }
}
