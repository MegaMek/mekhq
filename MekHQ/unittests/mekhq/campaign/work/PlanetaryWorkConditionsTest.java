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
package mekhq.campaign.work;

import static mekhq.campaign.personnel.skills.SkillType.S_ZERO_G_OPERATIONS;
import static mekhq.campaign.unit.Unit.SITE_FACILITY_BASIC;
import static mekhq.campaign.unit.Unit.SITE_FIELD_WORKSHOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.work.PlanetaryWorkConditions.GravityModifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Maintenance and repair under the Planetary Conditions Modifiers table (Campaign Operations p. 193). On Colmar, at
 * 1.27 G, work took the same time as on Terra and repairs had no penalty at all (MekHQ issue 3166).
 */
class PlanetaryWorkConditionsTest {
    private static final LocalDate TODAY = LocalDate.of(3067, 3, 1);
    private static final double COLMAR_GRAVITY = 1.27;
    private static final int MILD_TEMPERATURE = 20;
    private static final int BASE_TARGET = 4;

    private static Campaign campaignOn(double gravity, AtmosphericTaint taint, Atmosphere pressure,
          int temperature) {
        Campaign campaign = mockCampaign();
        when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        when(campaign.getLocalDate()).thenReturn(TODAY);
        Planet planet = mock(Planet.class);
        when(planet.getGravity()).thenReturn(gravity);
        when(planet.getAtmosphere(TODAY)).thenReturn(taint);
        when(planet.getPressure(TODAY)).thenReturn(pressure);
        when(planet.getTemperature(TODAY)).thenReturn(temperature);
        AbstractLocation location = mock(AbstractLocation.class);
        when(location.isOnPlanet()).thenReturn(true);
        when(location.getPlanet()).thenReturn(planet);
        when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation()).thenReturn(location);
        return campaign;
    }

    private static Campaign colmar() {
        return campaignOn(COLMAR_GRAVITY, AtmosphericTaint.BREATHABLE, Atmosphere.STANDARD, MILD_TEMPERATURE);
    }

    private static Campaign colmarWithTaintedAir() {
        return campaignOn(COLMAR_GRAVITY, AtmosphericTaint.TAINTED_POISON, Atmosphere.STANDARD, MILD_TEMPERATURE);
    }

    private static Unit unitAt(int site) {
        Unit unit = mock(Unit.class);
        when(unit.getSite()).thenReturn(site);
        return unit;
    }

    private static int rollFor(Campaign campaign, Unit unit, Person tech) {
        TargetRoll target = new TargetRoll(BASE_TARGET, "skill");
        PlanetaryWorkConditions.addRollModifiers(campaign, unit, tech, target);
        return target.getValue();
    }

    @ParameterizedTest(name = "{0} G")
    @CsvSource(delimiter = ';', value = {
          "0.5; 1; Low Gravity",
          "0.79; 1; Low Gravity",
          "1.21; 2; High Gravity",
          "1.27; 2; High Gravity",
          "1.99; 2; High Gravity",
          "2.0; 4; Very High Gravity",
          "3.5; 4; Very High Gravity" })
    void unusualGravityIsPenalised(double gravity, int expectedModifier, String expectedDescription) {
        GravityModifier gravityModifier = PlanetaryWorkConditions.getGravityModifier(gravity);

        assertEquals(new GravityModifier(expectedModifier, expectedDescription), gravityModifier);
    }

    @ParameterizedTest(name = "{0} G")
    @ValueSource(doubles = { 0.8, 1.0, 1.2 })
    void standardGravityHasNoPenalty(double gravity) {
        assertNull(PlanetaryWorkConditions.getGravityModifier(gravity));
    }

    @Test
    void highGravityAddsHalfTheTime() {
        assertEquals(1.5, PlanetaryWorkConditions.getTimeMultiplier(colmar(), unitAt(SITE_FIELD_WORKSHOP)));
    }

    /** Penalties add up the way salvage recovery's do: 1 + 0.5 for gravity + 0.5 for the air. */
    @Test
    void taintedAirInTheOpenAddsAnotherHalf() {
        assertEquals(2.0, PlanetaryWorkConditions.getTimeMultiplier(colmarWithTaintedAir(), unitAt(SITE_FIELD_WORKSHOP)));
    }

    @Test
    void facilityShelterFromTheAirButNotFromGravity() {
        Campaign campaign = colmarWithTaintedAir();
        Unit unit = unitAt(SITE_FACILITY_BASIC);

        assertEquals(1.5, PlanetaryWorkConditions.getTimeMultiplier(campaign, unit));
        assertEquals(BASE_TARGET + 2, rollFor(campaign, unit, null));
    }

    @Test
    void sparePartInTheWarehouseFeelsGravityOnly() {
        Campaign campaign = colmarWithTaintedAir();

        assertEquals(1.5, PlanetaryWorkConditions.getTimeMultiplier(campaign, null));
        assertEquals(BASE_TARGET + 2, rollFor(campaign, null, null));
    }

    /** +2 high gravity, +2 tainted air, +1 trace pressure, +1 extreme cold. */
    @Test
    void everyConditionInTheOpenAddsToTheRoll() {
        Campaign campaign = campaignOn(COLMAR_GRAVITY, AtmosphericTaint.TAINTED_POISON, Atmosphere.TRACE, -40);

        assertEquals(BASE_TARGET + 6, rollFor(campaign, unitAt(SITE_FIELD_WORKSHOP), null));
    }

    @Test
    void zeroGOperationsOffsetsPartOfTheGravityPenalty() {
        Person tech = mock(Person.class);
        Skill zeroGSkill = mock(Skill.class);
        when(zeroGSkill.getTotalSkillLevel(any())).thenReturn(1);
        when(tech.getSkill(S_ZERO_G_OPERATIONS)).thenReturn(zeroGSkill);

        assertEquals(BASE_TARGET + 1, rollFor(colmar(), unitAt(SITE_FIELD_WORKSHOP), tech));
    }

    @Test
    void nothingAppliesWithThePlanetaryModifiersOptionOff() {
        Campaign campaign = colmarWithTaintedAir();
        campaign.getCampaignOptions().set(CampaignOption.USE_PLANETARY_MODIFIERS, false);

        assertEquals(1.0, PlanetaryWorkConditions.getTimeMultiplier(campaign, unitAt(SITE_FIELD_WORKSHOP)));
        assertEquals(BASE_TARGET, rollFor(campaign, unitAt(SITE_FIELD_WORKSHOP), null));
    }

    @Test
    void nothingAppliesInTransit() {
        Campaign campaign = colmarWithTaintedAir();
        when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation().isOnPlanet()).thenReturn(false);

        assertEquals(1.0, PlanetaryWorkConditions.getTimeMultiplier(campaign, unitAt(SITE_FIELD_WORKSHOP)));
        assertEquals(BASE_TARGET, rollFor(campaign, unitAt(SITE_FIELD_WORKSHOP), null));
    }

    @Test
    void standardWorldChangesNothing() {
        Campaign campaign = campaignOn(1.0, AtmosphericTaint.BREATHABLE, Atmosphere.STANDARD, MILD_TEMPERATURE);

        assertEquals(1.0, PlanetaryWorkConditions.getTimeMultiplier(campaign, unitAt(SITE_FIELD_WORKSHOP)));
        assertEquals(BASE_TARGET, rollFor(campaign, unitAt(SITE_FIELD_WORKSHOP), null));
    }

    @Test
    void partWithNoCampaignTakesTheNormalTime() {
        assertEquals(1.0, PlanetaryWorkConditions.getTimeMultiplier(null, null));
    }
}
