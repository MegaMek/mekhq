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

import megamek.client.generator.RandomNameGenerator;
import megamek.common.enums.Gender;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
class MergingSurnameStyleTest {
    // region Variable Declarations
    private static final MergingSurnameStyle[] styles = MergingSurnameStyle.values();

    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    @BeforeEach
    void beforeEach() {
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
    }

    // region Getters
    @Test
    void testGetToolTipText() {
        assertEquals(resources.getString("MergingSurnameStyle.YOURS.toolTipText"),
                MergingSurnameStyle.YOURS.getToolTipText());
        assertEquals(resources.getString("MergingSurnameStyle.WEIGHTED.toolTipText"),
                MergingSurnameStyle.WEIGHTED.getToolTipText());
    }

    @Test
    void testGetDropDownText() {
        assertEquals(resources.getString("MergingSurnameStyle.BOTH_HYPHEN_YOURS.dropDownText"),
                MergingSurnameStyle.BOTH_HYPHEN_YOURS.getDropDownText());
        assertEquals(resources.getString("MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.dropDownText"),
                MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.getDropDownText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsNoChange() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.NO_CHANGE) {
                assertTrue(mergingSurnameStyle.isNoChange());
            } else {
                assertFalse(mergingSurnameStyle.isNoChange());
            }
        }
    }

    @Test
    void testIsYours() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.YOURS) {
                assertTrue(mergingSurnameStyle.isYours());
            } else {
                assertFalse(mergingSurnameStyle.isYours());
            }
        }
    }

    @Test
    void testIsSpouse() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.SPOUSE) {
                assertTrue(mergingSurnameStyle.isSpouse());
            } else {
                assertFalse(mergingSurnameStyle.isSpouse());
            }
        }
    }

    @Test
    void testIsSpaceYours() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.SPACE_YOURS) {
                assertTrue(mergingSurnameStyle.isSpaceYours());
            } else {
                assertFalse(mergingSurnameStyle.isSpaceYours());
            }
        }
    }

    @Test
    void testIsBothSpaceYours() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.BOTH_SPACE_YOURS) {
                assertTrue(mergingSurnameStyle.isBothSpaceYours());
            } else {
                assertFalse(mergingSurnameStyle.isBothSpaceYours());
            }
        }
    }

    @Test
    void testIsHyphenYours() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.HYPHEN_YOURS) {
                assertTrue(mergingSurnameStyle.isHyphenYours());
            } else {
                assertFalse(mergingSurnameStyle.isHyphenYours());
            }
        }
    }

    @Test
    void testIsBothHyphenYours() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.BOTH_HYPHEN_YOURS) {
                assertTrue(mergingSurnameStyle.isBothHyphenYours());
            } else {
                assertFalse(mergingSurnameStyle.isBothHyphenYours());
            }
        }
    }

    @Test
    void testIsSpaceSpouse() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.SPACE_SPOUSE) {
                assertTrue(mergingSurnameStyle.isSpaceSpouse());
            } else {
                assertFalse(mergingSurnameStyle.isSpaceSpouse());
            }
        }
    }

    @Test
    void testIsBothSpaceSpouse() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.BOTH_SPACE_SPOUSE) {
                assertTrue(mergingSurnameStyle.isBothSpaceSpouse());
            } else {
                assertFalse(mergingSurnameStyle.isBothSpaceSpouse());
            }
        }
    }

    @Test
    void testIsHyphenSpouse() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.HYPHEN_SPOUSE) {
                assertTrue(mergingSurnameStyle.isHyphenSpouse());
            } else {
                assertFalse(mergingSurnameStyle.isHyphenSpouse());
            }
        }
    }

    @Test
    void testIsBothHyphenSpouse() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.BOTH_HYPHEN_SPOUSE) {
                assertTrue(mergingSurnameStyle.isBothHyphenSpouse());
            } else {
                assertFalse(mergingSurnameStyle.isBothHyphenSpouse());
            }
        }
    }

    @Test
    void testIsMale() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.MALE) {
                assertTrue(mergingSurnameStyle.isMale());
            } else {
                assertFalse(mergingSurnameStyle.isMale());
            }
        }
    }

    @Test
    void testIsFemale() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.FEMALE) {
                assertTrue(mergingSurnameStyle.isFemale());
            } else {
                assertFalse(mergingSurnameStyle.isFemale());
            }
        }
    }

    @Test
    void testIsWeighted() {
        for (final MergingSurnameStyle mergingSurnameStyle : styles) {
            if (mergingSurnameStyle == MergingSurnameStyle.WEIGHTED) {
                assertTrue(mergingSurnameStyle.isWeighted());
            } else {
                assertFalse(mergingSurnameStyle.isWeighted());
            }
        }
    }
    // endregion Boolean Comparison Methods

    @Test
    void testApplyNoChange() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        origin.setSurname("origin");
        final Person spouse = new Person(mockCampaign);
        spouse.setSurname("spouse");

        MergingSurnameStyle.NO_CHANGE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());
    }

    @Test
    void testApplyYours() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(true);

        final Person origin = new Person(mockCampaign);
        origin.setSurname("origin");
        final Person spouse = new Person(mockCampaign);
        spouse.setSurname("spouse");

        MergingSurnameStyle.YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());
    }

    @Test
    void testApplySpouse() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(true);

        final Person origin = new Person(mockCampaign);
        origin.setSurname("origin");
        final Person spouse = new Person(mockCampaign);
        spouse.setSurname("spouse");

        MergingSurnameStyle.SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());
    }

    @Test
    void testApplySpaceYours() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setSurname("origin");
        spouse.setSurname("");
        MergingSurnameStyle.SPACE_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());

        origin.setSurname("");
        spouse.setSurname("spouse");
        MergingSurnameStyle.SPACE_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("", origin.getSurname());
        assertEquals("", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("spouse");
        MergingSurnameStyle.SPACE_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("spouse origin", spouse.getSurname());
    }

    @Test
    void testApplyBothSpaceYours() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setSurname("");
        spouse.setSurname("");
        MergingSurnameStyle.BOTH_SPACE_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("", origin.getSurname());
        assertEquals("", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("");
        MergingSurnameStyle.BOTH_SPACE_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());

        origin.setSurname("");
        spouse.setSurname("spouse");
        MergingSurnameStyle.BOTH_SPACE_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("spouse");
        MergingSurnameStyle.BOTH_SPACE_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse origin", origin.getSurname());
        assertEquals("spouse origin", spouse.getSurname());
    }

    @Test
    void testApplyHyphenYours() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setSurname("origin");
        spouse.setSurname("");
        MergingSurnameStyle.HYPHEN_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());

        origin.setSurname("");
        spouse.setSurname("spouse");
        MergingSurnameStyle.HYPHEN_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("", origin.getSurname());
        assertEquals("", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("spouse");
        MergingSurnameStyle.HYPHEN_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("spouse-origin", spouse.getSurname());
    }

    @Test
    void testApplyBothHyphenYours() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setSurname("");
        spouse.setSurname("");
        MergingSurnameStyle.BOTH_HYPHEN_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("", origin.getSurname());
        assertEquals("", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("");
        MergingSurnameStyle.BOTH_HYPHEN_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());

        origin.setSurname("");
        spouse.setSurname("spouse");
        MergingSurnameStyle.BOTH_HYPHEN_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("spouse");
        MergingSurnameStyle.BOTH_HYPHEN_YOURS.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse-origin", origin.getSurname());
        assertEquals("spouse-origin", spouse.getSurname());
    }

    @Test
    void testApplySpaceSpouse() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setSurname("origin");
        spouse.setSurname("");
        MergingSurnameStyle.SPACE_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("", origin.getSurname());
        assertEquals("", spouse.getSurname());

        origin.setSurname("");
        spouse.setSurname("spouse");
        MergingSurnameStyle.SPACE_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("spouse");
        MergingSurnameStyle.SPACE_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());
    }

    @Test
    void testApplyBothSpaceSpouse() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setSurname("");
        spouse.setSurname("");
        MergingSurnameStyle.BOTH_SPACE_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("", origin.getSurname());
        assertEquals("", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("");
        MergingSurnameStyle.BOTH_SPACE_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());

        origin.setSurname("");
        spouse.setSurname("spouse");
        MergingSurnameStyle.BOTH_SPACE_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("spouse");
        MergingSurnameStyle.BOTH_SPACE_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin spouse", origin.getSurname());
        assertEquals("origin spouse", spouse.getSurname());
    }

    @Test
    void testApplyHyphenSpouse() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setSurname("origin");
        spouse.setSurname("");
        MergingSurnameStyle.HYPHEN_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("", origin.getSurname());
        assertEquals("", spouse.getSurname());

        origin.setSurname("");
        spouse.setSurname("spouse");
        MergingSurnameStyle.HYPHEN_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("spouse");
        MergingSurnameStyle.HYPHEN_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin-spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());
    }

    @Test
    void testApplyBothHyphenSpouse() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setSurname("");
        spouse.setSurname("");
        MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("", origin.getSurname());
        assertEquals("", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("");
        MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());

        origin.setSurname("");
        spouse.setSurname("spouse");
        MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());

        origin.setSurname("origin");
        spouse.setSurname("spouse");
        MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin-spouse", origin.getSurname());
        assertEquals("origin-spouse", spouse.getSurname());
    }

    @Test
    void testApplyMale() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setGender(Gender.MALE);
        origin.setSurname("origin");
        spouse.setGender(Gender.FEMALE);
        spouse.setSurname("spouse");

        MergingSurnameStyle.MALE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());

        origin.setGender(Gender.FEMALE);
        origin.setSurname("origin");
        spouse.setGender(Gender.MALE);
        spouse.setSurname("spouse");
        MergingSurnameStyle.MALE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());
    }

    @Test
    void testApplyFemale() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        origin.setGender(Gender.MALE);
        origin.setSurname("origin");
        spouse.setGender(Gender.FEMALE);
        spouse.setSurname("spouse");

        MergingSurnameStyle.FEMALE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("spouse", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());

        origin.setGender(Gender.FEMALE);
        origin.setSurname("origin");
        spouse.setGender(Gender.MALE);
        spouse.setSurname("spouse");
        MergingSurnameStyle.FEMALE.apply(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("origin", spouse.getSurname());
    }

    @Test
    void testApplyWeighted() {
        when(mockCampaignOptions.isLogMarriageNameChanges()).thenReturn(false);

        final WeightedIntMap<MergingSurnameStyle> weightMap = new WeightedIntMap<>();
        weightMap.add(1, MergingSurnameStyle.WEIGHTED);

        final MergingSurnameStyle mockStyle = mock(MergingSurnameStyle.class);
        doCallRealMethod().when(mockStyle).apply(any(), any(), any(), any());
        when(mockStyle.isWeighted()).thenReturn(true);
        when(mockStyle.createWeightedSurnameMap(any())).thenReturn(weightMap);

        final Person person = new Person(mockCampaign);
        mockStyle.apply(mockCampaign, LocalDate.of(3025, 1, 1),
                person, mock(Person.class));
        assertEquals(RandomNameGenerator.UNNAMED_SURNAME, person.getSurname());
    }

    @Test
    void testCreateWeightedSurnameMap() {
        final Map<MergingSurnameStyle, Integer> weights = new HashMap<>();
        for (final MergingSurnameStyle style : styles) {
            weights.put(style, 1);
        }
        assertFalse(MergingSurnameStyle.WEIGHTED.createWeightedSurnameMap(weights)
                .containsValue(MergingSurnameStyle.WEIGHTED));
    }

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(MergingSurnameStyle.NO_CHANGE, MergingSurnameStyle.parseFromString("NO_CHANGE"));
        assertEquals(MergingSurnameStyle.BOTH_SPACE_SPOUSE, MergingSurnameStyle.parseFromString("BOTH_SPACE_SPOUSE"));

        // Error Case
        assertEquals(MergingSurnameStyle.FEMALE, MergingSurnameStyle.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("MergingSurnameStyle.BOTH_SPACE_SPOUSE.text"),
                MergingSurnameStyle.BOTH_SPACE_SPOUSE.toString());
        assertEquals(resources.getString("MergingSurnameStyle.WEIGHTED.text"),
                MergingSurnameStyle.WEIGHTED.toString());
    }
}
