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
package mekhq.campaign.personnel.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests {@link AbilityChoices}: which abilities need a choice, the values offered, and the value chosen for each
 * person from the weapons on their unit.
 */
class AbilityChoicesTest {
    private static final String LASER = "Medium Laser";
    private static final String LRM = "LRM 10";
    private static final String AUTOCANNON = "AC/10";

    private static final SpecialAbility WEAPON_SPECIALIST =
          new SpecialAbility(OptionsConstants.GUNNERY_WEAPON_SPECIALIST);
    private static final SpecialAbility SANDBLASTER = new SpecialAbility(OptionsConstants.GUNNERY_SANDBLASTER);
    private static final SpecialAbility SPECIALIST = new SpecialAbility(OptionsConstants.GUNNERY_SPECIALIST);
    private static final SpecialAbility RANGE_MASTER = new SpecialAbility(OptionsConstants.GUNNERY_RANGE_MASTER);
    private static final SpecialAbility ENVIRONMENT_SPECIALIST =
          new SpecialAbility(OptionsConstants.MISC_ENV_SPECIALIST);
    private static final SpecialAbility HUMAN_TRO = new SpecialAbility(OptionsConstants.MISC_HUMAN_TRO);

    private WeaponType laser;
    private WeaponType lrm;
    private WeaponType autocannon;

    @BeforeEach
    void setUp() {
        laser = weaponType(WeaponType.F_ENERGY);
        lrm = weaponType(WeaponType.F_MISSILE);
        autocannon = weaponType(WeaponType.F_BALLISTIC);
    }

    private static WeaponType weaponType(WeaponTypeFlag weaponClass) {
        WeaponType type = mock(WeaponType.class);
        when(type.hasFlag(weaponClass)).thenReturn(true);
        return type;
    }

    private static WeaponMounted mounted(String name, WeaponType type) {
        WeaponMounted weapon = mock(WeaponMounted.class);
        when(weapon.getName()).thenReturn(name);
        when(weapon.getType()).thenReturn(type);
        return weapon;
    }

    /** @return a MekWarrior whose unit mounts the given weapons */
    private static Person crewing(WeaponMounted... weapons) {
        Entity entity = mock(Entity.class);
        when(entity.getWeaponList()).thenReturn(new ArrayList<>(List.of(weapons)));
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        Person person = mock(Person.class);
        when(person.getUnit()).thenReturn(unit);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        return person;
    }

    private static SpecialAbility custom(String... choices) {
        SpecialAbility ability = new SpecialAbility("custom_choice_ability");
        ability.getChoiceValues().addAll(List.of(choices));
        return ability;
    }

    @Nested
    class WhichNeedAChoice {
        @Test
        void theBuiltInChoiceAbilitiesDo() {
            for (SpecialAbility ability : List.of(WEAPON_SPECIALIST, SANDBLASTER, SPECIALIST, RANGE_MASTER,
                  ENVIRONMENT_SPECIALIST, HUMAN_TRO)) {
                assertTrue(AbilityChoices.needsChoice(ability), ability.getName());
            }
        }

        @Test
        void otherAbilitiesDoOnlyIfTheyListValuesOtherThanNone() {
            assertFalse(AbilityChoices.needsChoice(custom()));
            assertFalse(AbilityChoices.needsChoice(custom("None")));
            assertTrue(AbilityChoices.needsChoice(custom("None", "Alpha")));
        }

        @Test
        void onlyTheWeaponAbilitiesAreChosenForEachPerson() {
            assertTrue(AbilityChoices.canChooseForEachPerson(WEAPON_SPECIALIST));
            assertTrue(AbilityChoices.canChooseForEachPerson(SANDBLASTER));
            assertTrue(AbilityChoices.canChooseForEachPerson(SPECIALIST));
            assertFalse(AbilityChoices.canChooseForEachPerson(RANGE_MASTER));
            assertFalse(AbilityChoices.canChooseForEachPerson(custom("Alpha")));
        }
    }

    @Nested
    class SharedChoices {
        @Test
        void weaponAbilitiesOfferEveryWeaponOnTheirUnitsInOrder() {
            Person first = crewing(mounted(LRM, lrm), mounted(LASER, laser));
            Person second = crewing(mounted(LASER, laser), mounted(AUTOCANNON, autocannon));

            assertEquals(List.of(AUTOCANNON, LRM, LASER),
                  AbilityChoices.sharedChoices(WEAPON_SPECIALIST, List.of(first, second)));
        }

        @Test
        void peopleWithoutAUnitAddNoWeapons() {
            Person person = mock(Person.class);

            assertTrue(AbilityChoices.sharedChoices(WEAPON_SPECIALIST, List.of(person)).isEmpty());
        }

        @Test
        void fixedChoicesAreOffered() {
            assertEquals(List.of(Crew.SPECIAL_ENERGY, Crew.SPECIAL_BALLISTIC, Crew.SPECIAL_MISSILE),
                  AbilityChoices.sharedChoices(SPECIALIST, List.of()));
            assertEquals(List.of(Crew.RANGEMASTER_MEDIUM, Crew.RANGEMASTER_LONG, Crew.RANGEMASTER_EXTREME),
                  AbilityChoices.sharedChoices(RANGE_MASTER, List.of()));
            assertEquals(5, AbilityChoices.sharedChoices(ENVIRONMENT_SPECIALIST, List.of()).size());
            assertEquals(4, AbilityChoices.sharedChoices(HUMAN_TRO, List.of()).size());
        }

        @Test
        void otherAbilitiesOfferTheirListedValuesExceptNone() {
            assertEquals(List.of("Alpha", "Beta"),
                  AbilityChoices.sharedChoices(custom("Alpha", "none", "Beta"), List.of()));
        }
    }

    @Nested
    class ChoosingForEachPerson {
        @Test
        void weaponSpecialistPicksTheWeaponMountedMost() {
            Person person = crewing(mounted(LRM, lrm), mounted(LASER, laser), mounted(LASER, laser));

            assertEquals(LASER, AbilityChoices.choiceFor(WEAPON_SPECIALIST, person));
        }

        @Test
        void specialistPicksTheClassMostWeaponsBelongTo() {
            Person person = crewing(mounted(LASER, laser), mounted(LRM, lrm), mounted(LRM + " (2)", lrm));

            assertEquals(Crew.SPECIAL_MISSILE, AbilityChoices.choiceFor(SPECIALIST, person));
        }

        @Test
        void sandblasterOnlyPicksClusterWeapons() {
            Person person = crewing(mounted(LASER, laser), mounted(LASER, laser), mounted(LRM, lrm));

            try (MockedStatic<SpecialAbility> abilities = mockStatic(SpecialAbility.class, CALLS_REAL_METHODS)) {
                abilities.when(() -> SpecialAbility.isWeaponEligibleForSPA(any(), any(), anyBoolean()))
                      .thenReturn(false);
                abilities.when(() -> SpecialAbility.isWeaponEligibleForSPA(eq(lrm), any(), eq(true)))
                      .thenReturn(true);

                assertEquals(LRM, AbilityChoices.choiceFor(SANDBLASTER, person));
                assertEquals(List.of(LRM), AbilityChoices.sharedChoices(SANDBLASTER, List.of(person)));
            }
        }

        @Test
        void nothingIsPickedWithoutASuitableWeapon() {
            assertNull(AbilityChoices.choiceFor(WEAPON_SPECIALIST, crewing()));
            assertNull(AbilityChoices.choiceFor(WEAPON_SPECIALIST, mock(Person.class)));
        }

        @Test
        void abilitiesThePlayerMustPickAreNeverPicked() {
            assertNull(AbilityChoices.choiceFor(RANGE_MASTER, crewing(mounted(LASER, laser))));
        }
    }

    @Nested
    class Validity {
        @Test
        void aWeaponMustBeOnThePersonsUnit() {
            Person person = crewing(mounted(LASER, laser));

            assertTrue(AbilityChoices.isValidFor(WEAPON_SPECIALIST, person, LASER));
            assertFalse(AbilityChoices.isValidFor(WEAPON_SPECIALIST, person, LRM));
        }

        @Test
        void otherValuesMustBeListed() {
            Person person = mock(Person.class);

            assertTrue(AbilityChoices.isValidFor(RANGE_MASTER, person, Crew.RANGEMASTER_LONG));
            assertFalse(AbilityChoices.isValidFor(RANGE_MASTER, person, "Point Blank"));
        }
    }
}
