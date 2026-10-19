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
package mekhq.gui.dialog.markets.contractMarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.LocalPersonnel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.Test;

/**
 * Verifies how {@link ContractNegotiatorPickerDialog} ranks the people who can negotiate for the player, and in
 * particular that any number of lead negotiators can be flagged at once.
 */
class ContractNegotiatorPickerDialogTest {

    /** A mock campaign whose detachment holds exactly the given people. */
    private static Campaign campaignWith(Person... people) {
        Campaign campaign = mockCampaign();
        LocalPersonnel personnel = mock(LocalPersonnel.class);
        when(personnel.values()).thenReturn(List.of(people));
        when(campaign.getPlayerForce().getForceDetachment().getPersonnel()).thenReturn(personnel);
        return campaign;
    }

    /** An active person with the given lead negotiator flag and Negotiation level ({@code null} for untrained). */
    private static Person person(boolean leadNegotiator, Integer negotiationLevel) {
        Person person = mock(Person.class);
        when(person.isActive()).thenReturn(true);
        when(person.isLeadNegotiator()).thenReturn(leadNegotiator);
        if (negotiationLevel != null) {
            Skill skill = mock(Skill.class);
            when(skill.getLevel()).thenReturn(negotiationLevel);
            when(person.hasSkill(SkillType.S_NEGOTIATION)).thenReturn(true);
            when(person.getSkill(SkillType.S_NEGOTIATION)).thenReturn(skill);
        }
        return person;
    }

    @Test
    void everyLeadNegotiatorIsOfferedAheadOfTheTrainedPersonnelBestFirst() {
        Person trainedExpert = person(false, 8);
        Person weakLead = person(true, 2);
        Person untrainedLead = person(true, null);
        Person strongLead = person(true, 5);
        Person trainedNovice = person(false, 1);

        Campaign campaign = campaignWith(trainedExpert, weakLead, untrainedLead, strongLead, trainedNovice);

        assertEquals(List.of(strongLead, weakLead, untrainedLead, trainedExpert, trainedNovice),
              ContractNegotiatorPickerDialog.eligibleNegotiators(campaign));
    }

    @Test
    void theMostSkilledLeadNegotiatorIsPutForwardByDefault() {
        Person weakLead = person(true, 2);
        Person strongLead = person(true, 5);
        Person trainedExpert = person(false, 8);

        Campaign campaign = campaignWith(weakLead, trainedExpert, strongLead);

        assertEquals(strongLead, ContractNegotiatorPickerDialog.leadNegotiator(campaign));
    }

    @Test
    void noLeadNegotiatorIsPutForwardWhenNoneIsFlagged() {
        Campaign campaign = campaignWith(person(false, 8), person(false, 3));

        assertNull(ContractNegotiatorPickerDialog.leadNegotiator(campaign));
    }

    @Test
    void anInactiveLeadNegotiatorIsNotOffered() {
        Person inactiveLead = person(true, 9);
        when(inactiveLead.isActive()).thenReturn(false);
        Person activeLead = person(true, 1);

        Campaign campaign = campaignWith(inactiveLead, activeLead);

        assertEquals(List.of(activeLead), ContractNegotiatorPickerDialog.eligibleNegotiators(campaign));
    }
}
