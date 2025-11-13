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
package mekhq.gui.dialog.markets.personnelMarket;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * Defines the columns for the personnel market applicant table in the GUI.
 *
 * <p>Each column corresponds to a visible property for an applicant (e.g., full name, profession). Provides
 * localization for column headers.</p>
 *
 * <p>Each constant is associated with a column index for use in table models.</p>
 *
 * <p>The localized label for each column is retrieved from a resource bundle.</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public enum ApplicantTableColumns {
    /**
     * The applicant's full name
     */
    FULL_NAME(0),
    /**
     * The applicant's primary role
     */
    PROFESSION(1),
    /**
     * The applicant's experience level
     */
    EXPERIENCE(2),
    /**
     * The applicant's age
     */
    AGE(3),
    /**
     * The applicant's gender
     */
    GENDER(4),
    /**
     * How many positive SPAs the character has
     */
    POSITIVE_ABILITIES(5),
    /**
     * How many negative SPAs (Flaws) the character has
     */
    NEGATIVE_ABILITIES(6),
    /**
     * How highly the character scored on their performance exam (a measure of how high their Talent score it)
     */
    PERFORMANCE_EXAM(6),
    /**
     * The cost to hire the applicant
     */
    HIRING_COST(7);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ApplicantTableColumns";

    public final int columnIndex;

    /**
     * Constructs a new ApplicantTableColumns value with the supplied column index.
     *
     * @param columnIndex the integer index for the column
     *
     * @author Illiani
     * @since 0.50.06
     */
    ApplicantTableColumns(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    /**
     * Returns the localized display label for this column using the resource bundle.
     *
     * @return localized column header label
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, name() + ".label");
    }
}
