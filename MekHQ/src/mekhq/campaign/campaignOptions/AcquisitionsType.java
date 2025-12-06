/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.campaignOptions;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * Represents the different acquisition resolution methods available in MekHQ's campaign options. Acquisition type
 * affects how procurement attempts are processed, such as whether they rely on negotiation, or the administration
 * skill, any technical skill, or use no skill.
 *
 * @author Illiani
 * @since 0.50.10
 */
public enum AcquisitionsType {
    ADMINISTRATION("ADMINISTRATION"),
    /** Acquisitions that may be handled by any technical skill or support. */
    ANY_TECH("ANY_TECH"),
    /** Acquisitions that succeed automatically without rolls or checks. */
    AUTOMATIC("AUTOMATIC"),
    NEGOTIATION("NEGOTIATION");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AcquisitionsType";

    private final String lookupName;
    private final String label;

    /**
     * Creates a new {@link AcquisitionsType} instance and initializes its localized label.
     *
     * @param lookupName the lookup key used to resolve the localized label
     *
     * @author Illiani
     * @since 0.50.10
     */
    AcquisitionsType(String lookupName) {
        this.lookupName = lookupName;
        this.label = generateLabel();
    }

    public String getLookupName() {
        return lookupName;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Generates the localized label for this acquisition type by querying the resource bundle entry.
     *
     * @return the resolved label string
     *
     * @author Illiani
     * @since 0.50.10
     */
    private String generateLabel() {
        return getTextAt(RESOURCE_BUNDLE, "AcquisitionsType." + lookupName + ".label");
    }

    /**
     * Attempts to resolve an {@link AcquisitionsType} from its lookup name.
     *
     * <p>If no matching acquisition type is found, this method defaults to returning {@link #ANY_TECH} (the default
     * value under Campaign Operations).</p>
     *
     * @param lookupName the lookup key to match
     *
     * @return the corresponding {@link AcquisitionsType}, or {@link #ANY_TECH} if no match exists
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static AcquisitionsType parseFromLookupName(String lookupName) {
        for (AcquisitionsType type : AcquisitionsType.values()) {
            if (type.lookupName.equals(lookupName)) {
                return type;
            }
        }
        return ANY_TECH;
    }
}
