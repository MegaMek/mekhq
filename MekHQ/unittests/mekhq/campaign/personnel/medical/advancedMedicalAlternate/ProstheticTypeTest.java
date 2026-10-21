/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static megamek.common.options.OptionsConstants.MD_GAS_EFFUSER_PHEROMONE;
import static megamek.common.options.OptionsConstants.MD_GAS_EFFUSER_TOXIN;
import static megamek.common.options.OptionsConstants.MD_PL_ENHANCED;
import static megamek.common.options.OptionsConstants.MD_PL_EXTRA_LIMBS;
import static megamek.common.options.OptionsConstants.MD_PL_FLIGHT;
import static megamek.common.options.OptionsConstants.MD_PL_GLIDER;
import static megamek.common.options.OptionsConstants.MD_PL_I_ENHANCED;
import static megamek.common.options.OptionsConstants.MD_PL_TAIL;
import static megamek.common.options.OptionsConstants.MD_SUICIDE_IMPLANTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public class ProstheticTypeTest {
    @Test
    @DisplayName("Each ProstheticType has a unique, non-null InjuryType")
    public void injuryTypeIsUniqueAndNonNull() {
        Set<InjuryType> seen = new HashSet<>();

        for (ProstheticType prostheticType : ProstheticType.values()) {
            InjuryType injuryType = prostheticType.getInjuryType();
            assertNotNull(injuryType, () -> prostheticType.name() + " has null injuryType");
            boolean added = seen.add(injuryType);
            assertTrue(added, () -> "Duplicate injuryType detected for " + prostheticType.name());
        }

        assertEquals(ProstheticType.values().length, seen.size(),
              "InjuryType must be unique across all ProstheticType entries");
    }

    @Test
    @DisplayName("getProstheticFromInjury round-trips for all ProstheticType injury mappings")
    public void getProstheticFromInjury_roundTrip() {
        for (ProstheticType prostheticType : ProstheticType.values()) {
            InjuryType injuryType = prostheticType.getInjuryType();
            if (injuryType.getSubType().isPermanentModification()) {
                ProstheticType resolved = ProstheticType.getProstheticFromInjury(injuryType);
                assertSame(prostheticType, resolved, () -> "Expected round-trip to return " + prostheticType.name());
            }
        }
    }

    @Test
    @DisplayName("getProstheticFromInjury returns null for non-permanent modification injuries")
    public void getProstheticFromInjury_returnsNullForNonPermanent() {
        InjuryType nonPermanent = AlternateInjuries.FRACTURED_RIB;
        assertNotNull(nonPermanent);
        assertNull(ProstheticType.getProstheticFromInjury(nonPermanent),
              "Non-permanent injuries must not map to a ProstheticType");
    }

    @ParameterizedTest(name = "{0} grants {1}")
    @MethodSource(value = "implantGrantingData")
    @DisplayName("Each Manei Domini prosthetic fitted by surgery grants exactly its implant")
    public void augmentationGrantsItsImplant(ProstheticType prostheticType, String implant) {
        assertEquals(List.of(implant), prostheticType.getAssociatedPilotOptions());
    }

    static Stream<Arguments> implantGrantingData() {
        return Stream.of(
              Arguments.of(ProstheticType.PHEROMONE_EFFUSER, MD_GAS_EFFUSER_PHEROMONE),
              Arguments.of(ProstheticType.TOXIN_EFFUSER, MD_GAS_EFFUSER_TOXIN),
              Arguments.of(ProstheticType.EXPLOSIVE_SUICIDE_IMPLANT, MD_SUICIDE_IMPLANTS),
              Arguments.of(ProstheticType.ENHANCED_PROSTHETIC_LIMB, MD_PL_ENHANCED),
              Arguments.of(ProstheticType.IMPROVED_ENHANCED_PROSTHETIC_LIMB, MD_PL_I_ENHANCED),
              Arguments.of(ProstheticType.ENHANCED_TAIL_PROSTHETIC, MD_PL_TAIL),
              Arguments.of(ProstheticType.EXTRANEOUS_PROSTHETIC_LIMBS, MD_PL_EXTRA_LIMBS),
              Arguments.of(ProstheticType.GLIDER_WINGS, MD_PL_GLIDER),
              Arguments.of(ProstheticType.POWERED_FLIGHT_WINGS, MD_PL_FLIGHT)
        );
    }

    /**
     * These implants are worked out each day from how many limbs carry the prosthetic. Granting the implant at
     * surgery would hand it over after the first limb.
     */
    @ParameterizedTest
    @MethodSource(value = "derivedProstheticData")
    @DisplayName("Prosthetics behind a derived implant grant no implant at surgery")
    public void derivedProstheticsGrantNoImplant(ProstheticType prostheticType) {
        assertTrue(prostheticType.getAssociatedPilotOptions().isEmpty(),
              prostheticType.name() + " should leave its implant to the daily eligibility check");
    }

    static Stream<ProstheticType> derivedProstheticData() {
        return Stream.of(ProstheticType.DERMAL_MYOMER_ARM_ARMOR, ProstheticType.DERMAL_MYOMER_LEG_ARMOR,
              ProstheticType.DERMAL_MYOMER_ARM_CAMO, ProstheticType.DERMAL_MYOMER_LEG_CAMO,
              ProstheticType.DERMAL_MYOMER_ARM_TRIPLE, ProstheticType.DERMAL_MYOMER_LEG_TRIPLE,
              ProstheticType.PROSTHETIC_LEG_MASC);
    }

    @Test
    @DisplayName("Wings and extraneous limbs are spine implants, and only the wings exclude each other")
    public void spineAugmentationsAreImplants() {
        for (ProstheticType wings : List.of(ProstheticType.GLIDER_WINGS, ProstheticType.POWERED_FLIGHT_WINGS)) {
            assertEquals(Set.of(BodyLocation.SPINE), wings.getEligibleLocations());
            assertEquals(InjurySubType.IMPLANT_WINGS, wings.getInjuryType().getSubType());
        }

        ProstheticType limbs = ProstheticType.EXTRANEOUS_PROSTHETIC_LIMBS;
        assertEquals(Set.of(BodyLocation.SPINE), limbs.getEligibleLocations());
        assertTrue(limbs.getInjuryType().getSubType().isImplant());
        assertFalse(limbs.getInjuryType().getSubType().isMutuallyExclusiveImplant(),
              "fitting extra limbs must not remove a warrior's wings");
    }

    @Test
    @DisplayName("Enhanced prosthetics replace arms, leg MASC replaces legs, and the tail replaces the rump")
    public void limbAugmentationsGoWhereTheyBelong() {
        Set<BodyLocation> arms = Set.of(BodyLocation.LEFT_ARM, BodyLocation.RIGHT_ARM);
        assertEquals(arms, ProstheticType.ENHANCED_PROSTHETIC_LIMB.getEligibleLocations());
        assertEquals(arms, ProstheticType.IMPROVED_ENHANCED_PROSTHETIC_LIMB.getEligibleLocations());
        assertEquals(Set.of(BodyLocation.LEFT_LEG, BodyLocation.RIGHT_LEG),
              ProstheticType.PROSTHETIC_LEG_MASC.getEligibleLocations());
        assertEquals(Set.of(BodyLocation.RUMP), ProstheticType.ENHANCED_TAIL_PROSTHETIC.getEligibleLocations());
    }

    @Test
    @DisplayName("Effusers, the power supply and the suicide implant are internal implants that leave other internal "
          + "work in place")
    public void internalAugmentationsAreImplants() {
        // Surgery that isn't an implant clears everything at its location, so any of these fitted as a replacement
        // would strip the others - and the implant options that go with them - whichever order they were fitted in.
        for (ProstheticType prostheticType : List.of(ProstheticType.TOXIN_EFFUSER,
              ProstheticType.PHEROMONE_EFFUSER, ProstheticType.POWER_SUPPLY,
              ProstheticType.EXPLOSIVE_SUICIDE_IMPLANT)) {
            assertEquals(Set.of(BodyLocation.INTERNAL), prostheticType.getEligibleLocations());
            assertEquals(InjurySubType.IMPLANT_GENERIC, prostheticType.getInjuryType().getSubType());
        }
    }

    @Test
    @DisplayName("The suicide implant does not count towards the too-many-prosthetics threshold")
    public void suicideImplantDoesNotCountTowardsDegradation() {
        // isHasTooManyProsthetics counts only Type 4 and Type 5 prosthetics
        assertTrue(ProstheticType.EXPLOSIVE_SUICIDE_IMPLANT.getProstheticType() < 4);
    }

    @Test
    @DisplayName("The toxin effuser is Word of Blake technology, like the pheromone effuser")
    public void toxinEffuserIsWordOfBlakeOnly() {
        assertTrue(ProstheticType.TOXIN_EFFUSER.isWordOfBlakeOnly());
        assertTrue(ProstheticType.PHEROMONE_EFFUSER.isWordOfBlakeOnly());
    }

    @ParameterizedTest
    @EnumSource(ProstheticType.class)
    @DisplayName("Every prosthetic, and the record it leaves, has its name in the resource bundles")
    public void everyProstheticHasItsNames(ProstheticType prostheticType) {
        assertFalse(prostheticType.toString().contains("!ProstheticType."),
              () -> prostheticType.name() + " is missing its name: " + prostheticType);
        String simpleName = String.valueOf(prostheticType.getInjuryType().getSimpleName());
        assertFalse(simpleName.contains("!AlternateInjuries."),
              () -> prostheticType.name() + " leaves a record missing its name: " + simpleName);
    }

    @ParameterizedTest
    @EnumSource(value = ProstheticType.class, names = { "DERMAL_MYOMER_ARM_TRIPLE", "DERMAL_MYOMER_LEG_TRIPLE",
                                                        "PROSTHETIC_LEG_MASC" })
    @DisplayName("Prosthetics behind a derived implant explain it in their tooltip")
    public void derivedProstheticTooltipsHaveTheirText(ProstheticType prostheticType) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn("MERC");

        String tooltip = prostheticType.getTooltip(faction, 3075, 1.0, BodyLocation.GENERIC);

        assertFalse(tooltip.contains("!ProstheticType."),
              () -> prostheticType.name() + " has a tooltip line missing from the resource bundle: " + tooltip);
    }
}
