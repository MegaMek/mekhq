/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.market.contractMarket;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import mekhq.campaign.mission.enums.AtBContractType;

/**
 * Utility class that implements the mission tables as described in CamOps (4th printing).
 */
public class MissionSelector {
    public static AtBContractType getInnerSphereClanMission(int roll, int margin, boolean isClan) {
        if (isClan) {
            int result = roll + margin;
            if (result < 4 || result > 11) {
                // Reroll on the IS/Clan column if the force is clan and the result is Covert or Special
                return getInnerSphereClanMission(Compute.d6(2), margin, true);
            }
        }
        return switch(MathUtility.clamp(roll + margin, 2, 12)) {
            case 2 -> getCovertMission(Compute.d6(2), margin);
            case 3, 12 -> getSpecialMission(Compute.d6(2), margin);
            case 4 -> AtBContractType.PIRATE_HUNTING;
            case 5 -> AtBContractType.PLANETARY_ASSAULT;
            case 6, 7 -> AtBContractType.OBJECTIVE_RAID;
            case 8 -> AtBContractType.EXTRACTION_RAID;
            case 9 -> AtBContractType.RECON_RAID;
            case 10 -> AtBContractType.GARRISON_DUTY;
            default -> AtBContractType.CADRE_DUTY;
        };
    }

    public static AtBContractType getIndependentMission(int roll, int margin, boolean isClan) {
        if (isClan) {
            int result = roll + margin;
            if (result < 4 || result > 11) {
                // Reroll on the IS/Clan column if the force is clan and the result is Covert or Special
                return getInnerSphereClanMission(Compute.d6(2), margin, true);
            }
        }
        return switch(MathUtility.clamp(roll + margin, 2, 12)) {
            case 2 -> getCovertMission(Compute.d6(2), margin);
            case 3, 12 -> getSpecialMission(Compute.d6(2), margin);
            case 4 -> AtBContractType.PLANETARY_ASSAULT;
            case 5, 9 -> AtBContractType.OBJECTIVE_RAID;
            case 6 -> AtBContractType.EXTRACTION_RAID;
            case 7 -> AtBContractType.PIRATE_HUNTING;
            case 8 -> AtBContractType.SECURITY_DUTY;
            case 10 -> AtBContractType.GARRISON_DUTY;
            default -> AtBContractType.CADRE_DUTY;
        };
    }

    public static AtBContractType getCorporationMission(int roll, int margin, boolean isClan) {
        if (isClan) {
            int result = roll + margin;
            if (result < 5 || result > 11) {
                // Reroll on the IS/Clan column if the force is clan and the result is Covert or Special
                return getInnerSphereClanMission(Compute.d6(2), margin, true);
            }
        }
        return switch(MathUtility.clamp(roll + margin, 2, 12)) {
            case 2, 3 -> getCovertMission(Compute.d6(2), margin);
            case 4, 12 -> getSpecialMission(Compute.d6(2), margin);
            case 5, 8 -> AtBContractType.OBJECTIVE_RAID;
            case 6 -> AtBContractType.EXTRACTION_RAID;
            case 7 -> AtBContractType.RECON_RAID;
            case 9 -> AtBContractType.SECURITY_DUTY;
            case 10 -> AtBContractType.GARRISON_DUTY;
            // TODO: determine which is the higher paying between cadre/garrison and return that
            default -> AtBContractType.CADRE_DUTY;
        };
    }

    public static AtBContractType getPirateMission(int roll, int margin) {
        roll += margin;
        if (roll < 6) {
            return AtBContractType.RECON_RAID;
        } else {
            return AtBContractType.OBJECTIVE_RAID;
        }
    }

    private static AtBContractType getSpecialMission(int roll, int margin) {
        return switch(MathUtility.clamp(roll + margin, 2, 12)) {
            case 2 -> getCovertMission(Compute.d6(2), margin);
            // TODO: figure out how to offer planetary assault followup contracts
            case 3, 4 -> AtBContractType.GUERRILLA_WARFARE;
            // TODO: figure out how to offer planetary assault followup contracts
            case 5, 8 -> AtBContractType.RECON_RAID;
            case 6 -> AtBContractType.EXTRACTION_RAID;
            // TODO: change this to RETAINER if/when that is implemented
            case 7 -> AtBContractType.GARRISON_DUTY;
            case 9 -> AtBContractType.RELIEF_DUTY;
            // TODO: figure out how to offer planetary assault followup contracts
            case 10 -> AtBContractType.DIVERSIONARY_RAID;
            // TODO: determine which is the higher paying between riot/garrison and return that
            case 11 -> AtBContractType.RIOT_DUTY;
            // TODO: determine which is the higher paying between cadre/garrison and return that
            default -> AtBContractType.CADRE_DUTY;
        };
    }

    private static AtBContractType getCovertMission(int roll, int margin) {
        // TODO: most of the covert mission types are not implemented in MekHQ at the time of writing,
        //  so just use the special missions table for now.
        return getSpecialMission(Compute.d6(2), margin);
    }
}
