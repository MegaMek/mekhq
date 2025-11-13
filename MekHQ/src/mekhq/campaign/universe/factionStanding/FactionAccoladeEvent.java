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

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.*;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.PIRACY_SUCCESS_INDEX_FACTION_CODE;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getFactionName;
import static mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog.getFactionJudgmentDialogResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import megamek.common.enums.Gender;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.common.universe.FactionLeaderData;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionAccoladeConfirmationDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentNewsArticle;

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
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionJudgmentDialog";

    static final String LOOKUP_AFFIX_ADOPTION = ".adoption";
    static final String LOOKUP_AFFIX_MEKS = ".meks";
    static final String MAGISTRACY_HOLO_STAR_GIVEN_NAME = "Mia";
    static final String MAGISTRACY_HOLO_STAR_SURNAME = "Meklove";

    static final double C_BILL_MULTIPLIER = 5000000.0;
    static final int C_BILL_MULTIPLIER_TEXT = (int) (C_BILL_MULTIPLIER / 1000000);

    static final int REFUSE_ACCOLADE_RESPONSE_INDEX = 2;

    final Campaign campaign;
    final String factionCode;

    /**
     * Creates a new {@link FactionAccoladeEvent} and applies its effects to the campaign.
     *
     * <p>Handles user dialog interaction, confirmation (if required), processing narrative events, and generating
     * new units awarded by the accolade.</p>
     *
     * @param campaign          the campaign receiving the accolade
     * @param accoladingFaction the faction granting the accolade
     * @param accoladeLevel     the type/level of accolade
     * @param isSameFaction     whether the campaign's commander currently belongs to the awarding faction
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionAccoladeEvent(Campaign campaign, Faction accoladingFaction, FactionAccoladeLevel accoladeLevel,
          boolean isSameFaction) {
        this.campaign = campaign;
        this.factionCode = accoladingFaction.getShortName();

        // This is a minor accolade level with no special effects
        if (accoladeLevel.is(TAKING_NOTICE_0) || accoladeLevel.is(TAKING_NOTICE_1)) {
            triggerTakingNoticeNotification(campaign, accoladingFaction, accoladeLevel);
            return;
        }

        // If the faction isn't playable, we don't want to give the player a chance to join.
        boolean isAdoptionOrLance = accoladeLevel.is(ADOPTION_OR_MEKS);
        if (isAdoptionOrLance) {
            if (!accoladingFaction.isPlayable()) {
                return;
            }
        }

        boolean isCashReward = accoladeLevel.is(CASH_BONUS_0) ||
                                     accoladeLevel.is(CASH_BONUS_1) ||
                                     accoladeLevel.is(CASH_BONUS_2) ||
                                     accoladeLevel.is(CASH_BONUS_3) ||
                                     accoladeLevel.is(CASH_BONUS_4);

        Person commander = campaign.getCommander();

        boolean accoladeWasRefused;

        boolean triggerNewsArticle = isTriggerNewsArticle(accoladingFaction, accoladeLevel);
        if (triggerNewsArticle) {
            boolean useFactionCapitalAsLocation = accoladeLevel.is(STATUE_OR_SIBKO);
            new FactionJudgmentNewsArticle(campaign, commander, null, accoladeLevel.getLookupName(),
                  accoladingFaction, FactionStandingJudgmentType.ACCOLADE, useFactionCapitalAsLocation);
            return;
        } else {
            String lookupName = accoladeLevel.getLookupName();
            String oocText = null;
            if (accoladeLevel.is(ADOPTION_OR_MEKS)) {
                lookupName += isSameFaction ? LOOKUP_AFFIX_MEKS : LOOKUP_AFFIX_ADOPTION;

                String oocTextKey = isSameFaction
                                          ? "FactionJudgmentDialog.message.ACCOLADE.ADOPTION_OR_MEKS.meks.ooc"
                                          : "FactionJudgmentDialog.message.ACCOLADE.ADOPTION_OR_MEKS.adoption.ooc";
                oocText = getTextAt(getFactionJudgmentDialogResourceBundle(), oocTextKey);
            }

            ImmersiveDialogWidth dialogWidth;
            if (accoladeLevel.is(APPEARING_IN_SEARCHES) ||
                      isCashReward) {
                dialogWidth = ImmersiveDialogWidth.MEDIUM;
            } else {
                dialogWidth = ImmersiveDialogWidth.LARGE;
            }

            Integer moneyReward = null;
            if (isCashReward) {
                moneyReward = accoladeLevel.getRecognition() * C_BILL_MULTIPLIER_TEXT;
            }

            Person speaker = getSpeaker(campaign, accoladingFaction, accoladeLevel);
            if (speaker != null &&
                      isCashReward &&
                      accoladingFaction.getShortName().equals(PIRACY_SUCCESS_INDEX_FACTION_CODE)) {
                speaker = null;
            }

            FactionJudgmentDialog initialDialog = new FactionJudgmentDialog(campaign, speaker, commander, lookupName,
                  accoladingFaction, FactionStandingJudgmentType.ACCOLADE, dialogWidth, oocText, moneyReward);
            accoladeWasRefused = initialDialog.getChoiceIndex() == REFUSE_ACCOLADE_RESPONSE_INDEX;
        }

        if (isAdoptionOrLance) {
            FactionAccoladeConfirmationDialog confirmationDialog = new FactionAccoladeConfirmationDialog(campaign,
                  accoladeLevel);
            if (!confirmationDialog.wasConfirmed()) {
                new FactionAccoladeEvent(campaign, accoladingFaction, accoladeLevel, isSameFaction);
                return;
            }

            if (!isSameFaction && accoladeWasRefused) {
                String message = getFormattedTextAt(getFactionJudgmentDialogResourceBundle(),
                      "FactionJudgmentDialog.message.ACCOLADE.ADOPTION_OR_MEKS.meks.campaign",
                      getFactionName(accoladingFaction, campaign.getGameYear()),
                      spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG);

                new ImmersiveDialogNotification(campaign, message, false);
                return;
            }

            if (!isSameFaction) {
                GoingRogue.processGoingRogue(campaign, accoladingFaction, campaign.getCommander(), null,
                      campaign.getCampaignOptions().isTrackFactionStanding(), false);
            }

            List<Entity> generatedEntities = generateUnits();
            for (Entity entity : generatedEntities) {
                campaign.addNewUnit(entity, false, 0);
            }
            return;
        }

        if (isCashReward) {
            campaign.getFinances().credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(),
                  Money.of(accoladeLevel.getRecognition() * C_BILL_MULTIPLIER),
                  getTextAt(RESOURCE_BUNDLE, "FactionAccoladeDialog.credit"));
        }
    }

    private static void triggerTakingNoticeNotification(Campaign campaign, Faction accoladingFaction,
          FactionAccoladeLevel accoladeLevel) {
        String factionName = FactionStandingUtilities.getFactionName(accoladingFaction, campaign.getGameYear());
        String key = "FactionJudgmentDialog.message." + accoladeLevel.name() + ".message";
        String message = getFormattedTextAt(RESOURCE_BUNDLE, key, factionName);
        new ImmersiveDialogNotification(campaign, message, true);
    }

    /**
     * Determines and constructs the {@link Person} who will serve as the speaker for a given accolade event based on
     * the campaign state, the awarding faction, and the level of the accolade.
     *
     * <p>This method applies special rules for certain factions (such as the Magistracy of Canopus) and accolade
     * types, including adjusting the speaker's role, name, and gender when appropriate. For head-of-state letters, it
     * attempts to use the real leader's data if available.
     *
     * @param campaign          the current {@link Campaign} context in which the accolade is awarded
     * @param accoladingFaction the {@link Faction} granting the accolade
     * @param accoladeLevel     the {@link FactionAccoladeLevel} of the accolade being issued
     *
     * @return the generated {@link Person} to act as the speaker for this accolade event
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static Person getSpeaker(Campaign campaign, Faction accoladingFaction, FactionAccoladeLevel accoladeLevel) {
        Person speaker;
        if (accoladeLevel.is(TAKING_NOTICE_0) || accoladeLevel.is(TAKING_NOTICE_1)) {
            return null;
        } else if (accoladeLevel.is(APPEARING_IN_SEARCHES)) {
            speaker = campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND);
        } else {
            boolean isLetterFromHeadOfState = accoladeLevel.is(LETTER_FROM_HEAD_OF_STATE);
            boolean isMagistracySpecialCase = accoladingFaction.getShortName().equals("MOC")
                                                    && (accoladeLevel.is(ADOPTION_OR_MEKS) ||
                                                              accoladeLevel.is(STATUE_OR_SIBKO) ||
                                                              accoladeLevel.is(TRIUMPH_OR_REMEMBRANCE));

            PersonnelRole personnelRole = accoladingFaction.isClan()
                                                ? PersonnelRole.MEKWARRIOR
                                                : PersonnelRole.MILITARY_LIAISON;
            if (isMagistracySpecialCase) {
                personnelRole = PersonnelRole.HOLO_STAR;
            } else if (accoladingFaction.isClan() && accoladingFaction.isMercenaryOrganization()) {
                personnelRole = PersonnelRole.MERCHANT;
            } else if (!accoladingFaction.isClan() && isLetterFromHeadOfState) {
                personnelRole = PersonnelRole.NOBLE;
            } else if (accoladingFaction.isClan() && accoladeLevel.is(PROPAGANDA_REEL)) {
                personnelRole = PersonnelRole.MILITARY_HOLO_FILMER;
            }

            speaker = campaign.newPerson(personnelRole, accoladingFaction.getShortName(), Gender.RANDOMIZE);
            if (isMagistracySpecialCase) {
                speaker.setGender(Gender.FEMALE);
                speaker.setGivenName(MAGISTRACY_HOLO_STAR_GIVEN_NAME);
                speaker.setSurname(MAGISTRACY_HOLO_STAR_SURNAME);
                speaker.setSecondaryRole(PersonnelRole.MILITARY_PROMOTER);
            } else if (isLetterFromHeadOfState) {
                FactionLeaderData leaderData = accoladingFaction.getLeaderForYear(campaign.getGameYear());
                if (leaderData != null) {
                    String name = leaderData.getFullTitle(true);
                    speaker.setGivenName(name);
                    speaker.setSurname("");
                    speaker.setGender(leaderData.gender());
                }
            }
        }
        return speaker;
    }

    /**
     * Determines whether receiving an accolade of the given {@link FactionAccoladeLevel} from a specific
     * {@link Faction} should trigger the creation of a news article.
     *
     * <p>The result is based on the combination of the accolade level and the attributes of the awarding faction,
     * with special cases for Clans, the Magistracy of Canopus, ComStar, and Word of Blake.</p>
     *
     * @param accoladingFaction the {@link Faction} giving the accolade
     * @param accoladeLevel     the level of accolade as a {@link FactionAccoladeLevel}
     *
     * @return {@code true} if a news article should be triggered as a result of this accolade, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static boolean isTriggerNewsArticle(Faction accoladingFaction, FactionAccoladeLevel accoladeLevel) {
        boolean isClan = accoladingFaction.isClan() && !accoladingFaction.isMercenaryOrganization();

        final String MAGISTRACY_OF_CANOPUS = "MOC";
        boolean isMOC = accoladingFaction.getShortName().equals(MAGISTRACY_OF_CANOPUS);
        boolean isComStar = accoladingFaction.isComStar();
        boolean isWordOfBlake = accoladingFaction.isWoB();

        return switch (accoladeLevel) {
            case NO_ACCOLADE, CASH_BONUS_4, LETTER_FROM_HEAD_OF_STATE, CASH_BONUS_3, CASH_BONUS_2, ADOPTION_OR_MEKS,
                 CASH_BONUS_1, CASH_BONUS_0, APPEARING_IN_SEARCHES, TAKING_NOTICE_1, TAKING_NOTICE_0 -> false;
            case PRESS_RECOGNITION -> true;
            case PROPAGANDA_REEL -> !isClan;
            case TRIUMPH_OR_REMEMBRANCE -> !(isMOC || isClan);
            case STATUE_OR_SIBKO -> !(isMOC || isComStar || isWordOfBlake || isClan);
        };
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
                  DragoonRating.DRAGOON_A.getRating(),
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
