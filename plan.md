# Briefing Room UI Improvement Plan

## Initial State

The Briefing Room is currently a dense single-screen layout built around nested split panes:

- The left side contains mission selection, mission/contract details, a large StratCon tutorial panel, and the scenario table.
- The right side contains scenario action buttons, the selected scenario details, and the StratCon deployment requirement/current assignment tables.
- A separate tutorial hyperlink strip sits at the bottom of the tab.

This creates several usability and rendering problems:

- The scenario list is embedded inside `MissionViewPanel`, which itself is placed inside `scrollMissionView`. This produces nested scrolling when many scenarios exist.
- The large StratCon tutorial panel takes prime center space and has fixed-width sizing, even though the same concepts are available through glossary/help links.
- The selected scenario brief is not visually central, even though it is usually the most important decision-making content on the screen.
- The assignment tables are squeezed into the bottom-right area and can become hard to scan.
- The `LanceAssignmentView` renderer likely contributes to stale/smeared text during scrolling because some renderer branches do not reset renderer state through `super.getTableCellRendererComponent(...)`.
- Some scenario detail panels resist resizing/reflowing, so dragging split panes does not always make content meaningfully wider.

## Goal State

The Briefing Room should become a tabbed operational workspace with a clear default workflow and room for deeper secondary tasks.

Recommended tabs:

1. `Overview`
   - The default tab.
   - Shows the active mission/contract context, scenario queue, selected scenario brief/objectives, current deployment status, and primary scenario actions.
   - Optimized for answering: "What should I fight next, what does it require, who is assigned, and what action do I take?"

2. `Assignments`
   - Dedicated StratCon deployment management.
   - Shows deployment requirements and current combat team assignments with enough room to edit roles and contract assignment.
   - Optimized for correcting shortfalls and preparing the force before advancing time or deploying.

3. `History`
   - Secondary view for completed/resolved scenarios and after-action information.
   - Keeps old reports, resolved scenarios, loot/cost notes, and completed scenario details out of the primary decision space.

The long tutorial content should move out of the main layout and into glossary/help access. The bottom tutorial hyperlink strip can remain as a compact support affordance.

## Design Principles

- Keep the main scenario decision visible: scenario list, selected scenario brief, objective/deployment requirements, and action buttons should be available together in `Overview`.
- Use tabs for different modes of work, not for fragments of the same decision.
- Avoid nested scroll panes in normal workflows.
- Prefer a single obvious scroll surface per region.
- Make the selected scenario details reflow with available width.
- Keep changes phased so each step leaves the Briefing Room usable and reviewable.
- Preserve existing behavior first; change layout and rendering before changing gameplay logic.

## Phase 1: Stabilize Existing UI

### Work

- Fix `LanceAssignmentView` table renderers so every branch calls `super.getTableCellRendererComponent(...)` before customizing text/alignment/color.
- Review renderer state for both deployment requirement and current assignment tables.
- Ensure table repainting remains stable when scrolling or changing selection.
- Avoid changing layout in this phase except where required for the renderer fix.

### Result After Phase 1

- The reported text smearing in `Current Assignments` should be reduced or eliminated.
- Users get an immediate quality improvement before the larger layout work begins.
- The change is small, easy to review, and low risk.

### Validation

- Open a campaign with enough combat teams to make `Current Assignments` scroll.
- Scroll the assignment table repeatedly.
- Change selected rows and verify text does not smear or retain stale values.
- Run focused compile/tests if available for MekHQ GUI code.

## Phase 2: Remove Nested Scenario Scrolling

### Work

- Extract the scenario table out of `MissionViewPanel` so it is no longer inside the scrollable mission detail panel.
- Create a dedicated scenario queue panel owned by `BriefingTab` or a small new view class.
- Give the scenario queue its own scroll pane and stable preferred size.
- Keep existing scenario selection, sorting, context menu, and `focusOnScenario(...)` behavior intact.
- Preserve `ScenarioTableModel` and `ScenarioTableMouseAdapter` unless a small targeted adjustment is necessary.

### Result After Phase 2

- Contracts with many scenarios have one clear scenario-list scrollbar.
- The scenario list can be moved independently in later layout phases.
- Existing table behavior remains familiar while the major pain point is removed.

### Validation

- Select a mission with more than a dozen scenarios.
- Confirm only the scenario queue scrolls when the pointer is over the scenario list.
- Confirm selecting a scenario still updates the scenario detail view.
- Confirm right-click actions such as `Deploy...`, `Edit...`, and GM remove still work.
- Confirm report links and `CampaignGUI.focusOnScenario(...)` still select the correct mission and scenario.

## Phase 3: Remove Main-Screen Tutorial Weight

### Work

- Remove the large fixed-width StratCon tutorial panel from the primary mission detail area.
- Keep the compact bottom `TutorialHyperlinkPanel("missionTab")` or replace it with an equivalent compact help strip.
- Ensure the long tutorial text remains available through glossary/help entries.
- If needed, add or adjust glossary links so the help strip points users to the same concepts formerly shown inline.

### Result After Phase 3

- The central area is no longer occupied by static tutorial text.
- More space becomes available for scenario decision-making.
- New and returning players still have accessible help without sacrificing layout real estate.

### Validation

- Open an AtB/StratCon contract and confirm the mission detail panel no longer forces a wide tutorial block.
- Click the bottom help links and verify glossary dialogs still open.
- Confirm non-StratCon missions still render mission details correctly.

## Phase 4: Introduce The Tabbed Briefing Room Shell

### Work

- Add a `JTabbedPane` to `BriefingTab` as the main content container.
- Create three tabs: `Overview`, `Assignments`, and `History`.
- Move existing mission selector and mission action buttons into shared/top-level controls, or keep them in `Overview` initially if that is less invasive.
- Keep the compact help strip outside the tabs at the bottom of the Briefing Room.
- Preserve event subscriptions and refresh methods, routing updates to whichever panels now own the affected components.

### Result After Phase 4

- The Briefing Room has a clear tab structure while still preserving existing core behavior.
- The code has an obvious place for the next layout improvements.
- Reviewers can evaluate the shell before content is heavily reorganized.

### Validation

- Confirm tab switching does not clear the selected mission or selected scenario unexpectedly.
- Confirm `refreshAll()`, mission changes, scenario changes, and StratCon deployment events update the right views.
- Confirm keyboard focus and default tab selection feel reasonable.

## Phase 5: Build The Overview Tab

### Work

- Arrange `Overview` around the primary decision workflow:
  - Mission/contract summary on the left or top-left.
  - Scenario queue in a dedicated region.
  - Selected scenario brief/objectives/details in the central, largest region.
  - Primary action buttons near the selected scenario header or footer.
  - Compact deployment status/shortfall summary visible without showing the full assignment tables.
- Reorder selected scenario details so the scenario description, objectives, deployment instructions, and map conditions appear before long force trees and after-action details.
- Make AtB scenario detail panels track viewport width unless horizontal scrolling is truly required.
- Avoid fixed-width inner panels that ignore split pane resizing.

### Result After Phase 5

- The default Briefing Room experience centers the selected scenario brief.
- Users can scan scenario options, understand the selected scenario, and act without hunting through cramped panes.
- The scenario detail area benefits from additional width when the window is resized.

### Validation

- Test small and large windows.
- Test contracts with few scenarios and many scenarios.
- Test AtB dynamic scenarios, legacy/special scenarios, resolved scenarios, and scenarios with reports.
- Confirm primary actions enable/disable exactly as before.
- Confirm selected scenario scroll starts at the top after selection changes.

## Phase 6: Build The Assignments Tab

### Work

- Move the full `LanceAssignmentView` into the `Assignments` tab.
- Give deployment requirements and current assignments more vertical and horizontal room.
- Consider splitting requirements and assignments vertically or with a small internal split pane if both tables need independent space.
- Add a compact summary at the top showing active deployment shortfalls by contract.
- Keep role and contract editing behavior unchanged.

### Result After Phase 6

- Users have a dedicated place to manage combat roles and contract assignments.
- The `Overview` tab no longer has to reserve large space for detailed assignment tables.
- The weekly deployment shortfall nag points to a clearer workflow.

### Validation

- Change combat roles and assigned contracts from the `Assignments` tab.
- Confirm deployment requirements update immediately after edits.
- Confirm active contract defaults and eligibility filtering still work.
- Confirm assignment changes affect scenario deployment availability as before.

## Phase 7: Build The History Tab

### Work

- Add a filtered scenario table or list for completed/resolved scenarios.
- Show selected historical scenario details, after-action report, loot/cost information, and final status.
- Keep current/unresolved scenarios focused in `Overview` while allowing resolved content to remain accessible.
- Decide whether the existing scenario table in `Overview` should hide resolved scenarios by default or simply make filtering easy.

### Result After Phase 7

- The main scenario queue becomes less cluttered during long contracts.
- Players can still review past engagements without crowding the active decision view.

### Validation

- Resolve or load a campaign with resolved scenarios.
- Confirm resolved scenarios appear in `History` with correct details.
- Confirm current scenarios remain easy to find in `Overview`.
- Confirm editing or GM removal behavior is still available where appropriate.

## Phase 8: Polish And Accessibility

### Work

- Add scenario queue filters if needed: `Current`, `Unresolved`, `Crisis`, `Strategic`, `Turning Point`, `Resolved`.
- Add or refine visual labels for high-priority scenario types.
- Check button grouping and order for primary actions: start/join/load, deploy/export/print, resolve/reset.
- Audit text wrapping, scroll behavior, and minimum sizes at common resolutions.
- Consider moving some action buttons to icons only if the surrounding UI already supports clear tooltips and this matches MekHQ conventions.
- Keep all user-facing strings in resource bundles if the surrounding code expects localization.

### Result After Phase 8

- The new layout feels intentional rather than merely rearranged.
- High-priority scenarios are easier to identify.
- The UI remains usable at smaller resolutions and cleaner at larger resolutions.

### Validation

- Manual UI pass at multiple window sizes and GUI scaling settings.
- Test with light/dark themes if available.
- Confirm no text overlaps or clipped buttons in common layouts.
- Confirm all new user-visible strings are localizable.

## Phase 9: Optional StratCon AO Integration

### Work

- Add a compact deployment/status widget to the StratCon AO tab, using the existing `TODO` in `StratConTab` as the entry point.
- Keep the full assignment editor in the Briefing Room `Assignments` tab.
- Provide enough AO-side information to reduce back-and-forth while deploying from the map.
- Avoid making the AO tab the only place where deployment requirements can be managed.

### Result After Phase 9

- Players deploying from the AO map get better context without leaving the map.
- The Briefing Room remains the complete operational hub.
- Mapless StratCon and non-map workflows remain supported.

### Validation

- Deploy from the AO map and confirm the widget updates.
- Deploy from the Briefing Room scenario table and confirm behavior remains unchanged.
- Test mapless StratCon deployment workflows.

## Suggested Implementation Order

The safest order is:

1. Renderer stability fix.
2. Scenario table extraction / nested scroll removal.
3. Tutorial panel removal from primary content.
4. Tab shell.
5. Overview layout.
6. Assignments layout.
7. History layout.
8. Polish and filters.
9. Optional AO integration.

This order keeps early changes small and lets each PR or commit produce a user-visible improvement.

## Main Files Likely Involved

- `MekHQ/src/mekhq/gui/BriefingTab.java`
  - Owns the Briefing Room layout, mission selector, scenario table, selected scenario view, action buttons, refresh behavior, and focus-on-scenario behavior.

- `MekHQ/src/mekhq/gui/view/MissionViewPanel.java`
  - Currently mixes mission/contract details, large StratCon tutorial content, and the scenario table.

- `MekHQ/src/mekhq/gui/view/LanceAssignmentView.java`
  - Owns deployment requirements and current assignment tables.

- `MekHQ/src/mekhq/gui/view/AtBScenarioViewPanel.java`
  - Owns AtB/StratCon selected scenario detail rendering.

- `MekHQ/src/mekhq/gui/view/ScenarioViewPanel.java`
  - Owns non-AtB selected scenario detail rendering.

- `MekHQ/src/mekhq/gui/model/ScenarioTableModel.java`
  - Owns scenario table columns, values, rendering, and sorting data.

- `MekHQ/src/mekhq/gui/adapter/ScenarioTableMouseAdapter.java`
  - Owns scenario table context menu actions, including deployment and editing.

- `MekHQ/src/mekhq/gui/StratConTab.java`
  - Candidate for later AO-side deployment context.

- `MekHQ/resources/mekhq/resources/*.properties`
  - User-visible strings and help/glossary content.

## Compatibility Notes

- Preserve `BriefingTab.focusOnScenario(int targetId)` behavior because reports and dialogs rely on it to jump users to a scenario.
- Preserve `ScenarioTableMouseAdapter` deployment behavior, including mapless deployment through `MaplessStratCon.deployWithoutMap(...)`.
- Preserve existing scenario action button enablement rules from `refreshScenarioView()`.
- Preserve current mission action behavior: add, edit, complete, delete, and GM scenario generation.
- Avoid making StratCon-only UI appear for non-StratCon campaigns.
- Keep GM-only actions guarded by GM mode.

## Open Questions

- Should `Overview` show all visible scenarios with filters, or default to current/unresolved scenarios and leave resolved scenarios to `History`?
- Should mission selector and mission action buttons be shared above all tabs, or live only in `Overview`?
- Should `Assignments` include all active contracts at once, or default to the selected mission/contract with an option to show all?
- Should the compact deployment summary in `Overview` be read-only, or allow quick role/contract edits?
- Should scenario action buttons remain text buttons for consistency, or be grouped more compactly after the layout is stable?

## Risks

- Moving the scenario table can break scenario selection, sorting, or context menus if ownership is not handled carefully.
- Moving assignment tables can break update timing if event handlers still assume the old component hierarchy.
- Tabbed UI can hide important context if `Overview` becomes too sparse.
- Fixed-size Swing components may still constrain resizing unless each affected child panel is audited.
- UI changes are hard to validate with automated tests, so manual campaign testing is important.

## Definition Of Done

- The Briefing Room opens to an `Overview` tab that clearly prioritizes scenario selection and briefing details.
- The scenario queue has one obvious scrollbar and handles long contracts comfortably.
- The large tutorial content no longer consumes central screen space.
- Deployment requirements and current assignments are available in a dedicated `Assignments` tab with stable scrolling/rendering.
- Resolved scenario review is available without cluttering the active scenario workflow.
- Existing scenario actions, mission actions, report links, and StratCon deployment paths still work.
- The UI behaves reasonably at common window sizes and GUI scaling settings.