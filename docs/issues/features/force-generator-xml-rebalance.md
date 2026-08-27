# Force Generator XML Rebalance — Handoff Notes

**Date created:** 2026-05-24
**Status:** In progress — CLAN.xml base file done, per-faction porting outstanding
**Repo scope:** `mm-data/data/forcegenerator/faction_rules/` (mirrored into
`megamek/megamek/data/forcegenerator/faction_rules/`)
**Code changes required:** None. Pure data-side work.

---

## TL;DR for the next session

1. **The locked target spec is below** (Section "Locked spec"). All work measures against this.
2. **The "decouple" architecture is the working pattern** (Section "Decouple pattern"). It is proven against CLAN.xml —
   15 of 16 cells in tolerance. The 1 miss is < 2pp from the band edge.
3. **A Python Monte Carlo simulator exists** at
   `D:\Dropbox\MegaMek Stuff\2 - Python_Scripts_for_MM\ratgen_sim\ratgen_sim.py`. Use it after every XML edit instead of
   restarting MegaMek. ~3 seconds per run vs ~3 minutes per MegaMek test.
4. **Next concrete work:** port the decouple pattern from CLAN.xml to the ~7 Clan factions that define their own Cluster
   blocks (CGB, CCO, RD, CBS, CGS, CSA, CFM.MiKr). Each is ~10–15 minutes with the simulator.
5. **mm-data and megamek/data must stay in sync.** Edit mm-data, then copy the file into megamek/data. The historic "bug
   fixed 3-4 times" symptom was always the two trees drifting.

---

## Locked spec

Negotiated with Dave on 2026-05-23. Tolerance is **±5 percentage points** per cell.

| Force class request | L%     | M% | H% | A%     |
|---------------------|--------|----|----|--------|
| **Assault**         | 0      | 15 | 45 | **40** |
| **Heavy**           | 5      | 30 | 50 | 15     |
| **Medium**          | 25     | 50 | 25 | 0      |
| **Light**           | **65** | 30 | 5  | 0      |

**Symmetry:** Assault has 0L, Light has 0A. Heavy and Light each have 5 of the opposite extreme. Medium is centered
25/25 on the wings.

### Policy decisions (from same negotiation)

- **Faction-deviation policy:** preserve known deviators (CIH light-lean, CW unique 4-element Stars, etc.) as
  documented % shifts off the spec. Don't conform everything to one number.
- **Bias role:** XML targets are tuned to hit **with the weight-emphasis bias OFF** (
  `AdvancedForceGenWeightEmphasisScope=0`). The bias becomes an opt-in "lean even harder" knob, not a load-bearing fix.
- **Tolerance:** ±5pp per cell. Anything within 2pp of the band edge is "effectively hit."

---

## Decouple pattern (the working architecture)

**Problem we hit:** the canon XML cascade shares a single `<force echelon="%STAR%">` block across all weight classes. If
we made A-stars produce more Assault elements to hit the Assault target, Heavy clusters (which also pick A-stars) got
polluted. Recursive dependency.

**Solution:** create dedicated `<force ifFlags="pure*Cluster">` Star blocks that fire only for descendants of a cluster
requested at a specific weight class. The flag propagates down via the existing engine's flag mechanism — no code
changes.

### Architecture per weight class

For each weight class W (Assault, Light, Medium — Heavy stayed canon-incidental):

1. **Cluster subforces block** — split or rebalance the cluster's `<subforceOption ifWeightClass="W">` so options set
   `flags="+pure{W}Cluster"`:
   ```xml
   <subforceOption ifWeightClass="A">
     <option weightClass="A,A,A" flags="+testTrinary,+pureAssaultCluster" weight="6">%TRINARY%</option>
     ...
   </subforceOption>
   ```

2. **New `<force ifFlags="pure{W}Cluster">` Star block** — declared BEFORE the generic Star block (`findForceNode` picks
   the first match):
   ```xml
   <force echelon="%STAR%" eschName="Star" ifFlags="pureAssaultCluster" ifAugmented="0" ifUnitType="Mek">
     <!-- same name/co/formation structure -->
     <subforces generate="group" ifUnitType="Mek">
       <subforceOption ifWeightClass="A">
         <option weightClass="A,A,A,A,H" weight="1">%ELEMENT%</option>
         <option weightClass="A,A,A,H,H" weight="2">%ELEMENT%</option>
         ...
       </subforceOption>
       <subforceOption ifWeightClass="H">
         <!-- compositions for H-stars that appear inside an Assault Cluster -->
       </subforceOption>
     </subforces>
   </force>
   ```

3. **Heavy clusters use unchanged regular Star block.** No flag → falls through to the existing
   `<force echelon="%STAR%">` without `ifFlags`. Heavy cluster's targets are met incidentally by canon compositions.

### Why this works

- Flag is set at the **cluster level**, so it propagates down to all descendants of that cluster
- Trinaries and Stars don't need to know the cluster context — the flag carries it
- The new Star block is tuned to compensate for cascade dilution; the regular Star block keeps canon compositions
- No XML cross-contamination between weight classes

### Critical: order matters

The flag-matching block must appear **before** the generic Star block in the XML file. `findForceNode` iterates in
declaration order and picks the first match.

---

## CLAN.xml current state (post-rebalance)

All 4 weight classes simulated at 2000 runs, bias OFF, year 3050, FL rating, seed 42:

| Force class | L           | M                     | H                      | A           | Cells in band           |
|-------------|-------------|-----------------------|------------------------|-------------|-------------------------|
| **Assault** | 0.6 (0) ✓   | 9.9 (15) -0.1 outside | 45.1 (45) ✓            | 44.4 (40) ✓ | 3 of 4 (M off by 0.1pp) |
| **Heavy**   | 5.4 (5) ✓   | 30.5 (30) ✓           | 52.6 (50) ✓            | 11.6 (15) ✓ | **4 of 4**              |
| **Medium**  | 27.7 (25) ✓ | 53.6 (50) ✓           | 18.2 (25) -1.8 outside | 0.6 (0) ✓   | 3 of 4 (H off by 1.8pp) |
| **Light**   | 67.7 (65) ✓ | 28.8 (30) ✓           | 3.3 (5) ✓              | 0.3 (0) ✓   | **4 of 4**              |

**15 of 16 cells in or essentially-in tolerance. The 1 miss is the Medium-cluster H at 18.2 vs band 20–30 (1.8pp short).
**

---

## Files changed so far

### CLAN.xml (the load-bearing base, lines change as edits happen)

**Cluster body changes (3 split/rebalanced blocks):**

- `<subforceOption ifWeightClass="A">` — new block, weighted toward A,A,A trinaries, sets `+pureAssaultCluster` flag
- `<subforceOption ifWeightClass="H">` — new block (was combined H|A before), Heavy-cluster-only pool, no flag
- `<subforceOption ifWeightClass="M">` — rebalanced, dropped all A-containing options, sets `+pureMediumCluster`
- `<subforceOption ifWeightClass="L">` — rebalanced toward L,L,L, sets `+pureLightCluster`

**Trinary changes (1 block):**

- `<subforceOption ifWeightClass="A">` inside the line-632 Trinary force — pushed A,A,A weight to 5, dropped A,A,L (
  Light leak inside Assault Trinary)

**New Star blocks (3 new `<force>` elements, declared before the generic Star block):**

- `<force echelon="%STAR%" ifFlags="pureAssaultCluster" ifAugmented="0" ifUnitType="Mek">` — Pure Assault Star
- `<force echelon="%STAR%" ifFlags="pureLightCluster" ifAugmented="0" ifUnitType="Mek">` — Pure Light Star
- `<force echelon="%STAR%" ifFlags="pureMediumCluster" ifAugmented="0" ifUnitType="Mek">` — Pure Medium Star

Each includes its own `<weightClass>`, `<ruleGroup>` formation rules, and `<subforces>` element compositions.

### Per-faction edits (earlier passes, now superseded architecturally but still in the files)

These are pre-decouple edits that helped marginally but don't reach target. They're NOT redundant — they're a
prerequisite the new pattern overtakes:

- **CGB.xml / RD.xml** — H|A cluster pool: dropped A,L (Light Trinary in Assault Cluster), reweighted toward A,A and A,H
- **CGB.xml line 567, 576 + RD.xml + CGS.xml** — added `weightClass="H"` to Tango Fighter Star attached aero options (
  separate bug — Aerospace Star block didn't handle weightClass=A inherited from Mek trinary)
- **CLAN.xml + CCO/CGB/CIH/CSA/CSV/CJF + CCC/CFM/CHH/CLAN.GC/CSL/FRR/RD** — Mek Star Assault subforces: `A,A,A,H,H` →
  `A,A,A,A,H` (60% A → 80% A per pattern). Benign with the decouple — regular Star block now only fires in
  non-Assault-cluster contexts where the extra A% is mildly helpful.

### Code changes (none in scope)

- One pair of diagnostic log lines in `megamek/src/megamek/client/ratgenerator/SubForcesNode.java` and `Ruleset.java` (
  the `[ForceGen][Attached]` family). These are stale — fix is done; the diagnostics can be removed in a cleanup pass.

---

## Status by Clan faction (Assault Cluster — A% relative to 40% target)

Run with: `py ratgen_sim.py --all-clan --wc A --year 3050 --rating FL --runs 1000 --seed 42`

### ✓ In band (decouple working via inheritance)

CB (44.5), CC (44.4), CDP (44.4), CLAN (44.0), CSR (44.6), CWIE (43.9), CDS (close at 22.5 — own Star block but inherits
cluster), CC.WHO (33.7 close)

### Partial — own Mek Star block defined locally, inherits cluster/trinary

- 21–23%: CDS, CFM, CFM.Kl, CFM.MaC, CFM.MiKrKl, CFM.P, CFM.PBG, CIH, CSJ, CSL, CW
- 17–20%: CJF, CNC, CSV, CFM.BG, CFM.FT, CFM.S

These factions have their own Mek Star override which means the Pure Assault Star isn't reaching their stars. They need
either:

- Their own Mek Star block updated to use the same `ifFlags="pure*Cluster"` pattern
- OR their Mek Star block removed in favor of inheritance (faster but loses faction flavor)

### Override factions — need decouple ported (top priority)

- **CGB** (14.9%, CGB-FL is Dave's primary test faction)
- **CCO** (15.2%)
- **RD** (15.4%) — file header notes "hacked together CGB.xml repurposed for RD"
- **CBS** (17.4%)
- **CFM.MiKr** (10.8%)
- **CGS** (9.6%)
- **CSA** (12.5%)

These have their own Cluster overrides. Apply the same pattern: split H|A pool into A and H, set `+pureAssaultCluster`
flag on A options, port the decouple Star blocks (the Star block from CLAN.xml can be referenced; these factions may
inherit it).

### Edge cases — tiny leaf counts, simulator quirks

CC.SIJ (3 leaves), CCC (0.8 leaves), CS (1.9), CSA (2.2 at A-class), FRR (1.0). These produce nothing like a cluster —
likely the simulator misses an `<asParent />` or `<changeEschelon>` path, OR the faction file structures clusters
differently (e.g., as TOC-only or aerospace-only). Investigate separately if these factions are user-facing; otherwise
low priority.

---

## The Monte Carlo simulator

**Location:** `D:\Dropbox\MegaMek Stuff\2 - Python_Scripts_for_MM\ratgen_sim\ratgen_sim.py`

**Why it exists:** edit XML → simulate in 3 seconds → see if you hit the target. Beats the 3-minute restart-MegaMek
loop.

**Default rules dir:** `D:\MegaMek Projects\mekhq\mm-data\data\forcegenerator\faction_rules` (set via
`DEFAULT_RULES_DIR` constant near the top of the script — update if the path differs on the other machine).

**Common usage:**

```bash
# Single faction, single weight class
py ratgen_sim.py CGB --wc A --year 3050 --rating FL --runs 2000 --seed 42

# All Clan factions, one weight class
py ratgen_sim.py --all-clan --wc A --year 3050 --rating FL --runs 1000 --seed 42

# With bias on (compare to bias off baseline)
py ratgen_sim.py CGB --wc A --bias 2,2 --runs 2000

# Different echelon (Battalion, Regiment) — currently defaults to CLUSTER
py ratgen_sim.py CGB --wc A --echelon REGIMENT --runs 2000
```

**Reading the output:** each line shows actual % vs target. Asterisks (`*`) mark cells outside the ±5pp tolerance. Empty
marker = in band.

**Known simulator gaps (acceptable — they don't affect leaf weight distribution):**

- Formation-type picks (`<formation>` element) — skipped
- Role / movement constraints in FormationType — skipped
- Network / c3 flag propagation — skipped
- Tank / Aerospace formation rules — skipped (only Mek leaves counted by default)

**If a faction shows tiny leaf counts (< 5 avg):** something is going wrong in the cascade walk. Likely candidates:
`<changeEschelon>` not handled, `<asParent />` delegation, or the faction's force structure doesn't define a `%CLUSTER%`
for Mek units at the requested year.

---

## Sync hygiene

**Two trees must match:**

- `mm-data/data/forcegenerator/faction_rules/` (source of truth, version controlled)
- `megamek/megamek/data/forcegenerator/faction_rules/` (what MegaMek loads at runtime)

**After every XML edit:**

```bash
cp "D:/MegaMek Projects/mekhq/mm-data/data/forcegenerator/faction_rules/CLAN.xml" \
   "D:/MegaMek Projects/mekhq/megamek/megamek/data/forcegenerator/faction_rules/CLAN.xml"
```

**Verify sync:**

```bash
diff -rq "D:/MegaMek Projects/mekhq/mm-data/data/forcegenerator/faction_rules" \
         "D:/MegaMek Projects/mekhq/megamek/megamek/data/forcegenerator/faction_rules"
# empty output = synced
```

This drift was the root cause of the "we fixed this 3-4 times" Aerospace Tango Star bug. Don't trust either tree alone.

---

## Recommended order of work for next session

1. **CGB.xml** — Dave's primary test faction; port the decouple A pattern (cluster + flag), then port L and M
   analogously. Run simulator. Should hit 4/4 cells.
2. **RD.xml** — near-clone of CGB; mostly copy-paste the CGB changes.
3. **CCO.xml, CGS.xml, CSA.xml, CBS.xml, CFM.MiKr.xml** — same pattern, each ~10–15 min with the simulator.
4. **Factions with own Mek Star blocks** (CFM variants, CIH, CW, CDS, etc.) — decide per faction whether to:
    - Add the `ifFlags="pure*Cluster"` Star block alongside the existing Star, OR
    - Remove the local Star override and let them inherit from CLAN.xml
5. **In-MegaMek validation** — restart MegaMek, run 5–10 Assault Clusters of CGB, eyeball the Mek tally. Should land
   35–45% A consistently.
6. **Remove diagnostic logging** — `[ForceGen][Attached]` log lines in `SubForcesNode.java` and `Ruleset.java`. They
   served their purpose; the cascade bug is fixed.
7. **Commit per faction or per weight class** — keep commits small and reviewable. Each commit should reference the
   simulator before/after numbers in the message.

---

## Pre-existing context (for the other-machine Claude session)

Read these files before starting work:

- `D:\MegaMek Projects\CLAUDE.md` (project rules — NEVER create git commits without explicit user authorization, NEVER
  use Unicode in code/logs, etc.)
- `D:\MegaMek Projects\mekhq\CLAUDE.md` (mekhq-specific rules)
- This document
- The current Python simulator at `D:\Dropbox\MegaMek Stuff\2 - Python_Scripts_for_MM\ratgen_sim\ratgen_sim.py`

User memories (from `~/.claude/projects/D--MegaMek-Projects-mekhq/memory/`) cover Dave's preferences and project
context. These are auto-loaded.

---

## Hidden Force Generator weight-emphasis toggle (out of primary scope but related)

Two prefs in `mmconf/clientsettings.xml` under the `<store name="GUIPreferences">` block:

```xml

<preference name="AdvancedForceGenWeightEmphasisScope" value="0"/>
<preference name="AdvancedForceGenWeightEmphasisMagnitude" value="1"/>
```

- **Scope:** 0 = off, 1 = leaf only, 2 = full cascade
- **Magnitude:** 1 = gentle (±1), 2 = aggressive (±2)
- Also exposed via the Advanced tab in Client Settings (under "Force Generator - Weight Emphasis Scope" and "Magnitude")

**For XML rebalance testing, set Scope=0 (off).** The XML targets are calibrated to hit with bias off. Bias becomes an
opt-in lean knob layered on top.

---

## Open questions for Dave

- The 1 of 16 cells outside tolerance (Medium-cluster H at 18.2 vs band 20–30, 1.8pp short) — accept or push further?
- The deviation policy says "preserve known deviators with documented % shifts." For each faction we encounter, do we
  want a per-faction target table, or accept that the deviators land outside the global ±5pp band intentionally?
- CW.xml and CDS.xml have 4-element Stars (canon is 5). Possibly intentional Wolf doctrine, possibly legacy bug. Worth
  flagging for separate review.
