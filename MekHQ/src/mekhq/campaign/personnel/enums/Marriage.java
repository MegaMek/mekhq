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
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public enum Marriage {
    //region Enum Declarations
    NO_CHANGE("Marriage.NO_CHANGE.text", "Marriage.NO_CHANGE.toolTipText", "Marriage.NO_CHANGE.dropDownText"),
    YOURS("Marriage.YOURS.text", "Marriage.YOURS.toolTipText", "Marriage.YOURS.dropDownText"),
    SPOUSE("Marriage.SPOUSE.text", "Marriage.SPOUSE.toolTipText", "Marriage.SPOUSE.dropDownText"),

    SPACE_YOURS("Marriage.SPACE_YOURS.text", "Marriage.SPACE_YOURS.toolTipText", "Marriage.SPACE_YOURS.dropDownText"),
    BOTH_SPACE_YOURS( "Marriage.BOTH_SPACE_YOURS.text", "Marriage.BOTH_SPACE_YOURS.toolTipText", "Marriage.BOTH_SPACE_YOURS.dropDownText"),
    HYP_YOURS("Marriage.HYP_YOURS.text", "Marriage.HYP_YOURS.toolTipText", "Marriage.HYP_YOURS.dropDownText"),
    BOTH_HYP_YOURS("Marriage.BOTH_HYP_YOURS.text", "Marriage.BOTH_HYP_YOURS.toolTipText", "Marriage.BOTH_HYP_YOURS.dropDownText"),

    SPACE_SPOUSE("Marriage.SPACE_SPOUSE.text", "Marriage.SPACE_SPOUSE.toolTipText", "Marriage.SPACE_SPOUSE.dropDownText"),
    BOTH_SPACE_SPOUSE( "Marriage.BOTH_SPACE_SPOUSE.text", "Marriage.BOTH_SPACE_SPOUSE.toolTipText", "Marriage.BOTH_SPACE_SPOUSE.dropDownText"),
    HYP_SPOUSE("Marriage.HYP_SPOUSE.text", "Marriage.HYP_SPOUSE.toolTipText", "Marriage.HYP_SPOUSE.dropDownText"),
    BOTH_HYP_SPOUSE("Marriage.BOTH_HYP_SPOUSE.text", "Marriage.BOTH_HYP_SPOUSE.toolTipText", "Marriage.BOTH_HYP_SPOUSE.dropDownText"),

    MALE("Marriage.MALE.text", "Marriage.MALE.toolTipText", "Marriage.MALE.dropDownText"),
    FEMALE("Marriage.FEMALE.text", "Marriage.FEMALE.toolTipText", "Marriage.FEMALE.dropDownText"),
    WEIGHTED("Marriage.WEIGHTED.text", "Marriage.WEIGHTED.toolTipText", "Marriage.WEIGHTED.dropDownText");
    // NOTE: WEIGHTED MUST be the last option, or otherwise the WeightedMap creation method must change
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String dropDownText;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    Marriage(final String name, final String toolTipText, final String dropDownText) {
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

    public void marry(final Campaign campaign, final Person origin, final Person spouse) {
        final String surname = origin.getSurname();
        final String spouseSurname = spouse.getSurname();
        Marriage surnameStyle = this;

        if (surnameStyle == WEIGHTED) {
            WeightedIntMap<Marriage> map = createWeightedSurnameMap(campaign);
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
                MekHQ.getLogger().error(String.format("Marriage Surname Style is not defined, and cannot be used \"%s\" and \"%s\"",
                        origin.getFullName(), spouse.getFullName()));
                break;
        }

        // Now we set both Maiden Names, to avoid any divorce bugs (as the default is now an empty string)
        origin.setMaidenName(surname);
        spouse.setMaidenName(spouseSurname);

        origin.getGenealogy().setSpouse(spouse);
        spouse.getGenealogy().setSpouse(origin);

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


    private WeightedIntMap<Marriage> createWeightedSurnameMap(final Campaign campaign) {
        final WeightedIntMap<Marriage> map = new WeightedIntMap<>();
        final int[] weights = campaign.getCampaignOptions().getMarriageSurnameWeights();
        final Marriage[] styles = Marriage.values();
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
