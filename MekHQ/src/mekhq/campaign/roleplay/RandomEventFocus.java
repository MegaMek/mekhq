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
 * event. A focus may cover more than one roll range; see {@link #fromRoll(int)}.
 */
public enum RandomEventFocus {
    REMOTE_EVENT("REMOTE_EVENT"),
    AMBIGUOUS_EVENT("AMBIGUOUS_EVENT"),
    NEW_NPC("NEW_NPC"),
    NPC_ACTION("NPC_ACTION"),
    NPC_NEGATIVE("NPC_NEGATIVE"),
    NPC_POSITIVE("NPC_POSITIVE"),
    MOVE_TOWARD_A_THREAD("MOVE_TOWARD_A_THREAD"),
    MOVE_AWAY_FROM_A_THREAD("MOVE_AWAY_FROM_A_THREAD"),
    PC_NEGATIVE("PC_NEGATIVE"),
    PC_POSITIVE("PC_POSITIVE"),
    CURRENT_CONTEXT("CURRENT_CONTEXT");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    /** The highest roll of each row of the table, in roll order. Pairs with {@link #ROLL_TABLE_RESULTS}. */
    private static final int[] ROLL_TABLE_MAXIMUMS = { 5, 10, 20, 40, 45, 50, 55, 65, 70, 80, 85, 100 };

    /**
     * The focus for each row of the table, in roll order. Each row covers every roll above the previous row's highest
     * roll, up to and including its own.
     */
    private static final RandomEventFocus[] ROLL_TABLE_RESULTS = {
          REMOTE_EVENT,
          AMBIGUOUS_EVENT,
          NEW_NPC,
          NPC_ACTION,
          NPC_NEGATIVE,
          NPC_POSITIVE,
          MOVE_TOWARD_A_THREAD,
          MOVE_AWAY_FROM_A_THREAD,
          MOVE_TOWARD_A_THREAD,
          PC_NEGATIVE,
          PC_POSITIVE,
          CURRENT_CONTEXT
    };

    private final String label;
    private final String description;

    RandomEventFocus(String lookupName) {
        this.label = getTextAt(RESOURCE_BUNDLE, "RandomEventFocus." + lookupName + ".label");
        this.description = getTextAt(RESOURCE_BUNDLE, "RandomEventFocus." + lookupName + ".description");
    }

    /**
     * Looks up the focus for a d100 roll.
     *
     * @param roll the d100 roll (1-100); values outside the range are clamped
     *
     * @return the matching focus
     */
    public static RandomEventFocus fromRoll(final int roll) {
        for (int row = 0; row < ROLL_TABLE_MAXIMUMS.length; row++) {
            if (roll <= ROLL_TABLE_MAXIMUMS[row]) {
                return ROLL_TABLE_RESULTS[row];
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

    /**
     * @return the localized explanation of what this focus means for the story
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
