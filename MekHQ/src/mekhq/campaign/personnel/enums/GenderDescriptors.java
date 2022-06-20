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

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ResourceBundle;

/**
 * This is used to determine which gender descriptor to use based on the following specified format
 */
public enum GenderDescriptors {
    //region Enum Declarations
    /**
     * Descriptor: Male, Female, or Other
     */
    MALE_FEMALE_OTHER("GenderDescriptors.MALE.text", "GenderDescriptors.FEMALE.text", "GenderDescriptors.OTHER.text"),
    /**
     * Descriptor: He, She, or They
     */
    HE_SHE_THEY("GenderDescriptors.HE.text", "GenderDescriptors.SHE.text", "GenderDescriptors.THEY.text"),
    /**
     * Descriptor: Him, Her, or Them
     */
    HIM_HER_THEM("GenderDescriptors.HIM.text", "GenderDescriptors.HER.text", "GenderDescriptors.THEM.text"),
    /**
     * Descriptor: His, Her, or Their
     */
    HIS_HER_THEIR("GenderDescriptors.HIS.text", "GenderDescriptors.HER.text", "GenderDescriptors.THEIR.text"),
    /**
     * Descriptor: His, Hers, or Theirs
     */
    HIS_HERS_THEIRS("GenderDescriptors.HIS.text", "GenderDescriptors.HERS.text", "GenderDescriptors.THEIRS.text"),
    /**
     * Descriptor: Boy or Girl
     */
    BOY_GIRL("GenderDescriptors.BOY.text", "GenderDescriptors.GIRL.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String masculine;
    private final String feminine;
    private final String neutral;
    //endregion Variable Declarations

    //region Constructors
    GenderDescriptors(final String masculine, final String feminine) {
        this(masculine, feminine, null);
    }

    GenderDescriptors(final String masculine, final String feminine, final @Nullable String neutral) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        this.masculine = resources.getString(masculine);
        this.feminine = resources.getString(feminine);
        this.neutral = (neutral == null) ? "" : resources.getString(neutral);
    }
    //endregion Constructors

    //region Getters
    public String getMasculine() {
        return masculine;
    }

    public String getFeminine() {
        return feminine;
    }

    public String getNeutral() {
        return neutral;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isMaleFemaleOther() {
        return this == MALE_FEMALE_OTHER;
    }

    public boolean isHeSheThey() {
        return this == HE_SHE_THEY;
    }

    public boolean isHimHerThem() {
        return this == HIM_HER_THEM;
    }

    public boolean isHisHerTheir() {
        return this == HIS_HER_THEIR;
    }

    public boolean isHisHersTheirs() {
        return this == HIS_HERS_THEIRS;
    }

    public boolean isBoyGirl() {
        return this == BOY_GIRL;
    }
    //endregion Boolean Comparison Methods

    /**
     * @param gender the gender to return the descriptor for
     * @return the descriptor
     */
    public String getDescriptor(final Gender gender) {
        switch (gender) {
            case MALE:
                return getMasculine();
            case FEMALE:
                return getFeminine();
            case OTHER_MALE:
                return getNeutral().isBlank() ? getMasculine() : getNeutral();
            case OTHER_FEMALE:
                return getNeutral().isBlank() ? getFeminine() : getNeutral();
            default:
                return getNeutral();
        }
    }

    /**
     * This returns a descriptor with the first letter capitalized
     * @param gender the gender to return the descriptor for
     * @return the string with its first letter capitalized
     */
    public String getDescriptorCapitalized(final Gender gender) {
        final String descriptor = getDescriptor(gender).trim();
        return descriptor.isBlank() ? ""
                : descriptor.substring(0, 1).toUpperCase() + descriptor.substring(1);
    }
}
