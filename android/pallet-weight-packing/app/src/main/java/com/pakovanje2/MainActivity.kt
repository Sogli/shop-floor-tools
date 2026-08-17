package com.pakovanje2

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val MM_TO_M = 1000.0
private const val PALLET_CLEARANCE_MM = 50.0
private const val MATERIAL_PLACEHOLDER = "Izaberite"
private const val WEIGHT_TOLERANCE = 0.10
private const val WEIGHT_PRECISION = 10

private val MATERIAL_DENSITIES = mapOf(
    "cu" to 8960.0,
    "cuzn10" to 8800.0,
    "cuzn15" to 8686.0,
    "cuzn20" to 8530.0,
    "cuzn30" to 8403.0,
    "cuzn37" to 8285.0
)

private val MATERIAL_ALIASES = mapOf(
    "10" to "cuzn10",
    "15" to "cuzn15",
    "20" to "cuzn20",
    "30" to "cuzn30",
    "37" to "cuzn37"
)

private data class RollDimensions(
    val outerDiameterMm: Double,
    val coreDiameterMm: Double,
    val thicknessMm: Double,
    val widthMm: Double
) {
    companion object {
        fun fromRadialThickness(
            radialThicknessMm: Double,
            coreDiameterMm: Double,
            thicknessMm: Double,
            widthMm: Double
        ): RollDimensions {
            val outerDiameter = coreDiameterMm + (2.0 * radialThicknessMm)
            return RollDimensions(outerDiameter, coreDiameterMm, thicknessMm, widthMm)
        }
    }

    val outerRadiusMm: Double
        get() = outerDiameterMm / 2.0

    val coreRadiusMm: Double
        get() = coreDiameterMm / 2.0

    val radialThicknessMm: Double
        get() = (outerDiameterMm - coreDiameterMm) / 2.0

    fun validate() {
        if (thicknessMm <= 0.0) {
            throw IllegalArgumentException("Debljina materijala mora biti pozitivna vrednost.")
        }
        if (widthMm <= 0.0) {
            throw IllegalArgumentException("Sirina trake mora biti pozitivna vrednost.")
        }
        if (outerRadiusMm <= coreRadiusMm) {
            throw IllegalArgumentException("Spoljasnji precnik mora biti veci od unutrasnjeg precnika dobosa.")
        }
    }
}

private data class RollResult(
    val lengthM: Double,
    val weightKg: Double
)

private data class CoilEntry(
    val material: String,
    val dimensions: RollDimensions,
    val coilCount: Int,
    val roll: RollResult
) {
    val totalWeightKg: Double
        get() = roll.weightKg * coilCount
}

private data class PalletLoad(
    val index: Int,
    val coilCount: Int,
    val totalHeightMm: Double,
    val maxHeightMm: Double,
    val totalWeightKg: Double
)

private data class CoilItem(
    val widthMm: Double,
    val weightKg: Double
)

private data class PalletState(
    var coilCount: Int,
    var stackHeightMm: Double,
    var totalWeightKg: Double,
    val items: MutableList<CoilItem>
)

private data class TargetSelection(
    val items: List<CoilItem>,
    val totalWeightKg: Double,
    val inTolerance: Boolean
)

class MainActivity : AppCompatActivity() {
    private lateinit var messageLabel: TextView
    private lateinit var recommendedPalletText: TextView
    private lateinit var entriesHeader: TextView
    private lateinit var entriesContainer: LinearLayout
    private lateinit var resultsHeader: TextView
    private lateinit var resultsContainer: LinearLayout

    private val entries = mutableListOf<CoilEntry>()
    private var recommendedPalletSizeCm: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val palletHeightInput = findViewById<TextInputEditText>(R.id.palletHeightInput)
        val separatorInput = findViewById<TextInputEditText>(R.id.separatorInput)
        val maxPalletWeightInput = findViewById<TextInputEditText>(R.id.maxPalletWeightInput)
        val targetWeightInput = findViewById<TextInputEditText>(R.id.targetWeightInput)
        val manualPalletSizeInput = findViewById<TextInputEditText>(R.id.manualPalletSizeInput)

        val radialThicknessInput = findViewById<TextInputEditText>(R.id.radialThicknessInput)
        val coreDiameterInput = findViewById<TextInputEditText>(R.id.coreDiameterInput)
        val materialThicknessInput = findViewById<TextInputEditText>(R.id.materialThicknessInput)
        val stripWidthInput = findViewById<TextInputEditText>(R.id.stripWidthInput)
        val coilCountInput = findViewById<TextInputEditText>(R.id.coilCountInput)
        val materialSpinner = findViewById<Spinner>(R.id.materialSpinner)

        val addEntryButton = findViewById<MaterialButton>(R.id.addEntryButton)
        val calculateButton = findViewById<MaterialButton>(R.id.calculateButton)
        val resetButton = findViewById<MaterialButton>(R.id.resetButton)

        messageLabel = findViewById(R.id.messageLabel)
        recommendedPalletText = findViewById(R.id.recommendedPalletText)
        entriesHeader = findViewById(R.id.entriesHeader)
        entriesContainer = findViewById(R.id.entriesContainer)
        resultsHeader = findViewById(R.id.resultsHeader)
        resultsContainer = findViewById(R.id.resultsContainer)

        ArrayAdapter.createFromResource(
            this,
            R.array.material_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            materialSpinner.adapter = adapter
        }
        materialSpinner.setSelection(0)
        updateRecommendedPallet()

        // Kada se pritisne Done na poslednjem polju, pokreni dodavanje
        coilCountInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                addEntryButton.performClick()
                true
            } else {
                false
            }
        }

        addEntryButton.setOnClickListener {
            clearMessage()
            clearResults()
            try {
                val radial = parsePositiveDouble(
                    radialThicknessInput.text?.toString().orEmpty(),
                    "Poluprecnik trake",
                    allowZero = false
                )
                val core = parsePositiveDouble(
                    coreDiameterInput.text?.toString().orEmpty(),
                    "Unutrasnji precnik",
                    allowZero = false
                )
                val thicknessValue = parsePositiveDouble(
                    materialThicknessInput.text?.toString().orEmpty(),
                    "Debljina materijala",
                    allowZero = false
                )
                val widthValue = parsePositiveDouble(
                    stripWidthInput.text?.toString().orEmpty(),
                    "Sirina trake",
                    allowZero = false
                )
                val coilCount = parsePositiveInt(
                    coilCountInput.text?.toString().orEmpty(),
                    "Broj kotura"
                )
                val material = parseMaterial(materialSpinner.selectedItem?.toString().orEmpty())

                val dimensions = RollDimensions.fromRadialThickness(
                    radialThicknessMm = radial,
                    coreDiameterMm = core,
                    thicknessMm = thicknessValue,
                    widthMm = widthValue
                )
                val roll = computeRoll(dimensions, material)
                val entry = CoilEntry(
                    material = material,
                    dimensions = dimensions,
                    coilCount = coilCount,
                    roll = roll
                )
                entries.add(entry)
                renderEntries()
                updateRecommendedPallet()
                coilCountInput.setText(getString(R.string.default_coil_count))
                showMessage(
                    getString(R.string.entry_added_format, formatValue(entry.totalWeightKg, 1)),
                    isError = false
                )
            } catch (ex: IllegalArgumentException) {
                showMessage(ex.message ?: "Greska u unosu.", isError = true)
            }
        }

        calculateButton.setOnClickListener {
            clearMessage()
            resultsContainer.removeAllViews()
            resultsHeader.visibility = View.GONE

            if (entries.isEmpty()) {
                showMessage(getString(R.string.missing_entries_error), isError = true)
                return@setOnClickListener
            }

            try {
                val palletHeight = parsePositiveDouble(
                    palletHeightInput.text?.toString().orEmpty(),
                    "Visina palete",
                    allowZero = false
                )
                val separator = parsePositiveDouble(
                    separatorInput.text?.toString().orEmpty(),
                    "Sirina separatora",
                    allowZero = true
                )
                val maxPalletWeight = parseOptionalPositiveDouble(
                    maxPalletWeightInput.text?.toString().orEmpty(),
                    "Maksimalna kilaza po paleti"
                )
                val targetWeight = parseOptionalPositiveDouble(
                    targetWeightInput.text?.toString().orEmpty(),
                    "Trazena kilaza"
                )
                val manualPalletSize = parseOptionalPositiveDouble(
                    manualPalletSizeInput.text?.toString().orEmpty(),
                    "Dimenzija palete"
                )
                val palletSizeCm = manualPalletSize
                    ?: recommendedPalletSizeCm?.toDouble()
                    ?: throw IllegalArgumentException("Nema preporucene dimenzije palete.")

                val maxStackHeight = palletSizeCm * 10.0 - palletHeight
                if (maxStackHeight <= 0.0) {
                    throw IllegalArgumentException("Visina palete je veca od raspolozive visine.")
                }

                val items = buildItems(
                    entries = entries,
                    maxStackHeightMm = maxStackHeight,
                    maxPalletWeightKg = maxPalletWeight
                )
                val selection = selectItemsForTarget(items, targetWeight)
                val selectedItems = selection.items
                if (selectedItems.isEmpty()) {
                    throw IllegalArgumentException("Nema koturova za trazenu kilazu.")
                }
                val totalCoils = selectedItems.size
                val totalWeight = selection.totalWeightKg
                val pallets = buildCombinedPlan(
                    items = selectedItems,
                    maxStackHeightMm = maxStackHeight,
                    palletHeightMm = palletHeight,
                    separatorMm = separator,
                    maxPalletWeightKg = maxPalletWeight
                )
                resultsHeader.text = if (targetWeight != null) {
                    getString(
                        R.string.result_header_target_format,
                        totalCoils,
                        formatValue(totalWeight, 1),
                        formatValue(targetWeight, 1)
                    )
                } else {
                    getString(
                        R.string.result_header_format,
                        totalCoils,
                        formatValue(totalWeight, 1)
                    )
                }
                resultsHeader.visibility = View.VISIBLE
                if (targetWeight != null && !selection.inTolerance) {
                    showMessage(
                        getString(
                            R.string.target_weight_warning_format,
                            formatValue(targetWeight, 1),
                            formatValue(totalWeight, 1)
                        ),
                        isError = true
                    )
                }

                pallets.forEach { pallet ->
                    addResultCard(
                        resultsContainer,
                        title = getString(R.string.pallet_title_format, pallet.index),
                        badge = getString(R.string.pallet_badge_format, pallet.coilCount),
                        subtitle = getString(
                            R.string.pallet_detail_format,
                            formatValue(pallet.totalHeightMm, 1),
                            formatValue(pallet.maxHeightMm, 1),
                            formatValue(pallet.maxHeightMm - pallet.totalHeightMm, 1),
                            formatValue(pallet.totalWeightKg, 1)
                        )
                    )
                }
            } catch (ex: IllegalArgumentException) {
                showMessage(ex.message ?: "Greska u proracunu.", isError = true)
            }
        }

        resetButton.setOnClickListener {
            entries.clear()
            entriesContainer.removeAllViews()
            entriesHeader.visibility = View.GONE
            resultsContainer.removeAllViews()
            resultsHeader.visibility = View.GONE
            updateRecommendedPallet()
            showMessage(getString(R.string.reset_message), isError = false)
        }
    }

    private fun renderEntries() {
        entriesContainer.removeAllViews()
        if (entries.isEmpty()) {
            entriesHeader.visibility = View.GONE
            return
        }
        entriesHeader.text = getString(R.string.entries_header_format, entries.size)
        entriesHeader.visibility = View.VISIBLE

        entries.forEachIndexed { index, entry ->
            val dimensionsText = getString(
                R.string.entry_dimensions_format,
                entry.material,
                formatValue(entry.dimensions.radialThicknessMm, 1),
                formatValue(entry.dimensions.coreDiameterMm, 1),
                formatValue(entry.dimensions.thicknessMm, 2),
                formatValue(entry.dimensions.widthMm, 1)
            )
            val weightText = getString(
                R.string.entry_weight_format,
                formatValue(entry.roll.weightKg, 1),
                formatValue(entry.totalWeightKg, 1)
            )
            val subtitle = listOf(dimensionsText, weightText).joinToString("\n")
            addResultCard(
                entriesContainer,
                title = getString(R.string.entry_title_format, index + 1),
                badge = getString(R.string.entry_badge_format, entry.coilCount),
                subtitle = subtitle
            )
        }
    }

    private fun updateRecommendedPallet() {
        val maxOuterDiameter = entries.maxOfOrNull { it.dimensions.outerDiameterMm }
        if (maxOuterDiameter == null) {
            recommendedPalletSizeCm = null
            recommendedPalletText.text = getString(R.string.recommended_pallet_empty)
            return
        }
        val sizes = resources.getStringArray(R.array.pallet_sizes)
            .mapNotNull { it.toIntOrNull() }
            .sorted()
        if (sizes.isEmpty()) {
            recommendedPalletSizeCm = null
            recommendedPalletText.text = getString(R.string.recommended_pallet_empty)
            return
        }
        val requiredMm = maxOuterDiameter + PALLET_CLEARANCE_MM
        val recommended = sizes.firstOrNull { (it * 10.0) > requiredMm } ?: sizes.last()
        recommendedPalletSizeCm = recommended
        recommendedPalletText.text = getString(R.string.recommended_pallet_format, recommended)
    }

    private fun clearResults() {
        resultsHeader.visibility = View.GONE
        resultsContainer.removeAllViews()
    }

    private fun clearMessage() {
        messageLabel.visibility = View.GONE
    }

    private fun showMessage(message: String, isError: Boolean) {
        messageLabel.text = message
        val colorId = if (isError) R.color.error else R.color.success
        messageLabel.setTextColor(ContextCompat.getColor(this, colorId))
        messageLabel.visibility = View.VISIBLE
    }

    private fun addResultCard(
        container: LinearLayout,
        title: String,
        badge: String,
        subtitle: String
    ) {
        val card = layoutInflater.inflate(
            R.layout.item_pallet_result,
            container,
            false
        ) as MaterialCardView
        val titleView = card.findViewById<TextView>(R.id.resultTitle)
        val badgeView = card.findViewById<TextView>(R.id.resultBadge)
        val subtitleView = card.findViewById<TextView>(R.id.resultSubtitle)

        titleView.text = title
        badgeView.text = badge
        subtitleView.text = subtitle

        container.addView(card)
    }

    private fun buildItems(
        entries: List<CoilEntry>,
        maxStackHeightMm: Double,
        maxPalletWeightKg: Double?
    ): List<CoilItem> {
        val items = mutableListOf<CoilItem>()
        entries.forEach { entry ->
            if (entry.dimensions.widthMm > maxStackHeightMm) {
                throw IllegalArgumentException("Sirina kotura je prevelika za izabranu paletu.")
            }
            if (maxPalletWeightKg != null && entry.roll.weightKg > maxPalletWeightKg) {
                throw IllegalArgumentException(
                    "Maksimalna kilaza po paleti je manja od tezine jednog kotura."
                )
            }
            repeat(entry.coilCount) {
                items.add(CoilItem(entry.dimensions.widthMm, entry.roll.weightKg))
            }
        }
        return items
    }

    private fun selectItemsForTarget(
        items: List<CoilItem>,
        targetWeightKg: Double?
    ): TargetSelection {
        val totalWeight = items.sumOf { it.weightKg }
        if (targetWeightKg == null || items.isEmpty()) {
            return TargetSelection(items, totalWeight, true)
        }
        val minWeight = targetWeightKg * (1.0 - WEIGHT_TOLERANCE)
        val maxWeight = targetWeightKg * (1.0 + WEIGHT_TOLERANCE)
        if (totalWeight in minWeight..maxWeight) {
            return TargetSelection(items, totalWeight, true)
        }
        return pickBestItemsForTarget(items, targetWeightKg, minWeight, maxWeight)
    }

    private fun pickBestItemsForTarget(
        items: List<CoilItem>,
        targetWeightKg: Double,
        minWeightKg: Double,
        maxWeightKg: Double
    ): TargetSelection {
        val weightUnits = items.map { weightToUnits(it.weightKg) }
        val totalUnits = weightUnits.sum()
        val targetUnits = weightToUnits(targetWeightKg)
        val minUnits = ceil(minWeightKg * WEIGHT_PRECISION - 0.0001).toInt()
        val maxUnits = floor(maxWeightKg * WEIGHT_PRECISION + 0.0001).toInt()
        val prevSum = IntArray(totalUnits + 1) { -1 }
        val prevItem = IntArray(totalUnits + 1) { -1 }
        prevSum[0] = -2

        weightUnits.forEachIndexed { index, weight ->
            for (sum in totalUnits - weight downTo 0) {
                if (prevSum[sum] != -1 && prevSum[sum + weight] == -1) {
                    prevSum[sum + weight] = sum
                    prevItem[sum + weight] = index
                }
            }
        }

        val bestSum = findBestSum(
            prevSum = prevSum,
            targetUnits = targetUnits,
            minUnits = minUnits,
            maxUnits = maxUnits,
            requireNonZero = items.isNotEmpty()
        )
        if (bestSum <= 0) {
            return TargetSelection(emptyList(), 0.0, false)
        }

        val selectedFlags = BooleanArray(items.size)
        var current = bestSum
        while (current > 0) {
            val index = prevItem[current]
            if (index < 0) {
                break
            }
            selectedFlags[index] = true
            current = prevSum[current]
        }

        val selectedItems = items.filterIndexed { index, _ -> selectedFlags[index] }
        val totalWeight = selectedItems.sumOf { it.weightKg }
        val inTolerance = totalWeight in minWeightKg..maxWeightKg
        return TargetSelection(selectedItems, totalWeight, inTolerance)
    }

    private fun findBestSum(
        prevSum: IntArray,
        targetUnits: Int,
        minUnits: Int,
        maxUnits: Int,
        requireNonZero: Boolean
    ): Int {
        val start = if (requireNonZero) 1 else 0
        val end = prevSum.lastIndex
        val rangeStart = minUnits.coerceAtLeast(start)
        val rangeEnd = maxUnits.coerceAtMost(end)
        var bestSum = -1
        var bestDiff = Int.MAX_VALUE

        if (rangeStart <= rangeEnd) {
            for (sum in rangeStart..rangeEnd) {
                if (prevSum[sum] == -1) {
                    continue
                }
                val diff = abs(targetUnits - sum)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestSum = sum
                }
            }
        }
        if (bestSum >= 0) {
            return bestSum
        }
        for (sum in start..end) {
            if (prevSum[sum] == -1) {
                continue
            }
            val diff = abs(targetUnits - sum)
            if (diff < bestDiff) {
                bestDiff = diff
                bestSum = sum
            }
        }
        return bestSum
    }

    private fun weightToUnits(weightKg: Double): Int {
        return (weightKg * WEIGHT_PRECISION).roundToInt().coerceAtLeast(1)
    }

    private fun buildCombinedPlan(
        items: List<CoilItem>,
        maxStackHeightMm: Double,
        palletHeightMm: Double,
        separatorMm: Double,
        maxPalletWeightKg: Double?
    ): List<PalletLoad> {
        if (items.isEmpty()) {
            return emptyList()
        }

        val sortedItems = items.toMutableList()
        sortedItems.sortWith(
            compareByDescending<CoilItem> { it.widthMm }.thenByDescending { it.weightKg }
        )

        val pallets = mutableListOf<PalletState>()
        sortedItems.forEach { item ->
            var bestIndex = -1
            var bestRemaining = Double.MAX_VALUE
            pallets.forEachIndexed { index, pallet ->
                val extraHeight = if (pallet.coilCount == 0) {
                    item.widthMm
                } else {
                    item.widthMm + separatorMm
                }
                val newHeight = pallet.stackHeightMm + extraHeight
                if (newHeight > maxStackHeightMm) {
                    return@forEachIndexed
                }
                if (maxPalletWeightKg != null && (pallet.totalWeightKg + item.weightKg) > maxPalletWeightKg) {
                    return@forEachIndexed
                }
                val remaining = maxStackHeightMm - newHeight
                if (remaining < bestRemaining) {
                    bestRemaining = remaining
                    bestIndex = index
                }
            }

            if (bestIndex >= 0) {
                val pallet = pallets[bestIndex]
                addItemToPallet(pallet, item, separatorMm)
            } else {
                pallets.add(
                    PalletState(
                        coilCount = 1,
                        stackHeightMm = item.widthMm,
                        totalWeightKg = item.weightKg,
                        items = mutableListOf(item)
                    )
                )
            }
        }

        balancePallets(
            pallets = pallets,
            maxStackHeightMm = maxStackHeightMm,
            separatorMm = separatorMm,
            maxPalletWeightKg = maxPalletWeightKg
        )

        return pallets.mapIndexed { index, pallet ->
            PalletLoad(
                index = index + 1,
                coilCount = pallet.coilCount,
                totalHeightMm = palletHeightMm + pallet.stackHeightMm,
                maxHeightMm = palletHeightMm + maxStackHeightMm,
                totalWeightKg = pallet.totalWeightKg
            )
        }
    }

    private fun parseMaterial(raw: String): String {
        val cleaned = raw.trim()
        if (cleaned.isEmpty() || cleaned.equals(MATERIAL_PLACEHOLDER, ignoreCase = true)) {
            throw IllegalArgumentException("Materijal je obavezno polje.")
        }
        return cleaned
    }

    private fun parsePositiveInt(raw: String, fieldName: String): Int {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) {
            throw IllegalArgumentException("$fieldName je obavezno polje.")
        }
        val value = cleaned.toIntOrNull()
            ?: throw IllegalArgumentException("$fieldName mora biti ceo broj.")
        if (value <= 0) {
            throw IllegalArgumentException("$fieldName mora biti pozitivna vrednost.")
        }
        return value
    }

    private fun parsePositiveDouble(raw: String, fieldName: String, allowZero: Boolean): Double {
        val cleaned = raw.trim().replace(",", ".")
        if (cleaned.isEmpty()) {
            throw IllegalArgumentException("$fieldName je obavezno polje.")
        }
        val value = cleaned.toDoubleOrNull()
            ?: throw IllegalArgumentException("$fieldName mora biti numericka vrednost.")
        if (allowZero && value < 0.0) {
            throw IllegalArgumentException("$fieldName mora biti nenegativna vrednost.")
        }
        if (!allowZero && value <= 0.0) {
            throw IllegalArgumentException("$fieldName mora biti pozitivna vrednost.")
        }
        return value
    }

    private fun parseOptionalPositiveDouble(raw: String, fieldName: String): Double? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) {
            return null
        }
        val value = cleaned.replace(",", ".").toDoubleOrNull()
            ?: throw IllegalArgumentException("$fieldName mora biti numericka vrednost.")
        if (value <= 0.0) {
            throw IllegalArgumentException("$fieldName mora biti pozitivna vrednost.")
        }
        return value
    }

    private fun normalizeMaterial(materialInput: String): String {
        val material = materialInput.trim().lowercase(Locale.US)
        return MATERIAL_ALIASES[material] ?: material
    }

    private fun getDensityOrThrow(materialInput: String): Double {
        val normalized = normalizeMaterial(materialInput)
        return MATERIAL_DENSITIES[normalized] ?: throw IllegalArgumentException(
            "Nepoznat materijal. Koristite Cu, CuZn10, CuZn15, CuZn20, CuZn30, CuZn37."
        )
    }

    private fun calculateRollLength(dimensions: RollDimensions): Double {
        dimensions.validate()
        val outerR = dimensions.outerRadiusMm
        val coreR = dimensions.coreRadiusMm
        val crossSectionalArea = PI * (outerR + coreR) * (outerR - coreR)
        val lengthMm = crossSectionalArea / dimensions.thicknessMm
        return lengthMm / MM_TO_M
    }

    private fun calculateRollWeight(lengthM: Double, dimensions: RollDimensions, density: Double): Double {
        val widthM = dimensions.widthMm / MM_TO_M
        val thicknessM = dimensions.thicknessMm / MM_TO_M
        val volumeM3 = lengthM * widthM * thicknessM
        return volumeM3 * density
    }

    private fun computeRoll(dimensions: RollDimensions, material: String): RollResult {
        val density = getDensityOrThrow(material)
        val length = calculateRollLength(dimensions)
        val weight = calculateRollWeight(length, dimensions, density)
        return RollResult(lengthM = length, weightKg = weight)
    }

    private fun balancePallets(
        pallets: MutableList<PalletState>,
        maxStackHeightMm: Double,
        separatorMm: Double,
        maxPalletWeightKg: Double?
    ) {
        if (pallets.size < 2) {
            return
        }
        val maxIterations = pallets.sumOf { it.coilCount } * pallets.size
        var iterations = 0
        while (iterations < maxIterations) {
            var moved = false
            val donors = pallets.sortedByDescending { it.coilCount }
            val receivers = pallets.sortedBy { it.coilCount }
            for (donor in donors) {
                for (receiver in receivers) {
                    if (donor === receiver) {
                        continue
                    }
                    if (donor.coilCount - receiver.coilCount <= 1) {
                        continue
                    }
                    val itemIndex = findMovableItemIndex(
                        donor = donor,
                        receiver = receiver,
                        maxStackHeightMm = maxStackHeightMm,
                        separatorMm = separatorMm,
                        maxPalletWeightKg = maxPalletWeightKg
                    )
                    if (itemIndex >= 0) {
                        val item = removeItemFromPallet(donor, itemIndex, separatorMm)
                        addItemToPallet(receiver, item, separatorMm)
                        moved = true
                        break
                    }
                }
                if (moved) {
                    break
                }
            }
            if (!moved) {
                break
            }
            iterations += 1
        }
    }

    private fun findMovableItemIndex(
        donor: PalletState,
        receiver: PalletState,
        maxStackHeightMm: Double,
        separatorMm: Double,
        maxPalletWeightKg: Double?
    ): Int {
        var bestIndex = -1
        var bestWidth = Double.MAX_VALUE
        var bestWeight = Double.MAX_VALUE
        donor.items.forEachIndexed { index, item ->
            if (!canAddItem(receiver, item, maxStackHeightMm, separatorMm, maxPalletWeightKg)) {
                return@forEachIndexed
            }
            if (item.widthMm < bestWidth || (item.widthMm == bestWidth && item.weightKg < bestWeight)) {
                bestIndex = index
                bestWidth = item.widthMm
                bestWeight = item.weightKg
            }
        }
        return bestIndex
    }

    private fun canAddItem(
        pallet: PalletState,
        item: CoilItem,
        maxStackHeightMm: Double,
        separatorMm: Double,
        maxPalletWeightKg: Double?
    ): Boolean {
        val extraHeight = if (pallet.coilCount == 0) {
            item.widthMm
        } else {
            item.widthMm + separatorMm
        }
        val newHeight = pallet.stackHeightMm + extraHeight
        if (newHeight > maxStackHeightMm) {
            return false
        }
        if (maxPalletWeightKg != null && (pallet.totalWeightKg + item.weightKg) > maxPalletWeightKg) {
            return false
        }
        return true
    }

    private fun addItemToPallet(pallet: PalletState, item: CoilItem, separatorMm: Double) {
        val extraHeight = if (pallet.coilCount == 0) {
            item.widthMm
        } else {
            item.widthMm + separatorMm
        }
        pallet.stackHeightMm += extraHeight
        pallet.totalWeightKg += item.weightKg
        pallet.coilCount += 1
        pallet.items.add(item)
    }

    private fun removeItemFromPallet(
        pallet: PalletState,
        itemIndex: Int,
        separatorMm: Double
    ): CoilItem {
        val item = pallet.items.removeAt(itemIndex)
        val extraHeight = if (pallet.coilCount <= 1) {
            item.widthMm
        } else {
            item.widthMm + separatorMm
        }
        pallet.stackHeightMm -= extraHeight
        pallet.totalWeightKg -= item.weightKg
        pallet.coilCount -= 1
        return item
    }

    private fun formatValue(value: Double, decimals: Int = 1): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}
