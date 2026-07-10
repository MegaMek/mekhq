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
import static megamek.common.compute.Compute.d6;

import mekhq.campaign.mission.enums.ContractCommandRights;

/**
 * The CamOps contract-terms roll tables (pg 41, rev 5th printing). Each method rolls 2d6, applies the opposed
 * negotiation result, clamps the total to the table's range, and reads off the corresponding term. This is a static
 * utility class and is not instantiable.
 */
public class NegotiationTermsTables {
    /** Sentinel salvage value indicating a salvage-exchange arrangement rather than a percentage share. */
    static double SALVAGE_RIGHTS_EXCHANGE_MARKER = -1.0;

    private NegotiationTermsTables() {}

    /**
     * Rolls on the command-rights table.
     *
     * @param opposedNegotiationModifier the result of the opposed negotiation skill check, applied to the roll
     * @param commandModifier            the employer's command-rights modifier
     *
     * @return the resulting command-rights arrangement
     */
    public static ContractCommandRights rollOnCommandRightsTable(int opposedNegotiationModifier, int commandModifier) {
        int roll = d6(2) + opposedNegotiationModifier + commandModifier;
        roll = clamp(roll, 1, 13);

        return switch (roll) {
            case 1, 2 -> ContractCommandRights.INTEGRATED;
            case 3, 4, 5, 6, 7 -> ContractCommandRights.HOUSE;
            case 8, 9, 10, 11 -> ContractCommandRights.LIAISON;
            case 12, 13 -> ContractCommandRights.INDEPENDENT;
            default -> throw new IllegalStateException("Unexpected value in rollOnCommandRightsTable: " + roll);
        };
    }

    /**
     * Rolls on the salvage-rights table.
     *
     * @param opposedNegotiationModifier the result of the opposed negotiation skill check, applied to the roll
     * @param salvageModifier            the employer's salvage-rights modifier
     *
     * @return the salvage share as a fraction, or {@link #SALVAGE_RIGHTS_EXCHANGE_MARKER} for a salvage-exchange result
     */
    public static double rollOnSalvageRightsTable(int opposedNegotiationModifier, int salvageModifier) {
        int roll = d6(2) + opposedNegotiationModifier + salvageModifier;
        roll = clamp(roll, 1, 13);

        return switch (roll) {
            case 1 -> 0.0;
            case 2, 3 -> SALVAGE_RIGHTS_EXCHANGE_MARKER;
            case 4 -> 0.1;
            case 5 -> 0.2;
            case 6 -> 0.3;
            case 7 -> 0.4;
            case 8 -> 0.5;
            case 9 -> 0.6;
            case 10 -> 0.7;
            case 11 -> 0.8;
            case 12 -> 0.9;
            case 13 -> 1.0;
            default -> throw new IllegalStateException("Unexpected value in rollOnSalvageRightsTable: " + roll);
        };
    }

    /**
     * Rolls on the support-rights table.
     *
     * @param opposedNegotiationModifier the result of the opposed negotiation skill check, applied to the roll
     * @param supportModifier            the employer's support-rights modifier
     *
     * @return the support share as a fraction; a negative value denotes battle-loss compensation terms
     */
    public static double rollOnSupportRightsTable(int opposedNegotiationModifier, int supportModifier) {
        int roll = d6(2) + opposedNegotiationModifier + supportModifier;
        roll = clamp(roll, 1, 13);

        return switch (roll) {
            case 1, 2 -> 0.0;
            case 3 -> 0.2;
            case 4 -> 0.4;
            case 5 -> 0.6;
            case 6 -> 0.8;
            case 7 -> 1.0;
            case 8 -> -0.1;
            case 9 -> -0.2;
            case 10 -> -0.4;
            case 11 -> -0.6;
            case 12 -> -0.8;
            case 13 -> -1.0;
            default -> throw new IllegalStateException("Unexpected value in rollOnSupportRightsTable: " + roll);
        };
    }

    /**
     * Rolls on the transport-rights table.
     *
     * @param opposedNegotiationModifier the result of the opposed negotiation skill check, applied to the roll
     * @param transportModifier          the employer's transport-rights modifier
     *
     * @return the transport share as a fraction
     */
    public static double rollOnTransportRightsTable(int opposedNegotiationModifier, int transportModifier) {
        int roll = d6(2) + opposedNegotiationModifier + transportModifier;
        roll = clamp(roll, 1, 13);

        return switch (roll) {
            case 1 -> 0.0;
            case 2 -> 0.2;
            case 3 -> 0.25;
            case 4 -> 0.3;
            case 5 -> 0.35;
            case 6 -> 0.4;
            case 7 -> 0.45;
            case 8 -> 0.5;
            case 9 -> 0.55;
            case 10 -> 0.6;
            case 11, 12, 13 -> 1.0;
            default -> throw new IllegalStateException("Unexpected value in rollOnTransportRightsTable: " + roll);
        };
    }
}
