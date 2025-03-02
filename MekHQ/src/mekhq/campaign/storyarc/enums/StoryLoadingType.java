/*
 * Copyright (C) 2021-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.campaign.storyarc.enums;

/**
 * This enum indicates whether a story arc has to start a new campaign, can be added to an existing
 * campaign or both.
 */
public enum StoryLoadingType {
    //region Enum Declarations
    START_NEW,
    LOAD_EXISTING,
    BOTH;
    //endregion Enum Declarations

    public boolean canStartNew() {
        switch (this) {
            case START_NEW:
            case BOTH:
                return true;
            default:
                return false;
        }
    }

    public boolean canLoadExisting() {
        switch (this) {
            case LOAD_EXISTING:
            case BOTH:
                return true;
            default:
                return false;
        }
    }



}
