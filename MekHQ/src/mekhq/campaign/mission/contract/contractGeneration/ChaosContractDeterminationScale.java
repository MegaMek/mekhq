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
package mekhq.campaign.mission.contract.contractGeneration;

import static java.lang.Math.ceil;
import static mekhq.campaign.force.FormationType.STANDARD;

import java.lang.ref.WeakReference;
import java.time.LocalDate;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.unit.Unit;

public class ChaosContractDeterminationScale {
    private final static double BATTLE_VALUE_PER_SCALE = 4_500.0; // Draconis Reach first printing pg 36
    private final static double BATTLEFIELD_SUPPORT_POINTS_PER_SCALE = 32.0; // Draconis Reach first printing pg 36
    private final static double BATTLE_VALUE_PER_BSP = 500.0; // Battle for Tukayyid, pg24

    /**
     * The last whole-TO&amp;E Scale worked out, reused until the day, the hangar's size, or the conversion option
     * changes. Working it out means calculating the Battle Value of every unit, which the finances report and every
     * contract offer would otherwise repeat.
     */
    private record TableOfOrganizationScale(WeakReference<Campaign> campaign, LocalDate date, int unitCount,
          boolean convertSupportPointsToBattleValue, int scale) {}

    private static volatile TableOfOrganizationScale cachedTableOfOrganizationScale;

    /**
     * As {@link #generateScaleForTableOfOrganization(PlayerForce, LocalHangar, boolean)} for the campaign's player
     * force, reusing the result for the rest of the day while the hangar's size is unchanged.
     *
     * @param campaign the campaign
     *
     * @return the Scale of the whole TO&amp;E
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getScaleForTableOfOrganization(Campaign campaign) {
        PlayerForce playerForce = campaign.getPlayerForce();
        LocalHangar hangar = playerForce.getHangar();
        LocalDate today = campaign.getLocalDate();
        int unitCount = hangar.getUnits().size();
        boolean convertSupportPointsToBattleValue = campaign.getCampaignOptions()
                                                          .get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION);

        TableOfOrganizationScale cached = cachedTableOfOrganizationScale;
        if (cached != null
                  && cached.campaign().get() == campaign
                  && cached.date().equals(today)
                  && cached.unitCount() == unitCount
                  && cached.convertSupportPointsToBattleValue() == convertSupportPointsToBattleValue) {
            return cached.scale();
        }

        int scale = generateScaleForTableOfOrganization(playerForce, hangar, convertSupportPointsToBattleValue);
        cachedTableOfOrganizationScale = new TableOfOrganizationScale(new WeakReference<>(campaign), today, unitCount,
              convertSupportPointsToBattleValue, scale);
        return scale;
    }

    static int generateScaleForDetachment(PlayerForce playerForce, LocalHangar hangar, boolean isCadreDuty,
          boolean convertSupportPointsToBattleValue) {
        int validBattleValue = 0;

        for (Unit unit : hangar.getUnits()) {
            int formationId = unit.getFormationId();
            Formation formation = playerForce.getFormation(formationId);
            if (formation != null) {
                CombatRole roleInMemory = formation.getCombatRoleInMemory();
                boolean hasCombatRole = roleInMemory.isCombatRole() || (isCadreDuty && roleInMemory.isCadre());
                if (formation.isFormationType(STANDARD) && hasCombatRole) {
                    Entity entity = unit.getEntity();
                    validBattleValue += entity != null ? entity.calculateBattleValue(true, true) : 0;
                }
            }
        }

        return convertBattleValueToScale(validBattleValue, convertSupportPointsToBattleValue);
    }

    /**
     * Determines the Scale of the player's entire TO&amp;E, for Hot Spots upkeep. Unlike
     * {@link #generateScaleForDetachment(PlayerForce, LocalHangar, boolean, boolean)}, every unit assigned to a
     * formation counts, regardless of formation type or combat role. Units outside the TO&amp;E and mothballed units
     * are ignored.
     *
     * @param playerForce                       the player force whose TO&amp;E is weighed
     * @param hangar                            the hangar holding the player force's units
     * @param convertSupportPointsToBattleValue whether the per-Scale battlefield support point allotment is folded into
     *                                          the per-Scale Battle Value
     *
     * @return the Scale of the whole TO&amp;E
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int generateScaleForTableOfOrganization(PlayerForce playerForce, LocalHangar hangar,
          boolean convertSupportPointsToBattleValue) {
        int totalBattleValue = 0;

        for (Unit unit : hangar.getUnits()) {
            if (unit.isMothballed()) {
                continue;
            }

            Formation formation = playerForce.getFormation(unit.getFormationId());
            if (formation != null) {
                Entity entity = unit.getEntity();
                totalBattleValue += entity != null ? entity.calculateBattleValue(true, true) : 0;
            }
        }

        return convertBattleValueToScale(totalBattleValue, convertSupportPointsToBattleValue);
    }

    /**
     * Converts a Battle Value total into Scale, rounding up.
     *
     * @param battleValue                       the Battle Value to convert
     * @param convertSupportPointsToBattleValue whether the per-Scale battlefield support point allotment is folded into
     *                                          the per-Scale Battle Value
     *
     * @return the resulting Scale
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static int convertBattleValueToScale(int battleValue, boolean convertSupportPointsToBattleValue) {
        double battleValuePerScale = BATTLE_VALUE_PER_SCALE;
        if (convertSupportPointsToBattleValue) {
            // Fold the battlefield-support-point allotment into the per-scale Battle Value by converting it to BV.
            battleValuePerScale += BATTLEFIELD_SUPPORT_POINTS_PER_SCALE * BATTLE_VALUE_PER_BSP;
        }

        return (int) ceil(battleValue / battleValuePerScale);
    }
}
