/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.campaign.event;

import megamek.common.event.MMEvent;
import mekhq.campaign.force.Force;

/**
 * MekHQ event relating to the deployment of a force to a StratCon track.
 * @author NickAragua
 */
public class StratconDeploymentEvent extends MMEvent {
    private final Force force;
    
    public StratconDeploymentEvent(Force force) {
        super();
        
        this.force = force;
    }
    
    public Force getForce() {
        return force;
    }
}
