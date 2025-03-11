/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.event;

import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;

import megamek.common.annotations.Nullable;

/**
 * Triggered when a part is assigned to a tech for repair. If getTech() == null, the tech has
 * been removed from the assignment.
 *
 */
public class PartAssignmentEvent extends PartChangedEvent {

    private final Person tech;

    public PartAssignmentEvent(Part part, @Nullable Person tech) {
        super(part);
        this.tech = tech;
    }

    public @Nullable Person getTech() {
        return tech;
    }

}
