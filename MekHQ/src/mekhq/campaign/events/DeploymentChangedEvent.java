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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.campaign.events;

import mekhq.campaign.events.scenarios.ScenarioChangedEvent;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;

/**
 * Triggered when a force or unit is deployed to a scenario or un-deployed from it.
 */
public class DeploymentChangedEvent extends ScenarioChangedEvent {

    private final Unit unit;
    private final Formation formation;

    public DeploymentChangedEvent(Unit unit, Scenario scenario) {
        super(scenario);
        this.unit = unit;
        this.formation = null;
    }

    public DeploymentChangedEvent(Formation formation, Scenario scenario) {
        super(scenario);
        this.formation = formation;
        this.unit = null;
    }

    public Unit getUnit() {
        return unit;
    }

    public Formation getForce() {
        return formation;
    }
}
