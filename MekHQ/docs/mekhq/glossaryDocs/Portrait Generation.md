# Random Portraits in MekHQ

## When Does MekHQ Assign a Random Portrait?

MekHQ hands out a random portrait in these situations:

| Situation                                      | Extra conditions                                                                                       |
|------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| A new character is created                     | Hiring, recruiting, and so on                                                                           |
| The Force Generator crews a unit               | Ignores the role checkboxes and the child option; *Random Portraits for Officers Only* still applies   |
| A character's primary role is changed          | Only with *Reassign Portrait on Role Change* on, and only if they still have the default portrait       |
| A character is promoted to an officer rank     | Only with *Random Portraits for Officers Only* on, and only if they still have the default portrait     |
| A child comes of age (their 16th birthday)     | Only with *Generate Random Portraits for Children Coming of Age* on, and only if they still have the default portrait |
| You ask for one by hand                        | Always (see below)                                                                                     |

The automatic cases check the campaign options below first. Asking by hand (**Randomize Portrait** on the personnel
right-click menu, or the random portrait button in the new recruit dialog) skips the role, child and officer checks:
it always picks a new portrait, and only *Allow Duplicate Portraits* and *Portraits Ignore Roles* affect which one.

If no eligible picture is found anywhere, the character keeps the portrait they had.

### Campaign Options

The options live under *Campaign Options → Name and Portrait Generation Options*.

| Option                                              | Default | Effect                                                                                                                                                  |
|-----------------------------------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Portrait Assignment (one checkbox per primary role) | MekWarrior only | A character whose **primary** role is unticked is never given a random portrait automatically. This is usually the first thing to change. |
| Reassign Portrait on Role Change                    | Off     | Gives a portrait to a character without one when their primary role changes.                                                                           |
| Allow Duplicate Portraits                           | On      | With this off, any portrait already used by someone in the campaign is removed from the pool before picking.                                            |
| Portraits Ignore Roles                              | Off     | Skips the role and role group folders and goes straight to Combat/Support, then the gender folder.                                                     |
| No Random Portraits for Children                    | On      | Children are not given a portrait when they are created.                                                                                                |
| Random Portraits for Officers Only                  | Off     | Only characters holding an officer rank are given a portrait automatically. Characters without one get it when promoted to an officer rank.             |
| Generate Random Portraits for Children Coming of Age| On      | Gives a portrait to a character on their 16th birthday, if they still have the default one. This works whether or not *No Random Portraits for Children* is on. |

The portrait assignment list has a single **Civilian** checkbox that covers every civilian role, and
**Enable All** / **Disable All** buttons.

## How a Portrait Is Picked

MekHQ works out the character's gender folder, then tries these folders in order:

1. `/<Gender>/<Primary Role>`: for example `/Male/MekWarrior` or `/Female/Vehicle Crew/Ground`
2. `/<Gender>/<Role Group>`: for example `/Male/Admin` or `/Female/Tech` (only some roles have a group; see below)
3. `/<Gender>/Combat` or `/<Gender>/Support`
4. `/<Gender>`: every portrait for that gender

The first folder that has at least one eligible picture wins, and a random picture is chosen from it. The search
then stops. So if you have pictures in both `/Male/MekWarrior` and `/Male/Combat`, male MekWarriors will only get a
Combat picture once every MekWarrior picture is taken (and only if duplicates are off).

**Subfolders count.** Each step looks in that folder *and* everything nested inside it. This means:

- You can organise a folder into subfolders however you like: `/Male/Admin` also covers `/Male/Admin/Veterans`.
- The last step covers every portrait filed anywhere under that gender, so in practice a character nearly always ends
  up with something rather than the default silhouette.

Pictures outside the two gender folders are never picked at random. That includes `default.gif` in the top-level
portraits folder and the named unit folders that ship with MekHQ (such as `Black Widow Company`). They can still be
chosen by hand.

## Where the Portraits Live

These locations are read and merged:

- `<MekHQ install directory>/data/images/portraits`
- `<your MegaMek user directory>/data/images/portraits`
- `data/images/portraits` inside each story arc folder

The user directory is set in MegaMek's client settings, and its contents survive upgrading MekHQ, so it is the best
place for your own portraits. All paths below are relative to one of these portraits folders.

Examples:

| To give a portrait to...                                              | Put it in            |
|-----------------------------------------------------------------------|----------------------|
| Male MekWarriors only                                                 | `/Male/MekWarrior`   |
| Any female administrator                                              | `/Female/Administrator` |
| Any female technician (MekTech, Mechanic, AeroTek or Battle Armor Tech)| `/Female/Tech`       |

## Gender Folders

Every folder below must sit inside one of these, or its portraits will never be picked at random.

| Characters      | Folder    |
|-----------------|-----------|
| Female          | `/Female` |
| Everyone else   | `/Male`   |

## Primary Role Folders

The folder name is the role's label exactly as it appears in the personnel table. Where a label contains a `/`, it
is a real nested folder: Vehicle Crew/Ground reads from `/<Gender>/Vehicle Crew/Ground`.

| Role                       | Folder                        |
|----------------------------|-------------------------------|
| MekWarrior                 | `/MekWarrior`                 |
| LAM Pilot                  | `/LAM Pilot`                  |
| Vehicle Crew/Ground        | `/Vehicle Crew/Ground`        |
| Vehicle Crew/Naval         | `/Vehicle Crew/Naval`         |
| Vehicle Crew/VTOL          | `/Vehicle Crew/VTOL`          |
| Aerospace Pilot            | `/Aerospace Pilot`            |
| Conventional Aircraft Crew | `/Conventional Aircraft Crew` |
| ProtoMek Pilot             | `/ProtoMek Pilot`             |
| Battle Armor Pilot         | `/Battle Armor Pilot`         |
| Battle Armor Pilot (Clan)  | `/Elemental`                  |
| Soldier                    | `/Soldier`                    |
| Vessel Pilot               | `/Vessel Pilot`               |
| Vessel Gunner              | `/Vessel Gunner`              |
| Vessel Crewmember          | `/Vessel Crewmember`          |
| Hyperspace Navigator       | `/Hyperspace Navigator`       |
| MekTech                    | `/MekTech`                    |
| Mechanic                   | `/Mechanic`                   |
| AeroTek                    | `/AeroTek`                    |
| Battle Armor Tech          | `/Battle Armor Tech`          |
| Astech                     | `/Astech`                     |
| Doctor                     | `/Doctor`                     |
| Medic                      | `/Medic`                      |
| Administrator              | `/Administrator`              |
| Every civilian role        | `/Civilian`                   |

Battle Armor Pilot is the only role with a separate Clan name. Characters flagged as Clan personnel read from
`/Elemental`; everyone else reads from `/Battle Armor Pilot`.

All civilian roles share the single `/Civilian` folder. That covers Dependent, None and the whole list of civilian
professions (Chef, Broker, Noble, and the rest). There is no folder for an individual civilian profession, and no
separate `/Dependent` or `/None` folder. MekHQ does not ship a `/Civilian` folder, so out of the box civilians fall
through to `/Support` and then to the gender folder.

## Role Group Folders

These are the fallback when the role's own folder is missing or used up. Only the roles listed here have one.

| Group               | Folder          | Roles                                                                 |
|---------------------|-----------------|-----------------------------------------------------------------------|
| Administrators      | `/Admin`        | Administrator                                                         |
| Vehicle Crews       | `/Vehicle Crew` | Vehicle Crew/Ground, Vehicle Crew/Naval, Vehicle Crew/VTOL            |
| Vessel Crews        | `/Vessel Crew`  | Vessel Pilot, Vessel Gunner, Vessel Crewmember, Hyperspace Navigator  |
| Techs               | `/Tech`         | MekTech, Mechanic, AeroTek, Battle Armor Tech                         |
| Medical Staff       | `/Medical`      | Doctor, Medic                                                         |

Astechs have no group folder. They go from `/Astech` straight to `/Support`.

Because subfolders count, `/<Gender>/Vehicle Crew` also contains the three Vehicle Crew role folders. Once a vehicle
crew member's own folder is used up, they can be given a picture from either of the other two vehicle roles, or from
`/Vehicle Crew/Generic`.

This step is kept so that collections sorted by role family keep working. Apart from that Vehicle Crew overlap, the
stock portraits are all filed under their own role folder, so nothing shipped depends on it. The group folder you are
most likely to have is `/<Gender>/Admin`, which is where administrator portraits shipped before 0.51.01; it still
works and still feeds every administrator.

## General Grouping Folders

| Characters        | Folder     |
|-------------------|------------|
| Combat personnel  | `/Combat`  |
| Everyone else     | `/Support` |

Combat covers every combat role: MekWarrior, LAM Pilot, the three Vehicle Crew roles, Aerospace Pilot, Conventional
Aircraft Crew, ProtoMek Pilot, Battle Armor Pilot, Soldier, Vessel Pilot, Vessel Gunner, Vessel Crewmember and
Hyperspace Navigator. Everything else, support staff and civilians alike, uses Support.

## Upgrade Notes

### 0.51.01: Administrators Merged

The four administrator roles were merged into a single Administrator role. Admin/Command, Admin/Logistical,
Admin/Transport and Admin/HR can no longer be assigned, and all administrators read from `/<Gender>/Administrator`,
falling back to `/<Gender>/Admin`.

The stock portraits moved to match. The four `/<Gender>/Admin/{Command,HR,Logistical,Transport}` folders are gone,
and their portraits are now in `/<Gender>/Administrator`.

Existing campaigns are migrated for you:

- Characters keep the portrait they already had. When a campaign is loaded, any portrait pointing into one of the
  four old folders is repointed at `/<Gender>/Administrator`, and the new path is saved the next time the campaign is
  saved. The picture itself is unchanged.
- Characters with one of the old administrator roles are converted to Administrator when the save is opened.

Your own portraits are not touched. If you built your own `/<Gender>/Admin` folder, it keeps its name and contents
and keeps feeding administrators through the role group step. Only the four folder names that shipped with MekHQ are
repointed, matched by whole folder name (so `Admin/HR Reserves` is left alone).

### 0.50.10: Vehicle Crew Rework

The vehicle crew update replaced the old vehicle roles and their portrait folders.

| Old folder            | New folder                                                                                  |
|-----------------------|---------------------------------------------------------------------------------------------|
| `/Vehicle Driver`     | `/Vehicle Crew/Ground`                                                                      |
| `/Naval Driver`       | `/Vehicle Crew/Naval`                                                                       |
| `/VTOL Pilot`         | `/Vehicle Crew/VTOL`                                                                        |
| `/Vehicle Gunner`     | `/Vehicle Crew/Ground` (the Vehicle Gunner role was removed)                                |
| `/Vehicle Crewmember` | `/Vehicle Crew/Generic` (no role reads this directly; it is reached through the `/Vehicle Crew` group step) |

If you have your own portraits in the old folders, rename the folders to match. Anything left under an old name is
not lost, since it still sits under `/<Gender>` and can be picked by the final step, but it is no longer tied to the
role you filed it under.

## For Developers

| Class                         | Package                                  | Purpose                                                                                       |
|-------------------------------|------------------------------------------|-----------------------------------------------------------------------------------------------|
| `RandomPortraitGenerator`     | `mekhq.campaign.personnel.generator`     | The folder search described above.                                                            |
| `ForceHumanResources`         | `mekhq.campaign`                         | `assignRandomPortraitFor()`, the officer check, and the new-person and promotion triggers.    |
| `CampaignNewDayManager`       | `mekhq.campaign`                         | The coming-of-age trigger.                                                                    |
| `PersonnelTableMouseAdapter`  | `mekhq.gui.adapter`                      | The role-change trigger and the manual *Randomize Portrait* command.                          |
| `CampaignOption`              | `mekhq.campaign.campaignOptions`         | The option keys and defaults (`USE_PORTRAIT_FOR_ROLE`, `ALLOW_DUPLICATE_PORTRAITS`, and so on).|
| `NameAndPortraitGenerationPage` | `mekhq.gui.campaignOptions.contents`   | The options page, including the single Civilian checkbox.                                     |
| `MMStaticDirectoryManager`    | `megamek.client.ui.tileset` (MegaMek)    | Loads and merges the portrait folders.                                                        |
| `AbstractIcon`                | `megamek.common.icons` (MegaMek)         | Repoints the old administrator folders on load (`migrateAdministratorCategories`).            |
