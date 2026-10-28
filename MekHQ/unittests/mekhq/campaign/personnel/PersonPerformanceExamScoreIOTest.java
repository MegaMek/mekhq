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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.randomEvents.personalities.Reasoning;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/**
 * Verifies a person's performance exam score survives being saved and loaded.
 *
 * <p>The loader once looked for a different tag name than the one the writer emits, so the score was never read back
 * and every person reloaded with the default of 50.</p>
 */
class PersonPerformanceExamScoreIOTest {

    private static final Version VERSION = new Version("0.50.10");

    @BeforeAll
    static void loadRankSystems() {
        // The person loader resolves a rank system by code, which needs the shipped rank data.
        Ranks.initializeRankSystems();
    }

    private static Campaign campaign() {
        Campaign campaign = mockCampaign();
        when(campaign.getVersion()).thenReturn(VERSION);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3067, 1, 1));
        when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        return campaign;
    }

    private static Person roundTrip(Person person, Campaign campaign) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(bytes, true, StandardCharsets.UTF_8)) {
            person.writeToXML(writer, 0, campaign);
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes.toByteArray())) {
            Element element = MHQXMLUtility.newSafeDocumentBuilder().parse(input).getDocumentElement();
            element.normalize();
            Person loaded = Person.generateInstanceFromXML(element, campaign, VERSION);
            assertNotNull(loaded, "the person must load back");
            return loaded;
        }
    }

    /** The regression: a non-default score used to reload as 50. */
    @Test
    void nonDefaultScoreSurvivesASave() throws Exception {
        Campaign campaign = campaign();
        Person person = new Person("", "Jane", "Smith", "", campaign, "MERC");
        person.setPerformanceExamScore(87);

        assertEquals(87, roundTrip(person, campaign).getPerformanceExamScore());
    }

    /** A non-average reasoning is written alongside the score and must not overwrite it on a current-version load. */
    @Test
    void scoreIsKeptWhenReasoningIsAlsoSaved() throws Exception {
        Campaign campaign = campaign();
        Person person = new Person("", "Jane", "Smith", "", campaign, "MERC");
        person.setReasoning(Reasoning.BRAIN_DEAD);
        person.setPerformanceExamScore(73);

        Person loaded = roundTrip(person, campaign);
        assertEquals(Reasoning.BRAIN_DEAD, loaded.getReasoning());
        assertEquals(73, loaded.getPerformanceExamScore());
    }
}
