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
 * MechWarrior, BattleMek, `Mek and AeroTek are registered trademarks
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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static mekhq.campaign.personnel.PersonnelOptions.FLAW_SUPER_SPREADER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.Inoculations.DiseaseScanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class InoculationsTest {

    @Test
    @DisplayName("Permanent disease carriers do not contribute their disease to the unit-wide spread set")
    void getActiveDiseases_excludesPermanentInfections() {
        InjuryType permanentType = mock(InjuryType.class);
        Injury permanentInjury = injury(permanentType, InjurySubType.DISEASE_GENERIC, true);
        Person carrier = personWith(permanentInjury);

        InjuryType genericType = mock(InjuryType.class);
        Injury genericInjury = injury(genericType, InjurySubType.DISEASE_GENERIC, false);
        Person infected = personWith(genericInjury);

        InjuryType bioweaponType = mock(InjuryType.class);
        Injury bioweaponInjury = injury(bioweaponType, InjurySubType.DISEASE_CANON_BIOWEAPON, false);
        Person attacked = personWith(bioweaponInjury);

        DiseaseScanResult result = Inoculations.getActiveDiseases(List.of(carrier, infected, attacked));

        assertFalse(result.activeDiseases().contains(permanentType),
              "Permanent disease must not appear in the active spread set");
        assertFalse(result.activeCanonDiseases().contains(permanentType),
              "Permanent disease must not appear in the canon spread set");
        assertTrue(result.activeDiseases().contains(genericType),
              "Non-permanent generic disease should remain in the spread set");
        assertTrue(result.activeCanonDiseases().contains(bioweaponType),
              "Non-permanent canon bioweapon should remain in the canon spread set");
    }

    @Test
    @DisplayName("Permanent canon-bioweapon carriers are excluded from the canon spread set")
    void getActiveDiseases_excludesPermanentCanonBioweapons() {
        InjuryType permanentBioweaponType = mock(InjuryType.class);
        Injury permanentBioweaponInjury = injury(permanentBioweaponType, InjurySubType.DISEASE_CANON_BIOWEAPON, true);
        Person permanentCarrier = personWith(permanentBioweaponInjury);

        InjuryType activeBioweaponType = mock(InjuryType.class);
        Injury activeBioweaponInjury = injury(activeBioweaponType, InjurySubType.DISEASE_CANON_BIOWEAPON, false);
        Person activeCarrier = personWith(activeBioweaponInjury);

        DiseaseScanResult result =
              Inoculations.getActiveDiseases(List.of(permanentCarrier, activeCarrier));

        assertFalse(result.activeCanonDiseases().contains(permanentBioweaponType),
              "Permanent canon bioweapon must not appear in the canon spread set");
        assertTrue(result.activeCanonDiseases().contains(activeBioweaponType),
              "Non-permanent canon bioweapon should remain in the canon spread set");
        assertTrue(result.activeDiseases().isEmpty(),
              "Generic spread set must be empty when only canon-typed injuries are present");
    }

    @Test
    @DisplayName("A unit whose only carrier has a permanent disease reports an empty active-disease set")
    void getActiveDiseases_permanentOnlyUnitReportsEmpty() {
        InjuryType permanentType = mock(InjuryType.class);
        Injury permanentInjury = injury(permanentType, InjurySubType.DISEASE_GENERIC, true);
        Person carrier = personWith(permanentInjury);

        DiseaseScanResult result = Inoculations.getActiveDiseases(List.of(carrier));

        assertTrue(result.activeDiseases().isEmpty(),
              "Active diseases set must be empty when only permanent infections exist");
        assertTrue(result.activeCanonDiseases().isEmpty(),
              "Active canon diseases set must be empty when only permanent infections exist");
    }

    private static Injury injury(InjuryType type, InjurySubType subType, boolean permanent) {
        Injury injury = mock(Injury.class);
        when(injury.getType()).thenReturn(type);
        when(injury.getSubType()).thenReturn(subType);
        when(injury.isPermanent()).thenReturn(permanent);
        return injury;
    }

    private static Person personWith(Injury... injuries) {
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);
        when(options.booleanOption(FLAW_SUPER_SPREADER)).thenReturn(false);
        when(person.getOptions()).thenReturn(options);
        when(person.getInjuries()).thenReturn(List.of(injuries));
        return person;
    }
}
