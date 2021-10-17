/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All rights reserved.
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
import megamek.common.util.StringUtil;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;

import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;

public enum MarriageSurnameStyle {
    //region Enum Declarations
    NO_CHANGE("MarriageSurnameStyle.NO_CHANGE.text", "MarriageSurnameStyle.NO_CHANGE.toolTipText", "MarriageSurnameStyle.NO_CHANGE.dropDownText"),
    YOURS("MarriageSurnameStyle.YOURS.text", "MarriageSurnameStyle.YOURS.toolTipText", "MarriageSurnameStyle.YOURS.dropDownText"),
    SPOUSE("MarriageSurnameStyle.SPOUSE.text", "MarriageSurnameStyle.SPOUSE.toolTipText", "MarriageSurnameStyle.SPOUSE.dropDownText"),

    SPACE_YOURS("MarriageSurnameStyle.SPACE_YOURS.text", "MarriageSurnameStyle.SPACE_YOURS.toolTipText", "MarriageSurnameStyle.SPACE_YOURS.dropDownText"),
    BOTH_SPACE_YOURS( "MarriageSurnameStyle.BOTH_SPACE_YOURS.text", "MarriageSurnameStyle.BOTH_SPACE_YOURS.toolTipText", "MarriageSurnameStyle.BOTH_SPACE_YOURS.dropDownText"),
    HYP_YOURS("MarriageSurnameStyle.HYP_YOURS.text", "MarriageSurnameStyle.HYP_YOURS.toolTipText", "MarriageSurnameStyle.HYP_YOURS.dropDownText"),
    BOTH_HYP_YOURS("MarriageSurnameStyle.BOTH_HYP_YOURS.text", "MarriageSurnameStyle.BOTH_HYP_YOURS.toolTipText", "MarriageSurnameStyle.BOTH_HYP_YOURS.dropDownText"),

    SPACE_SPOUSE("MarriageSurnameStyle.SPACE_SPOUSE.text", "MarriageSurnameStyle.SPACE_SPOUSE.toolTipText", "MarriageSurnameStyle.SPACE_SPOUSE.dropDownText"),
    BOTH_SPACE_SPOUSE( "MarriageSurnameStyle.BOTH_SPACE_SPOUSE.text", "MarriageSurnameStyle.BOTH_SPACE_SPOUSE.toolTipText", "MarriageSurnameStyle.BOTH_SPACE_SPOUSE.dropDownText"),
    HYP_SPOUSE("MarriageSurnameStyle.HYP_SPOUSE.text", "MarriageSurnameStyle.HYP_SPOUSE.toolTipText", "MarriageSurnameStyle.HYP_SPOUSE.dropDownText"),
    BOTH_HYP_SPOUSE("MarriageSurnameStyle.BOTH_HYP_SPOUSE.text", "MarriageSurnameStyle.BOTH_HYP_SPOUSE.toolTipText", "MarriageSurnameStyle.BOTH_HYP_SPOUSE.dropDownText"),

    MALE("MarriageSurnameStyle.MALE.text", "MarriageSurnameStyle.MALE.toolTipText", "MarriageSurnameStyle.MALE.dropDownText"),
    FEMALE("MarriageSurnameStyle.FEMALE.text", "MarriageSurnameStyle.FEMALE.toolTipText", "MarriageSurnameStyle.FEMALE.dropDownText"),
    WEIGHTED("MarriageSurnameStyle.WEIGHTED.text", "MarriageSurnameStyle.WEIGHTED.toolTipText", "MarriageSurnameStyle.WEIGHTED.dropDownText");
    // NOTE: WEIGHTED MUST be the last option, or otherwise the WeightedMap creation method must change
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String dropDownText;
    //endregion Variable Declarations

    //region Constructors
    MarriageSurnameStyle(final String name, final String toolTipText, final String dropDownText) {
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
    public boolean isNoChange() {
        return this == NO_CHANGE;
    }

    public boolean isYours() {
        return this == YOURS;
    }

    public boolean isSpouse() {
        return this == SPOUSE;
    }

    public boolean isSpaceYours() {
        return this == SPACE_YOURS;
    }

    public boolean isBothSpaceYours() {
        return this == BOTH_SPACE_YOURS;
    }

    public boolean isHypYours() {
        return this == HYP_YOURS;
    }

    public boolean isBothHypYours() {
        return this == BOTH_HYP_YOURS;
    }

    public boolean isSpaceSpouse() {
        return this == SPACE_SPOUSE;
    }

    public boolean isBothSpaceSpouse() {
        return this == BOTH_SPACE_SPOUSE;
    }

    public boolean isHypSpouse() {
        return this == HYP_SPOUSE;
    }

    public boolean isBothHypSpouse() {
        return this == BOTH_HYP_SPOUSE;
    }

    public boolean isMale() {
        return this == MALE;
    }

    public boolean isFemale() {
        return this == FEMALE;
    }

    public boolean isWeighted() {
        return this == WEIGHTED;
    }
    //endregion Boolean Comparison Methods

    /**
     * This applies the surname changes that occur during a marriage
     * @param campaign the campaign to use in processing
     * @param today the current day
     * @param origin the origin person
     * @param spouse the origin person's new spouse
     */
    public void apply(final Campaign campaign, final LocalDate today, final Person origin,
                      final Person spouse) {
        final String surname = origin.getSurname();
        final String spouseSurname = spouse.getSurname();
        final MarriageSurnameStyle surnameStyle = isWeighted()
                ? createWeightedSurnameMap(campaign.getCampaignOptions().getMarriageSurnameWeights()).randomItem()
                : this;

        switch (surnameStyle) {
            case NO_CHANGE:
                break;
            case YOURS:
                spouse.setSurname(surname);
                break;
            case SPOUSE:
                origin.setSurname(spouseSurname);
                break;
            case SPACE_YOURS:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    spouse.setSurname(spouseSurname + " " + surname);
                } else {
                    spouse.setSurname(surname);
                }
                break;
            case BOTH_SPACE_YOURS:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname + " " + surname);
                    spouse.setSurname(spouseSurname + " " + surname);
                } else if (!StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case HYP_YOURS:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    spouse.setSurname(spouseSurname + "-" + surname);
                } else {
                    spouse.setSurname(surname);
                }
                break;
            case BOTH_HYP_YOURS:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname + "-" + surname);
                    spouse.setSurname(spouseSurname + "-" + surname);
                } else if (!StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case SPACE_SPOUSE:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(surname + " " + spouseSurname);
                } else {
                    origin.setSurname(spouseSurname);
                }
                break;
            case BOTH_SPACE_SPOUSE:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(surname + " " + spouseSurname);
                    spouse.setSurname(surname + " " + spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case HYP_SPOUSE:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(surname + "-" + spouseSurname);
                } else {
                    origin.setSurname(spouseSurname);
                }
                break;
            case BOTH_HYP_SPOUSE:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(surname + "-" + spouseSurname);
                    spouse.setSurname(surname + "-" + spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case MALE:
                if (origin.getGender().isMale()) {
                    spouse.setSurname(surname);
                } else {
                    origin.setSurname(spouseSurname);
                }
                break;
            case FEMALE:
                if (origin.getGender().isMale()) {
                    origin.setSurname(spouseSurname);
                } else {
                    spouse.setSurname(surname);
                }
                break;
            case WEIGHTED:
            default:
                MekHQ.getLogger().error(String.format("Marriage Surname Style is not defined, and cannot be used \"%s\" and \"%s\"",
                        origin.getFullName(), spouse.getFullName()));
                break;
        }

        if (campaign.getCampaignOptions().isLogMarriageNameChanges()) {
            if (!spouse.getSurname().equals(spouseSurname)) {
                PersonalLogger.marriageNameChange(spouse, origin, today);
            }

            if (!origin.getSurname().equals(surname)) {
                PersonalLogger.marriageNameChange(origin, spouse, today);
            }
        }
    }


    private WeightedIntMap<MarriageSurnameStyle> createWeightedSurnameMap(
            final Map<MarriageSurnameStyle, Integer> weights) {
        final WeightedIntMap<MarriageSurnameStyle> map = new WeightedIntMap<>();
        for (final MarriageSurnameStyle style : MarriageSurnameStyle.values()) {
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
