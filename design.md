# Planetary Data Editor — Design

Status: Draft / Brainstorm
Branch: `vc/planetary-editor` (mekhq)
Scope: MekHQ-side feature; no changes to canonical MM data shipped in `mm-data`.

---

## 0. Implementation Status (April 2026)

The MVP and most of Tier 2 are now implemented on `vc/planetary-editor`. Quick
status summary (full per-feature markers are inline in §4 and §7):

**Shipped**

- GM-only `PlanetarySystemEditorDialog` reachable from the main menu (and
  re-entry safe — multiple opens reuse a single dialog).
- Read-only system list with searchable filter, sort by name/X/Y, "only systems
  with unsaved changes" / "only systems with overrides" filters, and a
  visible `[U]` / `[O]` badge per row with a hover tooltip listing the
  changed fields.
- Per-planet **event timeline editor** (add / update / delete / duplicate /
  clear) with faction multi-pick (`Factions` registry + date filtering),
  HPG/socio-industrial dropdowns, source/version metadata, and `clearEventFields`.
- Per-planet **static-field editor** (name, type, gravity, diameter, day/year
  length, temperature, pressure, atmosphere, composition, % water, life form,
  small moons, ring, description) split into **Gameplay** (red ⚠ banner —
  affects maintenance / AtB / StratCon) and **Flavor** (display only) sub-sections.
  Numeric fields are range-checked (e.g. gravity 0–10, temperature -300–1000).
- Per-planet **landmass and satellite (moon) editors** as inline tables in the
  properties panel — add / remove / inline-edit name, capital, and size.
  Treated as pure flavor (zero gameplay impact across mekhq/megamek/megameklab),
  diffs surfaced in Review Changes.
- **System-level event timeline editor** (Pass H) — dedicated tab with an
  inline-editable Date / Nadir Station / Zenith Station / Source / Version
  table for recharge-station flags, plus a red gameplay-warning banner.
  These flags drive StratOps recharge time and command-circuit costing.
- Per-system **Revert Changes** (undo in-memory edits) and **Delete Override**
  (remove the disk file) split into two clearly-scoped buttons.
- Per-planet **Revert Planet** restoring both events and static fields from
  the baseline.
- **Validation** panel in the dialog (errors + warnings live, blocks save on
  errors) wrapping `SystemValidator` and `cleanupSystems` rules.
- **Save** writes to `<userDir>/data/universe/planetary_systems/edits/<id>.yml`
  with a timestamped backup of any prior file.
- **Import / Export overrides** — zip bundle of all `.yml` files in the user
  edits directory, with per-file conflict prompts (Overwrite / Skip / Cancel)
  and zip-slip path-traversal guard on import.
- Keyboard shortcuts: `Ctrl+S` (save), `Ctrl+F` (focus search),
  `Delete` / `Ctrl+D` (delete event in the table).
- Plumbing from Phase 0: shared `PlanetarySystemYamlIO.createMapper()`,
  `SourceableValueSerializer` symmetric with the deserializer including
  `version` round-trip, `PlanetarySystemValidator`,
  `Systems.saveUserSystem` / `deleteUserSystem`.
- Restart-required save messaging; same-session edits are not yet refreshed
  in the rest of MekHQ.

**Deliberately dropped**

- **System-level field editing** (sucsId, system `name`). `name` is vestigial
  (only used as id fallback at load); `sucsId` is a Sarna catalog tag with no
  gameplay impact and only triggers the duplicate-id validator. Editing it was
  more footgun than feature.
- **Density** (`Planet.getSourcedDensity`) — already
  `@Deprecated(forRemoval = true)` on `Planet`.
- Editing the icon library — out of MVP scope.

**Missing / not yet implemented**

- Same-session refresh (`UniverseEditService`) — design §5.2 option 2.
- Campaign-scoped overlay — design §5.2 option 3.
- Wizards: transfer-ownership, create-new-system, clone-from-template (§4.3).
- Undo/redo within the dialog and a confirmation diff before save (§4.2).
- Live in-dialog `PlanetarySystemMapPanel` preview tab (§5.5).
- HPG / recharge plausibility warnings, coordinate-collision warnings (§4.2).
- Auto-event from contract resolution (§4.3 stretch).
- Round-trip golden test across every canon file (§5.4) — only the
  `PlanetarySystemChangeSummaryTest` is in place so far.

---

## 1. Motivation

Players running long campaigns frequently want to deviate from the canon
universe — a planet changes hands after a successful campaign mission, a homebrew
faction takes a system, an in-fiction event flips an HPG, a player wants to add a
custom personal homeworld, etc. Today the only way to express this is to hand-edit
YAML files under `data/universe/planetary_systems` (or the user-dir equivalent),
which is error-prone, undocumented for end users, and easy to break with malformed
YAML.

A community attempt exists as a standalone Shiny app
(<https://aaron-gullickson.shinyapps.io/planetary_system_editor/>) but it is
abandoned, lives outside the game, and cannot leverage MekHQ's in-memory model,
faction date-awareness, or campaign context.

This proposal is for an in-game editor inside MekHQ.

---

## 2. Does it belong inside MekHQ?

**Yes — with caveats.** Pros:

- The data model is already loaded and parsed by MekHQ at startup
  (`Systems.loadDefault()` in [Systems.java](MekHQ/src/mekhq/campaign/universe/Systems.java#L228)).
  We have `PlanetarySystem`, `Planet`, `PlanetaryEvent`, `PlanetarySystemEvent`,
  `SourceableValue<T>` and matching Jackson YAML deserializers in place
  (see [Planet.java](MekHQ/src/mekhq/campaign/universe/Planet.java),
  [PlanetarySystem.java](MekHQ/src/mekhq/campaign/universe/PlanetarySystem.java),
  [SourceableValue.java](MekHQ/src/mekhq/campaign/universe/SourceableValue.java)).
- MekHQ already supports per-user override of universe data via the user dir:
  `Systems.loadDefault()` calls `load(userDir + PLANETARY_SYSTEM_DIRECTORY_PATH)`
  after the canonical data, and the loader does a straight
  `systemList.put(system.getId(), system)` ([Systems.java:367](MekHQ/src/mekhq/campaign/universe/Systems.java#L367)),
  meaning a user-dir file with the same `id` cleanly replaces the canon entry.
  We get persistence "for free" by writing edited systems to that location.
- MekHQ has the contextual data the Shiny editor lacks: `Factions` registry
  with date-aware membership, the campaign's current date, the
  `InterstellarMapPanel` for visual feedback, faction colors, HPG link
  computation, and validators (e.g. `cleanupSystems()`).
- GM-mode gating is a well-established pattern (`campaign.isGM()`,
  used in `BriefingTab`, `PersonnelTableMouseAdapter`, etc.).
- Edits are observable in-game after restart/reload. Same-session live updates
  are deliberately out of scope for the MVP because they require invalidating
  several object references and derived caches. We may add live updates later
  if benchmarking shows the refresh path is fast enough for batch editing.

Caveats / things to be careful about:

- The `Systems` singleton is **process-global** and shared with all campaigns
  loaded in the JVM session. Edits made in one campaign affect any other
  campaign opened in the same session unless we scope by campaign or reload.
- Canon data should never be silently mutated on disk. All persistence
  must go to the user dir (or a campaign-scoped overlay), and we must clearly
  mark user-edited systems in the UI.
- MegaMekLab also reads the same data; user-dir overrides will follow them
  there, which is probably desirable but worth calling out.
- The data format is rich (every leaf can be a `SourceableValue` with
  `source` / `version` / `value`, history is event-sourced, planets nest
  satellites and landmasses). A naive form editor will struggle; we need a
  thoughtful UI.

**Recommendation:** Build it inside MekHQ as a GM-mode-only tool, with
write-through to the user dir (not the canonical data), and a clear "this
system has been customized" indicator everywhere a system is shown.

---

## 3. Data Model Recap (so the design is grounded)

A planetary system YAML (see e.g.
[`MaxiesPlanet.yml`](../mm-data/data/universe/planetary_systems/canon_systems/MaxiesPlanet.yml))
contains:

- **System-level fixed data:** `id`, `sucsId`, `xcood`, `ycood`,
  `spectralType`, `primarySlot`.
- **System-level event timeline (`event:`):** dated changes to
  `nadirCharge` / `zenithCharge` / HPG presence at recharge stations, etc.
- **Per-planet block (`planet:` list):** physical attributes
  (`type`, `orbitalDist`, `sysPos`, `pressure`, `atmosphere`, `composition`,
  `gravity`, `diameter`, `density`, `dayLength`, `yearLength`, `temperature`,
  `water`, `lifeForm`, `icon`), nested `landmass` and `satellite` lists,
  and a per-planet `event:` timeline carrying `population`, `socioIndustrial`,
  `faction`, `hpg`, `name` rename, capital changes, etc.
- **Sourcing wrapper:** any leaf may be either a bare value
  (e.g. `pressure: STANDARD`) or a `{source, version, value}` triple
  (e.g. `name: { source: canon, value: Maxie's Planet }`). The custom
  `SourceableValue.SourceableValueDeserializer` handles both shapes
  ([SourceableValue.java:79](MekHQ/src/mekhq/campaign/universe/SourceableValue.java#L79)).

Implications for the editor:

- The natural unit of edit is **(planet, date) -> event-patch**, not
  "edit the planet". A faction change in 3050 is a new entry in the
  planet's event list, not a mutation of base data.
- We must preserve the `SourceableValue` wrapper and not strip canon
  attribution. Edits the user makes should be tagged with a new source
  (e.g. `source: "user:<campaign-name>"`) so they are visually
  distinguishable and round-trippable.
- We must preserve the optional `version` metadata on sourced values. The
  current `SourceableValue` model stores `source` and `value` but not
  `version`; Phase 0 should add version support before any save path exists so
  SUCS/canon metadata is not silently discarded.
- We do **not** currently have a YAML *writer* — only a reader. The same
  Jackson `ObjectMapper` with `YAMLFactory` can write, but we will need
  matching `@JsonProperty` ordering and a serializer for `SourceableValue`
  that emits the bare-value shorthand when there is no source.

---

## 4. Feature Set

Tiered so we can ship in slices.

### 4.1 Tier 1 — MVP "Just an editor" (event-level edits)

The 80% use case: "I want to change who owns this planet starting on this
date." Read the existing system, append/edit an event in its timeline,
write the modified file out to user dir.

- **Entry point.** Right-click a system on the `InterstellarMapPanel`
  (or a button in `PlanetarySystemMapPanel`) -> "Edit System (GM)".
  Menu item only shown when `campaign.isGM()`.
  Status: ✅ **Done** — main-menu entry under GM tools is wired and
  GM-gated; the right-click `GM Mode > Edit System (GM)...` entry on the
  interstellar map opens the editor pre-selected on the clicked system
  via `PlanetarySystemEditorDialog.selectSystemById`.
- **System overview pane.** Read-only summary of current state at the
  campaign date: name, coordinates (locked), star type, primary planet,
  faction(s), HPG, recharge stations, population.
  Status: ✅ **Done** (Details tab in the dialog, formatted at the campaign
  date).
- **Per-planet event timeline.** Sortable table of events
  (date / faction / population / socioIndustrial / hpg / message).
  Add / edit / delete rows. Date-picker for dates. Faction picker
  validates against `Factions.getInstance()` and filters to factions
  active on the selected date (see §4.3).
  Status: ✅ **Done** — also supports duplicate, clear-all, and
  `clearEventFields` markers.
- **System-level event timeline.** Same UX for nadir/zenith charge changes.
  Status: ✅ **Done in Pass H** — added a dedicated **System Events** tab
  in the editor with an inline-editable table (Date / Nadir Station / Zenith
  Station / Source / Version), Add/Remove buttons, a red gameplay-warning
  banner (these flags affect StratOps recharge time and command-circuit
  costing), and diff lines in Review Changes.
- **Save.** Writes the *full* system YAML to
  `<userDir>/data/universe/planetary_systems/edits/<id>.yml`. The
  user-dir loader will pick it up on next load and override the canon
  copy. The MVP shows a clear "restart/reload required" message after save.
  Same-session visibility is a later optional feature (see §5.3).
  Status: ✅ **Done** — `Systems.saveUserSystem` writes to the edits
  directory with timestamped backups; restart-required banner is shown.
- **Revert.** Delete the user-dir file and reload that system from canon.
  Status: ✅ **Done** — split into two actions: **Revert Changes**
  (in-memory) and **Delete Override** (removes the disk file). A
  per-planet **Revert Planet** also restores both events and static fields.
- **Diff badge.** On the interstellar map, edited systems get a small
  marker (e.g. an outline ring or a "*" suffix on hover) so the player
  always knows "this is not canon".
  Status: ✅ **Done** — systems with a user-dir override file get a thin
  cyan outline ring on the `InterstellarMapPanel` (backed by a cached
  `Systems.hasUserOverride` lookup), and `[U]` / `[O]` badges plus a
  per-field tooltip are shown on the dialog's system list.

### 4.2 Tier 2 — Editor with validation and helpers

What separates this from "open notepad on the YAML":

- **Faction validation.** When picking a faction for a date:
  - List is filtered to factions whose start/end years bracket that date
    (Faction has era-aware methods already).
  - Multi-select is supported (matches the YAML `value: [LA, FC]` shape).
  - Highlight unusual choices (Clan faction in 2900, etc.) as warnings,
    not errors — players doing alt-history will deliberately pick these.
  Status: ✅ **Done** for the date filter and multi-select; "unusual
  choice" warnings are not implemented (factions are simply omitted from
  the date-filtered list rather than warned about).
- **Planet-data validation.** Numeric bounds for `gravity`, `pressure`,
  `temperature`, `water%`. Enum-backed dropdowns for `type`
  ([PlanetaryType](MekHQ/src/mekhq/campaign/universe/enums/PlanetaryType.java)),
  `atmosphere`, `lifeForm`, `pressure`. Reuse the existing enums.
  Status: ✅ **Done** — Properties tab is split into a **Gameplay** group
  (gravity, temperature, pressure, atmosphere; flagged with a red ⚠
  banner because these affect maintenance / AtB / StratCon) and a
  **Flavor** group (display-only fields). All ranges enforced; pressure
  is the megamek `Atmosphere` enum and atmosphere composition is the
  mekhq `Atmosphere` enum.
- **Socio-industrial picker.** Dedicated 5-column widget for
  `Tech-Industry-RawMat-Output-Agriculture` ratings using
  `SocioIndustrialData` enum values, instead of free-text "D-C-A-D-D".
  Status: ⚠ **Partial** — the event editor uses the rating dropdowns
  per slot, but a single "5-column widget" surfacing the resolved
  current rating is not implemented.
- **Sanity checks on save.** Run the same rules as
  `Systems.cleanupSystems()` ([Systems.java:373](MekHQ/src/mekhq/campaign/universe/Systems.java#L373))
  — coordinates present, star present, primarySlot <= planets.size().
  Show errors before allowing save.
  Status: ✅ **Done** via `PlanetarySystemValidator` + the validation
  panel (errors block save, warnings do not).
- **HPG / recharge plausibility.** Warn if you set HPG=A on a system
  whose current owner has no HPG-A historically; warn (don't block) if
  enabling a nadir/zenith station before the year jump-stations were
  invented.
  Status: ❌ **Not done**.
- **Coordinate collision warning.** Reuse `getNearbySystems` to warn if
  the coordinates you set for a brand-new system put it `<1 ly` from
  another (the same warning `logVeryCloseSystems()` already emits).
  Status: ❌ **Not done** — coordinates are not editable in MVP, so this
  only matters once the create-new-system wizard lands.
- **Preview in-context.** Live-update the
  [`InterstellarMapPanel`](MekHQ/src/mekhq/gui/InterstellarMapPanel.java)
  faction-hex shading and the
  [`PlanetarySystemMapPanel`](MekHQ/src/mekhq/gui/PlanetarySystemMapPanel.java)
  view as the user edits.
  Status: ❌ **Not done** — restart-required is the explicit MVP
  trade-off; same-session refresh is deferred to Phase 4.
- **Undo / redo within the dialog**, plus a confirmation diff before save
  ("3 events added on Sati, 1 faction change on Morrighan") so the user
  can audit what they're about to write.
  Status: ⚠ **Partial** — there is no undo/redo stack, but the **Review
  Changes** action shows a per-system human-readable diff
  (`PlanetarySystemChangeSummary`) before save.

### 4.3 Tier 3 — Helpers / quality of life

- **"Transfer ownership" wizard.** One-click action: pick a date,
  pick a new faction, optionally apply to a radius of nearby systems
  (post-campaign conquest). Creates the right faction events on every
  affected planet's primary.
  Status: ❌ **Not done**.
- **Create-new-system wizard.** Required fields only (id, x, y,
  spectral type, one planet with name+type+sysPos). Everything else
  defaulted; user can fill in later.
  Status: ❌ **Not done**.
- **Clone-from-template.** "New system based on Galatea" — copies the
  canonical structure as a starting point.
  Status: ❌ **Not done**.
- **Bulk import / export.** Zip up all user edits into a sharable bundle
  (e.g. `myCampaignUniverse.zip`) — the loader already accepts `.zip`
  files in the planetary systems directory ([Systems.java:319](MekHQ/src/mekhq/campaign/universe/Systems.java#L319)).
  Lets players share alt-universes with each other.
  Status: ✅ **Done** — `PlanetarySystemYamlIO.exportOverrides` /
  `importOverrides` write/read a zip of every file under `edits/`, with
  per-file conflict prompts (Overwrite / Skip / Cancel) and a zip-slip
  guard on import. Buttons live in the editor dialog's button bar.
- **Campaign-scoped vs. user-scoped overrides.** Optional toggle on save:
  "Apply to this campaign only" (stored inside the campaign save) vs.
  "Apply to all my campaigns" (written to user dir). Implementing
  campaign-scoped overrides requires a new overlay layer in `Systems`
  (see §5.2) and is the most invasive piece of work in the design.
  Status: ❌ **Not done** — all overrides are user-scoped (option 1).
- **Search / filter.** "Show me all systems where I have edits", "Show
  me all systems owned by faction X on date Y".
  Status: ⚠ **Partial** — text search and "only systems with unsaved
  changes" / "only systems with overrides" filters are present; "owned
  by faction X on date Y" is not.
- **Auto-event from gameplay (stretch).** Hook into
  `AtBContract` resolution: if the GM marks a contract as
  "ownership-changing", offer to auto-create the matching faction event
  on the contract planet for the contract's end date.
  Status: ❌ **Not done**.

### 4.4 Explicitly out of scope (for now)

- Editing star coordinates of existing canon systems. (Allowed for
  user-created systems; for canon systems we lock coords to keep jump
  paths and shopping radii predictable, but make this overridable
  with a confirm dialog.)
  Status: ✅ **Respected** — coordinates are not editable in MVP.
- Editing the icon image library itself.
  Status: ✅ **Respected**.
- Multi-user / shared-campaign sync. Sharing happens via the export
  bundle, not a server.
  Status: ✅ **Respected** — the only sharing path is the zip export.
- **System-level fields** (`name`, `sucsId`).
  Status: ➖ **Added to out-of-scope post-design.** `name` is vestigial
  (only used as id fallback at load) and `sucsId` is a Sarna catalog tag
  that triggers the duplicate-id validator if mis-set; neither has any
  gameplay effect, so editing them was deliberately dropped during Pass C.

---

## 5. Architecture

### 5.1 Persistence model

```
<canon>      mm-data/data/universe/planetary_systems/canon_systems/*.yml
<user>       <userDir>/data/universe/planetary_systems/...        (existing)
<edits>      <userDir>/data/universe/planetary_systems/edits/*.yml (new convention)
```

Load order is canon -> user -> (new) edits, with last-write-wins on `id`.
We isolate editor output under `edits/` so the user can see at a glance
what the editor produced vs. what they hand-wrote.

### 5.2 In-memory model

Three options, in increasing order of scope:

1. **Restart-required user-dir override (MVP).** Save the edited system YAML
   to the user-dir override path and require the user to restart/reload before
   MekHQ consumes it. This keeps the implementation small, avoids cache
   invalidation risk, and makes batch edits cheap: a GM can edit several
   systems and pay the reload cost once.
2. **Mutate the global universe with a refresh service (future).** Apply the
   edited `PlanetarySystem` to `Systems`, rebuild its indexes, and publish a
   universe-data-changed event so open UI and campaign services can invalidate
   caches. This is still process-global, so any open campaign sees the same
   edited universe.
3. **Per-campaign overlay (clean).** Add an
   `Optional<Map<String, PlanetarySystem>> overlay` to `Campaign`, and
   a `Campaign.getSystem(String id)` accessor that checks the overlay
   before falling back to `Systems.getInstance()`. All planet/system
   reads inside MekHQ would need to migrate to that accessor over time.
    *Downside:* large diff, touches many files; do this only if
   campaign-scoped overrides are committed to.

Recommend **starting with option 1** for the MVP. Move to option 2 only after
we benchmark the refresh cost and confirm it will not make multi-planet editing
feel clunky. Move to option 3 only if/when campaign-scoped overrides ship.

### 5.3 Ownership propagation and restart behavior

Changing a planet's owner is not just display data. MekHQ reads current
ownership in contract generation, map rendering, markets, finances,
personnel origin/academy filtering, and some scenario force-generation logic.
The behavior splits into three categories.

**Honored after restart/reload:**

- The interstellar map asks each visible `PlanetarySystem` for
  `getFactionSet(date)`, so faction coloring follows edited ownership once the
  map has an updated system list.
- Future contract generation uses `RandomFactionGenerator`, which builds
  employers, enemies, and mission targets from `FactionBorderTracker`. The
  border tracker builds its regional faction set from
  `PlanetarySystem.getFactionSet(date)`, so future contract offers honor
  ownership edits after border data is rebuilt.
- Contract quantity and local modifiers in `AtbMonthlyContractMarket`, unit
  markets, personnel markets, currency fallback, academy/origin filters, and
  HPG/jump-network display all consult current system data and should honor
  edits after their local caches are refreshed.

**Partially honored:**

- Already-generated contracts store `employerCode`, `enemyCode`, and
  `systemId`. A later ownership edit should not automatically rewrite those
  terms. It may affect later scenario generation only where the scenario asks
  for the current planet owner.
- `AtBDynamicScenarioFactory` has explicit `PlanetOwner` force alignment.
  Those forces resolve from `contract.getSystem().getFactions(currentDate)` at
  generation time, so they follow edited ownership.
- `AtBScenario` reinforcement logic checks whether the OpFor owns the planet
  when deciding air support, turrets, conventional infantry, or battle armor.
  Those checks follow edited ownership, but the OpFor faction itself still
  comes from the contract's stored `enemyCode`.

**Not automatically honored:**

- Existing contract employer, enemy, ally, system, payout, and generated
  scenario terms remain as saved. If we want ownership edits to rewrite active
  contracts, that should be a separate GM action such as "Re-evaluate contract
  factions from edited ownership" with a clear preview.
- Random force tables and most RAT selection use the contract/scenario faction
  code, not the local planet owner. They change only if the contract/scenario
  faction changes or the template uses `PlanetOwner`.

For the MVP, saving user-dir YAML does not update the running universe. The UI
must make that explicit and should support editing several systems before the
player restarts. Good MVP copy is something like: "Planetary data saved. MekHQ
will use this change after restart/reload. Existing contracts are unchanged."

If we add same-session editing later, it requires an explicit invalidation path.
A proposed `UniverseEditService.applySystemEdit(PlanetarySystem editedSystem)`
should:

- Replace or mutate the system in `Systems.systemList` without leaving stale
  references to old data.
- Rebuild `Systems.systemGrid` when coordinates or system membership change,
  because nearby-system queries use the grid, not just `systemList`.
- Clear `Systems` HPG-network cache.
- Clear all affected `Planet.CurrentEvents` caches, because each planet caches
  its date-derived event state.
- Rebuild or invalidate `RandomFactionGenerator` / `FactionBorderTracker`
  border data.
- Rebind `CurrentLocation` if the edited system is the campaign's current
  system, since `CurrentLocation` stores a direct `PlanetarySystem` reference.
- Refresh map panels and other UI snapshots such as `InterstellarMapPanel`'s
  copied system list.
- Reset secondary caches that depend on current ownership, including default
  currency and HPG-network display.

This future refresh service should be benchmarked before we expose it. If a GM
edits several planets in one sitting, the implementation should either refresh
once after an explicit "Apply Now" action or debounce/rebatch refresh work so
we do not rebuild expensive derived data after every field change.

### 5.4 YAML writer

- Align with existing project style. MekHQ does not currently have a broad
  repository/service abstraction for this kind of data; it mostly uses focused
  managers and static utilities. Relevant existing patterns are
  [`Systems.load`](MekHQ/src/mekhq/campaign/universe/Systems.java#L272),
  [`SystemValidator`](MekHQ/src/mekhq/utilities/SystemValidator.java),
  [`MHQXMLUtility`](MekHQ/src/mekhq/utilities/MHQXMLUtility.java), and
  MegaMek's [`YamlEncDec`](../megamek/megamek/src/megamek/common/util/YamlEncDec.java).
- Add a focused planetary YAML helper/factory rather than hand-building
  mappers in multiple places. Candidate names:
  `PlanetarySystemYamlMapper`, `PlanetarySystemYamlIO`, or
  `PlanetarySystemFileService`. Prefer whichever best matches the final class
  responsibilities, but keep it in or near `mekhq.campaign.universe` so
  `Systems`, the editor, and validation tests can share it.
- Reuse the same Jackson `ObjectMapper` configuration currently duplicated in
  [`Systems.load`](MekHQ/src/mekhq/campaign/universe/Systems.java#L272) and
  [`SystemValidator`](MekHQ/src/mekhq/utilities/SystemValidator.java#L84).
  Update both to use the shared mapper so load, validation, and save behavior
  cannot drift.
- Add a `SourceableValueSerializer` symmetric to the existing
  deserializer: emit the bare value if `source == null`, else the
  `{source, version, value}` object. Preserve `version` on load and save.
- Add `@JsonInclude(NON_NULL)` so we don't litter unset fields.
- Write editor output only under the configured user directory from
  `PreferenceManager.getClientPreferences().getUserDir()`. Do not hardcode a
  user path and do not write beside canon data. This follows existing user-dir
  merge behavior used by `Systems.loadDefault()` and other user data such as
  awards/icons.
- If an edited file already exists, make a backup before overwriting it. MekHQ
  campaign saves already use an adjacent `_backup` file pattern; the editor can
  use that or a timestamped adjacent backup, but it should keep the backup in
  the user's configured data area.
- Round-trip test: load every canon system, write it back, reload,
  assert structural equivalence. This is the single most important
  test in the feature.

### 5.5 UI placement

- New entry in MekHQ's main menu **GM Tools -> Planetary System
  Editor...** (only enabled when `campaign.isGM()`), and a context-menu
  shortcut on the interstellar map.
- The editor itself is a modal `JDialog` with:
  - Left: searchable system list (with "edited" filter).
  - Right: tabbed panels — *System*, *Planets* (sub-tabs per planet),
    *Events*, *Preview*, *Diff vs canon*.
  - Bottom: Validate / Save / Save As / Revert / Cancel.
- Reuse `PlanetarySystemMapPanel` for the in-dialog preview tab.

### 5.6 Validation pipeline

A single `PlanetarySystemValidator` class returns a list of
`ValidationIssue { Severity, path, message }`. The dialog blocks Save on
`ERROR`, surfaces `WARNING` in a panel, and lets the user proceed. The
validator wraps the rules from `cleanupSystems()` plus the new ones
listed in §4.2.

---

## 6. Risks & Concerns

- **Data corruption.** A bad edit could brick a player's universe.
  Mitigations: never write to canon paths; keep an automatic timestamped
  backup of the previous user-dir version on every save; ship a "reset
  this system to canon" button.
- **Performance.** Loading is already ~1–3s for canon. Per-edit re-save
  is one file, fast. The restart-required MVP avoids live invalidation costs
  entirely and is better for batch editing. Live application is not just O(1)
  `systemList.put()`; it must also rebuild universe indexes and invalidate
  ownership-dependent caches. That may still be acceptable for a single-system
  edit, but it must be benchmarked before we promise it in the UI.
- **Partial propagation.** Ownership edits affect many future calculations,
  but they should not silently rewrite existing contracts or already-generated
  scenarios. The UI should distinguish "future universe state changed" from
  "active contract terms changed".
- **Save game compatibility.** Campaigns reference systems by `id`
  (see [`CurrentLocation.generateInstanceFromXML`](MekHQ/src/mekhq/campaign/CurrentLocation.java#L588)).
  As long as we preserve `id`, existing saves keep working. **Renaming
  a system's `id`** would break saves — disallow this in the editor for
  canon systems.
- **MekHQ vs MegaMekLab divergence.** User-dir overrides are picked up
  by both apps the same way (same path), so this is actually a feature.
  Worth documenting.
- **SUCS / canon updates.** When `mm-data` ships an updated
  `BergmansPlanet.yml`, the user's `edits/BergmansPlanet.yml` will
  override it silently and they'll miss the upstream improvement.
  Mitigation: store the `mm-data` version we forked from in a
  `# editor-meta:` YAML comment, and on load detect when canon has
  changed and surface a "canon was updated; review your edits" notice.
- **`SourceableValue` round-trip.** Easiest place to silently lose data.
  Treat the symmetric serializer as a hard requirement, not a nice-to-have,
  and golden-file test it.
- **User error in faction codes.** Free-text faction codes in the YAML
  ("LA", "FC", "IND", "UND", "ABN") — must always be picked from the
  registry, never typed.
- **GM-mode misuse perception.** Some players treat "GM mode" as a
  cheat. Clearly label this as a worldbuilding tool, not a gameplay
  cheat, and keep it in a "GM Tools" menu so the framing is right.

---

## 7. Implementation Phases

A suggested order; each phase is independently shippable.

1. **Phase 0 — Plumbing.** ✅ **Done**
   - Extract a shared planetary YAML mapper/helper from the duplicated mapper
     setup in `Systems.load` and `SystemValidator`.
     ✅ `PlanetarySystemYamlIO.createMapper()`.
   - Add `version` to `SourceableValue` and preserve it during deserialization
     and serialization.
     ✅ Round-tripped by the symmetric serializer.
   - `SourceableValueSerializer` + symmetric round-trip tests across
     all canon files.
     ⚠ Serializer is in place; a per-canon-file golden test is **not yet**
     written — only `PlanetarySystemChangeSummaryTest` covers the diff path.
   - Helper `Systems.saveUserSystem(PlanetarySystem)` writing to the
     `edits/` directory under the configured user dir, with backup.
     ✅ Plus `Systems.deleteUserSystem` for the Delete Override action.
   - `PlanetarySystemValidator` skeleton wrapping today's rules. ✅
   - Explicit restart-required save messaging and tests that prove the saved
     user-dir file loads as an override on the next startup/load.
     ⚠ Banner shown; the override-roundtrip test is **not yet** automated.
2. **Phase 1 — Editor shell (Tier 1a).** ✅ **Done** — GM-gated menu entry,
  read-only system list/details, validation panel, and save/revert plumbing
  through the user-dir override path.
3. **Phase 2 — MVP editing (Tier 1b/Tier 2).** ⚠ **Mostly done** —
  Per-planet event table edit, faction-by-date picker, enum dropdowns,
  and the static-field properties tab are implemented. The dedicated
  socio-industrial widget, in-dialog `PlanetarySystemMapPanel` preview,
  and undo/redo are not done; the diff view is provided as a "Review
  Changes" dialog rather than a tab.
4. **Phase 3 — Wizards & bulk ops (Tier 3 minus campaign overlay).**
   ⚠ **Partial** — Export/Import bundle ✅ done; transfer-ownership,
   new-system, and clone-from-template wizards are **not done**.
5. **Phase 4 — Live refresh spike (optional).** ❌ **Not started.** Benchmark
   and, if acceptable, add `UniverseEditService` or an equivalent refresh path
   that applies a system edit and invalidates `Systems`,
   `FactionBorderTracker`, HPG, planet event, current-location, map, and
   currency caches. Prefer an explicit "Apply Now" or debounced batch refresh
   over refreshing after every field change.
6. **Phase 5 — Campaign-scoped overrides (optional).** ❌ **Not started.**
   The `Campaign` overlay refactor, behind a campaign option, only if there's
   clear demand.

---

## 8. Open Questions (please answer before implementation)

1. Should canon-system coordinates be **locked** in the editor, or just
   warn-on-change? (Affects jump paths globally.)
   ✅ **Answered (locked)** — coordinates are not editable for canon
   systems in MVP. Revisit when the create-new-system wizard lands.
2. Do we want to allow **deleting** a canon system, or only
   "abandon"-ing it via the `ABN` faction code? Deletion would require
   handling references from saved games (`CurrentLocation`, contracts).
   ✅ **Answered (no deletion)** — there is no delete-system action;
   "abandon" via faction event is the only path.
3. Should edits be **per-campaign by default** (option 3 in §5.2) or
   **per-user by default** (option 1)? Per-campaign is cleaner but a
   bigger refactor.
   ✅ **Answered (per-user)** — option 1; per-campaign deferred to Phase 5.
4. Where do edits live for **non-GM** players who load a campaign that
   was edited by a GM? Probably: the campaign save embeds the overlay,
   and on load we re-register those systems. This pushes us toward
    option 3 in §5.2.
   ⏳ **Open** — current MVP requires the receiving player to import the
   shared zip into their own user dir. Acceptable for now; revisit if
   Phase 5 is picked up.
5. Is there appetite to also **expose this in MegaMekLab**, or keep it
   strictly MekHQ?
   ⏳ **Open** — strictly MekHQ for now (the user-dir overrides still
   apply in MML automatically).
6. Should the editor offer a GM-only **re-evaluate active contracts** action
    after ownership changes, or should it only affect future generation and
    scenario pieces that explicitly ask for current planet ownership?
   ⏳ **Open** — N/A in MVP (restart-required), revisit alongside Phase 4.

---

## 9. TL;DR Recommendation

- Build it inside MekHQ, GM-mode-gated.
- Persist via the existing user-dir override mechanism — never touch
  canon files.
- MVP behavior is restart/reload-required after save. This is simpler,
  safer, and better for editing several planets in one sitting.
- Start with a focused MVP that edits the per-planet event timeline
  (faction / population / socio-industrial / HPG), because that's 90%
  of the "I want to reflect my campaign in the universe" use case.
- Treat same-session ownership propagation as optional future work, gated by
  benchmark results and implemented as an explicit/batched refresh path.
- Make `SourceableValue` round-trip and `PlanetarySystem` YAML write early,
  and golden-test it against every canon file.
- Layer validation, helpers, wizards, and (optionally) campaign-scoped
  overrides on top in later phases.
