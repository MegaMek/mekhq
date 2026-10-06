# Force Generator: Command Designer (Model -> Accept & Build)

**Status:** Implemented. Model accumulation, the Accept & Build commit flow, and the design-stage UX
(dialog rename, banner, tree title, status line, empty-state, commit confirmation) are built. Remaining:
Composition Summary recalculation on remove (backlog item 1) and the two vestigial-UI backlog items.
**Scope:** MekHQ (Command Designer dialog + generation pipeline) and MegaMek (accumulating preview
model in the embedded `ForceGeneratorViewUi`). Builds on the include/exclude work already landed on
`Implement-Force-Generator-in-MekHQ`.

## Concept

The Command Generator becomes a **Command Designer**: a design workspace where the player builds an
**in-memory Model** of a command by rolling combat forces (mix-and-match) and removing what they don't
want. Nothing touches the campaign until the player hits **Accept & Build**, which generates support for
the Model and commits the whole command (combat + support) to the official campaign TOE.

Support generation is **command-creation-only** - it happens once, at Accept. After the command exists,
the player uses the normal build/hire tools. There is no cross-session "add later and top up"; that
removes the reconciliation-across-sessions complexity.

## Approved workflow (Model = in-memory, "Option A")

1. **Open** -> empty Model (Design stage). The campaign TOE is untouched.
2. **Set options** and **Generate** -> the rolled combat command is **added to the Model** (accumulates).
   Model tree + Composition Summary update.
3. **Generate again** (different options) -> appends another command to the Model. Mix-and-match.
4. **Right-click -> Remove** nodes from the Model -> tree + Composition Summary **recalculate**.
5. Iterate 2-4 until the command looks right.
6. **Accept & Build** -> summary confirmation -> generates support sized to the Model, commits the whole
   Model (combat + support) to the **official campaign TOE**, closes.
7. **Cancel / Discard Model** anytime -> discards the Model; campaign untouched.

## Approved UX (clarity is a first-class requirement)

The risk is a player not realizing the workspace is a non-committed draft, so the Design stage is
signalled everywhere:

- **Dialog name:** "Command Designer".
- **Header banner (persistent):** "DESIGN STAGE - this is a model. Nothing is added to your campaign
  until you Accept & Build."
- **Preview tree titled** "Command Model (Design)" so it never reads as the live TOE.
- **Status line:** "Model: N units across M formations - not yet committed."
- **Empty-state hint:** "Generate a force to start building your command." when the Model is empty.
- **Commit button:** "Accept & Build Command", tooltip "Generates support and adds this command to your
  campaign TOE."
- **Commit confirmation:** "Build this command? N combat units + generated support will be added to your
  campaign TOE." [Build / Cancel] - the one moment the Model becomes real.
- **Cancel button:** "Discard Model".
- Composition Summary always reflects the Model and recalculates on add/remove.

## Architecture

### Accept side (MekHQ) - mostly built

Accept reuses the Step 1 pipeline split already committed:

- `commitCombatForces(campaign, modelDescriptor, options)` - materialize the Model's combat units/crews
  into the campaign TOE.
- `generateSupportFromToe(campaign, options)` - size + generate support from the now-committed combat
  (reconciliation + formation reuse make it duplicate-safe within the build).

So Accept = confirmation -> `commitCombatForces(Model)` -> `generateSupportFromToe(campaign)` ->
post-generation extras. `CompanyGenerator.applyToCampaign(..., generateSupport=false)` already isolates
the combat commit.

### Model side (MegaMek) - the main new work

Today the embedded `ForceGeneratorViewUi` **replaces** the tree with a single rolled `ForceDescriptor`
on each Generate. The Model requires:

- An **accumulating Model descriptor** that each Generate **appends** into (rather than replacing), which
  the preview tree displays.
- **Remove** deletes nodes from the Model (the include/exclude work becomes real removal on the Model).
- The Model descriptor is what `commitCombatForces` walks at Accept.

## Reuse of already-built pieces

- **Step 1 split** (`commitCombatForces` / `generateSupportFromToe`) - committed.
- **Vehicle + personnel reconciliation, HR-to-zero** - built (personnel uncommitted). Still valuable as a
  safety within a single Accept; less critical now that support is one-shot at creation.
- **Formation reuse** (`AddSupportUnitsToTOE`) - built, prevents duplicate support formations.
- **BA-company nesting** (`ForceDescriptorWalker`) - built; loose attached platoons wrap into a
  unit-typed company.
- **Include/exclude tree menu + strikethrough** - the basis for Model removal.

## Build order

1. [DONE] **Accept side (MekHQ):** Accept & Build wired - build confirmation ("Build this command? N
   combat units plus generated support...") -> combat commit (`applyToCampaign(..., generateSupport=false)`)
   -> `generateSupportFromToe`, support always generated (command-creation-only). Interim auto-prompt removed.
2. [DONE] **Model side (MegaMek):** rolls accumulate into a Model descriptor; the include/exclude menu
   drives removal from the committed set; the tree is driven from the Model.
3. [DONE] **UX (MekHQ + MegaMek):** renamed to Command Designer, design-stage banner, "Command Model
   (Design)" tree title, status line ("Model: N unit(s) in M command(s) - not yet committed."),
   empty-state hint, "Accept & Build Command" / "Discard Model" button labels, Build/Cancel confirmation.
4. [TODO] Composition Summary recalculation on add/remove (backlog item 1).

## Superseded

The interim **auto-prompt MVP** (Accept immediately asked "Generate support now?" after the first roll)
is replaced by this Model workflow and should be removed when the Accept side is rebuilt.

## Backlog (captured during testing)

1. Recalculate the Composition Summary/History box when units are removed from the TOE (folds into the
   Model removal work).
2. Hide the Rolled Units box in TOE mode (MegaMek `toeExclusionMode` gate) - vestigial in this workflow.
3. Cargo Summary - investigate how it is computed (separate).
