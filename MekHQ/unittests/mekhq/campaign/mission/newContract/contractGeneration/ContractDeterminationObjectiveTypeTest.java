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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the objective-type determination roll tables. Rolls are made deterministic by giving the negotiator zero
 * connections (equip level 0), a stubbed skill margin of success, and a controlled {@code Compute.d6(2)} sequence, so
 * {@code roll = clamp(d6 + margin + equipLevel, 2, 12)} reduces to the supplied d6 values.
 */
class ContractDeterminationObjectiveTypeTest {
    /** Controls the stubbed margin of success from the negotiation/investigation skill check. */
    private int skillMargin = 0;
    /** Controls {@code Compute.randomInt(1)} used by {@code pickAlternatingObjectiveType}. */
    private int alternatingRoll = 0;

    private ContractDeterminationObjectiveType build(GlobalEmployerTableValue table,
          IndependentEmployerTableValue independentTable, boolean isClan, boolean isPirate, HiringHallLevel hall,
          Integer... d6Sequence) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.isPirateCampaign()).thenReturn(isPirate);

        Person negotiator = mock(Person.class);
        when(negotiator.getAdjustedConnections(false)).thenReturn(0);
        PersonnelOptions options = mock(PersonnelOptions.class);
        when(negotiator.getOptions()).thenReturn(options);

        SkillCheck skillCheck = mock(SkillCheck.class);
        when(negotiator.checkSkill(anyString(), any())).thenReturn(skillCheck);
        ActionCheckResult result = mock(ActionCheckResult.class);
        when(skillCheck.resolve(anyBoolean(), any())).thenReturn(result);
        when(result.getMarginOfSuccess()).thenReturn(skillMargin);

        Faction employer = mock(Faction.class);
        when(employer.isClan()).thenReturn(isClan);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            Integer[] rest = Arrays.copyOfRange(d6Sequence, 1, d6Sequence.length);
            compute.when(() -> Compute.d6(2)).thenReturn(d6Sequence[0], rest);
            compute.when(() -> Compute.randomInt(1)).thenReturn(alternatingRoll);

            return new ContractDeterminationObjectiveType(campaign, hall, negotiator, employer, table,
                  independentTable);
        }
    }

    // ---- Inner Sphere / Clan table ----------------------------------------------------------

    @Test
    public void testInnerSphere_Roll5_PlanetaryAssault() {
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.MAJOR_POWER, null, false, false,
              HiringHallLevel.STANDARD, 5);

        assertEquals(List.of(AtBContractType.PLANETARY_ASSAULT), result.getObjectiveTypes());
        assertFalse(result.isHighRisk());
        assertFalse(result.isCovert());
    }

    @Test
    public void testInnerSphere_Roll6_ObjectiveRaid() {
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.SUPER_POWER, null, false, false,
              HiringHallLevel.STANDARD, 6);

        assertEquals(List.of(AtBContractType.OBJECTIVE_RAID), result.getObjectiveTypes());
    }

    @Test
    public void testInnerSphere_Roll2_TriggersCovertSubTable() {
        // Main roll 2 -> convert; the covert sub-roll of 2 -> TERRORISM.
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.MAJOR_POWER, null, false, false,
              HiringHallLevel.STANDARD, 2, 2);

        assertEquals(List.of(AtBContractType.TERRORISM), result.getObjectiveTypes());
        assertTrue(result.isCovert());
        assertFalse(result.isHighRisk());
    }

    @Test
    public void testInnerSphere_Roll3_SpecialSubTable_MultiObjectiveIsHighRisk() {
        // Main roll 3 -> special; the special sub-roll of 3 -> a two-objective result, which is flagged high-risk.
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.MINOR_POWER, null, false, false,
              HiringHallLevel.STANDARD, 3, 3);

        assertEquals(List.of(AtBContractType.GUERRILLA_WARFARE, AtBContractType.PLANETARY_ASSAULT),
              result.getObjectiveTypes());
        assertTrue(result.isHighRisk());
        assertFalse(result.isCovert());
    }

    // ---- Independent table ------------------------------------------------------------------

    @Test
    public void testIndependent_Noble_Roll4_PlanetaryAssault() {
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.INDEPENDENT,
              IndependentEmployerTableValue.NOBLE, false, false, HiringHallLevel.STANDARD, 4);

        assertEquals(List.of(AtBContractType.PLANETARY_ASSAULT), result.getObjectiveTypes());
    }

    @Test
    public void testIndependent_Corporation_Roll5_ObjectiveRaid() {
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.INDEPENDENT,
              IndependentEmployerTableValue.CORPORATION, false, false, HiringHallLevel.STANDARD, 5);

        assertEquals(List.of(AtBContractType.OBJECTIVE_RAID), result.getObjectiveTypes());
    }

    @Test
    public void testIndependent_Corporation_Roll11_AlternatesToCadre() {
        alternatingRoll = 0;
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.INDEPENDENT,
              IndependentEmployerTableValue.CORPORATION, false, false, HiringHallLevel.STANDARD, 11);

        assertEquals(List.of(AtBContractType.CADRE_DUTY), result.getObjectiveTypes());
    }

    @Test
    public void testIndependent_Corporation_Roll11_AlternatesToGarrison() {
        alternatingRoll = 1;
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.INDEPENDENT,
              IndependentEmployerTableValue.CORPORATION, false, false, HiringHallLevel.STANDARD, 11);

        assertEquals(List.of(AtBContractType.GARRISON_DUTY), result.getObjectiveTypes());
    }

    // ---- Clan reroll ------------------------------------------------------------------------

    @Test
    public void testClanCampaign_RerollsProhibitedRoll() {
        // Clan campaigns cannot generate covert/special (rolls 2, 3, 12). A first roll of 2 is rerolled to 5.
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.MAJOR_POWER, null, true, false,
              HiringHallLevel.STANDARD, 2, 5);

        assertEquals(List.of(AtBContractType.PLANETARY_ASSAULT), result.getObjectiveTypes());
        assertFalse(result.isCovert());
    }

    // ---- Hiring hall polarity ---------------------------------------------------------------

    @Test
    public void testQuestionableHiringHall_InvertsModifiers() {
        // With a skill margin of +1 and d6=5, a normal hall rolls 6 (OBJECTIVE_RAID); a questionable hall inverts the
        // modifier to roll 4 (PIRATE_HUNTING) instead, biasing toward different (more covert-leaning) results.
        skillMargin = 1;

        ContractDeterminationObjectiveType standard = build(GlobalEmployerTableValue.MAJOR_POWER, null, false, false,
              HiringHallLevel.STANDARD, 5);
        assertEquals(List.of(AtBContractType.OBJECTIVE_RAID), standard.getObjectiveTypes());

        ContractDeterminationObjectiveType questionable = build(GlobalEmployerTableValue.MAJOR_POWER, null, false,
              false, HiringHallLevel.QUESTIONABLE, 5);
        assertEquals(List.of(AtBContractType.PIRATE_HUNTING), questionable.getObjectiveTypes());
    }

    // ---- Pirate campaign --------------------------------------------------------------------

    @Test
    public void testPirateCampaign_LowRoll_ReconRaid() {
        // A pirate campaign uses the dedicated pirate table regardless of the employer table value.
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.MAJOR_POWER, null, false, true,
              HiringHallLevel.STANDARD, 4);

        assertEquals(List.of(AtBContractType.RECON_RAID), result.getObjectiveTypes());
        assertFalse(result.isCovert());
        assertFalse(result.isHighRisk());
    }

    @Test
    public void testPirateCampaign_HighRoll_ObjectiveRaid() {
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.INDEPENDENT,
              IndependentEmployerTableValue.CORPORATION, false, true, HiringHallLevel.STANDARD, 10);

        assertEquals(List.of(AtBContractType.OBJECTIVE_RAID), result.getObjectiveTypes());
    }

    @Test
    public void testPirateCampaign_ClanEmployer_DoesNotReroll() {
        // The pirate table has no Covert/Special results, so the Clan reroll must not apply: a first roll of 2 (which
        // the Clan reroll would otherwise reject) is used directly, yielding RECON_RAID rather than rerolling to 6.
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.MAJOR_POWER, null, true, true,
              HiringHallLevel.STANDARD, 2, 6);

        assertEquals(List.of(AtBContractType.RECON_RAID), result.getObjectiveTypes());
    }

    // ---- Accessor ---------------------------------------------------------------------------

    @Test
    public void testGetObjectiveTypes_ReturnsListContents() {
        ContractDeterminationObjectiveType result = build(GlobalEmployerTableValue.MAJOR_POWER, null, false, false,
              HiringHallLevel.STANDARD, 8);

        assertEquals(List.of(AtBContractType.EXTRACTION_RAID), result.getObjectiveTypes());
    }
}
