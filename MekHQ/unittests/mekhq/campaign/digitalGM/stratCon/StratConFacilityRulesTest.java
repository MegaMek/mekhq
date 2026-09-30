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
package mekhq.campaign.digitalGM.stratCon;

import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
import static megamek.common.units.UnitType.CONV_FIGHTER;
import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation.AllGroundTerrain;
import static mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation.LowAtmosphere;
import static mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation.Space;
import static mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation.SpecificGroundTerrain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests the StratCon facility rules: facilities that prevent aerospace, the objective type given to placed objective
 * facilities, what a captured facility takes from its new owner's definition, and which facilities count against the
 * non-objective share.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityRulesTest {

    private static final String LAND = "Grasslands";

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConTrackState landTrack(int width, int height) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        track.setDisplayableName("Sector Test");

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                track.setTerrainTile(new StratConCoords(x, y), LAND);
            }
        }

        return track;
    }

    private static StratConFacility facility(ForceAlignment owner, boolean preventAerospace) {
        StratConFacility facility = new StratConFacility();
        facility.setOwner(owner);
        facility.setPreventAerospace(preventAerospace);
        return facility;
    }

    @Nested
    class AerospacePrevention {
        @Test
        void aTrackWithNoFacilitiesAllowsAerospace() {
            assertFalse(landTrack(3, 3).isAerospacePrevented());
        }

        @Test
        void facilitiesWithoutTheFlagAllowAerospace() {
            StratConTrackState track = landTrack(3, 3);
            track.addFacility(new StratConCoords(0, 0), facility(ForceAlignment.Opposing, false));
            track.addFacility(new StratConCoords(1, 1), facility(ForceAlignment.Allied, false));

            assertFalse(track.isAerospacePrevented());
        }

        @Test
        void aHostileFacilityWithTheFlagPreventsAerospace() {
            StratConTrackState track = landTrack(3, 3);
            track.addFacility(new StratConCoords(0, 0), facility(ForceAlignment.Opposing, true));

            assertTrue(track.isAerospacePrevented());
        }

        @Test
        void anAlliedFacilityWithTheFlagAlsoPreventsAerospace() {
            // The rule ignores who holds the facility.
            StratConTrackState track = landTrack(3, 3);
            track.addFacility(new StratConCoords(0, 0), facility(ForceAlignment.Allied, true));

            assertTrue(track.isAerospacePrevented());
        }

        @Test
        void removingTheFacilityLiftsThePrevention() {
            StratConTrackState track = landTrack(3, 3);
            StratConCoords coords = new StratConCoords(0, 0);
            track.addFacility(coords, facility(ForceAlignment.Opposing, true));
            track.removeFacility(coords);

            assertFalse(track.isAerospacePrevented());
        }
    }

    @Nested
    class MapLocationRestriction {
        private StratConTrackState preventedTrack() {
            StratConTrackState track = landTrack(3, 3);
            track.addFacility(new StratConCoords(0, 0), facility(ForceAlignment.Opposing, true));
            return track;
        }

        @Test
        void noTrackLeavesTheLocationsAlone() {
            Set<MapLocation> allowed = EnumSet.of(Space);

            assertSame(allowed, StratConRulesManager.restrictMapLocationsForTrack(allowed, null));
            assertNull(StratConRulesManager.restrictMapLocationsForTrack(null, null));
        }

        @Test
        void anUnrestrictedTrackLeavesTheLocationsAlone() {
            Set<MapLocation> allowed = EnumSet.of(Space, LowAtmosphere);

            assertSame(allowed, StratConRulesManager.restrictMapLocationsForTrack(allowed, landTrack(3, 3)));
            assertNull(StratConRulesManager.restrictMapLocationsForTrack(null, landTrack(3, 3)));
        }

        @Test
        void aPreventedTrackAllowsOnlyGroundWhenNothingElseIsRestricted() {
            assertEquals(EnumSet.of(AllGroundTerrain, SpecificGroundTerrain),
                  StratConRulesManager.restrictMapLocationsForTrack(null, preventedTrack()));
        }

        @Test
        void aPreventedTrackRemovesAirFromAnExistingRestriction() {
            assertEquals(EnumSet.noneOf(MapLocation.class),
                  StratConRulesManager.restrictMapLocationsForTrack(EnumSet.of(Space, LowAtmosphere),
                        preventedTrack()));
        }

        @Test
        void theCallersSetIsNotChanged() {
            Set<MapLocation> allowed = EnumSet.of(Space, AllGroundTerrain);
            StratConRulesManager.restrictMapLocationsForTrack(allowed, preventedTrack());

            assertEquals(EnumSet.of(Space, AllGroundTerrain), allowed);
        }
    }

    @Nested
    class UnitTypeAdjustment {
        @Test
        void noRestrictionKeepsTheUnitType() {
            assertEquals(AEROSPACE_FIGHTER, StratConRulesManager.adjustUnitTypeForMapLocations(AEROSPACE_FIGHTER, null));
            assertEquals(MEK, StratConRulesManager.adjustUnitTypeForMapLocations(MEK, null));
        }

        @Test
        void anAerospaceTypeBecomesAMekWhenOnlyGroundIsAllowed() {
            Set<MapLocation> groundOnly = EnumSet.of(AllGroundTerrain, SpecificGroundTerrain);

            assertEquals(MEK, StratConRulesManager.adjustUnitTypeForMapLocations(AEROSPACE_FIGHTER, groundOnly));
            assertEquals(MEK, StratConRulesManager.adjustUnitTypeForMapLocations(CONV_FIGHTER, groundOnly));
        }

        @Test
        void aGroundTypeBecomesAnAerospaceFighterWhenOnlySpaceAndAirAreAllowed() {
            assertEquals(AEROSPACE_FIGHTER,
                  StratConRulesManager.adjustUnitTypeForMapLocations(MEK, EnumSet.of(Space, LowAtmosphere)));
        }

        @Test
        void aGroundTypeBecomesAConventionalFighterWhenOnlyAirIsAllowed() {
            assertEquals(CONV_FIGHTER, StratConRulesManager.adjustUnitTypeForMapLocations(MEK, EnumSet.of(LowAtmosphere)));
        }

        @Test
        void aTypeThatFitsTheAllowedLocationsIsKept() {
            assertEquals(MEK, StratConRulesManager.adjustUnitTypeForMapLocations(MEK, EnumSet.of(AllGroundTerrain)));
            assertEquals(AEROSPACE_FIGHTER,
                  StratConRulesManager.adjustUnitTypeForMapLocations(AEROSPACE_FIGHTER, EnumSet.of(Space)));
        }

        @Test
        void nothingAllowedKeepsTheUnitType() {
            // With no location left there is no better type to pick; the empty selection is left to the factory.
            Set<MapLocation> none = EnumSet.noneOf(MapLocation.class);

            assertEquals(MEK, StratConRulesManager.adjustUnitTypeForMapLocations(MEK, none));
            assertEquals(AEROSPACE_FIGHTER, StratConRulesManager.adjustUnitTypeForMapLocations(AEROSPACE_FIGHTER, none));
        }

        @Test
        void anAerospaceFormationOnAPreventedTrackAsksForAGroundTemplate() {
            StratConTrackState track = landTrack(3, 3);
            track.addFacility(new StratConCoords(0, 0), facility(ForceAlignment.Allied, true));

            try (MockedStatic<StratConScenarioFactory> factory = mockStatic(StratConScenarioFactory.class,
                  CALLS_REAL_METHODS)) {
                StratConRulesManager.getRandomScenarioForTrack(track, AEROSPACE_FIGHTER, true, false);

                factory.verify(() -> StratConScenarioFactory.getRandomScenario(MEK,
                      true,
                      false,
                      EnumSet.of(AllGroundTerrain, SpecificGroundTerrain)));
            }
        }

        @Test
        void anUnrestrictedTrackAsksForAnyTemplate() {
            try (MockedStatic<StratConScenarioFactory> factory = mockStatic(StratConScenarioFactory.class,
                  CALLS_REAL_METHODS)) {
                StratConRulesManager.getRandomScenarioForTrack(landTrack(3, 3), AEROSPACE_FIGHTER, false, true);

                factory.verify(() -> StratConScenarioFactory.getRandomScenario(AEROSPACE_FIGHTER, false, true, null));
            }
        }
    }

    @Nested
    class CaptureCopy {
        private StratConFacility capturedDefinition(boolean visible) {
            StratConFacility definition = new StratConFacility();
            definition.setOwner(ForceAlignment.Allied);
            definition.setDisplayableName("Allied Data Center");
            definition.setFacilityType(FacilityType.DataCenter);
            definition.setVisible(visible);
            return definition;
        }

        private StratConFacility hostileFacility(boolean visible) {
            StratConFacility facility = new StratConFacility();
            facility.setOwner(ForceAlignment.Opposing);
            facility.setDisplayableName("Hostile Comms Center");
            facility.setFacilityType(FacilityType.CommandCenter);
            facility.setVisible(visible);
            return facility;
        }

        @Test
        void theNameTypeAndOwnerAreCopied() {
            StratConFacility facility = hostileFacility(true);
            facility.copyRulesDataFrom(capturedDefinition(true));

            assertEquals("Allied Data Center", facility.getDisplayableName());
            assertEquals(FacilityType.DataCenter, facility.getFacilityType());
            assertEquals(ForceAlignment.Allied, facility.getOwner());
        }

        @Test
        void aSeenFacilityStaysVisibleWhenTheDefinitionStartsHidden() {
            StratConFacility facility = hostileFacility(true);
            facility.copyRulesDataFrom(capturedDefinition(false));

            assertTrue(facility.getVisible());
        }

        @Test
        void aHiddenFacilityBecomesVisibleWhenTheDefinitionStartsVisible() {
            StratConFacility facility = hostileFacility(false);
            facility.copyRulesDataFrom(capturedDefinition(true));

            assertTrue(facility.getVisible());
        }

        @Test
        void aHiddenFacilityStaysHiddenWhenTheDefinitionStartsHidden() {
            StratConFacility facility = hostileFacility(false);
            facility.copyRulesDataFrom(capturedDefinition(false));

            assertFalse(facility.getVisible());
        }
    }

    @Nested
    class ObjectiveFacilityPlacement {
        private StratConStrategicObjective placeOne(ForceAlignment owner, StrategicObjectiveType objectiveType,
              StratConTrackState track) {
            StratConContractInitializer.initializeTrackFacilities(track,
                  1,
                  owner,
                  objectiveType,
                  Collections.emptyList());

            assertEquals(1, track.getStrategicObjectives().size());
            return track.getStrategicObjectives().get(0);
        }

        @Test
        void aFacilityDestructionObjectiveKeepsItsType() {
            StratConTrackState track = landTrack(5, 5);
            StratConStrategicObjective objective = placeOne(ForceAlignment.Opposing,
                  StrategicObjectiveType.FacilityDestruction,
                  track);

            assertEquals(StrategicObjectiveType.FacilityDestruction, objective.getObjectiveType());
            StratConFacility facility = track.getFacility(objective.getObjectiveCoords());
            assertTrue(facility.isStrategicObjective());
            assertFalse(facility.getVisible());
        }

        @Test
        void aHostileFacilityControlObjectiveKeepsItsType() {
            StratConStrategicObjective objective = placeOne(ForceAlignment.Opposing,
                  StrategicObjectiveType.HostileFacilityControl,
                  landTrack(5, 5));

            assertEquals(StrategicObjectiveType.HostileFacilityControl, objective.getObjectiveType());
        }

        @Test
        void anAlliedFacilityControlObjectiveIsRevealed() {
            StratConTrackState track = landTrack(5, 5);
            StratConStrategicObjective objective = placeOne(ForceAlignment.Allied,
                  StrategicObjectiveType.AlliedFacilityControl,
                  track);

            assertEquals(StrategicObjectiveType.AlliedFacilityControl, objective.getObjectiveType());
            assertTrue(track.getFacility(objective.getObjectiveCoords()).getVisible());
            assertTrue(track.getRevealedCoords().contains(objective.getObjectiveCoords()));
        }

        @Test
        void noObjectiveTypePlacesOrdinaryFacilities() {
            StratConTrackState track = landTrack(5, 5);
            StratConContractInitializer.initializeTrackFacilities(track,
                  3,
                  ForceAlignment.Opposing,
                  null,
                  Collections.emptyList());

            assertEquals(3, track.getFacilities().size());
            assertTrue(track.getStrategicObjectives().isEmpty());
            for (StratConFacility facility : track.getFacilities().values()) {
                assertFalse(facility.isStrategicObjective());
            }
        }
    }

    @Nested
    class NonObjectiveShare {
        @Test
        void onlyTheGivenSidesFacilitiesAreCounted() {
            StratConTrackState first = landTrack(3, 3);
            first.addFacility(new StratConCoords(0, 0), facility(ForceAlignment.Allied, false));
            first.addFacility(new StratConCoords(1, 0), facility(ForceAlignment.Opposing, false));
            StratConTrackState second = landTrack(3, 3);
            second.addFacility(new StratConCoords(0, 0), facility(ForceAlignment.Opposing, false));
            second.addFacility(new StratConCoords(1, 1), facility(ForceAlignment.Opposing, false));

            List<StratConTrackState> tracks = List.of(first, second);

            assertEquals(1, StratConContractInitializer.countFacilitiesOwnedBy(tracks, ForceAlignment.Allied));
            assertEquals(3, StratConContractInitializer.countFacilitiesOwnedBy(tracks, ForceAlignment.Opposing));
        }

        @Test
        void hostileObjectivesDoNotEatAnAlliedShare() {
            // A defensive contract at scale 2 with support points factored in: two allied facilities are wanted. Two
            // hostile objective facilities must not use them up.
            StratConTrackState track = landTrack(5, 5);
            StratConContractInitializer.initializeTrackFacilities(track,
                  2,
                  ForceAlignment.Opposing,
                  StrategicObjectiveType.HostileFacilityControl,
                  Collections.emptyList());

            int alliedObjectiveCount = StratConContractInitializer.countFacilitiesOwnedBy(List.of(track),
                  ForceAlignment.Allied);

            assertEquals(0, alliedObjectiveCount);
            assertEquals(2, StratConContractInitializer.getNonObjectiveFacilityCount(2, true, alliedObjectiveCount));
        }

        @Test
        void theDefendersOwnObjectivesStillCount() {
            StratConTrackState track = landTrack(5, 5);
            StratConContractInitializer.initializeTrackFacilities(track,
                  1,
                  ForceAlignment.Allied,
                  StrategicObjectiveType.AlliedFacilityControl,
                  Collections.emptyList());

            int alliedObjectiveCount = StratConContractInitializer.countFacilitiesOwnedBy(List.of(track),
                  ForceAlignment.Allied);

            assertEquals(1, alliedObjectiveCount);
            assertEquals(1, StratConContractInitializer.getNonObjectiveFacilityCount(2, true, alliedObjectiveCount));
        }

        @Test
        void noTracksCountsNothing() {
            assertEquals(0, StratConContractInitializer.countFacilitiesOwnedBy(List.of(), ForceAlignment.Allied));
        }
    }
}
