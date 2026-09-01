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
package mekhq.campaign.mission.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.PlanetaryConditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests that the air a world is recorded as having reaches the battle, and survives being saved.
 */
class ScenarioAtmosphericTaintTest {

    @Test
    @DisplayName("A scenario starts out in breathable air")
    void aNewScenarioHasBreathableAir() {
        assertEquals(AtmosphericTaint.BREATHABLE, new Scenario().getAtmosphericTaint());
    }

    @ParameterizedTest
    @EnumSource(AtmosphericTaint.class)
    @DisplayName("The scenario's air is handed to the planetary conditions the battle is fought under")
    void theTaintReachesThePlanetaryConditions(AtmosphericTaint atmosphericTaint) {
        Scenario scenario = new Scenario();
        scenario.setAtmosphericTaint(atmosphericTaint);

        PlanetaryConditions planetaryConditions = scenario.createPlanetaryConditions();

        assertEquals(atmosphericTaint, planetaryConditions.getAtmosphericTaint(),
              "the battle should be fought in the air the scenario recorded");
    }

    @ParameterizedTest
    @EnumSource(AtmosphericTaint.class)
    @DisplayName("Air set on the planetary conditions is read back onto the scenario")
    void theTaintIsReadBackFromThePlanetaryConditions(AtmosphericTaint atmosphericTaint) {
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphericTaint(atmosphericTaint);

        Scenario scenario = new Scenario();
        scenario.readPlanetaryConditions(planetaryConditions);

        assertEquals(atmosphericTaint, scenario.getAtmosphericTaint());
    }

    @Test
    @DisplayName("The taint and the pressure are carried separately")
    void theTaintAndThePressureAreIndependent() {
        // A tainted world is not necessarily a thin one, and MekHQ records the two against a planet separately.
        Scenario scenario = new Scenario();
        scenario.setAtmosphere(Atmosphere.HIGH);
        scenario.setAtmosphericTaint(AtmosphericTaint.TOXIC_CAUSTIC);

        PlanetaryConditions planetaryConditions = scenario.createPlanetaryConditions();

        assertEquals(Atmosphere.HIGH, planetaryConditions.getAtmosphere());
        assertEquals(AtmosphericTaint.TOXIC_CAUSTIC, planetaryConditions.getAtmosphericTaint());
    }

    @ParameterizedTest
    @EnumSource(AtmosphericTaint.class)
    @DisplayName("Every atmosphere survives the name written into the save file")
    void everyTaintRoundTripsThroughItsSavedName(AtmosphericTaint atmosphericTaint) {
        // Scenario writes the external id rather than the ordinal, so this is the exact string a save carries.
        String savedValue = atmosphericTaint.getExternalId();

        assertEquals(atmosphericTaint, AtmosphericTaint.getAtmosphericTaint(savedValue));
    }

    @Test
    @DisplayName("A scenario saved before tainted air existed loads as breathable rather than failing")
    void anAbsentSavedTaintReadsAsBreathable() {
        // Older saves carry no atmosphericTaint tag at all, so the field keeps the value the constructor gave it.
        assertEquals(AtmosphericTaint.BREATHABLE, new Scenario().getAtmosphericTaint());
        // An unreadable value falls back the same way rather than leaving the scenario without any air.
        assertEquals(AtmosphericTaint.BREATHABLE, AtmosphericTaint.getAtmosphericTaint("NOT_A_REAL_TAINT"));
    }
}
