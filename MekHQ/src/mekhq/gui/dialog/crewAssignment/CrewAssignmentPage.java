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
package mekhq.gui.dialog.crewAssignment;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * The pages of the {@link CrewAssignmentDialog}.
 *
 * @author Illiani
 * @since 0.51.01
 */
enum CrewAssignmentPage {
    /** People on the left, units and their seats on the right: where every assignment is made. */
    ASSIGN("CrewAssignmentPage.ASSIGN"),
    /** The same layout for maintenance: the techs on the left, the units and who maintains them on the right. */
    TECHS("CrewAssignmentPage.TECHS"),
    REVIEW("CrewAssignmentPage.REVIEW");

    private final String labelKey;

    CrewAssignmentPage(String labelKey) {
        this.labelKey = labelKey;
    }

    String getLabel() {
        return getTextAt(CrewAssignmentDialog.RESOURCE_BUNDLE, labelKey);
    }

    /**
     * @return {@code true} for the pages that list people beside unit cards; {@code false} for the Review page
     *
     * @author Illiani
     * @since 0.51.01
     */
    boolean isWorkPage() {
        return this != REVIEW;
    }
}
