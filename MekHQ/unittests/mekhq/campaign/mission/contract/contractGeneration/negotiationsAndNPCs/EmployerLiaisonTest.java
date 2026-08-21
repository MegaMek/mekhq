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
package mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs;

import static mekhq.campaign.personnel.ranks.Rank.RO_MIN;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import testUtilities.MHQTestUtilities;

/**
 * Tests {@link EmployerLiaison#generateLiaison}, which picks the contact a contract's employer sends to deal with the
 * unit.
 *
 * <p>Who that is depends on both the kind of employer and whether they are Clan: a House sends a command
 * administrator, a Clan sends a warrior, a mercenary hall sends a military liaison (a merchant, for a Clan), and a
 * pirate sends a broker. The unofficial contacts - the hall's and the pirate's - carry no military rank, so the rank
 * assignment must be skipped for them rather than stamping a House rank on someone who has none.</p>
 */
class EmployerLiaisonTest {
    /** A campaign whose personnel generator returns {@code person} for whatever role it is asked for. */
    private static Campaign campaignGenerating(Person person) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(any(), any(PersonnelRole.class), any(), any(Gender.class))).thenReturn(person);
        return campaign;
    }

    /**
     * A liaison with an origin faction, since ranking one resolves that faction's own rank system so the resulting rank
     * is appropriate for Clans, ComStar, and Inner Sphere powers alike.
     */
    private static Person personWithRole(PersonnelRole role) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(role);

        RankSystem rankSystem = mock(RankSystem.class);
        when(rankSystem.getType()).thenReturn(RankSystemType.DEFAULT);

        Faction originFaction = mock(Faction.class);
        when(originFaction.getRankSystem()).thenReturn(rankSystem);
        when(person.getOriginFaction()).thenReturn(originFaction);

        return person;
    }

    @ParameterizedTest
    @CsvSource({
          "GOVERNMENT, false, ADMINISTRATOR_COMMAND",
          "GOVERNMENT, true, MEKWARRIOR",
          "MERCENARY, false, MILITARY_LIAISON",
          "MERCENARY, true, MERCHANT",
          // Arena organizers reuse the mercenary-hall roles until dedicated tournament generation lands.
          "TOURNAMENT, false, MILITARY_LIAISON",
          "TOURNAMENT, true, MERCHANT",
          "PIRATE, false, BROKER",
          "PIRATE, true, BROKER"
    })
    void theLiaisonsRoleFollowsTheEmployerKindAndWhetherTheyAreClan(ContractSearchType searchType,
          boolean employerIsClan, PersonnelRole expectedRole) {
        Person liaison = personWithRole(expectedRole);
        Campaign campaign = campaignGenerating(liaison);

        assertSame(liaison, EmployerLiaison.generateLiaison(campaign, searchType, employerIsClan, "LA"));
        verify(campaign.getPlayerForce().getHumanResources())
              .newPerson(campaign, expectedRole, "LA", Gender.RANDOMIZE);
    }

    @ParameterizedTest
    @CsvSource({ "GOVERNMENT, false, ADMINISTRATOR_COMMAND", "GOVERNMENT, true, MEKWARRIOR" })
    void anOfficialLiaisonIsGivenAFactionAppropriateRank(ContractSearchType searchType, boolean employerIsClan,
          PersonnelRole role) {
        Person liaison = personWithRole(role);

        EmployerLiaison.generateLiaison(campaignGenerating(liaison), searchType, employerIsClan, "LA");

        verify(liaison).setRank(RO_MIN);
    }

    @ParameterizedTest
    @CsvSource({ "PIRATE, false, BROKER", "MERCENARY, false, MILITARY_LIAISON", "MERCENARY, true, MERCHANT" })
    void anUnofficialContactIsLeftUnranked(ContractSearchType searchType, boolean employerIsClan, PersonnelRole role) {
        Person liaison = personWithRole(role);

        EmployerLiaison.generateLiaison(campaignGenerating(liaison), searchType, employerIsClan, "LA");

        verify(liaison, never()).setRank(RO_MIN);
        verify(liaison, never()).setRankSystem(any(RankValidator.class), any());
    }

    @Test
    void aLiaisonThatCannotBeGeneratedComesBackAsNothingRatherThanThrowing() {
        assertNull(EmployerLiaison.generateLiaison(campaignGenerating(null),
              ContractSearchType.GOVERNMENT,
              false,
              "LA"));
    }
}
