# Formation Icons in MekHQ

*Originally written by Windchild, 07-Nov-2021. Rewritten for MekHQ 0.51.01, 23-Nov-2026.*

Force icons were renamed **formation icons** in 0.50.12. The in-game menus, the options and the code all use the
new name; the folder on disk is still `data/images/force`.

There are three kinds of icon:

- **Standard formation icon**: a single picture, picked from anywhere in the formation icon folder.
- **Layered formation icon**: an icon built by stacking pieces from the `Pieces` folder. New formations start with one
  of these, holding only the default frame.
- **Unit icon**: the icon for your whole command, set in *Campaign Options → General*. It is unrelated to the
  icons on individual formations.

## Where the Icons Live

Two folders are read and merged:

- `<MekHQ install directory>/data/images/force`
- `<your MegaMek user directory>/data/images/force`

The user directory is set in MegaMek's client settings, and its contents survive upgrading MekHQ, so put your own
icons there. Mirror the layout below inside it.

| Folder              | Contents                                                                                                                   |
|---------------------|----------------------------------------------------------------------------------------------------------------------------|
| `force/`            | The origin folder. It must contain `empty.png`, which is the fallback whenever an icon cannot be found.                    |
| `force/Pieces/`     | The pieces used to build layered icons. Only the eight layer folders listed below are read for layered icons.              |
| `force/Units/`      | Unit icons. The unit icon chooser opens here when no unit icon is set.                                                     |
| Any other folder    | Standard formation icons. Nothing else ships in the stock install, so this is where your own single-image icons go.        |

The standard icon chooser shows the whole tree, `Pieces` and `Units` included, so any image anywhere under `force/`
can be used as a standard formation icon or as the unit icon.

### Layer Folders

Each folder under `Pieces/` is one layer of a layered icon, and one tab in the layered icon editor. Any subfolders
inside a layer folder are read as well; see `Pieces/Types/` for an example of how the stock pieces are organised.

| Tab           | Folder                        | Pieces per icon | Stock contents                                                                              |
|---------------|-------------------------------|-----------------|---------------------------------------------------------------------------------------------|
| Types         | `Pieces/Types/`               | Several         | Unit type symbols: `StratOps`, `NATO`, `Graphical`, `Graphical (Primitive)`, `Alphanumeric`, `Formation` |
| Formations    | `Pieces/Formations/`          | One             | Echelon symbols for `Inner Sphere`, `Clan` and `ComStar`                                    |
| Adjustments   | `Pieces/Adjustments/`         | Several         | Modifiers to the type symbol (`StratOps`, `NATO`)                                           |
| Alphanumerics | `Pieces/Alphanumerics/`       | Several         | Letters and numbers, filed by position (`Top Left`, `Bottom Right`, and so on)              |
| Special       | `Pieces/Special Modifiers/`   | One             | The operational status indicators, in `Border` and `Tab` styles                             |
| Backgrounds   | `Pieces/Backgrounds/`         | One             | Faction backgrounds                                                                         |
| Frames        | `Pieces/Frames/`              | One             | The outer frame. `Frame.png` is the default frame every new formation starts with.          |
| Logos         | `Pieces/Logos/`               | One             | Faction logos for the centre of the icon                                                    |

The Logos tab has an extra `Factions` category. It is not a folder under `Pieces/Logos/`: MekHQ fills it from the
faction logos in `data/images/universe/factions/` (install and user directory), so every faction logo the rest of
MekHQ uses is also available as a formation icon piece.

### How Layers Are Drawn

The pieces are drawn in this order, so later layers sit on top of earlier ones:

1. Backgrounds
2. Frames
3. Types
4. Formations
5. Adjustments
6. Alphanumerics
7. Special
8. Logos

The finished icon is as wide as the widest piece and as tall as the tallest piece. Each piece is aligned to the
bottom-right corner, so pieces of different sizes line up on that corner rather than being centred. If none of the
selected pieces can be found, the icon falls back to `empty.png`.

## Setting a Formation's Icon

Right-click a formation in the TO&E and open the **Formation Icon** menu:

- **Change Formation Icon...** opens the icon dialog (one formation only). It has two tabs: *Standard* picks a single
  image, and *Layered* builds an icon from pieces. Whichever tab is showing when you press OK is the icon you get.
  The dialog opens on the Layered tab if the formation already has a layered icon.
- **Copy Formation Icon** remembers this formation's icon (one formation only).
- **Paste Formation Icon** applies the copied icon to the selected formations.
- **Paste Formation Icon to Force and Sub forces** applies it to the selected formations and every formation under
  them.
- **Auto-Generate Formation Icons** draws an icon for every formation here and below that still has the blank
  default icon, and updates the icons MekHQ drew itself to match their current size and units. Icons you designed are
  left alone.
- **Regenerate All Formation Icons...** replaces *every* icon here and below with a freshly generated one, including
  icons you designed. It asks for confirmation first.

Both *Paste* items only appear once you have copied an icon. The copied icon is kept only until MekHQ closes.

### Using the Layered Tab

Each layer has its own tab. Types, Adjustments and Alphanumerics accept several pieces (Ctrl- or Shift-click); the
other layers take one. **Clear Current Tab** removes the pieces chosen on the tab you are looking at.

The selection on a tab is lost when you change folder, type in the search box, or toggle *Include Subdirectories*.
Pick the folder first, then select all the pieces you want from it.

The **Refresh Directory** button rescans the icon folders, so icons added while MekHQ is running show up without a
restart.

## Automatically Generated Icons

MekHQ can draw layered icons for you, the same way the Force Generator does when it builds a new command. A generated
icon has:

- a **Type** piece from `Pieces/Types/StratOps/`, chosen from what the formation is for or what it contains:
  - support formations show their job: Medical, Maintenance, Supply, or Headquarters (Staff) for the command HQ
  - otherwise the most common unit type in the formation and everything under it. 'Mek and aerospace formations
    get a silhouette plus a weight class letter worked out from the average weight of their units; ground and naval
    vehicles get only the weight class letter; other types get their own symbol. Empty or unrecognised formations
    get the generic `BattleMek (Center).png`.
- a **Formation** piece matching the formation's echelon (Lance, Company, Star, Level II, and so on), from the
  `Inner Sphere`, `Clan` or `ComStar` set to suit your faction
- a **Background**, if your faction defines one

The top-level formation of a newly generated command can instead get your faction's logo, if that Force Generator
option is on.

MekHQ only updates icons it believes it drew: the Formation layer must be a single echelon symbol from the expected
set, and every Type piece must be one the generator uses. Anything else counts as yours and is never touched by
*Auto-Generate* or by the new-day update. Only *Regenerate All Formation Icons...* overwrites your own icons.

## New-Day Options

These live in *File → MekHQ Client Options... → New Day*. They are client settings, not campaign options, so they
apply to every campaign you open on that computer.

| Option                               | Default | Effect                                                                                                                         |
|--------------------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------|
| Automatic Formation Icons            | Off     | Each new day, does what *Auto-Generate Formation Icons* does for the whole TO&E.                                               |
| Formation Icon Operation Status Update | On    | Each new day, puts an operational status indicator on every layered formation icon. This replaces anything on the Special layer. |
| Operation Status Indicator Style     | Border  | *Border* draws a coloured border around the frame; *Tab* draws a coloured tab at the top right.                               |

Automatic icons are drawn first, so the status indicator ends up on top of them. Standard (single image) icons never
get a status indicator.

### How Operational Status Is Worked Out

Each unit in the formation and in every formation below it gets a status:

| Status                   | Colour | When                                                                                    |
|--------------------------|--------|-----------------------------------------------------------------------------------------|
| Fully Operational        | Green  | Undamaged                                                                               |
| Substantially Operational| Yellow | Light or moderate damage                                                                |
| Marginally Operational   | Red    | Heavy damage or crippled                                                                |
| Not Operational          | Gray   | Mothballed or mothballing, refitting, not present, unrepairable, or not functional      |
| Factory Fresh            | Blue   | Never assigned automatically; available to pick by hand                                 |

The formation's status is the average of its units' statuses, rounded to the nearest. A formation with no units in or
below it has its indicator removed.

The indicator files are `(01) Green - Fully Operational.png` through `(05) Blue - Factory Fresh.png`, inside
`Pieces/Special Modifiers/Operational Indicators (Border)/` and `.../Operational Indicators (Tab)/`. If you replace
them, keep the filenames.

## Hard-Coded File Names

The generator and the status update look for these by name. Renaming or removing them breaks those features, though
icons you build by hand are unaffected.

- `force/empty.png`
- `Pieces/Frames/Frame.png`
- `Pieces/Special Modifiers/Operational Indicators (Border)/` and `(Tab)/`, with the five status files above
- `Pieces/Types/StratOps/`: `BattleMek (Left).png`, `BattleMek (Center).png`, `Aerospace (Left).png`, `ProtoMek.png`,
  `Battle Armor.png`, `Infantry (Xenoplanetary).png`, `DropShip.png`, `JumpShip.png`, `WarShip.png`,
  `Space Station.png`, `Airship.png`, `Headquarters.png`, `Headquarters (Staff).png`, `Medical.png`,
  `Maintenance.png`, `Supply.png`, and the weight class letters `Light.png`, `Medium.png`, `Heavy.png`,
  `Assault.png` and `Superheavy.png` (ultra-light units use `Light.png`; small, medium and large support vehicles use
  `Light.png`, `Medium.png` and `Heavy.png`)
- `Pieces/Formations/Inner Sphere/`, `Clan/` and `ComStar/`, with the echelon files named in `MHQConstants`
  (for example `(04) Lance.png`, `(02) Star.png`, `(02) Level II.png`)

## For Developers

| Class                                     | Package / location                                  | Purpose                                                                                                                   |
|-------------------------------------------|-----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `StandardFormationIcon`                   | `mekhq.campaign.icons`                              | A single-image icon. Falls back to `empty.png`. Saved as `<standardFormationIcon>`. Was `StandardForceIcon`.              |
| `LayeredFormationIcon`                    | `mekhq.campaign.icons`                              | A map of layer → pieces, merged in draw order. Saved as `<layeredForceIcon>`. Was `LayeredForceIcon`.                     |
| `FormationPieceIcon`                      | `mekhq.campaign.icons`                              | One piece of a layered icon; its category is relative to its layer folder. Saved as `<forcePieceIcon>`. Was `ForcePieceIcon`. |
| `UnitIcon`                                | `mekhq.campaign.icons`                              | The command's icon. Allows a null filename, meaning "no icon", in which case the faction logo is shown instead.            |
| `LayeredFormationIconLayer`               | `mekhq.campaign.icons.enums`                        | The eight layers: folder, tab name, selection mode, and draw order (`getInDrawOrder()`).                                  |
| `OperationalStatus`                       | `mekhq.campaign.icons.enums`                        | The five statuses, their filenames, and how a unit's status is worked out.                                                |
| `FormationIconOperationalStatusStyle`     | `mekhq.gui.enums`                                   | Border or Tab, and the folder for each.                                                                                   |
| `FormationIconBuilder`                    | `mekhq.campaign.universe.commandGeneration.ratgen`  | Generates, refreshes and regenerates layered icons.                                                                       |
| `Formation`                               | `mekhq.campaign.force`                              | Holds the icon; `updateFormationIconOperationalStatus()` sets the status indicator.                                       |
| `CampaignNewDayManager`                   | `mekhq.campaign`                                    | Runs the automatic icon and status updates each day (`processNewDayFormations()`).                                        |
| `MHQStaticDirectoryManager`               | `mekhq`                                             | Loads and merges the icon folders and adds the faction logos.                                                             |
| `LayeredFormationIconDialog`              | `mekhq.gui.dialog.iconDialogs`                      | The two-tab icon dialog used from the TO&E.                                                                               |
| `LayeredFormationIconCreationPanel`       | `mekhq.gui.panels`                                  | The Layered tab: one `ForcePieceIconChooser` per layer, plus a preview.                                                   |
| `StandardFormationIconChooser` / `...Tree`| `mekhq.gui.panels` / `mekhq.gui.trees`              | The Standard tab.                                                                                                         |
| `ForcePieceIconChooser` / `...Tree`       | `mekhq.gui.panels` / `mekhq.gui.trees`              | One layer's piece chooser.                                                                                                |
| `UnitIconDialog` / `UnitIconChooser`      | `mekhq.gui.dialog.iconDialogs` / `mekhq.gui.panels` | The unit icon chooser, with a *None* button.                                                                              |
| `TOEMouseAdapter`                         | `mekhq.gui.adapter`                                 | The TO&E right-click menu. The copied icon is held by `CampaignGUI`.                                                      |
| `MHQOptions`                              | `mekhq`                                             | The three new-day settings above.                                                                                         |
| `MHQConstants`                            | `mekhq`                                             | Every folder and file name above (`LAYERED_FORCE_ICON_*`).                                                                |

The XML tags `layeredForceIcon` and `forcePieceIcon` kept their old names so existing saves still load.
