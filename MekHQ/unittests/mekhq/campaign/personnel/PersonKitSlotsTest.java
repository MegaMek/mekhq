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

import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_BASIC_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_DESCARTES_MK_XXV;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_NOTEPUTER;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_PERSONAL_COMPUTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.quartermaster.KitSlot;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Coverage for {@link Person}'s two equipment-kit slots: the slot accessors route to the right field, ownership checks
 * both slots, and all four kit fields survive a write/read round trip — including a legacy save that only knew the
 * single {@code repairKitName} slot.
 */
class PersonKitSlotsTest {
    private static final String[] KIT_TAGS = { "repairKitName", "intendedRepairKitName", "secondaryKitName",
                                               "intendedSecondaryKitName" };

    private static Person newPerson() {
        return new Person("Given", "Sur", mockCampaign(), "MERC");
    }

    private static Person parsePerson(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Campaign campaign = mockCampaign();
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn("MERC");
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
        when(campaign.getVersion()).thenReturn(new Version("0.51.1"));

        return Person.generateInstanceFromXML(doc.getDocumentElement(), campaign, new Version("0.51.1"));
    }

    /** Just the kit tags the writer emitted, so the round trip does not depend on unrelated fields. */
    private static String kitTags(String xml) {
        StringBuilder tags = new StringBuilder();
        for (String tag : KIT_TAGS) {
            Matcher matcher = Pattern.compile("<" + tag + ">[^<]*</" + tag + ">").matcher(xml);
            if (matcher.find()) {
                tags.append(matcher.group());
            }
        }
        return tags.toString();
    }

    // region slot accessors
    @Test
    void slotAccessorsRouteToTheirOwnFields() {
        Person person = newPerson();
        person.setKitName(KitSlot.PRIMARY, KIT_BASIC_TOOLKIT);
        person.setKitName(KitSlot.SECONDARY, KIT_PERSONAL_COMPUTER);
        person.setIntendedKitName(KitSlot.PRIMARY, KIT_DESCARTES_MK_XXV);
        person.setIntendedKitName(KitSlot.SECONDARY, KIT_NOTEPUTER);

        assertEquals(KIT_BASIC_TOOLKIT, person.getRepairKitName());
        assertEquals(KIT_PERSONAL_COMPUTER, person.getSecondaryKitName());
        assertEquals(KIT_DESCARTES_MK_XXV, person.getIntendedRepairKitName());
        assertEquals(KIT_NOTEPUTER, person.getIntendedSecondaryKitName());

        assertEquals(KIT_BASIC_TOOLKIT, person.getKitName(KitSlot.PRIMARY));
        assertEquals(KIT_PERSONAL_COMPUTER, person.getKitName(KitSlot.SECONDARY));
        assertEquals(KIT_DESCARTES_MK_XXV, person.getIntendedKitName(KitSlot.PRIMARY));
        assertEquals(KIT_NOTEPUTER, person.getIntendedKitName(KitSlot.SECONDARY));
    }

    @Test
    void clearingOneSlotLeavesTheOtherAlone() {
        Person person = newPerson();
        person.setKitName(KitSlot.PRIMARY, KIT_BASIC_TOOLKIT);
        person.setKitName(KitSlot.SECONDARY, KIT_PERSONAL_COMPUTER);

        person.setKitName(KitSlot.PRIMARY, null);

        assertNull(person.getRepairKitName());
        assertEquals(KIT_PERSONAL_COMPUTER, person.getSecondaryKitName());
    }

    @Test
    void hasRepairKitMatchesEitherSlot() {
        Person person = newPerson();
        person.setKitName(KitSlot.PRIMARY, KIT_BASIC_TOOLKIT);
        person.setKitName(KitSlot.SECONDARY, KIT_PERSONAL_COMPUTER);

        assertTrue(person.hasRepairKit(KIT_BASIC_TOOLKIT));
        assertTrue(person.hasRepairKit(KIT_PERSONAL_COMPUTER));
        assertFalse(person.hasRepairKit(KIT_NOTEPUTER));
        assertFalse(person.hasRepairKit(null));
    }

    @Test
    void anAwaitedKitIsNotOwned() {
        Person person = newPerson();
        person.setIntendedKitName(KitSlot.SECONDARY, KIT_PERSONAL_COMPUTER);
        assertFalse(person.hasRepairKit(KIT_PERSONAL_COMPUTER));
    }
    // endregion slot accessors

    // region XML
    @Test
    void allFourKitFieldsSurviveAWriteReadRoundTrip() throws Exception {
        Campaign campaign = mockCampaign();
        Person person = new Person("Given", "Sur", campaign, "MERC");
        person.setKitName(KitSlot.PRIMARY, KIT_BASIC_TOOLKIT);
        person.setKitName(KitSlot.SECONDARY, KIT_PERSONAL_COMPUTER);
        person.setIntendedKitName(KitSlot.PRIMARY, KIT_DESCARTES_MK_XXV);
        person.setIntendedKitName(KitSlot.SECONDARY, KIT_NOTEPUTER);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        person.writeToXML(pw, 0, campaign);
        pw.flush();
        String tags = kitTags(sw.toString());
        for (String tag : KIT_TAGS) {
            assertTrue(tags.contains("<" + tag + ">"), "writer should emit " + tag);
        }

        Person loaded = parsePerson("<person><givenName>x</givenName>" + tags + "</person>");
        assertEquals(KIT_BASIC_TOOLKIT, loaded.getRepairKitName());
        assertEquals(KIT_PERSONAL_COMPUTER, loaded.getSecondaryKitName());
        assertEquals(KIT_DESCARTES_MK_XXV, loaded.getIntendedRepairKitName());
        assertEquals(KIT_NOTEPUTER, loaded.getIntendedSecondaryKitName());
    }

    @Test
    void emptySlotsAreNotWritten() {
        Campaign campaign = mockCampaign();
        Person person = new Person("Given", "Sur", campaign, "MERC");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        person.writeToXML(pw, 0, campaign);
        pw.flush();

        assertEquals("", kitTags(sw.toString()));
    }

    @Test
    void aLegacySaveWithOnlyTheSingleSlotLoadsIntoThePrimarySlot() throws Exception {
        Person loaded = parsePerson("""
              <person>
                <givenName>Legacy</givenName>
                <repairKitName>Basic Toolkit</repairKitName>
              </person>
              """);

        assertEquals(KIT_BASIC_TOOLKIT, loaded.getKitName(KitSlot.PRIMARY));
        assertNull(loaded.getKitName(KitSlot.SECONDARY));
        assertNull(loaded.getIntendedKitName(KitSlot.SECONDARY));
    }
    // endregion XML
}
