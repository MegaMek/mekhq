# Ranks in MekHQ

*Originally written by Windchild, 25-Mar-2021; updated 01-Nov-2021. Rewritten for MekHQ 0.51.01, 23-Nov-2026.*

A **rank system** is a named table of ranks. Each rank has a name for each of nine **professions**, whether it counts
as an officer rank, and a pay multiplier. A character's displayed rank depends on their rank system, their rank, and
the profession their primary role belongs to.

## Where Rank Systems Come From

| Type                          | Where it is stored                                                                    | Validation on load        |
|-------------------------------|---------------------------------------------------------------------------------------|---------------------------|
| Default Rank System           | `<MekHQ install directory>/data/universe/ranks.xml`                                   | Duplicate code check only |
| User Data Rank System         | `<MekHQ install directory>/userdata/data/universe/ranks.xml`, then `<your MegaMek user directory>/data/universe/ranks.xml` | Full validation           |
| Campaign Custom Rank System   | Inside the campaign save file                                                         | Full validation on import |

The files are loaded in the order above. Every rank system needs a unique **code**. If a code has already been
loaded, the later system is rejected and an error is written to the log, so a user data system cannot replace a
default one by reusing its code; give it a new code instead.

User data systems are then fully validated (see [Validation](#validation)). One that fails is left out and the reason
is logged. Default systems skip this step, which is one reason not to edit the default file.

A campaign can use a rank system that is in neither file. In that case the whole system is written into the save,
rather than just its code. When the campaign is loaded, if a loaded file already has a system with that code, the
file's version is used instead of the one in the save. A campaign can hold only one custom system: the one it is
using.

Upgrading MekHQ replaces `data/universe/ranks.xml`. Keep your own systems in a user data file.

### The SSLDF System

MekHQ falls back to the system with the code `SSLDF` whenever a requested code is missing, and the code expects it to
exist. If it fails to load, MekHQ logs an error and substitutes an empty placeholder, and ranks will not work
properly. You can rename it and change every rank in it, but keep the code `SSLDF`.

### Shipped Rank Systems

| Code    | Name                                       |
|---------|--------------------------------------------|
| `SSLDF` | Second Star League Defense Force           |
| `SLDF`  | Star League Defense Force                  |
| `AFFS`  | Armed Forces of the Federated Suns         |
| `AFFC`  | Armed Forces of the Federated Commonwealth |
| `LCAF`  | Lyran Commonwealth Armed Forces            |
| `LAAF`  | Lyran Alliance Armed Forces                |
| `FWLM`  | Free Worlds League Military                |
| `CCAF`  | Capellan Confederation Armed Forces        |
| `CCWH`  | Capellan Confederation Warrior House       |
| `DCMS`  | Draconis Combine Mustered Soldiery         |
| `CLAN`  | Clan                                       |
| `CG`    | Com Guard                                  |
| `WOBM`  | Word of Blake Militia                      |
| `WOBPM` | Word of Blake Protectorate Militia         |
| `MAF`   | Magistracy Armed Forces                    |
| `TDF`   | Taurian Defense Force                      |
| `MHAF`  | Marian Hegemony Armed Forces               |
| `AMC`   | Alliance Military Corps                    |
| `KA`    | KungsArmé                                  |
| `ACM`   | Aurigan Coalition Military                 |
| `ROS`   | Republic Armed Forces                      |

Each faction's data file names the rank system its people use (the `rankSystem` key in
`data/universe/factions/<code>.yml`). MekHQ uses it for NPCs such as employer liaisons and opposing commanders, and
for some faction standing events.

## Professions

Each rank has one name per profession. The groupings of roles into professions are fixed.

| Column | Profession    | Code       | Roles                                                                                              |
|--------|---------------|------------|----------------------------------------------------------------------------------------------------|
| 1      | MekWarrior    | `MW`       | MekWarrior, LAM Pilot, ProtoMek Pilot                                                              |
| 2      | Aerospace     | `ASF`      | Aerospace Pilot, Conventional Aircraft Crew                                                        |
| 3      | Vehicle       | `VEE`      | Vehicle Crew/Ground, Vehicle Crew/Naval, Vehicle Crew/VTOL                                         |
| 4      | Naval         | `NAVAL`    | Vessel Pilot, Vessel Gunner, Vessel Crewmember, Hyperspace Navigator                               |
| 5      | Infantry      | `INF`      | Battle Armor Pilot (Elemental for Clan personnel), Soldier                                         |
| 6      | Tech          | `TECH`     | MekTech, Mechanic, AeroTek, Battle Armor Tech, Astech                                              |
| 7      | Medical       | `MEDICAL`  | Doctor, Medic                                                                                      |
| 8      | Administrator | `ADMIN`    | Administrator                                                                                      |
| 9      | Civilian      | `CIVILIAN` | Every other role: Dependent, None, and all the civilian professions                                 |

The column number is the position of the name in a rank's `rankNames` list, so the order matters.

MekWarrior is the default profession. It can never be an empty profession (see below), and an unrecognised
`--CODE` falls back to it.

## Rank Tiers

Every user data or campaign rank system must have exactly **51 ranks**, in this order:

| Position | Tier       | Notes                                                                                     |
|----------|------------|-------------------------------------------------------------------------------------------|
| 0        | E0         | The starting rank. Usually "None", which MekHQ displays as no rank at all.                |
| 1–20     | E1–E20     | Enlisted ranks                                                                            |
| 21–30    | WO1–WO10   | Warrant officer ranks                                                                     |
| 31–50    | O1–O20     | Officer ranks                                                                             |

The tier names are only labels. Whether a rank counts as an officer rank is set by its own `officer` flag, not by
its position.

## Writing a Rank Name

Each entry in a rank's `rankNames` list is one of:

| Entry                        | Meaning                                                                                                    |
|------------------------------|------------------------------------------------------------------------------------------------------------|
| A name, e.g. `Sergeant`      | The rank's name for this profession.                                                                       |
| `-`, or blank                | **Empty**: this profession has no rank at this tier, and it is not offered when changing rank.             |
| `--` and a code, e.g. `--MW` | **Alternate profession**: use that profession's name at this tier. The code must be uppercase.             |
| A name, `:` and a number, e.g. `Acolyte:25` | A rank with levels. It can be given as the plain name or with a level from I up to the number (Acolyte I … Acolyte XXV). |

A profession is an **empty profession** when its E0 entry is an alternate profession and every other tier is empty.
Its whole column is then taken from the profession named at E0, following further redirects until a real
profession is reached. This is how most shipped systems give every support profession the same ranks: their E0 is
`--MW` or `--TECH` and the rest of the column is `-`.

## Writing a Custom Rank System

The easiest way is in-game, under *Campaign Options → Rank Systems*:

1. Select the system closest to what you want and press **Create Custom Rank System**. Give it a unique code and a
   name. The new system starts as a copy of the selected one.
2. Edit the table. The columns are the professions, Officer and Pay Multiplier. Default systems cannot be edited;
   only custom ones can. The Officer column is read-only here: to change which ranks count as officers, export the
   system and edit its XML (see below).
3. Set its **Rank System Type**:
   - *Campaign Custom Rank System* keeps it in this campaign's save only.
   - *User Data Rank System* makes it available to every campaign, once you press **Export User Data Rank Systems**.
4. Press **Export User Data Rank Systems** to write every user data system to
   `<MekHQ install directory>/userdata/data/universe/ranks.xml`. This reloads all rank systems.

| Button                             | Effect                                                                                                    |
|------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Export Current Rank System         | Saves the selected system to a file of its own.                                                           |
| Export User Data Rank Systems      | Writes every user data system to the user data file, then reloads.                                        |
| Export Rank Systems                | Writes every system to a file you choose. Meant for development.                                          |
| Import Individual Rank System      | Loads a single exported system as a campaign custom system.                                               |
| Import Rank Systems                | Loads a file of systems as campaign custom systems. They are discarded unless you use one for the campaign or convert it to user data and export. |
| Refresh Rank Systems From Files    | Reloads everything from disk and rechecks every character's rank.                                         |

The in-game editor does not validate the table as you type. A system that breaks the rules is only rejected (with a
log message) when it is next loaded.

### Writing the XML by Hand

Add a `<rankSystem>` block inside the `<rankSystems>` element of a user data `ranks.xml`, then press
**Refresh Rank Systems From Files** (or restart MekHQ). The stock `data/universe/ranks.xml` is a good template.

```xml
<rankSystems version="0.51.01">
    <rankSystem>
        <code>MYMERC</code>
        <name>My Mercenary Command</name>
        <description>Ranks for my mercenary command.</description>
        <useROMDesignation>false</useROMDesignation>   <!-- optional -->
        <useManeiDomini>false</useManeiDomini>         <!-- optional -->
        <rank>
            <rankNames>None,--MW,--MW,--MW,--MW,--MW,--MW,--MW,--MW</rankNames>
            <officer>false</officer>
            <payMultiplier>1.0</payMultiplier>
        </rank> <!-- E0 "None" -->
        <rank>
            <rankNames>Private,-,-,-,-,-,-,-,-</rankNames>
            <officer>false</officer>
            <payMultiplier>1.0</payMultiplier>
        </rank> <!-- E1 -->
        <!-- ...49 more <rank> blocks, for 51 in total... -->
    </rankSystem>
</rankSystems>
```

| Element             | Required | Meaning                                                                                       |
|---------------------|----------|-----------------------------------------------------------------------------------------------|
| `code`              | Yes      | Unique identifier, used internally and in saves.                                              |
| `name`              | Yes      | The name shown in menus.                                                                      |
| `description`       | No       | Shown in the rank system options.                                                             |
| `useROMDesignation` | No       | Adds ComStar ROM branch designations to rank names, and enables the ROM designation menu on characters. |
| `useManeiDomini`    | No       | Adds Word of Blake Manei Domini classes and ranks, and enables their menus on characters.     |
| `rank`              | 51 times | One per tier, in order. `rankNames` has nine comma-separated entries, one per profession column. `payMultiplier` is a salary multiplier; use `1` for no change. |

The comments after each `</rank>` are optional, but they make the file much easier to follow.

## Validation

Every user data and campaign system is checked when it loads. It is rejected if:

- its code is already in use
- it does not have exactly 51 ranks
- any profession is empty at E0 (each E0 entry must be a name or an alternate profession)
- no profession has a real name at E0 (they are all alternate professions)
- an alternate profession points at an empty entry
- alternate professions form a loop, such as a Naval entry of `--ASF` where the Aerospace entry is `--NAVAL`

The reason is written to the log.

When rank systems are reloaded, every character is rechecked. A character whose rank is empty for their profession
is moved down to the highest rank below it that has a name.

## How a Character's Rank Name Is Built

1. Find the profession for the character's primary role, following any empty-profession redirect.
2. Take that profession's name at the character's rank, following any alternate profession redirect.
3. If the system uses Manei Domini, put the character's class in front and their Manei Domini rank after.
4. If the system uses ROM designations, add the character's designation.
5. If the character has a rank level, add it as a Roman numeral.
6. Put any prisoner status in front. A rank named "None" shows only the prisoner status, or nothing.

## What Not to Do

1. Do not remove the `SSLDF` system or change its code.
2. Do not edit the default `data/universe/ranks.xml`. It is replaced on upgrade and skips validation; put your
   systems in a user data file and test them there.
3. Do not make alternate professions loop back on themselves.

## For Developers

| Class                  | Package                                  | Purpose                                                                                                         |
|------------------------|------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `Ranks`                | `mekhq.campaign.personnel.ranks`         | Static loader and store of all rank systems, plus import and export. Not to be instantiated.                     |
| `RankSystem`           | `mekhq.campaign.personnel.ranks`         | One system: code, name, description, type, ROM and Manei Domini flags, and its ranks.                           |
| `Rank`                 | `mekhq.campaign.personnel.ranks`         | One tier: a name and level count per profession, the officer flag and the pay multiplier. Its position in the system's list is its numeric rank. |
| `RankValidator`        | `mekhq.campaign.personnel.ranks`         | The checks above, and the rechecking of campaign and character rank systems.                                    |
| `AutomaticRankAssigner`| `mekhq.campaign.personnel.ranks`         | Gives ranks to generated crews and moves NPCs onto their faction's rank system.                                 |
| `Profession`           | `mekhq.campaign.personnel.enums`         | The nine professions, role mapping, and redirect resolution.                                                    |
| `RankSystemType`       | `mekhq.campaign.personnel.enums`         | Default, User Data or Campaign, and the file path for each.                                                     |
| `RankDisplay`          | `mekhq.gui.displayWrappers`              | Wraps a rank for menus and combo boxes, filtered and named for a profession.                                    |
| `RankSystemsPane`      | `mekhq.gui.panes`                        | The Rank Systems options page and its buttons.                                                                  |
| `RankTableModel`       | `mekhq.gui.model`                        | The editable rank table.                                                                                        |
| `Person`               | `mekhq.campaign.personnel`               | A character's rank system, numeric rank, rank level, Manei Domini class and rank, and ROM designation; `getRankName()` builds the display name. |
| `Faction`              | `mekhq.campaign.universe`                | `getRankSystemCode()` and `getRankSystem()` for a faction.                                                      |
| `MHQConstants`         | `mekhq`                                  | `RANKS_FILE_PATH` and `USER_RANKS_FILE_PATH`.                                                                   |
