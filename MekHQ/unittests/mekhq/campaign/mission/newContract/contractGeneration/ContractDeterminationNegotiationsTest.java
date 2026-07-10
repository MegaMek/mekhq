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
package mekhq.campaign.mission.newContract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ContractDeterminationNegotiationsTest {
    /**
     * Builds a {@link Person} whose negotiation skill check resolves without error. The actual margin of success is
     * irrelevant here because {@link NegotiationTermsTables} is stubbed in each test.
     */
    private static Person negotiator() {
        Person person = mock(Person.class);

        PersonnelOptions options = mock(PersonnelOptions.class);
        when(person.getOptions()).thenReturn(options);

        SkillCheck skillCheck = mock(SkillCheck.class);
        when(person.checkSkill(anyString(), any())).thenReturn(skillCheck);

        ActionCheckResult result = mock(ActionCheckResult.class);
        when(skillCheck.resolve(anyBoolean(), any())).thenReturn(result);

        return person;
    }

    // ---- Pirate terms (deterministic) -------------------------------------------------------

    @Test
    public void testNegotiateInitialContractTerms_Pirate_ReturnsFixedTerms() {
        // The pirate branch ignores the negotiators, campaign, and modifiers entirely.
        NegotiationsData data = ContractDeterminationNegotiations.negotiateInitialContractTerms(null, null, null,
              CampaignTypeForContractDetermination.PIRATE, null);

        assertEquals(ContractCommandRights.INDEPENDENT, data.commandRights());
        assertEquals(1.0, data.salvageRights());
        assertEquals(0.0, data.supportRights());
        assertEquals(0.0, data.transportRights());
    }

    // ---- Non-pirate initial terms -----------------------------------------------------------

    @Test
    public void testNegotiateInitialContractTerms_NonPirate_PopulatesFromEachTable() {
        Person player = negotiator();
        Person employer = negotiator();
        Campaign campaign = mock(Campaign.class);
        EmployerModifierData modifierData = new EmployerModifierData();

        try (MockedStatic<NegotiationTermsTables> tables = mockStatic(NegotiationTermsTables.class)) {
            tables.when(() -> NegotiationTermsTables.rollOnCommandRightsTable(anyInt(), anyInt()))
                  .thenReturn(ContractCommandRights.LIAISON);
            tables.when(() -> NegotiationTermsTables.rollOnSalvageRightsTable(anyInt(), anyInt())).thenReturn(0.5);
            tables.when(() -> NegotiationTermsTables.rollOnSupportRightsTable(anyInt(), anyInt())).thenReturn(0.25);
            tables.when(() -> NegotiationTermsTables.rollOnTransportRightsTable(anyInt(), anyInt())).thenReturn(0.75);

            NegotiationsData data = ContractDeterminationNegotiations.negotiateInitialContractTerms(player, employer,
                  campaign, CampaignTypeForContractDetermination.GOVERNMENT, modifierData);

            assertEquals(ContractCommandRights.LIAISON, data.commandRights());
            assertEquals(0.5, data.salvageRights());
            assertEquals(0.25, data.supportRights());
            assertEquals(0.75, data.transportRights());
        }
    }

    @Test
    public void testNegotiateInitialContractTerms_MercenaryUsesSameNonPiratePath() {
        Person player = negotiator();
        Person employer = negotiator();
        Campaign campaign = mock(Campaign.class);

        try (MockedStatic<NegotiationTermsTables> tables = mockStatic(NegotiationTermsTables.class)) {
            tables.when(() -> NegotiationTermsTables.rollOnCommandRightsTable(anyInt(), anyInt()))
                  .thenReturn(ContractCommandRights.HOUSE);
            tables.when(() -> NegotiationTermsTables.rollOnSalvageRightsTable(anyInt(), anyInt())).thenReturn(0.1);
            tables.when(() -> NegotiationTermsTables.rollOnSupportRightsTable(anyInt(), anyInt())).thenReturn(0.2);
            tables.when(() -> NegotiationTermsTables.rollOnTransportRightsTable(anyInt(), anyInt())).thenReturn(0.3);

            NegotiationsData data = ContractDeterminationNegotiations.negotiateInitialContractTerms(player, employer,
                  campaign, CampaignTypeForContractDetermination.MERCENARY, new EmployerModifierData());

            assertEquals(ContractCommandRights.HOUSE, data.commandRights());
            assertEquals(0.1, data.salvageRights());
            assertEquals(0.2, data.supportRights());
            assertEquals(0.3, data.transportRights());
        }
    }

    // ---- renegotiateContractTerm: clause routing --------------------------------------------

    @Test
    public void testRenegotiate_CommandRights_UsesCommandTableAndKeepsOthers() {
        NegotiationsData original = new NegotiationsData(ContractCommandRights.INTEGRATED, 0.1, 0.2, 0.3);

        try (MockedStatic<NegotiationTermsTables> tables = mockStatic(NegotiationTermsTables.class)) {
            tables.when(() -> NegotiationTermsTables.rollOnCommandRightsTable(anyInt(), anyInt()))
                  .thenReturn(ContractCommandRights.LIAISON);

            NegotiationsData data = ContractDeterminationNegotiations.renegotiateContractTerm(
                  new EmployerModifierData(), negotiator(), negotiator(), mock(Campaign.class), original,
                  ContractNegotiationClause.COMMAND_RIGHTS);

            assertEquals(ContractCommandRights.LIAISON, data.commandRights());
            assertEquals(0.1, data.salvageRights());
            assertEquals(0.2, data.supportRights());
            assertEquals(0.3, data.transportRights());
        }
    }

    @Test
    public void testRenegotiate_SalvageRights_UsesSalvageTableAndKeepsOthers() {
        NegotiationsData original = new NegotiationsData(ContractCommandRights.INTEGRATED, 0.1, 0.2, 0.3);

        try (MockedStatic<NegotiationTermsTables> tables = mockStatic(NegotiationTermsTables.class)) {
            tables.when(() -> NegotiationTermsTables.rollOnSalvageRightsTable(anyInt(), anyInt())).thenReturn(0.9);

            NegotiationsData data = ContractDeterminationNegotiations.renegotiateContractTerm(
                  new EmployerModifierData(), negotiator(), negotiator(), mock(Campaign.class), original,
                  ContractNegotiationClause.SALVAGE_RIGHTS);

            assertEquals(0.9, data.salvageRights());
            assertEquals(ContractCommandRights.INTEGRATED, data.commandRights());
            assertEquals(0.2, data.supportRights());
            assertEquals(0.3, data.transportRights());
        }
    }

    @Test
    public void testRenegotiate_SupportRights_UsesSupportTableAndKeepsOthers() {
        NegotiationsData original = new NegotiationsData(ContractCommandRights.INTEGRATED, 0.1, 0.2, 0.3);

        try (MockedStatic<NegotiationTermsTables> tables = mockStatic(NegotiationTermsTables.class)) {
            tables.when(() -> NegotiationTermsTables.rollOnSupportRightsTable(anyInt(), anyInt())).thenReturn(0.9);

            NegotiationsData data = ContractDeterminationNegotiations.renegotiateContractTerm(
                  new EmployerModifierData(), negotiator(), negotiator(), mock(Campaign.class), original,
                  ContractNegotiationClause.SUPPORT_RIGHTS);

            assertEquals(0.9, data.supportRights());
            assertEquals(ContractCommandRights.INTEGRATED, data.commandRights());
            assertEquals(0.1, data.salvageRights());
            assertEquals(0.3, data.transportRights());
        }
    }

    @Test
    public void testRenegotiate_TransportRights_UsesTransportTableAndKeepsOthers() {
        NegotiationsData original = new NegotiationsData(ContractCommandRights.INTEGRATED, 0.1, 0.2, 0.3);

        try (MockedStatic<NegotiationTermsTables> tables = mockStatic(NegotiationTermsTables.class)) {
            tables.when(() -> NegotiationTermsTables.rollOnTransportRightsTable(anyInt(), anyInt())).thenReturn(0.9);

            NegotiationsData data = ContractDeterminationNegotiations.renegotiateContractTerm(
                  new EmployerModifierData(), negotiator(), negotiator(), mock(Campaign.class), original,
                  ContractNegotiationClause.TRANSPORT_RIGHTS);

            assertEquals(0.9, data.transportRights());
            assertEquals(ContractCommandRights.INTEGRATED, data.commandRights());
            assertEquals(0.1, data.salvageRights());
            assertEquals(0.2, data.supportRights());
        }
    }
}
