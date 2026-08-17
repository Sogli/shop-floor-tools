package com.precnik.ui.screens

import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorScreenSourceTest {
    @Test
    fun resultUnitIsInlineWithNumberAndUsesAccentColor() {
        val screen = readSource("src/main/java/com/precnik/ui/screens/CalculatorScreen.kt")

        assertTrue(screen.contains("\"\$result mm\""))
        assertTrue(screen.contains("color = Accent"))
        assertFalse(screen.contains("text = \"mm\""))
    }

    private fun readSource(relativePath: String): String {
        val cwd = Path.of("").toAbsolutePath()
        val directPath = cwd.resolve(relativePath)
        val repoPath = cwd.resolve("app").resolve(relativePath)
        val path = if (directPath.toFile().exists()) directPath else repoPath

        return path.toFile().readText()
    }
}
