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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/**
 * Verifies the {@link Person} senior appointment flags and, above all, that adding them cannot damage a
 * save written before they existed.
 *
 * <p>A campaign save is a long-lived artifact. Each flag is written only when held and read only when
 * present, so a save from an older build loads with every post unheld, and a campaign that appoints
 * nobody writes exactly what it wrote before.</p>
 */
class PersonSeniorAppointmentTest {

    private static final Version VERSION = new Version("0.50.10");

    @BeforeAll
    static void loadRankSystems() {
        // The person loader resolves a rank system by code, which needs the shipped rank data. Without
        // it the loader throws and hands back null, which would look like a serialization bug here.
        Ranks.initializeRankSystems();
    }

    /** The shared mock plus the version stub the person loader needs. */
    private static Campaign campaign() {
        Campaign campaign = mockCampaign();
        when(campaign.getVersion()).thenReturn(VERSION);
        // The person loader dates its subject against the campaign clock; an unstubbed null there makes
        // it fail silently and hand back null.
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3067, 1, 1));
        // The loader consults campaign options while restoring relationship state.
        when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        return campaign;
    }

    /** Serializes a person and returns the {@code <person>} element back. */
    private static Element roundTrip(Person person, Campaign campaign) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(bytes, true, StandardCharsets.UTF_8)) {
            person.writeToXML(writer, 0, campaign);
        }
        // writeToXML emits a complete <person> element, so it parses as its own document root.
        return parse(bytes.toString(StandardCharsets.UTF_8));
    }

    private static Element parse(String xml) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            Element element = MHQXMLUtility.newSafeDocumentBuilder().parse(input).getDocumentElement();
            element.normalize();
            return element;
        }
    }

    /** Whether the serialized element carries {@code tag}, counting the element itself. */
    private static boolean containsTag(Element element, String tag) {
        return element.getElementsByTagName(tag).getLength() > 0;
    }

    @Test
    void everyPostIsUnheldOnANewPerson() {
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        assertFalse(person.isChiefMedicalOfficer());
        assertFalse(person.isHeadTechnician());
        assertFalse(person.isChiefAdministrator());
    }

    @Test
    void postsAreIndependentOfOneAnother() {
        // Nothing stops one person holding two posts - a small command might have the same person as
        // head technician and chief administrator - so setting one must not disturb the others.
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        person.setChiefMedicalOfficer(true);
        assertTrue(person.isChiefMedicalOfficer());
        assertFalse(person.isHeadTechnician());
        assertFalse(person.isChiefAdministrator());
    }

    @Test
    void heldPostsSurviveTheSaveAndLoadRoundTrip() throws Exception {
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        person.setChiefMedicalOfficer(true);
        person.setChiefAdministrator(true);

        Campaign campaign = campaign();
        Element written = roundTrip(person, campaign);
        assertTrue(containsTag(written, "chiefMedicalOfficer"));
        assertTrue(containsTag(written, "chiefAdministrator"));

        Person loaded = Person.generateInstanceFromXML(written, campaign, VERSION);
        assertNotNull(loaded);
        assertTrue(loaded.isChiefMedicalOfficer(), "the CMO post should survive a save and load");
        assertTrue(loaded.isChiefAdministrator());
        assertFalse(loaded.isHeadTechnician(), "an unheld post should not come back held");
    }

    @Test
    void noTagIsWrittenForAnUnheldPost() throws Exception {
        // A campaign that appoints nobody must write the save it wrote before these flags existed,
        // rather than four extra tags on every person in the roster.
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        Element written = roundTrip(person, campaign());
        assertFalse(containsTag(written, "chiefMedicalOfficer"));
        assertFalse(containsTag(written, "headTechnician"));
        assertFalse(containsTag(written, "chiefAdministrator"));
    }

    @Test
    void aSaveWithoutTheTagsLoadsWithEveryPostUnheld() throws Exception {
        // The old-save case, stated directly: a person element from a build that predates these flags.
        String legacyPerson = """
              <person>
                  <givenName>Jane</givenName>
                  <surname>Smith</surname>
              </person>
              """;
        Element element = parse(legacyPerson);
        Person loaded = Person.generateInstanceFromXML(element, campaign(), VERSION);
        assertNotNull(loaded, "a person from an older save should still load");
        assertFalse(loaded.isChiefMedicalOfficer(), "and must not come back holding a post");
        assertFalse(loaded.isHeadTechnician());
        assertFalse(loaded.isChiefAdministrator());
    }

    @Test
    void aPersonHoldingNoPostShowsNoAppointmentText() {
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        assertEquals("", person.getSeniorAppointmentAbbreviations());
        assertEquals("", person.getSeniorAppointmentTitles());
    }

    @Test
    void eachPostHasAnAbbreviationAndAFullTitle() {
        Person chiefMedicalOfficer = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        chiefMedicalOfficer.setChiefMedicalOfficer(true);
        assertEquals("CMO", chiefMedicalOfficer.getSeniorAppointmentAbbreviations());
        assertEquals("Chief Medical Officer", chiefMedicalOfficer.getSeniorAppointmentTitles());

        Person headTechnician = new Person("", "John", "Smith", "", campaign(), "MERC");
        headTechnician.setHeadTechnician(true);
        assertEquals("HT", headTechnician.getSeniorAppointmentAbbreviations());
        assertEquals("Head Technician", headTechnician.getSeniorAppointmentTitles());

        Person chiefAdministrator = new Person("", "Alex", "Smith", "", campaign(), "MERC");
        chiefAdministrator.setChiefAdministrator(true);
        assertEquals("CA", chiefAdministrator.getSeniorAppointmentAbbreviations());
        assertEquals("Chief Administrator", chiefAdministrator.getSeniorAppointmentTitles());
    }

    @Test
    void severalPostsHeldByOnePersonAreListedTogether() {
        // A small command can put the same person in charge of more than one section.
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        person.setChiefMedicalOfficer(true);
        person.setChiefAdministrator(true);

        assertEquals("CMO, CA", person.getSeniorAppointmentAbbreviations(),
              "posts should be listed in a fixed order so the display does not reshuffle");
        assertEquals("Chief Medical Officer, Chief Administrator", person.getSeniorAppointmentTitles());
    }

    @Test
    void theDepartmentHeadTitleNamesTheDepartment() {
        // The title is built through MessageFormat, so a printf-style placeholder in the resource
        // bundle would survive into the display untouched. This is what catches that.
        Campaign campaign = campaign();
        Person person = new Person("", "Jane", "Smith", "", campaign, "MERC");
        person.setPrimaryRole(LocalDate.of(3067, 1, 1), PersonnelRole.MEK_TECH);
        person.setDepartmentHead(true);

        String title = person.getDepartmentHeadTitle();
        assertFalse(title.contains("%s"), "the placeholder should have been substituted, got: " + title);
        assertFalse(title.contains("{0}"), "the placeholder should have been substituted, got: " + title);
        assertTrue(title.startsWith("Head "), "got: " + title);
        assertTrue(title.length() > "Head ".length(), "the department should be named, got: " + title);
    }

    @Test
    void aPersonHeadingNoDepartmentHasNoDepartmentTitle() {
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        person.setPrimaryRole(LocalDate.of(3067, 1, 1), PersonnelRole.MEK_TECH);
        assertEquals("", person.getDepartmentHeadTitle());
    }

    @Test
    void aDepartmentHeadFlagSurvivesTheSaveAndLoadRoundTrip() throws Exception {
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        person.setDepartmentHead(true);

        Campaign campaign = campaign();
        Element written = roundTrip(person, campaign);
        assertTrue(containsTag(written, "departmentHead"));

        Person loaded = Person.generateInstanceFromXML(written, campaign, VERSION);
        assertNotNull(loaded);
        assertTrue(loaded.isDepartmentHead(), "the department head flag should survive a save and load");
    }

    @Test
    void aBloodhouseIsSeparateFromTheBloodnameEarnedFromIt() {
        // The distinction the two fields exist for: every trueborn descends from a House, and only
        // those who win a Trial of Bloodright carry its name.
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        person.setBloodhouse("Ward");

        assertTrue(person.hasBloodhouse());
        assertEquals("Ward", person.getBloodhouse());
        assertTrue(isNullOrBlankString(person.getBloodname()),
              "descent alone must not give the warrior the name");
    }

    @Test
    void aBloodhouseDoesNotBecomePartOfTheName() {
        // Winning the name changes what the warrior is called; descent alone does not.
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        String nameBeforeHouse = person.getFullName();
        person.setBloodhouse("Ward");
        assertEquals(nameBeforeHouse, person.getFullName(),
              "a warrior of the Ward House is not called Ward until they win the name");
    }

    @Test
    void aBloodhouseSurvivesTheSaveAndLoadRoundTrip() throws Exception {
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        person.setBloodhouse("Kerensky");

        Campaign campaign = campaign();
        Element written = roundTrip(person, campaign);
        assertTrue(containsTag(written, "bloodhouse"));

        Person loaded = Person.generateInstanceFromXML(written, campaign, VERSION);
        assertNotNull(loaded);
        assertEquals("Kerensky", loaded.getBloodhouse());
    }

    @Test
    void aSaveWithNoBloodhouseWritesNoTagAndLoadsClean() throws Exception {
        // A campaign with no Clan personnel must write the save it always wrote, and one from an
        // older build must load with no descent rather than a wrong one.
        Person person = new Person("", "Jane", "Smith", "", campaign(), "MERC");
        Element written = roundTrip(person, campaign());
        assertFalse(containsTag(written, "bloodhouse"));

        Person loaded = Person.generateInstanceFromXML(written, campaign(), VERSION);
        assertNotNull(loaded);
        assertFalse(loaded.hasBloodhouse());
    }

    /** Local helper so the test does not depend on a particular string utility being imported. */
    private static boolean isNullOrBlankString(String value) {
        return (value == null) || value.isBlank();
    }
}
