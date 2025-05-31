# Custom SPAs

MekHQ supports unofficial extensions to the SPA (special pilot abilities) system used in MegaMek. This allows support
personnel to spend XP to gain special abilities used in their duties (though in this context SPA should probably mean
special personnel abilities) as well as allowing them to set triggers to spend edge points to reroll critical failed
skill checks in critical situations. At the time of writing, the only custom SPA is Clan tech knowledge, which allows
Inner Sphere techs to work on Clan equipment without penalty. There are also edge triggers for part breaking parts
during repair for techs, failed healing checks for doctors, and failed acquisition rolls for administrators (or
whichever role acquires parts in the campaign).

There is also a means for players to add their own custom SPAs, edge triggers, and implants. At the time of writing
there are no actual in-game effects of custom SPAs, and they are provided for book-keeping purposes. MekHQ can manage
the bookkeeping of spending XP and tracking which personnel have acquired the special ability, but it is up the player
to manage the effects. There are plans to add plugin module support to MekHQ at some point in the future, and plugins
can be designed to apply the effects of the SPA. Note that without a plugin system edge triggers have no practical
purpose and were included with the eventual availability of plugins in mind.

To add an SPA, edge trigger, or implant, an entry must be made in the `data/universe/customspa.xml` file. The
distributed file has a comment section showing one example of each type. The minimum required for the entry to work is a
unique value for the name attribute. XML entities are provided for all legal values of the group and type nodes. The
group should be set to `&L3;` for spas, `&EDGE;` for edge triggers, or `&AUGMENTATION;` for implants. If the group is
not set it defaults to `&L3;`. In most cases the type should be set to `&BOOLEAN:`, which is the default and indicates
an spa that is either set or not. The second most common is `&CHOICE;`, which is for an SPA that can be set to one of a
list of available values. The actual values will be provided in the next step.

Adding to `customspa.xml` will add the option to the hard-coded options, but just as with the hard-coded options it
still needs an entry in `defaultspa.xml` to provide XP costs, display name, and description used by MekHQ. This is done
as for any other SPA, or HQ-specific edge triggers and implants. The one addition is that custom SPAs of type CHOICE
should list all the possible settings in a `<choiceValues>` node using two colons as a separator between values. The
`defaultspa.xml` has a comment section showing the entries corresponding to the examples in `customspa.xml` at the end.
