# Planetary Data Editor - Design

Status: Current-state design note
Branch: `vc/planetary-editor` (mekhq)
Scope: MekHQ-side GM tool; no changes to canonical planetary data shipped in `mm-data`.

---

## 1. Motivation for the Editor

Players running long campaigns often want the campaign universe to diverge from canon:

- A planet changes hands after a successful campaign arc.
- A homebrew or resurrected faction takes control of a system.
- An HPG or recharge-station state changes because of in-fiction events.
- A GM wants a personal homeworld or alt-history setting without hand-editing YAML.

Today, the only practical way to make those changes is to edit planetary YAML files under
`data/universe/planetary_systems` or the user-dir equivalent. That is powerful, but it is also easy to get wrong: the
format is nested, event-sourced, source/version-aware, and lightly documented for end users.

The Planetary Data Editor gives GMs an in-game tool that edits the existing MekHQ planetary model, validates the result,
and writes user-dir override files. It is a worldbuilding/admin tool, not a campaign-save editor and not a canonical
`mm-data` authoring tool.

---

## 2. Design / Architecture

### Scope and Entry Points

The editor is GM-only and lives inside MekHQ. It is reachable from:

- **GM Tools -> Planetary System Editor...** in the main menu.
- The **Interstellar Map** context menu, where the clicked system is pre-selected.

Both entry points open `PlanetarySystemEditorDialog`. Each editor instance is disposed on close so native window resources
are released and `windowClosed` listeners run.

### Data Model

The editor works with MekHQ's existing planetary universe classes:

- `Systems`: process-global registry of loaded planetary systems.
- `PlanetarySystem`: system-level data, including fixed fields and system-level event timeline.
- `Planet`: planet static data, landmasses, satellites, and per-planet event timeline.
- `Planet.PlanetaryEvent`: dated event patch for ownership, population, HPG, socio-industrial data, names, and messages.
- `PlanetarySystem.PlanetarySystemEvent`: dated event patch for system-level values such as nadir/zenith recharge flags.
- `SourceableValue<T>`: wrapper that preserves source, version, and value metadata where canon data provides it.

The important modeling rule is that campaign-history changes are event patches, not direct mutations of current state. A
faction change in 3050 is a new dated planet event. Static/mostly-static physical properties live on the planet itself.

### Persistence Model

The editor never writes to canonical `mm-data` files. Saves write a complete system override to the configured user data
directory:

```text
<userDir>/data/universe/planetary_systems/edits/<systemId>.yml
```

The effective load model is:

```text
canonical data -> user data -> editor edits
```

where later files with the same system id replace earlier ones. The dedicated `edits/` directory makes editor output easy
to distinguish from hand-authored user data.

`PlanetarySystemYamlIO` is the shared YAML gateway for this feature. It owns the Jackson mapper configuration used for
read/write/copy operations, including `SourceableValue`, `SocioIndustrialData`, and `StarType` serializers. Existing
override files are copied to an adjacent `*_backup` file before replacement.

### User-Dir Scope

Overrides are scoped to the configured MekHQ user data directory, not to an installation and not to a campaign save. Any
campaign that starts with the same configured user directory will see the same editor overrides after restart/reload.

This is intentional for the current implementation. A campaign-scoped overlay would require a separate campaign data
layer and campaign-aware accessors throughout code that currently reads from the process-global `Systems` registry.

### Runtime Behavior

The current editor uses a restart/reload boundary. Saving an override does not refresh all already-loaded universe data in
the same MekHQ session. This keeps the implementation conservative because planetary data is read by maps, contract
generation, markets, HPG/currency logic, current location objects, and derived caches.

If same-session application is added later, it needs an explicit refresh service that can replace the edited system,
rebuild `Systems` indexes, clear HPG and planet event caches, refresh faction-border data, rebind current location, and
notify open UI panels.

### UI Structure

The dialog is organized around the main user workflow: find a system, inspect it, edit scoped data, validate, review, and
save.

- **Left pane:** searchable system list with sort options, unsaved/override filters, `[U]` / `[O]` row badges, and hover
  summaries of changed fields.
- **System Details tab:** read-only current-state summary at the campaign date.
- **System Events tab:** inline-editable timeline for nadir and zenith recharge-station flags, with a gameplay warning.
- **Planet Events tab:** planet list plus per-planet event timeline editor.
- **Planet Properties tab:** synchronized planet list plus static/mostly-static property editor.
- **Validation panel:** persistent errors/warnings below the tabs.
- **Button row:** Review Changes, Save, Revert Changes, Delete Override, Import Overrides, Export Overrides, Close.

The two planet-scoped tabs each show their own planet list, but both lists share a model and synchronized selection so the
selected planet stays coherent across tabs.

### Validation and Review

`PlanetarySystemValidator` is the editor-facing validation facade. It currently delegates to `SystemValidator`, keeping
editor saves aligned with the data-loading validation path while leaving one place for future editor-only warnings.

The dialog blocks save on validation errors and shows warnings without blocking. `PlanetarySystemChangeSummary` builds the
human-readable Review Changes output from the baseline system and the edited in-memory copy.

### Sharing Overrides

Import/export operates on zip bundles of `.yml` files from the user-dir `edits/` directory. Import prompts on file
conflicts and guards against zip-slip path traversal. This is the current sharing path for GMs who want another player or
developer to load the same altered universe.

---

## 3. What Is Implemented

- GM-gated editor entry from the main menu.
- Interstellar Map context-menu entry that opens the editor with the clicked system pre-selected.
- Editor instances are disposed on close so map repaint listeners and native resource cleanup run.
- Searchable/sortable system list with filters for unsaved systems and systems with override files.
- `[U]` / `[O]` system badges and changed-field tooltips.
- Read-only System Details tab at the campaign date.
- System Events tab for dated nadir/zenith recharge-station flags.
- Planet Events tab for dated ownership/population/HPG/socio-industrial/source/version/message edits.
- Faction picker backed by the `Factions` registry and filtered by the event date.
- Support for duplicate, clear, remove, and custom-marked planet events.
- Planet Properties tab for name, type, gravity, diameter, day/year length, temperature, pressure, atmosphere,
  composition, percent water, life form, moons, rings, description, source, and version.
- Landmass and satellite table editors in Planet Properties.
- Gameplay/flavor grouping in Planet Properties, with gameplay warnings for fields that affect maintenance, AtB,
  StratCon, or recovery behavior.
- Numeric and enum-backed property inputs where the model supports them.
- Shared synchronized planet selection between Planet Events and Planet Properties.
- Per-system Revert Changes for in-memory edits.
- Delete Override for removing the saved user-dir override file.
- Per-planet Revert Planet restoring both event and static/property edits from the baseline.
- Review Changes dialog powered by `PlanetarySystemChangeSummary`.
- Live validation panel powered by `PlanetarySystemValidator` / `SystemValidator`.
- Save to `<userDir>/data/universe/planetary_systems/edits/<systemId>.yml`.
- Adjacent `*_backup` copy before replacing an existing override file.
- Restart/reload-required save messaging.
- Import/export zip bundle support for all editor override files.
- Zip import conflict prompts: Overwrite, Skip, Cancel.
- Zip-slip guard on import.
- Keyboard shortcuts: `Ctrl+S` save, `Ctrl+F` focus search, `Delete` / `Ctrl+D` delete selected event.
- Shared YAML read/write/copy path in `PlanetarySystemYamlIO`.
- Symmetric `SourceableValue` serialization/deserialization with version round-trip.
- Focused tests for YAML IO, change summaries, and validation.

---

## 4. What Is Missing

- Same-session universe refresh after saving. Current behavior requires restart/reload before the rest of MekHQ consumes
  saved overrides.
- Campaign-scoped overrides stored inside the campaign save.
- A campaign-aware planetary accessor layer that can choose between campaign overlay data and global `Systems` data.
- Transfer-ownership wizard for applying faction changes across a system/radius/date.
- Create-new-system wizard.
- Clone-from-template wizard.
- Undo/redo stack inside the dialog.
- In-dialog `PlanetarySystemMapPanel` preview.
- HPG plausibility warnings, such as historically unusual HPG ratings for the current owner.
- Recharge-station plausibility warnings, such as very early recharge station availability.
- Coordinate-collision warnings for future user-created systems.
- Owner/date search filters such as "systems owned by faction X on date Y".
- Auto-event creation from contract resolution or other gameplay outcomes.
- A golden round-trip test across every canonical planetary system file.
- Detection that a user override is masking newer canon data from a later `mm-data` update.
- GM action to re-evaluate active contracts after ownership edits. Existing contracts are intentionally not rewritten by
  the current editor.

---

## 5. What We Discarded as Out of Scope

- **Editing canonical `mm-data` files.** The editor writes only user-dir overrides.
- **Campaign-save persistence.** Current persistence is user-dir scoped; campaign-scoped overlays are future work.
- **Automatic same-session propagation.** The current editor does not mutate all running universe consumers after save.
- **System id editing.** Existing saves and references depend on stable system ids.
- **System `name` editing.** The field is mostly vestigial and used as an id fallback during load.
- **`sucsId` editing.** It is a Sarna catalog tag with no gameplay value in the editor and can trigger duplicate-id
  validation problems if mis-set.
- **Density editing.** `Planet.getSourcedDensity` is deprecated for removal.
- **Canon-system coordinate editing.** Coordinates affect travel, shopping radii, and nearby-system queries globally.
- **Deleting canon systems.** Abandoning or changing ownership through events is the supported path.
- **Editing the icon image library.** Icon data is outside the editor MVP.
- **Multi-user synchronization.** Sharing is through exported/imported override bundles, not a server or shared campaign
  protocol.
- **Automatic rewriting of active contracts.** Ownership edits define future/current universe state but do not silently
  rewrite saved contract terms.
