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
package mekhq.gui.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.ArrayList;
import java.util.List;

import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignLocationManager;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class PersonnelStateColorsTest {
    private final Campaign campaign = mockCampaign();
    private final CampaignOptions options = new CampaignOptions();
    private final CampaignLocationManager locationManager = mock(CampaignLocationManager.class);
    private final MHQOptions mhqOptions = mock(MHQOptions.class);
    private final MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
    private final List<String> reasons = new ArrayList<>();

    private final ComponentColors absent = mock(ComponentColors.class);
    private final ComponentColors gone = mock(ComponentColors.class);
    private final ComponentColors deployed = mock(ComponentColors.class);
    private final ComponentColors queued = mock(ComponentColors.class);
    private final ComponentColors away = mock(ComponentColors.class);
    private final ComponentColors injured = mock(ComponentColors.class);
    private final ComponentColors pregnant = mock(ComponentColors.class);
    private final ComponentColors fatigued = mock(ComponentColors.class);
    private final ComponentColors healed = mock(ComponentColors.class);

    PersonnelStateColorsTest() {
        mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);
        when(mhqOptions.getAbsentColors()).thenReturn(absent);
        when(mhqOptions.getGoneColors()).thenReturn(gone);
        when(mhqOptions.getDeployedColors()).thenReturn(deployed);
        when(mhqOptions.getQueuedForTravelColors()).thenReturn(queued);
        when(mhqOptions.getAwayFromMainForceColors()).thenReturn(away);
        when(mhqOptions.getInjuredColors()).thenReturn(injured);
        when(mhqOptions.getPregnantColors()).thenReturn(pregnant);
        when(mhqOptions.getFatiguedColors()).thenReturn(fatigued);
        when(mhqOptions.getHealedInjuriesColors()).thenReturn(healed);

        options.set(CampaignOption.USE_ADVANCED_MEDICAL, false);
        options.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, false);
        options.set(CampaignOption.USE_FATIGUE, false);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getCampaignLocationManager()).thenReturn(locationManager);
    }

    @AfterEach
    void closeStaticMocks() {
        mekHQ.close();
    }

    private static Person person(PersonnelStatus status) {
        Person person = mock(Person.class);
        when(person.getStatus()).thenReturn(status);
        return person;
    }

    private ComponentColors colorsFor(Person person) {
        return PersonnelStateColors.getStateColors(campaign, person, reasons);
    }

    @Test
    void healthyActivePersonIsUncolored() {
        assertNull(colorsFor(person(PersonnelStatus.ACTIVE)));
        assertTrue(reasons.isEmpty());
    }

    @Test
    void absentPerson() {
        assertSame(absent, colorsFor(person(PersonnelStatus.ON_LEAVE)));
        assertEquals(List.of("colorReason.personnel.absent"), reasons);
    }

    @Test
    void departedPerson() {
        assertSame(gone, colorsFor(person(PersonnelStatus.RETIRED)));
        assertEquals(List.of("colorReason.personnel.departed"), reasons);
    }

    @Test
    void deployedPerson() {
        Person person = person(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(true);

        assertSame(deployed, colorsFor(person));
        assertEquals(List.of("colorReason.personnel.deployed"), reasons);
    }

    @Test
    void personQueuedForTravel() {
        Person person = person(PersonnelStatus.ACTIVE);
        when(person.isQueuedForTravel(locationManager)).thenReturn(true);

        assertSame(queued, colorsFor(person));
        assertEquals(List.of("colorReason.personnel.queuedForTravel"), reasons);
    }

    @Test
    void personAwayFromTheMainForceIsColoredAndExplained() {
        Person person = person(PersonnelStatus.ACTIVE);
        when(person.hasLocationNode()).thenReturn(true);
        when(person.getParentLocation()).thenReturn(mock(AbstractLocation.class));

        assertSame(away, colorsFor(person));
        assertEquals(List.of("colorReason.personnel.awayFromMainForce"), reasons);
    }

    @Test
    void injuredPersonUnderStandardMedical() {
        Person person = person(PersonnelStatus.ACTIVE);
        when(person.getHits()).thenReturn(2);

        assertSame(injured, colorsFor(person));
        assertEquals(List.of("colorReason.personnel.injured"), reasons);
    }

    @Test
    void injuredPersonUnderAdvancedMedical() {
        options.set(CampaignOption.USE_ADVANCED_MEDICAL, true);
        Person person = person(PersonnelStatus.ACTIVE);
        when(person.hasInjuries(true)).thenReturn(true);

        assertSame(injured, colorsFor(person));
    }

    @Test
    void hitsDontCountUnderAdvancedMedical() {
        options.set(CampaignOption.USE_ADVANCED_MEDICAL, true);
        Person person = person(PersonnelStatus.ACTIVE);
        when(person.getHits()).thenReturn(2);

        assertNull(colorsFor(person));
    }

    @Test
    void pregnantPerson() {
        Person person = person(PersonnelStatus.ACTIVE);
        when(person.isPregnant()).thenReturn(true);

        assertSame(pregnant, colorsFor(person));
        assertEquals(List.of("colorReason.personnel.pregnant"), reasons);
    }

    @Test
    void fatigueIsOnlyHighlightedFromFivePoints() {
        options.set(CampaignOption.USE_FATIGUE, true);
        Person tired = person(PersonnelStatus.ACTIVE);
        Person rested = person(PersonnelStatus.ACTIVE);

        try (MockedStatic<Fatigue> fatigue = mockStatic(Fatigue.class)) {
            fatigue.when(() -> Fatigue.getEffectiveFatigue(tired, campaign)).thenReturn(5);
            fatigue.when(() -> Fatigue.getEffectiveFatigue(rested, campaign)).thenReturn(4);

            assertSame(fatigued, colorsFor(tired));
            assertEquals(List.of("colorReason.personnel.fatigued"), reasons);
            reasons.clear();
            assertNull(colorsFor(rested));
        }
    }

    @Test
    void fatigueIsIgnoredWhenFatigueIsOff() {
        Person person = person(PersonnelStatus.ACTIVE);

        try (MockedStatic<Fatigue> fatigue = mockStatic(Fatigue.class)) {
            fatigue.when(() -> Fatigue.getEffectiveFatigue(any(), any())).thenReturn(20);

            assertNull(colorsFor(person));
        }
    }

    @Test
    void healedPermanentInjuries() {
        Person person = person(PersonnelStatus.ACTIVE);
        when(person.hasNonProstheticPermanentInjuries(false)).thenReturn(true);

        assertSame(healed, colorsFor(person));
        assertEquals(List.of("colorReason.personnel.healedInjuries"), reasons);
    }

    @Test
    void firstMatchSetsTheColorButEveryReasonIsListed() {
        Person person = person(PersonnelStatus.ON_LEAVE);
        when(person.getHits()).thenReturn(1);
        when(person.isPregnant()).thenReturn(true);

        assertSame(absent, colorsFor(person));
        assertEquals(List.of("colorReason.personnel.absent", "colorReason.personnel.injured",
              "colorReason.personnel.pregnant"), reasons);
    }

    @Test
    void reasonsAreLocalizedAndJoinedWithLineBreaks() {
        String text = PersonnelStateColors.getColorReasonsText(List.of("colorReason.personnel.absent",
              "colorReason.personnel.awayFromMainForce"));

        String[] lines = text.split("<br>");
        assertEquals(2, lines.length, text);
        for (String line : lines) {
            assertFalse(line.isBlank());
            assertFalse(line.startsWith("!"), line);
        }
    }

    @Test
    void noReasonsGiveNoText() {
        assertEquals("", PersonnelStateColors.getColorReasonsText(List.of()));
    }
}
