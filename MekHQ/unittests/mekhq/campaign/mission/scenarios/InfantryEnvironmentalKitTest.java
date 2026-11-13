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
import megamek.common.equipment.MiscType;
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

    /** Extreme cold (below -30) and extreme heat (above 50), as MegaMek defines them. */
    static List<Arguments> extremeTemperatures() {
        return List.of(Arguments.of(-40), Arguments.of(-31), Arguments.of(51), Arguments.of(56));
    }

    @ParameterizedTest(name = "{0} degrees C")
    @MethodSource("extremeTemperatures")
    @DisplayName("A platoon that lost its kit on reload is re-dressed before deployment")
    void kitlessInfantryAreDressedForExtremeTemperatureBeforeDeployment(int temperature) {
        // A scenario's bot forces are saved as MUL data, which keeps a kit's flags but not the kit itself. A platoon
        // that was handed a snow suit or heat suit comes back from a save with no kit, which MegaMek treats as
        // unprotected. This is the state of the platoon the player sees die in the Wolf Brigade report.
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setTemperature(temperature);
        ConvInfantry platoon = new ConvInfantry();

        assertNotNull(conditions.whyDoomed(platoon, new Game()),
              "a platoon with no kit should be doomed at " + temperature + " degrees C");

        AtBDynamicScenarioFactory.ensureInfantryProtection(platoon, conditions);

        String reasonDoomed = conditions.whyDoomed(platoon, new Game());
        assertNull(reasonDoomed,
              "the platoon should survive deployment at " + temperature + " degrees C, but MegaMek still dooms it: " +
                    reasonDoomed);
    }

    @Test
    @DisplayName("Infantry already wearing a kit are not re-dressed before deployment")
    void dressedInfantryAreLeftAloneBeforeDeployment() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setTemperature(MILD_TEMPERATURE);
        ConvInfantry platoon = new ConvInfantry();
        platoon.setArmorKit(MiscType.createSnowSuitInfArmor());
        EquipmentType kitBefore = platoon.getArmorKit();

        AtBDynamicScenarioFactory.ensureInfantryProtection(platoon, conditions);

        assertEquals(kitBefore, platoon.getArmorKit(), "an existing kit should not be replaced");
    }

    @Test
    @DisplayName("Infantry are not dressed before deployment when the weather is mild")
    void infantryAreNotDressedInMildWeatherBeforeDeployment() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setTemperature(MILD_TEMPERATURE);
        ConvInfantry platoon = new ConvInfantry();

        AtBDynamicScenarioFactory.ensureInfantryProtection(platoon, conditions);

        assertNull(platoon.getArmorKit(), "mild weather in breathable air should not issue a kit");
    }

    @Test
    @DisplayName("Re-rolling cold weather to mild strips the snow suit from enemy infantry")
    void rerollingColdToMildRemovesSnowSuit() {
        AtBDynamicScenario scenario = createScenario(AtmosphericTaint.BREATHABLE, Atmosphere.STANDARD);
        ConvInfantry enemyPlatoon = new ConvInfantry();
        AtBDynamicScenarioFactory.changeInfantryKit(enemyPlatoon, false, false, -40);
        assertNotNull(enemyPlatoon.getArmorKit(), "the platoon should start in a snow suit");
        scenario.getBotForces().add(new BotForce("Enemy", 2, 0, List.of(enemyPlatoon)));

        // The re-roll has produced a mild temperature
        scenario.setTemperature(MILD_TEMPERATURE);
        AtBDynamicScenarioFactory.redressInfantryForWeather(scenario, createCampaignOptions());

        assertNull(enemyPlatoon.getArmorKit(), "the snow suit should be removed once the weather is mild");
        assertFalse(enemyPlatoon.isArmorEncumbering(), "the snow suit's encumbering flag should be cleared");
    }

    @Test
    @DisplayName("Re-rolling mild weather to extreme heat issues a heat suit to enemy and allied infantry")
    void rerollingMildToHotIssuesHeatSuit() {
        AtBDynamicScenario scenario = createScenario(AtmosphericTaint.BREATHABLE, Atmosphere.STANDARD);
        ConvInfantry enemyPlatoon = new ConvInfantry();
        ConvInfantry alliedPlatoon = new ConvInfantry();
        AtBDynamicScenarioFactory.changeInfantryKit(enemyPlatoon, false, false, -40);
        AtBDynamicScenarioFactory.changeInfantryKit(alliedPlatoon, false, false, -40);
        scenario.getBotForces().add(new BotForce("Enemy", 2, 0, List.of(enemyPlatoon)));
        scenario.getAlliesPlayer().add(alliedPlatoon);

        scenario.setTemperature(56);
        AtBDynamicScenarioFactory.redressInfantryForWeather(scenario, createCampaignOptions());

        for (ConvInfantry platoon : List.of(enemyPlatoon, alliedPlatoon)) {
            assertEquals(MiscType.createISHeatSuitInfArmor().getInternalName(),
                  platoon.getArmorKit().getInternalName(),
                  "the cold-weather kit should be replaced with a heat suit");
        }
    }

    @Test
    @DisplayName("Re-rolling the weather leaves kits that MekHQ did not issue alone")
    void rerollingWeatherKeepsOtherKits() {
        AtBDynamicScenario scenario = createScenario(AtmosphericTaint.BREATHABLE, Atmosphere.STANDARD);
        ConvInfantry marines = new ConvInfantry();
        marines.setArmorKit(MiscType.createISEnvironmentSuitMarineInfArmor());
        EquipmentType kitBefore = marines.getArmorKit();
        scenario.getBotForces().add(new BotForce("Enemy", 2, 0, List.of(marines)));

        AtBDynamicScenarioFactory.redressInfantryForWeather(scenario, createCampaignOptions());

        assertEquals(kitBefore, marines.getArmorKit(), "a marine suit should survive a weather re-roll");
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
        when(campaignOptions.get(CampaignOption.USE_PLANETARY_MODIFIERS)).thenReturn(true);
        return campaignOptions;
    }
}
