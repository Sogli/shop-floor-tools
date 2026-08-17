# Project Map
_Generated: 2026-05-12 22:44 | Staleness: timestamps_

## Directory Structure
app/ — Android XML/AppCompat kalkulator pakovanja kotura na palete.
app/src/main/java/com/pakovanje/ — `MainActivity`, crash logging i TextView helperi.
app/src/main/res/layout/ — XML layout za glavni ekran i karticu rezultata.
app/src/main/res/values/ — stringovi, dimenzije, boje, stilovi i array vrednosti paleta.

## Key Files
settings.gradle.kts — definiše Gradle root `Pakovanje`.
app/build.gradle.kts — Android AppCompat/Material konfiguracija bez Compose-a.
app/src/main/AndroidManifest.xml — Application i launcher aktivnost.
app/src/main/java/com/pakovanje/MainActivity.kt — celokupna UI kontrola, parsiranje, validacija i raspodela kotura po paletama.
app/src/main/java/com/pakovanje/util/TextViewExtensions.kt — responsive TextView helperi za XML UI.
app/src/main/res/layout/activity_main.xml — glavni formular.
app/src/main/res/layout/item_pallet_result.xml — prikaz pojedinačne palete.
app/src/main/res/values/arrays.xml — opcije dimenzija paleta.
pakovanje.py — Python/Kivy desktop prototip.

## Critical Constraints
- Poštovati parent `AGENTS.md`: lokalno bez Git komandi.
- Ovaj projekat nije Compose; pratiti XML/AppCompat stil i ne uvoditi Compose bez posebnog zahteva.
- Poslovna logika je trenutno u `MainActivity.kt`; za veće izmene prvo zaključati ponašanje testovima ili izdvojiti pažljivo.
- Validacione poruke su korisnički vidljive i treba ih pisati srpskom latinicom sa dijakritikom pri budućim izmenama.

## Hot Files
app/src/main/java/com/pakovanje/MainActivity.kt, app/src/main/res/layout/activity_main.xml, app/src/main/res/layout/item_pallet_result.xml, app/src/main/res/values/arrays.xml
