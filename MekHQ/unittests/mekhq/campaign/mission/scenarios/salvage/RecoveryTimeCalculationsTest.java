package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.universe.Planet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link RecoveryTimeCalculations}, covering each environmental multiplier (CamOps 5th printing pg 193 &
 * pg 209), how they combine, and the special handling of space scenarios.
 *
 * @author Illiani
 * @since 0.51.01
 */
class RecoveryTimeCalculationsTest {
    private static final String ENTITY_NAME = "Test Unit";
    private static final int BASE_RECOVERY_TIME = 100;
    private static final double DELTA = 0.0001;

    /**
     * Creates a ground scenario with benign conditions, so each test only needs to change the condition it cares
     * about.
     */
    private static Scenario benignGroundScenario() {
        Scenario scenario = mock(Scenario.class);
        when(scenario.getBoardType()).thenReturn(Scenario.T_GROUND);
        when(scenario.getWeather()).thenReturn(Weather.CLEAR);
        when(scenario.getWind()).thenReturn(Wind.CALM);
        when(scenario.getTemperature()).thenReturn(25);
        when(scenario.getGravity()).thenReturn(1.0f);
        when(scenario.getAtmosphere()).thenReturn(Atmosphere.STANDARD);
        when(scenario.getLight()).thenReturn(Light.DAY);
        when(scenario.getDate()).thenReturn(LocalDate.of(3051, 1, 1));
        return scenario;
    }

    private static Planet planetWithAtmosphere(AtmosphericTaint atmosphericTaint) {
        Planet planet = mock(Planet.class);
        when(planet.getAtmosphere(any())).thenReturn(atmosphericTaint);
        return planet;
    }

    private static RecoveryTimeData calculate(Scenario scenario) {
        return RecoveryTimeCalculations.calculateRecoveryTimeForEntity(ENTITY_NAME, BASE_RECOVERY_TIME, false,
              scenario, planetWithAtmosphere(AtmosphericTaint.BREATHABLE));
    }

    @Test
    void benignGroundConditionsLeaveRecoveryTimeUnchanged() {
        RecoveryTimeData data = calculate(benignGroundScenario());

        assertEquals(BASE_RECOVERY_TIME, data.totalRecoveryTime());
        assertEquals(0.0, data.weatherMultiplier(), DELTA);
        assertEquals(0.0, data.windMultiplier(), DELTA);
        assertEquals(0.0, data.temperatureMultiplier(), DELTA);
        assertEquals(0.0, data.gravityMultiplier(), DELTA);
        assertEquals(0.0, data.atmosphereMultiplier(), DELTA);
        assertEquals(0.0, data.lightMultiplier(), DELTA);
    }

    @Test
    void nonAerospaceSalvageInSpaceAppliesZeroGravityAndVacuum() {
        Scenario scenario = benignGroundScenario();
        when(scenario.getBoardType()).thenReturn(Scenario.T_SPACE);
        // Hostile planetary conditions must be ignored in space
        when(scenario.getWeather()).thenReturn(Weather.LIGHTNING_STORM);
        when(scenario.getLight()).thenReturn(Light.PITCH_BLACK);

        RecoveryTimeData data = calculate(scenario);

        assertEquals(0.5, data.gravityMultiplier(), DELTA);
        assertEquals(0.5, data.atmosphereMultiplier(), DELTA);
        assertEquals(0.0, data.weatherMultiplier(), DELTA);
        assertEquals(0.0, data.lightMultiplier(), DELTA);
        assertEquals(BASE_RECOVERY_TIME * 2, data.totalRecoveryTime());
    }

    @Test
    void aerospaceUnitsInSpaceIgnoreEnvironmentalMultipliers() {
        Scenario scenario = benignGroundScenario();
        when(scenario.getBoardType()).thenReturn(Scenario.T_SPACE);

        RecoveryTimeData data = RecoveryTimeCalculations.calculateRecoveryTimeForEntity(ENTITY_NAME,
              BASE_RECOVERY_TIME, true, scenario, planetWithAtmosphere(AtmosphericTaint.TAINTED_POISON));

        assertEquals(0.0, data.gravityMultiplier(), DELTA);
        assertEquals(0.0, data.atmosphereMultiplier(), DELTA);
        assertEquals(BASE_RECOVERY_TIME, data.totalRecoveryTime());
    }

    @Test
    void aerospaceUnitsOnTheGroundUseEnvironmentalMultipliers() {
        Scenario scenario = benignGroundScenario();
        when(scenario.getLight()).thenReturn(Light.PITCH_BLACK); // +0.5

        RecoveryTimeData data = RecoveryTimeCalculations.calculateRecoveryTimeForEntity(ENTITY_NAME,
              BASE_RECOVERY_TIME, true, scenario, planetWithAtmosphere(AtmosphericTaint.BREATHABLE));

        assertEquals(150, data.totalRecoveryTime());
    }

    @ParameterizedTest
    @CsvSource({
          "CALM, 0.0",
          "LIGHT_GALE, 0.0",
          "MOD_GALE, 0.0",
          "STRONG_GALE, 0.25",
          "STORM, 0.0",
          "TORNADO_F1_TO_F3, 0.5",
          "TORNADO_F4, 0.5"
    })
    void windMultiplier(Wind wind, double expectedMultiplier) {
        Scenario scenario = benignGroundScenario();
        when(scenario.getWind()).thenReturn(wind);

        RecoveryTimeData data = calculate(scenario);

        assertEquals(expectedMultiplier, data.windMultiplier(), DELTA);
        assertEquals((int) Math.round(BASE_RECOVERY_TIME * (1.0 + expectedMultiplier)), data.totalRecoveryTime());
    }

    @ParameterizedTest
    @CsvSource({
          "CLEAR, 0.0",
          "HEAVY_RAIN, 0.0",
          "SLEET, 0.0",
          "DOWNPOUR, 0.25",
          "HEAVY_SNOW, 0.25",
          "ICE_STORM, 0.25",
          "LIGHTNING_STORM, 0.25"
    })
    void weatherMultiplier(Weather weather, double expectedMultiplier) {
        Scenario scenario = benignGroundScenario();
        when(scenario.getWeather()).thenReturn(weather);

        assertEquals(expectedMultiplier, calculate(scenario).weatherMultiplier(), DELTA);
    }

    @ParameterizedTest
    @CsvSource({
          "0.0, 0.5",
          "0.79, 0.25",
          "0.8, 0.0",
          "1.2, 0.0",
          "1.21, 0.5",
          "1.99, 0.5",
          "2.0, 1.0"
    })
    void gravityMultiplierBoundaries(float gravity, double expectedMultiplier) {
        Scenario scenario = benignGroundScenario();
        when(scenario.getGravity()).thenReturn(gravity);

        assertEquals(expectedMultiplier, calculate(scenario).gravityMultiplier(), DELTA);
    }

    @ParameterizedTest
    @CsvSource({
          "-31, 0.25",
          "-30, 0.0",
          "50, 0.0",
          "51, 0.25"
    })
    void temperatureMultiplierBoundaries(int temperature, double expectedMultiplier) {
        Scenario scenario = benignGroundScenario();
        when(scenario.getTemperature()).thenReturn(temperature);

        assertEquals(expectedMultiplier, calculate(scenario).temperatureMultiplier(), DELTA);
    }

    @ParameterizedTest
    @CsvSource({
          "VACUUM, 0.5",
          "TRACE, 0.25",
          "THIN, 0.0",
          "STANDARD, 0.0",
          "HIGH, 0.0",
          "VERY_HIGH, 0.25"
    })
    void atmosphereMultiplier(Atmosphere atmosphere, double expectedMultiplier) {
        Scenario scenario = benignGroundScenario();
        when(scenario.getAtmosphere()).thenReturn(atmosphere);

        assertEquals(expectedMultiplier, calculate(scenario).atmosphereMultiplier(), DELTA);
    }

    @Test
    void taintedPlanetaryAtmosphereOverridesLesserScenarioAtmosphere() {
        Scenario scenario = benignGroundScenario();
        when(scenario.getAtmosphere()).thenReturn(Atmosphere.TRACE);

        RecoveryTimeData data = RecoveryTimeCalculations.calculateRecoveryTimeForEntity(ENTITY_NAME,
              BASE_RECOVERY_TIME, false, scenario, planetWithAtmosphere(AtmosphericTaint.TAINTED_POISON));

        assertEquals(0.5, data.atmosphereMultiplier(), DELTA);
    }

    @ParameterizedTest
    @CsvSource({
          "BREATHABLE, 0.0",
          "TAINTED_CAUSTIC, 0.5",
          "TAINTED_POISON, 0.5",
          "TAINTED_FLAME, 0.5",
          "TOXIC_CAUSTIC, 0.5",
          "TOXIC_POISON, 0.5",
          "TOXIC_FLAME, 0.5"
    })
    void taintedAndToxicPlanetaryAtmospheresShareTheTaintedMultiplier(AtmosphericTaint atmosphericTaint,
          double expectedMultiplier) {
        RecoveryTimeData data = RecoveryTimeCalculations.calculateRecoveryTimeForEntity(ENTITY_NAME,
              BASE_RECOVERY_TIME, false, benignGroundScenario(), planetWithAtmosphere(atmosphericTaint));

        assertEquals(expectedMultiplier, data.atmosphereMultiplier(), DELTA);
    }

    @Test
    void missingPlanetDoesNotApplyAtmosphericTaint() {
        RecoveryTimeData data = RecoveryTimeCalculations.calculateRecoveryTimeForEntity(ENTITY_NAME,
              BASE_RECOVERY_TIME, false, benignGroundScenario(), null);

        assertEquals(0.0, data.atmosphereMultiplier(), DELTA);
        assertEquals(BASE_RECOVERY_TIME, data.totalRecoveryTime());
    }

    @ParameterizedTest
    @CsvSource({
          "DAY, 0.0",
          "DUSK, 0.0",
          "FULL_MOON, 0.0",
          "MOONLESS, 0.25",
          "SOLAR_FLARE, 0.25",
          "PITCH_BLACK, 0.5"
    })
    void lightMultiplier(Light light, double expectedMultiplier) {
        Scenario scenario = benignGroundScenario();
        when(scenario.getLight()).thenReturn(light);

        assertEquals(expectedMultiplier, calculate(scenario).lightMultiplier(), DELTA);
    }

    @Test
    void nullConditionsApplyNoMultiplier() {
        Scenario scenario = benignGroundScenario();
        when(scenario.getWeather()).thenReturn(null);
        when(scenario.getWind()).thenReturn(null);
        when(scenario.getAtmosphere()).thenReturn(null);
        when(scenario.getLight()).thenReturn(null);

        assertEquals(BASE_RECOVERY_TIME, calculate(scenario).totalRecoveryTime());
    }

    @Test
    void multipliersAreAdditive() {
        Scenario scenario = benignGroundScenario();
        when(scenario.getWeather()).thenReturn(Weather.DOWNPOUR); // +0.25
        when(scenario.getLight()).thenReturn(Light.PITCH_BLACK); // +0.5
        when(scenario.getGravity()).thenReturn(2.5f); // +1.0

        assertEquals(275, calculate(scenario).totalRecoveryTime());
    }

    @Test
    void totalRecoveryTimeIsRounded() {
        Scenario scenario = benignGroundScenario();
        when(scenario.getWind()).thenReturn(Wind.STRONG_GALE); // x1.25

        RecoveryTimeData data = RecoveryTimeCalculations.calculateRecoveryTimeForEntity(ENTITY_NAME, 10, false,
              scenario, null);

        assertEquals(13, data.totalRecoveryTime()); // 12.5 rounds up
    }

    @Test
    void overflowingRecoveryTimeIsCappedAtMaximumInteger() {
        Scenario scenario = benignGroundScenario();
        when(scenario.getBoardType()).thenReturn(Scenario.T_SPACE); // x2.0

        RecoveryTimeData data = RecoveryTimeCalculations.calculateRecoveryTimeForEntity(ENTITY_NAME,
              Integer.MAX_VALUE, false, scenario, null);

        assertEquals(Integer.MAX_VALUE, data.totalRecoveryTime());
    }
}
