# Project Map
_Generated: 2026-05-12 22:44 | Staleness: timestamps_

## Directory Structure
app/ — Android Compose aplikacija za smene, brigadne rasporede, zaradu, praznike i auto-popunu.
app/src/main/java/com/livnica/ — glavni domain, UI, kalkulacije, repozitorijum i teme.
app/src/main/java/com/livnica/ui/components/ — izdvojene Compose komponente za forme, indikatore, ikonice i responsive tekst.
app/src/main/assets/ — inicijalni JSON podaci za smene.
app/src/test/ — unit testovi za praznike, obračun, repozitorijum i scheduler.

## Key Files
settings.gradle.kts — Gradle root je trenutno `Rad`, iako je folder/projekat `livnica`.
app/build.gradle.kts — Compose Android konfiguracija sa Detekt pluginom.
detekt.yml — statička analiza za Kotlin.
app/src/main/AndroidManifest.xml — Application i launcher aktivnost.
app/src/main/java/com/livnica/MainActivity.kt — ulaz u aplikaciju.
app/src/main/java/com/livnica/LivnicaApplication.kt — Application inicijalizacija.
app/src/main/java/com/livnica/ShiftRepository.kt — persistence, import/export i pristup evidenciji dana.
app/src/main/java/com/livnica/PayCalculator.kt — obračun dnevne i mesečne zarade.
app/src/main/java/com/livnica/ShiftScheduler.kt — generisanje smena, bolovanja i godišnjeg.
app/src/main/java/com/livnica/ShiftTrackerScreen.kt — glavni Compose tok aplikacije.
app/src/main/java/com/livnica/AutoFillService.kt — auto-popuna iz gate session podataka.
app/src/test/java/com/livnica/PayCalculatorTest.kt — osnovna regresiona zaštita obračuna.

## Critical Constraints
- Poštovati parent `AGENTS.md`: ne koristiti Git komande u ovom workspace-u.
- Zbog sličnog domena kao `rad`, ne pretpostavljati da su pravila identična; proveriti konkretne `livnica` modele i testove.
- `ShiftRepository`, `PayCalculator` i `ShiftScheduler` čine isti poslovni tok; izmene obračuna obično traže testove za sva tri sloja.
- Detekt postoji u projektu; za strukturne Kotlin izmene proveriti i statičku analizu kada je relevantno.

## Hot Files
app/src/main/java/com/livnica/ShiftRepository.kt, app/src/main/java/com/livnica/PayCalculator.kt, app/src/main/java/com/livnica/ShiftScheduler.kt, app/src/main/java/com/livnica/ShiftTrackerScreen.kt, app/src/main/java/com/livnica/Models.kt
