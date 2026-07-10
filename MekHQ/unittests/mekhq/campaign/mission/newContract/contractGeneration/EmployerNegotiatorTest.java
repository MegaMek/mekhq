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

import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.AutoAssignRankForCompanyGenerator;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class EmployerNegotiatorTest {
    private static final int MINIMUM_CHARISMA = 7;
    private static final int MINIMUM_NEGOTIATION = 4;
    private static final int RO_MIN = 31;

    private static Faction faction(boolean isClan) {
        Faction faction = mock(Faction.class);
        when(faction.isClan()).thenReturn(isClan);
        return faction;
    }

    private static PersonnelRole invokeGetNegotiatorRole(HiringHallLevel hall,
          CampaignTypeForContractDetermination type, Faction faction) throws Exception {
        Method method = EmployerNegotiator.class.getDeclaredMethod("getNegotiatorRole", HiringHallLevel.class,
              CampaignTypeForContractDetermination.class, Faction.class);
        method.setAccessible(true);
        return (PersonnelRole) method.invoke(null, hall, type, faction);
    }

    private static void invokePrivate(String name, Person negotiator) throws Exception {
        Method method = EmployerNegotiator.class.getDeclaredMethod(name, Person.class);
        method.setAccessible(true);
        method.invoke(null, negotiator);
    }

    // ---- getNegotiatorRole ------------------------------------------------------------------

    @Test
    public void testGetNegotiatorRole_Pirate_IsBrokerRegardlessOfFactionOrHall() throws Exception {
        assertEquals(PersonnelRole.BROKER, invokeGetNegotiatorRole(HiringHallLevel.GREAT,
              CampaignTypeForContractDetermination.PIRATE, faction(false)));
        assertEquals(PersonnelRole.BROKER, invokeGetNegotiatorRole(HiringHallLevel.QUESTIONABLE,
              CampaignTypeForContractDetermination.PIRATE, faction(true)));
    }

    @Test
    public void testGetNegotiatorRole_Government_NonClan_IsAdministratorCommand() throws Exception {
        assertEquals(PersonnelRole.ADMINISTRATOR_COMMAND, invokeGetNegotiatorRole(HiringHallLevel.STANDARD,
              CampaignTypeForContractDetermination.GOVERNMENT, faction(false)));
    }

    @Test
    public void testGetNegotiatorRole_Government_Clan_IsMekWarrior() throws Exception {
        assertEquals(PersonnelRole.MEKWARRIOR, invokeGetNegotiatorRole(HiringHallLevel.STANDARD,
              CampaignTypeForContractDetermination.GOVERNMENT, faction(true)));
    }

    @Test
    public void testGetNegotiatorRole_Mercenary_NonClan_Standard_IsLawyer() throws Exception {
        assertEquals(PersonnelRole.LAWYER, invokeGetNegotiatorRole(HiringHallLevel.STANDARD,
              CampaignTypeForContractDetermination.MERCENARY, faction(false)));
    }

    @Test
    public void testGetNegotiatorRole_Mercenary_NonClan_Questionable_IsBroker() throws Exception {
        assertEquals(PersonnelRole.BROKER, invokeGetNegotiatorRole(HiringHallLevel.QUESTIONABLE,
              CampaignTypeForContractDetermination.MERCENARY, faction(false)));
    }

    @Test
    public void testGetNegotiatorRole_Mercenary_Clan_IsMerchant_EvenWhenQuestionable() throws Exception {
        // The clan check takes precedence over the questionable-hall check.
        assertEquals(PersonnelRole.MERCHANT, invokeGetNegotiatorRole(HiringHallLevel.QUESTIONABLE,
              CampaignTypeForContractDetermination.MERCENARY, faction(true)));
    }

    // ---- adjustCharisma ---------------------------------------------------------------------

    @Test
    public void testAdjustCharisma_BelowMinimum_RaisesToMinimum() throws Exception {
        Person negotiator = mock(Person.class);
        when(negotiator.getAttributeScore(SkillAttribute.CHARISMA)).thenReturn(MINIMUM_CHARISMA - 1);

        invokePrivate("adjustCharisma", negotiator);

        verify(negotiator).setAttributeScore(SkillAttribute.CHARISMA, MINIMUM_CHARISMA);
    }

    @Test
    public void testAdjustCharisma_AtOrAboveMinimum_LeavesUnchanged() throws Exception {
        Person negotiator = mock(Person.class);
        when(negotiator.getAttributeScore(SkillAttribute.CHARISMA)).thenReturn(MINIMUM_CHARISMA);

        invokePrivate("adjustCharisma", negotiator);

        verify(negotiator, never()).setAttributeScore(eq(SkillAttribute.CHARISMA), anyInt());
    }

    // ---- adjustNegotiation ------------------------------------------------------------------

    @Test
    public void testAdjustNegotiation_NoSkill_AddsAtMinimum() throws Exception {
        Person negotiator = mock(Person.class);
        when(negotiator.getSkill(anyString())).thenReturn(null);

        invokePrivate("adjustNegotiation", negotiator);

        verify(negotiator).addSkill(S_NEGOTIATION, MINIMUM_NEGOTIATION, 0);
    }

    @Test
    public void testAdjustNegotiation_BelowMinimum_RaisesToMinimum() throws Exception {
        Skill skill = mock(Skill.class);
        when(skill.getLevel()).thenReturn(MINIMUM_NEGOTIATION - 1);
        Person negotiator = mock(Person.class);
        when(negotiator.getSkill(anyString())).thenReturn(skill);

        invokePrivate("adjustNegotiation", negotiator);

        verify(skill).setLevel(MINIMUM_NEGOTIATION);
    }

    @Test
    public void testAdjustNegotiation_AtOrAboveMinimum_LeavesUnchanged() throws Exception {
        Skill skill = mock(Skill.class);
        when(skill.getLevel()).thenReturn(MINIMUM_NEGOTIATION);
        Person negotiator = mock(Person.class);
        when(negotiator.getSkill(anyString())).thenReturn(skill);

        invokePrivate("adjustNegotiation", negotiator);

        verify(skill, never()).setLevel(anyInt());
    }

    // ---- assignRank -------------------------------------------------------------------------

    @Test
    public void testAssignRank_UnrankedRole_DoesNotAssign() throws Exception {
        Person negotiator = mock(Person.class);
        when(negotiator.getPrimaryRole()).thenReturn(PersonnelRole.BROKER);

        try (MockedStatic<AutoAssignRankForCompanyGenerator> ranks =
                   mockStatic(AutoAssignRankForCompanyGenerator.class)) {
            invokePrivate("assignRank", negotiator);

            ranks.verifyNoInteractions();
        }
    }

    @Test
    public void testAssignRank_RankedRole_AssignsRankSystem() throws Exception {
        Person negotiator = mock(Person.class);
        when(negotiator.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_COMMAND);

        try (MockedStatic<AutoAssignRankForCompanyGenerator> ranks =
                   mockStatic(AutoAssignRankForCompanyGenerator.class)) {
            invokePrivate("assignRank", negotiator);

            ranks.verify(() -> AutoAssignRankForCompanyGenerator.assignRankSystemFromFaction(negotiator, RO_MIN));
        }
    }
}
