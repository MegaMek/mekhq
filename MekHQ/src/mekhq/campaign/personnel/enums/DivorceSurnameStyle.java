/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.util.EncodeControl;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

import java.util.Map;
import java.util.ResourceBundle;

public enum DivorceSurnameStyle {
    //region Enum Declarations
    ORIGIN_CHANGES_SURNAME("DivorceSurnameStyle.ORIGIN_CHANGES_SURNAME.text", "DivorceSurnameStyle.ORIGIN_CHANGES_SURNAME.toolTipText", "DivorceSurnameStyle.ORIGIN_CHANGES_SURNAME.dropDownText"),
    SPOUSE_CHANGES_SURNAME("DivorceSurnameStyle.SPOUSE_CHANGES_SURNAME.text", "DivorceSurnameStyle.SPOUSE_CHANGES_SURNAME.toolTipText", "DivorceSurnameStyle.SPOUSE_CHANGES_SURNAME.dropDownText"),
    BOTH_CHANGE_SURNAME("DivorceSurnameStyle.BOTH_CHANGE_SURNAME.text", "DivorceSurnameStyle.BOTH_CHANGE_SURNAME.toolTipText", "DivorceSurnameStyle.BOTH_CHANGE_SURNAME.dropDownText"),
    BOTH_KEEP_SURNAME("DivorceSurnameStyle.BOTH_KEEP_SURNAME.text", "DivorceSurnameStyle.BOTH_KEEP_SURNAME.toolTipText", "DivorceSurnameStyle.BOTH_KEEP_SURNAME.dropDownText"),
    WEIGHTED("DivorceSurnameStyle.WEIGHTED.text", "DivorceSurnameStyle.WEIGHTED.toolTipText", "DivorceSurnameStyle.WEIGHTED.dropDownText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String dropDownText;
    //endregion Variable Declarations

    //region Constructors
    DivorceSurnameStyle(final String name, final String toolTipText, final String dropDownText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel", new EncodeControl());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.dropDownText = resources.getString(dropDownText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public String getDropDownText() {
        return dropDownText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isOriginChangesSurname() {
        return this == ORIGIN_CHANGES_SURNAME;
    }

    public boolean isSpouseChangesSurname() {
        return this == SPOUSE_CHANGES_SURNAME;
    }

    public boolean isBothChangeSurname() {
        return this == BOTH_CHANGE_SURNAME;
    }

    public boolean isBothKeepSurname() {
        return this == BOTH_KEEP_SURNAME;
    }

    public boolean isWeighted() {
        return this == WEIGHTED;
    }
    //endregion Boolean Comparison Methods

    /**
     * This applies the surname changes that occur during a divorce
     * @param campaign the campaign to use in processing
     * @param origin the origin person
     * @param spouse the origin person's former spouse
     */
    public void apply(final Campaign campaign, final Person origin, final Person spouse) {
        final DivorceSurnameStyle surnameStyle = isWeighted()
                ? createWeightedSurnameMap(campaign.getCampaignOptions().getDivorceSurnameWeights()).randomItem()
                : this;

        switch (surnameStyle) {
            case ORIGIN_CHANGES_SURNAME:
                if (origin.getMaidenName() != null) {
                    origin.setSurname(origin.getMaidenName());
                }
                break;
            case SPOUSE_CHANGES_SURNAME:
                if (spouse.getMaidenName() != null) {
                    spouse.setSurname(spouse.getMaidenName());
                }
                break;
            case BOTH_CHANGE_SURNAME:
                if (origin.getMaidenName() != null) {
                    origin.setSurname(origin.getMaidenName());
                }

                if (spouse.getMaidenName() != null) {
                    spouse.setSurname(spouse.getMaidenName());
                }
                break;
            case BOTH_KEEP_SURNAME:
            default:
                break;
        }
    }

    private WeightedIntMap<DivorceSurnameStyle> createWeightedSurnameMap(
            final Map<DivorceSurnameStyle, Integer> weights) {
        final WeightedIntMap<DivorceSurnameStyle> map = new WeightedIntMap<>();
        for (final DivorceSurnameStyle style : DivorceSurnameStyle.values()) {
            if (style.isWeighted()) {
                continue;
            }
            map.add(weights.get(style), style);
        }
        return map;
    }

    @Override
    public String toString() {
        return name;
    }
}
