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
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/**
 * Verifies a warrior's implants survive being saved and loaded.
 *
 * <p>Enhanced imaging is held in an option group of its own rather than with the Manei Domini
 * implants, and only that group was written, so an implanted Clan warrior came back from a save with
 * no implant. Both groups now go into the one {@code implants} tag, which the loader restores by
 * looking each name up across every group.</p>
 */
class PersonImplantPersistenceTest {

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
        when(campaign.getCampaignOptions()).thenReturn(mock(CampaignOptions.class));
        return campaign;
    }

    private static Person warrior(Campaign campaign) {
        return new Person("", "Jane", "Smith", "", campaign, "CSJ");
    }

    private static Person roundTrip(Person person, Campaign campaign) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(bytes, true, StandardCharsets.UTF_8)) {
            person.writeToXML(writer, 0, campaign);
        }
        try (ByteArrayInputStream input =
                   new ByteArrayInputStream(bytes.toString(StandardCharsets.UTF_8)
                                                  .getBytes(StandardCharsets.UTF_8))) {
            Element element = MHQXMLUtility.newSafeDocumentBuilder().parse(input).getDocumentElement();
            element.normalize();
            Person loaded = Person.generateInstanceFromXML(element, campaign, VERSION);
            assertNotNull(loaded, "the person must load back");
            return loaded;
        }
    }

    private static boolean hasEnhancedImaging(Person person) {
        return person.getOptions().booleanOption(OptionsConstants.MD_EI_IMPLANT);
    }

    /** The regression: an enhanced imaging warrior used to reload with no implant at all. */
    @Test
    void enhancedImagingSurvivesASave() throws Exception {
        Campaign campaign = campaign();
        Person person = warrior(campaign);
        person.getOptions().getOption(OptionsConstants.MD_EI_IMPLANT).setValue(true);

        assertTrue(hasEnhancedImaging(roundTrip(person, campaign)),
              "the implant must still be there after a save and load");
    }

    /** The Manei Domini implants shared the tag before and must keep working. */
    @Test
    void maneiDominiImplantsStillSurviveASave() throws Exception {
        Campaign campaign = campaign();
        Person person = warrior(campaign);
        person.getOptions().getOption(OptionsConstants.MD_PAIN_SHUNT).setValue(true);

        Person loaded = roundTrip(person, campaign);
        assertTrue(loaded.getOptions().booleanOption(OptionsConstants.MD_PAIN_SHUNT));
        assertFalse(hasEnhancedImaging(loaded), "nothing the warrior did not carry is added");
    }

    /** A warrior carrying both kinds gets both back, the two groups sharing one tag. */
    @Test
    void bothKindsOfImplantSurviveTogether() throws Exception {
        Campaign campaign = campaign();
        Person person = warrior(campaign);
        person.getOptions().getOption(OptionsConstants.MD_EI_IMPLANT).setValue(true);
        person.getOptions().getOption(OptionsConstants.MD_PAIN_SHUNT).setValue(true);

        Person loaded = roundTrip(person, campaign);
        assertTrue(hasEnhancedImaging(loaded));
        assertTrue(loaded.getOptions().booleanOption(OptionsConstants.MD_PAIN_SHUNT));
        assertEquals(2, loaded.countImplants(), "both implants are counted as implants");
    }

    /** A warrior with no implants writes no tag, so a save is unchanged by any of this. */
    @Test
    void aWarriorWithNoImplantsWritesNothing() throws Exception {
        Campaign campaign = campaign();
        Person loaded = roundTrip(warrior(campaign), campaign);

        assertEquals(0, loaded.countImplants());
        assertTrue(loaded.implantOptionList().isEmpty());
    }
}
