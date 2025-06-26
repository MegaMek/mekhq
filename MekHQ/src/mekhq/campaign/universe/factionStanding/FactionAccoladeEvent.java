/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.rating.IUnitRating.DRAGOON_A;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.ADOPTION_OR_LANCE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionAccoladeConfirmationDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionAccoladeDialog;

public class FactionAccoladeEvent {
    private static final MMLogger LOGGER = MMLogger.create(FactionAccoladeEvent.class);

    final Campaign campaign;
    final String factionCode;

    public FactionAccoladeEvent(Campaign campaign, Faction faction, FactionAccoladeLevel accoladeLevel,
          boolean isSameFaction) {
        this.campaign = campaign;
        this.factionCode = faction.getShortName();

        Person commander = campaign.getCommander();

        FactionAccoladeDialog dialog = new FactionAccoladeDialog(campaign, factionCode, accoladeLevel, isSameFaction,
              commander);

        if (accoladeLevel.is(ADOPTION_OR_LANCE)) {
            if (dialog.wasRefused()) {
                FactionAccoladeConfirmationDialog confirmationDialog = new FactionAccoladeConfirmationDialog(campaign,
                      accoladeLevel);
                if (!confirmationDialog.wasConfirmed()) {
                    new FactionAccoladeEvent(campaign, faction, accoladeLevel, isSameFaction);
                    return;
                }
            }

            if (!isSameFaction) {
                GoingRogue.processGoingRogue(campaign, faction, campaign.getCommander(), null);
            }

            List<Entity> generatedEntities = generateUnits();
            for (Entity entity : generatedEntities) {
                campaign.addNewUnit(entity, false, 0);
            }
        }
    }

    private List<Entity> generateUnits() {
        List<Entity> generatedEntities = new ArrayList<>();

        final Collection<EntityMovementMode> movementModes = new ArrayList<>();
        movementModes.add(EntityMovementMode.BIPED);

        Faction faction = Factions.getInstance().getFaction(factionCode);
        boolean factionIsClan = faction.isClan();
        int formationSize = faction.getFormationBaseSize();

        int gameYear = campaign.getGameYear();

        IUnitGenerator unitGenerator = campaign.getUnitGenerator();

        for (int i = 0; i < formationSize; i++) {
            int weight = AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, faction);

            final MekSummary mekSummary = unitGenerator.generate(factionCode,
                  UnitType.MEK,
                  weight,
                  gameYear,
                  DRAGOON_A,
                  movementModes,
                  new ArrayList<>(),
                  ms -> isSuitable(ms, gameYear, factionIsClan));

            if (mekSummary != null) {
                try {
                    Entity entity = new MekFileParser(mekSummary.getSourceFile(),
                          mekSummary.getEntryName()).getEntity();

                    if (entity != null) {
                        generatedEntities.add(entity);
                    }
                } catch (EntityLoadingException e) {
                    LOGGER.error("Failed to load entity from file '{}' for mek '{}'",
                          mekSummary.getSourceFile(),
                          mekSummary.getEntryName(),
                          e);
                }
            }
        }

        return generatedEntities;
    }

    private static boolean isSuitable(MekSummary mekSummary, int gameYear, boolean factionIsClan) {
        if (gameYear < mekSummary.getYear()) {
            return false;
        }

        if (mekSummary.isClan()) {
            return factionIsClan;
        } else {
            return !factionIsClan;
        }
    }
}
