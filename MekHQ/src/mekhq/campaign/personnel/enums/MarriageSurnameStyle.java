/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.logging.LogLevel;
import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import megamek.common.util.WeightedMap;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public enum MarriageSurnameStyle {
    //region Enum Declarations
    NO_CHANGE("MarriageSurnameStyle.NO_CHANGE.text", "MarriageSurnameStyle.NO_CHANGE.toolTipText", "MarriageSurnameStyle.NO_CHANGE.dropDownText"),
    YOURS("MarriageSurnameStyle.YOURS.text", "MarriageSurnameStyle.YOURS.toolTipText", "MarriageSurnameStyle.YOURS.dropDownText"),
    SPOUSE("MarriageSurnameStyle.SPOUSE.text", "MarriageSurnameStyle.SPOUSE.toolTipText", "MarriageSurnameStyle.SPOUSE.dropDownText"),

    SPACE_YOURS("MarriageSurnameStyle.SPACE_YOURS.text", "MarriageSurnameStyle.SPACE_YOURS.toolTipText", "MarriageSurnameStyle.SPACE_YOURS.dropDownText"),
    BOTH_SPACE_YOURS("MarriageSurnameStyle.BOTH_SPACE_YOURS.text", "MarriageSurnameStyle.BOTH_SPACE_YOURS.toolTipText", "MarriageSurnameStyle.BOTH_SPACE_YOURS.dropDownText"),
    HYP_YOURS("MarriageSurnameStyle.HYP_YOURS.text", "MarriageSurnameStyle.HYP_YOURS.toolTipText", "MarriageSurnameStyle.HYP_YOURS.dropDownText"),
    BOTH_HYP_YOURS("MarriageSurnameStyle.BOTH_HYP_YOURS.text", "MarriageSurnameStyle.BOTH_HYP_YOURS.toolTipText", "MarriageSurnameStyle.BOTH_HYP_YOURS.dropDownText"),

    SPACE_SPOUSE("MarriageSurnameStyle.SPACE_SPOUSE.text", "MarriageSurnameStyle.SPACE_SPOUSE.toolTipText", "MarriageSurnameStyle.SPACE_SPOUSE.dropDownText"),
    BOTH_SPACE_SPOUSE("MarriageSurnameStyle.BOTH_SPACE_SPOUSE.text", "MarriageSurnameStyle.BOTH_SPACE_SPOUSE.toolTipText", "MarriageSurnameStyle.BOTH_SPACE_SPOUSE.dropDownText"),
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
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    MarriageSurnameStyle(String name, String toolTipText, String dropDownText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.dropDownText = resources.getString(dropDownText);
    }
    //endregion Constructors

    //region Getters
    public String getName() {
        return name;
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public String getDropDownText() {
        return dropDownText;
    }
    //endregion Getters

    public void generateAndAssignSurnames(Person origin, Person spouse, Campaign campaign) {
        String surname = origin.getSurname();
        String spouseSurname = spouse.getSurname();
        MarriageSurnameStyle style = this;


        if (style == WEIGHTED) {
            WeightedMap<MarriageSurnameStyle> map = createWeightedSurnameMap(campaign);
            style = map.randomItem();
        }

        switch (style) {
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
                    origin.setSurname(surname + " " + spouseSurname);
                } else {
                    origin.setSurname(spouseSurname);
                }
                break;
            case BOTH_SPACE_YOURS:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(surname + " " + spouseSurname);
                    spouse.setSurname(surname + " " + spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case HYP_YOURS:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(surname + "-" + spouseSurname);
                } else {
                    origin.setSurname(spouseSurname);
                }
                break;
            case BOTH_HYP_YOURS:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(surname + "-" + spouseSurname);
                    spouse.setSurname(surname + "-" + spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case SPACE_SPOUSE:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    spouse.setSurname(spouseSurname + " " + surname);
                } else {
                    spouse.setSurname(surname);
                }
                break;
            case BOTH_SPACE_SPOUSE:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname + " " + surname);
                    spouse.setSurname(spouseSurname + " " + surname);
                } else if (!StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case HYP_SPOUSE:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    spouse.setSurname(spouseSurname + "-" + surname);
                } else {
                    spouse.setSurname(surname);
                }
                break;
            case BOTH_HYP_SPOUSE:
                if (!StringUtil.isNullOrEmpty(surname) && !StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname + "-" + surname);
                    spouse.setSurname(spouseSurname + "-" + surname);
                } else if (!StringUtil.isNullOrEmpty(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtil.isNullOrEmpty(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case MALE:
                if (origin.isMale()) {
                    spouse.setSurname(surname);
                } else {
                    origin.setSurname(spouseSurname);
                }
                break;
            case FEMALE:
                if (origin.isMale()) {
                    origin.setSurname(spouseSurname);
                } else {
                    spouse.setSurname(surname);
                }
                break;
            case WEIGHTED:
            default:
                MekHQ.getLogger().log(getClass(), "marry", LogLevel.ERROR,
                        String.format("Marriage Surname Style is not defined, and cannot be used \"%s\" and \"%s\"",
                                origin.getFullName(), spouse.getFullName()));
                break;
        }

        origin.setMaidenName(surname);
        spouse.setMaidenName(spouseSurname);
    }


    private WeightedMap<MarriageSurnameStyle> createWeightedSurnameMap(Campaign campaign) {
        WeightedMap<MarriageSurnameStyle> map = new WeightedMap<>();

        int[] weights = campaign.getCampaignOptions().getRandomMarriageSurnameWeights();
        MarriageSurnameStyle[] styles = MarriageSurnameStyle.values();
        for (int i = 0; i < (styles.length - 1); i++) {
            map.add(weights[i], styles[i]);
        }

        return map;
    }
}
