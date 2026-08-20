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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile.HABITABLE_TEMPERATURE_CELSIUS;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile.NEUTRAL_GRAVITY;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile.NEUTRAL_LANDMASS_COUNT;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile.NEUTRAL_TEMPERATURE_CELSIUS;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile.NEUTRAL_WATER_PERCENT;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile.TERRA_DIAMETER_KM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Atmosphere;
import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.SourceableValue;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PlanetProfile}: the derived planetary values that drive improved sector generation (size factor,
 * habitability, population scale, tech level, composition and atmosphere reads), and - most importantly - the neutral
 * fallbacks {@link PlanetProfile#from} substitutes for the many universe-data fields real planets simply do not
 * record.
 */
class PlanetProfileTest {

    private static final LocalDate DATE = LocalDate.of(3151, 1, 1);
    private static final double TOLERANCE = 1e-9;

    /**
     * Builds a profile that varies only in the fields a test cares about, keeping everything else Terra-like.
     */
    private static PlanetProfile profile(int temperatureCelsius, double diameterKm, boolean airless,
          Atmosphere atmosphere, String composition, Long population, HPGRating hpg) {
        return new PlanetProfile(temperatureCelsius,
              diameterKm,
              NEUTRAL_WATER_PERCENT,
              airless,
              atmosphere,
              composition,
              NEUTRAL_LANDMASS_COUNT,
              NEUTRAL_GRAVITY,
              population,
              hpg);
    }

    private static PlanetProfile temperate(int temperatureCelsius) {
        return profile(temperatureCelsius, TERRA_DIAMETER_KM, false, null, "", null, HPGRating.X);
    }

    // region sizeFactor()

    @Test
    void sizeFactor_terraSizedPlanet_isOne() {
        assertEquals(1.0,
              profile(25, TERRA_DIAMETER_KM, false, null, "", null, HPGRating.X).sizeFactor(),
              TOLERANCE,
              "a Terra-diameter planet should scale sectors by exactly 1.0");
    }

    @Test
    void sizeFactor_unknownDiameter_fallsBackToTerraSized() {
        assertEquals(1.0,
              profile(25, 0.0, false, null, "", null, HPGRating.X).sizeFactor(),
              TOLERANCE,
              "a planet with no recorded diameter should be treated as Terra-sized");
        assertEquals(1.0,
              profile(25, -500.0, false, null, "", null, HPGRating.X).sizeFactor(),
              TOLERANCE,
              "a nonsensical negative diameter should be treated as Terra-sized, not scaled");
    }

    @Test
    void sizeFactor_scalesLinearlyBetweenTheClamps() {
        assertEquals(0.75,
              profile(25, TERRA_DIAMETER_KM * 0.75, false, null, "", null, HPGRating.X).sizeFactor(),
              TOLERANCE,
              "a three-quarter-diameter planet should scale sectors by 0.75");
        assertEquals(1.5,
              profile(25, TERRA_DIAMETER_KM * 1.5, false, null, "", null, HPGRating.X).sizeFactor(),
              TOLERANCE,
              "a one-and-a-half-diameter planet should scale sectors by 1.5");
    }

    @Test
    void sizeFactor_isClampedAtBothEnds() {
        assertEquals(0.5,
              profile(25, TERRA_DIAMETER_KM * 0.01, false, null, "", null, HPGRating.X).sizeFactor(),
              TOLERANCE,
              "a tiny world should clamp to the 0.5 minimum size factor");
        assertEquals(2.0,
              profile(25, TERRA_DIAMETER_KM * 50, false, null, "", null, HPGRating.X).sizeFactor(),
              TOLERANCE,
              "a huge world should clamp to the 2.0 maximum size factor");
    }

    // endregion sizeFactor()

    // region habitability()

    @Test
    void habitability_airlessWorld_isExactlyZero() {
        PlanetProfile airless = profile(HABITABLE_TEMPERATURE_CELSIUS,
              TERRA_DIAMETER_KM,
              true,
              null,
              "",
              null,
              HPGRating.X);

        assertEquals(0.0,
              airless.habitability(),
              TOLERANCE,
              "an airless world must be exactly zero habitability regardless of its temperature");
    }

    @Test
    void habitability_isNeverNegativeAcrossTheWholeTemperatureRange() {
        for (int temperature = -200; temperature <= 200; temperature++) {
            assertTrue(temperate(temperature).habitability() >= 0.0,
                  "habitability went negative at " + temperature + "C");
        }
    }

    @Test
    void habitability_isSymmetricAboutTheIdealTemperature() {
        // Only the absolute distance from the ideal temperature matters, so equally hot and equally cold worlds are
        // equally habitable. This is what lets a single sector temperature drive both hemispheres.
        for (int offset = 1; offset <= 150; offset++) {
            double warmer = temperate(HABITABLE_TEMPERATURE_CELSIUS + offset).habitability();
            double colder = temperate(HABITABLE_TEMPERATURE_CELSIUS - offset).habitability();

            assertEquals(warmer,
                  colder,
                  TOLERANCE,
                  "habitability was asymmetric " + offset + "C either side of the ideal temperature");
        }
    }

    @Test
    void habitability_peaksAtTheIdealTemperatureAndFallsOffToZeroAtOneSpan() {
        // Guards a regression that shipped once: computing "1 - (SPAN / distance)" instead of "1 - (distance / SPAN)"
        // divides by a zero distance at the ideal temperature, so every temperate world scored 0.0 and comfort rose
        // with extremity. That silently disabled everything keyed off habitability, volcanism damping included.
        assertEquals(1.0,
              temperate(HABITABLE_TEMPERATURE_CELSIUS).habitability(),
              TOLERANCE,
              "the ideal temperature should be maximally habitable");
        assertEquals(0.5,
              temperate(HABITABLE_TEMPERATURE_CELSIUS + 30).habitability(),
              TOLERANCE,
              "half a habitable span from ideal should be half as comfortable");
        assertEquals(0.0,
              temperate(HABITABLE_TEMPERATURE_CELSIUS + 60).habitability(),
              TOLERANCE,
              "a world a full habitable span from ideal should be uninhabitable");
        assertEquals(0.0,
              temperate(HABITABLE_TEMPERATURE_CELSIUS + 120).habitability(),
              TOLERANCE,
              "comfort must not rise again beyond a full span");
    }

    @Test
    void habitability_taintedAtmosphere_isReducedMoreThanNonBreathable() {
        // Half a habitable span from ideal, so the base comfort is a clean 0.5 and the atmosphere factors are visible
        // in the result. It must stay inside one span: a full span away the base is 0.0 and every factor multiplies to
        // zero, which would make the comparison below vacuous.
        int uncomfortable = HABITABLE_TEMPERATURE_CELSIUS + 30;
        double base = temperate(uncomfortable).habitability();

        PlanetProfile tainted = profile(uncomfortable, TERRA_DIAMETER_KM, false, Atmosphere.TAINTED_POISON, "", null,
              HPGRating.X);
        PlanetProfile toxic = profile(uncomfortable, TERRA_DIAMETER_KM, false, Atmosphere.TOXIC_CAUSTIC, "", null,
              HPGRating.X);
        PlanetProfile thin = profile(uncomfortable, TERRA_DIAMETER_KM, false, Atmosphere.NONE, "", null, HPGRating.X);

        assertEquals(base * 0.4,
              tainted.habitability(),
              TOLERANCE,
              "a tainted atmosphere should cut habitability to 40%");
        assertEquals(base * 0.4,
              toxic.habitability(),
              TOLERANCE,
              "a toxic atmosphere should cut habitability to 40%, same as tainted");
        assertEquals(base * 0.6,
              thin.habitability(),
              TOLERANCE,
              "a merely non-breathable atmosphere should cut habitability to 60%");
        assertTrue(tainted.habitability() < thin.habitability(),
              "a tainted atmosphere should be harsher than a simply non-breathable one");
    }

    @Test
    void habitability_breathableAtmosphere_isNotPenalized() {
        int uncomfortable = HABITABLE_TEMPERATURE_CELSIUS + 30;

        assertEquals(temperate(uncomfortable).habitability(),
              profile(uncomfortable, TERRA_DIAMETER_KM, false, Atmosphere.BREATHABLE, "", null,
                    HPGRating.X).habitability(),
              TOLERANCE,
              "an explicitly breathable atmosphere should behave the same as an unknown (neutral) one");
    }

    // endregion habitability()

    // region populationLog()

    @Test
    void populationLog_convertsPopulationToItsOrderOfMagnitude() {
        assertEquals(6.0,
              profile(25, TERRA_DIAMETER_KM, false, null, "", 1_000_000L, HPGRating.X).populationLog(),
              TOLERANCE,
              "a million people should read as 6");
        assertEquals(9.0,
              profile(25, TERRA_DIAMETER_KM, false, null, "", 1_000_000_000L, HPGRating.X).populationLog(),
              TOLERANCE,
              "a billion people should read as 9");
    }

    @Test
    void populationLog_unknownOrEmptyPopulation_isZero() {
        assertEquals(0.0,
              profile(25, TERRA_DIAMETER_KM, false, null, "", null, HPGRating.X).populationLog(),
              TOLERANCE,
              "an unknown population should read as 0, not NaN");
        assertEquals(0.0,
              profile(25, TERRA_DIAMETER_KM, false, null, "", 0L, HPGRating.X).populationLog(),
              TOLERANCE,
              "an uninhabited world should read as 0, not negative infinity");
        assertEquals(0.0,
              profile(25, TERRA_DIAMETER_KM, false, null, "", -5L, HPGRating.X).populationLog(),
              TOLERANCE,
              "a nonsensical negative population should read as 0, not NaN");
    }

    // endregion populationLog()

    // region techLevel()

    @Test
    void techLevel_spansZeroToOneAcrossTheHpgRatings() {
        assertEquals(0.0,
              profile(25, TERRA_DIAMETER_KM, false, null, "", null, HPGRating.X).techLevel(),
              TOLERANCE,
              "no HPG should be the bottom of the tech scale");
        assertEquals(1.0,
              profile(25, TERRA_DIAMETER_KM, false, null, "", null, HPGRating.A).techLevel(),
              TOLERANCE,
              "an A-rated HPG should be the top of the tech scale");
        assertEquals(0.75,
              profile(25, TERRA_DIAMETER_KM, false, null, "", null, HPGRating.B).techLevel(),
              TOLERANCE,
              "a B-rated HPG should sit three quarters up the tech scale");
    }

    @Test
    void techLevel_isMonotonicAcrossTheRatings() {
        double previous = -1.0;
        for (HPGRating rating : HPGRating.values()) {
            double techLevel = profile(25, TERRA_DIAMETER_KM, false, null, "", null, rating).techLevel();
            assertTrue(techLevel > previous, rating + " did not increase the tech level");
            assertTrue((techLevel >= 0.0) && (techLevel <= 1.0), rating + " produced a tech level outside 0..1");
            previous = techLevel;
        }
    }

    // endregion techLevel()

    // region composition and atmosphere reads

    @Test
    void compositionPredicates_matchTheirKeywordsAnywhereInTheString() {
        assertTrue(profile(25, TERRA_DIAMETER_KM, false, null, "ice", null, HPGRating.X).hasIcyComposition(),
              "\"ice\" should read as icy");
        assertTrue(profile(25, TERRA_DIAMETER_KM, false, null, "frozen rock", null, HPGRating.X).hasIcyComposition(),
              "\"frozen rock\" should read as icy");
        assertTrue(profile(25, TERRA_DIAMETER_KM, false, null, "arid/rock", null, HPGRating.X).hasRockyComposition(),
              "\"arid/rock\" should read as rocky");
        assertTrue(profile(25, TERRA_DIAMETER_KM, false, null, "desert", null, HPGRating.X).hasRockyComposition(),
              "\"desert\" should read as rocky");
    }

    @Test
    void compositionPredicates_unknownComposition_matchNothing() {
        PlanetProfile unknown = profile(25, TERRA_DIAMETER_KM, false, null, "", null, HPGRating.X);

        assertFalse(unknown.hasIcyComposition(), "an unrecorded composition must not read as icy");
        assertFalse(unknown.hasRockyComposition(), "an unrecorded composition must not read as rocky");
    }

    @Test
    void breathable_unknownAtmosphere_isTreatedAsBreathable() {
        // Most universe entries record no atmosphere at all. Treating that as breathable is what keeps ordinary
        // worlds from generating as lifeless rock.
        assertTrue(temperate(25).breathable(), "an unknown atmosphere should be treated as breathable");
        assertFalse(temperate(25).taintedOrToxic(), "an unknown atmosphere must not read as tainted or toxic");
    }

    @Test
    void breathable_airlessWorldIsNeverBreathable() {
        PlanetProfile airless = profile(25, TERRA_DIAMETER_KM, true, Atmosphere.BREATHABLE, "", null, HPGRating.X);

        assertFalse(airless.breathable(),
              "an airless world must never be breathable, even if an atmosphere is somehow recorded");
    }

    @Test
    void taintedOrToxic_coversBothTaintedAndToxicVariants() {
        for (Atmosphere atmosphere : List.of(Atmosphere.TAINTED_POISON,
              Atmosphere.TAINTED_CAUSTIC,
              Atmosphere.TAINTED_FLAME,
              Atmosphere.TOXIC_POISON,
              Atmosphere.TOXIC_CAUSTIC,
              Atmosphere.TOXIC_FLAME)) {
            PlanetProfile hostile = profile(25, TERRA_DIAMETER_KM, false, atmosphere, "", null, HPGRating.X);

            assertTrue(hostile.taintedOrToxic(), atmosphere + " should read as tainted or toxic");
            assertFalse(hostile.breathable(), atmosphere + " must not read as breathable");
        }

        assertFalse(profile(25, TERRA_DIAMETER_KM, false, Atmosphere.BREATHABLE, "", null, HPGRating.X)
                          .taintedOrToxic(), "a breathable atmosphere must not read as tainted or toxic");
        assertFalse(profile(25, TERRA_DIAMETER_KM, false, Atmosphere.NONE, "", null, HPGRating.X).taintedOrToxic(),
              "a \"None\" atmosphere is absent, not tainted");
    }

    // endregion composition and atmosphere reads

    // region neutral()

    @Test
    void neutral_usesEveryNeutralDefaultAndTheGivenTemperature() {
        PlanetProfile neutral = PlanetProfile.neutral(-30);

        assertEquals(-30, neutral.temperatureCelsius(), "neutral() should keep the caller's temperature");
        assertEquals(TERRA_DIAMETER_KM, neutral.diameterKm(), TOLERANCE, "neutral() should be Terra-sized");
        assertEquals(NEUTRAL_WATER_PERCENT, neutral.waterPercent(), "neutral() should use the neutral water coverage");
        assertFalse(neutral.airless(), "neutral() must not be airless");
        assertNull(neutral.atmosphere(), "neutral() should record no atmosphere");
        assertEquals("", neutral.composition(), "neutral() should record no composition");
        assertEquals(NEUTRAL_LANDMASS_COUNT,
              neutral.landmassCount(),
              "neutral() should use the neutral landmass count");
        assertEquals(NEUTRAL_GRAVITY, neutral.gravity(), TOLERANCE, "neutral() should use the neutral gravity");
        assertNull(neutral.population(), "neutral() should record no population");
        assertSame(HPGRating.X, neutral.hpg(), "neutral() should record no HPG");
    }

    // endregion neutral()

    // region from()

    /**
     * A planet that records nothing at all. The sourced getters are left to Mockito's {@code null} default, but the
     * boxed scalars must be stubbed explicitly: Mockito hands back {@code 0} rather than {@code null} for wrapper-typed
     * returns, which would otherwise look like a planet that really does record a value.
     */
    private static Planet emptyPlanet() {
        Planet planet = mock(Planet.class);
        when(planet.getTemperature(DATE)).thenReturn(null);
        when(planet.getGravity()).thenReturn(null);
        when(planet.getPopulation(DATE)).thenReturn(null);
        when(planet.getHPG(DATE)).thenReturn(null);
        return planet;
    }

    @Test
    void from_planetRecordingNothing_usesEveryNeutralFallback() {
        // Most planets in play record only a handful of these fields, so the fallbacks below are the values improved
        // sector generation actually runs on.
        PlanetProfile resolved = PlanetProfile.from(emptyPlanet(), DATE);

        assertEquals(NEUTRAL_TEMPERATURE_CELSIUS,
              resolved.temperatureCelsius(),
              "an unrecorded temperature should fall back to the neutral room temperature");
        assertEquals(NEUTRAL_WATER_PERCENT,
              resolved.waterPercent(),
              "unrecorded surface water should fall back to the neutral 50%");
        assertEquals(NEUTRAL_GRAVITY,
              resolved.gravity(),
              TOLERANCE,
              "unrecorded gravity should fall back to 1G");
        assertEquals(NEUTRAL_LANDMASS_COUNT,
              resolved.landmassCount(),
              "an unrecorded landmass list should fall back to a single landmass");
        assertEquals("", resolved.composition(), "an unrecorded composition should resolve to an empty string");
        assertNull(resolved.atmosphere(), "an unrecorded atmosphere should stay null rather than becoming NONE");
        assertSame(HPGRating.X, resolved.hpg(), "an unrecorded HPG should fall back to X");
        assertEquals(1.0, resolved.sizeFactor(), TOLERANCE, "an unrecorded diameter should scale sectors by 1.0");
    }

    @Test
    void from_planetRecordingNoAtmosphere_isBreathableRatherThanAirless() {
        // getAtmosphere() collapses "unknown" onto NONE; the profile deliberately reads the sourced value so that an
        // absent datum does not turn an ordinary world into a vacuum.
        PlanetProfile resolved = PlanetProfile.from(emptyPlanet(), DATE);

        assertFalse(resolved.airless(), "a planet with no atmosphere datum must not be treated as airless");
        assertTrue(resolved.breathable(), "a planet with no atmosphere datum should be treated as breathable");
    }

    @Test
    void from_vacuumPressure_isAirless() {
        Planet planet = mock(Planet.class);
        when(planet.getPressure(DATE)).thenReturn(megamek.common.planetaryConditions.Atmosphere.VACUUM);

        PlanetProfile resolved = PlanetProfile.from(planet, DATE);

        assertTrue(resolved.airless(), "a vacuum pressure should make the profile airless");
        assertEquals(0.0, resolved.habitability(), TOLERANCE, "an airless profile should have zero habitability");
    }

    @Test
    void from_explicitNoneAtmosphere_isAirless() {
        Planet planet = mock(Planet.class);
        when(planet.getSourcedAtmosphere(DATE)).thenReturn(SourceableValue.of(Atmosphere.NONE));

        assertTrue(PlanetProfile.from(planet, DATE).airless(),
              "an explicitly recorded \"None\" atmosphere should make the profile airless");
    }

    @Test
    void from_readsAndNormalizesEveryRecordedValue() {
        Planet planet = mock(Planet.class);
        when(planet.getTemperature(DATE)).thenReturn(-12);
        when(planet.getDiameter()).thenReturn(TERRA_DIAMETER_KM * 1.25);
        when(planet.getSourcedPercentWater(DATE)).thenReturn(SourceableValue.of(73));
        when(planet.getSourcedAtmosphere(DATE)).thenReturn(SourceableValue.of(Atmosphere.BREATHABLE));
        when(planet.getSourcedComposition(DATE)).thenReturn(SourceableValue.of("Arid/ROCK"));
        when(planet.getLandMasses()).thenReturn(List.of(mock(LandMass.class), mock(LandMass.class)));
        when(planet.getGravity()).thenReturn(1.4);
        when(planet.getPopulation(DATE)).thenReturn(1_000_000_000L);
        when(planet.getHPG(DATE)).thenReturn(HPGRating.A);

        PlanetProfile resolved = PlanetProfile.from(planet, DATE);

        assertEquals(-12, resolved.temperatureCelsius(), "the recorded temperature should be used as-is");
        assertEquals(1.25, resolved.sizeFactor(), TOLERANCE, "the recorded diameter should drive the size factor");
        assertEquals(73, resolved.waterPercent(), "the recorded water coverage should be used as-is");
        assertSame(Atmosphere.BREATHABLE, resolved.atmosphere(), "the recorded atmosphere should be used as-is");
        assertEquals("arid/rock",
              resolved.composition(),
              "the recorded composition should be lower-cased so the keyword predicates match");
        assertTrue(resolved.hasRockyComposition(), "\"Arid/ROCK\" should read as rocky after normalization");
        assertEquals(2, resolved.landmassCount(), "the recorded landmasses should be counted");
        assertEquals(1.4, resolved.gravity(), TOLERANCE, "the recorded gravity should be used as-is");
        assertEquals(9.0, resolved.populationLog(), TOLERANCE, "a billion people should read as 9");
        assertSame(HPGRating.A, resolved.hpg(), "the recorded HPG should be used as-is");
    }

    @Test
    void from_outOfRangeWaterCoverage_isClampedToPercent() {
        Planet tooWet = mock(Planet.class);
        when(tooWet.getSourcedPercentWater(DATE)).thenReturn(SourceableValue.of(180));
        Planet tooDry = mock(Planet.class);
        when(tooDry.getSourcedPercentWater(DATE)).thenReturn(SourceableValue.of(-40));

        assertEquals(100, PlanetProfile.from(tooWet, DATE).waterPercent(), "water coverage should clamp at 100%");
        assertEquals(0, PlanetProfile.from(tooDry, DATE).waterPercent(), "water coverage should clamp at 0%");
    }

    @Test
    void from_emptyLandmassList_stillYieldsOneLandmass() {
        Planet planet = mock(Planet.class);
        when(planet.getLandMasses()).thenReturn(List.of());

        assertEquals(NEUTRAL_LANDMASS_COUNT,
              PlanetProfile.from(planet, DATE).landmassCount(),
              "an empty landmass list should still resolve to a single landmass so generation has land to work with");
    }

    @Test
    void from_contract_readsTheContractsTargetPlanet() {
        Planet planet = mock(Planet.class);
        when(planet.getTemperature(DATE)).thenReturn(-40);

        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getTargetPlanet()).thenReturn(planet);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(DATE);

        assertEquals(-40,
              PlanetProfile.from(contract, campaign).temperatureCelsius(),
              "the profile should come from the contract's target planet");
    }

    /**
     * A contract targets a specific world, not merely a system, so the profile is read from the target planet alone. A
     * contract that names a destination system but no world inside it is still unprofiled - the system's primary world
     * is not substituted, since the fighting may well be somewhere else in the system.
     */
    @Test
    void from_contractWithASystemButNoTargetPlanet_fallsBackToANeutralProfile() {
        Planet primaryPlanet = mock(Planet.class);
        when(primaryPlanet.getTemperature(DATE)).thenReturn(-40);

        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getPrimaryPlanet()).thenReturn(primaryPlanet);

        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getTargetSystem()).thenReturn(system);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(DATE);

        assertEquals(PlanetProfile.neutral(NEUTRAL_TEMPERATURE_CELSIUS),
              PlanetProfile.from(contract, campaign),
              "a contract with no target planet should yield a neutral profile rather than the system's primary "
                    + "world");
    }

    @Test
    void from_contractWithNoTargetPlanet_fallsBackToANeutralProfile() {
        AbstractContract contract = mock(AbstractContract.class);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(DATE);

        assertEquals(PlanetProfile.neutral(NEUTRAL_TEMPERATURE_CELSIUS),
              PlanetProfile.from(contract, campaign),
              "a contract with no destination should yield a fully neutral profile rather than throwing");
    }

    // endregion from()
}
