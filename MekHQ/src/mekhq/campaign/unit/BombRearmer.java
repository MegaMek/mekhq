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
package mekhq.campaign.unit;

import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.getWarningColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.units.IBomber;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

/**
 * Tops an aircraft's bombs back up to its preferred loadout from the warehouse after a scenario, so the player does not
 * have to choose the same bombs again every time (issue #6200). Only aircraft whose player opted in, by ticking the box
 * in the bomb dialog, have a preferred loadout. Bombs the warehouse is short of are left off, and the daily report
 * says which.
 */
public final class BombRearmer {
    private static final MMLogger LOGGER = MMLogger.create(BombRearmer.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.BombRearmer";

    private BombRearmer() {}

    /**
     * Rearms the unit with its preferred bombs, if it has a preferred loadout. A loadout the aircraft can no longer
     * carry, for example after a refit, is dropped and the player is told.
     *
     * @param campaign the campaign whose warehouse supplies the bombs
     * @param unit     the unit back from a scenario
     */
    public static void rearmAfterScenario(Campaign campaign, Unit unit) {
        PreferredBombLoadout preferredLoadout = unit.getPreferredBombLoadout();
        if (preferredLoadout == null) {
            return;
        }
        if (!(unit.getEntity() instanceof IBomber bomber)) {
            LOGGER.debug("[BombRearm] {} has a preferred bomb loadout but cannot carry bombs", unit.getName());
            return;
        }
        if (!canCarry(bomber, preferredLoadout)) {
            LOGGER.info("[BombRearm] {}: preferred bomb loadout no longer fits, so it is dropped", unit.getName());
            unit.setPreferredBombLoadout(null);
            campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE, "BombRearmer.noLongerFits",
                  getWarningColor(), unit.getHyperlinkedName()));
            return;
        }

        BombLoadout internalBombs = new BombLoadout(bomber.getIntBombChoices());
        BombLoadout externalBombs = new BombLoadout(bomber.getExtBombChoices());
        BombLoadout shortages = new BombLoadout();
        int bombsLoaded = topUp(campaign, preferredLoadout.internalBombs(), internalBombs, shortages)
                                + topUp(campaign, preferredLoadout.externalBombs(), externalBombs, shortages);
        bomber.setIntBombChoices(internalBombs);
        bomber.setExtBombChoices(externalBombs);
        LOGGER.debug("[BombRearm] {}: {} bombs loaded, {} short", unit.getName(), bombsLoaded,
              shortages.getTotalBombs());

        if (shortages.getTotalBombs() > 0) {
            campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE, "BombRearmer.partlyRearmed",
                  getWarningColor(), unit.getHyperlinkedName(), bombsLoaded, describe(shortages)));
        } else if (bombsLoaded > 0) {
            campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE, "BombRearmer.rearmed",
                  unit.getHyperlinkedName(), bombsLoaded));
        }
    }

    /**
     * The aircraft can carry the loadout while its bombs fit in the internal bays and on the external hardpoints, the
     * same limit the bomb dialog uses.
     */
    private static boolean canCarry(IBomber bomber, PreferredBombLoadout preferredLoadout) {
        boolean doInternalBombsFit = preferredLoadout.internalBombs().getTotalBombCost() <= bomber.getMaxIntBombPoints();
        boolean doExternalBombsFit = preferredLoadout.externalBombs().getTotalBombCost() <= bomber.getMaxExtBombPoints();
        return doInternalBombsFit && doExternalBombsFit;
    }

    /**
     * Takes from the warehouse what the current bombs are short of the preferred ones.
     *
     * @param preferredBombs the bombs wanted in this position
     * @param currentBombs   the bombs carried in this position, topped up in place
     * @param shortages      the bombs the warehouse could not supply, added to
     *
     * @return how many bombs were loaded
     */
    private static int topUp(Campaign campaign, BombLoadout preferredBombs, BombLoadout currentBombs,
          BombLoadout shortages) {
        int bombsLoaded = 0;
        for (Map.Entry<BombTypeEnum, Integer> preferredBomb : preferredBombs.entrySet()) {
            BombTypeEnum bombType = preferredBomb.getKey();
            int bombsNeeded = preferredBomb.getValue() - currentBombs.getCount(bombType);
            if (bombsNeeded <= 0) {
                continue;
            }
            int bombsTaken = 0;
            // Pods such as TAG are equipment, not ammunition, and are never held in the warehouse as bombs
            if (EquipmentType.get(bombType.getInternalName()) instanceof AmmoType bombAmmunition) {
                bombsTaken = campaign.getQuartermaster().removeAmmo(bombAmmunition, bombsNeeded);
            }
            currentBombs.addBombs(bombType, bombsTaken);
            bombsLoaded += bombsTaken;
            if (bombsTaken < bombsNeeded) {
                shortages.addBombs(bombType, bombsNeeded - bombsTaken);
            }
        }
        return bombsLoaded;
    }

    /**
     * @return the bombs as a list for the report, such as "2 HE Bomb, 1 Cluster Bomb"
     */
    private static String describe(BombLoadout bombs) {
        List<String> bombDescriptions = new ArrayList<>();
        for (Map.Entry<BombTypeEnum, Integer> bombAndCount : bombs.entrySet()) {
            if (bombAndCount.getValue() > 0) {
                bombDescriptions.add(getFormattedTextAt(RESOURCE_BUNDLE, "BombRearmer.bombCount",
                      bombAndCount.getValue(), bombAndCount.getKey().getDisplayName()));
            }
        }
        return String.join(", ", bombDescriptions);
    }
}
