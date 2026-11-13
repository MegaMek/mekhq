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
package mekhq.campaign.personnel.procreation;

/**
 * Describes how a child who was not born into the campaign comes to join a parent's family.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum ChildArrivalType {
    /** A biological child the parent already had, added by the GM. */
    BIOLOGICAL("childArrived"),
    /** A biological child the parent did not know about until now, found through random paternity. */
    PATERNITY("childDiscovered"),
    /** A child the parent has adopted. */
    ADOPTION("childAdopted");

    private final String reportKey;

    ChildArrivalType(final String reportKey) {
        this.reportKey = reportKey;
    }

    /**
     * @return {@code true} if the child is biologically related to their parents, and so inherits from them
     */
    public boolean isBiological() {
        return this != ADOPTION;
    }

    /**
     * @return the stem of the resource keys used to report and log the child's arrival
     */
    public String getReportKey() {
        return reportKey;
    }
}
