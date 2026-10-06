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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.OffBoardDirection;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.SynchronizedDeploymentType;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.scenarios.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.scenarios.ScenarioObjective.TimeLimitType;
import mekhq.campaign.mission.scenarios.ScenarioTemplate.BattlefieldControlType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Serialization safety net for {@link ScenarioTemplate}. This class establishes the shared test harness (Phase 0.1): it
 * locates the shipped scenario template corpus copied into {@code testresources} and confirms every file deserializes.
 * The round-trip idempotence and cross-format equivalence assertions build on this plumbing in later sub-phases.
 */
class ScenarioTemplateSerializationTest {

    /**
     * Location of the scenario template corpus within the module. Mirrors the {@code testresources}-relative resolution
     * used by other data-file tests (e.g. {@code PlanetarySystemYamlIOTest}); Gradle runs tests from the module
     * directory, so this relative path resolves against {@code mekhq/MekHQ/}.
     */
    private static final Path CORPUS_DIRECTORY = Path.of("testresources", "data", "scenariotemplates");

    /**
     * The manifest file is not a scenario template and must never be part of the corpus. It is excluded when the
     * fixtures are staged, but is guarded here as well so an accidental copy fails loudly.
     */
    private static final String SCENARIO_MANIFEST_FILE_NAME = "ScenarioManifest.json";

    /**
     * Provides every shipped scenario template file in the corpus, sorted for stable ordering.
     *
     * @return a stream of paths to the {@code .xml} template fixtures
     */
    private static Stream<Path> shippedScenarioTemplateFiles() throws IOException {
        try (Stream<Path> paths = Files.list(CORPUS_DIRECTORY)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                         .filter(path -> !path.getFileName().toString().equals(SCENARIO_MANIFEST_FILE_NAME))
                         .sorted(Comparator.comparing(Path::toString))
                         .toList()
                         .stream();
        }
    }

    @Test
    void corpusDirectoryExistsAndIsPopulated() throws IOException {
        assertTrue(Files.isDirectory(CORPUS_DIRECTORY),
              "Scenario template corpus directory is missing: " + CORPUS_DIRECTORY.toAbsolutePath());
        assertFalse(shippedScenarioTemplateFiles().findAny().isEmpty(),
              "Scenario template corpus is empty: " + CORPUS_DIRECTORY.toAbsolutePath());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shippedScenarioTemplateFiles")
    void shippedTemplateDeserializes(Path templateFile) {
        ScenarioTemplate template = ScenarioTemplate.Deserialize(templateFile.toFile());

        assertNotNull(template, "Failed to deserialize scenario template: " + templateFile);
        assertNotNull(template.name, "Deserialized template has a null name: " + templateFile);
        assertFalse(template.name.isBlank(), "Deserialized template has a blank name: " + templateFile);
    }

    /**
     * Round-trip idempotence guard (Phase 0.2). The first marshal normalizes any legacy or formatting quirks present in
     * the shipped file, so we compare the serialized form of one round-trip against the serialized form of a second
     * round-trip. Any drift means serialization is not stable: a dropped field, a re-ordered element, or a value that
     * does not survive a load/save cycle. This is the baseline that later phases (correctness fixes, the XML to JSON
     * migration) must not regress.
     *
     * @param templateFile a shipped template fixture
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("shippedScenarioTemplateFiles")
    void shippedTemplateSurvivesRoundTripUnchanged(Path templateFile, @TempDir Path tempDir) throws IOException {
        ScenarioTemplate firstLoad = ScenarioTemplate.Deserialize(templateFile.toFile());
        assertNotNull(firstLoad, "Failed to deserialize scenario template: " + templateFile);

        String firstSerialization = serialize(firstLoad);

        ScenarioTemplate secondLoad = roundTripViaJson(firstLoad, tempDir.resolve("round-trip.json"));
        assertNotNull(secondLoad, "Failed to re-deserialize round-tripped template: " + templateFile);

        String secondSerialization = serialize(secondLoad);

        assertEquals(firstSerialization, secondSerialization,
              "Scenario template serialization is not idempotent: " + templateFile);
    }

    /**
     * Maximal-coverage round-trip guard (Phase 0.3). The shipped corpus (0.2) proves stability only for the field
     * combinations that happen to appear in shipped files; several persisted fields are rare or absent there (for
     * example {@code fixedMul} appears in no shipped template, and {@code objectiveLinkedForces},
     * {@code subjectToRandomRemoval} and {@code stratConScenarioType} appear in only a handful). This test builds a
     * single template with every persisted field set to a distinctive non-default value, then asserts that value
     * survives a load/save cycle unchanged.
     *
     * <p>Equality is checked via the canonical XML form (the deterministic JAXB writer proven stable in 0.2) rather
     * than {@code equals()}, keeping the whole safety net test-only. The same canonical-form comparison is what the
     * later cross-format (XML vs JSON) equivalence guard reuses: any field the new format drops or reshapes shows up as
     * a diff in the re-serialized XML.
     */
    @Test
    void maximalTemplateSurvivesRoundTripUnchanged(@TempDir Path tempDir) throws IOException {
        ScenarioTemplate maximal = buildMaximalTemplate();

        String firstSerialization = serialize(maximal);

        ScenarioTemplate reloaded = roundTripViaJson(maximal, tempDir.resolve("maximal.json"));
        assertNotNull(reloaded, "Failed to deserialize the maximal template");

        String secondSerialization = serialize(reloaded);

        assertEquals(firstSerialization, secondSerialization,
              "Maximal template lost or reshaped a field across a round-trip");
    }

    /**
     * Regression guard for the {@code additionalMapSheetTall}/{@code Wide} data-loss defect found in Phase 0.3 and
     * fixed in Phase 1.2.
     *
     * <p>These fields have getters and their backing fields are private, but originally had no setters. Under JAXB's
     * default {@code PUBLIC_MEMBER} access the values were marshaled out (they appear in every shipped template) but
     * could not be read back in, so a non-zero value authored in a file was silently dropped the moment the template
     * loaded - real data loss for map dimensions. Phase 1.2 added the setters; this test now asserts the authored value
     * survives a load. {@code Gauntlet Run.xml} ships with {@code additionalMapSheetTall = 2}.
     */
    @Test
    void additionalMapSheetFieldsSurviveLoad() {
        Path gauntletRun = CORPUS_DIRECTORY.resolve("Gauntlet Run.json");
        ScenarioTemplate template = ScenarioTemplate.Deserialize(gauntletRun.toFile());

        assertNotNull(template, "Failed to deserialize Gauntlet Run.json");
        assertEquals(2, template.mapParameters.getAdditionalMapSheetTall(),
              "additionalMapSheetTall authored in Gauntlet Run.xml (2) was lost on load");
    }

    /**
     * Builds a scenario template with every persisted field populated to a distinctive non-default value, including two
     * fully-specified forces (one player-supplied, one bot) and a fully-specified objective with success and failure
     * effects. Fields that JAXB does not persist ({@code syncRetreatThreshold}, the derived {@code requiredRoles}) are
     * intentionally left alone, as they cannot survive a round-trip and are not part of the serialization contract.
     */
    private static ScenarioTemplate buildMaximalTemplate() {
        ScenarioTemplate template = new ScenarioTemplate();
        template.name = "Maximal Coverage Template";
        template.setStratConScenarioType("CONVOY");
        template.shortBriefing = "Short briefing text.";
        template.detailedBriefing = "Detailed briefing text spanning the full field set.";
        template.isHostileFacility = true;
        template.isAlliedFacility = true;
        template.isSuitedForAmbushes = true;
        template.isSuitedForBungledPatrols = true;
        template.battlefieldControl = BattlefieldControlType.ENEMY;
        template.scenarioModifiers.add("SomeFixedModifierKey");
        template.scenarioModifiers.add("AnotherFixedModifierKey");

        ScenarioMapParameters mapParameters = new ScenarioMapParameters();
        mapParameters.setBaseWidth(35);
        mapParameters.setBaseHeight(45);
        mapParameters.setWidthScalingIncrement(6);
        mapParameters.setHeightScalingIncrement(7);
        mapParameters.setAdditionalMapSheetTall(3);
        mapParameters.setAdditionalMapSheetWide(2);
        mapParameters.setAllowRotation(true);
        mapParameters.setUseStandardAtBSizing(true);
        mapParameters.setMapLocation(MapLocation.SpecificGroundTerrain);
        mapParameters.getAllowedTerrainType().add("Woods");
        mapParameters.getAllowedTerrainType().add("Rough");
        template.mapParameters = mapParameters;

        ScenarioForceTemplate botForce = buildMaximalBotForce();
        ScenarioForceTemplate playerForce = buildMaximalPlayerForce();
        template.getScenarioForces().put(botForce.getForceName(), botForce);
        template.getScenarioForces().put(playerForce.getForceName(), playerForce);

        template.scenarioObjectives.add(buildMaximalObjective(botForce.getForceName()));

        return template;
    }

    private static ScenarioForceTemplate buildMaximalBotForce() {
        ScenarioForceTemplate force = new ScenarioForceTemplate(ScenarioForceTemplate.ForceAlignment.Opposing.ordinal(),
              ScenarioForceTemplate.ForceGenerationMethod.BVScaled.ordinal(),
              1.25,
              List.of(2, 4, 6),
              ScenarioForceTemplate.DESTINATION_EDGE_RANDOM,
              40,
              UnitType.TANK);
        force.setForceName("Bravo");
        force.setCanReinforceLinked(true);
        force.setContributesToBV(true);
        force.setContributesToUnitCount(true);
        force.setContributesToMapSize(true);
        force.setSyncDeploymentType(SynchronizedDeploymentType.None);
        force.setSyncedForceName("Alpha");
        force.setArrivalTurn(3);
        force.setMaxWeightClass(EntityWeightClass.WEIGHT_ASSAULT);
        force.setMinWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        force.setActualDeploymentZone(5);
        force.setFixedUnitCount(4);
        force.setGenerationOrder(2);
        force.setAllowAeroBombs(true);
        force.setStartingAltitude(6);
        force.setUseArtillery(true);
        force.setDeployOffboard(true);
        force.setObjectiveLinkedForces(List.of("Alpha"));
        force.setSubjectToRandomRemoval(false);
        force.setSyncRetreatThreshold(true);
        force.setFixedMul("SomeFixed.mul");
        force.getRoleCollections().add("Skirmisher,Fire_Support");
        return force;
    }

    private static ScenarioForceTemplate buildMaximalPlayerForce() {
        ScenarioForceTemplate force = new ScenarioForceTemplate(ScenarioForceTemplate.ForceAlignment.Player.ordinal(),
              ScenarioForceTemplate.ForceGenerationMethod.PlayerSupplied.ordinal(),
              1.0,
              List.of(1, 3),
              megamek.client.bot.princess.CardinalEdge.NORTH.getIndex(),
              50,
              ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);
        force.setForceName("Alpha");
        force.setGenerationOrder(1);
        force.setArrivalTurn(0);
        return force;
    }

    private static ScenarioObjective buildMaximalObjective(String linkedForceName) {
        ScenarioObjective objective = new ScenarioObjective();
        objective.setObjectiveCriterion(ObjectiveCriterion.Destroy);
        objective.setDescription("Destroy the hostile column.");
        objective.setPercentage(75);
        objective.setFixedAmount(2);
        objective.setDestinationEdge(OffBoardDirection.NORTH);
        objective.setTimeLimitType(TimeLimitType.Fixed);
        objective.setTimeLimit(8);
        objective.setTimeLimitScaleFactor(2);
        objective.setTimeLimitAtMost(true);
        objective.addForce(linkedForceName);
        objective.addDetail("An additional detail line.");

        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        successEffect.effectScaling = EffectScalingType.Linear;
        successEffect.howMuch = 2;
        objective.addSuccessEffect(successEffect);

        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        failureEffect.effectScaling = EffectScalingType.Fixed;
        failureEffect.howMuch = 1;
        objective.addFailureEffect(failureEffect);

        return objective;
    }

    /**
     * Serializes a template to its XML string form using the fragment writer used by the editor.
     */
    private static String serialize(ScenarioTemplate template) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            template.Serialize(printWriter);
        }
        return stringWriter.toString();
    }

    /**
     * Round-trips a template through the production JSON file path: saves it to a scratch {@code .json} file and reads
     * it back. JSON is the only standalone format, so a save/load cycle goes through it.
     */
    private static ScenarioTemplate roundTripViaJson(ScenarioTemplate template, Path scratchFile) {
        template.Serialize(scratchFile.toFile());
        return ScenarioTemplate.Deserialize(scratchFile.toFile());
    }
}
