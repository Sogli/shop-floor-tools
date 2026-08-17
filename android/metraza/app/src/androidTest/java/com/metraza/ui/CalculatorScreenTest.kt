package com.metraza.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.metraza.ui.screens.CalculatorScreen
import com.metraza.ui.theme.MetrazaTheme
import org.junit.Rule
import org.junit.Test

class CalculatorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchScreen() {
        composeTestRule.setContent {
            MetrazaTheme {
                CalculatorScreen()
            }
        }
    }

    @Test
    fun initialState_coreCalculatorContentIsDisplayed() {
        launchScreen()

        composeTestRule.onNodeWithText("Metraža").assertExists()
        composeTestRule.onNodeWithText("Kalkulator dužine i težine trake").assertExists()
        composeTestRule.onNodeWithText("PARAMETRI").assertExists()
        composeTestRule.onNodeWithText("IZRAČUNAJ").assertExists()
    }

    @Test
    fun initialState_materialChipsAndInputsAreDisplayed() {
        launchScreen()

        listOf("Cu", "CuZn10", "CuZn15", "CuZn20", "CuZn30", "CuZn37").forEach {
            composeTestRule.onNodeWithText(it).assertExists()
        }

        listOf("npr. 1000", "npr. 0.5", "npr. 100", "npr. 5", "npr. 150").forEach {
            composeTestRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun calculationFlow_validInputs_showsCurrentResults() {
        launchScreen()

        composeTestRule.onNodeWithText("Cu").performClick()
        composeTestRule.onNodeWithTag("total-weight-input").performTextInput("100")
        composeTestRule.onNodeWithTag("thickness-input").performTextInput("0.5")
        composeTestRule.onNodeWithTag("cut-width-input").performTextInput("50")
        composeTestRule.onNodeWithTag("rolls-input").performTextInput("3")

        composeTestRule.onNodeWithText("IZRAČUNAJ").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("REZULTATI").assertExists()
        composeTestRule.onNodeWithText("Za 3 reza", substring = true).assertExists()
        composeTestRule.onAllNodesWithText("+10%").assertCountEquals(3)
        composeTestRule.onAllNodesWithText("-10%").assertCountEquals(3)
    }

    @Test
    fun calculationFlow_missingMaterial_showsValidationMessage() {
        launchScreen()

        composeTestRule.onNodeWithText("IZRAČUNAJ").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("odaberite materijal", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun theme_headerAndSectionHierarchyRemainVisibleAfterThemePolish() {
        launchScreen()

        composeTestRule.onNodeWithText("Metraža").assertExists()
        composeTestRule.onNodeWithText("PARAMETRI").assertExists()
        composeTestRule.onNodeWithText("Materijal").assertExists()
    }

    @Test
    fun inputFields_showMeasurementUnits() {
        launchScreen()

        composeTestRule.onNodeWithText("kg").assertExists()
        composeTestRule.onAllNodesWithText("mm").assertCountEquals(3)
        composeTestRule.onNodeWithText("rez.").assertExists()
    }

    @Test
    fun materialSelection_exposesSelectedState() {
        launchScreen()

        composeTestRule.onNodeWithText("CuZn30").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("CuZn30").assertIsSelected()
        composeTestRule.onNodeWithText("Cu").assertIsNotSelected()
    }

    @Test
    fun primaryAction_andCompactHeaderRemainAvailable() {
        launchScreen()

        composeTestRule.onNodeWithText("Metraža").assertExists()
        composeTestRule.onNodeWithText("IZRAČUNAJ").assertExists()
    }

    @Test
    fun calculationFlow_withInnerDiameter_showsMetricResultHeadersAndSuccessIcon() {
        launchScreen()

        composeTestRule.onNodeWithText("Cu").performClick()
        composeTestRule.onNodeWithTag("total-weight-input").performTextInput("100")
        composeTestRule.onNodeWithTag("thickness-input").performTextInput("0.5")
        composeTestRule.onNodeWithTag("cut-width-input").performTextInput("50")
        composeTestRule.onNodeWithTag("rolls-input").performTextInput("3")
        composeTestRule.onNodeWithTag("inner-diameter-input").performTextInput("150")

        composeTestRule.onNodeWithText("IZRAČUNAJ").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Uspešan proračun").assertExists()
        composeTestRule.onAllNodesWithText("METARA", substring = true).assertCountEquals(3)
        composeTestRule.onAllNodesWithText("TEŽINA", substring = true).assertCountEquals(3)
        composeTestRule.onAllNodesWithText("PREČNIK").assertCountEquals(3)
        composeTestRule.onAllNodesWithText("Ø", substring = true).assertCountEquals(9)
        composeTestRule.onAllNodesWithText("/R", substring = true).assertCountEquals(9)
        composeTestRule.onAllNodesWithText("polupr.", substring = true, ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun calculationFlow_missingMaterial_showsErrorIcon() {
        launchScreen()

        composeTestRule.onNodeWithText("IZRAČUNAJ").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Greška u proračunu").assertExists()
    }
}
