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
package mekhq.campaign.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import megamek.Version;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.TestSystems;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import testUtilities.MHQTestUtilities;

/**
 * Pins the behaviour that keeps a long campaign's save from filling up with recruits' units nobody can hire any more:
 * a saved unit with no matching recruit is skipped on load, dropped on the daily refresh, and the market itself is
 * written into the save exactly once.
 */
class PersonnelMarketTest {
    /** Any version at or above the current release; keeps the version-gated compatibility branches dormant. */
    private static final Version VERSION = new Version(999, 0, 0);

    private Campaign campaign;

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        try {
            Factions.setInstance(Factions.loadDefault(true));
            Systems.setInstance(TestSystems.loadDefault());
        } catch (Exception exception) {
            LogManager.getLogger().error("", exception);
        }
    }

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();
    }

    @Test
    void savedUnitWithNoRecruitIsSkippedWithoutStoppingTheLoad() throws Exception {
        UUID orphanId = UUID.randomUUID();
        // The unit comes before the recruit type on purpose: the old loader tried to load it on the spot, and an
        // unknown unit name threw and abandoned everything after it.
        String xml = "<personnelMarket>"
              + "<entity id=\"" + orphanId + "\">No Such Unit XYZ-1</entity>"
              + "<paidRecruitType>ADMINISTRATOR</paidRecruitType>"
              + "</personnelMarket>";
        Document document = MHQXMLUtility.parseDocument(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        PersonnelMarket market = PersonnelMarket.generateInstanceFromXML(document.getDocumentElement(),
              campaign,
              VERSION);

        assertNotNull(market);
        assertTrue(market.getPersonnel().isEmpty(), "no recruit was saved, so none is loaded");
        assertNull(market.getAttachedEntity(orphanId), "a unit with no recruit is not loaded");
        assertEquals(PersonnelRole.ADMINISTRATOR, market.getPaidRecruitRole(),
              "the nodes after the orphaned unit are still read");
    }

    @Test
    void purgeKeepsTheRecruitsUnitAndDropsTheOrphan() {
        PersonnelMarket market = new PersonnelMarket();
        UUID recruitId = UUID.randomUUID();
        UUID orphanId = UUID.randomUUID();
        Person recruit = mock(Person.class);
        when(recruit.getId()).thenReturn(recruitId);
        Entity recruitUnit = mock(Entity.class);
        Entity orphanUnit = mock(Entity.class);
        market.addPerson(recruit);
        market.addAttachedEntity(recruitId, recruitUnit);
        market.addAttachedEntity(orphanId, orphanUnit);

        market.purgeOrphanedEntities();

        assertSame(recruitUnit, market.getAttachedEntity(recruitId));
        assertNull(market.getAttachedEntity(orphanId));
    }

    @Test
    void campaignWritesThePersonnelMarketOnce() {
        StringWriter output = new StringWriter();
        campaign.writeToXML(new PrintWriter(output), false);

        long marketBlocks = output.toString()
                                  .lines()
                                  .filter(line -> line.trim().equals("<personnelMarket>"))
                                  .count();

        assertEquals(1L, marketBlocks);
    }
}
