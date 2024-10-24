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
public class SplittingSurnameStyleTest {
    //region Variable Declarations
    private static final SplittingSurnameStyle[] styles = SplittingSurnameStyle.values();

    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
    }

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.toolTipText"),
                SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.getToolTipText());
        assertEquals(resources.getString("SplittingSurnameStyle.WEIGHTED.toolTipText"),
                SplittingSurnameStyle.WEIGHTED.getToolTipText());
    }

    @Test
    public void testGetDropDownText() {
        assertEquals(resources.getString("SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.dropDownText"),
                SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.getDropDownText());
        assertEquals(resources.getString("SplittingSurnameStyle.WEIGHTED.dropDownText"),
                SplittingSurnameStyle.WEIGHTED.getDropDownText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsOriginChangesSurname() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME) {
                assertTrue(style.isOriginChangesSurname());
            } else {
                assertFalse(style.isOriginChangesSurname());
            }
        }
    }

    @Test
    public void testIsSpouseChangesSurname() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.SPOUSE_CHANGES_SURNAME) {
                assertTrue(style.isSpouseChangesSurname());
            } else {
                assertFalse(style.isSpouseChangesSurname());
            }
        }
    }

    @Test
    public void testIsBothChangeSurname() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.BOTH_CHANGE_SURNAME) {
                assertTrue(style.isBothChangeSurname());
            } else {
                assertFalse(style.isBothChangeSurname());
            }
        }
    }

    @Test
    public void testIsBothKeepSurname() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.BOTH_KEEP_SURNAME) {
                assertTrue(style.isBothKeepSurname());
            } else {
                assertFalse(style.isBothKeepSurname());
            }
        }
    }

    @Test
    public void testIsWeighted() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.WEIGHTED) {
                assertTrue(style.isWeighted());
            } else {
                assertFalse(style.isWeighted());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testApplyOriginChangesSurname() {
        final Person person = new Person(mockCampaign);

        SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.apply(mockCampaign, person, mock(Person.class));
        assertEquals(RandomNameGenerator.UNNAMED_SURNAME, person.getSurname());

        person.setMaidenName("Testing");
        SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.apply(mockCampaign, person, mock(Person.class));
        assertEquals("Testing", person.getSurname());
    }

    @Test
    public void testApplySpouseChangesSurname() {
        final Person person = new Person(mockCampaign);

        SplittingSurnameStyle.SPOUSE_CHANGES_SURNAME.apply(mockCampaign, mock(Person.class), person);
        assertEquals(RandomNameGenerator.UNNAMED_SURNAME, person.getSurname());

        person.setMaidenName("Testing");
        SplittingSurnameStyle.SPOUSE_CHANGES_SURNAME.apply(mockCampaign, mock(Person.class), person);
        assertEquals("Testing", person.getSurname());
    }

    @Test
    public void testApplyBothChangeSurname() {
        final Person origin = new Person(mockCampaign);
        final Person spouse = new Person(mockCampaign);

        SplittingSurnameStyle.BOTH_CHANGE_SURNAME.apply(mockCampaign, origin, spouse);
        assertEquals(RandomNameGenerator.UNNAMED_SURNAME, origin.getSurname());
        assertEquals(RandomNameGenerator.UNNAMED_SURNAME, spouse.getSurname());

        origin.setMaidenName("origin");
        spouse.setMaidenName("spouse");
        SplittingSurnameStyle.BOTH_CHANGE_SURNAME.apply(mockCampaign, origin, spouse);
        assertEquals("origin", origin.getSurname());
        assertEquals("spouse", spouse.getSurname());
    }

    @Test
    public void testApplyBothKeepSurname() {
        final Person origin = new Person(mockCampaign);
        origin.setMaidenName("origin");

        final Person spouse = new Person(mockCampaign);
        spouse.setMaidenName("spouse");

        SplittingSurnameStyle.BOTH_KEEP_SURNAME.apply(mockCampaign, origin, spouse);
        assertEquals(RandomNameGenerator.UNNAMED_SURNAME, origin.getSurname());
        assertEquals(RandomNameGenerator.UNNAMED_SURNAME, spouse.getSurname());
    }

    @Test
    public void testApplyWeighted() {
        final WeightedIntMap<SplittingSurnameStyle> weightMap = new WeightedIntMap<>();
        weightMap.add(1, SplittingSurnameStyle.WEIGHTED);

        final SplittingSurnameStyle mockStyle = mock(SplittingSurnameStyle.class);
        doCallRealMethod().when(mockStyle).apply(any(), any(), any());
        when(mockStyle.isWeighted()).thenReturn(true);
        when(mockStyle.createWeightedSurnameMap(any())).thenReturn(weightMap);

        final Person person = new Person(mockCampaign);
        mockStyle.apply(mockCampaign, person, mock(Person.class));
        assertEquals(RandomNameGenerator.UNNAMED_SURNAME, person.getSurname());
    }

    @Test
    public void testCreateWeightedSurnameMap() {
        final Map<SplittingSurnameStyle, Integer> weights = new HashMap<>();
        for (final SplittingSurnameStyle style : styles) {
            weights.put(style, 1);
        }
        assertFalse(SplittingSurnameStyle.WEIGHTED.createWeightedSurnameMap(weights).containsValue(SplittingSurnameStyle.WEIGHTED));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.text"),
                SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.toString());
        assertEquals(resources.getString("SplittingSurnameStyle.WEIGHTED.text"),
                SplittingSurnameStyle.WEIGHTED.toString());
    }
}
