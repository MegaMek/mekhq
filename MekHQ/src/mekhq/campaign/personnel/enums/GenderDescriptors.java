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

import megamek.common.Crew;
import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

/**
 * This is used to determine which gender descriptor to use based on the following specified format
 */
public enum GenderDescriptors {
    //region Enum Declarations
    /**
     * Descriptor: Male, Female, or Other
     */
    MALE_FEMALE("GenderDescriptors.MALE.text", "GenderDescriptors.FEMALE.text", "GenderDescriptors.OTHER.text"),
    /**
     * Descriptor: He, She, or They
     */
    HE_SHE("GenderDescriptors.HE.text", "GenderDescriptors.SHE.text", "GenderDescriptors.THEY.text"),
    /**
     * Descriptor: Him, Her, or Them
     */
    HIM_HER("GenderDescriptors.HIM.text", "GenderDescriptors.HER.text", "GenderDescriptors.THEM.text"),
    /**
     * Descriptor: His, Her, or Their
     */
    HIS_HER("GenderDescriptors.HIS.text", "GenderDescriptors.HER.text", "GenderDescriptors.THEIR.text"),
    /**
     * Descriptor: His, Hers, or Theirs
     */
    HIS_HERS("GenderDescriptors.HIS.text", "GenderDescriptors.HERS.text", "GenderDescriptors.THEIRS.text"),
    /**
     * Descriptor: Boy or Girl
     */
    BOY_GIRL("GenderDescriptors.BOY.text", "GenderDescriptors.GIRL.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String masculine;
    private final String feminine;
    private final String other;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    GenderDescriptors(String masculine, String feminine) {
        this(masculine, feminine, null);
    }

    GenderDescriptors(String masculine, String feminine, String other) {
        this.masculine = resources.getString(masculine);
        this.feminine = resources.getString(feminine);
        this.other = (other != null) ? resources.getString(other): "";
    }
    //endregion Constructors

    /**
     * @param gender the gender to return the descriptor for
     * @return the descriptor
     */
    public String getDescriptor(Gender gender) {
        switch (gender) {
            case MALE:
                return this.masculine;
            case FEMALE:
                return this.feminine;
            case OTHER_MALE:
                return (this.other != null) ? this.other : this.masculine;
            case OTHER_FEMALE:
                return (this.other != null) ? this.other : this.feminine;
            default:
                return this.other;
        }
    }

    /**
     * This returns a descriptor with the first letter capitalized
     * @param gender the gender to return the descriptor for
     * @return the string with its first letter capitalized
     */
    public String getDescriptorCapitalized(Gender gender) {
        String descriptor = getDescriptor(gender).trim();
        switch (descriptor.length()) {
            case 0:
                return descriptor;
            case 1:
                return descriptor.toUpperCase();
            default:
                return descriptor.substring(0, 1).toUpperCase() + descriptor.substring(1);
        }
    }
}
