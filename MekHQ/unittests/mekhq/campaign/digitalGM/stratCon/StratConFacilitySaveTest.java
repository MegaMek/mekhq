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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.digitalGM.stratCon.facility.FacilityOperation;
import mekhq.campaign.digitalGM.stratCon.facility.FacilityTrait;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityFactory;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityOrder;
import mekhq.campaign.digitalGM.stratCon.facility.StratConRoadCut;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests facilities and their new state through the path real saves take: {@link StratConCampaignState#Serialize} and
 * {@link StratConCampaignState#Deserialize}, for a save from before 0.51.01 and for a round trip of the new state.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilitySaveTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 1, 4);
    private static final StratConCoords FACILITY_COORDS = new StratConCoords(1, 1);
    private static final StratConCoords ROAD_COORDS = new StratConCoords(3, 3);

    /** A campaign state as a save from before 0.51.01 held it: a hostile Mek Base in the old full-copy format. */
    private static final String OLD_SAVE = """
          <StratConCampaignState>
              <campaignTracks>
                  <campaignTrack>
                      <width>5</width>
                      <height>5</height>
                      <trackFacilities>
                          <entry>
                              <key><x>1</x><y>1</y></key>
                              <value>
                                  <owner>Opposing</owner>
                                  <displayableName>Mek Base</displayableName>
                                  <facilityType>MekBase</facilityType>
                                  <userDescription>Hostile Meks will participate in scenarios.</userDescription>
                                  <visible>true</visible>
                                  <isAvailable>true</isAvailable>
                                  <sharedModifiers>EnemyMekReinforcements.json</sharedModifiers>
                                  <localModifiers>EnemyMekGarrison.json</localModifiers>
                                  <localModifiers>FacilityHostileDestroy.json</localModifiers>
                                  <capturedDefinition>AlliedMekBase.json</capturedDefinition>
                                  <strategicObjective>true</strategicObjective>
                              </value>
                          </entry>
                      </trackFacilities>
                  </campaignTrack>
              </campaignTracks>
              <supportPoints>3</supportPoints>
          </StratConCampaignState>
          """;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConCampaignState load(String xml) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                                  .newDocumentBuilder()
                                  .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        StratConCampaignState campaignState = StratConCampaignState.Deserialize(document.getDocumentElement());
        assertNotNull(campaignState, "the campaign state should load");
        return campaignState;
    }

    private static StratConCampaignState saveAndLoad(StratConCampaignState campaignState) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            campaignState.Serialize(printWriter);
        }
        return load(stringWriter.toString());
    }

    @Test
    void anOldSavesFacilityLoadsAsTheNewKind() throws Exception {
        StratConCampaignState campaignState = load(OLD_SAVE);

        StratConFacility facility = campaignState.getTrack(0).getFacility(FACILITY_COORDS);
        assertNotNull(facility);
        assertEquals("MekBase", facility.getDefinitionId());
        assertEquals(ForceAlignment.Opposing, facility.getOwner());
        assertTrue(facility.isStrategicObjective());
        assertEquals(List.of("FacilityHostileDestroy.json"), facility.getAdditionalLocalModifiers());
        // An old facility knew nothing of tiers: it loads as a Base at full garrison, intact.
        assertEquals(FacilityTier.BASE, facility.getTier());
        assertEquals(facility.getGarrisonMaximum(), facility.getGarrison());
        assertEquals(FacilityCondition.INTACT, facility.getCondition());
    }

    @Test
    void anOldSaveStillLoadsAfterBeingSavedAgain() throws Exception {
        StratConCampaignState reloaded = saveAndLoad(load(OLD_SAVE));

        StratConFacility facility = reloaded.getTrack(0).getFacility(FACILITY_COORDS);
        assertNotNull(facility);
        assertEquals("MekBase", facility.getDefinitionId());
        assertEquals(List.of("FacilityHostileDestroy.json"), facility.getAdditionalLocalModifiers());
    }

    @Test
    void theNewFacilityStateSurvivesASave() throws Exception {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        StratConFacility facility = new StratConFacility(StratConFacilityFactory.getDefinition("MekBase"),
              ForceAlignment.Opposing);
        facility.setTier(FacilityTier.STRONGHOLD);
        facility.setCondition(FacilityCondition.DAMAGED);
        facility.setGarrison(2);
        facility.setIntel(FacilityIntel.DETAILED);
        facility.addTrait(FacilityTrait.HARDENED);
        track.addFacility(FACILITY_COORDS, facility);

        track.getRoadCuts().add(new StratConRoadCut(ROAD_COORDS, TODAY.plusDays(28)));
        track.getCutOffFacilities().add(FACILITY_COORDS);
        StratConFacilityOrder siege = new StratConFacilityOrder(FacilityOperation.SIEGE,
              7,
              FACILITY_COORDS,
              TODAY,
              null);
        siege.setWasAlreadySticky(true);
        track.addFacilityOrder(siege);

        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        campaignState.setLastCounterattackDate(TODAY);
        campaignState.getEnemyEngineerDates().add(TODAY.plusDays(5));

        StratConCampaignState reloadedState = saveAndLoad(campaignState);
        StratConTrackState reloadedTrack = reloadedState.getTrack(0);

        StratConFacility reloadedFacility = reloadedTrack.getFacility(FACILITY_COORDS);
        assertNotNull(reloadedFacility);
        assertEquals(FacilityTier.STRONGHOLD, reloadedFacility.getTier());
        assertEquals(FacilityCondition.DAMAGED, reloadedFacility.getCondition());
        assertEquals(2, reloadedFacility.getGarrison());
        assertEquals(FacilityIntel.DETAILED, reloadedFacility.getIntel());
        assertEquals(List.of(FacilityTrait.HARDENED), reloadedFacility.getTraits());

        assertEquals(1, reloadedTrack.getRoadCuts().size());
        assertEquals(ROAD_COORDS, reloadedTrack.getRoadCuts().get(0).getCoords());
        assertEquals(TODAY.plusDays(28), reloadedTrack.getRoadCuts().get(0).getEndDate());
        assertTrue(reloadedTrack.isRoadCut(ROAD_COORDS));
        assertTrue(reloadedTrack.getCutOffFacilities().contains(FACILITY_COORDS));

        StratConFacilityOrder reloadedSiege = reloadedTrack.getFacilityOrder(7);
        assertNotNull(reloadedSiege);
        assertEquals(FacilityOperation.SIEGE, reloadedSiege.getOperation());
        assertEquals(FACILITY_COORDS, reloadedSiege.getTargetCoords());
        assertEquals(TODAY, reloadedSiege.getCompletionDate());
        assertTrue(reloadedSiege.isWasAlreadySticky());

        assertEquals(TODAY, reloadedState.getLastCounterattackDate());
        assertEquals(List.of(TODAY.plusDays(5)), reloadedState.getEnemyEngineerDates());
    }
}
