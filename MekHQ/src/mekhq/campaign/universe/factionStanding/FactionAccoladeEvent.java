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
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.ADOPTION_OR_MEKS;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.CASH_BONUS;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.TAKING_NOTICE;
import static mekhq.utilities.MHQInternationalization.getTextAt;

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
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionAccoladeConfirmationDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionAccoladeDialog;

/**
 * Handles events where a campaign receives a faction accolade, such as adoption.
 *
 * <p>This class manages the process of applying faction accolades to a campaign, potentially including confirming
 * with the user, generating narrative dialogs, and adding new units to the campaign roster as a result of the accolade
 * event. Accolade effects and unit generation can be configured based on both the awarded faction and the level of
 * recognition.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionAccoladeEvent {
    private static final MMLogger LOGGER = MMLogger.create(FactionAccoladeEvent.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionAccoladeDialog";

    private static final double C_BILL_REWARD = 25000000.0;

    final Campaign campaign;
    final String factionCode;

    /**
     * Creates a new {@link FactionAccoladeEvent} and applies its effects to the campaign.
     *
     * <p>Handles user dialog interaction, confirmation (if required), processing narrative events, and generating
     * new units awarded by the accolade.</p>
     *
     * @param campaign      the campaign receiving the accolade
     * @param faction       the faction granting the accolade
     * @param accoladeLevel the type/level of accolade
     * @param isSameFaction whether the campaign's commander currently belongs to the awarding faction
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionAccoladeEvent(Campaign campaign, Faction faction, FactionAccoladeLevel accoladeLevel,
          boolean isSameFaction) {
        this.campaign = campaign;
        this.factionCode = faction.getShortName();

        // This is a silent accolade level, it gets logged internally, but the player isn't made aware of it
        if (accoladeLevel.is(TAKING_NOTICE)) {
            return;
        }

        // If the faction isn't playable, we don't want to give the player a chance to join.
        boolean isAdoptionOrLance = accoladeLevel.is(ADOPTION_OR_MEKS);
        if (isAdoptionOrLance) {
            if (!faction.isPlayable()) {
                return;
            }
        }

        Person commander = campaign.getCommander();

        FactionAccoladeDialog initialDialog = new FactionAccoladeDialog(campaign, faction, accoladeLevel, commander);

        if (isAdoptionOrLance) {
            FactionAccoladeConfirmationDialog confirmationDialog = new FactionAccoladeConfirmationDialog(campaign,
                  accoladeLevel);
            if (!confirmationDialog.wasConfirmed()) {
                new FactionAccoladeEvent(campaign, faction, accoladeLevel, isSameFaction);
                return;
            }

            if (initialDialog.wasRefused()) {
                return;
            }

            if (!isSameFaction) {
                GoingRogue.processGoingRogue(campaign, faction, campaign.getCommander(), null);
            }

            List<Entity> generatedEntities = generateUnits();
            for (Entity entity : generatedEntities) {
                campaign.addNewUnit(entity, false, 0);
            }
            return;
        }

        if (accoladeLevel.is(CASH_BONUS)) {
            campaign.getFinances().credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(),
                  Money.of(C_BILL_REWARD), getTextAt(RESOURCE_BUNDLE, "FactionAccoladeDialog.credit"));
        }
    }

    /**
     * Generates and returns a list of units awarded as part of the accolade event.
     *
     * <p>The generation process considers the faction, game year, required movement modes, and whether clan or inner
     * sphere eligibility applies in unit selection. If unit files fail to load, this is logged, and failed units are
     * skipped.</p>
     *
     * @return a list of {@link Entity} objects representing the generated units; may be empty if generation failed
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<Entity> generateUnits() {
        List<Entity> generatedEntities = new ArrayList<>();

        final Collection<EntityMovementMode> movementModes = new ArrayList<>();
        movementModes.add(EntityMovementMode.BIPED);

        Faction faction = Factions.getInstance().getFaction(factionCode);
        boolean factionIsClan = faction.isClan();
        int formationSize = CombatTeam.getStandardForceSize(faction, FormationLevel.COMPANY.getDepth());

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

    /**
     * Determines whether a {@link MekSummary} represents a unit that is suitable for addition to this event's
     * accolade.
     *
     * <p>A unit is suitable if its introduction year is less than the campaign's current year and its affiliation
     * (clan vs. inner sphere) matches the awarding faction.</p>
     *
     * @param mekSummary    the summary for the unit to check
     * @param gameYear      the current campaign year
     * @param factionIsClan whether the awarding faction is a clan
     *
     * @return {@code true} if the unit meets all suitability criteria; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
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
