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
package mekhq.campaign.stratCon;

import static mekhq.utilities.MHQInternationalization.getTextAt;

public enum StratConPlayType {
    DISABLED("DISABLED"),
    NORMAL("NORMAL"),
    MAPLESS("MAPLESS"),
    SINGLES("SINGLES");

    private final String lookupName;
    private final String label;
    private final String tooltip;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConPlayType";

    StratConPlayType(String lookupName) {
        this.lookupName = lookupName;
        this.label = generateLabel();
        this.tooltip = generateTooltip();
    }

    public String getLookupName() {
        return lookupName;
    }

    public String getLabel() {
        return label;
    }

    public String getTooltip() {
        return tooltip;
    }

    private String generateLabel() {
        return getTextAt(RESOURCE_BUNDLE, "StratConPlayType." + lookupName + ".label");
    }

    private String generateTooltip() {
        return getTextAt(RESOURCE_BUNDLE, "StratConPlayType." + lookupName + ".tooltip");
    }

    public static StratConPlayType fromLookupName(String lookupName) {
        for (StratConPlayType type : StratConPlayType.values()) {
            if (type.lookupName.equals(lookupName)) {
                return type;
            }
        }

        return DISABLED;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
