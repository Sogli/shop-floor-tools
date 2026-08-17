package com.example.racunanjekilaze.ui.sections

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InputSectionFormattingTest {
    @Test
    fun formatCoilWeightTitle_usesSerbianCases() {
        assertThat(formatCoilWeightTitle(1)).isEqualTo("Kilaža za 1 traku")
        assertThat(formatCoilWeightTitle(2)).isEqualTo("Kilaža za 2 trake")
        assertThat(formatCoilWeightTitle(5)).isEqualTo("Kilaža za 5 traka")
    }
}
