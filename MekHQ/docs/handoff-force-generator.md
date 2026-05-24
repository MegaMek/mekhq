# MekHQ Force Generator integration — handoff

## What this is

Multi-phase plan to integrate MegaMek's Force Generator engine into MekHQ as a new Company Generation method (
`RULESET_BASED`). Replaces the Mek-only `companyCount x lancesPerCompany x lanceSize` math with rule-driven generation
that produces faction-correct combined-arms forces up to Regiment / Cluster / Galaxy scale.

Full plan: `C:\Users\drivi\.claude\plans\one-thing-players-are-majestic-thunder.md`

## Branch state (as of handoff)

- `mekhq/Implement-MM-Force-Generator-in-MekHQ` — 51 commits ahead of `origin/main`, fully pushed, clean
- `megamek/Implement-MM-Force-Generator-in-MekHQ` — 9 commits ahead of `origin/main`, fully pushed, clean
- mm-data — no branch for this plan yet; not touched

Dave keeps MegaMek + mm-data. This handoff is the MekHQ slice only.

## What's done on mekhq

**Phase 1 — Plumbing.** All 5 originally-planned classes plus 8 more, in
`MekHQ/src/mekhq/campaign/universe/companyGeneration/ratgen/`: `CompanyGenerator`, `ForceDescriptorWalker`,
`CrewDescriptorAdapter`, `MultiCrewAssembler`, `PersonnelRoleResolver`, `RulesetEngineBootstrap`, `RulesetRankAssigner`,
`RankAssigner`, `ForceDescriptorSnapshot`, `FormationIconBuilder`, `SupportPersonnelAssigner` + `Calculator` +
`Generator`. `CompanyGenerationMethod.RULESET_BASED` wired through `CompanyGenerationDialog`.

**Phase 2 — Non-Mek + multi-crew.** `MultiCrewAssembler` handles drivers, gunners, squads, vessels.
`PersonnelRoleResolver` covers every `UnitType`.

**Phase 3 — Regiment scale + combined-arms + faction-aware echelon.** `RulesetRankAssigner` walks the tree and assigns
ranks via the faction's rank system. `FormationIconBuilder` classifies icons by dominant unit type with weight-class
breakpoints (StratOps).

**UI rebuild (beyond plan scope).** `CompanyGenerationPane` with 4 tabs — Setup, Force Generator (embeds
`ForceGeneratorOptionsView` from megamek), Spares, Other. Styled-component library mirroring `CampaignOptions`. Progress
dialog + long-generation warning + hang diagnostics. Pre-generation warnings combined into one dialog.

**Hardening (beyond plan scope).** Concurrent modification fixes (Warehouse, Campaign, `getServiceableUnits` snapshot).
EDT dispatch for `ReportEvent` + `UnitNewEvent`. Spare parts warehouse stocking during generation. Support personnel
pipeline (CamOps Stage 7e) with coverage %, assistant, and tech-assignment UI.

**Other.** Force-ID collision fix in `Formation.getFullMMName()`. Command Center Unit Weight row. Full TOE breadcrumb in
Formation columns by default. Faction picker wiring (seed from campaign, override on OK). Rank system swap on generated
Persons.

## What's outstanding on mekhq

### 1. Phase 4 — custom ruleset dir + fallback confirmation

**Blocked on megamek-side API** Dave needs to land first:

- `Ruleset.directory` becomes `List<String>` (currently `private static final String`)
- `Ruleset.addRulesetDirectory(String)` method
- `Ruleset.loadData()` iterates the list

**MekHQ slice (after that ships):**

- In `RulesetEngineBootstrap.ensureLoaded(year)`, call
  `Ruleset.addRulesetDirectory(MHQConstants.USER_DATA_DIR + "/forcegenerator/faction_rules")` before
  `Ruleset.loadData()`.
- In `CompanyGenerator.generate(campaign)`, before `Ruleset.findRuleset(fd).processRoot(...)`, detect fallback-only
  ruleset state and surface a confirmation: *"No specific ruleset for X; fall back to generic IS rules?" [OK / Cancel]*.
- Fallback detection: `Ruleset.findRuleset` walks `FactionRecord.getParentFactions()` then drops to `IS_GENERAL_KEY`.
  Need to compare the returned ruleset's faction code against the requested one, or get Dave to add a helper like
  `Ruleset.isDefaultsOnly()`.

### 2. MUL parallel stream — `<forces>` block import

**Blocked on megamek-side writer** Dave needs to land first:

- New overload `EntityListFile.saveTo(File, List<Entity>, ForceDescriptor rootForce)`
- `<forces>` block emitted before entities, each `<force>` element carrying id / name / parentId / echelon /
  echelonName / faction / unitType / formationType / flags
- Each `<entity>` gets a `forceId` attribute
- Tab 6 right-click "Export as MUL" wired to the new overload

**MekHQ slice (after writer ships):**

- `MekHQ/src/mekhq/campaign/unit/MULParser.java` — read the `<forces>` block, build a map of
  `id -> ForceNodeImport(name, parentId, echelon, faction, unitType)`.
- Walk the map to build a MekHQ `Force` tree under the user-selected parent (or top-level).
- For each entity, look up `forceId`, create `Unit`, assign to matching `Force`.
- Generate `Person`s for crews via the existing personnel generator, seeded from any `<pilot>` data in the MUL.
- Fallback path: if no `<forces>` block, parse `forceString` for hierarchy (current behavior).

### 3. Verification matrix — none run yet

| # | Scenario                                 | Status     | Blocked on            |
|---|------------------------------------------|------------|-----------------------|
| 1 | FedSuns BattleMek Battalion 3025 Regular | unverified | nothing — can run now |
| 2 | LC Combined-Arms Regiment 3050 Veteran   | unverified | nothing — can run now |
| 3 | Clan Wolf Cluster 3050 Elite             | unverified | nothing — can run now |
| 4 | MUL ToE round-trip                       | unverified | parallel stream       |
| 5 | Old MUL backward-compat                  | unverified | parallel stream       |

Per scenario: open TO&E (echelon names + counts), Hangar (unit types), Personnel (role distribution), Finances (
cash/parts scale with leaf count).

### 4. Loose ends from prior work

- **Specific Unit picker** — TODO marker in megamek commit `1ab127fbf9`. May need a MekHQ consumer when Dave lands the
  picker. No code to write yet.
- **Weight-class assignment bug** — Prior session diagnosed Force Generator pinning Clan-only models (e.g. "Victor C")
  to Inner Sphere factions. megamek-side fixes landed in `622e1af8f8`; the root cause of the cross-faction model pin may
  still be live. Worth a sanity-check after Phase 4. Not blocking.

## Key files

```
MekHQ/src/mekhq/campaign/universe/companyGeneration/ratgen/
  CompanyGenerator.java                <- orchestrator; Phase 4 fallback dialog lands here
  RulesetEngineBootstrap.java          <- Phase 4 addRulesetDirectory() call lands here
  ForceDescriptorWalker.java           <- tree -> MekHQ Formations
  MultiCrewAssembler.java              <- per-unit-type crew assembly
  PersonnelRoleResolver.java           <- UnitType -> PersonnelRole
  RulesetRankAssigner.java             <- rank tier per Formation depth
  FormationIconBuilder.java            <- StratOps icon picker
  SupportPersonnelGenerator.java       <- CamOps support personnel

MekHQ/src/mekhq/campaign/universe/enums/CompanyGenerationMethod.java
MekHQ/src/mekhq/campaign/universe/companyGeneration/CompanyGenerationOptions.java
MekHQ/src/mekhq/gui/dialog/CompanyGenerationDialog.java
MekHQ/src/mekhq/gui/panels/CompanyGenerationOptionsPanel.java
MekHQ/src/mekhq/campaign/unit/MULParser.java          <- parallel-stream reader lands here
MekHQ/src/mekhq/campaign/force/Formation.java
MekHQ/src/mekhq/campaign/force/FormationLevel.java
```

## Cross-repo coordination

The two outstanding code items both need Dave to ship megamek-side API first:

| MekHQ item              | Blocking megamek change                                    |
|-------------------------|------------------------------------------------------------|
| Phase 4 fallback dialog | `Ruleset.addRulesetDirectory()` + `List<String>` directory |
| MUL `<forces>` import   | `EntityListFile.saveTo` overload + tab 6 export wiring     |

The MekHQ-side reader and fallback dialog can be drafted against the planned API shape before Dave's side ships — just
sync on the wire format (XML attribute names for `<forces>`, exact method signature for `addRulesetDirectory`) before
merging.

## Project rules (CLAUDE.md highlights)

- **Never commit** — user does that manually
- No Unicode in code/logs (Windows crash risk)
- No trademark words in Java source: `Mech -> Mek`, `BattleMech -> BattleMek`, `MechWarrior -> MekWarrior` (comments and
  identifiers count; Markdown exempt)
- Use `UIUtil.scaleForGUI()` for dialog dimensions, not raw pixels
- Test save/load when creating or modifying Record classes — XStream 1.4 can't deserialize Records, needs a
  `SerializationHelper` converter
- Check `MekHQ/docs/issues/` for existing per-issue notes before starting

Full rules: `D:\MegaMek Projects\CLAUDE.md` and `D:\MegaMek Projects\mekhq\CLAUDE.md`.
