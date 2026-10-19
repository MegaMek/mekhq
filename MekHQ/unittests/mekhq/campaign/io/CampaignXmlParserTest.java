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
package mekhq.campaign.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import megamek.Version;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignLocationManager;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOptionsUnmarshaller;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.education.AcademyFactory;
import mekhq.campaign.personnel.enums.education.EducationStage;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import testUtilities.MHQTestUtilities;

/**
 * Tests the save-load reputation migration in {@link CampaignXmlParser}: Chaos Reputation defaults on for new
 * campaigns, but existing campaigns (whose saves predate the option) must not be silently switched over.
 */
class CampaignXmlParserTest {
    private static final Version VERSION = new Version("0.50.10");

    /** Parses a {@code <campaignOptions>} element with the supplied inner tags, mirroring on-disk save content. */
    private static Node campaignOptionsNode(final String innerXml) {
        final String xml = "<campaignOptions>" + innerXml + "</campaignOptions>";
        final Document document = assertDoesNotThrow(() -> MHQXMLUtility.parseDocument(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        return document.getElementsByTagName("campaignOptions").item(0);
    }

    @Test
    void existingCampaignWithoutTag_keepsCamOpsReputation() {
        // A save predating the option has no <useChaosReputation> tag; the parsed value falls back to the (true)
        // default, which must be overridden so the campaign is not flipped onto Chaos Reputation.
        Node node = campaignOptionsNode("<manualUnitRatingModifier>0</manualUnitRatingModifier>");
        CampaignOptions options = CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);
        assertTrue(options.get(CampaignOption.USE_CHAOS_REPUTATION), "default should start enabled before migration");

        CampaignXmlParser.preserveLegacyReputationForExistingCampaigns(node, options);

        assertFalse(options.get(CampaignOption.USE_CHAOS_REPUTATION),
              "a save without the tag must keep CamOps reputation");
    }

    @Test
    void newFormatSaveWithTagEnabled_keepsChaosReputation() {
        Node node = campaignOptionsNode("<useChaosReputation>true</useChaosReputation>");
        CampaignOptions options = CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);

        CampaignXmlParser.preserveLegacyReputationForExistingCampaigns(node, options);

        assertTrue(options.get(CampaignOption.USE_CHAOS_REPUTATION), "an explicit tag value must be honored");
    }

    @Test
    void newFormatSaveWithTagDisabled_staysDisabled() {
        Node node = campaignOptionsNode("<useChaosReputation>false</useChaosReputation>");
        CampaignOptions options = CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);

        CampaignXmlParser.preserveLegacyReputationForExistingCampaigns(node, options);

        assertFalse(options.get(CampaignOption.USE_CHAOS_REPUTATION), "an explicit tag value must be honored");
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    void legacySaveWithoutNameGenerationTags_takesTheValuesTheNameGenBlockSet() {
        final int originalPercentFemale = RandomGenderGenerator.getPercentFemale();
        final String originalChosenFaction = RandomNameGenerator.getInstance().getChosenFaction();
        try {
            // The <nameGen> block, parsed earlier in <info>, has already written these into the generators
            RandomGenderGenerator.setPercentFemale(37);
            RandomNameGenerator.getInstance().setChosenFaction("FS");
            Node node = campaignOptionsNode("<manualUnitRatingModifier>0</manualUnitRatingModifier>");
            CampaignOptions options = CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);

            CampaignXmlParser.preserveLegacyNameGenerationSettings(node, options);

            assertEquals(37, options.get(CampaignOption.PERCENT_FEMALE));
            assertEquals("FS", options.get(CampaignOption.NAME_GENERATOR_FACTION));
        } finally {
            RandomGenderGenerator.setPercentFemale(originalPercentFemale);
            RandomNameGenerator.getInstance().setChosenFaction(originalChosenFaction);
        }
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    void newFormatSaveWithNameGenerationTags_keepsItsOwnValues() {
        final int originalPercentFemale = RandomGenderGenerator.getPercentFemale();
        final String originalChosenFaction = RandomNameGenerator.getInstance().getChosenFaction();
        try {
            RandomGenderGenerator.setPercentFemale(37);
            RandomNameGenerator.getInstance().setChosenFaction("FS");
            Node node = campaignOptionsNode("<percentFemale>64</percentFemale>"
                                                  + "<nameGeneratorFaction>CC</nameGeneratorFaction>");
            CampaignOptions options = CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);

            CampaignXmlParser.preserveLegacyNameGenerationSettings(node, options);

            assertEquals(64, options.get(CampaignOption.PERCENT_FEMALE));
            assertEquals("CC", options.get(CampaignOption.NAME_GENERATOR_FACTION));
        } finally {
            RandomGenderGenerator.setPercentFemale(originalPercentFemale);
            RandomNameGenerator.getInstance().setChosenFaction(originalChosenFaction);
        }
    }

    /** Tests for {@link CampaignXmlParser#migrateLegacyEducationTravel} journey-stage conversion. */
    @Nested
    class LegacyEducationTravelMigration {
        private static final String ACADEMY_SET = "TestSet";
        private static final String ACADEMY_NAME = "Test Academy";

        Campaign campaign;
        Detachment forceDetachment;
        AcademyCampusLocation campus;
        PlanetarySystem academySystem;
        PlanetarySystem campaignSystem;
        Person person;

        @BeforeEach
        void setUp() {
            campaign = mockCampaign();
            PlayerForce playerForce = mock(PlayerForce.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            forceDetachment = new Detachment();
            when(playerForce.getForceDetachment()).thenReturn(forceDetachment);
            when(playerForce.getPersonnel()).thenReturn(forceDetachment.getPersonnel());

            CampaignLocationManager locationManager = mock(CampaignLocationManager.class);
            when(campaign.getCampaignLocationManager()).thenReturn(locationManager);
            campus = new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME);
            when(locationManager.getOrCreateCampusLocation(any(), any(), any(), any())).thenReturn(campus);

            academySystem = mock(PlanetarySystem.class);
            when(academySystem.getId()).thenReturn("AcademySystem");
            when(campaign.getSystemById("AcademySystem")).thenReturn(academySystem);
            campaignSystem = mock(PlanetarySystem.class);
            when(campaignSystem.getId()).thenReturn("CampaignSystem");
            when(campaign.getCurrentSystem()).thenReturn(campaignSystem);

            // Mirror a loaded campaign: nodes can only be parented into trees rooted at a location.
            forceDetachment.setParent(new CurrentLocation(campaignSystem, 0));
            campus.setParent(new CurrentLocation(academySystem, 0));

            person = new Person("Test", "Student", null, "MERC");
            person.setEduAcademySet(ACADEMY_SET);
            person.setEduAcademyNameInSet(ACADEMY_NAME);
            person.setEduAcademySystem("AcademySystem");
            person.setEduJourneyTime(92);
            person.setEduDaysOfTravel(64);
            person.setParent(forceDetachment.getPersonnel());
            forceDetachment.getPersonnel().putIfAbsent(person.getId(), person);
        }

        private CurrentLocation migrate() {
            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of());

                CampaignXmlParser.migrateLegacyEducationTravel(campaign);
            }
            return assertInstanceOf(CurrentLocation.class, person.getCurrentLocation());
        }

        @Test
        void journeyFromCampus_travelsToCampaignSystemWithTheMainForce() {
            person.setEduEducationStage(EducationStage.JOURNEY_FROM_CAMPUS);

            CurrentLocation travelNode = migrate();

            assertSame(campaignSystem, travelNode.getCurrentSystem());
            assertEquals(28.0, travelNode.getTransitTime());
            assertEquals(List.of(campaignSystem), travelNode.getJumpPath().getSystems());
            assertSame(forceDetachment, travelNode.getParentLocation());
        }

        @Test
        void journeyToCampus_travelsToAcademySystemUnderTheCampus() {
            person.setEduEducationStage(EducationStage.JOURNEY_TO_CAMPUS);

            CurrentLocation travelNode = migrate();

            assertSame(academySystem, travelNode.getCurrentSystem());
            assertEquals(28.0, travelNode.getTransitTime());
            assertEquals(List.of(academySystem), travelNode.getJumpPath().getSystems());
            assertSame(campus, travelNode.getParentLocation());
        }

        @Test
        void journeyAlreadyComplete_arrivesWithoutPath() {
            person.setEduEducationStage(EducationStage.JOURNEY_FROM_CAMPUS);
            person.setEduDaysOfTravel(92);

            CurrentLocation travelNode = migrate();

            assertTrue(travelNode.isOnPlanet());
            assertNull(travelNode.getJumpPath());
        }
    }

    /**
     * The &lt;51.02 handler that records the prosthetics behind triple-strength myomer and leg MASC. It runs once loading
     * is done, so it reaches saves in either personnel format.
     */
    @Nested
    class DerivedImplantProstheticMigration {
        Campaign campaign;
        Person person;

        @BeforeEach
        void setUp() {
            campaign = MHQTestUtilities.getTestCampaign();
            campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, true);
            person = new Person(campaign);
            person.getOptions().acquireAbility(PersonnelOptions.MD_ADVANTAGES, OptionsConstants.MD_PL_MASC, true);
            campaign.getPlayerForce().getHumanResources().importPerson(person);
        }

        @Test
        void saveFromBeforeTheChange_recordsTheProsthetics() {
            CampaignXmlParser.migrateDerivedImplantProsthetics(campaign, new Version("0.51.01"));

            assertEquals(2, person.getProstheticInjuries().size());
        }

        @Test
        void saveFromAfterTheChange_isLeftAlone() {
            CampaignXmlParser.migrateDerivedImplantProsthetics(campaign, new Version("0.51.02"));

            assertTrue(person.getProstheticInjuries().isEmpty());
        }

        @Test
        void campaignWithoutAlternateMedical_isLeftAlone() {
            campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, false);

            CampaignXmlParser.migrateDerivedImplantProsthetics(campaign, new Version("0.51.01"));

            assertTrue(person.getProstheticInjuries().isEmpty());
        }
    }
}
