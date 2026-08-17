# Project Map
_Generated: 2026-05-12 22:44 | Staleness: timestamps_

## Directory Structure
app/ — Android Compose kalkulator metraže, rolni i materijala.
app/src/main/java/com/metraza/data/ — kalkulacije, modeli i validacija inputa.
app/src/main/java/com/metraza/ui/screens/ — glavni Compose kalkulator.
app/src/main/java/com/metraza/ui/components/ — dugmad, kartice, text fields i material selector.
app/src/test/ — unit testovi za kalkulaciju, modele i validaciju.
app/src/androidTest/ — UI testovi osnovnog toka kalkulatora.
.planning/ — lokalna mapa/roadmap/state artefakti za ranije planiranje.

## Key Files
settings.gradle.kts — definiše Gradle root `Metraza`.
app/build.gradle.kts — Compose Android konfiguracija i namespace `com.metraza`.
app/src/main/AndroidManifest.xml — Application i launcher aktivnost.
app/src/main/java/com/metraza/MainActivity.kt — ulaz u Compose UI.
app/src/main/java/com/metraza/data/Calculator.kt — proračun rezultata, gustina, parseri i formatiranje.
app/src/main/java/com/metraza/data/Models.kt — input/result modeli i message state.
app/src/main/java/com/metraza/data/Validation.kt — validacija svih polja kalkulatora.
app/src/main/java/com/metraza/ui/screens/CalculatorScreen.kt — glavni ekran i korisnički tok.
app/src/test/java/com/metraza/data/CalculatorTest.kt — regresiona zaštita poslovne matematike.
app/src/androidTest/java/com/metraza/ui/CalculatorScreenTest.kt — UI smoke/regresioni testovi.
metraza.py — stariji Python/Kivy prototip ili pomoćna desktop verzija.

## Critical Constraints
- Poštovati parent `AGENTS.md`: bez Git komandi, čak i ako postoji `.git` folder.
- Kalkulacija i validacija su već izdvojene u `data/`; UI ne treba da uvodi paralelnu poslovnu logiku.
- Promene materijala/gustina moraju proći kroz testove modela i kalkulatora.
- `.planning/` je pomoćni kontekst; izvor istine za runtime je `app/src/main`.

## Hot Files
app/src/main/java/com/metraza/data/Calculator.kt, app/src/main/java/com/metraza/data/Models.kt, app/src/main/java/com/metraza/data/Validation.kt, app/src/main/java/com/metraza/ui/screens/CalculatorScreen.kt
