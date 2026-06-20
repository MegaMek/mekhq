# PR #9417 - Latent ClassCastException / NPE sites from flattening the Mission inheritance chain

**Context:** PR #9417 ("Project Mission Overhaul") flattens the old
`AtBContract -> Contract -> Mission` inheritance chain. After the change, `Mission`, `Contract`, and
`AtBContract` each extend a new common base class `AbstractMissionTransition` directly.

**Key consequence:** an `AtBContract` is **no longer** an `instanceof Contract`, and a `Contract` is **no
longer** an `instanceof Mission`. Any pre-existing code that used the old "is-a" relationships - especially the
idiom `boolean isContract = !(mission instanceof Mission)` followed by a `(Contract) mission` cast - now throws
`ClassCastException` when handed an `AtBContract`.

This document records the risk sites found while reviewing the PR so they can be tracked and fixed. It
accompanies the disabled characterization test
`MekHQ/unittests/mekhq/campaign/mission/MissionFlatteningLatentBugTest.java` and the green regression tests
listed at the bottom.

---

## Confirmed defects (new `ClassCastException` introduced by the flattening)

All four share the same root cause: a `(Contract) mission` cast guarded only by `!(mission instanceof Mission)`.
An `AtBContract` is not a `Mission`, so it passes the guard, then fails the cast. In every case the methods
being called (`addSalvageByUnit`, `addSalvageByEmployer`, `getSalvagePercent`, `getSalvagedByUnit`,
`getSalvagedByEmployer`, `isSalvageExchange`) now live on `AbstractMissionTransition`, so **the cast is
unnecessary** - calling the method directly on the `AbstractMissionTransition` reference both fixes the bug and
simplifies the code.

### 1. `Campaign.initAtB()` - re-initializing AtB throws CCE (HIGH)
- **File:** `MekHQ/src/mekhq/campaign/Campaign.java:8620`
- **Code:**
  ```java
  if (!(mission instanceof Mission)) {
      missionEntry.setValue(new AtBContract((Contract) mission, this));
  }
  ```
- **Trigger:** any campaign that already contains an `AtBContract` when `initAtB(false)` runs (e.g. enabling
  AtB on a campaign that already has AtB contracts). The `AtBContract` is not a `Mission`, enters the branch,
  and `(Contract) mission` throws.
- **Suggested fix:** skip entries that are already `AtBContract`, e.g.
  `if (!(mission instanceof Mission) && !(mission instanceof AtBContract))`, or only convert plain `Contract`s
  via `if (mission instanceof Contract contract) { ... new AtBContract(contract, this) ... }`.
- **Covered by:** `MissionFlatteningLatentBugTest.initAtBDoesNotClassCastAnAlreadyStoredAtBContract` (disabled).

### 2. CamOps salvage resolution throws CCE for every AtB contract (HIGH / SEVERE)
- **File:** `MekHQ/src/mekhq/campaign/mission/camOpsSalvage/CamOpsSalvageUtilities.java`
- **Guard:** line 221 `boolean isContract = !(mission instanceof Mission);`
- **Cast sites:** lines **238, 261, 272, 273, 277, 293** - all `((Contract) mission).<salvageMethod>(...)`.
- **Trigger:** post-scenario salvage resolution under CamOps rules. In an AtB campaign `mission` is an
  `AtBContract`, so `isContract` is `true` and the first `(Contract) mission` cast throws. This breaks salvage
  for AtB contracts.
- **Note:** the same method already uses the correct pattern (`mission instanceof AtBContract atbContract`) on
  lines 216 and 230 for `getDeploymentTime`/`getRepairLocation`; only the salvage-money calls use the unsafe
  cast.
- **Suggested fix:** drop the `(Contract)` casts and call `mission.addSalvageByUnit(...)` etc. directly (the
  methods are on `AbstractMissionTransition`).

### 3. CamOps salvage picker dialog throws CCE for AtB contracts (HIGH)
- **File:** `MekHQ/src/mekhq/gui/dialog/camOpsSalvage/SalvagePostScenarioPicker.java`
- **Guard:** line 258 `boolean isContract = !(mission instanceof Mission);` (line 259 already calls
  `mission.getSalvagePercent()` directly - good - which makes the later casts clearly redundant).
- **Cast sites:** lines **265, 266, 268, 270** - `((Contract) mission).<salvageMethod>()`.
- **Trigger:** opening the post-scenario salvage picker for an AtB contract.
- **Suggested fix:** remove the `(Contract)` casts; call directly on `mission`.

### 4. Resolve-scenario wizard salvage read throws CCE for AtB contracts (HIGH)
- **File:** `MekHQ/src/mekhq/gui/dialog/ResolveScenarioWizardDialog.java:1847-1848`
- **Code:**
  ```java
  if (tracker.getMission() instanceof Mission || tracker.usesSalvageExchange()) {
      return;
  }
  salvageEmployer = ((Contract) tracker.getMission()).getSalvagedByEmployer();
  salvageUnit     = ((Contract) tracker.getMission()).getSalvagedByUnit();
  ```
- **Trigger:** resolving a scenario for an AtB contract that is not a salvage-exchange contract.
- **Suggested fix:** call `tracker.getMission().getSalvagedByEmployer()` / `...getSalvagedByUnit()` directly.

---

## Reviewed and found SAFE (no change needed - listed to avoid re-flagging)

- **`MissionViewPanel` `fillStatsContract()` / `fillStatsAtBContract()`** -
  `MekHQ/src/mekhq/gui/view/MissionViewPanel.java:252` and `:533`. The dispatch at lines 174-177 routes
  `AtBContract` to `fillStatsAtBContract()` (`(AtBContract)` cast) and only falls through to
  `fillStatsContract()` (`(Contract)` cast) for a non-`AtBContract`, non-`Mission` object - i.e. a plain
  `Contract`. Both casts are guarded correctly.
- **Casts *to* `AtBContract`** that are guarded by an `instanceof AtBContract` check or rely on the
  pre-existing "an AtB scenario belongs to an AtB contract" assumption (unchanged by the flattening):
  `AtBGameThread.java:736`, `AtBScenario.java:2164`, `ScenarioObjectiveProcessor.java:528/540/552`,
  `BriefingTab.java:792/832/914`, `CustomizeScenarioDialog.java:514/528`, `TheatreOfWarAwards.java:135`.
  These should still be sanity-checked during playtesting but are not newly broken by the `(Contract)` issue.

---

## Pre-existing NPE surfaces worth noting (not introduced by the flattening, but now centralized)

The following `AbstractMissionTransition` getters can return `null`; callers that dereference the result will
NPE. They are not new in this PR but are now the single base-class surface for all mission types:
`getSystem()`, `getJumpPath(Campaign)`, `getEmployerFaction()`, `getEnemy()`, `getStartDate()`,
`getEndingDate()`, `getStratConCampaignState()`, `getRoutedPayout()`, `getEnemyMercenaryEmployer()`.
For example `getEmployerNameFromFaction(int)` calls `getEmployerFaction().getFullName(year)` and will NPE if
the employer code does not resolve to a faction. The green tests cover the safe degradation paths
(`getSystemName` fallback, zeroed travel/transport costs, `getMonthsLeft`/`getMonthlyPayOut` guards).

---

## Tests added with this report

Green regression tests (lock in the intended post-flattening behavior):
- `MekHQ/unittests/mekhq/campaign/mission/MissionInheritanceFlatteningTest.java` - type-identity invariants,
  `MissionEvent.isContract()` semantics, and the `Mission` vs `Contract` polymorphic differences in
  `isActiveOn` / `calculateContract`.
- `MekHQ/unittests/mekhq/campaign/mission/AbstractMissionTransitionBehaviorTest.java` - base-class financial
  arithmetic, salvage-percentage rounding, and null-tolerant (no-system / no-dates) paths.
- `MekHQ/unittests/mekhq/campaign/CampaignMissionContractAccessorsTest.java` - the `Campaign` accessors
  (`getActiveContracts`, `getFutureContracts`, `getAtBContracts`, `getActiveAtBContracts`,
  `getCompletedAtBContracts`, `getActiveMissions`, `getFutureAtBContracts`, `getMission`) against a campaign
  seeded with every mission flavour.

Disabled characterization test (fails today, enable as the defect is fixed):
- `MekHQ/unittests/mekhq/campaign/mission/MissionFlatteningLatentBugTest.java` - defect #1 above.
