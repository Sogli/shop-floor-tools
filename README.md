# Shop-floor tools: softver nastao iz proizvodnje / software built from hands-on production work

[![Portfolio CI](https://github.com/Sogli/shop-floor-tools/actions/workflows/portfolio-ci.yml/badge.svg)](https://github.com/Sogli/shop-floor-tools/actions/workflows/portfolio-ci.yml)

---

# 🇷🇸 Srpski

Radim neposredno na mašinama za obradu i doradu bakra. Ponavljajuća podešavanja, ručne konverzije i praćenje proizvodnje trošili su vreme i ostavljali prostor za greške, pa sam napravio alate koje sam želeo da imam u pogonu.

Ovaj repozitorijum je rezultat: zbirka praktičnih Python i Android aplikacija koje svakodnevni rad čine bržim, doslednijim i lakšim za proveru. Ujedno je i najjasniji primer kako pristupam inženjerstvu: krenem od stvarnog problema, modelujem ograničenja, isporučim nešto upotrebljivo, pa to popravljam kroz povratne informacije i granične slučajeve.

Repozitorijum ne sadrži proizvodne zapise, podatke o zaposlenima, dokumenta firme, kredencijale ni ključeve za potpisivanje.

## Spisak programa

### Python

| Program | Čemu služi |
|---|---|
| `python/cutting-optimizer` | Optimizuje podešavanje rezanja i uzdužnog sečenja trake tako što bira raspored noževa i odstojnika uz poštovanje raspoloživog alata. |

### Android

| Program | Čemu služi |
|---|---|
| `android/metraza` | Računa dužinu i metražu koluta iz dimenzija i materijala. |
| `android/kilaza` | Pretvara dimenzije koluta u masu i grupiše ih po porudžbinama. |
| `android/coil-diameter` | Procenjuje spoljni prečnik koluta iz unutrašnjeg prečnika, dužine i debljine. |
| `android/pallet-packing` | Raspoređuje kolutove po paletama po zadatom broju komada. |
| `android/pallet-weight-packing` | Pakuje palete do ciljne mase, uz različite materijale i gustine. |
| `android/equipment-tracking` | Vodi evidenciju zaduženog alata i opreme: rokovi, istorija i dostupnost. |
| `android/livnica-shifts` | Rani prototip za smene, praznike i obračun zarade (istorijski, prethodnik projekta `shift-payroll-android`). |

## Izdvojeni projekat: optimizator rezanja

`python/cutting-optimizer/m39.py` je terminalna aplikacija za optimizaciju podešavanja rezanja i uzdužnog sečenja.

- Modeluje ograničenja zaliha noževa, odstojnika, osovina i guma.
- Koristi mešovito celobrojno linearno programiranje kroz PuLP i HiGHS.
- Traži rezervne strategije uz očuvanje konzistentnosti zaliha.
- Koristi `Decimal` kvantizaciju za fizičke dimenzije.
- Razbija simetrične prostore rešenja da bi skratio rad solvera; referentni scenario je pao sa oko 39 na oko 21 sekundu bez promene izabranog podešavanja.
- Ispisuje objašnjenje na srpskom, razumljivo operateru: izabrani raspored, rezervne varijante i korekcije balansa.

```mermaid
flowchart LR
    A["Dimenzije i raspoloživ alat"] --> B["Validacija i kvantizacija"]
    B --> C["Izgradnja MILP modela"]
    C --> D["HiGHS solver"]
    D --> E["Provera fizičke izvodljivosti"]
    E --> F["Balansiran raspored rezanja"]
    F --> G["Rezultat čitljiv operateru"]
```

Pokretanje:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r python/cutting-optimizer/requirements.txt
python python/cutting-optimizer/m39.py
```

Svaki Android folder je nezavisan Android Studio projekat sa sopstvenim Gradle wrapper-om:

```bash
cd android/metraza
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Šta ovo pokazuje

- Prevođenje znanja iz struke u eksplicitne modele i ograničenja.
- Razvoj za čoveka koji obavlja posao: srpska terminologija i brz unos.
- Put od malih prototipova do održivih Android aplikacija.
- Validacija, objašnjivost i regresioni testovi kao deo funkcionalnosti, a ne dodatak.
- Korišćenje AI agenata kao saradnika, uz proveru rezultata na stvarnim proizvodnim slučajevima.

Formule i primeri u ovom javnom portfoliju su generički ili sanirani. Nisu objava vlasničke procesne dokumentacije.

---

# 🇬🇧 English

I work hands-on with copper-processing and finishing machinery. Repetitive setup calculations, handwritten conversions and operational tracking created avoidable time loss and room for error, so I built the tools I wanted to have on the shop floor.

This repository is the result: a collection of practical Python and Android applications created to make daily production work faster, more consistent and easier to verify. It is also the clearest example of how I approach engineering: start from a real problem, model the constraints, ship something usable, then improve it from feedback and edge cases.

No production records, employee data, company documents, credentials or signing keys are included.

## Program list

### Python

| Program | What it does |
|---|---|
| `python/cutting-optimizer` | Optimizes cutting and slitting setups by picking the knife and spacer arrangement that fits the available tooling. |

### Android

| Program | What it does |
|---|---|
| `android/metraza` | Calculates coil length and running metres from dimensions and material. |
| `android/kilaza` | Converts coil dimensions into mass and groups them by order. |
| `android/coil-diameter` | Estimates external coil diameter from inner diameter, length and thickness. |
| `android/pallet-packing` | Distributes coils across pallets by piece count. |
| `android/pallet-weight-packing` | Packs pallets to a target mass across materials with different densities. |
| `android/equipment-tracking` | Tracks issued tools and equipment: deadlines, history and availability. |
| `android/livnica-shifts` | Early prototype for shift cycles, holidays and pay calculation (historical predecessor of `shift-payroll-android`). |

## Featured project: cutting setup optimizer

`python/cutting-optimizer/m39.py` is a terminal application for cutting and slitting setup optimization.

- Models knife, spacer, axle and tire inventory constraints.
- Uses mixed-integer linear programming through PuLP and HiGHS.
- Searches fallback strategies while preserving inventory consistency.
- Uses Decimal-based quantization for physical dimensions.
- Breaks symmetric solution spaces to reduce solver time; a reference scenario improved from about 39 seconds to about 21 seconds without changing the selected setup.
- Produces a Serbian operator-facing explanation of the selected arrangement, fallbacks and balance offsets.

```mermaid
flowchart LR
    A["Dimensions and available inventory"] --> B["Validation and quantization"]
    B --> C["MILP model builder"]
    C --> D["HiGHS solver"]
    D --> E["Physical feasibility checks"]
    E --> F["Balanced cutting arrangement"]
    F --> G["Operator-readable result"]
```

Run it with:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r python/cutting-optimizer/requirements.txt
python python/cutting-optimizer/m39.py
```

Each Android folder is an independent Android Studio project with its own Gradle wrapper:

```bash
cd android/metraza
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## What this demonstrates

- Translating domain knowledge into explicit models and constraints.
- Building for the person performing the work, including Serbian terminology and fast input flows.
- Moving from small prototypes to maintainable Android applications.
- Treating validation, explainability and regression tests as part of the feature.
- Using AI coding agents as collaborators while verifying output against real operational cases.

The formulas and examples in this public portfolio are generic or sanitized. They are not a release of proprietary process documentation.
