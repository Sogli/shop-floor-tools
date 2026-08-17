# Project Map
_Generated: 2026-05-12 22:44 | Staleness: timestamps_

## Directory Structure
app/ — Android Compose kalkulator spoljašnjeg prečnika kotura.
app/src/main/java/com/precnik/ui/screens/ — glavni kalkulator ekran.
app/src/main/java/com/precnik/ui/components/ — Compose dugmad, kartice i input komponente.
app/src/main/java/com/precnik/ui/theme/ — tema, boje i responsive layout tokeni.
app/src/main/res/ — logo, teme, boje, dimenzije i stringovi.

## Key Files
settings.gradle — Groovy Gradle settings za root `Precnik`.
build.gradle — Groovy root Gradle konfiguracija sa AGP/Kotlin/Compose pluginovima.
app/build.gradle — Groovy Android Compose konfiguracija i namespace `com.precnik`.
app/src/main/AndroidManifest.xml — Application i launcher aktivnost.
app/src/main/java/com/precnik/MainActivity.kt — ulaz u Compose UI.
app/src/main/java/com/precnik/CrashLoggingApplication.kt — Application i crash log setup.
app/src/main/java/com/precnik/ui/screens/CalculatorScreen.kt — ceo kalkulator: unos unutrašnjeg prečnika, dužine, debljine i formula za spoljašnji prečnik.
app/src/main/java/com/precnik/ui/theme/AppConstants.kt — responsive layout tokeni.
keystore.properties — lokalna release konfiguracija; ne dirati osim ako je zadatak baš signing.
precnik-release.jks — lokalni keystore artefakt; ne kopirati i ne menjati bez eksplicitnog zahteva.

## Critical Constraints
- Poštovati parent `AGENTS.md`: ne pokretati Git komande.
- Ovaj projekat koristi Groovy Gradle fajlove, za razliku od većine Kotlin DSL projekata u workspace-u.
- Matematička formula je trenutno u `CalculatorScreen.kt`; za veće promene prvo izdvojiti ili testirati poslovnu logiku.
- Signing fajlovi su osetljivi lokalni artefakti i ne treba ih pomerati, brisati ili uključivati u bilo kakav backup tok.

## Hot Files
app/src/main/java/com/precnik/ui/screens/CalculatorScreen.kt, app/src/main/java/com/precnik/ui/components/LabeledTextField.kt, app/src/main/java/com/precnik/ui/theme/AppConstants.kt, app/build.gradle
