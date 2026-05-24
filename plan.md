# Campaign Options Dialog Improvement Plan

## Scope

Improve the entire Campaign Options dialog opened from File -> Campaign Options, not just Human Resources. The current issues come from shared layout and construction patterns used by most sections and subsections, so the fix should target the dialog framework and then migrate individual tabs onto that framework.

Primary goals:

- Remove horizontal scrolling from normal dialog sizes.
- Keep only one predictable vertical scroll area for the active content.
- Reduce empty header/help space so useful controls appear in the first viewport.
- Make sections visually centered and consistently aligned across every category.
- Make the dialog appear quickly by avoiding construction of every tab before first paint.
- Preserve existing campaign option behavior, preset load/save behavior, and option-change confirmation flows.

## Investigation Summary

The menu action in `MekHQ/src/mekhq/gui/CampaignGUI.java` constructs `CampaignOptionsDialog` before showing it. `CampaignOptionsDialog` immediately constructs `CampaignOptionsPane`, calls `initialize()`, then the base dialog packs the fully built component tree before `setVisible(true)` runs.

`CampaignOptionsPane.initialize()` eagerly creates every top-level category and almost every nested subtab:

- General
- Human Resources: Personnel, Biography, Relationships, Turnover and Retention, Salaries
- Advancement: Awards and Randomization, Skills, Abilities
- Logistics and Maintenance: Supplies and Acquisition, Repairs and Maintenance
- Strategic Operations: Finances, Markets, Systems, Rulesets

The UI problems are shared, not isolated:

- `CampaignOptionsPane.createTab(...)` wraps parent tab panes in a `JScrollPane`, while `createGeneralTab(...)` and subtab contents also use scrollable-style panels. This creates nested scrolling and makes horizontal scrollbars likely.
- `CampaignOptionsUtilities.createSubTabs(...)` wraps each subtab panel in multiple holder panels, then sizes quote/help labels from preferred widths. This locks content to preferred-size behavior instead of viewport-size behavior.
- `CampaignOptionsHeaderPanel` reserves a large tooltip/tip area with blank `<br>` lines, plus a 100px faction logo, on nearly every subtab. On small or medium windows, the first viewport is mostly title, icon, separators, and empty help space.
- `CampaignOptionsStandardPanel` appears intended to enforce a standard preferred width, but it creates an anonymous `JPanel` whose `getPreferredSize()` and `getMinimumSize()` overrides are never applied to the actual panel. This is misleading and should be removed or replaced with real sizing behavior.
- Many tab layouts are hand-built with fixed column assumptions and `GridBagConstraints` reuse. Some tabs use wide grids, fixed-width labels, or preferred-size based wrappers that do not adapt to viewport width.
- There is duplicated responsibility in category creation: the category builder methods add top-level tabs and `initialize()` also wraps returned category panes through `createTab(...)`. This should be simplified to one tab registration path.
- `AbilitiesTab` does expensive work twice: its constructor builds ability info and refreshes all category panels, then `CampaignOptionsPane` asks it to create the category panels again.
- `SkillsTab` creates many hidden controls up front. Each skill panel includes 11 labels, 11 spinners, and 11 combo boxes even though the detailed controls start hidden.

## Design Direction

Treat Campaign Options as a settings workspace rather than a giant eagerly rendered form.

Recommended structure:

- A single modal dialog shell with fixed title/actions and a central settings workspace.
- A left navigation tree or list for category/subcategory navigation, replacing the current three-level tab stack over time.
- A search/filter box at the top of the navigation or content area so users can find options by label, tooltip text, or metadata flag.
- One active content panel at a time, inside one vertical `JScrollPane` with horizontal scrolling disabled.
- Content panels that use responsive form/group layout: two columns when the viewport is wide, one column when narrow.
- A compact contextual help area that updates on focus/hover, instead of every subtab reserving several blank lines in the header.

This can be delivered incrementally without changing all option data models at once.

## Exact Implementation Plan Order

This is the order we should actually work in. The phase sections below describe the major work themes; this list is the implementation sequence for the foundation pass.

### 1. Build the new dialog shell

Create the new Campaign Options workspace before redesigning individual screens.

Implementation tasks:

- Keep `CampaignOptionsDialog` as the modal dialog and preserve the existing bottom actions: Apply Settings, Save Preset, Load Preset, Cancel Changes.
- Convert `CampaignOptionsPane` into a shell/controller with three main regions:
  - Left navigation area.
  - Filter/search field above the navigation.
  - One central active-page content area.
- Use a `JSplitPane` or equivalent `BorderLayout` layout so the navigation has a stable width and the active page gets the remaining space.
- Select the General page by default.

Acceptance criteria:

- The dialog opens with the existing button behavior intact.
- The left navigation and filter field are visible.
- The General page appears in the central content area.
- No individual screen redesign is included yet.

### 2. Create the page registry and route model

Represent every existing category and subsection as a route in a navigation model.

Implementation tasks:

- Add a small page descriptor, such as `CampaignOptionsPage`, containing:
  - Stable page id.
  - Display title resource key or display text.
  - Tree path.
  - Page factory, preferably `Supplier<JComponent>`.
  - Optional apply/load hooks for pages that have been created.
- Register the current structure as tree paths:
  - General
  - Human Resources / Personnel / General
  - Human Resources / Personnel / Awards
  - Human Resources / Personnel / Medical
  - Human Resources / Personnel / Personnel Information
  - Human Resources / Personnel / Prisoners and Civilians
  - Human Resources / Biography / General
  - Human Resources / Biography / Backgrounds
  - Human Resources / Biography / Death
  - Human Resources / Biography / Education
  - Human Resources / Biography / Name and Portrait Generation
  - Human Resources / Biography / Rank
  - Human Resources / Relationships / Marriage
  - Human Resources / Relationships / Divorce
  - Human Resources / Relationships / Procreation
  - Human Resources / Turnover and Retention / Turnover
  - Human Resources / Turnover and Retention / Fatigue
  - Human Resources / Salaries / Combat
  - Human Resources / Salaries / Support
  - Human Resources / Salaries / Civilian
  - Advancement / Awards and Randomization / Randomization
  - Advancement / Awards and Randomization / XP Awards
  - Advancement / Awards and Randomization / Recruitment Bonuses
  - Advancement / Skills / Gunnery
  - Advancement / Skills / Piloting
  - Advancement / Skills / Support
  - Advancement / Skills / Utility
  - Advancement / Skills / Roleplay
  - Advancement / Abilities / Combat
  - Advancement / Abilities / Maneuvering
  - Advancement / Abilities / Utility
  - Advancement / Abilities / Character Flaws
  - Advancement / Abilities / Character Creation Only
  - Logistics and Maintenance / Supplies and Acquisition / Acquisition
  - Logistics and Maintenance / Supplies and Acquisition / Planetary Acquisition
  - Logistics and Maintenance / Supplies and Acquisition / Tech Limits
  - Logistics and Maintenance / Repairs and Maintenance / Repair
  - Logistics and Maintenance / Repairs and Maintenance / Maintenance
  - Strategic Operations / Finances / General
  - Strategic Operations / Finances / Price Multipliers
  - Strategic Operations / Markets / Personnel Market
  - Strategic Operations / Markets / Unit Market
  - Strategic Operations / Markets / Contract Market
  - Strategic Operations / Systems / Reputation
  - Strategic Operations / Systems / Faction Standing
  - Strategic Operations / Systems / A Time of War
  - Strategic Operations / Rulesets / StratCon
- Keep page ids stable so search, preferences, and future tests can target them.

Acceptance criteria:

- Every current tab/subtab has a matching tree route.
- Selecting a tree leaf can show the corresponding existing page.
- The old visual tab hierarchy can be bypassed by the tree for migrated pages.

### 3. Add lazy page creation

Make the dialog show before every page exists.

Implementation tasks:

- Page factories should not run during initial dialog construction except for the initially selected page.
- Build a page the first time its route is selected.
- Cache created pages so switching away and back preserves user edits.
- Track which page controllers have been instantiated.
- On Apply Settings, apply instantiated pages and leave non-instantiated pages unchanged.
- Preserve the current preset workflow. If preset loading needs all pages to reflect new values before a draft model exists, instantiate affected pages as a short-term compatibility step.

Acceptance criteria:

- Opening the dialog does not construct Human Resources, Advancement, Logistics, and Operations pages unless selected.
- Switching to a page constructs it once.
- Applying after editing only the General page does not accidentally reset options from unvisited pages.

### 4. Make the active content area the only normal scroll pane

Fix the shared scrollbar problem at the shell level.

Implementation tasks:

- Wrap only the active page region in a vertical `JScrollPane`.
- Set the active-page scroll pane horizontal policy to `HORIZONTAL_SCROLLBAR_NEVER`.
- Use a content host that implements `Scrollable` and tracks viewport width.
- Keep the navigation tree independently scrollable if needed, but do not nest scroll panes around tab panes and page content.
- Remove or bypass `CampaignOptionsPane.createTab(...)` for the new route-based pages.

Acceptance criteria:

- Normal pages have one vertical scrollbar in the central content area.
- The old bottom horizontal scrollbar no longer appears for normal option pages.
- The navigation tree can scroll independently only when the route list is taller than the dialog.

### 4.5 Split shell infrastructure before direct page migration

Extract the new Campaign Options shell helpers out of `CampaignOptionsPane` before migrating more routes to direct pages.

Implementation tasks:

- Create a route model class for route id, display path, search text, and top-level section lookup.
- Move the left navigation tree and filter field into a dedicated navigation panel class with a route-selected callback.
- Move the central viewport-width-tracking scroll host into a dedicated content host class.
- Keep `CampaignOptionsPane` responsible for coordinating lazy section creation, route registration, and apply/preset orchestration.
- Do not retry direct page migration in this step.

Acceptance criteria:

- The UI behaves the same as Step 4: tree/filter navigation works, lazy sections work, and legacy content still displays.
- `CampaignOptionsPane` no longer contains the tree node model or scrollable content-panel implementation.
- `CampaignOptionsPane` is smaller and easier to reason about before Step 5 resumes.

### 5. Replace the old tab-stack wrappers for migrated pages

Once tree routes exist, pages should be loaded directly instead of being packed into nested `JTabbedPane` structures.

Implementation tasks:

- Stop using `createSubTabs(...)` for route-based Campaign Options pages.
- Keep existing page creation methods, such as `personnelTab.createGeneralTab()`, but register them directly as page factories.
- Simplify or deprecate wrapper behavior in `CampaignOptionsUtilities.createSubTabs(...)` after the route model no longer depends on it.
- Remove duplicate top-level tab registration responsibility from `CampaignOptionsPane`.

Acceptance criteria:

- Selecting Human Resources / Personnel / General shows that page directly, not inside nested tabs.
- The central content area does not contain another category/subcategory tab strip.
- Existing page controllers still load values and apply changes correctly.

### 6. Fix shared width and wrapper behavior

After the shell owns scrolling, fix the common panels so pages respect the viewport.

Implementation tasks:

- Fix `CampaignOptionsStandardPanel` by removing the unused anonymous `JPanel` preferred-size override.
- Do not enforce a global minimum width inside every nested panel.
- Move any desired readable max width to the outer active-page wrapper.
- Make `createParentPanel(...)` anchor content near the top and center it only when the page is narrower than the viewport.
- Ensure common `GridBagConstraints` defaults do not cause controls to stretch or create artificial width.

Acceptance criteria:

- Pages are centered when there is extra width.
- Pages shrink to the viewport without horizontal scrolling at supported dialog sizes.
- Empty right-side whitespace is reduced without compressing controls into unreadable layouts.

### 7. Compact the shared header and help behavior

Remove the giant blank first viewport from all pages.

Implementation tasks:

- Reduce `CampaignOptionsHeaderPanel` height.
- Shrink decorative logos or remove them from settings pages.
- Remove reserved blank tooltip lines from page headers.
- Add a compact contextual help area in the shell or a collapsible help row.
- Keep existing hover/focus help text and option metadata badges.

Acceptance criteria:

- The first viewport of each page contains useful controls.
- Help text still exists, but it no longer reserves large empty space before interaction.
- Header style is consistent across all sections.

### 8. Add filter/search version 1: page filtering

Start with useful navigation search before exact option-level search.

Implementation tasks:

- Filter tree routes by page/category title.
- Preserve hierarchy for matched pages, showing ancestors of matching leaves.
- Selecting a search result navigates to that page.
- Clear search restores the full tree and current selection.

Acceptance criteria:

- Searching `salary`, `skills`, `contract`, `medical`, or `stratcon` narrows the tree to relevant pages.
- Search does not instantiate every page.
- Navigation remains keyboard and mouse usable.

### 9. Add filter/search version 2: option text indexing

After page-level search works, extend it to actual option labels and help text.

Implementation tasks:

- Index resource-bundle labels and tooltips used by options.
- Add optional keywords to page descriptors for terms that do not appear in titles.
- Show matched pages when any option text on that page matches.
- If practical, scroll to or highlight the matched option when the page is selected.

Acceptance criteria:

- Searching specific option names or tooltip terms finds the owning page.
- Search indexing does not require constructing every Swing page.

### 10. Fix heavy pages after general lazy loading works

Handle the worst construction offenders with targeted changes.

Implementation tasks:

- Fix `AbilitiesTab` so it does not build all ability category pages during construction and then build them again from `CampaignOptionsPane`.
- Build SPA cards only for the selected ability category.
- Make `SkillsTab` create collapsed skill rows first, then build detailed cost/milestone controls when expanded.
- Consider table-backed layouts for skills if the existing panel-per-skill UI remains too heavy.

Acceptance criteria:

- Opening the dialog does not build SPA cards or detailed skill controls.
- Opening one ability category does not build every other ability category.
- Skill pages are usable without freezing the dialog.

### 11. Begin screen-by-screen cleanup

Only start detailed page redesign after the shell, routing, scrolling, lazy loading, and basic search are stable.

Implementation tasks:

- Fix pages in high-impact order:
  - General
  - Human Resources / Personnel / General
  - Human Resources / Personnel / Medical
  - Strategic Operations / Markets / Contract Market
  - Advancement / Skills
  - Advancement / Abilities
  - Salaries
  - Remaining pages by reported pain.
- For each page, group controls by task, remove unnecessary empty space, and convert brittle grid layouts to shared form helpers.
- Keep each page cleanup as a focused change so regressions are easier to review.

Acceptance criteria:

- Each migrated page follows the same spacing, grouping, and scroll behavior.
- No page-specific cleanup breaks apply/cancel/preset behavior.

### 12. Move simple pages to descriptors

Once the UI shell is proven, start reducing the manual Swing duplication.

Implementation tasks:

- Introduce an option descriptor model for simple checkbox/spinner/combo pages.
- Start with checkbox-heavy pages where behavior is straightforward.
- Leave complex pages like Skills, Abilities, and Salaries custom until later.
- Move load/apply behavior toward a draft model so future lazy loading is cleaner.

Acceptance criteria:

- Simple pages can be rendered consistently from descriptors.
- Future page cleanup requires less hand-written Swing code.

## Foundation Milestone

The first implementation milestone should include tasks 1 through 5:

- New dialog shell.
- Left navigation tree.
- Filter field present, even if it only filters page titles later.
- Page registry and lazy page factories.
- One central vertical scroll pane.
- Existing pages shown directly through tree routes.
- Existing Apply, Cancel, Load Preset, and Save Preset behavior preserved.

After that milestone, tasks 6 through 10 give the broad UI/performance cleanup. Task 11 is where we begin fixing individual screens in earnest.

## Phase 1: Stabilize Shared Layout

Create a shared Campaign Options content container and migrate all subtabs through it.

Tasks:

1. Replace nested scroll panes with one scroll policy.
   - Top-level navigation and sub-navigation should not be inside scroll panes unless their own tab strip overflows.
   - Active content should be wrapped once in a vertical `JScrollPane`.
   - Set horizontal policy to `HORIZONTAL_SCROLLBAR_NEVER` for normal option content.
   - Use a scrollable panel that tracks viewport width.

2. Rewrite `CampaignOptionsUtilities.createSubTabs(...)`.
   - Stop creating multiple anonymous wrapper/holder panels per subtab.
   - Stop sizing quote/help labels from `mainPanel.getPreferredSize().width`.
   - Return content that fills the viewport width and anchors controls near the top.
   - Keep bottom quote text only if it is useful; otherwise move it into contextual help or remove it from the default layout.

3. Fix `CampaignOptionsStandardPanel`.
   - Remove the unused anonymous `JPanel` sizing override.
   - Do not enforce a global minimum width on every group panel.
   - If a max readable width is desired, implement it in the outer content container, not inside every nested panel.

4. Redesign `CampaignOptionsHeaderPanel`.
   - Reduce header height across all sections.
   - Shrink or remove decorative faction logos from settings pages.
   - Replace the reserved blank tip area with a compact help/status panel outside the scroll content, or a collapsible info row.
   - Preserve option tooltips and metadata badges.

5. Create common form helpers.
   - Add helpers for checkbox groups, label-control rows, titled groups, and responsive two-column group grids.
   - Make groups flow to one column when the viewport is narrow.
   - Use consistent insets, anchors, fill, and weight behavior.

Initial acceptance criteria:

- In a default 80 percent screen-sized dialog, regular option pages do not show a horizontal scrollbar.
- Every category and subcategory starts content near the top of the viewport.
- The first viewport contains meaningful controls, not mostly blank help/header area.
- Existing apply, cancel, load preset, and save preset buttons still work.

## Phase 2: Lazy Load Sections

Make the dialog visible before every option page exists.

Tasks:

1. Change category/subcategory creation to factories.
   - Replace `Map<String, JPanel>` inputs with `Map<String, Supplier<JComponent>>` or a small `CampaignOptionsPageFactory` record.
   - Store a placeholder for each page.
   - Build the selected page on first activation.
   - Cache the page after first construction.

2. Build only the initial route on open.
   - On dialog construction, create the shell, navigation model, General page, and buttons.
   - Defer Human Resources, Advancement, Logistics, and Operations content until selected.

3. Decouple apply from instantiated Swing controls.
   - Short-term: only apply initialized pages through their UI controls; for non-initialized pages, leave existing campaign option values unchanged.
   - Better long-term: introduce option binding descriptors so applying can read from a settings draft model rather than requiring every Swing panel to exist.

4. Fix redundant heavy builders.
   - Remove `AbilitiesTab` constructor refresh/rebuild work; build each ability page once on demand.
   - Build SPA cards only for the active ability category.
   - In `SkillsTab`, create collapsed skill rows first and build detailed milestone/cost controls only when a skill or category is expanded.

5. Cache shared assets.
   - Cache scaled/tinted images used by `CampaignOptionsHeaderPanel` if decorative images remain.
   - Avoid reloading and retinting image files for every page creation.

Initial performance targets:

- First visible dialog paint should not require creating every section and subsection.
- Opening the dialog should be dominated by the shell and default page only.
- First activation of a heavy page should show a lightweight loading placeholder or complete quickly enough that the UI does not feel frozen.

## Phase 3: Improve Navigation and Discoverability

The current tab hierarchy is difficult to scan because important settings are several tabs deep.

Tasks:

1. Add a left navigation model.
   - Categories: General, Human Resources, Advancement, Logistics and Maintenance, Strategic Operations.
   - Subsections under each category match current subtabs.
   - Keep selected route stable across dialog opens through preferences.

2. Add search/filter.
   - Search labels, tooltip text, metadata badges, and option identifiers.
   - Selecting a result navigates to the owning page and highlights or scrolls to the option.

3. Group options by user task.
   - Prefer group headings like Initiative, Personnel Cleanup, Awards, Market Refresh, Contract Pay.
   - Avoid huge generic pages where unrelated checkboxes are stacked together.

4. Make metadata more usable.
   - Keep Important, Custom System, Documented, Recommended, and Added Since badges.
   - Add filter chips/toggles for metadata flags if the search model supports it.

## Phase 4: Move Toward Data-Driven Option Pages

Many pages manually declare fields, create labels, add listeners, load values, and apply values. That makes layout fixes slow and inconsistent.

Tasks:

1. Introduce an option descriptor model.
   - Label resource key
   - Tooltip/help resource key
   - Metadata flags
   - Getter and setter against `CampaignOptions` or campaign draft state
   - Control type: checkbox, spinner, combo box, radio group, text field
   - Optional dependency/enablement rule

2. Render simple option groups from descriptors.
   - Start with checkbox-heavy pages like Personnel General, Repair, Maintenance, Systems, and Rulesets.
   - Leave complex pages like Skills, Abilities, and Salaries custom until later.

3. Centralize load/apply behavior.
   - Load values into a draft model when the dialog opens.
   - Apply changes from the draft model when the user clicks Apply Settings.
   - This enables true lazy UI loading because non-rendered pages can still have stable values.

4. Centralize validation and confirmation.
   - Preserve existing confirmation dialogs for behavior-changing options.
   - Run confirmation checks from changed option values rather than from individual Swing components.

## Phase 5: Verification

Testing needs to cover behavior and visual regressions.

Tasks:

1. Use manual perceived-load checks while developing.
   - Open the dialog before and after each foundation change.
   - Confirm the dialog appears quickly without waiting for unselected sections.
   - Confirm switching to a heavy page only pays the construction cost once.
   - Add temporary local timing only if a regression is not obvious by feel.

2. Add focused unit tests where possible.
   - Lazy page factories create pages once.
   - Non-instantiated pages keep existing option values unchanged.
   - Search index maps option identifiers to the correct route.
   - Preset loading updates the draft model and visible controls.

3. Manual visual QA matrix.
   - Windows at 100, 125, and 150 percent UI scale.
   - 1366x768, 1920x1080, and one ultrawide resolution.
   - Dark and light FlatLaf themes if both are supported.
   - Each top-level category and all current subsections.

4. Manual behavior QA.
   - Apply settings.
   - Cancel changes.
   - Save preset.
   - Load preset.
   - Startup mode.
   - Startup abridged mode.
   - Campaign upgrade mode.
   - Options that trigger confirmation dialogs.

## Work Order Summary

Use the Exact Implementation Plan Order above as the source of truth. In short:

1. Build the new shell with left navigation and a filter field.
2. Register every current page as a route.
3. Lazy-create pages on first selection.
4. Put active content in one vertical scroll pane.
5. Bypass the nested tab-stack wrappers for migrated pages.
6. Fix shared width and wrapper behavior.
7. Compact shared header/help behavior.
8. Add page-title filtering.
9. Add option-text search.
10. Fix heavy pages like Abilities and Skills.
11. Clean up individual screens one at a time.
12. Move simple pages toward descriptor-driven rendering.

## Risks and Constraints

- Current apply behavior assumes all tab controller instances and Swing components exist. Lazy loading requires either careful unchanged-value handling or a draft model.
- Preset load/save touches many option groups. The plan must preserve both normal campaign application and preset-only save behavior.
- Some options trigger side effects or confirmation dialogs. These should be driven by old/new option values, not by whether a UI page was created.
- Preferences may store selected tabs or dialog size. Navigation changes should preserve or migrate reasonable defaults.
- The dialog is used in multiple modes, not only from File -> Campaign Options. Startup, abridged startup, and campaign upgrade modes must stay supported.

## Done Criteria

The work is complete when:

- Opening Campaign Options shows the dialog quickly without waiting for every section to construct.
- Normal option pages have no horizontal scrollbar at supported default dialog sizes.
- The active page has one clear vertical scroll area.
- All categories and subsections use the same layout rules.
- Header/help areas no longer consume most of the first viewport.
- Users can find settings faster through clearer navigation and search.
- Existing option application, cancellation, preset, startup, and confirmation behavior is preserved.
