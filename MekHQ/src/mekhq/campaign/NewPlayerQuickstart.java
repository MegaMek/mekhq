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
package mekhq.campaign;

import static megamek.common.options.OptionsConstants.ALLOWED_YEAR;
import static mekhq.MHQConstants.CAMPAIGN_PRESET_DIRECTORY;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;

import java.io.File;
import java.time.LocalDate;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.units.UnitType;
import mekhq.CampaignPreset;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.commandGeneration.CommandBuilder;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.ratgen.CommandGenerator;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorSnapshot;

/**
 * Builds the New Player Quickstart: a mercenary company on Galatea at the start of 3151, ready to take its first
 * contract.
 *
 * <p>Nothing is shipped pre-built. The campaign takes its rules from the "New Players Start Here" preset and its
 * command from the Command Generator - a 'Mek company of three lances with the support staff, vehicles, spares, and
 * starting cash the Command Designer would give it - so the quickstart keeps up with both without anyone having to
 * maintain a save file.</p>
 *
 * <p>Neither step shows any UI, so both may run on the campaign loader's worker thread.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class NewPlayerQuickstart {
    /** The preset the quickstart takes its rules from, in {@link mekhq.MHQConstants#CAMPAIGN_PRESET_DIRECTORY}. */
    static final String PRESET_FILE_NAME = "1 - New Players Start Here.xml";
    public static final LocalDate START_DATE = LocalDate.of(3151, 1, 1);
    /** The mercenary capital in 3151, where hiring halls and contracts are easy to come by. */
    public static final String STARTING_SYSTEM_ID = "Galatea";
    /** A company: every Inner Sphere ruleset numbers it echelon 4, and builds it as three lances. */
    static final int COMPANY_ECHELON = 4;

    private NewPlayerQuickstart() {}

    /**
     * Sets up a freshly created campaign as the quickstart's mercenary company: its start date, faction, rank system,
     * name and starting location, and the rules of the "New Players Start Here" preset.
     *
     * @param campaign a new, empty campaign
     *
     * @throws IllegalStateException if the preset or the starting system cannot be found, which means the install is
     *                               damaged
     */
    public static void configure(Campaign campaign) {
        final CampaignPreset preset = loadPreset();
        final PlayerForce playerForce = campaign.getPlayerForce();

        // The preset's MegaMek options replace the campaign's, so the year they allow is set after them
        preset.applyContinuousOptions(campaign);
        campaign.setLocalDate(START_DATE);
        campaign.setCampaignStartDate(START_DATE);
        campaign.getGameOptions().getOption(ALLOWED_YEAR).setValue(campaign.getGameYear());
        campaign.setGMMode(preset.isGM());

        playerForce.setFaction(Factions.getInstance().getFaction(MERCENARY_FACTION_CODE));
        if (preset.getRankSystem() != null) {
            playerForce.setRankSystem(preset.getRankSystem());
        }

        final String name = BackgroundsController.randomMercenaryCompanyNameGenerator(null);
        playerForce.setName(name);
        playerForce.getFormations().setName(name);

        final PlanetarySystem startingSystem = campaign.getSystemById(STARTING_SYSTEM_ID);
        if (startingSystem == null) {
            throw new IllegalStateException("New Player Quickstart: starting system " + STARTING_SYSTEM_ID
                                                  + " is missing from the planetary data");
        }
        campaign.setStartingSystem(startingSystem.getPrimaryPlanet());
    }

    /**
     * Rolls the quickstart's company and builds it into the campaign, support and all, exactly as the Command Designer
     * would with its default settings, then finishes it as the designer does: awards, reputation, and the first
     * contract offers. The awards are issued without the ceremony, so nothing is shown. Follow it with
     * {@link #welcome} once the campaign is open.
     *
     * @param campaign a campaign already set up by {@link #configure}
     * @param listener progress listener, or {@code null} for none
     */
    public static void generateCommand(Campaign campaign, @Nullable Ruleset.ProgressListener listener) {
        final CommandGenerationOptions options = createCommandOptions(campaign);
        final ForceDescriptor rolled = CommandGenerator.rollCommand(options.getForceDescriptorSnapshot(), listener);
        CommandBuilder.build(campaign, options, rolled, listener);
        CommandBuilder.runPostGenerationExtras(campaign, false);
    }

    /**
     * Welcomes the new company as the Command Designer does once its build is accepted: the mercenary organization
     * introduces itself.
     *
     * <p>This is a dialog over the campaign window, so call it on the Event Dispatch Thread after the campaign has been
     * opened.</p>
     *
     * @param campaign the open campaign, built by {@link #generateCommand}
     */
    public static void welcome(Campaign campaign) {
        campaign.checkForNewMercenaryOrganizationStartUp(true, true);
    }

    /**
     * The Command Designer's defaults, pointed at a 'Mek company for the campaign's faction and year.
     *
     * @param campaign the campaign the command is for
     *
     * @return the generation options
     */
    static CommandGenerationOptions createCommandOptions(Campaign campaign) {
        final Faction faction = campaign.getPlayerForce().getFaction();
        final CommandGenerationOptions options = new CommandGenerationOptions();
        options.setSpecifiedFaction(faction);
        // The preset decides whether implants are tracked; a mercenary company has no augmentation to choose
        options.setApplyAugmentationRules(false);

        final ForceDescriptorSnapshot snapshot = options.getForceDescriptorSnapshot();
        snapshot.setFaction(faction.getShortName());
        snapshot.setYear(campaign.getGameYear());
        snapshot.setEchelon(COMPANY_ECHELON);
        snapshot.setUnitType(UnitType.MEK);
        return options;
    }

    /**
     * @return the "New Players Start Here" preset, fully loaded
     *
     * @throws IllegalStateException if it is missing or cannot be read
     */
    static CampaignPreset loadPreset() {
        final File presetFile = new File(CAMPAIGN_PRESET_DIRECTORY, PRESET_FILE_NAME);
        final CampaignPreset preset = CampaignPreset.parseFromFile(presetFile);
        if (preset == null) {
            throw new IllegalStateException("New Player Quickstart: unable to load the campaign preset "
                                                  + presetFile.getPath());
        }
        return preset;
    }
}
