#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cutting Optimization System - Refactored Version 2.10.0 (m39)

A manufacturing optimization tool for cutting/slitting operations.
Uses linear programming to optimize knife and gap combinations.

Changes in 2.10.0 (m39) — čišćenje i preostale stavke iz audita:
1. Kalkulator zazora UVEZAN u tok aplikacije: na promptu za zazor Enter
   otvara kalkulator (materijal → kvalitet → debljina → predlog zazora
   zaokružen na 0.05, uz mogućnost prihvatanja ili ručne korekcije).
   calc_clearance / QUALITY_GRADES / MATERIAL_LIST / round_to_nearest više
   nisu mrtav kod; UserInputs.material/quality/thickness se popunjavaju i
   prikazuju u panelu "Parametri zazora".
2. Uklonjen mrtav kod:
   - transaction() context manager (nikad korišćen; rollback po dimenziji
     bi menjao ponašanje pa se ne uvodi bez odluke o domenu),
   - cela allow_zeros mašinerija (Big-M LinkZero ograničenja, is_zero
     promenljive, zero_penalty težina, FixZeroCount) — obe strategije su
     uvek koristile allow_zeros=False,
   - InputHandler.get_user_inputs() (dupliralo run() tok),
   - LPSolver.solve() (koristio se samo try_solve) i sa njim
     OptimizationError/NoSolutionFoundError izuzeci,
   - LPSolution.variables (računato, nikad čitano),
   - import rich.progress.Progress, neiskorišćena lokalna promenljiva
     clearance u calculate(), redundantna nulta provera u solver_int.
3. NarrowCutResult.dimension preimenovan u gap_total — polje je uvek
   sadržalo zbir biksni, ne dimenziju.
4. _select_balanced_tire_setup: TireChoice NamedTuple umesto 7-torke sa
   magičnim indeksima; namerna asimetrija kriterijuma po granama sada je
   eksplicitno dokumentovana.
5. get_manual_tire_adjustments: default predlozi prate potrošnju guma
   prethodnih dimenzija u istoj sesiji (lokalno stanje se umanjuje).
6. Performanse (mereno na istoj scenariji: 39s → ~21s):
   - RAZBIJANJE SIMETRIJE u split LP-u: podele su međusobno zamenljive, pa
     se nameće neopadajući redosled split promenljivih — skup dostižnih
     multiskupova podela je nepromenjen (fizički raspored ionako određuje
     best_balance_arrangement), a faza B više ne udara u time limit od 5s.
     Rezultati su verifikovano identični (iste podele, isti broj biksni,
     isti offset).
   - Simulacija balansa kandidata ide bez eskalacije tolerancije (pravi
     rez i dalje eskalira do max_balance_tolerance).
7. calculate_splits default step_size usklađen sa CalculatorConfig (0.01).
8. get_positive_int dobio gornju granicu (default 1000) — štiti od unosa
   apsurdnog broja prolaza/dimenzija.
9. "Automatski balans izbor" se prikazuje kao info (plavo), ne kao
   upozorenje — žuto ostaje za odstupanja (fallback veličine, tolerancije).
10. Docstring determine_tire_setup_with_fallback usklađen sa stvarnim
    ponašanjem: prva veličina ima apsolutni prioritet, među preostalima
    odlučuje najmanje odstupanje od target_split (ne strogi redosled).

Changes in 2.9.0 (m38) — ispravke bagova iz audita:
1. HiGHS opcije prebačene iz 'options' liste u kwargs. Empirijski potvrđeno
   (PuLP 3.3.2): pulp.HiGHS čita SAMO optionsDict (kwargs) i imenovane
   parametre — 'options' lista se kod highspy interfejsa u potpunosti ignoriše.
   mip_max_nodes=50000 i simplex_strategy=1 su tek sada ZAISTA aktivni.
   log_to_console uklonjen (msg=False već postavlja output_flag=False).
2. mip_abs_gap=0.5 se postavlja SAMO za probleme sa celobrojnim ciljem
   (GapCombination, Split faza 1, Axle). Za Split fazu 2 (kontinualne
   devijacije) i NarrowCut (koeficijenti 0.1/0.01) abs_gap 0.5 je nekorektan
   — dozvolio bi rešenje do 0.5 lošije od optimalnog. Uvedena dva solver
   profila (integral/continuous); keš solvera ključan po (time_limit, profil).
3. Unos noževa: uklonjen mrtvi replace(',', '.') POSLE split(',') koji je
   lažno sugerisao podršku za decimalni zarez. Separatori su sada zarez,
   tačka-zarez i razmak; decimalne veličine se unose tačkom (npr. 4.5), što
   prompt eksplicitno kaže. Dodat eho izabranih noževa radi vidljive potvrde
   interpretacije unosa.
4. _try_split: parcijalno rešenje (manje podela od traženog) se više ne
   tretira kao uspeh — validira se len(results) == num_splits, a commit
   biksni se radi tek POSLE validacije (ranije su se biksne trošile i na
   odbačena parcijalna rešenja).
5. Poruka "Korišćenje malih biksni..." se dodaje samo kada ta strategija
   stvarno da rezultat (ranije se ispisivala i kad strategija propadne).
6. Balans: LP je simetričan po indeksima podela pa je redosled u rešenju
   proizvoljan. Offset se sada računa nad NAJBOLJIM rasporedom podela
   (sve permutacije za <=7 podela uz deduplikaciju, heuristike iznad), a
   stvarni široki rez se preuređuje u taj raspored da prikaz odgovara
   offsetu iz automatskog izbora (best_balance_arrangement).
7. Poruka fallback-a guma koristi fallback_order[0] umesto hardkodovanog
   "20mm" (poruka više ne laže ako se config promeni).
8. Relaksirani izbor gume zahteva avg_split >= najmanja dostupna biksna
   (relaxed_floor, default 0.1) — ranije je prihvatao i podele od npr.
   0.03mm koje su nizvodno neizvodljive.

Changes in 2.8.1:
- Ispravljen lažni izveštaj "Dimenzija X nije pogodna za standardne gume" za male
  dimenzije (npr. 20mm) koje su ispod standardnog opsega podela (min_split..max_split),
  a koje se zapravo USPESNO seku nizvodno (calculate_splits koristi balance_tolerance,
  ne prag min_split).
  * evaluate_tire_setup_for_size dobija parametar enforce_min_split (default True);
    kada je False, donja granica podele se ne primenjuje (samo avg_split > 0).
  * determine_tire_setup_with_fallback sada, pre vraćanja greške, radi relaksirani
    prolaz bez donje granice i bira veličinu po najmanjem odstupanju od target_split.
    Mala dimenzija tako dobija upotrebljivu gumu sa is_error=False i ispravnom porukom
    ("...sa uskim podelama (~Xmm)..."), umesto lažne greške i slučajnog 10mm fallback-a.
  * Izbor gume ostaje konzistentan sa onim što se realno koristi (npr. dim 20 -> 1x10mm).

Changes in 2.8.0:
- HiGHS solver tuning: mip_abs_gap=0.5 (najveći dobitak — sve vrednosti ciljne
  funkcije su celi brojevi, pa abs_gap 0.5 garantuje pronalazak optimalnog rešenja
  bez nepotrebne pretrage B&B stabla; može skratiti vreme rešavanja 50–80%)
- threads smanjen sa 0 (svi jezgri) na cpu_count()//2 — za male MIP probleme
  (<50 promenljivih) previše niti donosi koordinacioni overhead, ne ubrzanje
- mip_rel_gap smanjen sa 0.001 na 0.0005 (neznatno tesniji, ali sigurniji za
  tačnost celih vrednosti)
- Dodat mip_max_nodes=50000 — sigurnosni limit B&B čvorova (sprečava runaway)
- Dodat simplex_strategy=1 (dual simplex) — brži za root LP relaxaciju u MIP-u
- log_to_console eksplicitno postavljeno na "false" — eliminisanje slučajnih
  ispisa u nekim verzijama HiGHS-a
- TypeError fallback za stariji PuLP poboljšan: prenosi sve parametre kao opcije
- Dodat import os na vrhu (potreban za cpu_count())
- Napomena: pulp.HiGHS (ne HiGHS_CMD) već koristi highspy in-memory API —
  nema LP fajlova na disku, pa su ove izmene parametara pun kapacitet optimizacije

Changes in 2.7.1:
- HiGHS solver: added threads=0 (all cores), mip_rel_gap=0.001 (0.1% MIP gap),
  presolve="on" for faster solving; graceful TypeError fallback for older PuLP;
  HiGHS_CMD fallback also receives mip_rel_gap and presolve via options list

Changes in 2.7.0:
- Removed dead `num_splits == 0` branch in _calculate_wide_cut (was unreachable)
- Replaced fragile TireSetupResult.is_valid string-matching with explicit is_error field
- Removed unreachable _run_knife_comparison and wired it into Application.run() flow
- Collapsed duplicate lower_axle/upper_axle fields in CalculationResult to axle_base
- Removed fragile __iter__ from SplitResult and NarrowCutResult; use named attributes
- calculate_narrow_cut now returns NarrowCutResult directly (was a bare 4-tuple)
- SplitStrategy dict replaced with a typed frozen dataclass
- Simplified redundant post-quantize zero check in solver_int
- Extracted _sanitize_lp_name() to DRY up make_var_name / make_gap_var_name
- Added SplitStrategy to __all__

Changes in 2.6.5:
- Full code audit: fixed tire allocation consuming inventory before validation
- Fixed tire allocation continuing on failure (now aborts dimension)
- Fixed round_to_nearest using pure Decimal arithmetic (no float roundtrip)
- Fixed strategy dict mutation side-effect in split calculation
- Fixed cached_property on mutable InventoryConfig (replaced with property)
- Removed unused Result[T] generic class and dead imports (weakref, etc.)
- Fixed QUALITY_LIST tuple order for clarity (code, label, FactorRange)
- Fixed calc_clearance docstring referencing non-existent R200
- Added __all__ export list
- Extended Serbian ORDINAL_MAP to 20
- Improved code comments and documentation accuracy
- Cleaned up formatting inconsistencies

Changes in 2.6.4:
- Dodat R360(TT) – oprugasto tvrdo stanje, faktor zazora 12-15%

Changes in 2.6.3:
- Korigovani faktori zazora na osnovu praktičnih uputstava i deep research
- R220: 5-7%, R240: 7-9%, R250: 8-10%, R260: 9-11%, R290: 10-12%, R360: 12-15%

Changes in 2.6.2:
- Prilagođeni faktori zazora za SLITTING operacije
- Uklonjen dodatak za mesing

Changes in 2.6.1:
- Pojednostavljen prikaz širokih rezova (Guma/Gume + Prva/Druga/Treća podela)
- Pojednostavljen prikaz uskih rezova (samo Noževi + Kombinacija biksni)
- Uklonjeni debug podaci iz prikaza

Changes in 2.6:
- Added automatic tire size selection with fallback logic
- Tire inventory tracking (10mm, 15mm, 20mm with quantities)
- Automatic fallback when default tire size (20mm) doesn't fit:
  20mm -> 15mm -> 10mm
- Tire usage tracking and reporting
"""

from __future__ import annotations

__all__ = [
    # Core configuration
    'AppConfig', 'DEFAULT_CONFIG', 'SolverConfig', 'TireConfig',
    'CalculatorConfig', 'InventoryConfig', 'LPWeights', 'DEFAULT_SELECTED_KNIVES', 'AXLE_ALLOWED_KNIVES',
    # Clearance
    'FactorRange', 'ClearanceResult', 'calc_clearance', 'round_to_nearest',
    'QUALITY_GRADES', 'QUALITY_LIST', 'MATERIAL_LIST',
    # Domain models
    'UserInputs', 'DimensionInput', 'DimensionResult', 'CalculationResult',
    'SplitResult', 'NarrowCutResult', 'SplitStrategy', 'ClearanceSetup',
    # Calculator & Solver
    'CuttingCalculator', 'LPSolver', 'InventoryManager', 'InventoryState',
    # Exceptions
    'ValidationError',
    # Application
    'Application', 'InputHandler', 'ResultPresenter',
    # Tire utilities
    'determine_tire_setup', 'determine_tire_setup_with_fallback', 'TireSetupResult',
    'best_balance_arrangement',
]

import logging
import os
from collections import Counter
from itertools import permutations
from functools import lru_cache
from decimal import Decimal, getcontext, ROUND_HALF_UP, ROUND_DOWN, InvalidOperation
from dataclasses import dataclass, field
from typing import (
    List, Tuple, Dict, Optional, FrozenSet, Iterable, NamedTuple
)
from enum import Enum, auto
import pulp
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich import box

# Configure logging
logging.basicConfig(level=logging.WARNING)
logger = logging.getLogger(__name__)

# =============================================================================
# CONSTANTS
# =============================================================================

DECIMAL_PRECISION = 10
getcontext().prec = DECIMAL_PRECISION

# Rounding step for clearance (matches available gaps: 0.1, 0.15, 0.2, 0.25)
CLEARANCE_ROUNDING_STEP = Decimal('0.05')


# =============================================================================
# CLEARANCE CALCULATION
# =============================================================================

@dataclass(frozen=True, slots=True)
class FactorRange:
    """Immutable factor range for clearance calculation."""
    min_factor: float
    max_factor: float

    def __post_init__(self) -> None:
        if self.min_factor < 0 or self.max_factor < 0:
            raise ValueError("Factors must be non-negative")
        if self.min_factor > self.max_factor:
            raise ValueError("min_factor cannot exceed max_factor")

    @property
    def ideal(self) -> float:
        return (self.min_factor + self.max_factor) / 2.0


class ClearanceResult(NamedTuple):
    """Result of clearance calculation."""
    min_gap: float
    ideal_gap: float
    max_gap: float


# Quality grades (R-grades) with their factor ranges - EN standard
# Faktori prilagođeni za slitting mašinu (na osnovu praktičnih uputstava)
# Veći R = tvrđe = veći faktor za zazor
QUALITY_GRADES: Dict[str, Tuple[str, FactorRange]] = {
    "R220": ("R220 – meko (žareno)", FactorRange(0.05, 0.07)),
    "R240": ("R240 – polutvrdo", FactorRange(0.07, 0.09)),
    "R250": ("R250 – polutvrdo+ (standard)", FactorRange(0.08, 0.10)),
    "R260": ("R260 – tvrdo", FactorRange(0.09, 0.11)),
    "R290": ("R290 – ekstra tvrdo", FactorRange(0.10, 0.12)),
    "R360": ("R360(TT) – oprugasto tvrdo", FactorRange(0.12, 0.15)),
}

# For backwards compatibility - list format: (code, label, FactorRange)
QUALITY_LIST: List[Tuple[str, str, FactorRange]] = [
    (code, label, fr) for code, (label, fr) in QUALITY_GRADES.items()
]

MATERIAL_LIST: Tuple[str, ...] = (
    "Cu-DHP", "Cu-PHC", "ED-Cu", "Cu-ETP", "DVP1Cu",
    "CuZn10", "CuZn15", "CuZn28", "CuZn30", "CuZn37",
)

BRASS_PREFIX = "CuZn"
# Za slitting nije potreban dodatak za mesing.
# Promeniti na vrednost > 0 da bi se aktivirala korekcija za mesing.
BRASS_FACTOR_ADJUSTMENT = 0.0

# Podrazumevani izbor noževa za unos (Enter prihvata ove vrednosti).
DEFAULT_SELECTED_KNIVES: Tuple[Decimal, ...] = (
    Decimal('6'), Decimal('7'), Decimal('8')
)

# Dozvoljeni noževi za poslednji (axle) izbor, nezavisno od početnog izbora.
AXLE_ALLOWED_KNIVES: Tuple[Decimal, ...] = (
    Decimal('6'), Decimal('7'), Decimal('8'), Decimal('10'), Decimal('15')
)


def calc_clearance(material: str, quality_code: str, thickness: float) -> ClearanceResult:
    """
    Calculate recommended clearance (min / ideal / max) in mm.

    Args:
        material: Material identifier (e.g., "Cu-DHP", "CuZn37")
        quality_code: R-grade code (e.g., "R220", "R290")
        thickness: Material thickness in mm (must be positive)

    Returns:
        ClearanceResult with min, ideal, and max clearance values

    Raises:
        ValueError: If quality_code is unknown or thickness is non-positive
    """
    if thickness <= 0:
        raise ValueError(f"Thickness must be positive, got: {thickness}")

    if quality_code not in QUALITY_GRADES:
        raise ValueError(f"Unknown R grade: {quality_code}. Valid codes: {list(QUALITY_GRADES.keys())}")

    _, base_range = QUALITY_GRADES[quality_code]

    # Brass (CuZnXX) gets larger clearance
    is_brass = material.startswith(BRASS_PREFIX)
    adjustment = BRASS_FACTOR_ADJUSTMENT if is_brass else 0.0

    min_f = base_range.min_factor + adjustment
    max_f = base_range.max_factor + adjustment
    ideal_f = (min_f + max_f) / 2.0

    return ClearanceResult(
        min_gap=round(thickness * min_f, 3),
        ideal_gap=round(thickness * ideal_f, 3),
        max_gap=round(thickness * max_f, 3),
    )


def round_to_nearest(value: float, step: Decimal = CLEARANCE_ROUNDING_STEP) -> Decimal:
    """
    Round a value to the nearest step using pure Decimal arithmetic.

    Args:
        value: The value to round
        step: The rounding step (default: 0.05)

    Returns:
        Decimal rounded to the nearest step
    """
    d = Decimal(str(value))
    return (d / step).quantize(Decimal('1'), rounding=ROUND_HALF_UP) * step


# =============================================================================
# CONFIGURATION
# =============================================================================

@dataclass(frozen=True, slots=True)
class LPWeights:
    """Weights used in LP objective functions.

    v2.10.0: uklonjen zero_penalty — pripadao je mrtvoj allow_zeros mašineriji.
    """
    secondary_objective: float = 0.01
    gap_usage_penalty: float = 0.1
    minimum_split: Decimal = field(default_factory=lambda: Decimal('0.001'))


@dataclass(frozen=True, slots=True)
class SolverConfig:
    """Solver-related configuration parameters."""
    tolerance: Decimal = field(default_factory=lambda: Decimal('0.0001'))
    large_m: int = 1000
    rounding_tolerance: Decimal = field(default_factory=lambda: Decimal('1e-4'))
    time_limit: int = 5
    weights: LPWeights = field(default_factory=LPWeights)


@dataclass(frozen=True, slots=True)
class TireConfig:
    """Tire setup configuration."""
    default_size: Decimal = field(default_factory=lambda: Decimal('20'))
    min_split: Decimal = field(default_factory=lambda: Decimal('15'))
    max_split: Decimal = field(default_factory=lambda: Decimal('30'))
    target_split: Decimal = field(default_factory=lambda: Decimal('22.5'))
    allowed_sizes: FrozenSet[Decimal] = field(
        default_factory=lambda: frozenset({Decimal('10'), Decimal('15'), Decimal('20')})
    )
    # Prioritet veličina za fallback (od najveće ka najmanjoj)
    fallback_order: Tuple[Decimal, ...] = field(
        default_factory=lambda: (Decimal('20'), Decimal('15'), Decimal('10'))
    )


@dataclass(frozen=True, slots=True)
class CalculatorConfig:
    """Calculator behavior configuration."""
    step_size: Decimal = field(default_factory=lambda: Decimal('0.01'))
    balance_tolerance: Decimal = field(default_factory=lambda: Decimal('2.0'))
    max_balance_tolerance: Decimal = field(default_factory=lambda: Decimal('10.0'))


@dataclass
class InventoryConfig:
    """Inventory configuration for knives, gaps, and tires."""
    knives: Dict[Decimal, int] = field(default_factory=lambda: {
        Decimal('4'): 8,
        Decimal('4.5'): 6,
        Decimal('5'): 6,
        Decimal('5.5'): 5,
        Decimal('6'): 16,
        Decimal('7'): 20,
        Decimal('8'): 10,
        Decimal('8.5'): 6,
        Decimal('10'): 12,
        Decimal('15'): 0,
    })

    small_gaps: Dict[Decimal, int] = field(default_factory=lambda: {
        Decimal('0.1'): 20, Decimal('0.15'): 20,
        Decimal('0.2'): 20, Decimal('0.25'): 20
    })

    initial_gaps: Dict[Decimal, int] = field(default_factory=lambda: {
        Decimal('2'): 11, Decimal('3.4'): 3, Decimal('3.5'): 10, Decimal('4'): 10,
        Decimal('4.3'): 10, Decimal('4.4'): 2, Decimal('4.6'): 6, Decimal('4.7'): 5, Decimal('5'): 10,
        Decimal('5.1'): 7, Decimal('5.2'): 5, Decimal('5.5'): 10, Decimal('5.6'): 8,
        Decimal('5.7'): 6, Decimal('5.8'): 6, Decimal('5.9'): 6, Decimal('6'): 10,
        Decimal('6.6'): 10, Decimal('7'): 10, Decimal('7.5'): 9, Decimal('8'): 9,
        Decimal('9'): 11, Decimal('10'): 5, Decimal('11'): 6, Decimal('12'): 5,
        Decimal('15'): 5, Decimal('20'): 6, Decimal('25'): 4, Decimal('30'): 6,
        Decimal('75'): 4, Decimal('100'): 4
    })

    # Inventar guma sa veličinama i količinama
    tires: Dict[Decimal, int] = field(default_factory=lambda: {
        Decimal('10'): 13,   # 10mm - 10 komada
        Decimal('15'): 20,   # 15mm - 20 komada
        Decimal('20'): 20    # 20mm - 20 komada
    })

    @property
    def available_knife_sizes(self) -> FrozenSet[Decimal]:
        """Recomputed on each access since knives dict is mutable."""
        return frozenset(self.knives.keys())


@dataclass
class AppConfig:
    """Main application configuration container."""
    solver: SolverConfig = field(default_factory=SolverConfig)
    tire: TireConfig = field(default_factory=TireConfig)
    calculator: CalculatorConfig = field(default_factory=CalculatorConfig)
    inventory: InventoryConfig = field(default_factory=InventoryConfig)
    base_numbers: Dict[int, int] = field(default_factory=lambda: {25: 476, 50: 451})
    valid_ratios: FrozenSet[int] = field(default_factory=lambda: frozenset({25, 50}))

    def get_adjusted_gaps(self, ratio: int) -> Dict[Decimal, int]:
        """Get gaps adjusted for the given ratio."""
        if ratio not in self.valid_ratios:
            raise ValueError(f"Invalid ratio: {ratio}. Must be one of {self.valid_ratios}")
        gaps = self.inventory.initial_gaps.copy()
        gaps[Decimal('25')] = 4 if ratio == 25 else 2
        return gaps


# Singleton default configuration
DEFAULT_CONFIG = AppConfig()


# =============================================================================
# EXCEPTIONS
# =============================================================================

# v2.10.0: uklonjeni OptimizationError i NoSolutionFoundError — bacani su samo
# iz LPSolver.solve() koji se nikad nije koristio (svuda se koristi try_solve).

class ValidationError(Exception):
    """Raised when input validation fails."""
    pass


# =============================================================================
# ENUMS
# =============================================================================

class GapUsageType(Enum):
    """Enumeration for gap usage categories."""
    SPLITS = auto()
    NARROW = auto()
    AXLE = auto()


# =============================================================================
# UTILITY FUNCTIONS
# =============================================================================

@lru_cache(maxsize=1024)
def decimal_to_float(value: Decimal) -> float:
    """Convert Decimal to float with caching."""
    return float(value)


def _sanitize_lp_name(value: Decimal) -> str:
    """Sanitize a Decimal value for use in an LP variable name."""
    return str(value).replace('.', '_').replace('-', 'n')


def solver_int(value: Optional[float], config: Optional[SolverConfig] = None) -> int:
    """Convert solver output to integer with proper rounding."""
    if value is None:
        return 0
    try:
        return int(Decimal(str(value)).quantize(Decimal('1'), rounding=ROUND_HALF_UP))
    except (InvalidOperation, ValueError):
        logger.warning(f"Failed to convert solver value {value} to int")
        return 0


def solver_decimal(value: Optional[float], step: Decimal = Decimal('0.1')) -> Decimal:
    """Convert solver output to Decimal with specified step size."""
    if value is None:
        return Decimal('0')
    try:
        return Decimal(str(value)).quantize(step, rounding=ROUND_HALF_UP)
    except (InvalidOperation, ValueError):
        logger.warning(f"Failed to convert solver value {value} to Decimal")
        return Decimal('0')


def make_var_name(prefix: str, value: Decimal) -> str:
    """Create a valid LP variable name."""
    return f"{prefix}_{_sanitize_lp_name(value)}"


def quantize_to_hundredths(value: Decimal) -> Decimal:
    """Quantize a Decimal value to two decimal places."""
    return value.quantize(Decimal('0.01'))


def make_gap_var_name(name: str, index: int, gap: Decimal) -> str:
    """Create a gap variable name for LP problems."""
    return f"{name}_gap_{index}_{_sanitize_lp_name(gap)}"


def compute_min_pair_length(knife_counts: Dict[Decimal, int]) -> Optional[Decimal]:
    """
    Compute minimum length achievable with a pair of knives.

    Uses O(n) algorithm to find the smallest sum of two knives.
    """
    usable = sorted(
        [(size, min(count, 2)) for size, count in knife_counts.items() if count > 0],
        key=lambda x: x[0]
    )
    if not usable:
        return None

    best: Optional[Decimal] = None

    # Best case: two smallest different knives
    if len(usable) >= 2:
        best = usable[0][0] + usable[1][0]

    # Check if doubling the smallest knife with count >= 2 is even better
    # (e.g., two 4mm knives = 8mm may be less than 4mm + 4.5mm = 8.5mm)
    # We only need to check the first eligible knife since usable is sorted ascending
    for size, count in usable:
        if count >= 2:
            candidate = size * 2
            if best is None or candidate < best:
                best = candidate
            break  # Only the smallest matters; larger doubles can't beat this

    return best


def calculate_balance_metrics(
    splits: List[SplitResult],
    num_tires: int,
    tire_size: Decimal
) -> Optional[Tuple[List[Decimal], Decimal, Decimal, Decimal]]:
    """
    Calculate tire centers and balance metrics from natural split order.

    Returns:
        Tuple (tire_centers, balance_point, ideal_balance, offset) or None.
    """
    expected_splits = num_tires + 1
    if num_tires <= 0 or len(splits) != expected_splits:
        return None

    tire_centers: List[Decimal] = []
    position = Decimal('0')
    half_tire = tire_size / Decimal('2')

    for idx, split in enumerate(splits, start=1):
        position += split.value
        if idx <= num_tires:
            tire_centers.append(position + half_tire)
            position += tire_size

    if not tire_centers:
        return None

    total_layout = position
    ideal_balance = total_layout / Decimal('2')
    balance_point = sum(tire_centers, Decimal('0')) / Decimal(len(tire_centers))
    offset = abs(balance_point - ideal_balance)
    return tire_centers, balance_point, ideal_balance, offset


def best_balance_arrangement(
    splits: List[SplitResult],
    num_tires: int,
    tire_size: Decimal,
    max_exhaustive: int = 7,
) -> Tuple[List[SplitResult], Optional[Tuple[List[Decimal], Decimal, Decimal, Decimal]]]:
    """
    Nađi raspored podela koji minimizuje balansni offset (v2.9.0).

    LP je potpuno simetričan po indeksima podela, pa je redosled u LP rešenju
    proizvoljan — a operater fizički slaže podele po želji. Zato se offset
    računa nad najboljim rasporedom, a ne nad slučajnim LP redosledom.

    Za <= max_exhaustive podela proverava sve permutacije (uz deduplikaciju
    rasporeda sa identičnim vrednostima); za više podela koristi heuristike
    (rastući, opadajući, "planina" i "dolina" raspored).

    Returns:
        Tuple (raspoređene_podele, metrics) gde je metrics rezultat
        calculate_balance_metrics za taj raspored (ili None ako nije primenjivo).
    """
    metrics = calculate_balance_metrics(splits, num_tires, tire_size)
    if num_tires <= 0 or len(splits) != num_tires + 1:
        return splits, metrics

    best_arr: List[SplitResult] = list(splits)
    best_metrics = metrics
    best_offset: Optional[Decimal] = metrics[3] if metrics else None

    if len(splits) <= max_exhaustive:
        seen: set = set()
        candidates: List[Tuple[SplitResult, ...]] = []
        for perm in permutations(splits):
            key = tuple(s.value for s in perm)
            if key in seen:
                continue
            seen.add(key)
            candidates.append(perm)
    else:
        asc = sorted(splits, key=lambda s: s.value)
        desc = list(reversed(asc))

        def _pyramid(ordered: List[SplitResult]) -> List[SplitResult]:
            left: List[SplitResult] = []
            right: List[SplitResult] = []
            for idx, s in enumerate(ordered):
                (left if idx % 2 == 0 else right).append(s)
            return left + right[::-1]

        candidates = [
            tuple(asc), tuple(desc),
            tuple(_pyramid(asc)), tuple(_pyramid(desc)),
        ]

    for arrangement in candidates:
        m = calculate_balance_metrics(list(arrangement), num_tires, tire_size)
        if m is None:
            continue
        if best_offset is None or m[3] < best_offset:
            best_offset = m[3]
            best_metrics = m
            best_arr = list(arrangement)

    return best_arr, best_metrics


def determine_tire_setup(
    dimension: Decimal,
    min_split: Decimal = Decimal('15'),
    max_split: Decimal = Decimal('30'),
    target_split: Decimal = Decimal('22.5'),
    tire_size: Decimal = Decimal('20')
) -> Tuple[int, Decimal, Optional[str]]:
    """Determine optimal tire setup for a given dimension (legacy function)."""
    if dimension < tire_size:
        return 1, tire_size, f"Dimenzija {dimension} je manja od pneumatika {tire_size}, potrebna ručna provera."

    n_max = int(dimension // tire_size)
    candidates: List[Tuple[int, Decimal]] = []

    for n in range(1, n_max + 1):
        remaining = dimension - n * tire_size
        if remaining <= 0:
            continue
        avg_split = remaining / Decimal(n + 1)
        candidates.append((n, avg_split))

    if not candidates:
        return 1, tire_size, f"Dimenzija {dimension} nije pogodna za standardni pneumatik {tire_size}."

    in_range = [(n, avg) for n, avg in candidates if min_split <= avg <= max_split]
    if in_range:
        best_n, _ = min(in_range, key=lambda x: abs(x[1] - target_split))
    else:
        best_n, _ = min(candidates, key=lambda x: abs(x[1] - target_split))

    return best_n, tire_size, None


@dataclass
class TireSetupResult:
    """Rezultat određivanja setup-a guma."""
    num_tires: int
    tire_size: Decimal
    message: Optional[str] = None
    avg_split: Optional[Decimal] = None
    # Explicit error flag; avoids fragile string inspection in is_valid.
    is_error: bool = False

    @property
    def is_valid(self) -> bool:
        """Proveri da li je rezultat validan (bez greške)."""
        return not self.is_error


def evaluate_tire_setup_for_size(
    dimension: Decimal,
    tire_size: Decimal,
    min_split: Decimal,
    max_split: Decimal,
    target_split: Decimal,
    enforce_min_split: bool = True,
    relaxed_floor: Decimal = Decimal('0.1'),
) -> Optional[Tuple[int, Decimal, Decimal]]:
    """
    Evaluiraj setup za određenu veličinu gume.

    Args:
        enforce_min_split: Ako je True (default), prosečna podela mora biti >= min_split.
            Ako je False, donja granica je relaxed_floor — koristi se u relaksiranom
            prolazu za male dimenzije koje su ispod standardnog opsega podela.
        relaxed_floor: Donja granica podele u relaksiranom režimu (v2.9.0).
            Default 0.1 = najmanja biksna; podela manja od najmanje biksne je
            nizvodno neizvodljiva (gap sum mora da bude jednak podeli), pa se
            takvi setup-ovi više ne predlažu.

    Returns:
        Tuple (broj_guma, prosečna_podela, odstupanje_od_cilja) ili None ako nije moguće.
    """
    if dimension < tire_size:
        return None

    n_max = int(dimension // tire_size)
    best_result: Optional[Tuple[int, Decimal, Decimal]] = None

    for n in range(1, n_max + 1):
        remaining = dimension - n * tire_size
        if remaining <= 0:
            continue

        avg_split = remaining / Decimal(n + 1)

        # Proveri da li je prosečna podela u dozvoljenom opsegu.
        # U relaksiranom režimu donja granica je relaxed_floor (najmanja biksna),
        # a ne min_split — v2.9.0: ranije je bilo samo avg_split > 0, što je
        # dozvoljavalo nizvodno neizvodljive podele (npr. 0.03mm).
        lower_ok = (avg_split >= min_split) if enforce_min_split else (avg_split >= relaxed_floor)
        if lower_ok and avg_split <= max_split:
            deviation = abs(avg_split - target_split)
            if best_result is None or deviation < best_result[2]:
                best_result = (n, avg_split, deviation)

    return best_result


def determine_tire_setup_with_fallback(
    dimension: Decimal,
    available_tires: Dict[Decimal, int],
    min_split: Decimal = Decimal('15'),
    max_split: Decimal = Decimal('30'),
    target_split: Decimal = Decimal('22.5'),
    fallback_order: Tuple[Decimal, ...] = (Decimal('20'), Decimal('15'), Decimal('10')),
    min_feasible_split: Decimal = Decimal('0.1'),
) -> TireSetupResult:
    """
    Određuje broj i veličinu guma sa automatskim fallback-om.

    Logika:
    1. Pokušaj sa 20mm (default) - ako daje validne podele i ima na stanju
    2. Ako 20mm ne radi (dimenzija premala ili nema na stanju) → pokušaj 15mm
    3. Ako ni 15mm ne radi → pokušaj 10mm
    4. Ako nijedna veličina ne daje podele u standardnom opsegu → relaksirani prolaz
       (donja granica = min_feasible_split) za male dimenzije
    5. Ako stvarno ništa ne radi → vrati grešku

    Napomena o prioritetu (v2.10.0, usklađeno sa stvarnim ponašanjem):
    prva veličina iz fallback_order ima APSOLUTNI prioritet — ako je validna,
    koristi se bez daljeg poređenja. Među PREOSTALIM veličinama ne važi strogi
    redosled liste, već se bira ona sa najmanjim odstupanjem od target_split.

    Args:
        dimension: Dimenzija za sečenje
        available_tires: Dostupne gume sa količinama
        min_split: Minimalna dozvoljena podela
        max_split: Maksimalna dozvoljena podela
        target_split: Ciljna podela (22.5mm)
        fallback_order: Redosled pokušaja veličina guma
        min_feasible_split: Najmanja izvodljiva podela za relaksirani prolaz
            (v2.9.0; tipično = najmanja dostupna biksna, default 0.1)

    Returns:
        TireSetupResult sa brojem guma, veličinom i eventualnom porukom
    """
    # Filtriraj samo veličine koje imaju gume na stanju
    available_sizes = {size for size, count in available_tires.items() if count > 0}

    best_result: Optional[Tuple[Decimal, int, Decimal, Decimal]] = None  # (size, num, avg, dev)
    tried_sizes: List[str] = []

    for tire_size in fallback_order:
        # Proveri da li ima guma te veličine na stanju
        if tire_size not in available_sizes:
            tried_sizes.append(f"{tire_size}mm (nema na stanju)")
            continue

        result = evaluate_tire_setup_for_size(
            dimension, tire_size, min_split, max_split, target_split
        )

        if result is None:
            tried_sizes.append(f"{tire_size}mm (dimenzija premala)")
            continue

        num_tires, avg_split, deviation = result

        # Proveri da li ima dovoljno guma
        if available_tires[tire_size] < num_tires:
            tried_sizes.append(f"{tire_size}mm (potrebno {num_tires}, dostupno {available_tires[tire_size]})")
            continue

        # Nađena validna konfiguracija
        if best_result is None or deviation < best_result[3]:
            best_result = (tire_size, num_tires, avg_split, deviation)

        # Ako smo našli rezultat sa prvom (preferiranom) veličinom, koristi ga
        if tire_size == fallback_order[0]:
            break

    if best_result is not None:
        tire_size, num_tires, avg_split, _ = best_result

        # Ako nije default veličina, generiši poruku
        # (v2.9.0: bez hardkodovanog "20mm" — koristi stvarnu default veličinu)
        if tire_size != fallback_order[0]:
            message = (
                f"Automatski izabrana guma {tire_size}mm "
                f"({fallback_order[0]}mm nije pogodna za ovu dimenziju)"
            )
        else:
            message = None

        return TireSetupResult(
            num_tires=num_tires,
            tire_size=tire_size,
            message=message,
            avg_split=avg_split,
            is_error=False,
        )

    # Relaksirani prolaz: nijedna veličina ne daje podele u standardnom opsegu
    # [min_split..max_split], što je tipično za male dimenzije (npr. 20mm).
    # Probaj bez donje granice da mala dimenzija dobije upotrebljivu gumu
    # (bira se po najmanjem odstupanju od ciljne podele) umesto lažne greške.
    # Ovo je konzistentno sa nizvodnim calculate_splits, koje ionako koristi
    # balance_tolerance oko proseka, a ne tvrdi prag min_split.
    relaxed_best: Optional[Tuple[Decimal, int, Decimal, Decimal]] = None
    for tire_size in fallback_order:
        if tire_size not in available_sizes:
            continue
        result = evaluate_tire_setup_for_size(
            dimension, tire_size, min_split, max_split, target_split,
            enforce_min_split=False,
            relaxed_floor=min_feasible_split,
        )
        if result is None:
            continue
        num_tires, avg_split, deviation = result
        if available_tires[tire_size] < num_tires:
            continue
        if relaxed_best is None or deviation < relaxed_best[3]:
            relaxed_best = (tire_size, num_tires, avg_split, deviation)

    if relaxed_best is not None:
        tire_size, num_tires, avg_split, _ = relaxed_best
        return TireSetupResult(
            num_tires=num_tires,
            tire_size=tire_size,
            message=(
                f"Automatski izabrana guma {tire_size}mm sa uskim podelama "
                f"(~{avg_split.quantize(Decimal('0.1'), rounding=ROUND_HALF_UP)}mm) — "
                f"dimenzija {dimension} je ispod standardnog opsega podela "
                f"({min_split}–{max_split}mm)."
            ),
            avg_split=avg_split,
            is_error=False,
        )

    # Nijedna veličina nije radila ni u relaksiranom režimu - vrati grešku sa detaljima
    tried_str = ", ".join(tried_sizes) if tried_sizes else "nijedna veličina"
    return TireSetupResult(
        num_tires=1,
        tire_size=fallback_order[-1],  # Koristi najmanju kao fallback
        message=f"Dimenzija {dimension} nije pogodna za standardne gume. Pokušano: {tried_str}",
        avg_split=None,
        is_error=True,
    )


def calculate_sum_of_deductions(
    dimensions: List[Decimal],
    num_runs: int,
    reverse_sequence: bool
) -> Decimal:
    """Calculate total deduction based on run pattern."""
    if len(dimensions) < 2 or num_runs <= 0:
        return Decimal('0.0')

    a, b = (dimensions[0], dimensions[1]) if not reverse_sequence else (dimensions[1], dimensions[0])
    count_a = (num_runs + 1) // 2
    count_b = num_runs // 2
    return a * count_a + b * count_b


# Ordinal mappings (Serbian)
ORDINAL_MAP: Dict[int, str] = {
    1: "Prvi", 2: "Drugi", 3: "Treći", 4: "Četvrti", 5: "Peti",
    6: "Šesti", 7: "Sedmi", 8: "Osmi", 9: "Deveti", 10: "Deseti",
    11: "Jedanaesti", 12: "Dvanaesti", 13: "Trinaesti", 14: "Četrnaesti",
    15: "Petnaesti", 16: "Šesnaesti", 17: "Sedamnaesti", 18: "Osamnaesti",
    19: "Devetnaesti", 20: "Dvadeseti",
}

AXLE_FORMS: Dict[str, Dict[str, str]] = {
    'gornja osovina': {'locative': 'gornjoj osovini', 'accusative': 'gornju osovinu'},
    'donja osovina': {'locative': 'donjoj osovini', 'accusative': 'donju osovinu'},
}

def _axle_form(axle_name: str, case: str) -> str:
    return AXLE_FORMS.get(axle_name, {}).get(case, axle_name)


def get_ordinal(n: int) -> str:
    """Get Serbian ordinal for a number."""
    return ORDINAL_MAP.get(n, f"{n}.")


# =============================================================================
# DOMAIN MODELS
# =============================================================================

@dataclass(slots=True)
class SplitResult:
    """Result of a single split calculation."""
    value: Decimal
    gaps: List[Decimal]
    warning: Optional[str] = None


@dataclass(slots=True)
class NarrowCutResult:
    """Result of a narrow cut calculation.

    v2.10.0: polje 'dimension' preimenovano u 'gap_total' — uvek je sadržalo
    zbir upotrebljenih biksni, ne dimenziju reza.
    """
    gap_total: Decimal
    gaps: List[Decimal]
    note: Optional[str] = None
    knives_used: Optional[Tuple[Decimal, ...]] = None


@dataclass(slots=True)
class UserInputs:
    """Validated user inputs."""
    ratio: int
    clearance: Decimal
    selected_knives: List[Decimal]
    material: Optional[str] = None
    quality: Optional[str] = None
    thickness: Optional[Decimal] = None

    def __post_init__(self) -> None:
        if self.ratio not in (25, 50):
            raise ValidationError(f"Invalid ratio: {self.ratio}. Must be 25 or 50.")
        if self.clearance <= 0:
            raise ValidationError(f"Clearance must be positive: {self.clearance}")
        if not self.selected_knives:
            raise ValidationError("At least one knife must be selected.")
        # Ensure knives are sorted and unique
        self.selected_knives = sorted(set(self.selected_knives))


@dataclass(slots=True)
class DimensionInput:
    """Input specification for a dimension."""
    dimension: Decimal
    num_runs: int
    num_tires: Optional[int] = None
    tire_size: Optional[Decimal] = None

    def __post_init__(self) -> None:
        if self.dimension <= 0:
            raise ValidationError(f"Dimension must be positive: {self.dimension}")
        if self.num_runs <= 0:
            raise ValidationError(f"Number of runs must be positive: {self.num_runs}")
        if self.num_tires is not None and self.num_tires <= 0:
            raise ValidationError(f"Number of tires must be positive: {self.num_tires}")
        if self.tire_size is not None and self.tire_size <= 0:
            raise ValidationError(f"Tire size must be positive: {self.tire_size}")


@dataclass(slots=True)
class DimensionContext:
    """Computed context for dimension processing."""
    index: int
    dimension: Decimal
    num_runs: int
    num_tires: int
    tire_size: Decimal
    wide_dimension: Decimal
    reduced_dimension: Decimal
    tire_message: Optional[str] = None


@dataclass(slots=True)
class DimensionResult:
    """Complete result for a dimension calculation."""
    dim_index: int
    dimension: Decimal
    wide_dimension: Decimal
    clearance: Decimal
    num_tires: int
    tire_size: Decimal
    wide_results: List[List[SplitResult]]
    narrow_results: List[NarrowCutResult]
    knives_selection: List[Optional[Tuple[Decimal, ...]]]


@dataclass
class CalculationResult:
    """Complete calculation result for all dimensions.

    axle_base: The shared starting value for both axles before knife/gap deduction.
    The per-axle knife and gap details are captured in deduction_msg_upper /
    deduction_msg_lower respectively.
    """
    dimensions: List[DimensionResult]
    total_gaps_used: Counter
    knives_used: Counter
    tires_used: Counter
    axle_base: Decimal
    deduction_msg_upper: str = ""
    deduction_msg_lower: str = ""
    messages: List[str] = field(default_factory=list)
    solver_name: str = "nepoznat"



@dataclass
class InventoryState:
    """Current state of inventory (knives, gaps, and tires)."""
    available_gaps: Dict[Decimal, int]
    small_gaps: Dict[Decimal, int]
    available_knives: Dict[Decimal, int]
    available_tires: Dict[Decimal, int]
    gaps_used_splits: Counter = field(default_factory=Counter)
    gaps_used_narrow: Counter = field(default_factory=Counter)
    gaps_used_axle: Counter = field(default_factory=Counter)
    knives_used: Counter = field(default_factory=Counter)
    tires_used: Counter = field(default_factory=Counter)
    knife_selection_history: List[Tuple[Decimal, ...]] = field(default_factory=list)

    # Private cache fields
    _total_gaps_cache: Optional[Counter] = field(default=None, repr=False, compare=False)
    _combined_gaps_cache: Optional[Dict[Decimal, int]] = field(default=None, repr=False, compare=False)
    _cache_dirty: bool = field(default=True, repr=False, compare=False)

    @property
    def total_gaps_used(self) -> Counter:
        """Return total gaps used with caching."""
        if self._cache_dirty or self._total_gaps_cache is None:
            self._total_gaps_cache = self.gaps_used_splits + self.gaps_used_narrow + self.gaps_used_axle
            self._cache_dirty = False
        return self._total_gaps_cache

    def invalidate_caches(self) -> None:
        """Invalidate all cached values. Call after mutations."""
        self._cache_dirty = True
        self._combined_gaps_cache = None

    def copy(self) -> InventoryState:
        """Create a deep copy of this state."""
        return InventoryState(
            available_gaps=dict(self.available_gaps),
            small_gaps=dict(self.small_gaps),
            available_knives=dict(self.available_knives),
            available_tires=dict(self.available_tires),
            gaps_used_splits=Counter(self.gaps_used_splits),
            gaps_used_narrow=Counter(self.gaps_used_narrow),
            gaps_used_axle=Counter(self.gaps_used_axle),
            knives_used=Counter(self.knives_used),
            tires_used=Counter(self.tires_used),
            knife_selection_history=list(self.knife_selection_history),
        )

    def get_combined_gaps(self) -> Dict[Decimal, int]:
        """Return combined available_gaps and small_gaps with caching."""
        if self._combined_gaps_cache is None:
            combined = dict(self.available_gaps)
            for gap, count in self.small_gaps.items():
                combined[gap] = combined.get(gap, 0) + count
            self._combined_gaps_cache = combined
        return self._combined_gaps_cache


# =============================================================================
# LP SOLVER UTILITIES
# =============================================================================

@dataclass(frozen=True, slots=True)
class LPSolution:
    """Container for LP solution results.

    v2.10.0: uklonjeno polje 'variables' — računato je za svako rešenje,
    a nigde se nije čitalo (ekstrakcije koriste pulp.value direktno).
    """
    status: str
    objective_value: Optional[float]
    is_optimal: bool

    @classmethod
    def from_problem(cls, prob: pulp.LpProblem) -> LPSolution:
        status = pulp.LpStatus[prob.status]
        obj_value = pulp.value(prob.objective) if prob.objective else None
        return cls(
            status=status,
            objective_value=obj_value,
            is_optimal=(status == 'Optimal')
        )


class LPSolver:
    """Centralized LP solver with configurable settings.

    Koristi pulp.HiGHS koji poziva highspy in-memory API direktno
    (nema LP fajlova na disku). Od v2.9.0 postoje dva profila:
    'integral' (sa mip_abs_gap=0.5, za celobrojne ciljeve) i
    'continuous' (bez abs_gap, za kontinualne/mešovite ciljeve).

    Note: _solver_cache is shared across all instances (class-level).
    This is intentional for performance but means this class is
    NOT safe for concurrent use across threads or processes.
    """

    # v2.9.0: keš ključan po (time_limit, integral_objective) — dva profila
    _solver_cache: Dict[Tuple[int, bool], pulp.apis.LpSolver] = {}

    def __init__(self, config: Optional[SolverConfig] = None):
        self.config = config or SolverConfig()
        # v2.9.0: dva solver profila.
        #   - integral: ciljna funkcija je celobrojna (broj biksni/noževa) →
        #     mip_abs_gap=0.5 je korektan i značajno skraćuje B&B.
        #   - continuous: cilj sadrži kontinualne članove (devijacije podela,
        #     koeficijenti 0.1/0.01) → abs_gap 0.5 bi dozvolio rešenje do 0.5
        #     lošije od optimalnog, pa se tamo NE postavlja.
        self._solver_integral = self._get_or_create_solver(self.config.time_limit, True)
        self._solver_continuous = self._get_or_create_solver(self.config.time_limit, False)
        self.solver_name = self._describe_solver(self._solver_integral)

    @classmethod
    def _build_highs_solver(
        cls,
        time_limit: int,
        integral_objective: bool = True
    ) -> pulp.apis.LpSolver:
        """Create a tuned HiGHS solver instance (requires highspy via pulp.HiGHS).

        Napomena (v2.9.0): PuLP-ov pulp.HiGHS čita SAMO kwargs (optionsDict) i
        imenovane parametre (threads, timeLimit...); 'options' lista se kod
        highspy interfejsa u potpunosti ignoriše (potvrđeno na PuLP 3.3.2:
        createAndConfigureSolver iterira isključivo self.optionsDict). Zato se
        svi HiGHS parametri prosleđuju kao kwargs.

        Performance settings:
          - threads = cpu_count()//2
                Za male MIP (<50 promenljivih) pola jezgara je empirijski optimum;
                svi jezgri uvode koordinacioni overhead koji usporava rešavanje.
          - mip_abs_gap = 0.5  (SAMO za integral_objective=True)
                Korektan isključivo kada su sve vrednosti ciljne funkcije celi
                brojevi (GapCombination, Split faza 1, Axle). Za kontinualne/
                mešovite ciljeve (Split faza 2, NarrowCut) se ne postavlja.
          - mip_rel_gap = 0.0005
                Relativni gap 0.05% — zanemarljiv za planove rezanja.
          - mip_max_nodes = 50000
                Sigurnosni limit čvorova B&B stabla — sprečava runaway.
                (v2.9.0: tek sada zaista aktivan — ranije u ignorisanoj listi.)
          - simplex_strategy = 1
                Dual simplex za root LP relaxaciju u MIP-u.
                (v2.9.0: tek sada zaista aktivan — ranije u ignorisanoj listi.)
          - presolve = "on"
                Smanjuje veličinu problema pre B&B — zadržati.
          - log_to_console: uklonjen — msg=False već postavlja output_flag=False
                u highspy, pa je bio suvišan (a u listi ionako ignorisan).
        """
        threads = max(1, (os.cpu_count() or 2) // 2)

        highs_kwargs: Dict[str, object] = {
            "mip_rel_gap": 0.0005,
            "presolve": "on",
            "mip_max_nodes": 50000,
            "simplex_strategy": 1,
        }
        if integral_objective:
            highs_kwargs["mip_abs_gap"] = 0.5

        # Za HiGHS_CMD opcije idu kao "key=value" stavke u options listi.
        cmd_options = [f"threads={threads}"] + [
            f"{key}={value}" for key, value in highs_kwargs.items()
        ]

        try:
            highs_solver = pulp.HiGHS(
                msg=False,
                timeLimit=time_limit,
                threads=threads,
                **highs_kwargs,
            )
            if highs_solver.available():
                return highs_solver
            logger.warning("pulp.HiGHS nije dostupan. Pokušavam HiGHS_CMD.")
        except (TypeError, AttributeError):
            # Stariji PuLP ne prima sve kwargs — fallback na HiGHS_CMD sa opcijama
            logger.warning(
                "pulp.HiGHS ne podržava sve parametre (stara verzija PuLP-a). "
                "Korišćen HiGHS_CMD fallback."
            )

        highs_cmd_solver = pulp.HiGHS_CMD(
            msg=False,
            timeLimit=time_limit,
            options=cmd_options,
        )
        if highs_cmd_solver.available():
            return highs_cmd_solver

        logger.warning("HiGHS_CMD nije dostupan. Korišćen CBC fallback.")
        return pulp.PULP_CBC_CMD(msg=False, timeLimit=time_limit)

    @staticmethod
    def _describe_solver(solver: pulp.apis.LpSolver) -> str:
        """Return a user-facing solver label for debug output."""
        solver_type = type(solver).__name__
        if solver_type == "HiGHS":
            return "HiGHS (highspy)"
        if solver_type == "HiGHS_CMD":
            return "HiGHS_CMD (highs komanda)"
        if solver_type == "PULP_CBC_CMD":
            return "CBC fallback"
        return solver_type

    @classmethod
    def _get_or_create_solver(
        cls,
        time_limit: int,
        integral_objective: bool
    ) -> pulp.apis.LpSolver:
        """Get or create a cached HiGHS solver instance for the given profile."""
        key = (time_limit, integral_objective)
        if key not in cls._solver_cache:
            cls._solver_cache[key] = cls._build_highs_solver(time_limit, integral_objective)
        return cls._solver_cache[key]

    @classmethod
    def clear_cache(cls) -> None:
        """Clear the solver cache to free memory."""
        cls._solver_cache.clear()

    def _pick_solver(self, integral_objective: bool) -> pulp.apis.LpSolver:
        """Izaberi solver profil prema tipu ciljne funkcije (v2.9.0)."""
        return self._solver_integral if integral_objective else self._solver_continuous

    # v2.10.0: uklonjen solve() (raise varijanta) — u celom kodu se koristi
    # isključivo try_solve; sa njim su otišli i OptimizationError/
    # NoSolutionFoundError izuzeci.

    def try_solve(
        self,
        prob: pulp.LpProblem,
        integral_objective: bool = True
    ) -> Optional[LPSolution]:
        """Try to solve the problem, returning None if not optimal.

        integral_objective: True kada su sve vrednosti ciljne funkcije celi
        brojevi (dozvoljava mip_abs_gap=0.5 profil); False za kontinualne/
        mešovite ciljeve (v2.9.0).
        """
        prob.solve(self._pick_solver(integral_objective))
        solution = LPSolution.from_problem(prob)
        return solution if solution.is_optimal else None


class GapCombinationBuilder:
    """Builder for gap combination LP problems."""

    def __init__(self, config: SolverConfig):
        self.config = config
        self.tolerance_float = decimal_to_float(config.tolerance)

    def build(
        self,
        name: str,
        target: Decimal,
        available_gaps: Dict[Decimal, int]
    ) -> Tuple[pulp.LpProblem, Dict[Decimal, pulp.LpVariable]]:
        """Build an LP problem to find gap combinations summing to target."""
        prob = pulp.LpProblem(name, pulp.LpMinimize)

        gap_vars = {
            gap: pulp.LpVariable(
                make_var_name("gap", gap),
                lowBound=0,
                upBound=count,
                cat='Integer'
            )
            for gap, count in available_gaps.items()
        }

        # Objective: minimize total gaps used
        prob += pulp.lpSum(gap_vars.values()), "TotalGapsUsed"

        # Constraint: sum must equal target (within tolerance)
        target_float = decimal_to_float(target)
        gap_sum = pulp.lpSum([
            decimal_to_float(gap) * var
            for gap, var in gap_vars.items()
        ])
        prob += gap_sum <= target_float + self.tolerance_float, "TargetUpperBound"
        prob += gap_sum >= target_float - self.tolerance_float, "TargetLowerBound"

        return prob, gap_vars

    def extract_result(
        self,
        gap_vars: Dict[Decimal, pulp.LpVariable],
        target: Decimal
    ) -> Tuple[List[Decimal], Optional[str], Counter]:
        """Extract gaps list and count from solved problem."""
        gaps: List[Decimal] = []
        gaps_count: Counter = Counter()
        total = Decimal('0')

        for gap, var in gap_vars.items():
            count = solver_int(pulp.value(var), self.config)
            if count > 0:
                gaps.extend([gap] * count)
                gaps_count[gap] = count
                total += gap * count

        warning = None
        if abs(total - target) > self.config.tolerance:
            warning = f"Kombinacija biksni se ne podudara tačno sa ciljem {target}. Suma: {total}"

        return gaps, warning, gaps_count


@dataclass(frozen=True)
class SplitStrategy:
    """Typed configuration for a single split fallback strategy.

    v2.10.0: uklonjeno polje allow_zeros — nijedna strategija ga nikada
    nije koristila (uvek False), pa je cela Big-M nulta mašinerija obrisana.
    """
    use_small_gaps: bool
    balance_tolerance: Decimal
    max_balance_tolerance: Decimal
    message: Optional[str] = None


class SplitProblemBuilder:
    """Builder for split dimension LP problems.

    v2.10.0: uklonjena allow_zeros/is_zero mašinerija (Big-M LinkZero
    ograničenja) — nikada nije bila aktivirana ni iz jedne strategije.
    """

    def __init__(self, config: SolverConfig):
        self.config = config
        self.tolerance_float = decimal_to_float(config.tolerance)

    def build(
        self,
        name: str,
        remaining_dimension: Decimal,
        num_splits: int,
        min_split: Decimal,
        max_split: Decimal,
        available_gaps: Dict[Decimal, int]
    ) -> Tuple[pulp.LpProblem, List[pulp.LpVariable], Dict]:
        """Build a two-phase LP problem for splitting dimensions."""
        if num_splits <= 0:
            raise ValueError(f"num_splits must be positive, got {num_splits}")

        prob = pulp.LpProblem(name, pulp.LpMinimize)
        gap_keys = list(available_gaps.keys())
        remaining_float = decimal_to_float(remaining_dimension)

        # Create split variables
        splits = [
            pulp.LpVariable(
                f"{name}_split_{i}",
                lowBound=float(min_split),
                upBound=float(max_split)
            )
            for i in range(num_splits)
        ]

        # Create gap variables for each split
        gap_vars = {
            (i, gap): pulp.LpVariable(
                make_gap_var_name(name, i, gap),
                lowBound=0,
                upBound=available_gaps[gap],
                cat='Integer'
            )
            for i in range(num_splits)
            for gap in gap_keys
        }

        # Total splits must equal remaining dimension
        prob += pulp.lpSum(splits) <= remaining_float + self.tolerance_float, f"{name}_TotalSplitUpperBound"
        prob += pulp.lpSum(splits) >= remaining_float - self.tolerance_float, f"{name}_TotalSplitLowerBound"

        # v2.10.0: RAZBIJANJE SIMETRIJE — podele su međusobno zamenljive
        # (problem je potpuno simetričan po indeksima), pa se nameće
        # neopadajući redosled split promenljivih. Skup dostižnih
        # multiskupova podela je nepromenjen, a B&B pretraga se drastično
        # skraćuje (faza B je pre ovoga umela da udari u time limit od 5s).
        # Fizički raspored podela ionako određuje best_balance_arrangement.
        for i in range(num_splits - 1):
            prob += splits[i] <= splits[i + 1], f"{name}_SymBreak_{i}"

        # Gap sum must equal split value for each split
        for i in range(num_splits):
            gap_sum = pulp.lpSum([
                decimal_to_float(gap) * gap_vars[(i, gap)]
                for gap in gap_keys
            ])
            prob += gap_sum <= splits[i] + self.tolerance_float, f"{name}_GapSumUpperSplit_{i}"
            prob += gap_sum >= splits[i] - self.tolerance_float, f"{name}_GapSumLowerSplit_{i}"

        # Cumulative gap usage constraint
        for gap in gap_keys:
            gap_str = str(gap).replace('.', '_')
            prob += (
                pulp.lpSum([gap_vars[(i, gap)] for i in range(num_splits)]) <= available_gaps[gap],
                f"{name}_CumulativeGapUsage_{gap_str}"
            )

        return prob, splits, gap_vars

    def add_phase1_objective(
        self,
        prob: pulp.LpProblem,
        gap_vars: Dict
    ) -> None:
        """Add Phase 1 objective: minimize gaps."""
        prob += pulp.lpSum(gap_vars.values()), "MinimizeTotalGaps"

    def add_phase2_objective(
        self,
        prob: pulp.LpProblem,
        splits: List[pulp.LpVariable],
        min_split: Decimal,
        max_split: Decimal
    ) -> None:
        """Add Phase 2 objective: minimize deviation between splits."""
        num_splits = len(splits)

        if num_splits == 2:
            deviation = pulp.LpVariable(f"{prob.name}_abs_diff", lowBound=0)
            prob += splits[0] - splits[1] <= deviation, f"{prob.name}_DiffPos"
            prob += splits[1] - splits[0] <= deviation, f"{prob.name}_DiffNeg"
            prob += deviation, "MinimizeDeviation"
        elif num_splits > 2:
            average = pulp.LpVariable(
                f"{prob.name}_avg",
                lowBound=float(min_split),
                upBound=float(max_split)
            )
            deviations = [
                pulp.LpVariable(f"{prob.name}_dev_{i}", lowBound=0)
                for i in range(num_splits)
            ]
            prob += num_splits * average == pulp.lpSum(splits), f"{prob.name}_AverageDefinition"
            for i in range(num_splits):
                prob += splits[i] - average <= deviations[i], f"{prob.name}_DevUpper_{i}"
                prob += average - splits[i] <= deviations[i], f"{prob.name}_DevLower_{i}"
            prob += pulp.lpSum(deviations), "MinimizeDeviation"
        else:
            # num_splits == 1: nema devijacije među podelama
            prob += 0, "MinimizeDeviation"


class NarrowCutProblemBuilder:
    """Builder for narrow cut LP problems."""

    def __init__(self, config: SolverConfig):
        self.config = config
        self.tolerance_float = decimal_to_float(config.tolerance)
        self.large_m = config.large_m
        self.weights = config.weights

    def build(
        self,
        dimension: Decimal,
        clearance: Decimal,
        available_knives: Dict[Decimal, int],
        available_gaps: Dict[Decimal, int]
    ) -> Tuple[pulp.LpProblem, Dict, Dict, pulp.LpVariable]:
        """Build LP problem for narrow cut knife selection."""
        prob = pulp.LpProblem("NarrowCutKnifeSelection", pulp.LpMinimize)

        # Knife variables (at most 2 per knife)
        knife_vars = {
            k: pulp.LpVariable(
                make_var_name("knife", k),
                lowBound=0,
                upBound=min(c, 2),
                cat='Integer'
            )
            for k, c in available_knives.items()
        }
        prob += pulp.lpSum(knife_vars.values()) == 2, "TotalKnivesUsed"

        # Calculate remaining dimension after knives
        total_knife = pulp.lpSum([
            decimal_to_float(k) * var
            for k, var in knife_vars.items()
        ])
        remaining = decimal_to_float(dimension - 2 * clearance) - total_knife

        # Binary variable to indicate if gaps are needed
        need_gaps = pulp.LpVariable("need_gaps", cat='Binary')
        remainder_abs = pulp.LpVariable("remainder_abs", lowBound=0)

        prob += remainder_abs >= remaining, "RemainderAbsUpper"
        prob += remainder_abs >= -remaining, "RemainderAbsLower"
        prob += (
            remainder_abs <= self.tolerance_float + self.large_m * need_gaps,
            "RemainderWithinToleranceIfNoGaps"
        )

        # Gap variables
        gap_vars = {
            g: pulp.LpVariable(
                make_var_name("gap", g),
                lowBound=0,
                upBound=c,
                cat='Integer'
            )
            for g, c in available_gaps.items()
        }
        gap_sum = pulp.lpSum([
            decimal_to_float(g) * var
            for g, var in gap_vars.items()
        ])

        # Gap constraints
        prob += (
            gap_sum <= remaining + self.tolerance_float + self.large_m * (1 - need_gaps),
            "GapsSumToRemainingDimensionUpper"
        )
        prob += (
            gap_sum >= remaining - self.tolerance_float - self.large_m * (1 - need_gaps),
            "GapsSumToRemainingDimensionLower"
        )
        prob += (
            pulp.lpSum(gap_vars.values()) <= self.large_m * need_gaps,
            "OnlyUseGapsIfNeeded"
        )

        # Objective
        prob += (
            pulp.lpSum(gap_vars.values()) +
            self.weights.gap_usage_penalty * need_gaps +
            self.weights.secondary_objective * remainder_abs,
            "MinimizeTotalGapsUsed"
        )

        return prob, knife_vars, gap_vars, need_gaps


class AxleDeductionProblemBuilder:
    """Builder for axle deduction LP problems."""

    def __init__(self, config: SolverConfig):
        self.config = config
        self.tolerance_float = decimal_to_float(config.tolerance)

    def build(
        self,
        axle_dimension: Decimal,
        available_knives: List[Decimal],
        available_gaps: Dict[Decimal, int]
    ) -> Tuple[pulp.LpProblem, Dict, Dict]:
        """Build LP problem for axle deduction."""
        prob = pulp.LpProblem("AxleDeductionKnifeSelection", pulp.LpMinimize)

        # Select exactly one knife
        knife_vars = {
            k: pulp.LpVariable(make_var_name("knife", k), cat='Binary')
            for k in available_knives
        }
        prob += pulp.lpSum(knife_vars.values()) == 1, "SelectOneKnife"

        # Calculate remaining after knife selection
        total_knife = pulp.lpSum([
            decimal_to_float(k) * var
            for k, var in knife_vars.items()
        ])
        remaining = decimal_to_float(axle_dimension) - total_knife

        # Gap variables
        gap_vars = {
            g: pulp.LpVariable(
                make_var_name("gap", g),
                lowBound=0,
                upBound=c,
                cat='Integer'
            )
            for g, c in available_gaps.items()
        }
        gap_sum = pulp.lpSum([
            decimal_to_float(g) * var
            for g, var in gap_vars.items()
        ])

        # Constraints
        prob += gap_sum <= remaining + self.tolerance_float, "GapsSumUpper"
        prob += gap_sum >= remaining - self.tolerance_float, "GapsSumLower"

        # Objective: minimize gaps
        prob += pulp.lpSum(gap_vars.values()), "MinimizeTotalGapsUsed"

        return prob, knife_vars, gap_vars


# =============================================================================
# INVENTORY MANAGER
# =============================================================================

class InventoryManager:
    """Manages gap, knife, and tire inventory.

    v2.10.0: uklonjen transaction() context manager — nikada nije korišćen.
    (Rollback po dimenziji bi menjao ponašanje — npr. dimenzija sa delimično
    neuspelim rezovima bi nestala iz plana umesto da se prikaže sa
    upozorenjima — pa se ne uvodi bez odluke o željenoj semantici.)
    """

    def __init__(
        self,
        state: InventoryState,
        config: Optional[SolverConfig] = None,
        narrow_allowed_knives: Optional[Iterable[Decimal]] = None
    ):
        self.state = state
        self.config = config or SolverConfig()
        self.narrow_allowed_knives = (
            tuple(sorted(set(narrow_allowed_knives)))
            if narrow_allowed_knives is not None
            else None
        )
        self.solver = LPSolver(self.config)
        self._gap_builder = GapCombinationBuilder(self.config)
        self._split_builder = SplitProblemBuilder(self.config)
        self._narrow_builder = NarrowCutProblemBuilder(self.config)
        self._axle_builder = AxleDeductionProblemBuilder(self.config)

    def find_gap_combination(
        self,
        target: Decimal,
        usage_type: GapUsageType = GapUsageType.SPLITS,
        simulate: bool = False,
        allow_small_gaps: bool = True
    ) -> Tuple[Optional[List[Decimal]], Optional[str], Optional[Counter]]:
        """Find gaps that sum to target."""
        result = self._find_combination_in_pool(
            target, self.state.available_gaps, simulate, usage_type
        )
        if result[0] is not None:
            return result

        if allow_small_gaps and self.state.small_gaps:
            return self._find_combination_with_small_gaps(target, simulate, usage_type)

        return None, f"Nije pronađena kombinacija za cilj {target}.", None

    def _find_combination_in_pool(
        self,
        target: Decimal,
        available_gaps: Dict[Decimal, int],
        simulate: bool,
        usage_type: GapUsageType
    ) -> Tuple[Optional[List[Decimal]], Optional[str], Optional[Counter]]:
        """Try to find combination in specific gap pool."""
        prob, gap_vars = self._gap_builder.build("GapCombination", target, available_gaps)
        solution = self.solver.try_solve(prob)

        if not solution:
            return None, None, None

        gaps, warning, gaps_count = self._gap_builder.extract_result(gap_vars, target)

        if not simulate:
            self._commit_gap_usage(gaps_count, usage_type)

        return gaps, warning, gaps_count

    def _find_combination_with_small_gaps(
        self,
        target: Decimal,
        simulate: bool,
        usage_type: GapUsageType
    ) -> Tuple[Optional[List[Decimal]], Optional[str], Optional[Counter]]:
        """Try to find combination including small gaps."""
        combined = self.state.get_combined_gaps()
        # Pass dry_run=True here to prevent a double-commit: we handle the
        # actual commit ourselves below once we know which gaps to deduct.
        result = self._find_combination_in_pool(target, combined, True, usage_type)

        if result[0] is None:
            return None, f"Nije pronađena kombinacija za cilj {target}.", None

        gaps, warning, gaps_count = result

        if not simulate:
            self._commit_gap_usage(gaps_count, usage_type, include_small_gaps=True)

        return gaps, warning, gaps_count

    def _commit_gap_usage(
        self,
        gaps_count: Counter,
        usage_type: GapUsageType,
        include_small_gaps: bool = False
    ) -> None:
        """Commit gap usage to inventory."""
        for gap, count in gaps_count.items():
            remaining = self._deduct_from_available_gaps(gap, count)
            if include_small_gaps and remaining > 0:
                self._deduct_from_small_gaps(gap, remaining)
            self._record_gap_usage(gap, count, usage_type)
        self.state.invalidate_caches()

    def _deduct_from_available_gaps(self, gap: Decimal, count: int) -> int:
        """Deduct from available_gaps, return remaining count."""
        if gap not in self.state.available_gaps:
            return count

        deduct = min(count, self.state.available_gaps[gap])
        self.state.available_gaps[gap] -= deduct

        if self.state.available_gaps[gap] <= 0:
            del self.state.available_gaps[gap]

        return count - deduct

    def _deduct_from_small_gaps(self, gap: Decimal, count: int) -> None:
        """Deduct from small_gaps pool."""
        if gap not in self.state.small_gaps:
            return

        deduct = min(count, self.state.small_gaps[gap])
        self.state.small_gaps[gap] -= deduct

        if self.state.small_gaps[gap] <= 0:
            del self.state.small_gaps[gap]

        self.state.invalidate_caches()

    def _record_gap_usage(self, gap: Decimal, count: int, usage_type: GapUsageType) -> None:
        """Record gap usage by type."""
        usage_map = {
            GapUsageType.SPLITS: self.state.gaps_used_splits,
            GapUsageType.NARROW: self.state.gaps_used_narrow,
            GapUsageType.AXLE: self.state.gaps_used_axle,
        }
        usage_map[usage_type][gap] += count

    def use_knives(self, knife_counts: Dict[Decimal, int]) -> None:
        """Record knife usage and update inventory."""
        knives_list: List[Decimal] = []

        for knife, count in knife_counts.items():
            if knife in self.state.available_knives:
                actual_count = min(count, self.state.available_knives[knife])
                self.state.available_knives[knife] -= actual_count
                self.state.knives_used[knife] += actual_count
                knives_list.extend([knife] * actual_count)

        if knives_list:
            self.state.knife_selection_history.append(tuple(knives_list))

    def use_tires(self, tire_size: Decimal, count: int) -> bool:
        """
        Koristi gume iz inventara.

        Args:
            tire_size: Veličina gume
            count: Broj guma za upotrebu

        Returns:
            True ako je uspešno, False ako nema dovoljno guma
        """
        if tire_size not in self.state.available_tires:
            return False

        if self.state.available_tires[tire_size] < count:
            return False

        self.state.available_tires[tire_size] -= count
        self.state.tires_used[tire_size] += count

        if self.state.available_tires[tire_size] <= 0:
            del self.state.available_tires[tire_size]

        return True

    def get_available_tire_sizes(self) -> Dict[Decimal, int]:
        """Vraća dostupne veličine i količine guma."""
        return {k: v for k, v in self.state.available_tires.items() if v > 0}

    def get_available_knife_sizes(
        self,
        min_count: int = 1,
        allowed_knives: Optional[Iterable[Decimal]] = None
    ) -> List[Decimal]:
        """Get list of available knife sizes with at least min_count available."""
        allowed_set = set(allowed_knives) if allowed_knives is not None else None
        return [
            size for size, count in self.state.available_knives.items()
            if count >= min_count and (allowed_set is None or size in allowed_set)
        ]

    def get_knife_capacities(
        self,
        allowed_knives: Optional[Iterable[Decimal]] = None
    ) -> Dict[Decimal, int]:
        """Get current knife capacities (non-zero only)."""
        allowed_set = set(allowed_knives) if allowed_knives is not None else None
        return {
            k: v for k, v in self.state.available_knives.items()
            if v > 0 and (allowed_set is None or k in allowed_set)
        }

    def calculate_splits(
        self,
        remaining_dimension: Decimal,
        num_splits: int,
        step_size: Decimal = Decimal('0.01'),
        balance_tolerance: Decimal = Decimal('2.0'),
        max_balance_tolerance: Decimal = Decimal('10.0')
    ) -> Tuple[Optional[List[SplitResult]], List[str]]:
        """
        Calculate splits with fallback strategies and strict balance window.

        v2.10.0: default step_size usklađen sa CalculatorConfig.step_size (0.01);
        ranije je default bio 0.1 iako su svi pozivi slali 0.01 iz configa.

        Fallback order:
        1. Regular gaps with increasing balance tolerance
        2. Small gaps with increasing balance tolerance
        """
        if num_splits <= 0:
            return None, ["Broj podela mora biti pozitivan."]

        messages: List[str] = []
        strategies = self._generate_split_strategies(
            remaining_dimension, balance_tolerance, max_balance_tolerance
        )

        for strategy in strategies:
            result, strategy_msg, final_tolerance = self._execute_split_strategy(
                remaining_dimension, num_splits, step_size, strategy
            )
            if result is not None:
                # v2.9.0: poruka strategije (npr. "Korišćenje malih biksni...")
                # se dodaje SAMO kada je ta strategija stvarno dala rezultat —
                # ranije se ispisivala i kad strategija propadne, obmanjujući
                # operatera da su male biksne upotrebljene.
                if strategy_msg:
                    messages.append(strategy_msg)
                if final_tolerance is not None and final_tolerance > balance_tolerance:
                    messages.append(
                        f"Povećana tolerancija podela na {final_tolerance}mm "
                        f"za dimenziju {remaining_dimension}"
                    )
                return result, messages

        return None, messages

    def _generate_split_strategies(
        self,
        dimension: Decimal,
        start_tolerance: Decimal,
        max_tolerance: Decimal
    ) -> List[SplitStrategy]:
        """Generate ordered list of fallback strategies."""
        strategies: List[SplitStrategy] = []
        has_small_gaps = bool(self.state.small_gaps)

        # Strategy 1: Regular gaps only
        strategies.append(SplitStrategy(
            use_small_gaps=False,
            balance_tolerance=start_tolerance,
            max_balance_tolerance=max_tolerance,
            message=None,
        ))

        # Strategy 2: Include small gaps
        if has_small_gaps:
            strategies.append(SplitStrategy(
                use_small_gaps=True,
                balance_tolerance=start_tolerance,
                max_balance_tolerance=max_tolerance,
                message=f"Korišćenje malih biksni za dimenziju {dimension}",
            ))

        return strategies

    def _execute_split_strategy(
        self,
        dimension: Decimal,
        num_splits: int,
        step_size: Decimal,
        strategy: SplitStrategy
    ) -> Tuple[Optional[List[SplitResult]], Optional[str], Optional[Decimal]]:
        """Execute a single split strategy with tolerance escalation.

        Returns:
            Tuple of (result, message, final_tolerance_used)
        """
        current_tolerance = strategy.balance_tolerance
        max_tolerance = strategy.max_balance_tolerance
        message = strategy.message

        while current_tolerance <= max_tolerance:
            result = self._try_split(
                dimension,
                num_splits,
                current_tolerance,
                step_size,
                strategy.use_small_gaps
            )
            if result is not None:
                return result, message, current_tolerance
            current_tolerance += Decimal('1.0')

        return None, message, None

    def _try_split(
        self,
        remaining_dimension: Decimal,
        num_splits: int,
        balance_tolerance: Decimal,
        step_size: Decimal,
        use_small_gaps: bool = False
    ) -> Optional[List[SplitResult]]:
        """Try to find a split solution."""
        average = remaining_dimension / num_splits
        half_tolerance = balance_tolerance / Decimal('2')
        min_split = max(average - half_tolerance, self.config.weights.minimum_split)
        max_split = average + half_tolerance

        gaps_to_use = (
            self.state.get_combined_gaps() if use_small_gaps
            else self.state.available_gaps
        )

        # Phase 1: Minimize gaps
        prob1, splits1, gap_vars1 = self._split_builder.build(
            "SplitPhaseA",
            remaining_dimension,
            num_splits,
            min_split,
            max_split,
            gaps_to_use
        )
        self._split_builder.add_phase1_objective(prob1, gap_vars1)

        solution1 = self.solver.try_solve(prob1)
        if not solution1:
            return None

        min_gaps = sum(
            solver_int(pulp.value(var), self.config)
            for var in gap_vars1.values()
        )

        # Phase 2: Minimize deviation
        prob2, splits2, gap_vars2 = self._split_builder.build(
            "SplitPhaseB",
            remaining_dimension,
            num_splits,
            min_split,
            max_split,
            gaps_to_use
        )
        prob2 += pulp.lpSum(gap_vars2.values()) == min_gaps, "FixTotalGaps"

        self._split_builder.add_phase2_objective(
            prob2, splits2, min_split, max_split
        )

        # v2.9.0: faza 2 minimizuje KONTINUALNE devijacije podela →
        # continuous profil (bez mip_abs_gap=0.5 koji bi dozvolio do 0.5mm
        # lošiji ukupni balans od optimalnog).
        solution2 = self.solver.try_solve(prob2, integral_objective=False)

        results, total_usage = self._extract_split_results(
            splits2 if solution2 else splits1,
            gap_vars2 if solution2 else gap_vars1,
            num_splits,
            step_size
        )

        # v2.9.0: parcijalno rešenje (manje podela od traženog) NIJE uspeh.
        # Commit biksni se radi tek POSLE ove validacije — ranije su se
        # biksne trošile i kada bi rezultat bio odbačen kao nepotpun.
        if len(results) != num_splits:
            return None

        if total_usage:
            self._commit_gap_usage(total_usage, GapUsageType.SPLITS, use_small_gaps)

        return results

    def _extract_split_results(
        self,
        splits: List[pulp.LpVariable],
        gap_vars: Dict,
        num_splits: int,
        step_size: Decimal
    ) -> Tuple[List[SplitResult], Counter]:
        """Extract split results WITHOUT committing gap usage (v2.9.0).

        Commit je odgovornost pozivaoca (_try_split) i dešava se tek posle
        validacije da je broj podela kompletan — time se sprečava trošenje
        biksni na parcijalna rešenja koja se odbacuju.

        Returns:
            Tuple (rezultati_podela, ukupna_potrošnja_biksni)
        """
        results: List[SplitResult] = []
        total_usage: Counter = Counter()

        for i in range(num_splits):
            split_value = pulp.value(splits[i])
            if split_value is None:
                continue

            split_decimal = solver_decimal(split_value, step_size)
            if split_decimal.copy_abs() <= self.config.rounding_tolerance:
                continue

            gaps, gaps_count = self._get_split_gaps(gap_vars, i)
            if not gaps_count:
                continue

            total_usage.update(gaps_count)
            results.append(SplitResult(value=split_decimal, gaps=gaps))

        return results, total_usage

    def _get_split_gaps(
        self,
        gap_vars: Dict,
        split_index: int
    ) -> Tuple[List[Decimal], Counter]:
        """Get gaps for a specific split."""
        gaps: List[Decimal] = []
        gaps_count: Counter = Counter()

        for (idx, gap), var in gap_vars.items():
            if idx != split_index:
                continue
            count = solver_int(pulp.value(var), self.config)
            if count > 0:
                gaps.extend([gap] * count)
                gaps_count[gap] = count

        return gaps, gaps_count

    def calculate_narrow_cut(
        self,
        dimension: Decimal,
        clearance: Decimal
    ) -> NarrowCutResult:
        """Calculate narrow cut with knife selection.

        Returns a NarrowCutResult; gap_total is 0 and gaps is empty on failure.
        """
        available = self.get_available_knife_sizes(allowed_knives=self.narrow_allowed_knives)
        if not available:
            return NarrowCutResult(
                gap_total=Decimal('0'),
                gaps=[],
                note="Nema dostupnih noževa.",
                knives_used=None,
            )

        capacities = self.get_knife_capacities(allowed_knives=self.narrow_allowed_knives)
        usable_slots = sum(min(c, 2) for c in capacities.values())

        if usable_slots < 2:
            return NarrowCutResult(
                gap_total=Decimal('0'),
                gaps=[],
                note=(
                    f"Nema dovoljno noževa za uski rez "
                    f"(potrebna su 2, dostupno {usable_slots})."
                ),
                knives_used=None,
            )

        target_length = dimension - 2 * clearance
        min_pair = compute_min_pair_length(capacities)

        if min_pair is not None and target_length + self.config.tolerance < min_pair:
            return NarrowCutResult(
                gap_total=Decimal('0'),
                gaps=[],
                note=(
                    f"Dimenzija posle zazora ({target_length}) je manja od "
                    f"zbira najmanja dva noža ({min_pair})."
                ),
                knives_used=None,
            )

        result = self._solve_narrow_cut(dimension, clearance, capacities)

        if result is None and self.state.small_gaps:
            result = self._solve_narrow_cut_with_small_gaps(dimension, clearance, capacities)

        if result is None:
            return NarrowCutResult(
                gap_total=Decimal('0'),
                gaps=[],
                note="Nije pronađena moguća kombinacija noževa i biksni.",
                knives_used=None,
            )

        return result

    def _solve_narrow_cut(
        self,
        dimension: Decimal,
        clearance: Decimal,
        knife_capacities: Dict[Decimal, int]
    ) -> Optional[NarrowCutResult]:
        """Solve narrow cut with regular gaps."""
        prob, knife_vars, gap_vars, need_gaps = self._narrow_builder.build(
            dimension, clearance, knife_capacities, self.state.available_gaps
        )
        # v2.9.0: cilj sadrži kontinualne članove (0.1*need_gaps +
        # 0.01*remainder_abs) → continuous profil, bez mip_abs_gap=0.5.
        solution = self.solver.try_solve(prob, integral_objective=False)

        if not solution:
            return None

        return self._extract_narrow_cut_results(knife_vars, gap_vars, need_gaps, False)

    def _solve_narrow_cut_with_small_gaps(
        self,
        dimension: Decimal,
        clearance: Decimal,
        knife_capacities: Dict[Decimal, int]
    ) -> Optional[NarrowCutResult]:
        """Solve narrow cut including small gaps."""
        combined = self.state.get_combined_gaps()
        prob, knife_vars, gap_vars, need_gaps = self._narrow_builder.build(
            dimension, clearance, knife_capacities, combined
        )
        # v2.9.0: kontinualan/mešovit cilj → continuous profil (v. _solve_narrow_cut)
        solution = self.solver.try_solve(prob, integral_objective=False)

        if not solution:
            return None

        return self._extract_narrow_cut_results(knife_vars, gap_vars, need_gaps, True)

    def _extract_narrow_cut_results(
        self,
        knife_vars: Dict,
        gap_vars: Dict,
        need_gaps: pulp.LpVariable,
        include_small_gaps: bool
    ) -> Optional[NarrowCutResult]:
        """Extract narrow cut results."""
        knives: List[Decimal] = []
        knife_counts: Counter = Counter()
        total_knife = Decimal('0')

        for knife, var in knife_vars.items():
            count = solver_int(pulp.value(var), self.config)
            if count > 0:
                knives.extend([knife] * count)
                knife_counts[knife] = count
                total_knife += knife * count

        if not knives:
            return None

        self.use_knives(knife_counts)

        use_gaps = solver_int(pulp.value(need_gaps), self.config) == 1
        gaps: List[Decimal] = []
        gaps_count: Counter = Counter()
        total_gap = Decimal('0')
        note: Optional[str] = None

        if use_gaps:
            for gap, var in gap_vars.items():
                count = solver_int(pulp.value(var), self.config)
                if count > 0:
                    gaps.extend([gap] * count)
                    gaps_count[gap] = count
                    total_gap += gap * count
            self._commit_gap_usage(gaps_count, GapUsageType.NARROW, include_small_gaps)
        else:
            note = f"Tačno poklapanje sa noževima (suma: {sum(knives)}), biksne nisu potrebne."

        return NarrowCutResult(
            gap_total=total_gap,
            gaps=gaps,
            note=note,
            knives_used=tuple(knives),
        )

    def calculate_axle_deduction(
        self,
        axle_dimension: Decimal
    ) -> Optional[Tuple[Decimal, Decimal, List[Decimal], Counter]]:
        """Calculate axle deduction with knife selection."""
        available = [
            k for k in AXLE_ALLOWED_KNIVES
            if self.state.available_knives.get(k, 0) > 0
        ]
        if not available:
            return None

        result = self._solve_axle_deduction(
            axle_dimension, available, self.state.available_gaps
        )

        if result is None and self.state.small_gaps:
            combined = self.state.get_combined_gaps()
            result = self._solve_axle_deduction(
                axle_dimension, available, combined, include_small_gaps=True
            )

        return result

    def _solve_axle_deduction(
        self,
        axle_dimension: Decimal,
        available_knives: List[Decimal],
        available_gaps: Dict[Decimal, int],
        include_small_gaps: bool = False
    ) -> Optional[Tuple[Decimal, Decimal, List[Decimal], Counter]]:
        """Solve axle deduction problem."""
        prob, knife_vars, gap_vars = self._axle_builder.build(
            axle_dimension, available_knives, available_gaps
        )
        solution = self.solver.try_solve(prob)

        if not solution:
            return None

        selected_knife = next(
            (k for k, var in knife_vars.items()
             if solver_int(pulp.value(var), self.config) == 1),
            None
        )

        if selected_knife is None:
            return None

        # Update knife inventory (sa zaštitom od negativnog stanja)
        current_count = self.state.available_knives.get(selected_knife, 0)
        if current_count <= 0:
            return None
        self.state.available_knives[selected_knife] = current_count - 1
        self.state.knives_used[selected_knife] += 1
        self.state.knife_selection_history.append((selected_knife,))

        # Extract gaps
        gaps: List[Decimal] = []
        gaps_count: Counter = Counter()
        total_gap = Decimal('0')

        for gap, var in gap_vars.items():
            count = solver_int(pulp.value(var), self.config)
            if count > 0:
                gaps.extend([gap] * count)
                gaps_count[gap] = count
                total_gap += gap * count

        self._commit_gap_usage(gaps_count, GapUsageType.AXLE, include_small_gaps)

        return selected_knife, total_gap, gaps, gaps_count


# =============================================================================
# MAIN CALCULATOR
# =============================================================================

class TireChoice(NamedTuple):
    """Kandidat u balansnoj selekciji guma (v2.10.0 — umesto 7-torke)."""
    offset: Decimal
    gaps_count: int
    order_idx: int
    tire_size: Decimal
    deviation: Decimal
    num_tires: int
    setup: TireSetupResult


class CuttingCalculator:
    """Main calculator for cutting optimization."""

    def __init__(self, user_inputs: UserInputs, config: Optional[AppConfig] = None):
        self.config = config or DEFAULT_CONFIG
        self.user_inputs = user_inputs
        self.calc_config = self.config.calculator
        self._inventory = self._create_inventory()
        self._manager = InventoryManager(
            self._inventory,
            self.config.solver,
            narrow_allowed_knives=self.user_inputs.selected_knives
        )
        self._messages: List[str] = []

    def _create_inventory(self) -> InventoryState:
        """Create initial inventory state."""
        gaps = self.config.get_adjusted_gaps(self.user_inputs.ratio)
        knives = dict(self.config.inventory.knives)
        tires = dict(self.config.inventory.tires)
        return InventoryState(
            available_gaps=gaps,
            small_gaps=dict(self.config.inventory.small_gaps),
            available_knives=knives,
            available_tires=tires
        )

    def calculate(self, dimensions_data: List[DimensionInput]) -> CalculationResult:
        """Perform complete calculation for all dimensions."""
        self._messages.clear()
        base_number = Decimal(self.config.base_numbers[self.user_inputs.ratio])

        results: List[DimensionResult] = []
        total_deduction = Decimal('0')
        total_runs = 0
        reverse_sequence = False

        for i, dim_input in enumerate(dimensions_data, start=1):
            context = self._build_dimension_context(i, dim_input)
            if context is None:
                continue

            dimensions_list = [context.dimension, context.reduced_dimension]
            total_deduction += calculate_sum_of_deductions(
                dimensions_list, context.num_runs, reverse_sequence
            )
            total_runs += context.num_runs

            dim_result = self._calculate_dimension(context)
            if dim_result:
                results.append(dim_result)

            if context.num_runs % 2 == 1:
                reverse_sequence = not reverse_sequence

        axle_base = (base_number - total_deduction).to_integral_value(rounding=ROUND_DOWN)

        if axle_base < 0:
            self._messages.append(
                f"Greška: Ukupan odbitak ({total_deduction}) premašuje osnovni broj ({base_number})."
            )
            return CalculationResult(
                dimensions=[],
                total_gaps_used=Counter(),
                knives_used=Counter(),
                tires_used=Counter(),
                axle_base=Decimal('0'),
                messages=list(self._messages),
                solver_name=self._manager.solver.solver_name,
            )

        deduction_msg_upper, deduction_msg_lower = self._apply_axle_deduction(
            axle_base, total_runs
        )

        return CalculationResult(
            dimensions=results,
            total_gaps_used=self._inventory.total_gaps_used,
            knives_used=self._inventory.knives_used,
            tires_used=self._inventory.tires_used,
            axle_base=axle_base,
            deduction_msg_upper=deduction_msg_upper,
            deduction_msg_lower=deduction_msg_lower,
            messages=list(self._messages),
            solver_name=self._manager.solver.solver_name,
        )

    def _build_dimension_context(
        self,
        index: int,
        dim_input: DimensionInput
    ) -> Optional[DimensionContext]:
        """Build context for a dimension."""
        dimension = dim_input.dimension
        clearance = self.user_inputs.clearance

        if dim_input.num_tires is not None and dim_input.tire_size is not None:
            # Ručno podešene gume
            num_tires = dim_input.num_tires
            tire_size = dim_input.tire_size
            tire_message = f"Korišćeni ručno podešeni pneumatici: {num_tires}x{tire_size}mm"

            # Proveri da li ima dovoljno guma na stanju
            available_tires = self._manager.get_available_tire_sizes()
            if tire_size not in available_tires or available_tires[tire_size] < num_tires * dim_input.num_runs:
                tire_message += " (Upozorenje: možda nema dovoljno guma na stanju)"
        else:
            # Automatski izbor guma sa optimizacijom balansne tačke
            tire_result = self._select_balanced_tire_setup(
                dimension, dim_input.num_runs
            )

            num_tires = tire_result.num_tires
            tire_size = tire_result.tire_size
            tire_message = tire_result.message

        if tire_message:
            self._messages.append(f"Dimenzija {index} ({dimension}): {tire_message}")

        # Validate dimensions BEFORE consuming tires
        wide_base = dimension - (tire_size * Decimal(num_tires))
        if wide_base <= 0:
            self._messages.append(
                f"Dimenzija {dimension} ne dozvoljava {num_tires} pneumatika "
                f"veličine {tire_size}. Preskačem ovu stavku."
            )
            return None

        wide_dimension = quantize_to_hundredths(wide_base)
        reduced_dimension = quantize_to_hundredths(dimension - (clearance * 2))

        if reduced_dimension <= 0:
            self._messages.append(
                f"Dimenzija {dimension} nije validna za zazor {clearance}. Preskačem ovu stavku."
            )
            return None

        # Consume tires AFTER validation; abort if not enough
        total_tires_needed = num_tires * dim_input.num_runs
        if not self._manager.use_tires(tire_size, total_tires_needed):
            self._messages.append(
                f"Greška: Nema dovoljno guma {tire_size}mm za dimenziju {dimension} "
                f"(potrebno {total_tires_needed}, dostupno "
                f"{self._manager.get_available_tire_sizes().get(tire_size, 0)}). "
                f"Preskačem ovu stavku."
            )
            return None

        return DimensionContext(
            index=index,
            dimension=dimension,
            num_runs=dim_input.num_runs,
            num_tires=num_tires,
            tire_size=tire_size,
            wide_dimension=wide_dimension,
            reduced_dimension=reduced_dimension,
            tire_message=tire_message
        )

    def _select_balanced_tire_setup(
        self,
        dimension: Decimal,
        num_runs: int
    ) -> TireSetupResult:
        """Select tire setup that minimizes balance offset for wide cut."""
        available_tires = self._manager.get_available_tire_sizes()
        if num_runs <= 0:
            num_runs = 1

        per_run_capacity = {
            size: count // num_runs
            for size, count in available_tires.items()
            if count >= num_runs
        }

        candidates = self._generate_tire_candidates(dimension, per_run_capacity)
        choices: List[TireChoice] = []

        for order_idx, tire_size, num_tires, avg_split, deviation in candidates:
            wide_base = dimension - tire_size * Decimal(num_tires)
            if wide_base <= 0:
                continue

            wide_dimension = quantize_to_hundredths(wide_base)
            simulation = self._simulate_wide_balance(
                wide_dimension, num_tires, tire_size
            )
            if simulation is None:
                continue

            offset, gaps_count = simulation
            message = (
                f"Automatski balans izbor: {num_tires}x{tire_size}mm "
                f"(offset: {offset.quantize(Decimal('0.1'), rounding=ROUND_HALF_UP)}mm)"
            )
            setup = TireSetupResult(
                num_tires=num_tires,
                tire_size=tire_size,
                message=message,
                avg_split=avg_split,
                is_error=False,
            )
            choices.append(TireChoice(
                offset=offset,
                gaps_count=gaps_count,
                order_idx=order_idx,
                tire_size=tire_size,
                deviation=deviation,
                num_tires=num_tires,
                setup=setup,
            ))

        if choices:
            balance_limit = self.calc_config.balance_tolerance
            preferred_rank = {Decimal('20'): 0, Decimal('15'): 1}
            balanced = [c for c in choices if c.offset <= balance_limit]
            preferred_balanced = [c for c in balanced if c.tire_size in preferred_rank]

            # NAMERNA asimetrija kriterijuma po granama (dokumentovano v2.10.0):
            #  - preferred_balanced: svi kandidati su VEĆ u balansnoj toleranciji,
            #    pa se prvo štedi materijal (manje biksni), zatim se preferira
            #    veća guma (20 pre 15), pa tek onda finiji offset.
            #  - balanced (nepreferirane veličine): primaran je offset, zatim broj
            #    biksni, pa redosled iz fallback liste.
            #  - van tolerancije: najmanji offset je glavni kriterijum jer nijedan
            #    kandidat nije "dovoljno dobar" — bira se najmanje loš.
            if preferred_balanced:
                best_choice = min(
                    preferred_balanced,
                    key=lambda c: (
                        c.gaps_count,
                        preferred_rank.get(c.tire_size, 99),
                        c.offset,
                        c.deviation,
                        c.num_tires,
                    )
                )
            elif balanced:
                best_choice = min(
                    balanced,
                    key=lambda c: (
                        c.offset,
                        c.gaps_count,
                        c.order_idx,
                        c.deviation,
                        c.num_tires,
                    )
                )
            else:
                best_choice = min(
                    choices,
                    key=lambda c: (
                        c.offset,
                        c.gaps_count,
                        c.deviation,
                        c.order_idx,
                        c.num_tires,
                    )
                )

            return best_choice.setup

        # v2.9.0: relaksirani prolaz dobija najmanju STVARNO dostupnu biksnu
        # kao donju granicu podele — podela manja od najmanje biksne je
        # nizvodno neizvodljiva, pa se takav setup ne predlaže.
        combined_gaps = self._inventory.get_combined_gaps()
        min_gap = min(
            (g for g, c in combined_gaps.items() if c > 0),
            default=Decimal('0.1')
        )

        return determine_tire_setup_with_fallback(
            dimension,
            per_run_capacity,
            self.config.tire.min_split,
            self.config.tire.max_split,
            self.config.tire.target_split,
            self.config.tire.fallback_order,
            min_feasible_split=min_gap,
        )

    def _generate_tire_candidates(
        self,
        dimension: Decimal,
        per_run_capacity: Dict[Decimal, int],
        top_per_size: int = 3
    ) -> List[Tuple[int, Decimal, int, Decimal, Decimal]]:
        """Generate top candidate setups per tire size by target split proximity."""
        candidates: List[Tuple[int, Decimal, int, Decimal, Decimal]] = []
        min_split = self.config.tire.min_split
        max_split = self.config.tire.max_split
        target_split = self.config.tire.target_split

        for order_idx, tire_size in enumerate(self.config.tire.fallback_order):
            capacity = per_run_capacity.get(tire_size, 0)
            if capacity <= 0:
                continue

            n_max = min(int(dimension // tire_size), capacity)
            if n_max <= 0:
                continue

            size_candidates: List[Tuple[Decimal, int, Decimal]] = []
            for num_tires in range(1, n_max + 1):
                remaining = dimension - tire_size * Decimal(num_tires)
                if remaining <= 0:
                    continue
                avg_split = remaining / Decimal(num_tires + 1)
                if min_split <= avg_split <= max_split:
                    deviation = abs(avg_split - target_split)
                    size_candidates.append((deviation, num_tires, avg_split))

            size_candidates.sort(key=lambda x: (x[0], x[1]))
            for deviation, num_tires, avg_split in size_candidates[:top_per_size]:
                candidates.append((
                    order_idx, tire_size, num_tires, avg_split, deviation
                ))

        return candidates

    def _simulate_wide_balance(
        self,
        wide_dimension: Decimal,
        num_tires: int,
        tire_size: Decimal
    ) -> Optional[Tuple[Decimal, int]]:
        """Simulate wide cut on copied inventory and return (offset, gaps_count)."""
        if wide_dimension <= 0:
            return None

        num_splits = num_tires + 1
        probe_state = self._inventory.copy()
        probe_manager = InventoryManager(probe_state, self.config.solver)
        # v2.10.0: simulacija ide BEZ eskalacije tolerancije (max = start) —
        # balansni filter ionako traži offset <= balance_tolerance, a puna
        # eskalacija (do 9 koraka x 2 LP faze po kandidatu) je pravila i do
        # ~150 MIP solve-ova pre stvarnog izračuna. Pravi rez u
        # _calculate_wide_cut i dalje eskalira do max_balance_tolerance, a ako
        # simulacija ne nađe nijednog kandidata, fallback setup takođe prolazi
        # kroz punu eskalaciju u stvarnom rezu.
        splits, _ = probe_manager.calculate_splits(
            wide_dimension,
            num_splits,
            self.calc_config.step_size,
            self.calc_config.balance_tolerance,
            self.calc_config.balance_tolerance,
        )
        if not splits:
            return None

        # v2.9.0: LP redosled podela je proizvoljan (problem je simetričan po
        # indeksima), pa se offset računa nad NAJBOLJIM rasporedom — istim onim
        # u koji se preuređuje i stvarni široki rez (_calculate_wide_cut).
        _, metrics = best_balance_arrangement(splits, num_tires, tire_size)
        if metrics is None:
            return None

        _, _, _, offset = metrics
        gaps_count = sum(len(split.gaps) for split in splits)
        return offset, gaps_count

    def _calculate_dimension(self, context: DimensionContext) -> Optional[DimensionResult]:
        """Calculate results for a single dimension."""
        clearance = self.user_inputs.clearance
        wide_results: List[List[SplitResult]] = []
        narrow_results: List[NarrowCutResult] = []
        knives_selection: List[Optional[Tuple[Decimal, ...]]] = []

        for _ in range(context.num_runs):
            wide_results.append(self._calculate_wide_cut(context))

            narrow = self._manager.calculate_narrow_cut(context.dimension, clearance)
            narrow_results.append(narrow)
            knives_selection.append(narrow.knives_used)

        return DimensionResult(
            dim_index=context.index,
            dimension=context.dimension,
            wide_dimension=context.wide_dimension,
            clearance=clearance,
            num_tires=context.num_tires,
            tire_size=context.tire_size,
            wide_results=wide_results,
            narrow_results=narrow_results,
            knives_selection=knives_selection
        )

    def _calculate_wide_cut(self, context: DimensionContext) -> List[SplitResult]:
        """Calculate wide cut for a dimension.

        num_splits is always >= 2 (num_tires >= 1 is guaranteed by validation),
        so the splits path is always taken.
        """
        remaining = context.wide_dimension
        num_splits = context.num_tires + 1

        splits, split_messages = self._manager.calculate_splits(
            remaining,
            num_splits,
            self.calc_config.step_size,
            self.calc_config.balance_tolerance,
            self.calc_config.max_balance_tolerance
        )
        self._messages.extend(split_messages)

        if splits:
            # v2.9.0: preuredi podele u raspored sa najboljim balansom, da
            # prikazani redosled odgovara offsetu iz automatskog izbora guma
            # (LP redosled je proizvoljan, a operater slaže podele po planu).
            arranged, _ = best_balance_arrangement(
                splits, context.num_tires, context.tire_size
            )
            return arranged

        return [SplitResult(
            value=remaining,
            gaps=[],
            warning=f"Nisu pronađene kombinacije biksni za podele od {remaining}."
        )]

    def _apply_axle_deduction(
        self,
        axle_base: Decimal,
        total_runs: int
    ) -> Tuple[str, str]:
        """Apply axle deduction calculations."""
        if total_runs == 0:
            return "", ""

        is_upper = total_runs % 2 == 1
        primary_axle = 'gornja osovina' if is_upper else 'donja osovina'
        secondary_axle = 'donja osovina' if is_upper else 'gornja osovina'

        primary_msg = self._calculate_primary_axle(primary_axle, axle_base)
        if primary_msg is None:
            return "", ""

        secondary_msg = self._calculate_secondary_axle(secondary_axle, axle_base)

        return (primary_msg, secondary_msg) if is_upper else (secondary_msg, primary_msg)

    def _calculate_primary_axle(self, axle_name: str, dimension: Decimal) -> Optional[str]:
        """Calculate primary axle deduction with knife selection.

        Primarna osovina je ona na kojoj se postavlja poslednji nož u nizu.
        Za nju se bira nož + biksne da bi se popunila dimenzija.
        """
        result = self._manager.calculate_axle_deduction(dimension)

        if result is None:
            self._messages.append(
                f"Nije pronađena moguća kombinacija noža i biksni "
                f"za odbitak na {_axle_form(axle_name, 'locative')} ({dimension})."
            )
            return None

        selected_knife, new_dimension, gaps, _ = result
        return self._format_axle_message(axle_name, selected_knife, new_dimension, gaps)

    def _calculate_secondary_axle(self, axle_name: str, dimension: Decimal) -> str:
        """Calculate secondary axle gaps (no knife).

        Sekundarna osovina nema nož - koristi samo biksne za popunjavanje.
        Nož se ne dodaje jer je poslednji nož već dodeljen primarnoj osovini.
        """
        gaps, warning, _ = self._manager.find_gap_combination(dimension, GapUsageType.AXLE)

        if gaps:
            msg = self._format_gaps_message(axle_name, dimension, gaps)
            if warning:
                msg += f" (Upozorenje: {warning})"
            return msg

        return f"Nije pronađena moguća kombinacija biksni za {_axle_form(axle_name, 'accusative')} ({dimension})."

    def _format_axle_message(
        self,
        axle_name: str,
        knife: Decimal,
        dimension: Decimal,
        gaps: List[Decimal]
    ) -> str:
        """Format the axle deduction message."""
        gaps_str = ', '.join(str(g) for g in gaps) if gaps else 'Nema'
        return (
            f"Poslednji nož na {_axle_form(axle_name, 'locative')}: {knife}\n"
            f"Kombinacija biksni za {_axle_form(axle_name, 'accusative')} ({dimension}): [{gaps_str}]"
        )

    def _format_gaps_message(
        self,
        axle_name: str,
        dimension: Decimal,
        gaps: List[Decimal]
    ) -> str:
        """Format gaps-only message."""
        gaps_str = ', '.join(str(g) for g in gaps)
        return f"Kombinacija biksni za {_axle_form(axle_name, 'accusative')} ({dimension}): [{gaps_str}]"


# =============================================================================
# PRESENTER
# =============================================================================

class ResultPresenter:
    """Presents calculation results in formatted output."""

    def __init__(self, console: Optional[Console] = None):
        self.console = console or Console()

    def present(
        self,
        result: CalculationResult,
        user_inputs: Optional[UserInputs] = None
    ) -> None:
        """Present full calculation results."""
        if user_inputs:
            self._show_clearance_params(user_inputs)

        self._print_messages(result.messages)

        if not result.dimensions:
            self.console.print(
                "[bold red]Računanje nije dalo nijedan rezultat zbog prethodnih grešaka.[/bold red]"
            )
            self._print_solver_debug(result.solver_name)
            return

        upper_results, lower_results = self._build_axle_results(result.dimensions)
        self._render_axle_section(
            "Gornja osovina", "Gornja", upper_results, result.deduction_msg_upper
        )
        self._render_axle_section(
            "Donja osovina", "Donja", lower_results, result.deduction_msg_lower
        )
        self._show_final_summary(result.total_gaps_used, result.tires_used, result.knives_used)
        self._print_solver_debug(result.solver_name)

    def _show_clearance_params(self, user_inputs: UserInputs) -> None:
        """Show clearance parameters (v2.10.0: i poreklo zazora ako je iz kalkulatora)."""
        lines = [f"[bold]Zazor:[/bold] {user_inputs.clearance} mm"]
        if user_inputs.material and user_inputs.quality and user_inputs.thickness is not None:
            quality_label = QUALITY_GRADES.get(
                user_inputs.quality, (user_inputs.quality, None)
            )[0]
            lines.append(
                f"[bold]Materijal:[/bold] {user_inputs.material}  |  "
                f"[bold]Kvalitet:[/bold] {quality_label}  |  "
                f"[bold]Debljina:[/bold] {user_inputs.thickness} mm"
            )
        self.console.print(Panel(
            "\n".join(lines),
            title="Parametri zazora",
            style="bold green"
        ))

    def _print_solver_debug(self, solver_name: str) -> None:
        """Print solver used for this calculation."""
        self.console.print(Panel(
            f"Solver: {solver_name}",
            title="Debug",
            style="dim cyan",
            expand=False,
        ))

    def _print_messages(self, messages: List[str]) -> None:
        """Print all messages with appropriate styling.

        v2.10.0: "Automatski balans izbor" je normalan tok (info/plavo);
        žuto upozorenje ostaje za odstupanja (fallback veličine, povećane
        tolerancije, male biksne).
        """
        warning_keywords = (
            "Upozorenje", "Povećana", "tolerancija podela", "malih biksni",
            "Automatski izabrana"
        )

        for msg in messages:
            if "Greška" in msg:
                style = "bold red"
            elif any(w in msg for w in warning_keywords):
                style = "bold yellow"
            else:
                style = "bold blue"
            self.console.print(Panel(msg, style=style))

    def _build_axle_results(
        self,
        dimensions: List[DimensionResult]
    ) -> Tuple[List, List]:
        """Build upper and lower axle results from dimensions."""
        upper_results: List = []
        lower_results: List = []
        counters = {
            'upper_wide': 0, 'upper_narrow': 0,
            'lower_wide': 0, 'lower_narrow': 0
        }
        global_run = 0

        for dim in dimensions:
            dim_label = f"Dimenzija {dim.dim_index}" if len(dimensions) > 1 else None
            upper_runs: List = []
            lower_runs: List = []

            for i in range(len(dim.wide_results)):
                global_run += 1
                is_odd_run = global_run % 2 == 1

                wide_text = self._format_wide(
                    dim.wide_results[i],
                    dim.num_tires,
                    dim.tire_size
                )
                narrow_text = self._format_narrow(
                    dim.narrow_results[i],
                    dim.knives_selection[i]
                )

                if is_odd_run:
                    counters['upper_wide'] += 1
                    counters['lower_narrow'] += 1
                    upper_runs.append((
                        f"{get_ordinal(counters['upper_wide'])} široki",
                        wide_text
                    ))
                    lower_runs.append((
                        f"{get_ordinal(counters['lower_narrow'])} uski",
                        narrow_text
                    ))
                else:
                    counters['upper_narrow'] += 1
                    counters['lower_wide'] += 1
                    upper_runs.append((
                        f"{get_ordinal(counters['upper_narrow'])} uski",
                        narrow_text
                    ))
                    lower_runs.append((
                        f"{get_ordinal(counters['lower_wide'])} široki",
                        wide_text
                    ))

            upper_results.append((dim_label, upper_runs))
            lower_results.append((dim_label, lower_runs))

        return upper_results, lower_results

    def _render_axle_section(
        self,
        title: str,
        short_title: str,
        results: List,
        deduction_msg: str
    ) -> None:
        """Render a single axle section."""
        self.console.print(Panel(title, style="bold cyan"))

        for dim_label, runs in results:
            if dim_label:
                self.console.print(Panel(dim_label, style="bold blue"))
            for label, text in runs:
                self.console.print(Panel(label, style="bold green", expand=False))
                self.console.print(text)

        if deduction_msg:
            self.console.print(Panel(
                deduction_msg,
                style="bold yellow",
                title=f"Odbitak osovine ({short_title})",
                expand=False
            ))

    def _format_wide(
        self,
        wide_result: List[SplitResult],
        num_tires: int,
        tire_size: Decimal
    ) -> str:
        """Format wide cut result."""
        lines: List[str] = []
        ordered_splits = wide_result

        # Redni brojevi za podele sa fiksnom širinom za poravnanje
        split_ordinals = {
            1: "Prva",   2: "Druga",  3: "Treća",  4: "Četvrta", 5: "Peta",
            6: "Šesta",  7: "Sedma",  8: "Osma",   9: "Deveta",  10: "Deseta"
        }

        # Prikazuj naizmenično: podela - guma - podela - guma - podela
        for idx, split in enumerate(ordered_splits, start=1):
            gap_str = ', '.join(str(g) for g in split.gaps) if split.gaps else "Nema"
            ordinal = split_ordinals.get(idx, f"{idx}.")
            # Poravnanje - "Četvrta podela:" je najduži (15 karaktera sa dvotačkom)
            label = f"{ordinal} podela:"
            padded_label = label.ljust(15)
            lines.append(f"  - [white]{padded_label}[/white] [{gap_str}]")
            if split.warning:
                lines.append(f"    [yellow]Upozorenje: {split.warning}[/yellow]")

            # Dodaj gumu posle svake podele osim poslednje
            if idx < len(ordered_splits):
                lines.append(f"      [white]Guma[/white]: [bold yellow]{tire_size}[/bold yellow]")

        return "\n".join(lines)

    def _format_narrow(
        self,
        narrow: NarrowCutResult,
        knives: Optional[Tuple[Decimal, ...]]
    ) -> str:
        """Format narrow cut result."""
        lines: List[str] = []
        gap_str = ', '.join(str(g) for g in narrow.gaps) if narrow.gaps else "Nema"

        if knives:
            knives_str = ', '.join(f'[bold red]{k}[/bold red]' for k in knives)
        else:
            knives_str = "N/A"

        lines.append(f"  - [white]Noževi[/white]: [{knives_str}]")
        lines.append(f"  - [white]Kombinacija biksni[/white]: [{gap_str}]")
        if narrow.note:
            lines.append(f"    [yellow]Upozorenje: {narrow.note}[/yellow]")

        return "\n".join(lines)

    def _show_final_summary(
        self,
        total_gaps_used: Counter,
        tires_used: Counter,
        knives_used: Counter
    ) -> None:
        """Show final usage summary."""
        self.console.print(Panel(
            "Završni pregled korišćenih biksni, guma i noževa",
            style="bold magenta"
        ))

        # Gap table
        gap_table = Table(
            title="Ukupno korišćenje biksni",
            show_header=True,
            header_style="bold magenta",
            box=box.MINIMAL_DOUBLE_HEAD
        )
        gap_table.add_column("Biksna", style="dim", justify="left")
        gap_table.add_column("Broj", justify="right")

        for gap, count in sorted(total_gaps_used.items(), key=lambda x: Decimal(str(x[0]))):
            gap_table.add_row(str(gap), str(count))

        self.console.print(gap_table)

        # Tire table
        tire_table = Table(
            title="Ukupno korišćenje guma",
            show_header=True,
            header_style="bold magenta",
            box=box.MINIMAL_DOUBLE_HEAD
        )
        tire_table.add_column("Guma", style="dim", justify="left")
        tire_table.add_column("Broj", justify="right")

        if tires_used:
            for tire, count in sorted(tires_used.items(), key=lambda x: Decimal(str(x[0]))):
                tire_table.add_row(str(tire), str(count))
        else:
            tire_table.add_row("Nema", "0")

        self.console.print(tire_table)

        # Knife table
        knife_table = Table(
            title="Ukupno korišćenje noževa",
            show_header=True,
            header_style="bold magenta",
            box=box.MINIMAL_DOUBLE_HEAD
        )
        knife_table.add_column("Nož", style="dim", justify="left")
        knife_table.add_column("Broj", justify="right")

        for knife, count in sorted(knives_used.items(), key=lambda x: Decimal(str(x[0]))):
            knife_table.add_row(f"[bold red]{knife}[/bold red]", str(count))

        self.console.print(knife_table)
        self.console.print("=" * 40)



# =============================================================================
# INPUT HANDLER
# =============================================================================

class ClearanceSetup(NamedTuple):
    """Rezultat unosa zazora (v2.10.0).

    Ako je zazor unet direktno, material/quality/thickness su None; ako je
    izračunat kalkulatorom, sadrže ulazne parametre radi prikaza i evidencije.
    """
    clearance: Decimal
    material: Optional[str] = None
    quality: Optional[str] = None
    thickness: Optional[Decimal] = None


class InputHandler:
    """Handles user input with validation."""

    def __init__(self, console: Console, config: Optional[AppConfig] = None):
        self.console = console
        self.config = config or DEFAULT_CONFIG

    def get_positive_decimal(self, prompt: str) -> Decimal:
        """Get a positive decimal from user."""
        while True:
            try:
                raw = self.console.input(prompt).strip().replace(',', '.')
                value = Decimal(raw)
                if value > 0:
                    return value
                self.console.print("[red]Vrednost mora biti pozitivna.[/red]")
            except InvalidOperation:
                self.console.print("[red]Nevažeći unos. Molimo unesite pozitivan broj.[/red]")

    def get_positive_int(self, prompt: str, max_value: int = 1000) -> int:
        """Get a positive integer from user.

        v2.10.0: dodata gornja granica (default 1000) — štiti od unosa
        apsurdno velikog broja dimenzija/prolaza koji bi blokirao program.
        """
        while True:
            try:
                value = int(self.console.input(prompt).strip())
                if 0 < value <= max_value:
                    return value
                if value > max_value:
                    self.console.print(f"[red]Vrednost mora biti najviše {max_value}.[/red]")
                else:
                    self.console.print("[red]Vrednost mora biti pozitivna.[/red]")
            except ValueError:
                self.console.print("[red]Nevažeći unos. Molimo unesite pozitivan ceo broj.[/red]")

    def get_ratio(self) -> int:
        """Get ratio selection from user."""
        valid = sorted(self.config.valid_ratios)
        valid_str = " ili ".join(map(str, valid))

        while True:
            try:
                ratio = int(self.console.input(f"Unesite odnos ({valid_str}): ").strip())
                if ratio in self.config.valid_ratios:
                    return ratio
                self.console.print(f"[red]Molimo unesite važeći odnos: {valid_str}.[/red]")
            except ValueError:
                self.console.print(f"[red]Molimo unesite važeći odnos: {valid_str}.[/red]")

    @staticmethod
    def _parse_knife_list(raw: str) -> List[Decimal]:
        """Parsiraj listu veličina noževa (v2.9.0).

        Separatori su zarez, tačka-zarez i razmak; decimalne veličine se
        unose TAČKOM (npr. 4.5). Zarez je isključivo separator — "4,5" znači
        dva noža (4 i 5), a nož 4.5 se unosi kao "4.5".

        Napomena: stari kod je radio replace(',', '.') POSLE split(','), što
        je bio mrtav kod koji je lažno sugerisao podršku za decimalni zarez.
        Sada je pravilo jednoznačno, prompt ga eksplicitno navodi, a izbor se
        ehuje korisniku radi potvrde interpretacije.

        Raises:
            InvalidOperation: ako neki deo nije validan broj.
        """
        parts = [p for p in raw.replace(';', ' ').replace(',', ' ').split() if p]
        return [Decimal(p) for p in parts]

    def get_selected_knives(self) -> List[Decimal]:
        """Get knife selection from user."""
        available = self.config.inventory.available_knife_sizes
        available_str = ', '.join(str(k) for k in sorted(available))
        default_str = ', '.join(str(k) for k in DEFAULT_SELECTED_KNIVES)

        while True:
            try:
                raw = self.console.input(
                    f"Unesite veličine noževa razdvojene zarezom ili razmakom; "
                    f"decimalu unesite tačkom, npr. 4.5 "
                    f"(Enter za podrazumevano [{default_str}]; dostupne: {available_str}): "
                ).strip()

                if not raw:
                    selected = list(DEFAULT_SELECTED_KNIVES)
                else:
                    selected = self._parse_knife_list(raw)

                if not selected:
                    self.console.print("[red]Nisu izabrani noževi.[/red]")
                    continue

                invalid = [k for k in selected if k not in available]
                if invalid:
                    invalid_str = ', '.join(str(k) for k in invalid)
                    self.console.print(f"[red]Veličine noževa {invalid_str} nisu dostupne.[/red]")
                    continue

                result = sorted(set(selected))
                # v2.9.0: eho interpretacije unosa — korisnik odmah vidi
                # kako je unos shvaćen (štiti od tihe pogrešne interpretacije).
                result_str = ', '.join(str(k) for k in result)
                self.console.print(f"[green]Izabrani noževi: [{result_str}][/green]")
                return result
            except InvalidOperation:
                self.console.print(
                    "[red]Nevažeći unos. Razdvojite veličine zarezom ili razmakom, "
                    "a decimalu unesite tačkom (npr. 4.5).[/red]"
                )

    def get_clearance_setup(self) -> ClearanceSetup:
        """Zazor: direktan unos ili kalkulator iz materijala/kvaliteta/debljine.

        v2.10.0: ranije mrtav kalkulator zazora (calc_clearance, QUALITY_GRADES,
        MATERIAL_LIST, round_to_nearest) je sada uvezan u tok — Enter na promptu
        otvara kalkulator.
        """
        while True:
            raw = self.console.input(
                "\nUnesite zazor u mm (npr. 0.1 ili 0.15), "
                "ili samo Enter za kalkulator zazora iz materijala/kvaliteta/debljine: "
            ).strip().replace(',', '.')

            if not raw:
                return self._clearance_via_calculator()

            try:
                value = Decimal(raw)
                if value > 0:
                    return ClearanceSetup(clearance=value)
                self.console.print("[red]Zazor mora biti pozitivan broj.[/red]")
            except InvalidOperation:
                self.console.print(
                    "[red]Nevažeći unos. Unesite pozitivan broj "
                    "ili Enter za kalkulator.[/red]"
                )

    def _clearance_via_calculator(self) -> ClearanceSetup:
        """Izračunaj zazor iz materijala, kvaliteta (R-stanja) i debljine."""
        material = self._select_material()
        quality = self._select_quality()
        thickness = self.get_positive_decimal("Unesite debljinu trake (mm): ")

        result = calc_clearance(material, quality, float(thickness))
        suggested = round_to_nearest(result.ideal_gap)

        self.console.print(Panel(
            f"Min: {result.min_gap} mm  |  Idealno: {result.ideal_gap} mm  |  "
            f"Max: {result.max_gap} mm\n"
            f"Predlog (zaokruženo na {CLEARANCE_ROUNDING_STEP}): [bold]{suggested} mm[/bold]",
            title=f"Preporučeni zazor — {material}, {quality}, {thickness} mm",
            style="bold green",
        ))

        while True:
            raw = self.console.input(
                f"Prihvatite predlog {suggested} mm (Enter) ili unesite svoju vrednost: "
            ).strip().replace(',', '.')

            if not raw:
                return ClearanceSetup(
                    clearance=suggested,
                    material=material,
                    quality=quality,
                    thickness=thickness,
                )
            try:
                value = Decimal(raw)
                if value > 0:
                    return ClearanceSetup(
                        clearance=value,
                        material=material,
                        quality=quality,
                        thickness=thickness,
                    )
                self.console.print("[red]Zazor mora biti pozitivan broj.[/red]")
            except InvalidOperation:
                self.console.print("[red]Nevažeći unos. Molimo unesite pozitivan broj.[/red]")

    def _select_material(self) -> str:
        """Izbor materijala sa liste (broj ili naziv)."""
        self.console.print("\n[bold]Materijali:[/bold]")
        for i, m in enumerate(MATERIAL_LIST, 1):
            self.console.print(f"  {i:2}. {m}")

        while True:
            raw = self.console.input("Izaberite materijal (broj ili naziv): ").strip()
            if raw.isdigit() and 1 <= int(raw) <= len(MATERIAL_LIST):
                return MATERIAL_LIST[int(raw) - 1]
            for m in MATERIAL_LIST:
                if raw.lower() == m.lower():
                    return m
            self.console.print("[red]Nepoznat materijal. Unesite broj sa liste ili tačan naziv.[/red]")

    def _select_quality(self) -> str:
        """Izbor kvaliteta / R-stanja sa liste (broj ili kod)."""
        self.console.print("\n[bold]Kvalitet (R-stanje):[/bold]")
        for i, (code, label, fr) in enumerate(QUALITY_LIST, 1):
            self.console.print(
                f"  {i}. {label}  (faktor {fr.min_factor:.2f}–{fr.max_factor:.2f})"
            )

        while True:
            raw = self.console.input("Izaberite kvalitet (broj ili kod, npr. R250): ").strip()
            if raw.isdigit() and 1 <= int(raw) <= len(QUALITY_LIST):
                return QUALITY_LIST[int(raw) - 1][0]
            code = raw.upper()
            if code in QUALITY_GRADES:
                return code
            self.console.print("[red]Nepoznat kvalitet. Unesite broj sa liste ili kod (npr. R250).[/red]")

    # v2.10.0: uklonjen get_user_inputs() — dupliralo je tok iz Application.run()
    # (drugačijim redosledom koraka) i nigde se nije pozivalo.

    def get_dimensions(self) -> List[DimensionInput]:
        """Get dimension inputs from user."""
        num = self.get_positive_int("Unesite broj dimenzija: ")
        dimensions: List[DimensionInput] = []

        for i in range(1, num + 1):
            dimension = self.get_positive_decimal(f"Unesite dimenziju {i}: ")
            num_runs = self.get_positive_int(f"Unesite broj prolaza za dimenziju {i}: ")
            dimensions.append(DimensionInput(dimension=dimension, num_runs=num_runs))

        return dimensions

    def get_manual_tire_adjustments(
        self,
        dimensions: List[DimensionInput]
    ) -> List[DimensionInput]:
        """Get manual tire adjustments for dimensions.

        v2.10.0: lokalno stanje guma se umanjuje posle svake dimenzije, pa
        default predlog za sledeću dimenziju ne računa sa već potrošenim
        gumama iz iste sesije.
        """
        adjusted: List[DimensionInput] = []
        available_tires = dict(self.config.inventory.tires)

        for idx, dim in enumerate(dimensions, start=1):
            tire_result = determine_tire_setup_with_fallback(
                dim.dimension,
                available_tires,
                self.config.tire.min_split,
                self.config.tire.max_split,
                self.config.tire.target_split,
                self.config.tire.fallback_order
            )
            auto_tires = tire_result.num_tires
            auto_size = tire_result.tire_size

            # Get tire count
            manual_count = self.console.input(
                f"Dimenzija {idx} ({dim.dimension}): Broj pneumatika (default {auto_tires}): "
            ).strip()
            try:
                num_tires = int(manual_count) if manual_count else auto_tires
                if num_tires <= 0:
                    num_tires = auto_tires
            except ValueError:
                num_tires = auto_tires

            # Get tire size
            manual_size = self.console.input(
                f"Dimenzija {idx} ({dim.dimension}): Veličina pneumatika (default {auto_size}): "
            ).strip()
            try:
                tire_size = Decimal(manual_size) if manual_size else auto_size
                if tire_size not in self.config.tire.allowed_sizes:
                    tire_size = auto_size
            except InvalidOperation:
                tire_size = auto_size

            adjusted.append(DimensionInput(
                dimension=dim.dimension,
                num_runs=dim.num_runs,
                num_tires=num_tires,
                tire_size=tire_size
            ))

            # v2.10.0: umanji lokalno stanje za izbor ove dimenzije
            used = num_tires * dim.num_runs
            if tire_size in available_tires:
                available_tires[tire_size] = max(0, available_tires[tire_size] - used)

        return adjusted


# =============================================================================
# MAIN APPLICATION
# =============================================================================

class Application:
    """Main application class."""

    def __init__(self, config: Optional[AppConfig] = None):
        self.config = config or DEFAULT_CONFIG
        self.console = Console()
        self.input_handler = InputHandler(self.console, self.config)
        self.presenter = ResultPresenter(self.console)

    def run(self) -> None:
        """Run the application."""
        try:
            # Korak 1: Osnovni parametri
            ratio = self.input_handler.get_ratio()
            clearance_setup = self.input_handler.get_clearance_setup()

            # Korak 2: Dimenzije
            dimensions = self.input_handler.get_dimensions()

            # Korak 3: Izbor noževa
            selected_knives = self.input_handler.get_selected_knives()

            user_inputs = UserInputs(
                ratio=ratio,
                clearance=clearance_setup.clearance,
                selected_knives=selected_knives,
                material=clearance_setup.material,
                quality=clearance_setup.quality,
                thickness=clearance_setup.thickness,
            )

            # Korak 4: Pun izračun sa izabranim noževima
            knives_str = ", ".join(str(k) for k in user_inputs.selected_knives)
            self.console.print(
                f"\n[bold cyan]Izračun sa noževima: [{knives_str}][/bold cyan]"
            )
            calculator = CuttingCalculator(user_inputs, self.config)
            result = calculator.calculate(dimensions)
            self.presenter.present(result, user_inputs)

            # Korak 5: Opciono ručno podešavanje pneumatika
            answer = self.console.input(
                "\nDa li su brojevi i veličine pneumatika prihvatljivi? (Y/n): "
            ).strip().lower()

            if answer in ("n", "no"):
                self.console.print("\n[bold yellow]Unesite ručno vrednosti:[/bold yellow]")
                adjusted = self.input_handler.get_manual_tire_adjustments(dimensions)
                self.console.print("\n[bold cyan]Rezultat sa ručnim podešavanjem:[/bold cyan]")
                calculator = CuttingCalculator(user_inputs, self.config)
                result = calculator.calculate(adjusted)
                self.presenter.present(result, user_inputs)
            else:
                self.console.print("[bold green]Automatski izračun je prihvaćen.[/bold green]")

        except ValidationError as e:
            self.console.print(f"[bold red]Greška validacije: {e}[/bold red]")
        except KeyboardInterrupt:
            self.console.print("\n[yellow]Prekinuto od strane korisnika.[/yellow]")
        except Exception as e:
            self.console.print(f"[bold red]Neočekivana greška: {e}[/bold red]")
            logger.exception("Unexpected error")

    def cleanup(self) -> None:
        """Clean up resources."""
        LPSolver.clear_cache()
        decimal_to_float.cache_clear()


def main() -> None:
    """Main entry point."""
    app = Application()
    try:
        app.run()
    finally:
        app.cleanup()


if __name__ == "__main__":
    main()
