package com.livnica.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon definitions for the Livnica app.
 * Uses Material Icons Extended library.
 */
object AppIcons {
    // Bottom bar icons
    val today: ImageVector = Icons.Filled.Today
    val sickLeave: ImageVector = Icons.Filled.LocalHospital
    val pay: ImageVector = Icons.Filled.AttachMoney
    val location: ImageVector = Icons.Filled.LocationOn
    val settings: ImageVector = Icons.Filled.Settings
    val delete: ImageVector = Icons.Filled.DeleteForever

    // Navigation Icons (replacing "<" and ">")
    val previousMonth: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val nextMonth: ImageVector = Icons.AutoMirrored.Filled.ArrowForward

    // Shift Icons
    val shift1: ImageVector = Icons.Outlined.WbSunny       // Prva smena (morning)
    val shift2: ImageVector = Icons.Outlined.WbTwilight    // Druga smena (afternoon)
    val shift3: ImageVector = Icons.Outlined.NightsStay    // Treća smena (night)
    val offDay: ImageVector = Icons.Filled.Weekend         // OFF
    val vacation: ImageVector = Icons.Filled.BeachAccess   // Godisnji odmor
    val sick: ImageVector = Icons.Filled.Healing           // Bolovanje

    // Action Icons
    val save: ImageVector = Icons.Filled.Check
    val cancel: ImageVector = Icons.Filled.Close
    val add: ImageVector = Icons.Filled.Add
    val remove: ImageVector = Icons.Filled.Remove

    // Import/Export
    val importData: ImageVector = Icons.Filled.FileUpload
    val exportData: ImageVector = Icons.Filled.FileDownload
}

