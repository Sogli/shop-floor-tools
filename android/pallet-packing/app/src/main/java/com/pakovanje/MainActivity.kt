package com.pakovanje

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private lateinit var messageLabel: TextView
    private lateinit var resultsHeader: TextView
    private lateinit var resultsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val palletSpinner = findViewById<Spinner>(R.id.palletSpinner)
        val palletHeightInput = findViewById<TextInputEditText>(R.id.palletHeightInput)
        val quantityInput = findViewById<TextInputEditText>(R.id.quantityInput)
        val coilWidthInput = findViewById<TextInputEditText>(R.id.coilWidthInput)
        val separatorInput = findViewById<TextInputEditText>(R.id.separatorInput)
        val calculateButton = findViewById<MaterialButton>(R.id.calculateButton)
        messageLabel = findViewById(R.id.messageLabel)
        resultsHeader = findViewById(R.id.resultsHeader)
        resultsContainer = findViewById(R.id.resultsContainer)

        ArrayAdapter.createFromResource(
            this,
            R.array.pallet_sizes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            palletSpinner.adapter = adapter
        }

        val palletSizes = resources.getStringArray(R.array.pallet_sizes)
        val defaultIndex = palletSizes.indexOf("90").takeIf { it >= 0 } ?: 0
        palletSpinner.setSelection(defaultIndex)

        // Kada se izabere paleta, fokusiraj sledeće polje
        palletSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                palletHeightInput.requestFocus()
                showKeyboard(palletHeightInput)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Kada se pritisne Done na poslednjem polju, pokreni izračunavanje
        separatorInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                calculateButton.performClick()
                true
            } else {
                false
            }
        }

        calculateButton.setOnClickListener {
            messageLabel.visibility = View.GONE
            resultsHeader.visibility = View.GONE
            resultsContainer.removeAllViews()

            try {
                val palletSize = parsePalletSize(palletSpinner.selectedItem?.toString().orEmpty())
                val palletHeight = parsePositiveDouble(
                    palletHeightInput.text?.toString().orEmpty(),
                    "Visina palete",
                    allowZero = false
                )
                val quantity = parsePositiveInt(quantityInput.text?.toString().orEmpty(), "Kolicina")
                val coilWidth = parsePositiveDouble(
                    coilWidthInput.text?.toString().orEmpty(),
                    "Sirina kotura",
                    allowZero = false
                )
                val separator = parsePositiveDouble(
                    separatorInput.text?.toString().orEmpty(),
                    "Sirina separatora",
                    allowZero = true
                )

                val maxHeight = palletSize * 10.0 - palletHeight
                if (maxHeight <= 0.0) {
                    throw IllegalArgumentException("Visina palete je veca od raspolozive visine.")
                }
                val capacity = maxStackableCoils(maxHeight, coilWidth, separator)
                if (capacity == 0) {
                    throw IllegalArgumentException("Sirina kotura je prevelika za izabranu paletu.")
                }

                val distribution = distributeEvenly(quantity, capacity)
                resultsHeader.text = getString(R.string.result_header_format, quantity, capacity)
                resultsHeader.visibility = View.VISIBLE

                distribution.forEachIndexed { index, count ->
                    val separatorsCount = max(0, count - 1)
                    val totalHeight = palletHeight + (count * coilWidth) + (separatorsCount * separator)
                    addResultCard(
                        title = getString(R.string.pallet_title_format, index + 1),
                        badge = getString(R.string.pallet_badge_format, count),
                        subtitle = getString(
                            R.string.pallet_height_format,
                            formatNumber(totalHeight)
                        )
                    )
                }
            } catch (ex: IllegalArgumentException) {
                showError(ex.message ?: "Greska u proracunu.")
            }
        }
    }

    private fun showError(message: String) {
        messageLabel.text = message
        messageLabel.setTextColor(ContextCompat.getColor(this, R.color.error))
        messageLabel.visibility = View.VISIBLE
    }

    private fun addResultCard(title: String, badge: String, subtitle: String) {
        val card = layoutInflater.inflate(
            R.layout.item_pallet_result,
            resultsContainer,
            false
        ) as MaterialCardView
        val titleView = card.findViewById<TextView>(R.id.resultTitle)
        val badgeView = card.findViewById<TextView>(R.id.resultBadge)
        val subtitleView = card.findViewById<TextView>(R.id.resultSubtitle)

        titleView.text = title
        badgeView.text = badge
        subtitleView.text = subtitle

        resultsContainer.addView(card)
    }

    private fun parsePalletSize(raw: String): Int {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) {
            throw IllegalArgumentException("Dimenzija palete je obavezna.")
        }
        return cleaned.toIntOrNull()
            ?: throw IllegalArgumentException("Dimenzija palete mora biti broj.")
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

    private fun maxStackableCoils(maxHeight: Double, coilWidth: Double, separator: Double): Int {
        if (coilWidth <= 0.0) {
            return 0
        }
        val capacity = floor((maxHeight + separator) / (coilWidth + separator)).toInt()
        return capacity.coerceAtLeast(0)
    }

    private fun distributeEvenly(total: Int, capacity: Int): List<Int> {
        if (capacity <= 0) {
            return emptyList()
        }
        var pallets = ceil(total.toDouble() / capacity.toDouble()).toInt()
        while (true) {
            val base = total / pallets
            val remainder = total % pallets
            val sizes = MutableList(pallets) { base }
            for (index in 0 until remainder) {
                sizes[index] = base + 1
            }
            if (sizes.all { it <= capacity }) {
                return sizes
            }
            pallets += 1
        }
    }

    private fun formatNumber(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}
