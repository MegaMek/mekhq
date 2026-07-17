# Faction Diplomacy Data

This document describes the faction diplomacy data in `data/universe/factionDiplomacy/`,
which replaces the monolithic `factionhints.xml`. The data details diplomatic relationships
between factions and is used in computing probabilities of conflicts between various
factions.

The format and the semantics of every section were carried over 1:1 from the XML file
originally written and documented by **Neoancient**; only the spelling of the tags changed
(see [Legacy XML mapping](#legacy-xml-mapping)). Neoancient's original documentation is
preserved verbatim in the [appendix](#appendix-original-factionhintsxml-documentation) — it
remains the authoritative description of how this data is interpreted.

Each file covers one conflict (`3059_operationBulldog.yml`, `3151_battleOfTerra.yml`, ...)
or one standing relationship (`2571-2781_starLeague.yml`,
`3020-3067_federatedCommonwealth.yml`, `3029-3081_comStarPresence.yml`, ...). A loader
should read every `*.yml` file in the data directory and merge the sections; no file has meaning
on its own beyond grouping related entries for maintainability.

## File naming

Files are prefixed with the year span they cover, an underscore, and the camelCase conflict
name, so the directory lists chronologically:

- `2398-2404_firstAndurienWar.yml` — bounded conflict (first start year to last end year
  across every entry in the file)
- `3059_operationBulldog.yml` — starts and ends in the same year
- `3144-present_combineFedSunsWar.yml` — ongoing at the end of the covered timeline
- `clanRivalries.yml` — no prefix: perpetual relationships with no dates

The prefix is documentation only; loaders must derive dates from the file contents, never
from the filename.

## Schema

Every file may contain any of the five top-level sections:

### `wars`

Limited periods of intense fighting. Bordering factions are twice as likely to fight if at
war than otherwise (the war doubles the border weight used by contract generation). A civil
war — the same faction listed twice in one party — is also what allows a faction to be
generated as its own enemy; without such an entry, employer-versus-itself contracts are
never offered.

The `name` is player-visible: it is shown by the diplomacy report and map tooltips for the
factions involved.

```yaml
wars:
  - name: Operation Bulldog
    start: 3059-05-20
    end: 3059-08-13
    parties:
      - factions: [SL, CSJ]
      - factions: [DC, CSJ]
```

### `alliances`

Bordering factions which are unlikely to fight each other. They are not necessarily formal
alliances. How strongly an alliance suppresses conflict depends on who is involved
(verified against `RandomFactionGenerator`):

- Between two non-Clan factions, an alliance prevents conflict generation entirely.
- Between two Clans, allied borders still generate conflicts at one quarter weight
  (allied Clans keep testing each other in trials).
- A Clan employer attacking a non-Clan ally is not blocked at all — alliances only
  restrain the Inner Sphere side of a mixed pairing.

### `rivalries`

Clan rivalries (by data convention — the code accepts any factions). A rivalry doubles the
border weight and is cumulative with wars and alliances: a pair that is both at war and
rivals gets four times the base weight.

```yaml
rivalries:
  - factions: [CJF, CW]
  - factions: [CBS, CSA]
    start: 3059-02-16
```

### `neutralFactions`

Each named faction is considered to be allied with all factions not explicitly listed in one
or more entries under `exceptions`. Exception entries may carry `start` and/or `end` dates.
A neutral faction is never offered as an enemy (in either direction) except against its
listed exceptions — and an active `wars` entry overrides neutrality automatically, which is
how a generally neutral faction like ComStar can still fight in Operation Bulldog or the
Jihad without a permanent exception.

Neutral factions still appear as employers. When a neutral faction employs against an enemy
it is not at war with, offensive contract types are downgraded (planetary assault becomes
garrison duty, relief duty becomes security duty), so neutral employers mostly offer
defensive work.

```yaml
neutralFactions:
  - faction: CS
    exceptions:
      - factions: [PIR, WOB]
      - factions: [CJF, CW]
        start: 3052-05-01
```

### `containedFactions`

Factions which are located within another faction and do not control any worlds there, but
have an independent military force that may attack or be attacked. Examples include
Wolf-in-Exile (in ARDC and later LA) and Nova Cat (in DC), and even temporary situations like
the Second Star League operating in DC space during Operation Bulldog.

- `host` is the faction that controls the space; `contained` is the faction within it.
  Contained factions also become available as employers even though they control no worlds.
- The optional `fraction` indicates what portion of the host faction's border should be
  apportioned to the contained faction. An explicit fraction *splits* the border weight: the
  contained faction gets `fraction` of it and the host keeps the remainder (so
  `fraction: 1.0` gives the whole border to the contained faction and none to the host). If
  omitted (or `0.0`), the full border is apportioned to *both* factions with no split.
- The optional `opponents` list restricts attacks by and against the contained faction to the
  factions listed (e.g. during Operation Bulldog, SL forces within the DC attack only Smoke
  Jaguar, and DC and its neighbors other than the Jaguars do not attack SL forces).

```yaml
containedFactions:
  - host: DC
    contained: SL
    start: 3059-05-20
    end: 3059-08-13
    fraction: 0.2
    opponents: [CSJ]
```

## Date handling

War, alliance, and rivalry entries have optional `name`, `start`, and `end` fields. Entries
without `start` and `end` dates are considered perpetual. Each entry under `parties` lists
the faction codes involved; any party may carry its own `start` or `end` date, which
overrides the value of the containing war or alliance for those factions.

More than two factions may be listed in the same `factions` list, provided that they are all
at war with (or allied with, or rivals of) each other. In wars with two or more sides that do
not attack each other, each combination must be given in a separate entry. In civil wars, the
same faction is listed twice in the entry.

Make dates day-accurate where the sources allow: exact `start` and `end` dates are not just
range bounds — the campaign fires player-facing diplomatic news ("war declared", "peace
treaty") on the precise day a war, alliance, or rivalry starts or ends for the player's
faction.

Faction codes must match a file in `data/universe/factions/`. Note that some factions change
code over time (Clan Wolf is `CW` through 3142 and `CWE`, the Wolf Empire, from 3143 on).

## Legacy XML mapping

Every concept in `factionhints.xml` maps directly to the YAML schema. Semantics are
unchanged; only names differ:

| XML                                | YAML                            | Notes |
|------------------------------------|---------------------------------|-------|
| `<war>`                            | `wars` entry                    | identical |
| `<alliance>`                       | `alliances` entry               | identical |
| `<rivals>` / `<parties>`           | `rivalries` entry               | one YAML entry per party line |
| `<neutral faction="...">`          | `neutralFactions` entry         | identical |
| `<exceptions>`                     | `exceptions` entry              | identical, including dates |
| `<location>`                       | `containedFactions` entry       | renamed for clarity |
| `<outer>`                          | `host`                          | renamed for clarity |
| `<inner>`                          | `contained`                     | one contained faction per entry (see below) |
| `<fraction>`                       | `fraction`                      | identical |
| `<opponents>`                      | `opponents`                     | comma list becomes a YAML list |
| `<parties>A,B,C</parties>`         | `- factions: [A, B, C]`         | identical, including per-party `start`/`end` overrides |
| `name`/`start`/`end` attributes    | `name`/`start`/`end` fields     | identical |

One deliberate difference from the legacy file:

- **One `contained` faction per entry.** The XML nominally allowed multiple `<inner>` tags in
  one `<location>`, but the MekHQ parser only ever kept the last one, silently dropping the
  rest. The YAML schema makes the working behavior explicit: write one entry per contained
  faction.

One implementation note: the parser stores an omitted `fraction` as `0.0` internally, and the
consuming code skips the border split whenever the fraction is not greater than zero — which
is exactly how the documented "full border is apportioned to both" behavior is implemented.
Omitted and explicit `0.0` are therefore identical, while any positive fraction (including
`1.0`) splits the border between contained faction and host.

### Custom legacy files

`factionhints.xml` is ignored whenever the `factionDiplomacy` directory is present. MekHQ
compares the XML's contents against the loaded YAML data at startup: if they differ (for
example, a player carried a customized XML into a new install expecting their homebrew wars
to work), it logs a warning and shows a notice pointing the player at the directory. Custom
conflicts belong in their own YAML file there (e.g. `myCustomWars.yml`) — it is loaded
alongside the shipped files, and survives updates as long as the filename doesn't collide
with a shipped one. This is also why the shipped XML and the YAML directory must be kept in
perfect sync: any divergence triggers that player-facing warning.

## Appendix: original factionhints.xml documentation

Preserved verbatim from the XML header. Read the tag names through the
[mapping table](#legacy-xml-mapping) above.

```text
factionhints.xml
written by Neoancient

This file details relationships between factions and is used in computing probabilities of
conflicts between various factions.

<war> tags indicate limited periods of intense fighting. Bordering factions are twice as likely to
fight if at war than otherwise.

<alliance> tags are used for bordering factions which are unlikely to
fight each other. They are not necessarily formal alliances.

<rivals> tags are used for Clan rivalries. They are cumulative with wars and alliances.

<location> tags are used for factions which are located within another faction and do not control
any worlds there, but have an independent military force that may attack or be attacked. Examples
include Wolf-in-Exile (in ARDC and later LA) and Nova Cat (in DC), and even temporary situations
like the Second Star League operating in DC space during Operation Bulldog.

War, alliance, and rivals nodes each have optional name, start, and end attributes. Those without
start and end nodes are considered perpetual. Child nodes all have <parties> tags with a pair of
faction codes separated by a comma. Any of the <parties> tags can have optional start or end
attributes which override that value for that pair of factions. More than two factions may be listed
in the same entry, provided that they are all at war with (or allied with or rivals of) each other.
In cases of wars with two or more sides that do not attack each other, each combination must be
given in a separate entry. In cases of civil wars, the same faction is listed twice in the entry.

<neutral> nodes work like the inverse of alliance nodes. The faction named in the required
"faction" atribute is considered to be allied with all factions not explicitly named by one or more
<exception> tags. The <neutral> tag does not take start or end dates, but the <exception> tags may.

<location> nodes have the same optional name, start, and end attributes as <war>, <alliance>, and
<rivals> tags. The faction that controls the space in question is marked as <outer> and the faction
contained within that space is marked as <inner>. An optional <fraction> tag indicates what portion
of the outer faction's border should be apportioned to the inner faction. If empty, the full border
is apportioned to both. The optional <opponents> tag restricts attacks by and against the inner
faction to those listed (e.g. during Operation Bulldog, SL forces within the DC attack only Smoke
Jaguar and not DC or any of its neighbors, and DC and its neighbors (other than SJ) do not attack SL
forces).
```
