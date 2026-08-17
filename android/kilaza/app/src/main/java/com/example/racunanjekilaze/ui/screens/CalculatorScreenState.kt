package com.example.racunanjekilaze.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.example.racunanjekilaze.data.CalculationResult
import com.example.racunanjekilaze.data.MessageState
import com.example.racunanjekilaze.data.MessageType
import com.example.racunanjekilaze.data.OrderEntry
import com.example.racunanjekilaze.data.RollDimensions
import com.example.racunanjekilaze.data.RollResult
import com.example.racunanjekilaze.data.computeFullResult
import com.example.racunanjekilaze.data.formatEntryCount
import com.example.racunanjekilaze.data.getTrakaFormNominative
import com.example.racunanjekilaze.data.parseOptionalDouble
import com.example.racunanjekilaze.data.parseMaterial
import com.example.racunanjekilaze.data.parsePositiveDouble
import com.example.racunanjekilaze.data.parsePositiveInt
import com.example.racunanjekilaze.ui.theme.MATERIAL_PLACEHOLDER

class CalculatorScreenState(
    initialRadialThickness: String = "",
    initialCoreDiameter: String = "",
    initialThickness: String = "",
    initialWidth: String = "",
    initialCoils: String = "",
    initialTargetWeight: String = "",
    initialSelectedMaterial: String = MATERIAL_PLACEHOLDER,
    initialNextEntryId: Int = 1,
    initialOrderEntries: List<OrderEntry> = emptyList()
) {
    // Input polja
    var radialThickness by mutableStateOf(initialRadialThickness)
    var coreDiameter by mutableStateOf(initialCoreDiameter)
    var thickness by mutableStateOf(initialThickness)
    var width by mutableStateOf(initialWidth)
    var coils by mutableStateOf(initialCoils)
    var targetWeight by mutableStateOf(initialTargetWeight)
    var selectedMaterial by mutableStateOf(initialSelectedMaterial)

    // Interno stanje
    var messageState by mutableStateOf(MessageState("", MessageType.NONE))
    var nextEntryId by mutableIntStateOf(initialNextEntryId)
    val orderEntries: SnapshotStateList<OrderEntry> = initialOrderEntries.toMutableStateList()

    // Debounce za dugme
    var isAddingEntry by mutableStateOf(false)

    // Derived state za zbir naloga, ciljnu kilazu i pregled trenutno validnog unosa.
    val totalWeight by derivedStateOf { orderEntries.sumOf { it.result.totalWeightKg } }
    val totalCoils by derivedStateOf { orderEntries.sumOf { it.coilCount } }
    val entryCountText by derivedStateOf { formatEntryCount(orderEntries.size) }
    val totalCoilsText by derivedStateOf { "$totalCoils ${getTrakaFormNominative(totalCoils)}" }
    val targetWeightValue by derivedStateOf { parseOptionalDouble(targetWeight) }
    val remainingWeight by derivedStateOf { targetWeightValue?.let { it - totalWeight } }
    val currentPreview by derivedStateOf {
        runCatching {
            val radial = parsePositiveDouble(radialThickness, "Poluprečnik trake")
            val core = parsePositiveDouble(coreDiameter, "Unutrašnji prečnik")
            val thicknessValue = parsePositiveDouble(thickness, "Debljina materijala")
            val widthValue = parsePositiveDouble(width, "Širina trake")
            val coilCount = if (coils.isBlank()) 1 else parsePositiveInt(coils, "Broj traka")
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

    // Maksimalan broj stavki
    val canAddEntry by derivedStateOf { orderEntries.size < MAX_ENTRIES && !isAddingEntry }

    companion object {
        const val MAX_ENTRIES = 200

        val Saver: Saver<CalculatorScreenState, *> = listSaver(
            save = { state ->
                val baseList = mutableListOf(
                    state.radialThickness,
                    state.coreDiameter,
                    state.thickness,
                    state.width,
                    state.coils,
                    state.targetWeight,
                    state.selectedMaterial,
                    state.nextEntryId.toString(),
                    state.orderEntries.size.toString()
                )
                // Serijalizuj svaku OrderEntry kao skup stringova
                state.orderEntries.forEach { entry ->
                    baseList.add(entry.id.toString())
                    baseList.add(entry.dimensions.outerDiameterMm.toString())
                    baseList.add(entry.dimensions.coreDiameterMm.toString())
                    baseList.add(entry.dimensions.thicknessMm.toString())
                    baseList.add(entry.dimensions.widthMm.toString())
                    baseList.add(entry.material)
                    baseList.add(entry.coilCount.toString())
                    baseList.add(entry.result.singleRoll.lengthM.toString())
                    baseList.add(entry.result.singleRoll.weightKg.toString())
                    baseList.add(entry.result.coilCount.toString())
                    baseList.add(entry.result.totalWeightKg.toString())
                }
                baseList
            },
            restore = { list ->
                val radial = list[0]
                val core = list[1]
                val thick = list[2]
                val wid = list[3]
                val coil = list[4]
                val target = list[5]
                val material = list[6]
                val nextId = list[7].toInt()
                val entryCount = list[8].toInt()

                val entries = mutableListOf<OrderEntry>()
                var idx = 9
                repeat(entryCount) {
                    val id = list[idx++].toInt()
                    val outerD = list[idx++].toDouble()
                    val coreD = list[idx++].toDouble()
                    val thickD = list[idx++].toDouble()
                    val widthD = list[idx++].toDouble()
                    val mat = list[idx++]
                    val cc = list[idx++].toInt()
                    val singleL = list[idx++].toDouble()
                    val singleW = list[idx++].toDouble()
                    val resCC = list[idx++].toInt()
                    val totalW = list[idx++].toDouble()

                    entries.add(
                        OrderEntry(
                            id = id,
                            dimensions = RollDimensions(outerD, coreD, thickD, widthD),
                            material = mat,
                            coilCount = cc,
                            result = CalculationResult(
                                singleRoll = RollResult(singleL, singleW),
                                coilCount = resCC,
                                totalWeightKg = totalW
                            )
                        )
                    )
                }

                CalculatorScreenState(
                    initialRadialThickness = radial,
                    initialCoreDiameter = core,
                    initialThickness = thick,
                    initialWidth = wid,
                    initialCoils = coil,
                    initialTargetWeight = target,
                    initialSelectedMaterial = material,
                    initialNextEntryId = nextId,
                    initialOrderEntries = entries
                )
            }
        )
    }
}

@Composable
fun rememberCalculatorScreenState(): CalculatorScreenState {
    return rememberSaveable(saver = CalculatorScreenState.Saver) {
        CalculatorScreenState()
    }
}
