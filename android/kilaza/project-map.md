# Project Map
_Generated: 2026-05-12 22:44 | Staleness: timestamps_

## Directory Structure
app/ — Android Compose kalkulator težine/dužine kotura i zbirnih porudžbina.
app/src/main/java/com/example/racunanjekilaze/data/ — čista kalkulacija, modeli i validacija dimenzija.
app/src/main/java/com/example/racunanjekilaze/ui/screens/ — glavni calculator ekran i zapamćeni state.
app/src/main/java/com/example/racunanjekilaze/ui/sections/ — podeljeni UI blokovi za header, input, listu i total.
app/src/main/java/com/example/racunanjekilaze/ui/components/ — reusable Compose komponente.
app/src/test/ — unit testovi za kalkulator i modele.

## Key Files
settings.gradle.kts — definiše Gradle root `racunanje-kilaze`.
app/build.gradle.kts — Compose Android konfiguracija i namespace `com.example.racunanjekilaze`.
app/src/main/AndroidManifest.xml — Application i launcher aktivnost.
app/src/main/java/com/example/racunanjekilaze/MainActivity.kt — ulaz u Compose UI.
app/src/main/java/com/example/racunanjekilaze/KilazaApplication.kt — Application i crash log setup.
app/src/main/java/com/example/racunanjekilaze/data/Calculator.kt — proračun dužine, težine, gustine i parsiranje unosa.
app/src/main/java/com/example/racunanjekilaze/data/Models.kt — `RollDimensions`, `RollResult`, `CalculationResult`, `OrderEntry`.
app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreen.kt — glavni ekran kalkulatora.
app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreenState.kt — UI state i interakcije.
app/src/test/java/com/example/racunanjekilaze/data/CalculatorTest.kt — regresiona zaštita za parser i proračun.

## Critical Constraints
- Poštovati parent `AGENTS.md`: lokalno bez Git komandi.
- Poslovna matematika treba da ostane u `data/Calculator.kt`, a ne u Compose sekcijama.
- Validacija dimenzija i parser decimalnog zareza su pokriveni testovima; pokrenuti relevantne unit testove kod promena kalkulacije.
- Nazivi i poruke su korisnički vidljivi; dodavati srpsku latinicu sa dijakritikom.

## Hot Files
app/src/main/java/com/example/racunanjekilaze/data/Calculator.kt, app/src/main/java/com/example/racunanjekilaze/data/Models.kt, app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreen.kt, app/src/main/java/com/example/racunanjekilaze/ui/screens/CalculatorScreenState.kt
