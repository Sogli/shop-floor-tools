# Shop-floor tools - software built from hands-on production work

[![Portfolio CI](https://github.com/Sogli/shop-floor-tools/actions/workflows/portfolio-ci.yml/badge.svg)](https://github.com/Sogli/shop-floor-tools/actions/workflows/portfolio-ci.yml)

I work hands-on with copper-processing and finishing machinery. Repetitive setup calculations, handwritten conversions and operational tracking created avoidable time loss and room for error, so I built the tools I wanted to have on the shop floor.

This repository is the result: a collection of practical Python and Android applications created to make daily production work faster, more consistent and easier to verify. It is also the clearest example of how I approach engineering - start from a real problem, model the constraints, ship something usable, then improve it from feedback and edge cases.

No production records, employee data, company documents, credentials or signing keys are included.

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

## Android tools

| Project | Problem it removes | Engineering focus |
|---|---|---|
| `android/metraza` | Repeated coil length, roll and material calculations | Compose UI, isolated calculation/validation layer, unit and UI tests |
| `android/kilaza` | Converting coil dimensions between length, mass and grouped orders | Typed models, decimal-input handling, saved UI state, regression tests |
| `android/coil-diameter` | Estimating external coil diameter from inner diameter, length and thickness | Focused calculator, responsive Compose interface |
| `android/pallet-packing` | Distributing coils across pallets | Input validation, deterministic allocation, XML/AppCompat UI |
| `android/pallet-weight-packing` | Packing against target pallet mass across multiple materials | Density-aware calculations and target-weight allocation |
| `android/equipment-tracking` | Tracking issued items, deadlines, history and availability | Room, JSON migration, notifications, Google Drive backup |
| `android/livnica-shifts` | Shift cycles, holidays and repeated pay calculations | Scheduling, persistence, payroll rules, tests and Detekt |

Each folder is an independent Android Studio project with its own Gradle wrapper. For example:

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

