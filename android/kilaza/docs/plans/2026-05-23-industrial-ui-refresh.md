# Industrial UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers-optimized:subagent-driven-development` (recommended) or `superpowers-optimized:executing-plans` to implement this plan task-by-task. Also apply `superpowers-optimized:frontend-design` standards for every UI task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pretvoriti postojeći Compose ekran u kompaktniji industrijski kalkulator sa manjim headerom, stalno vidljivim ukupnim zbirkom, preglednijim unosom, boljom listom stavki i pristupačnijim kontrolama.

**Architecture:** Zadržati postojeću jednoscreen Compose arhitekturu: `CalculatorScreen` ostaje orkestrator, `CalculatorScreenState` drži izvedeno stanje, a sekcije ostaju podeljene na header, unos, listu i ukupno. Poslovna matematika ostaje u `data/Calculator.kt`; UI sme samo da poziva postojeće parser/calculator funkcije preko izvedenog preview stanja. Vizuelni sistem se menja kroz postojeće theme/tokens fajlove i postojeće reusable komponente.

**Tech Stack:** Kotlin, Android Jetpack Compose, Material 3, Gradle Android plugin, JUnit/Truth unit testovi, opcioni Compose instrumentation testovi ako je emulator ili uređaj dostupan.

**Assumptions:**
- Assumes this app remains a single-purpose Android calculator — will NOT cover multi-screen navigation, account data, cloud sync, export, or persistence beyond current `rememberSaveable` behavior.
- Assumes Serbian Latin UI copy is desired — will NOT preserve ASCII-only visible labels such as `Racunanje kilaze`.
- Assumes business formulas are already correct — will NOT change `Calculator.kt` formulas, density values, parser rules, or data model semantics.
- Assumes the project-local rule "bez Git komandi" remains active — will NOT include `git add`, `git commit`, branch, merge, or push steps.

---

## File Structure

- `app/src/main/java/com/example/racunanjekilaze/ui/theme/AppConstants.kt` — color, spacing, radius and layout tokens; tighten visual system here first.
- `app/src/main/java/com/example/racunanjekilaze/ui/theme/Theme.kt` — typography hierarchy; remove decorative serif title and letter spacing drift.
- `app/src/main/java/com/example/racunanjekilaze/ui/components/RoundedButton.kt` — add `enabled` support and safer 44dp+ touch behavior for shared buttons.
- `app/src/main/java/com/example/racunanjekilaze/ui/components/LabeledTextField.kt` — add optional test tags and clearer error/helper slots if needed by UI tests.
- `app/src/main/java/com/example/racunanjekilaze/ui/components/MaterialGridSelector.kt` — keep grid selector but improve selectable semantics and touch feedback.
- `app/src/main/java/com/example/racunanjekilaze/ui/sections/HeaderSection.kt` — compact logo/title header.
- `app/src/main/java/com/example/racunanjekilaze/ui/sections/InputSection.kt` — grouped form, Serbian copy, and live calculation preview panel.
- `app/src/main/java/com/example/racunanjekilaze/ui/sections/OrderListSection.kt` — compact table-like rows and accessible delete action.
- `app/src/main/java/com/example/racunanjekilaze/ui/sections/TotalSection.kt` — keep as full summary section, but align styling with sticky total.
- `app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreen.kt` — screen layout, sticky bottom action/total bar, message placement, reset confirmation dialog.
- `app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreenState.kt` — derived live preview state only; no formula changes.
- `app/src/test/java/com/example/racunanjekilaze/ui/screens/CalculatorScreenStateTest.kt` — unit tests for derived preview state.
- `app/src/androidTest/java/com/example/racunanjekilaze/CalculatorScreenUiTest.kt` — optional UI tests for sticky actions and confirmation dialog when a device/emulator is available.
- `app/build.gradle.kts` — add Android UI test dependencies only if Task 6 chooses instrumentation coverage.

---

### Task 1: Tighten Design Tokens And Typography

**Status:** Completed

**Files:**
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/theme/AppConstants.kt`
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/theme/Theme.kt`

**Security flag:** `none`

**Does NOT cover:** Does not change screen layout or calculation behavior; this task only changes shared visual tokens and text hierarchy.

- [ ] **Step 1: Capture current proof before token edits**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS with existing calculator/model tests.

- [ ] **Step 2: Tighten layout tokens**

In `AppConstants.kt`, keep the existing palette but reduce visual bulk:

```kotlin
return LayoutTokens(
    isCompact = isCompact,
    screenPaddingHorizontal = if (isCompact) 12.dp else 16.dp,
    screenPaddingVertical = if (isCompact) 10.dp else 14.dp,
    sectionSpacing = if (isCompact) 8.dp else 10.dp,
    cardPaddingHorizontal = if (isCompact) 12.dp else 16.dp,
    cardPaddingVertical = if (isCompact) 10.dp else 14.dp,
    cardRadius = 10.dp,
    itemPaddingHorizontal = if (isCompact) 10.dp else 12.dp,
    itemPaddingVertical = if (isCompact) 8.dp else 10.dp,
    buttonMinHeight = if (isCompact) 48.dp else 52.dp,
    buttonContentPadding = if (isCompact) {
        PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    } else {
        PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    },
    buttonSpacing = if (isCompact) 8.dp else 10.dp
)
```

- [ ] **Step 3: Replace decorative typography with industrial sans hierarchy**

In `Theme.kt`, keep `FontFamily.SansSerif` for every text style, set all `letterSpacing` values to `0.sp`, and use these target sizes:

```kotlin
titleLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)
titleMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)
labelMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.sp
)
```

- [ ] **Step 4: Verify compile and tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS. If typography imports become unused, remove them before proceeding.

---

### Task 2: Compact Header And Serbian Copy

**Status:** Completed

**Files:**
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/sections/HeaderSection.kt`

**Security flag:** `none`

**Does NOT cover:** Does not change inputs, list rows, totals, or buttons; this task only changes the top header.

- [ ] **Step 1: Replace visible ASCII copy**

Use these exact visible strings in `HeaderSection.kt`:

```kotlin
AutoSizeText(
    text = "Računanje kilaže",
    style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
    color = TextPrimary,
    maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
    maxLines = 1,
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = layout.screenPaddingHorizontal)
)
Text(
    text = "Kalkulator metraže i težine trake",
    style = MaterialTheme.typography.bodyMedium,
    color = TextSecondary,
    textAlign = TextAlign.Center,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = layout.screenPaddingHorizontal)
)
```

- [ ] **Step 2: Limit logo height**

Keep `ContentScale.Fit`, but constrain the image so it no longer dominates the first viewport:

```kotlin
val logoMaxHeight = if (layout.isCompact) 68.dp else 84.dp

Image(
    painter = logoPainter,
    contentDescription = stringResource(R.string.app_name),
    contentScale = ContentScale.Fit,
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = logoMaxHeight)
        .aspectRatio(aspectRatio)
)
```

Add `import androidx.compose.foundation.layout.heightIn`.

- [ ] **Step 3: Tighten header spacing**

Set:

```kotlin
val headerSpacing = if (layout.isCompact) 4.dp else 6.dp
```

- [ ] **Step 4: Verify**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS.

Manual check on device/emulator: first screen shows the logo, title, and start of the form without needing a long scroll.

---

### Task 3: Add Derived Live Preview State

**Status:** Completed

**Files:**
- Create: `app/src/test/java/com/example/racunanjekilaze/ui/screens/CalculatorScreenStateTest.kt`
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreenState.kt`

**Security flag:** `none`

**Does NOT cover:** Does not add entries automatically, does not change target-weight logic, and does not alter parser error messages.

- [ ] **Step 1: Write failing unit tests**

Create `CalculatorScreenStateTest.kt`:

```kotlin
package com.example.racunanjekilaze.ui.screens

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculatorScreenStateTest {
    @Test
    fun currentPreviewUsesValidInputsWithoutAddingEntry() {
        val state = CalculatorScreenState()

        state.selectedMaterial = "Cu"
        state.radialThickness = "150"
        state.coreDiameter = "400"
        state.thickness = "2"
        state.width = "72"
        state.coils = "3"

        val preview = state.currentPreview

        assertThat(preview).isNotNull()
        assertThat(preview!!.coilCount).isEqualTo(3)
        assertThat(preview.totalWeightKg).isGreaterThan(0.0)
        assertThat(state.orderEntries).isEmpty()
    }

    @Test
    fun currentPreviewIsNullUntilAllRequiredInputsAreValid() {
        val state = CalculatorScreenState()

        state.selectedMaterial = "Cu"
        state.radialThickness = "150"
        state.coreDiameter = "400"
        state.thickness = "2"
        state.width = ""
        state.coils = "3"

        assertThat(state.currentPreview).isNull()
    }

    @Test
    fun currentPreviewAcceptsDecimalCommaInputs() {
        val state = CalculatorScreenState()

        state.selectedMaterial = "CuZn30"
        state.radialThickness = "150,5"
        state.coreDiameter = "400"
        state.thickness = "1,5"
        state.width = "72"
        state.coils = "2"

        assertThat(state.currentPreview).isNotNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: FAIL because `CalculatorScreenState.currentPreview` does not exist.

- [ ] **Step 3: Implement derived preview**

In `CalculatorScreenState.kt`, add imports:

```kotlin
import com.example.racunanjekilaze.data.computeFullResult
import com.example.racunanjekilaze.data.parseMaterial
import com.example.racunanjekilaze.data.parsePositiveDouble
import com.example.racunanjekilaze.data.parsePositiveInt
```

Add this property after `remainingWeight`:

```kotlin
val currentPreview by derivedStateOf {
    runCatching {
        val radial = parsePositiveDouble(radialThickness, "Poluprečnik trake")
        val core = parsePositiveDouble(coreDiameter, "Unutrašnji prečnik")
        val thicknessValue = parsePositiveDouble(thickness, "Debljina materijala")
        val widthValue = parsePositiveDouble(width, "Širina trake")
        val coilCount = parsePositiveInt(coils, "Broj traka")
        val material = parseMaterial(selectedMaterial)
        val dimensions = RollDimensions.fromRadialThickness(
            radialThicknessMm = radial,
            coreDiameterMm = core,
            thicknessMm = thicknessValue,
            widthMm = widthValue
        )

        computeFullResult(dimensions, material, coilCount)
    }.getOrNull()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS.

---

### Task 4: Restructure Input Section With Preview Panel

**Status:** Completed

**Files:**
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/sections/InputSection.kt`
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreen.kt`

**Security flag:** `none`

**Does NOT cover:** Does not move formulas into UI and does not add a new screen. Preview is only shown when all required inputs are valid.

- [ ] **Step 1: Pass preview into the section**

Add `preview: CalculationResult?` to `InputSection` parameters and import:

```kotlin
import com.example.racunanjekilaze.data.CalculationResult
import com.example.racunanjekilaze.data.formatValue
```

In `CalculatorScreen.kt`, pass:

```kotlin
preview = state.currentPreview,
```

- [ ] **Step 2: Update Serbian labels**

Use these exact labels in `InputSection.kt`:

```kotlin
"DIMENZIJE TRAKE"
"Materijal"
"Poluprečnik trake (mm)"
"Unutrašnji prečnik (mm)"
"Debljina materijala (mm)"
"Širina trake (mm)"
"Broj traka"
"Željena kilaža (kg) - opciono"
```

Keep current numeric keyboards, focus order, and validation predicates.

- [ ] **Step 3: Add preview card inside the existing input card**

Add a private composable at the bottom of `InputSection.kt`:

```kotlin
@Composable
private fun LivePreviewPanel(
    preview: CalculationResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "PREGLED PRE DODAVANJA",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Metraža: ${formatValue(preview.singleRoll.lengthM)} m",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Jedna traka: ${formatValue(preview.singleRoll.weightKg)} kg",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Ukupno za unos: ${formatValue(preview.totalWeightKg)} kg",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
```

After the target-weight field, render:

```kotlin
if (preview != null) {
    LivePreviewPanel(preview = preview)
}
```

- [ ] **Step 4: Verify**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS.

Manual check on device/emulator: preview appears only after material, dimensions, and `Broj traka` are valid; changing `Željena kilaža` alone does not affect preview.

---

### Task 5: Make Bottom Total And Primary Actions Sticky

**Status:** Completed

**Files:**
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/components/RoundedButton.kt`
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreen.kt`

**Security flag:** `none`

**Does NOT cover:** Does not change the meaning of add/reset actions; it only moves the primary action path into a sticky bottom area.

- [ ] **Step 1: Add enabled support to shared buttons**

In `RoundedButton.kt`, add `enabled: Boolean = true` to `PrimaryButton` and `SecondaryButton`, then pass it to `Button`:

```kotlin
Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.heightIn(min = minHeight),
    shape = MaterialTheme.shapes.large,
    colors = ButtonDefaults.buttonColors(
        containerColor = CopperDark,
        contentColor = Color.White,
        disabledContainerColor = SurfaceLight,
        disabledContentColor = TextSecondary
    ),
    contentPadding = contentPadding
)
```

For `SecondaryButton`, keep its current active colors and add matching disabled colors.

- [ ] **Step 2: Remove inline action buttons from the scroll content**

In `CalculatorScreen.kt`, remove the current compact/non-compact button block containing visible text `DODAJ` and `NOVI NALOG`.

- [ ] **Step 3: Add bottom action bar composable**

Add this private composable to `CalculatorScreen.kt`:

```kotlin
@Composable
private fun BottomActionBar(
    totalWeightText: String,
    totalCoilsText: String,
    canAddEntry: Boolean,
    layout: LayoutTokens,
    onAddEntry: () -> Unit,
    onRequestReset: () -> Unit
) {
    GlassCard(
        cornerRadius = 0.dp,
        elevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = layout.screenPaddingHorizontal,
                    vertical = 10.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = totalCoilsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AutoSizeText(
                    text = totalWeightText,
                    style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.End),
                    color = MaterialTheme.colorScheme.primary,
                    maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(layout.buttonSpacing)
            ) {
                PrimaryButton(
                    text = "Dodaj stavku",
                    onClick = onAddEntry,
                    enabled = canAddEntry,
                    modifier = Modifier.weight(1f),
                    minHeight = layout.buttonMinHeight,
                    contentPadding = layout.buttonContentPadding
                )
                SecondaryButton(
                    text = "Novi nalog",
                    onClick = onRequestReset,
                    modifier = Modifier.weight(1f),
                    minHeight = layout.buttonMinHeight,
                    contentPadding = layout.buttonContentPadding
                )
            }
        }
    }
}
```

Add imports used by this composable: `Alignment`, `LayoutTokens`, `GlassCard`, `AutoSizeText`, and `formatValue` if not already present.

- [ ] **Step 4: Render bottom action bar over the scroll content**

At the bottom of the root `Box` in `CalculatorScreen.kt`, render:

```kotlin
BottomActionBar(
    totalWeightText = "${formatValue(state.totalWeight)} kg",
    totalCoilsText = state.totalCoilsText,
    canAddEntry = state.canAddEntry,
    layout = layout,
    onAddEntry = addEntry,
    onRequestReset = resetOrder
)
```

Add bottom padding to the scroll content column so the last section is not hidden behind the bar:

```kotlin
.padding(bottom = 132.dp)
```

- [ ] **Step 5: Verify**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS.

Manual check on device/emulator: `Dodaj stavku`, `Novi nalog`, total coil count, and total kg remain visible while scrolling the form and list.

---

### Task 6: Add Reset Confirmation And Better Destructive Controls

**Status:** Completed

**Files:**
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreen.kt`
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/sections/OrderListSection.kt`
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/components/RoundedButton.kt`

**Security flag:** `none`

**Does NOT cover:** Confirmation is only for `Novi nalog` when entries exist. Single-row delete remains one-tap, but the target becomes larger and labeled for accessibility.

- [ ] **Step 1: Add confirmation state**

In `CalculatorScreen.kt`, add:

```kotlin
var showResetConfirmation by remember { mutableStateOf(false) }
```

Add imports:

```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
```

- [ ] **Step 2: Split reset request from reset execution**

Replace direct `resetOrder` calls from the bottom bar with:

```kotlin
val requestResetOrder = {
    if (state.orderEntries.isEmpty()) {
        resetOrder()
    } else {
        showResetConfirmation = true
    }
}
```

Pass `requestResetOrder` into `BottomActionBar`.

- [ ] **Step 3: Add dialog**

Render this inside the root `Box`:

```kotlin
if (showResetConfirmation) {
    AlertDialog(
        onDismissRequest = { showResetConfirmation = false },
        title = {
            Text(text = "Obrisati ceo nalog?")
        },
        text = {
            Text(text = "Sve stavke iz trenutnog naloga biće uklonjene.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showResetConfirmation = false
                    resetOrder()
                }
            ) {
                Text(text = "Obriši nalog")
            }
        },
        dismissButton = {
            TextButton(onClick = { showResetConfirmation = false }) {
                Text(text = "Otkaži")
            }
        }
    )
}
```

- [ ] **Step 4: Enlarge row delete touch target**

In `OrderListSection.kt`, change the delete button text from `x` to `Obriši` if there is enough width on non-compact layouts. On compact layouts keep a short visible mark but use a 44dp target:

```kotlin
SmallButton(
    text = if (layout.isCompact) "×" else "Obriši",
    onClick = onDelete,
    modifier = Modifier.heightIn(min = 44.dp),
    containerColor = Surface,
    contentColor = ErrorColor
)
```

Update `SmallButton` if needed so it accepts a wider modifier without forcing square dimensions.

- [ ] **Step 5: Verify**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS.

Manual check on device/emulator: tapping `Novi nalog` with at least one entry opens the confirmation dialog; `Otkaži` preserves entries; `Obriši nalog` clears entries; row delete target is easy to tap.

---

### Task 7: Redesign Order List Into Compact Work Table

**Status:** Completed

**Files:**
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/sections/OrderListSection.kt`

**Security flag:** `none`

**Does NOT cover:** Does not change entry sorting, saved entry data, total calculations, or max-entry behavior.

- [ ] **Step 1: Improve empty state copy**

Replace:

```kotlin
text = "Nema stavki."
```

with:

```kotlin
text = "Nalog je prazan. Unesi dimenzije i dodaj prvu stavku."
```

- [ ] **Step 2: Make row hierarchy scan faster**

In `OrderEntryRow`, keep the same calculated values but reorder visible text as:

```kotlin
val primaryText = "${entry.material}  •  $thicknessText x ${formatValue(dimensions.widthMm, 0)} mm"
val secondaryText = "${entry.coilCount} kom  •  ${formatValue(entry.result.singleRoll.lengthM)} m"
val singleWeightText = "Jedna traka: ${formatValue(entry.result.singleRoll.weightKg)} kg"
val totalWeightText = "${formatValue(entry.result.totalWeightKg)} kg"
```

Render `totalWeightText` as the strongest text on the right, `primaryText` as the first left row, and `secondaryText` plus `singleWeightText` below it.

- [ ] **Step 3: Reduce row elevation**

Change row card call to:

```kotlin
AccentGlassCard(
    bgColor = SurfaceLight,
    cornerRadius = 8.dp,
    elevation = 2.dp
)
```

- [ ] **Step 4: Keep internal scrolling usable**

Keep `heightIn(max = 400.dp)` on the list. Do not nest another scroll container around `LazyColumn`.

- [ ] **Step 5: Verify**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS.

Manual check on device/emulator: with 5+ entries, each row can be scanned by material/dimension, count, and total kg without reading every line.

---

### Task 8: Align Total Section With Sticky Summary

**Status:** Completed

**Files:**
- Modify: `app/src/main/java/com/example/racunanjekilaze/ui/sections/TotalSection.kt`

**Security flag:** `none`

**Does NOT cover:** Does not remove the sticky summary from Task 5. This section remains the detailed total/remaining target panel in the scroll content.

- [ ] **Step 1: Update Serbian copy**

Replace:

```kotlin
text = "UKUPNA TEZINA"
```

with:

```kotlin
text = "UKUPNA TEŽINA"
```

- [ ] **Step 2: Reduce dominant copper block feel**

Keep `AccentGlassCard`, but use a less heavy surface if the sticky bottom bar now carries the primary total:

```kotlin
bgColor = SurfaceElevated
```

Add import:

```kotlin
import com.example.racunanjekilaze.ui.theme.SurfaceElevated
```

Remove `CopperDark` import if unused.

- [ ] **Step 3: Keep remaining target highly visible**

Do not change `formatRemainingWeight(remainingWeight)` or the success/accent color rule:

```kotlin
val remainingColor = if (remainingInfo?.third == true) SuccessColor else Accent
```

- [ ] **Step 4: Verify**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: PASS.

Manual check on device/emulator: scroll total still shows target difference when `Željena kilaža` is entered, while sticky total stays focused on current order total.

---

### Task 9: Optional Instrumentation Tests For UI Behavior

**Status:** Completed

**Execution note:** `compileDebugAndroidTestKotlin` passed. `connectedDebugAndroidTest` was not run because `adb devices` reported no connected device.

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/androidTest/java/com/example/racunanjekilaze/CalculatorScreenUiTest.kt`
- Modify: UI files touched in Tasks 4-6 if tags are needed for stable tests.

**Security flag:** `none`

**Does NOT cover:** These tests do not replace manual visual checks. If no device/emulator is available, skip execution and do not claim UI-test proof.

- [ ] **Step 1: Add UI test dependencies**

In `app/build.gradle.kts`, add:

```kotlin
androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

- [ ] **Step 2: Add stable tags where needed**

Use `Modifier.testTag("bottom-action-bar")` on the bottom bar root and `Modifier.testTag("reset-confirmation-dialog")` on the dialog container if direct text lookup proves brittle. Add imports only in files that use tags:

```kotlin
import androidx.compose.ui.platform.testTag
```

- [ ] **Step 3: Create UI tests**

Create `CalculatorScreenUiTest.kt`:

```kotlin
package com.example.racunanjekilaze

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class CalculatorScreenUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryActionsUseFinalSerbianCopy() {
        composeRule.onNodeWithText("Dodaj stavku").assertIsDisplayed()
        composeRule.onNodeWithText("Novi nalog").assertIsDisplayed()
        composeRule.onNodeWithText("Računanje kilaže").assertIsDisplayed()
    }

    @Test
    fun emptyOrderUsesHelpfulCopy() {
        composeRule.onNodeWithText(
            "Nalog je prazan. Unesi dimenzije i dodaj prvu stavku."
        ).assertIsDisplayed()
    }
}
```

- [ ] **Step 4: Run instrumentation tests if a device/emulator is available**

Run: `.\gradlew.bat connectedDebugAndroidTest`

Expected: PASS on a connected device/emulator. If it fails with "No connected devices", record that UI tests were not run and continue with compile/lint/manual verification.

---

### Task 10: Final Accessibility, Layout, And Build Verification

**Status:** Completed

**Execution note:** `testDebugUnitTest lint assembleDebug` passed with `--no-daemon`. `connectedDebugAndroidTest` was not run because no device/emulator was connected.

**Files:**
- Review: `app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreen.kt`
- Review: `app/src/main/java/com/example/racunanjekilaze/ui/sections/HeaderSection.kt`
- Review: `app/src/main/java/com/example/racunanjekilaze/ui/sections/InputSection.kt`
- Review: `app/src/main/java/com/example/racunanjekilaze/ui/sections/OrderListSection.kt`
- Review: `app/src/main/java/com/example/racunanjekilaze/ui/sections/TotalSection.kt`
- Review: `app/src/main/java/com/example/racunanjekilaze/ui/components/RoundedButton.kt`
- Review: `app/src/main/java/com/example/racunanjekilaze/ui/components/MaterialGridSelector.kt`

**Security flag:** `none`

**Does NOT cover:** Does not add new features after Tasks 1-9; this task verifies the UI refresh and fixes regressions found during verification.

- [ ] **Step 1: Run full available Gradle checks**

Run: `.\gradlew.bat testDebugUnitTest lint`

Expected: PASS.

- [ ] **Step 2: Build debug APK**

Run: `.\gradlew.bat assembleDebug`

Expected: PASS and APK generated under `app/build/outputs/apk/debug/`.

- [ ] **Step 3: Manual 375dp-class phone verification**

On an emulator/device close to a narrow phone width, verify:

```text
- No horizontal scroll.
- Header is compact and does not dominate the viewport.
- All visible Serbian labels include proper diacritics.
- Bottom bar remains visible while scrolling.
- Bottom bar does not cover the last scrollable content.
- Buttons are at least 44dp high.
- Preview appears only after valid required inputs.
- Reset confirmation works only when entries exist.
```

- [ ] **Step 4: Manual larger phone/tablet verification**

On a wider emulator/device, verify:

```text
- Form spacing is still compact.
- List rows scan as a work table.
- Total weight is readable in both sticky bar and total section.
- Delete action has enough width and remains visually secondary to total kg.
```

- [ ] **Step 5: Reduced-motion and animation check**

Review `AnimatedVisibility` usage in `CalculatorScreen.kt`. Keep animations short as currently configured (`200ms` enter, `150ms` exit). If more animations were added during implementation, remove them or guard them with Compose motion policy before completion.

- [ ] **Step 6: Final proof**

Run: `.\gradlew.bat testDebugUnitTest lint assembleDebug`

Expected: PASS.

---

## Self-Review

- Spec coverage: Header compaction is covered by Task 2; sticky total/actions by Task 5; grouped input and live preview by Tasks 3-4; better list rows and delete action by Tasks 6-7; Serbian copy by Tasks 2, 4, and 8; visual token cleanup by Task 1; final accessibility/build checks by Task 10.
- Placeholder scan: No unresolved task content is left for the implementer.
- Type consistency: `currentPreview` is defined in `CalculatorScreenState` before `InputSection` consumes it; `CalculationResult` is imported where preview rendering needs it; button `enabled` support is added before the bottom bar uses it.
- Scope-reduction scan: The plan covers the full set of proposed UI improvements and explicitly excludes formula, sync, navigation, and persistence work.

