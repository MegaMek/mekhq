/*
 * RefitKit.java
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

import mekhq.campaign.work.Refit;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author natit
 */
public class RefitKit extends Part {
    protected String sourceName;
    protected String targetName;
    
    public RefitKit (boolean salvage, int tonnage, String sourceName, String targetName, int cost) {
        super(salvage, tonnage);
        this.sourceName = sourceName;
        this.targetName = targetName;
        this.name ="Refit Kit [" + sourceName + " -> " + targetName + "]";
        this.cost = cost;
    }
    
    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof RefitKit
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && this.sourceName.equals(((RefitKit) part).sourceName)
                && this.targetName.equals(((RefitKit) part).targetName);
    }

    @Override
    public int getPartType() {
        return PART_TYPE_OTHER;
    }

    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        if (task instanceof Refit) {
            return task.getUnit().getEntity().getShortName().equals(this.sourceName)
                    && ((Refit) task).getTargetEntity().getShortName().equals(this.targetName);
        } else {
            return false;
        }
    }
}
