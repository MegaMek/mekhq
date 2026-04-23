# Planetary Data Editor — Design

Status: Draft / Brainstorm
Branch: `vc/planetary-editor` (mekhq)
Scope: MekHQ-side feature; no changes to canonical MM data shipped in `mm-data`.

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
- Edits are immediately observable in-game (interstellar map, contracts,
  shopping radius, recruitment origin via `RandomOriginOptions`).

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
[`MaxiesPlanet.yml`](../../mm-data/data/universe/planetary_systems/canon_systems/MaxiesPlanet.yml))
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
- **System overview pane.** Read-only summary of current state at the
  campaign date: name, coordinates (locked), star type, primary planet,
  faction(s), HPG, recharge stations, population.
- **Per-planet event timeline.** Sortable table of events
  (date / faction / population / socioIndustrial / hpg / message).
  Add / edit / delete rows. Date-picker for dates. Faction picker
  validates against `Factions.getInstance()` and filters to factions
  active on the selected date (see §4.3).
- **System-level event timeline.** Same UX for nadir/zenith charge changes.
- **Save.** Writes the *full* system YAML to
  `<userDir>/data/universe/planetary_systems/edits/<id>.yml`. The
  user-dir loader will pick it up on next load and override the canon
  copy. Optionally also live-mutate the in-memory `PlanetarySystem`
  so changes are visible without a restart (see §5.3).
- **Revert.** Delete the user-dir file and reload that system from canon.
- **Diff badge.** On the interstellar map, edited systems get a small
  marker (e.g. an outline ring or a "*" suffix on hover) so the player
  always knows "this is not canon".

### 4.2 Tier 2 — Editor with validation and helpers

What separates this from "open notepad on the YAML":

- **Faction validation.** When picking a faction for a date:
  - List is filtered to factions whose start/end years bracket that date
    (Faction has era-aware methods already).
  - Multi-select is supported (matches the YAML `value: [LA, FC]` shape).
  - Highlight unusual choices (Clan faction in 2900, etc.) as warnings,
    not errors — players doing alt-history will deliberately pick these.
- **Planet-data validation.** Numeric bounds for `gravity`, `pressure`,
  `temperature`, `water%`. Enum-backed dropdowns for `type`
  ([PlanetaryType](MekHQ/src/mekhq/campaign/universe/enums/PlanetaryType.java)),
  `atmosphere`, `lifeForm`, `pressure`. Reuse the existing enums.
- **Socio-industrial picker.** Dedicated 5-column widget for
  `Tech-Industry-RawMat-Output-Agriculture` ratings using
  `SocioIndustrialData` enum values, instead of free-text "D-C-A-D-D".
- **Sanity checks on save.** Run the same rules as
  `Systems.cleanupSystems()` ([Systems.java:373](MekHQ/src/mekhq/campaign/universe/Systems.java#L373))
  — coordinates present, star present, primarySlot <= planets.size().
  Show errors before allowing save.
- **HPG / recharge plausibility.** Warn if you set HPG=A on a system
  whose current owner has no HPG-A historically; warn (don't block) if
  enabling a nadir/zenith station before the year jump-stations were
  invented.
- **Coordinate collision warning.** Reuse `getNearbySystems` to warn if
  the coordinates you set for a brand-new system put it `<1 ly` from
  another (the same warning `logVeryCloseSystems()` already emits).
- **Preview in-context.** Live-update the
  [`InterstellarMapPanel`](MekHQ/src/mekhq/gui/InterstellarMapPanel.java)
  faction-hex shading and the
  [`PlanetarySystemMapPanel`](MekHQ/src/mekhq/gui/PlanetarySystemMapPanel.java)
  view as the user edits.
- **Undo / redo within the dialog**, plus a confirmation diff before save
  ("3 events added on Sati, 1 faction change on Morrighan") so the user
  can audit what they're about to write.

### 4.3 Tier 3 — Helpers / quality of life

- **"Transfer ownership" wizard.** One-click action: pick a date,
  pick a new faction, optionally apply to a radius of nearby systems
  (post-campaign conquest). Creates the right faction events on every
  affected planet's primary.
- **Create-new-system wizard.** Required fields only (id, x, y,
  spectral type, one planet with name+type+sysPos). Everything else
  defaulted; user can fill in later.
- **Clone-from-template.** "New system based on Galatea" — copies the
  canonical structure as a starting point.
- **Bulk import / export.** Zip up all user edits into a sharable bundle
  (e.g. `myCampaignUniverse.zip`) — the loader already accepts `.zip`
  files in the planetary systems directory ([Systems.java:319](MekHQ/src/mekhq/campaign/universe/Systems.java#L319)).
  Lets players share alt-universes with each other.
- **Campaign-scoped vs. user-scoped overrides.** Optional toggle on save:
  "Apply to this campaign only" (stored inside the campaign save) vs.
  "Apply to all my campaigns" (written to user dir). Implementing
  campaign-scoped overrides requires a new overlay layer in `Systems`
  (see §5.2) and is the most invasive piece of work in the design.
- **Search / filter.** "Show me all systems where I have edits", "Show
  me all systems owned by faction X on date Y".
- **Auto-event from gameplay (stretch).** Hook into
  `AtBContract` resolution: if the GM marks a contract as
  "ownership-changing", offer to auto-create the matching faction event
  on the contract planet for the contract's end date.

### 4.4 Explicitly out of scope (for now)

- Editing star coordinates of existing canon systems. (Allowed for
  user-created systems; for canon systems we lock coords to keep jump
  paths and shopping radii predictable, but make this overridable
  with a confirm dialog.)
- Editing the icon image library itself.
- Multi-user / shared-campaign sync. Sharing happens via the export
  bundle, not a server.

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

Two options, in increasing order of scope:

1. **Mutate the singleton (simple).** Replace the entry in
   `Systems.getInstance().getSystems()` with the edited
   `PlanetarySystem`. Any open campaign sees the change immediately.
   *Downside:* cross-campaign leakage in the same JVM session.
2. **Per-campaign overlay (clean).** Add an
   `Optional<Map<String, PlanetarySystem>> overlay` to `Campaign`, and
   a `Campaign.getSystem(String id)` accessor that checks the overlay
   before falling back to `Systems.getInstance()`. All planet/system
   reads inside MekHQ would need to migrate to that accessor over time.
   *Downside:* large diff, touches many files; do this only if Tier 3
   campaign-scoped overrides are committed to.

Recommend **starting with option 1** for the MVP, plus a session-level
"these systems were edited; you may want to restart to revert"
notification. Move to option 2 only if/when campaign-scoped overrides
ship.

### 5.3 YAML writer

- Reuse the same Jackson `ObjectMapper` configured in
  [`Systems.load`](MekHQ/src/mekhq/campaign/universe/Systems.java#L272).
- Add a `SourceableValueSerializer` symmetric to the existing
  deserializer: emit the bare value if `source == null`, else the
  `{source, value}` (and optional `version`) object.
- Add `@JsonInclude(NON_NULL)` so we don't litter unset fields.
- Round-trip test: load every canon system, write it back, reload,
  assert structural equivalence. This is the single most important
  test in the feature.

### 5.4 UI placement

- New entry in MekHQ's main menu **GM Tools -> Planetary System
  Editor...** (only enabled when `campaign.isGM()`), and a context-menu
  shortcut on the interstellar map.
- The editor itself is a modal `JDialog` with:
  - Left: searchable system list (with "edited" filter).
  - Right: tabbed panels — *System*, *Planets* (sub-tabs per planet),
    *Events*, *Preview*, *Diff vs canon*.
  - Bottom: Validate / Save / Save As / Revert / Cancel.
- Reuse `PlanetarySystemMapPanel` for the in-dialog preview tab.

### 5.5 Validation pipeline

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
  is one file, fast. Live-mutating the singleton is O(1). Map repaint
  on edit is the existing repaint cost.
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

1. **Phase 0 — Plumbing.**
   - `SourceableValueSerializer` + symmetric round-trip tests across
     all canon files.
   - Helper `Systems.saveUserSystem(PlanetarySystem)` writing to the
     `edits/` directory with backup.
   - `PlanetarySystemValidator` skeleton wrapping today's rules.
2. **Phase 1 — MVP editor (Tier 1).** Read-only viewer + per-planet event
   table edit + save/revert + GM menu entry.
3. **Phase 2 — Validation & helpers (Tier 2).** Faction-by-date picker,
   enum dropdowns, socio-industrial widget, in-dialog preview,
   diff view.
4. **Phase 3 — Wizards & bulk ops (Tier 3 minus campaign overlay).**
   Transfer-ownership, new-system, clone-from-template, export bundle.
5. **Phase 4 — Campaign-scoped overrides (optional).** The `Campaign`
   overlay refactor, behind a campaign option, only if there's clear
   demand.

---

## 8. Open Questions (please answer before implementation)

1. Should canon-system coordinates be **locked** in the editor, or just
   warn-on-change? (Affects jump paths globally.)
2. Do we want to allow **deleting** a canon system, or only
   "abandon"-ing it via the `ABN` faction code? Deletion would require
   handling references from saved games (`CurrentLocation`, contracts).
3. Should edits be **per-campaign by default** (option 2 in §5.2) or
   **per-user by default** (option 1)? Per-campaign is cleaner but a
   bigger refactor.
4. Where do edits live for **non-GM** players who load a campaign that
   was edited by a GM? Probably: the campaign save embeds the overlay,
   and on load we re-register those systems. This pushes us toward
   option 2 in §5.2.
5. Is there appetite to also **expose this in MegaMekLab**, or keep it
   strictly MekHQ?

---

## 9. TL;DR Recommendation

- Build it inside MekHQ, GM-mode-gated.
- Persist via the existing user-dir override mechanism — never touch
  canon files.
- Start with a focused MVP that edits the per-planet event timeline
  (faction / population / socio-industrial / HPG), because that's 90%
  of the "I want to reflect my campaign in the universe" use case.
- Make `SourceableValue` round-trip and `PlanetarySystem` YAML write
  the very first thing you build, and golden-test it against every
  canon file.
- Layer validation, helpers, wizards, and (optionally) campaign-scoped
  overrides on top in later phases.
