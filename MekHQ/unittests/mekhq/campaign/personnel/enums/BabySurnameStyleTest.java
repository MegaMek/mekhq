/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BabySurnameStyleTest {
    //region Variable Declarations
    private static final BabySurnameStyle[] styles = BabySurnameStyle.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("BabySurnameStyle.FATHERS.toolTipText"),
                BabySurnameStyle.FATHERS.getToolTipText());
        assertEquals(resources.getString("BabySurnameStyle.WELSH_MATRONYMICS.toolTipText"),
                BabySurnameStyle.WELSH_MATRONYMICS.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsFathers() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.FATHERS) {
                assertTrue(babySurnameStyle.isFathers());
            } else {
                assertFalse(babySurnameStyle.isFathers());
            }
        }
    }

    @Test
    public void testIsMothers() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.MOTHERS) {
                assertTrue(babySurnameStyle.isMothers());
            } else {
                assertFalse(babySurnameStyle.isMothers());
            }
        }
    }

    @Test
    public void testIsMothersFathers() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.MOTHERS_FATHERS) {
                assertTrue(babySurnameStyle.isMothersFathers());
            } else {
                assertFalse(babySurnameStyle.isMothersFathers());
            }
        }
    }

    @Test
    public void testIsMothersHyphenFathers() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.MOTHERS_HYPHEN_FATHERS) {
                assertTrue(babySurnameStyle.isMothersHyphenFathers());
            } else {
                assertFalse(babySurnameStyle.isMothersHyphenFathers());
            }
        }
    }

    @Test
    public void testIsFathersMothers() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.FATHERS_MOTHERS) {
                assertTrue(babySurnameStyle.isFathersMothers());
            } else {
                assertFalse(babySurnameStyle.isFathersMothers());
            }
        }
    }

    @Test
    public void testIsFathersHyphenMothers() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.FATHERS_HYPHEN_MOTHERS) {
                assertTrue(babySurnameStyle.isFathersHyphenMothers());
            } else {
                assertFalse(babySurnameStyle.isFathersHyphenMothers());
            }
        }
    }

    @Test
    public void testIsWelshPatronymics() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.WELSH_PATRONYMICS) {
                assertTrue(babySurnameStyle.isWelshPatronymics());
            } else {
                assertFalse(babySurnameStyle.isWelshPatronymics());
            }
        }
    }

    @Test
    public void testIsWelshMatronymics() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.WELSH_MATRONYMICS) {
                assertTrue(babySurnameStyle.isWelshMatronymics());
            } else {
                assertFalse(babySurnameStyle.isWelshMatronymics());
            }
        }
    }

    @Test
    public void testIsIcelandicPatronymics() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.ICELANDIC_PATRONYMICS) {
                assertTrue(babySurnameStyle.isIcelandicPatronymics());
            } else {
                assertFalse(babySurnameStyle.isIcelandicPatronymics());
            }
        }
    }

    @Test
    public void testIsIcelandicMatronymics() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.ICELANDIC_MATRONYMICS) {
                assertTrue(babySurnameStyle.isIcelandicMatronymics());
            } else {
                assertFalse(babySurnameStyle.isIcelandicMatronymics());
            }
        }
    }

    @Test
    public void testIsIcelandicCombinationNymics() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.ICELANDIC_COMBINATION_NYMICS) {
                assertTrue(babySurnameStyle.isIcelandicCombinationNymics());
            } else {
                assertFalse(babySurnameStyle.isIcelandicCombinationNymics());
            }
        }
    }

    @Test
    public void testIsRussianPatronymics() {
        for (final BabySurnameStyle babySurnameStyle : styles) {
            if (babySurnameStyle == BabySurnameStyle.RUSSIAN_PATRONYMICS) {
                assertTrue(babySurnameStyle.isRussianPatronymics());
            } else {
                assertFalse(babySurnameStyle.isRussianPatronymics());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGenerateBabySurnameBaseline() {
        final Person mother = mock(Person.class);
        final Person father = mock(Person.class);

    }

    @Test
    public void testGenerateBabySurnameWelsh() {
        final Person mother = mock(Person.class);
        final Person father = mock(Person.class);

        // Patronymics

        // Matronymics
    }

    @Test
    public void testGenerateBabySurnameIcelandic() {
        final Person mother = mock(Person.class);
        final Person father = mock(Person.class);

        // Patronymics

        // Matronymics

        // Combined Nymics
    }

    @Test
    public void testGenerateBabySurnameRussianPatronymics() {
        final Person mother = mock(Person.class);
        when(mother.getSurname()).thenReturn("Mother");

        final Person father = mock(Person.class);

        // Rada - Expect Radevich / Radevna
        when(father.getGivenName()).thenReturn("Rada");
        assertEquals("Radevich", BabySurnameStyle.RUSSIAN_PATRONYMICS.generateBabySurname(mother, father, Gender.MALE));
        assertEquals("Radevna", BabySurnameStyle.RUSSIAN_PATRONYMICS.generateBabySurname(mother, father, Gender.FEMALE));

        // Dimitri - Expect Dimitrevich / Dimitrevna
        when(father.getGivenName()).thenReturn("Dimitri");
        assertEquals("Dimitrevich", BabySurnameStyle.RUSSIAN_PATRONYMICS.generateBabySurname(mother, father, Gender.MALE));
        assertEquals("Dimitrevna", BabySurnameStyle.RUSSIAN_PATRONYMICS.generateBabySurname(mother, father, Gender.FEMALE));

        // Ivan - Expect Ivanovich / Ivanova
        when(father.getGivenName()).thenReturn("Ivan");
        assertEquals("Ivanovich", BabySurnameStyle.RUSSIAN_PATRONYMICS.generateBabySurname(mother, father, Gender.MALE));
        assertEquals("Ivanovna", BabySurnameStyle.RUSSIAN_PATRONYMICS.generateBabySurname(mother, father, Gender.FEMALE));

        // Null Father - Expect Mother's Surname
        assertEquals("Mother", BabySurnameStyle.RUSSIAN_PATRONYMICS.generateBabySurname(mother, null, Gender.FEMALE));
    }

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(BabySurnameStyle.MOTHERS, BabySurnameStyle.parseFromString("MOTHERS"));
        assertEquals(BabySurnameStyle.ICELANDIC_MATRONYMICS, BabySurnameStyle.parseFromString("ICELANDIC_MATRONYMICS"));

        // Legacy Parsing - Enum Renames
        assertEquals(BabySurnameStyle.MOTHERS_HYPHEN_FATHERS, BabySurnameStyle.parseFromString("MOTHERS_HYP_FATHERS"));
        assertEquals(BabySurnameStyle.FATHERS_HYPHEN_MOTHERS, BabySurnameStyle.parseFromString("FATHERS_HYP_MOTHERS"));

        // Legacy Parsing - Magic Numbers
        assertEquals(BabySurnameStyle.MOTHERS, BabySurnameStyle.parseFromString("0"));
        assertEquals(BabySurnameStyle.FATHERS, BabySurnameStyle.parseFromString("1"));

        // Error Case
        assertEquals(BabySurnameStyle.MOTHERS, BabySurnameStyle.parseFromString("2"));
        assertEquals(BabySurnameStyle.MOTHERS, BabySurnameStyle.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("BabySurnameStyle.RUSSIAN_PATRONYMICS.text"), BabySurnameStyle.RUSSIAN_PATRONYMICS.toString());
        assertEquals(resources.getString("BabySurnameStyle.ICELANDIC_MATRONYMICS.text"), BabySurnameStyle.ICELANDIC_MATRONYMICS.toString());
    }
}
