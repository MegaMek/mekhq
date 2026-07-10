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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

class EmployerNegotiationsModifierTest {
    /** A year that falls in neither the pre-succession-wars nor post-clan-invasion era window. */
    private static final int NEUTRAL_YEAR = 3000;

    private static void assertModifiers(EmployerModifierData data, double tempo, int command, int salvage,
          int support, int transport) {
        assertEquals(tempo, data.getTempoMultiplier(), "tempoMultiplier");
        assertEquals(command, data.getCommandModifier(), "commandModifier");
        assertEquals(salvage, data.getSalvageModifier(), "salvageModifier");
        assertEquals(support, data.getSupportModifier(), "supportModifier");
        assertEquals(transport, data.getTransportModifier(), "transportModifier");
    }

    // ---- Per-constant accessors --------------------------------------------------------------

    @Test
    public void testAccessors_SuperPower() {
        EmployerNegotiationsModifier entry = EmployerNegotiationsModifier.EMPLOYER_SUPER_POWER;

        assertEquals(1.3, entry.getEmploymentMultiplier());
        assertEquals(0, entry.getCommandModifier());
        assertEquals(0, entry.getSalvageModifier());
        assertEquals(1, entry.getSupportModifier());
        assertEquals(2, entry.getTransportModifier());
    }

    @Test
    public void testAccessors_MercenaryOrCorporation() {
        EmployerNegotiationsModifier entry = EmployerNegotiationsModifier.EMPLOYER_MERCENARY_OR_CORPORATION;

        assertEquals(1.1, entry.getEmploymentMultiplier());
        assertEquals(-1, entry.getCommandModifier());
        assertEquals(2, entry.getSalvageModifier());
        assertEquals(1, entry.getSupportModifier());
        assertEquals(1, entry.getTransportModifier());
    }

    @Test
    public void testAccessors_TypicalNoModifiers() {
        EmployerNegotiationsModifier entry = EmployerNegotiationsModifier.TYPICAL_NO_MODIFIERS;

        assertEquals(1.0, entry.getEmploymentMultiplier());
        assertEquals(0, entry.getCommandModifier());
        assertEquals(0, entry.getSalvageModifier());
        assertEquals(0, entry.getSupportModifier());
        assertEquals(0, entry.getTransportModifier());
    }

    // ---- Faction dispatch --------------------------------------------------------------------

    @Test
    public void testGetNegotiationsModifier_SuperPower() {
        Faction employer = mock(Faction.class);
        when(employer.isSuperPower()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        assertModifiers(data, 1.3, 0, 0, 1, 2);
    }

    @Test
    public void testGetNegotiationsModifier_MajorPower() {
        Faction employer = mock(Faction.class);
        when(employer.isMajorPower()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        assertModifiers(data, 1.2, 0, -1, 0, 1);
    }

    @Test
    public void testGetNegotiationsModifier_MinorPower() {
        Faction employer = mock(Faction.class);
        when(employer.isMinorPower()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        assertModifiers(data, 1.1, 0, -2, 0, 0);
    }

    @Test
    public void testGetNegotiationsModifier_IndependentWorld() {
        Faction employer = mock(Faction.class);
        when(employer.isIndependent()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        assertModifiers(data, 1.0, 0, -1, -1, 0);
    }

    @Test
    public void testGetNegotiationsModifier_Mercenary() {
        Faction employer = mock(Faction.class);
        when(employer.isMercenary()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        assertModifiers(data, 1.1, -1, 2, 1, 1);
    }

    @Test
    public void testGetNegotiationsModifier_Corporation() {
        Faction employer = mock(Faction.class);
        when(employer.isCorporation()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        assertModifiers(data, 1.1, -1, 2, 1, 1);
    }

    @Test
    public void testGetNegotiationsModifier_NoMatchingFaction_UsesTypical() {
        // All faction predicates default to false -> TYPICAL_NO_MODIFIERS, and a neutral year contributes nothing.
        Faction employer = mock(Faction.class);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        assertModifiers(data, 1.0, 0, 0, 0, 0);
    }

    @Test
    public void testGetNegotiationsModifier_SuperPowerTakesPrecedenceOverMajorPower() {
        Faction employer = mock(Faction.class);
        when(employer.isSuperPower()).thenReturn(true);
        when(employer.isMajorPower()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        // Only the super-power branch applies.
        assertModifiers(data, 1.3, 0, 0, 1, 2);
    }

    // ---- Generosity --------------------------------------------------------------------------

    @Test
    public void testGetNegotiationsModifier_Generous() {
        Faction employer = mock(Faction.class);
        when(employer.isGenerous()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        // TYPICAL_NO_MODIFIERS (1.0, 0,0,0,0) + EMPLOYER_GENEROSITY_GENEROUS (0.2, 0,1,2,1)
        assertModifiers(data, 1.2, 0, 1, 2, 1);
    }

    @Test
    public void testGetNegotiationsModifier_Stingy() {
        Faction employer = mock(Faction.class);
        when(employer.isStingy()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        // TYPICAL_NO_MODIFIERS (1.0, 0,0,0,0) + EMPLOYER_GENEROSITY_STINGY (-0.2, 0,-1,-1,-1)
        assertModifiers(data, 0.8, 0, -1, -1, -1);
    }

    // ---- Oversight ---------------------------------------------------------------------------

    @Test
    public void testGetNegotiationsModifier_Controlling() {
        Faction employer = mock(Faction.class);
        when(employer.isControlling()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        // TYPICAL_NO_MODIFIERS (1.0, 0,0,0,0) + EMPLOYER_OVERSIGHT_CONTROLLING (0, -2,-1,0,0)
        assertModifiers(data, 1.0, -2, -1, 0, 0);
    }

    @Test
    public void testGetNegotiationsModifier_Lenient() {
        Faction employer = mock(Faction.class);
        when(employer.isLenient()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        // TYPICAL_NO_MODIFIERS (1.0, 0,0,0,0) + EMPLOYER_OVERSIGHT_LENIENT (0, 1,1,0,0)
        assertModifiers(data, 1.0, 1, 1, 0, 0);
    }

    // ---- Era boundaries ----------------------------------------------------------------------

    @Test
    public void testGetNegotiationsModifier_PreSuccessionWars_AtBoundary() {
        Faction employer = mock(Faction.class);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, 2780, data);

        // TYPICAL_NO_MODIFIERS + ERA_PRE_SUCCESSION_WARS (0, 0,-2,0,0)
        assertModifiers(data, 1.0, 0, -2, 0, 0);
    }

    @Test
    public void testGetNegotiationsModifier_JustAfterPreSuccessionBoundary_NoEra() {
        Faction employer = mock(Faction.class);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, 2781, data);

        assertModifiers(data, 1.0, 0, 0, 0, 0);
    }

    @Test
    public void testGetNegotiationsModifier_PostClanInvasionBoundary_NoEra() {
        Faction employer = mock(Faction.class);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, 3061, data);

        assertModifiers(data, 1.0, 0, 0, 0, 0);
    }

    @Test
    public void testGetNegotiationsModifier_PostClanInvasion() {
        Faction employer = mock(Faction.class);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, 3062, data);

        // TYPICAL_NO_MODIFIERS + ERA_POST_CLAN_INVASION (0, 0,-2,0,0)
        assertModifiers(data, 1.0, 0, -2, 0, 0);
    }

    // ---- Composition -------------------------------------------------------------------------

    @Test
    public void testGetNegotiationsModifier_StacksFactionGenerosityOversightAndEra() {
        Faction employer = mock(Faction.class);
        when(employer.isSuperPower()).thenReturn(true);
        when(employer.isGenerous()).thenReturn(true);
        when(employer.isControlling()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, 2780, data);

        // SUPER_POWER (1.3, 0,0,1,2) + GENEROUS (0.2, 0,1,2,1) + CONTROLLING (0, -2,-1,0,0)
        //   + PRE_SUCCESSION_WARS (0, 0,-2,0,0)
        assertModifiers(data, 1.5, -2, -2, 3, 3);
    }

    @Test
    public void testGetNegotiationsModifier_LeavesEmploymentMultiplierUntouched() {
        // The enum's employmentMultiplier feeds modifyTempoMultiplier, so the data's own
        // employmentMultiplier field is never written.
        Faction employer = mock(Faction.class);
        when(employer.isSuperPower()).thenReturn(true);

        EmployerModifierData data = new EmployerModifierData();
        EmployerNegotiationsModifier.getNegotiationsModifier(employer, NEUTRAL_YEAR, data);

        assertEquals(0.0, data.getEmploymentMultiplier());
    }
}
