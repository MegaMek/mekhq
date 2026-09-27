/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.roleplay;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * What kind of record a {@link JournalEntry} is: a note the player wrote, or one of the kinds of automatic Oracle log
 * record.
 */
public enum JournalEntryType {
    NOTE("NOTE"),
    FATE_CHART("FATE_CHART"),
    RANDOM_EVENT("RANDOM_EVENT"),
    CONCEPTS("CONCEPTS"),
    THREAD("THREAD");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final String label;

    JournalEntryType(String lookupName) {
        this.label = getTextAt(RESOURCE_BUNDLE, "JournalEntryType." + lookupName + ".label");
    }

    /**
     * @return the localized display label for this type
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
