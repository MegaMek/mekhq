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
package mekhq.campaign.roleplay;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * The Random Event Focus table, consulted with a follow-up d100 whenever a {@link FateChart} roll triggers a random
 * event. Entries are ordered by roll range; each holds the highest roll that selects it.
 */
public enum RandomEventFocus {
    REMOTE_EVENT("REMOTE_EVENT", 5),
    AMBIGUOUS_EVENT("AMBIGUOUS_EVENT", 10),
    NEW_NPC("NEW_NPC", 20),
    NPC_ACTION("NPC_ACTION", 40),
    NPC_NEGATIVE("NPC_NEGATIVE", 45),
    NPC_POSITIVE("NPC_POSITIVE", 50),
    MOVE_TOWARD_A_THREAD("MOVE_TOWARD_A_THREAD", 55),
    MOVE_AWAY_FROM_A_THREAD("MOVE_AWAY_FROM_A_THREAD", 65),
    CLOSE_A_THREAD("CLOSE_A_THREAD", 70),
    PC_NEGATIVE("PC_NEGATIVE", 80),
    PC_POSITIVE("PC_POSITIVE", 85),
    CURRENT_CONTEXT("CURRENT_CONTEXT", 100);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final String label;
    private final int maximumRoll;

    RandomEventFocus(String lookupName, int maximumRoll) {
        this.label = getTextAt(RESOURCE_BUNDLE, "RandomEventFocus." + lookupName + ".label");
        this.maximumRoll = maximumRoll;
    }

    /**
     * @return the highest d100 roll that selects this focus
     */
    public int getMaximumRoll() {
        return maximumRoll;
    }

    /**
     * Looks up the focus for a d100 roll.
     *
     * @param roll the d100 roll (1-100); values outside the range are clamped
     *
     * @return the matching focus
     */
    public static RandomEventFocus fromRoll(final int roll) {
        for (RandomEventFocus focus : values()) {
            if (roll <= focus.maximumRoll) {
                return focus;
            }
        }
        return CURRENT_CONTEXT;
    }

    /**
     * @return the localized display label for this focus
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
