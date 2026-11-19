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
package mekhq.campaign.universe.commandGeneration;

import static mekhq.campaign.personnel.PersonUtility.overrideSkills;
import static mekhq.campaign.personnel.PersonUtility.reRollAdvantages;
import static mekhq.campaign.personnel.PersonUtility.reRollLoyalty;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.BulkGenerationCompletedEvent;
import mekhq.campaign.events.CommandGenerationFinishedEvent;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractMarketAvailability;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.reputation.camOpsReputation.ForceReputationController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.ratgen.CommandGenerator;
import mekhq.campaign.universe.commandGeneration.ratgen.RulesetEngineBootstrap;
import mekhq.gui.campaignOptions.optionChangeDialogs.AdvancedScoutingCampaignOptionsChangedConfirmationDialog;

/**
 * Turns a rolled command into a finished one: everything the Command Designer's Build does once the player has a
 * force they like, with no designer involved.
 *
 * <p>The designer runs these stages in phases behind its own progress dialogs; {@link #build} runs them in one pass
 * for callers that have no designer, such as the New Player Quickstart. Both share the stages below, so a command
 * built either way comes out the same.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class CommandBuilder {
    private static final MMLogger LOGGER = MMLogger.create(CommandBuilder.class);

    private static final Ruleset.ProgressListener SILENT = (progress, message) -> { };

    private CommandBuilder() {}

    /**
     * Builds a rolled command into the campaign in one pass: commits the combat force, generates the support it needs,
     * stocks and lifts it, hires the medical reserve, flags the founders, grants the starting cash, and runs the
     * starting simulation if asked for. The people generated take their origin from the options' Random Origin
     * choices, as in the designer; the campaign's own choices are restored afterwards.
     *
     * <p>Does no UI work, so it may (and for anything larger than a lance, should) run off the Event Dispatch Thread.
     * Follow it with {@link #runPostGenerationExtras} once it returns.</p>
     *
     * @param campaign the campaign to build into
     * @param options  the generation options
     * @param rolled   the rolled force, from {@link CommandGenerator#rollCommand}
     * @param listener progress listener, or {@code null} for none
     *
     * @return everyone the build hired
     */
    public static List<Person> build(Campaign campaign, CommandGenerationOptions options, ForceDescriptor rolled,
          @Nullable Ruleset.ProgressListener listener) {
        final Ruleset.ProgressListener progress = (listener == null) ? SILENT : listener;
        final Set<UUID> preExistingPersonIds = CommandGenerator.snapshotPersonnelIds(campaign);
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final RandomOriginOptions campaignOrigin = campaignOptions.get(CampaignOption.RANDOM_ORIGIN_OPTIONS);
        if (options.getRandomOriginOptions() != null) {
            campaignOptions.set(CampaignOption.RANDOM_ORIGIN_OPTIONS, options.getRandomOriginOptions());
        }

        campaign.setBulkGenerationInProgress(true);
        try {
            RulesetEngineBootstrap.ensureLoaded(options.getForceDescriptorSnapshot().getYear());
            final List<Person> generatedPersons = new ArrayList<>(
                  CommandGenerator.applyToCampaign(campaign, options, rolled, progress, false).generatedPersons());
            generatedPersons.addAll(CommandGenerator.generateSupportFromToe(campaign, options, progress));
            CommandGenerator.completeBuild(campaign, options, progress);

            generateMedicalReserve(campaign, options, generatedPersons);
            final List<Person> hired = CommandGenerator.personsAddedSince(campaign, preExistingPersonIds);
            CommandGenerator.applyRosterFlags(campaign, options, hired);
            CommandGenerator.processStartingCash(campaign, options);

            if (options.isRunStartingSimulation()) {
                StartingSimulation.run(campaign, options, hired, progress);
            }
            return hired;
        } finally {
            campaign.setBulkGenerationInProgress(false);
            campaignOptions.set(CampaignOption.RANDOM_ORIGIN_OPTIONS, campaignOrigin);
        }
    }

    /**
     * The last steps of a build, once the command exists: announces it, runs the award ceremony, rates its reputation,
     * grants the bonuses its campaign options call for, and opens the contract market where it stands.
     *
     * <p>With the ceremony shown, the player picks the awards in a dialog over the campaign window, so call this on
     * the Event Dispatch Thread once the campaign is open. Without it, every eligible award is issued straight away
     * and nothing is shown, so it may run before the campaign window exists.</p>
     *
     * @param campaign              the campaign the command was built into
     * @param isAwardCeremonyShown  {@code true} to hold the award ceremony, {@code false} to issue the awards
     *                              without it
     */
    public static void runPostGenerationExtras(Campaign campaign, boolean isAwardCeremonyShown) {
        long startNanos = System.nanoTime();
        LOGGER.info("[CompanyGen][PostGen] START (thread={})", Thread.currentThread().getName());
        MekHQ.triggerEvent(new BulkGenerationCompletedEvent(campaign, campaign.getPlayerForce().getFormations()));

        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.get(CampaignOption.ENABLE_AUTO_AWARDS)) {
            LOGGER.info("[CompanyGen][PostGen] running AutoAwardsController (ceremony shown={})",
                  isAwardCeremonyShown);
            if (isAwardCeremonyShown) {
                new AutoAwardsController().ManualController(campaign, false);
            } else {
                new AutoAwardsController().issueEligibleAwards(campaign);
            }
        }

        ForceReputationController reputationController = new ForceReputationController();
        reputationController.initializeReputation(campaign);
        campaign.getPlayerForce().setCamOpsReputation(reputationController);

        // Support-vehicle generation (salvage, MASH, fatigue, StratCon convoy, security) is handled during generation
        // by SupportPersonnelToTOE and SupportUnitGenerator, so only the free scouting skills are granted here.
        if (campaignOptions.get(CampaignOption.USE_ADVANCED_SCOUTING) && campaignOptions.isUseStratCon()) {
            AdvancedScoutingCampaignOptionsChangedConfirmationDialog.processFreeSkills(campaign, true);
        }

        AbstractLocation currentLocation = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        if ((currentLocation != null) && currentLocation.isOnPlanet()) {
            LOGGER.info("[CompanyGen][PostGen] generating initial contract market offers");
            ChaosContractMarketAvailability.processNewMonth(campaign);
        }

        MekHQ.triggerEvent(new CommandGenerationFinishedEvent(campaign));
        LOGGER.info("[CompanyGen][PostGen] DONE in {}ms", (System.nanoTime() - startNanos) / 1_000_000);
    }

    /**
     * Medical reserve: a generation-only option (independent of the campaign-wide Alternative Advanced Medical rule).
     * Hires spare, unassigned personnel as injury replacements for every combat role, each role's reserve sized as a
     * percentage of the people generated in it.
     *
     * @param campaign         the campaign being built into
     * @param options          the generation options carrying the reserve toggle and percentage
     * @param generatedPersons the combat crews and support staff the build generated, counted per combat role
     */
    public static void generateMedicalReserve(Campaign campaign, CommandGenerationOptions options,
          List<Person> generatedPersons) {
        if (!options.isGenerateMedicalReserve()) {
            return;
        }
        Map<PersonnelRole, Integer> sparesByRole = getMedicalReserveSparesByRole(generatedPersons,
              options.getMedicalReservePercent());
        for (Map.Entry<PersonnelRole, Integer> entry : sparesByRole.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                hireSparePersonnel(campaign, options, entry.getKey());
            }
        }
    }

    /**
     * Works out how many medical reserve spares to generate for each combat role. Each generated person is counted
     * under their primary role if it is a combat role, otherwise under their secondary role if that is; anyone with
     * no combat role is ignored. Each role's spares are the given percentage of its headcount, rounded up, so every
     * combat role present gets at least one spare whenever the percentage is above zero.
     *
     * @param generatedPersons      the personnel produced by the generator
     * @param medicalReservePercent the reserve size as a percentage of each role's headcount
     *
     * @return the number of spares to generate for each combat role, omitting roles that need none
     */
    static Map<PersonnelRole, Integer> getMedicalReserveSparesByRole(List<Person> generatedPersons,
          int medicalReservePercent) {
        Map<PersonnelRole, Integer> combatantsByRole = new EnumMap<>(PersonnelRole.class);
        for (Person person : generatedPersons) {
            PersonnelRole combatRole = null;
            if (person.getPrimaryRole().isCombat()) {
                combatRole = person.getPrimaryRole();
            } else if (person.getSecondaryRole().isCombat()) {
                combatRole = person.getSecondaryRole();
            }

            if (combatRole != null) {
                combatantsByRole.merge(combatRole, 1, Integer::sum);
            }
        }

        Map<PersonnelRole, Integer> sparesByRole = new EnumMap<>(PersonnelRole.class);
        for (Map.Entry<PersonnelRole, Integer> entry : combatantsByRole.entrySet()) {
            int spares = (int) Math.ceil(entry.getValue() * medicalReservePercent / 100.0);
            if (spares > 0) {
                sparesByRole.put(entry.getKey(), spares);
            }
        }
        return sparesByRole;
    }

    private static void hireSparePersonnel(Campaign campaign, CommandGenerationOptions options, PersonnelRole role) {
        Person person = campaign.getPlayerForce().getHumanResources().newPerson(campaign, role);

        overrideSkills(campaign, person, role, SkillLevel.GREEN, true);

        SkillLevel actualSkillLevel = person.getSkillLevel(campaign, false);
        reRollLoyalty(person, actualSkillLevel);
        reRollAdvantages(campaign, person, actualSkillLevel);

        if (options.isAutomaticallyAssignRanks()) {
            final Faction faction = campaign.getPlayerForce().getFaction();
            person.setRank((faction.isComStarOrWoB() || faction.isClan()) ? 4 : 12);
        }

        campaign.getPlayerForce().getHumanResources().recruitPerson(campaign, person, true, true);
    }
}
