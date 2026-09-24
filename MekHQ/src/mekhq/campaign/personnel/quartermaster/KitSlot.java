package mekhq.campaign.personnel.quartermaster;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * The two equipment-kit slots every person has, separate from their armor kit. The primary slot is filled by the
 * default kit of the person's primary role, the secondary slot by that of their secondary role.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum KitSlot {
    PRIMARY, SECONDARY;

    /**
     * @param person the person whose role is wanted
     *
     * @return the role whose default kit fills this slot
     *
     * @author Illiani
     * @since 0.51.01
     */
    public PersonnelRole roleFor(Person person) {
        return (this == PRIMARY) ? person.getPrimaryRole() : person.getSecondaryRole();
    }

    /**
     * @return the other kit slot
     *
     * @author Illiani
     * @since 0.51.01
     */
    public KitSlot other() {
        return (this == PRIMARY) ? SECONDARY : PRIMARY;
    }
}
