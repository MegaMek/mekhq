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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import mekhq.EventSpy;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.persons.PersonStatusChangedEvent;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests that prisoners are removed from the campaign roster as soon as they die.
 *
 * @author Illiani
 * @since 0.51.01
 */
class PersonDeadPrisonerRemovalTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 1);

    private Campaign campaign;
    private ForceHumanResources humanResources;
    private Person person;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        when(campaign.getLocalDate()).thenReturn(TODAY);
        // Prerequisites for removeAllTechJobs, which every change to an inactive status runs
        when(campaign.getPlayerForce().getHangar()).thenReturn(new LocalHangar());
        when(campaign.getPlayerForce().getWarehouse()).thenReturn(new LocalWarehouse());

        Faction faction = mock(Faction.class);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(faction.getShortName()).thenReturn("MERC");

        person = new Person(campaign);
        person.setFullNameDirect("Unlucky Prisoner");

        humanResources = campaign.getPlayerForce().getHumanResources();
        when(humanResources.getPerson(person.getId())).thenReturn(person);
    }

    private void changeStatus(PersonnelStatus status) {
        try (EventSpy ignored = new EventSpy()) {
            person.changeStatus(campaign, TODAY, status, false);
        }
    }

    @ParameterizedTest
    @EnumSource(value = PrisonerStatus.class, names = { "PRISONER", "PRISONER_DEFECTOR" })
    void deadPrisonerIsRemoved(PrisonerStatus prisonerStatus) {
        person.setPrisonerStatusDirect(prisonerStatus);

        changeStatus(PersonnelStatus.MEDICAL_COMPLICATIONS);

        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
        assertEquals(TODAY, person.getDateOfDeath());
        verify(humanResources).removePerson(campaign, person, false);
    }

    @ParameterizedTest
    @EnumSource(value = PrisonerStatus.class, names = { "FREE", "BONDSMAN", "BECOMING_BONDSMAN" })
    void deadNonPrisonerIsKept(PrisonerStatus prisonerStatus) {
        person.setPrisonerStatusDirect(prisonerStatus);

        changeStatus(PersonnelStatus.HOMICIDE);

        verify(humanResources, never()).removePerson(any(), any(), anyBoolean());
    }

    @Test
    void livingPrisonerIsKept() {
        person.setPrisonerStatusDirect(PrisonerStatus.PRISONER);

        changeStatus(PersonnelStatus.MIA);

        verify(humanResources, never()).removePerson(any(), any(), anyBoolean());
    }

    @Test
    void deathIsAnnouncedBeforeRemoval() {
        person.setPrisonerStatusDirect(PrisonerStatus.PRISONER);

        try (EventSpy eventSpy = new EventSpy()) {
            doAnswer(invocation -> {
                assertNotNull(eventSpy.findEvent(PersonStatusChangedEvent.class, e -> e.getPerson() == person));
                return null;
            }).when(humanResources).removePerson(campaign, person, false);

            person.changeStatus(campaign, TODAY, PersonnelStatus.HOMICIDE, false);
        }

        verify(humanResources).removePerson(campaign, person, false);
    }

    @Test
    void changingCauseOfDeathDoesNotRemoveAgain() {
        person.setPrisonerStatusDirect(PrisonerStatus.PRISONER);

        changeStatus(PersonnelStatus.MEDICAL_COMPLICATIONS);
        when(humanResources.getPerson(person.getId())).thenReturn(null);
        changeStatus(PersonnelStatus.HOMICIDE);

        verify(humanResources, times(1)).removePerson(campaign, person, false);
    }
}
