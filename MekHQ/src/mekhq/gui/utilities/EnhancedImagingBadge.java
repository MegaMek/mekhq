/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.utilities;

import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * A short coloured tag marking a unit whose crew carry the enhanced imaging implant, for the force
 * tree.
 *
 * <p>Which warriors are implanted is otherwise only visible by opening each one in turn, and because
 * EI warriors are generated in whole formations rather than one to a star, what a reader wants to see
 * is which formations are EI units - a question the badge answers by looking down the tree.</p>
 *
 * <p>The implant alone does nothing. It works through an EI interface fitted to the unit, which
 * ProtoMeks carry built in and other units carry as equipment, so a warrior implanted in a machine
 * without one gains no benefit from it. The two cases are told apart rather than run together: an
 * implant that is working is coloured, and one that is inert is greyed and says so. The colour never
 * carries the meaning by itself - the inert case is labelled in text.</p>
 */
public final class EnhancedImagingBadge {

    /**
     * Reddish purple, from Okabe and Ito's palette, which is chosen so its colours stay distinct for
     * the common forms of colour blindness. It is not one of the network colours, so an EI badge is not
     * mistaken for a C3 network at a glance.
     */
    private static final String ACTIVE_COLOUR = "#CC79A7";

    /** Implanted, but in a unit with no interface for the implant to work through. */
    private static final String INERT_COLOUR = "#808080";

    private EnhancedImagingBadge() {
    }

    /**
     * The badge for one unit.
     *
     * @param unit the unit to describe, or {@code null}
     *
     * @return an HTML tag such as a coloured {@code [EI]}, or an empty string when no one aboard is
     *       implanted
     */
    public static String forUnit(@Nullable Unit unit) {
        if ((unit == null) || (unit.getEntity() == null)) {
            return "";
        }
        int crewSize = 0;
        int implanted = 0;
        for (Person crewMember : unit.getActiveCrew()) {
            crewSize++;
            if (hasImplant(crewMember)) {
                implanted++;
            }
        }
        if (implanted == 0) {
            return "";
        }

        // A squad only partly implanted is worth saying so; the whole-crew case is the ordinary one and
        // reads better without a count cluttering it.
        String label = (implanted < crewSize) ? "EI %d/%d".formatted(implanted, crewSize) : "EI";
        if (hasInterface(unit.getEntity())) {
            return "<font color='%s'><b>[%s]</b></font> ".formatted(ACTIVE_COLOUR, label);
        }
        return "<font color='%s'><b>[%s, no interface]</b></font> ".formatted(INERT_COLOUR, label);
    }

    /**
     * @return {@code true} if this crew member carries the implant
     */
    private static boolean hasImplant(@Nullable Person crewMember) {
        return (crewMember != null)
              && crewMember.getOptions().booleanOption(OptionsConstants.MD_EI_IMPLANT);
    }

    /**
     * @return {@code true} if the unit can put the implant to use, which needs an EI interface fitted -
     *       built into every ProtoMek, equipment on anything else
     */
    private static boolean hasInterface(Entity entity) {
        return entity.hasEiCockpit();
    }
}
