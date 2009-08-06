/*
 * MekEngine.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign.parts;

import megamek.common.Engine;
import megamek.common.Mech;
import mekhq.campaign.work.MekEngineReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekEngine extends Part {
    
    protected Engine engine;
    
    public MekEngine(boolean salvage, Engine e) {
        super(salvage);
        this.engine = e;
        this.name = engine.getEngineName() + " Engine";
        this.engine = e;
    }

    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        if(task instanceof MekEngineReplacement && task.getUnit().getEntity() instanceof Mech) {
            Engine eng = task.getUnit().getEntity().getEngine();
            if(null != eng) {
                return engine.getEngineType() == eng.getEngineType() && engine.getRating() == eng.getRating();
            }
        }
        return false;
    }

}
