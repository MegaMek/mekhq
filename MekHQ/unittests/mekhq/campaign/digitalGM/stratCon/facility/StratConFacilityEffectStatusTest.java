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
package mekhq.campaign.digitalGM.stratCon.facility;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffectStatus.SupportRules;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScanRangeEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.SharedModifiersEffect;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the explanations of what facilities are doing right now: a facility's own effects and why any is not working,
 * a sector's totals, and the support a fight drew.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityEffectStatusTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConFacilityOperations";
    private static final StratConCoords AIR_BASE_COORDS = new StratConCoords(1, 1);
    private static final StratConCoords SECOND_COORDS = new StratConCoords(4, 4);
    private static final StratConCoords THIRD_COORDS = new StratConCoords(6, 2);
    private static final SupportRules ONE_IN_TWO = new SupportRules(2, 2, false, false);
    private static final String AIR_SUPPORT = "Allied Air Support";

    private StratConTrackState track;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setWidth(8);
        track.setHeight(8);
    }

    private StratConFacility placeAirBase(ForceAlignment owner) {
        StratConFacility airBase = StratConTestData.facility(owner,
              FacilityType.AirBase,
              new LocalModifiersEffect(List.of("AlliedAirGarrison.json")),
              new SharedModifiersEffect(List.of("AlliedAirSupport.json")));
        track.addFacility(AIR_BASE_COORDS, airBase);
        return airBase;
    }

    private StratConFacility placeJammingStation(StratConCoords coords, FacilityIntel intel) {
        StratConFacility jammingStation = StratConTestData.facility(ForceAlignment.Opposing,
              FacilityType.JammingStation,
              new ScanRangeEffect(-1));
        jammingStation.setIntel(intel);
        track.addFacility(coords, jammingStation);
        return jammingStation;
    }

    private List<String> airBaseLines(SupportRules rules) {
        return StratConFacilityEffectStatus.getFacilityEffectLines(track, AIR_BASE_COORDS, rules);
    }

    @Test
    void modifierIdsReadAsWords() {
        assertEquals(AIR_SUPPORT, StratConFacilityEffectStatus.describeModifier("AlliedAirSupport.json"));
        assertEquals("Enemy Air Garrison", StratConFacilityEffectStatus.describeModifier("EnemyAirGarrison"));
        assertEquals("Allied Air Support, Allied Turrets",
              StratConFacilityEffectStatus.describeModifiers(List.of("AlliedAirSupport.json",
                    "AlliedTurrets.json",
                    "AlliedAirSupport.json")));
    }

    @Test
    void aReadyAirBaseSaysHowOftenItLends() {
        StratConFacility airBase = placeAirBase(ForceAlignment.Allied);
        // A Sensor Post has nothing to lend, so it is not one of the facilities a fight might draw on.
        track.addFacility(SECOND_COORDS, StratConTestData.facility(ForceAlignment.Allied,
              FacilityType.SensorPost,
              new ScanRangeEffect(1)));

        List<String> lines = airBaseLines(ONE_IN_TWO);

        assertTrue(lines.contains(getFormattedTextAt(RESOURCE_BUNDLE,
              "effect.support.ready.allied",
              AIR_SUPPORT,
              2,
              1)), lines.toString());
        // Its defenders are its garrison's, ladder steps included.
        assertTrue(lines.contains(getFormattedTextAt(RESOURCE_BUNDLE,
              "effect.defenders",
              StratConFacilityEffectStatus.describeModifiers(airBase.getLocalModifiers()))));
    }

    @Test
    void onlyYourEmployersFacilitiesSayTheyAnswerHalfTheTime() {
        String employerLine = getFormattedTextAt(RESOURCE_BUNDLE,
              "effect.support.employer",
              StratConRulesManager.EMPLOYER_LEND_DIE_SIZE);
        String sectorLine = getFormattedTextAt(RESOURCE_BUNDLE,
              "sector.support.employer",
              StratConRulesManager.EMPLOYER_LEND_DIE_SIZE);

        StratConFacility airBase = placeAirBase(ForceAlignment.Allied);
        assertTrue(airBaseLines(ONE_IN_TWO).contains(employerLine));
        assertTrue(StratConFacilityEffectStatus.getSectorEffectLines(track, ONE_IN_TWO).contains(sectorLine));

        airBase.setOwner(ForceAlignment.Player);
        assertFalse(airBaseLines(ONE_IN_TWO).contains(employerLine));
        assertFalse(StratConFacilityEffectStatus.getSectorEffectLines(track, ONE_IN_TWO).contains(sectorLine));
    }

    @Test
    void anAirBaseThatHasLentThisWeekSaysWhenItIsReadyAgain() {
        StratConFacility airBase = placeAirBase(ForceAlignment.Player);
        airBase.setIsAvailable(false);

        assertTrue(airBaseLines(ONE_IN_TWO).contains(getFormattedTextAt(RESOURCE_BUNDLE,
              "effect.support.spent.allied",
              AIR_SUPPORT)));
    }

    @Test
    void aDamagedAirBaseLendsNothingAndIsNotCountedAsReady() {
        StratConFacility airBase = placeAirBase(ForceAlignment.Allied);
        airBase.setCondition(FacilityCondition.DAMAGED);

        assertTrue(airBaseLines(ONE_IN_TWO).contains(getFormattedTextAt(RESOURCE_BUNDLE,
              "effect.support.condition.allied",
              AIR_SUPPORT,
              "damaged")));
        assertFalse(StratConFacilityEffectStatus.isReadyToLend(airBase, ONE_IN_TWO));
    }

    @Test
    void supportTurnedOffInTheOptionsIsSaidSo() {
        placeAirBase(ForceAlignment.Allied);

        assertTrue(airBaseLines(new SupportRules(0, 2, false, false)).contains(getFormattedTextAt(RESOURCE_BUNDLE,
              "effect.support.disabled",
              AIR_SUPPORT)));
    }

    @Test
    void aFacilityWithNoGarrisonLeftSaysItAddsNoDefenders() {
        StratConFacility airBase = placeAirBase(ForceAlignment.Allied);
        airBase.setGarrison(0);

        assertTrue(airBaseLines(ONE_IN_TWO).contains(getTextAt(RESOURCE_BUNDLE, "effect.defenders.noGarrison")));
    }

    @Test
    void anUnscoutedEnemyFacilityGivesNothingAway() {
        StratConFacility airBase = placeAirBase(ForceAlignment.Opposing);
        airBase.setIntel(FacilityIntel.LOCATED);
        assertTrue(airBaseLines(ONE_IN_TWO).isEmpty());

        airBase.setIntel(FacilityIntel.SCOUTED);
        List<String> lines = airBaseLines(ONE_IN_TWO);
        assertTrue(lines.contains(getFormattedTextAt(RESOURCE_BUNDLE, "effect.support.ready.enemy", AIR_SUPPORT)));
        // The garrison, and so its defenders, is only known with detailed intel.
        assertFalse(lines.contains(getFormattedTextAt(RESOURCE_BUNDLE,
              "effect.defenders",
              StratConFacilityEffectStatus.describeModifiers(airBase.getLocalModifiers()))));
    }

    @Test
    void aWeakenedNumberShowsItsFullStrengthToo() {
        StratConFacility sensorPost = StratConTestData.facility(ForceAlignment.Allied,
              FacilityType.SensorPost,
              new ScanRangeEffect(2));
        sensorPost.setCondition(FacilityCondition.DAMAGED);
        track.addFacility(SECOND_COORDS, sensorPost);

        String expected = getFormattedTextAt(RESOURCE_BUNDLE,
              "effect.weakened",
              getFormattedTextAt(RESOURCE_BUNDLE, "effect.scanRange", "+1"),
              "+2",
              "damaged");
        assertTrue(StratConFacilityEffectStatus.getFacilityEffectLines(track, SECOND_COORDS, ONE_IN_TWO)
                         .contains(expected));
    }

    @Test
    void theSectorShowsJammingFromFacilitiesThePlayerHasNotFoundWithoutNamingThem() {
        StratConFacility knownStation = placeJammingStation(SECOND_COORDS, FacilityIntel.LOCATED);
        placeJammingStation(THIRD_COORDS, FacilityIntel.UNKNOWN);

        List<String> lines = StratConFacilityEffectStatus.getSectorEffectLines(track, ONE_IN_TWO);

        String sources = getFormattedTextAt(RESOURCE_BUNDLE,
              "sector.source",
              knownStation.getDisplayableName(),
              "-1")
                               + ", "
                               + getFormattedTextAt(RESOURCE_BUNDLE, "sector.source.unknown", "-1");
        assertTrue(lines.contains(getFormattedTextAt(RESOURCE_BUNDLE, "sector.scanRange", "-2", sources)),
              lines.toString());
        assertTrue(lines.contains(getFormattedTextAt(RESOURCE_BUNDLE, "sector.scanRadius", 0, 0)));
    }

    @Test
    void theSectorCountsYourReadySupport() {
        placeAirBase(ForceAlignment.Allied);
        StratConFacility commsCenter = StratConTestData.facility(ForceAlignment.Allied,
              FacilityType.CommandCenter,
              new SharedModifiersEffect(List.of("AlliedReinforcements.json")));
        commsCenter.setIsAvailable(false);
        track.addFacility(SECOND_COORDS, commsCenter);

        assertTrue(StratConFacilityEffectStatus.getSectorEffectLines(track, ONE_IN_TWO)
                         .contains(getFormattedTextAt(RESOURCE_BUNDLE, "sector.support", 1, 2, 2)));
    }

    @Test
    void aQuietSectorHasNothingToReport() {
        assertTrue(StratConFacilityEffectStatus.getSectorEffectLines(track, ONE_IN_TWO).isEmpty());
    }

    @Test
    void aFightSetUpBeforeSupportWasRecordedShowsNothing() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(SECOND_COORDS);

        assertTrue(StratConFacilityEffectStatus.getScenarioSupportLines(track, scenario).isEmpty());
    }

    @Test
    void aFightsSupportRoundTripsThroughTheSave() throws JAXBException {
        StratConScenario scenario = new StratConScenario();
        scenario.setFacilitySupportRecorded(true);
        scenario.getAlliedFacilitySupport().add("Air Base");
        scenario.getHostileFacilitySupport().add(THIRD_COORDS);

        JAXBContext context = JAXBContext.newInstance(StratConScenario.class);
        StringWriter writer = new StringWriter();
        context.createMarshaller()
              .marshal(new JAXBElement<>(new QName("scenario"), StratConScenario.class, scenario), writer);
        StratConScenario loaded = context.createUnmarshaller()
                                        .unmarshal(new StreamSource(new StringReader(writer.toString())),
                                              StratConScenario.class)
                                        .getValue();

        assertTrue(loaded.isFacilitySupportRecorded());
        assertEquals(List.of("Air Base"), loaded.getAlliedFacilitySupport());
        assertEquals(List.of(THIRD_COORDS), loaded.getHostileFacilitySupport());
    }

    @Test
    void aFightThatDrewNoSupportSaysSo() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(SECOND_COORDS);
        scenario.setFacilitySupportRecorded(true);

        assertEquals(List.of(getTextAt(RESOURCE_BUNDLE, "scenario.support.none")),
              StratConFacilityEffectStatus.getScenarioSupportLines(track, scenario));
    }

    @Test
    void aFightNamesItsSupportButNotEnemyFacilitiesThePlayerHasNotFound() {
        placeJammingStation(THIRD_COORDS, FacilityIntel.UNKNOWN);
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(SECOND_COORDS);
        scenario.setFacilitySupportRecorded(true);
        scenario.getAlliedFacilitySupport().add("Air Base");
        scenario.getHostileFacilitySupport().add(THIRD_COORDS);

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "scenario.support.allied", "Air Base"),
                    getFormattedTextAt(RESOURCE_BUNDLE,
                          "scenario.support.enemy",
                          getFormattedTextAt(RESOURCE_BUNDLE, "scenario.support.enemy.unknown", 1))),
              StratConFacilityEffectStatus.getScenarioSupportLines(track, scenario));
    }
}
