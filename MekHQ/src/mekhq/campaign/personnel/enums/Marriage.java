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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import megamek.common.util.WeightedMap;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public enum Marriage {
    //region Enum Declarations
    NO_CHANGE(0, "Marriage.NO_CHANGE.text", "Marriage.NO_CHANGE.toolTipText", "Marriage.NO_CHANGE.dropDownText"),
    YOURS(1, "Marriage.YOURS.text", "Marriage.YOURS.toolTipText", "Marriage.YOURS.dropDownText"),
    SPOUSE(2, "Marriage.SPOUSE.text", "Marriage.SPOUSE.toolTipText", "Marriage.SPOUSE.dropDownText"),

    SPACE_YOURS(3, "Marriage.SPACE_YOURS.text", "Marriage.SPACE_YOURS.toolTipText", "Marriage.SPACE_YOURS.dropDownText"),
    BOTH_SPACE_YOURS(4, "Marriage.BOTH_SPACE_YOURS.text", "Marriage.BOTH_SPACE_YOURS.toolTipText", "Marriage.BOTH_SPACE_YOURS.dropDownText"),
    HYP_YOURS(5, "Marriage.HYP_YOURS.text", "Marriage.HYP_YOURS.toolTipText", "Marriage.HYP_YOURS.dropDownText"),
    BOTH_HYP_YOURS(6, "Marriage.BOTH_HYP_YOURS.text", "Marriage.BOTH_HYP_YOURS.toolTipText", "Marriage.BOTH_HYP_YOURS.dropDownText"),

    SPACE_SPOUSE(7, "Marriage.SPACE_SPOUSE.text", "Marriage.SPACE_SPOUSE.toolTipText", "Marriage.SPACE_SPOUSE.dropDownText"),
    BOTH_SPACE_SPOUSE(8, "Marriage.BOTH_SPACE_SPOUSE.text", "Marriage.BOTH_SPACE_SPOUSE.toolTipText", "Marriage.BOTH_SPACE_SPOUSE.dropDownText"),
    HYP_SPOUSE(9, "Marriage.HYP_SPOUSE.text", "Marriage.HYP_SPOUSE.toolTipText", "Marriage.HYP_SPOUSE.dropDownText"),
    BOTH_HYP_SPOUSE(10, "Marriage.BOTH_HYP_SPOUSE.text", "Marriage.BOTH_HYP_SPOUSE.toolTipText", "Marriage.BOTH_HYP_SPOUSE.dropDownText"),

    MALE(11, "Marriage.MALE.text", "Marriage.MALE.toolTipText", "Marriage.MALE.dropDownText"),
    FEMALE(12, "Marriage.FEMALE.text", "Marriage.FEMALE.toolTipText", "Marriage.FEMALE.dropDownText"),
    WEIGHTED("Marriage.WEIGHTED.text", "Marriage.WEIGHTED.toolTipText", "Marriage.WEIGHTED.dropDownText");
    // NOTE: WEIGHTED MUST be the last option, or otherwise the WeightedMap creation method must change
    //endregion Enum Declarations

    //region Variable Declarations
    private final int weightsNumber;
    private final String name;
    private final String toolTipText;
    private final String dropDownText;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    Marriage(String name, String toolTipText, String dropDownText) {
        this(-1, name, toolTipText, dropDownText);
    }

    Marriage(int weightsNumber, String name, String toolTipText, String dropDownText) {
        this.weightsNumber = weightsNumber;
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.dropDownText = resources.getString(dropDownText);
    }
    //endregion Constructors

    //region Getters
    public int getWeightsNumber() {
        return weightsNumber;
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public String getDropDownText() {
        return dropDownText;
    }
    //endregion Getters

    public void marry(Person origin, Person spouse, Campaign campaign) {
        String surname = origin.getSurname();
        String spouseSurname = spouse.getSurname();
        Marriage surnameStyle = this;

        if (surnameStyle == WEIGHTED) {
            WeightedMap<Marriage> map = createWeightedSurnameMap(campaign);
            surnameStyle = map.randomItem();
        }

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
                MekHQ.getLogger().error(this, String.format("Marriage Surname Style is not defined, and cannot be used \"%s\" and \"%s\"",
                                origin.getFullName(), spouse.getFullName()));
                break;
        }

        // Now we set both Maiden Names, to avoid any divorce bugs (as the default is now an empty string)
        origin.setMaidenName(surname);
        spouse.setMaidenName(spouseSurname);

        origin.getGenealogy().setSpouse(spouse.getId());
        spouse.getGenealogy().setSpouse(origin.getId());

        // Then we do the logging
        PersonalLogger.marriage(origin, spouse, campaign.getLocalDate());
        PersonalLogger.marriage(spouse, origin, campaign.getLocalDate());

        if (campaign.getCampaignOptions().logMarriageNameChange()) {
            if (!spouse.getSurname().equals(spouseSurname)) {
                PersonalLogger.marriageNameChange(spouse, origin, campaign.getLocalDate());
            }
            if (!origin.getSurname().equals(surname)) {
                PersonalLogger.marriageNameChange(origin, spouse, campaign.getLocalDate());
            }
        }

        campaign.addReport(String.format("%s has married %s!", origin.getHyperlinkedName(),
                spouse.getHyperlinkedName()));

        // And finally we trigger person changed events
        MekHQ.triggerEvent(new PersonChangedEvent(origin));
        MekHQ.triggerEvent(new PersonChangedEvent(spouse));
    }


    private WeightedMap<Marriage> createWeightedSurnameMap(Campaign campaign) {
        WeightedMap<Marriage> map = new WeightedMap<>();

        int[] weights = campaign.getCampaignOptions().getRandomMarriageSurnameWeights();
        Marriage[] styles = Marriage.values();
        for (int i = 0; i < (styles.length - 1); i++) {
            map.add(weights[i], styles[i]);
        }

        return map;
    }

    @Override
    public String toString() {
        return name;
    }
}
