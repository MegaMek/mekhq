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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.ConvInfantry;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Regression tests for the environmental kit MekHQ issues to conventional infantry it generates for a scenario.
 *
 * <p>The kit only matters if MegaMek agrees the platoon can survive the battlefield, so every check here asks
 * MegaMek's own {@link PlanetaryConditions#whyDoomed} rather than inspecting the kit directly. A platoon that MekHQ
 * believes it has protected but MegaMek still dooms dies the moment it deploys, which is how freed prisoners in
 * Prisoner Liberation scenarios were lost on tainted worlds: they were handed environment suits but not the XCT
 * training MegaMek also requires. If MegaMek tightens its rules again, these tests should fail before players
 * notice.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class InfantryEnvironmentalKitTest {
    /** A comfortable temperature, so only the air and pressure decide whether the platoon survives. */
    private static final int MILD_TEMPERATURE = 25;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Every pairing of unbreathable air with a pressure MekHQ will still put conventional infantry into.
     */
    static List<Arguments> unbreathableAirAtEveryPressure() {
        List<Arguments> airAndPressurePairs = new ArrayList<>();
        for (AtmosphericTaint atmosphericTaint : AtmosphericTaint.values()) {
            if (atmosphericTaint.isBreathable()) {
                continue;
            }
            for (Atmosphere atmosphere : Atmosphere.values()) {
                airAndPressurePairs.add(Arguments.of(atmosphericTaint, atmosphere));
            }
        }
        return airAndPressurePairs;
    }

    @ParameterizedTest(name = "{0} air at {1} pressure")
    @MethodSource("unbreathableAirAtEveryPressure")
    @DisplayName("Infantry dressed for a scenario's unbreathable air can deploy into it")
    void infantryDressedForUnbreathableAirSurvivesDeployment(AtmosphericTaint atmosphericTaint,
          Atmosphere atmosphere) {
        AtBDynamicScenario scenario = createScenario(atmosphericTaint, atmosphere);
        ConvInfantry platoon = new ConvInfantry();

        // The same call that dresses escapees, mole-hunt VIPs and fixed-MUL infantry
        AtBDynamicScenarioFactory.updateArmorKits(scenario, platoon, createCampaignOptions());

        String reasonDoomed = scenario.createPlanetaryConditions().whyDoomed(platoon, new Game());
        assertNull(reasonDoomed,
              "MekHQ dressed this platoon for " + atmosphericTaint + " air at " + atmosphere +
                    " pressure, but MegaMek still dooms it: " + reasonDoomed);
    }

    @ParameterizedTest
    @EnumSource(value = Atmosphere.class)
    @DisplayName("Infantry dressed for breathable air at any pressure can deploy into it")
    void infantryDressedForBreathableAirSurvivesDeployment(Atmosphere atmosphere) {
        AtBDynamicScenario scenario = createScenario(AtmosphericTaint.BREATHABLE, atmosphere);
        ConvInfantry platoon = new ConvInfantry();

        AtBDynamicScenarioFactory.updateArmorKits(scenario, platoon, createCampaignOptions());

        String reasonDoomed = scenario.createPlanetaryConditions().whyDoomed(platoon, new Game());
        assertNull(reasonDoomed,
              "MekHQ dressed this platoon for breathable air at " + atmosphere +
                    " pressure, but MegaMek still dooms it: " + reasonDoomed);
    }

    @ParameterizedTest
    @EnumSource(value = AtmosphericTaint.class, names = { "TAINTED_CAUSTIC", "TAINTED_POISON", "TOXIC_CAUSTIC",
                                                          "TOXIC_POISON" })
    @DisplayName("Undressed infantry are doomed in air that requires XCT troops")
    void undressedInfantryAreDoomedInHarmfulAir(AtmosphericTaint atmosphericTaint) {
        // Guards the tests above against passing for the wrong reason. If this fails, MegaMek no longer dooms
        // unprotected infantry in this air, and the kit tests above no longer prove the kit is doing anything.
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphericTaint(atmosphericTaint);

        assertNotNull(planetaryConditions.whyDoomed(new ConvInfantry(), new Game()),
              "MegaMek should doom conventional infantry with no kit in " + atmosphericTaint + " air");
    }

    @Test
    @DisplayName("Infantry dressed for tainted air become XCT troops")
    void dressingForTaintedAirGrantsXct() {
        ConvInfantry platoon = new ConvInfantry();

        AtBDynamicScenarioFactory.changeInfantryKit(platoon, false, true, MILD_TEMPERATURE);

        assertTrue(platoon.isXCT(), "a platoon dressed for tainted air should be XCT trained");
        assertTrue(platoon.isXCTForTaintedAtmosphere(), "the platoon should be XCT with a kit rated for tainted air");
        assertTrue(platoon.isXCTForToxicAtmosphere(), "the platoon should be XCT with a kit rated for toxic air");
    }

    @Test
    @DisplayName("Infantry in breathable air are not made XCT troops")
    void dressingForBreathableAirDoesNotGrantXct() {
        // XCT multiplies a platoon's cost by five, so it should only be handed out where the air demands it
        ConvInfantry platoon = new ConvInfantry();

        AtBDynamicScenarioFactory.changeInfantryKit(platoon, true, false, MILD_TEMPERATURE);

        assertFalse(platoon.isXCT(), "a platoon dressed for thin but breathable air should not be XCT trained");
    }

    @Test
    @DisplayName("Dressing infantry for tainted air keeps their existing specializations")
    void dressingForTaintedAirKeepsOtherSpecializations() {
        ConvInfantry platoon = new ConvInfantry();
        platoon.setSpecializations(ConvInfantry.MOUNTAIN_TROOPS);

        AtBDynamicScenarioFactory.changeInfantryKit(platoon, false, true, MILD_TEMPERATURE);

        assertEquals(ConvInfantry.MOUNTAIN_TROOPS | ConvInfantry.XCT, platoon.getSpecializations(),
              "the platoon should gain XCT without losing its mountain troops specialization");
    }

    private static AtBDynamicScenario createScenario(AtmosphericTaint atmosphericTaint, Atmosphere atmosphere) {
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        scenario.setAtmosphericTaint(atmosphericTaint);
        scenario.setAtmosphere(atmosphere);
        scenario.setTemperature(MILD_TEMPERATURE);
        return scenario;
    }

    private static CampaignOptions createCampaignOptions() {
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.USE_WEATHER_CONDITIONS)).thenReturn(false);
        return campaignOptions;
    }
}
