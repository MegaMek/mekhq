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

import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;

import megamek.codeUtilities.StringUtility;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;

public enum MergingSurnameStyle {
    // region Enum Declarations
    NO_CHANGE("MergingSurnameStyle.NO_CHANGE.text", "MergingSurnameStyle.NO_CHANGE.toolTipText",
            "MergingSurnameStyle.NO_CHANGE.dropDownText"),
    YOURS("MergingSurnameStyle.YOURS.text", "MergingSurnameStyle.YOURS.toolTipText",
            "MergingSurnameStyle.YOURS.dropDownText"),
    SPOUSE("MergingSurnameStyle.SPOUSE.text", "MergingSurnameStyle.SPOUSE.toolTipText",
            "MergingSurnameStyle.SPOUSE.dropDownText"),

    SPACE_YOURS("MergingSurnameStyle.SPACE_YOURS.text", "MergingSurnameStyle.SPACE_YOURS.toolTipText",
            "MergingSurnameStyle.SPACE_YOURS.dropDownText"),
    BOTH_SPACE_YOURS("MergingSurnameStyle.BOTH_SPACE_YOURS.text", "MergingSurnameStyle.BOTH_SPACE_YOURS.toolTipText",
            "MergingSurnameStyle.BOTH_SPACE_YOURS.dropDownText"),
    HYPHEN_YOURS("MergingSurnameStyle.HYPHEN_YOURS.text", "MergingSurnameStyle.HYPHEN_YOURS.toolTipText",
            "MergingSurnameStyle.HYPHEN_YOURS.dropDownText"),
    BOTH_HYPHEN_YOURS("MergingSurnameStyle.BOTH_HYPHEN_YOURS.text", "MergingSurnameStyle.BOTH_HYPHEN_YOURS.toolTipText",
            "MergingSurnameStyle.BOTH_HYPHEN_YOURS.dropDownText"),

    SPACE_SPOUSE("MergingSurnameStyle.SPACE_SPOUSE.text", "MergingSurnameStyle.SPACE_SPOUSE.toolTipText",
            "MergingSurnameStyle.SPACE_SPOUSE.dropDownText"),
    BOTH_SPACE_SPOUSE("MergingSurnameStyle.BOTH_SPACE_SPOUSE.text", "MergingSurnameStyle.BOTH_SPACE_SPOUSE.toolTipText",
            "MergingSurnameStyle.BOTH_SPACE_SPOUSE.dropDownText"),
    HYPHEN_SPOUSE("MergingSurnameStyle.HYPHEN_SPOUSE.text", "MergingSurnameStyle.HYPHEN_SPOUSE.toolTipText",
            "MergingSurnameStyle.HYPHEN_SPOUSE.dropDownText"),
    BOTH_HYPHEN_SPOUSE("MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.text",
            "MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.toolTipText",
            "MergingSurnameStyle.BOTH_HYPHEN_SPOUSE.dropDownText"),

    MALE("MergingSurnameStyle.MALE.text", "MergingSurnameStyle.MALE.toolTipText",
            "MergingSurnameStyle.MALE.dropDownText"),
    FEMALE("MergingSurnameStyle.FEMALE.text", "MergingSurnameStyle.FEMALE.toolTipText",
            "MergingSurnameStyle.FEMALE.dropDownText"),
    WEIGHTED("MergingSurnameStyle.WEIGHTED.text", "MergingSurnameStyle.WEIGHTED.toolTipText",
            "MergingSurnameStyle.WEIGHTED.dropDownText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String dropDownText;
    // endregion Variable Declarations

    // region Constructors
    MergingSurnameStyle(final String name, final String toolTipText, final String dropDownText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.dropDownText = resources.getString(dropDownText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public String getDropDownText() {
        return dropDownText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
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

    public boolean isHyphenYours() {
        return this == HYPHEN_YOURS;
    }

    public boolean isBothHyphenYours() {
        return this == BOTH_HYPHEN_YOURS;
    }

    public boolean isSpaceSpouse() {
        return this == SPACE_SPOUSE;
    }

    public boolean isBothSpaceSpouse() {
        return this == BOTH_SPACE_SPOUSE;
    }

    public boolean isHyphenSpouse() {
        return this == HYPHEN_SPOUSE;
    }

    public boolean isBothHyphenSpouse() {
        return this == BOTH_HYPHEN_SPOUSE;
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
    // endregion Boolean Comparison Methods

    /**
     * This applies the surname changes that occur during a marriage
     *
     * @param campaign the campaign to use in processing
     * @param today    the current day
     * @param origin   the origin person
     * @param spouse   the origin person's new spouse
     */
    public void apply(final Campaign campaign, final LocalDate today, final Person origin,
            final Person spouse) {
        final String surname = origin.getSurname();
        final String spouseSurname = spouse.getSurname();
        final MergingSurnameStyle surnameStyle = isWeighted()
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
                if (!StringUtility.isNullOrBlank(surname) && !StringUtility.isNullOrBlank(spouseSurname)) {
                    spouse.setSurname(spouseSurname + ' ' + surname);
                } else {
                    spouse.setSurname(surname);
                }
                break;
            case BOTH_SPACE_YOURS:
                if (!StringUtility.isNullOrBlank(surname) && !StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(spouseSurname + ' ' + surname);
                    spouse.setSurname(spouseSurname + ' ' + surname);
                } else if (!StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtility.isNullOrBlank(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case HYPHEN_YOURS:
                if (!StringUtility.isNullOrBlank(surname) && !StringUtility.isNullOrBlank(spouseSurname)) {
                    spouse.setSurname(spouseSurname + '-' + surname);
                } else {
                    spouse.setSurname(surname);
                }
                break;
            case BOTH_HYPHEN_YOURS:
                if (!StringUtility.isNullOrBlank(surname) && !StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(spouseSurname + '-' + surname);
                    spouse.setSurname(spouseSurname + '-' + surname);
                } else if (!StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtility.isNullOrBlank(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case SPACE_SPOUSE:
                if (!StringUtility.isNullOrBlank(surname) && !StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(surname + ' ' + spouseSurname);
                } else {
                    origin.setSurname(spouseSurname);
                }
                break;
            case BOTH_SPACE_SPOUSE:
                if (!StringUtility.isNullOrBlank(surname) && !StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(surname + ' ' + spouseSurname);
                    spouse.setSurname(surname + ' ' + spouseSurname);
                } else if (!StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtility.isNullOrBlank(surname)) {
                    spouse.setSurname(surname);
                }
                break;
            case HYPHEN_SPOUSE:
                if (!StringUtility.isNullOrBlank(surname) && !StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(surname + '-' + spouseSurname);
                } else {
                    origin.setSurname(spouseSurname);
                }
                break;
            case BOTH_HYPHEN_SPOUSE:
                if (!StringUtility.isNullOrBlank(surname) && !StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(surname + '-' + spouseSurname);
                    spouse.setSurname(surname + '-' + spouseSurname);
                } else if (!StringUtility.isNullOrBlank(spouseSurname)) {
                    origin.setSurname(spouseSurname);
                } else if (!StringUtility.isNullOrBlank(surname)) {
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
                MMLogger.create(MergingSurnameStyle.class)
                        .error(String.format(
                                "Merging Surname Style %s is not defined, and cannot be used for \"%s\" and \"%s\"",
                                surnameStyle.name(), origin.getFullName(), spouse.getFullName()));
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

    /**
     * @param weights the weights to use in creating the weighted surname map
     * @return the created weighted surname map
     */
    WeightedIntMap<MergingSurnameStyle> createWeightedSurnameMap(
            final Map<MergingSurnameStyle, Integer> weights) {
        final WeightedIntMap<MergingSurnameStyle> map = new WeightedIntMap<>();
        for (final MergingSurnameStyle style : MergingSurnameStyle.values()) {
            if (style.isWeighted()) {
                continue;
            }
            map.add(weights.get(style), style);
        }
        return map;
    }

    // region File I/O
    /**
     * @param text containing the MergingSurnameStyle
     * @return the saved MergingSurnameStyle
     */
    public static MergingSurnameStyle parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Migration Occurred in 0.49.9
        switch (text) {
            case "HYP_YOURS":
                return HYPHEN_YOURS;
            case "BOTH_HYP_YOURS":
                return BOTH_HYPHEN_YOURS;
            case "HYP_SPOUSE":
                return HYPHEN_SPOUSE;
            case "BOTH_HYP_SPOUSE":
                return BOTH_HYPHEN_SPOUSE;
            default:
                break;
        }

        MMLogger.create(MergingSurnameStyle.class)
                .error("Unable to parse " + text + " into a MergingSurnameStyle. Returning FEMALE.");
        return FEMALE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
