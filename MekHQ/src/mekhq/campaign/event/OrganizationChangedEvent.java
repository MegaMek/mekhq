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

import megamek.common.event.MMEvent;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;

/**
 * Triggered when the TO&amp;E structure changes by adding, removing, or moving a force or adding or removing
 * a unit.
 */
public class OrganizationChangedEvent extends MMEvent {

    private final Force force;
    private final Unit unit;

    /**
     * This version also populates formation levels
     */
    public OrganizationChangedEvent(Campaign campaign, Force force) {
        this.force = force;
        this.unit = null;

        Force.populateFormationLevelsFromOrigin(campaign);
    }

    public OrganizationChangedEvent(Force force) {
        this.force = force;
        this.unit = null;
    }

    public OrganizationChangedEvent(Unit unit) {
        this.force = null;
        this.unit = unit;
    }

    /**
     * This version also populates formation levels
     */
    public OrganizationChangedEvent(Campaign campaign, Force force, Unit unit) {
        this.force = force;
        this.unit = unit;

        Force.populateFormationLevelsFromOrigin(campaign);
    }

    public Force getForce() {
        return force;
    }

    public Unit getUnit() {
        return unit;
    }
}
