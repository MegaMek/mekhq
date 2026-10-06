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
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
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
 * <p>Whether an implant does anything is a game option rather than a property of the warrior, so the
 * badge reads {@link NeuralInterfaceMode} from the campaign's options and says what the campaign will
 * actually see. With the rules off the implant is inert whatever the machine carries; under Pilot
 * Abilities Only the implant alone is enough; under Full Tracking the machine must carry an EI
 * interface too - built into every ProtoMek, equipment on anything else.</p>
 *
 * <p>A working implant is coloured and an inert one is greyed and says why. The colour never carries
 * the meaning by itself - every inert case is labelled in text.</p>
 */
public final class EnhancedImagingBadge {

    /**
     * Reddish purple, from Okabe and Ito's palette, which is chosen so its colours stay distinct for
     * the common forms of colour blindness. It is not one of the network colours, so an EI badge is not
     * mistaken for a C3 network at a glance.
     */
    private static final String ACTIVE_COLOUR = "#CC79A7";

    /** Implanted, but under rules or in a machine that give the implant nothing to do. */
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
        NeuralInterfaceMode mode = neuralInterfaceModeOf(unit);
        if (!mode.isOn()) {
            return greyed(label + ", rules off");
        }
        if (mode.requiresInterfaceHardware() && !unit.getEntity().hasEiCockpit()) {
            return greyed(label + ", no interface");
        }
        return "<font color='%s'><b>[%s]</b></font> ".formatted(ACTIVE_COLOUR, label);
    }

    private static String greyed(String label) {
        return "<font color='%s'><b>[%s]</b></font> ".formatted(INERT_COLOUR, label);
    }

    /**
     * @return {@code true} if this crew member carries the implant
     */
    private static boolean hasImplant(@Nullable Person crewMember) {
        return (crewMember != null)
              && crewMember.getOptions().booleanOption(OptionsConstants.MD_EI_IMPLANT);
    }

    /**
     * @return the neural interface rules the unit's campaign is playing under, or
     *       {@link NeuralInterfaceMode#OFF} for a unit with no campaign behind it
     */
    private static NeuralInterfaceMode neuralInterfaceModeOf(Unit unit) {
        Campaign campaign = unit.getCampaign();
        return (campaign == null) ? NeuralInterfaceMode.OFF
                     : NeuralInterfaceMode.from(campaign.getGameOptions());
    }
}
