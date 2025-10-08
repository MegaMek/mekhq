/*
 * Copyright (c) 2016 - Carl Spain. All rights reserved.
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.Parameters;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Provides access to RATGenerator through the AbstractUnitGenerator and thus the IUnitGenerator interface.
 *
 * @author Neoancient
 */
public class RATGeneratorConnector extends AbstractUnitGenerator {
    private static final MMLogger LOGGER = MMLogger.create(RATGeneratorConnector.class);

    /**
     * Initialize RATGenerator and load the data for the current game year
     */
    public RATGeneratorConnector(final int year) {
        while (!RATGenerator.getInstance().isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                LOGGER.error("", e);
            }
        }
        RATGenerator.getInstance().loadYear(year);
    }

    private @Nullable UnitTable findTable(final String faction, final int unitType, final int weightClass,
          final int year, final int quality, final Collection<EntityMovementMode> movementModes,
          final Collection<MissionRole> missionRoles) {
        final FactionRecord factionRecord = Factions.getInstance().getFactionRecordOrFallback(faction);
        if (factionRecord == null) {
            return null;
        }
        final String rating = getFactionSpecificRating(factionRecord, quality);
        final List<Integer> weightClasses = new ArrayList<>();
        if (weightClass >= 0) {
            weightClasses.add(weightClass);
        }
        return UnitTable.findTable(factionRecord,
              unitType,
              year,
              rating,
              weightClasses,
              ModelRecord.NETWORK_NONE,
              movementModes,
              missionRoles,
              2,
              factionRecord);
    }

    /**
     * Helper function that extracts the string-based unit rating from the given int-based unit-rating for the given
     * faction.
     *
     * @param factionRecord Faction record
     * @param quality       Unit quality number
     *
     * @return Unit quality string
     */
    public static String getFactionSpecificRating(final FactionRecord factionRecord, final int quality) {
        String rating = null;
        if (factionRecord.getRatingLevels().size() != 1) {
            final List<String> ratings = factionRecord.getRatingLevelSystem();
            rating = ratings.get(Math.min(quality, ratings.size() - 1));
        }
        return rating;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.campaign.universe.IUnitGenerator#isSupportedUnitType(int)
     */
    @Override
    public boolean isSupportedUnitType(final int unitType) {
        return (unitType != UnitType.GUN_EMPLACEMENT) && (unitType != UnitType.SPACE_STATION);
    }

    @Override
    public @Nullable MekSummary generate(final String faction, final int unitType, final int weightClass,
          final int year, final int quality, final Collection<EntityMovementMode> movementModes,
          final Collection<MissionRole> missionRoles, @Nullable Predicate<MekSummary> filter) {
        final UnitTable table = findTable(faction, unitType, weightClass, year, quality, movementModes, missionRoles);
        return (table == null) ? null : table.generateUnit((filter == null) ? null : filter::test);
    }

    @Override
    public List<MekSummary> generate(final int count, final String faction, final int unitType, final int weightClass,
          final int year, final int quality, final Collection<EntityMovementMode> movementModes,
          final Collection<MissionRole> missionRoles, @Nullable Predicate<MekSummary> filter) {
        final UnitTable table = findTable(faction, unitType, weightClass, year, quality, movementModes, missionRoles);
        return (table == null) ? new ArrayList<>() : table.generateUnits(count, (filter == null) ? null : filter::test);
    }

    /**
     * Generates a list of mek summaries from a RAT determined by the given faction, quality and other parameters. We
     * force a fallback to try to ensure that something is generated if the parents have any possible units to generate,
     * as that is the normally expected behaviour for MekHQ OpFor generation.
     *
     * @param count      How many units to generate
     * @param parameters RATGenerator parameters
     */
    @Override
    public List<MekSummary> generate(final int count, final UnitGeneratorParameters parameters) {
        final UnitTable table = findOpForTable(parameters);
        return table.generateUnits(count,
              (parameters.getFilter() == null) ? null : ms -> parameters.getFilter().test(ms));
    }

    /**
     * Generates a single mek summary from a RAT determined by the given faction, quality and other parameters. We force
     * a fallback to try to ensure that something is generated if the parents have any possible units to generate, as
     * that is the normally expected behaviour for MekHQ OpFor generation.
     *
     * @param parameters RATGenerator parameters
     */
    @Override
    public @Nullable MekSummary generate(final UnitGeneratorParameters parameters) {
        final UnitTable table = findOpForTable(parameters);
        return table.generateUnit((parameters.getFilter() == null) ? null : ms -> parameters.getFilter().test(ms));
    }

    /**
     * This finds a unit table for OpFor generation. It falls back using the parent faction to try to ensure there are
     * units in the unit table, so an OpFor is generated.
     *
     * @param unitParameters the base parameters to find the table using.
     *
     * @return the unit table to use in generating OpFor mek summaries
     */
    private UnitTable findOpForTable(final UnitGeneratorParameters unitParameters) {
        final Parameters parameters = unitParameters.getRATGeneratorParameters();
        UnitTable table = UnitTable.findTable(parameters);
        if (!table.hasUnits()) {
            // Do Parent Factions Fallbacks to try to ensure units can be generated, at a
            // maximum of 10
            List<String> factions = parameters.getFaction().getParentFactions();
            for (int i = 0; (i < 10) && !factions.isEmpty(); i++) {
                final Set<String> parentFactions = new HashSet<>();
                for (final String factionCode : factions) {
                    // Use the current Parent Faction
                    FactionRecord newFaction = RATGenerator.getInstance().getFaction(factionCode);
                    if (newFaction == null) {
                        // No parent faction found
                        LOGGER.warn("Failed lookup of faction code '{}', skipping...", factionCode);
                        continue;
                    }
                    parameters.setFaction(RATGenerator.getInstance().getFaction(factionCode));

                    // Get the table for the new Parent Faction
                    table = UnitTable.findTable(parameters);

                    // Check if this one has units, returning if it does
                    if (table.hasUnits()) {
                        return table;
                    }

                    // Save the Potential Parent Factions
                    parentFactions.addAll(parameters.getFaction().getParentFactions());
                }
                factions = new ArrayList<>(parentFactions);
            }
        }
        return table;
    }
}
