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
package mekhq.campaign.universe.garrison;

import java.time.LocalDate;
import java.util.function.Function;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.units.UnitType;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.commandGeneration.ratgen.CommandGenerator;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorSnapshot;

/**
 * Generates a planetary garrison for a militia faction: it resolves the roll modifiers for the system and date, rolls
 * the {@link RandomGarrisonTable} to size the garrison, then generates each contingent (infantry regiments, armor
 * battalions, and BattleMek battalions) and assembles them under a single garrison force.
 */
public final class PlanetaryGarrisonGenerator {

    // Echelon levels from data/forcegenerator/faction_rules/constants.txt; there is no shared Java enum for them.
    private static final int BRIGADE_ECHELON = 7;
    private static final int REGIMENT_ECHELON = 6;
    private static final int BATTALION_ECHELON = 5;

    /** Planetary militia always generate at the lowest equipment rating. */
    private static final String MILITIA_RATING = "F";

    private static final String GARRISON_NAME = "Planetary Militia";

    private PlanetaryGarrisonGenerator() {}

    /**
     * Rolls and builds a planetary garrison for the given faction defending the given system on the given date.
     *
     * @param factionKey the militia faction the garrison belongs to
     * @param system     the planetary system being garrisoned
     * @param when       the date to generate for
     *
     * @return the garrison force, its contingents nested beneath it
     */
    public static ForceDescriptor generate(String factionKey, PlanetarySystem system, LocalDate when) {
        int modifier = GarrisonModifierResolver.resolveModifier(system, when);
        GarrisonComposition composition = RandomGarrisonTable.roll(modifier);
        return assemble(factionKey, when.getYear(), composition, snapshot -> CommandGenerator.rollCommand(snapshot, null));
    }

    /**
     * Assembles a garrison from a fixed composition using the supplied formation generator. The formation generator is
     * injected so this can be tested without the RAT generation engine; production passes
     * {@link CommandGenerator#rollCommand}.
     *
     * @param factionKey         the militia faction the garrison belongs to
     * @param year               the generation year
     * @param composition        the number of each contingent to generate
     * @param formationGenerator generates one formation from a snapshot, or returns {@code null} if it could not
     *
     * @return the garrison force with each generated contingent nested beneath it
     */
    static ForceDescriptor assemble(String factionKey, int year, GarrisonComposition composition,
          Function<ForceDescriptorSnapshot, ForceDescriptor> formationGenerator) {
        ForceDescriptor garrison = new ForceDescriptor();
        garrison.setTopLevel(true);
        garrison.setFaction(factionKey);
        garrison.setYear(year);
        garrison.setEchelon(BRIGADE_ECHELON);
        garrison.setName(GARRISON_NAME);

        addContingent(garrison, factionKey, year, UnitType.INFANTRY, REGIMENT_ECHELON,
              composition.infantryRegiments(), formationGenerator);
        addContingent(garrison, factionKey, year, UnitType.TANK, BATTALION_ECHELON,
              composition.armorBattalions(), formationGenerator);
        addContingent(garrison, factionKey, year, UnitType.MEK, BATTALION_ECHELON,
              composition.mekBattalions(), formationGenerator);

        return garrison;
    }

    private static void addContingent(ForceDescriptor garrison, String factionKey, int year, int unitType,
          int echelon, int count, Function<ForceDescriptorSnapshot, ForceDescriptor> formationGenerator) {
        for (int i = 0; i < count; i++) {
            ForceDescriptor formation = formationGenerator.apply(
                  buildSnapshot(factionKey, year, unitType, echelon));
            if (formation != null) {
                formation.setParent(garrison);
                garrison.addSubForce(formation);
            }
        }
    }

    private static ForceDescriptorSnapshot buildSnapshot(String factionKey, int year, int unitType, int echelon) {
        ForceDescriptorSnapshot snapshot = new ForceDescriptorSnapshot();
        snapshot.setFaction(factionKey);
        snapshot.setYear(year);
        snapshot.setUnitType(unitType);
        snapshot.setEchelon(echelon);
        snapshot.setRating(MILITIA_RATING);
        return snapshot;
    }
}
