/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.event;

import megamek.common.event.MMEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;

/**
 * Triggered when a force or unit is deployed to a scenario or undeployed from it.
 *
 */
public class DeploymentChangedEvent extends MMEvent {

    final private Unit unit;
    final private Force force;
    final private Scenario scenario;
    
    public DeploymentChangedEvent(Unit unit, Scenario scenario) {
        this.unit = unit;
        this.scenario = scenario;
        this.force = null;
    }
    
    public DeploymentChangedEvent(Force force, Scenario scenario) {
        this.force = force;
        this.scenario = scenario;
        this.unit = null;
    }
    
    public Unit getUnit() {
        return unit;
    }
    
    public Force getForce() {
        return force;
    }
    
    public Scenario getScenario() {
        return scenario;
    }
}
