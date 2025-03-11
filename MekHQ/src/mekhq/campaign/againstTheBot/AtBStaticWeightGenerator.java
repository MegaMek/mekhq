/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.againstTheBot;

import megamek.common.Compute;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;

public class AtBStaticWeightGenerator {
    /**
     * @param campaign the campaign to generate the unit weight based on
     * @param unitType the unit type to determine the format of weight to generate
     * @param faction  the faction to generate the weight for
     * @return the generated weight
     */
    public static int getRandomWeight(final Campaign campaign, final int unitType,
            final Faction faction) {
        return getRandomWeight(unitType, faction, campaign.getCampaignOptions().isRegionalMekVariations());
    }

    /**
     * @param unitType         the unit type to determine the format of weight to
     *                         generate
     * @param faction          the faction to generate the weight for
     * @param regionVariations whether to generate 'Mek weights based on hardcoded
     *                         regional variations
     * @return the generated weight
     */
    private static int getRandomWeight(final int unitType, final Faction faction,
            final boolean regionVariations) {
        if (unitType == UnitType.AEROSPACEFIGHTER) {
            return getRandomAerospaceWeight();
        } else if ((unitType == UnitType.MEK) && regionVariations) {
            return getRegionalMekWeight(faction);
        } else {
            return getRandomMekWeight();
        }
    }

    /**
     * @return the generated weight for a BattleMek
     */
    private static int getRandomMekWeight() {
        final int roll = Compute.randomInt(10);
        if (roll < 3) {
            return EntityWeightClass.WEIGHT_LIGHT;
        } else if (roll < 7) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        } else if (roll < 9) {
            return EntityWeightClass.WEIGHT_HEAVY;
        } else {
            return EntityWeightClass.WEIGHT_ASSAULT;
        }
    }

    /**
     * @param faction the faction to determine the regional BattleMek weight for
     * @return the generated weight for a BattleMek
     */
    private static int getRegionalMekWeight(final Faction faction) {
        final int roll = Compute.randomInt(100);
        switch (faction.getShortName()) {
            case "DC":
                if (roll < 40) {
                    return EntityWeightClass.WEIGHT_LIGHT;
                } else if (roll < 60) {
                    return EntityWeightClass.WEIGHT_MEDIUM;
                } else if (roll < 90) {
                    return EntityWeightClass.WEIGHT_HEAVY;
                } else {
                    return EntityWeightClass.WEIGHT_ASSAULT;
                }
            case "LA":
                if (roll < 20) {
                    return EntityWeightClass.WEIGHT_LIGHT;
                } else if (roll < 50) {
                    return EntityWeightClass.WEIGHT_MEDIUM;
                } else if (roll < 85) {
                    return EntityWeightClass.WEIGHT_HEAVY;
                } else {
                    return EntityWeightClass.WEIGHT_ASSAULT;
                }
            case "FWL":
                if (roll < 30) {
                    return EntityWeightClass.WEIGHT_LIGHT;
                } else if (roll < 70) {
                    return EntityWeightClass.WEIGHT_MEDIUM;
                } else if (roll < 92) {
                    return EntityWeightClass.WEIGHT_HEAVY;
                } else {
                    return EntityWeightClass.WEIGHT_ASSAULT;
                }
            default:
                if (roll < 30) {
                    return EntityWeightClass.WEIGHT_LIGHT;
                } else if (roll < 70) {
                    return EntityWeightClass.WEIGHT_MEDIUM;
                } else if (roll < 90) {
                    return EntityWeightClass.WEIGHT_HEAVY;
                } else {
                    return EntityWeightClass.WEIGHT_ASSAULT;
                }
        }
    }

    /**
     * @return the generated random weight for an Aerospace Fighter
     */
    private static int getRandomAerospaceWeight() {
        final int roll = Compute.randomInt(8);
        if (roll < 3) {
            return EntityWeightClass.WEIGHT_LIGHT;
        } else if (roll < 7) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        } else {
            return EntityWeightClass.WEIGHT_HEAVY;
        }
    }
}
