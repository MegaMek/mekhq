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
package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.clamp;

import megamek.logging.MMLogger;

public enum UnitReputationNegotiationsModifier {
    UNIT_REPUTATION_RATING_0(-2, -1, -1, -3),
    UNIT_REPUTATION_RATING_1(-1, -1, -1, -2),
    UNIT_REPUTATION_RATING_2(-1, 0, 0, -2),
    UNIT_REPUTATION_RATING_3(-1, 0, 0, -1),
    UNIT_REPUTATION_RATING_4(0, 0, 0, -1),
    UNIT_REPUTATION_RATING_5(0, 0, 0, 0),
    UNIT_REPUTATION_RATING_6(1, 1, 0, 0),
    UNIT_REPUTATION_RATING_7(1, 1, 0, 0),
    UNIT_REPUTATION_RATING_8(1, 1, 1, 0),
    UNIT_REPUTATION_RATING_9(2, 2, 1, 1),
    UNIT_REPUTATION_RATING_10_PLUS(3, 2, 2, 2);

    private final int commandModifier;
    private final int salvageModifier;
    private final int supportModifier;
    private final int transportModifier;

    private final static MMLogger LOGGER = MMLogger.create(UnitReputationNegotiationsModifier.class);

    UnitReputationNegotiationsModifier(final int commandModifier, final int salvageModifier, final int supportModifier,
          final int transportModifier) {
        this.commandModifier = commandModifier;
        this.salvageModifier = salvageModifier;
        this.supportModifier = supportModifier;
        this.transportModifier = transportModifier;
    }

    public int getCommandModifier() {
        return commandModifier;
    }

    public int getSalvageModifier() {
        return salvageModifier;
    }

    public int getSupportModifier() {
        return supportModifier;
    }

    public int getTransportModifier() {
        return transportModifier;
    }

    public static void getNegotiationsModifier(int reputationRating, EmployerModifierData modifierData) {
        int clampedRating = clamp(reputationRating, 0, 10);

        UnitReputationNegotiationsModifier modifierEntry = switch (clampedRating) {
            case 0 -> UNIT_REPUTATION_RATING_0;
            case 1 -> UNIT_REPUTATION_RATING_1;
            case 2 -> UNIT_REPUTATION_RATING_2;
            case 3 -> UNIT_REPUTATION_RATING_3;
            case 4 -> UNIT_REPUTATION_RATING_4;
            case 5 -> UNIT_REPUTATION_RATING_5;
            case 6 -> UNIT_REPUTATION_RATING_6;
            case 7 -> UNIT_REPUTATION_RATING_7;
            case 8 -> UNIT_REPUTATION_RATING_8;
            case 9 -> UNIT_REPUTATION_RATING_9;
            case 10 -> UNIT_REPUTATION_RATING_10_PLUS;
            default -> {
                LOGGER.error("Unexpected reputation rating: {}", clampedRating);
                yield null;
            }
        };

        if (modifierEntry != null) {
            applyNegotiationModifiers(modifierEntry, modifierData);
        }
    }

    private static void applyNegotiationModifiers(UnitReputationNegotiationsModifier modifierEntry,
          EmployerModifierData modifierData) {
        modifierData.modifyCommandModifier(modifierEntry.getCommandModifier());
        modifierData.modifySalvageModifier(modifierEntry.getSalvageModifier());
        modifierData.modifySupportModifier(modifierEntry.getSupportModifier());
        modifierData.modifyTransportModifier(modifierEntry.getTransportModifier());
    }
}
