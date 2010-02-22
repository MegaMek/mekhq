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

import java.util.ArrayList;
import megamek.common.Engine;
import megamek.common.Mech;
import megamek.common.TechConstants;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Faction;
import mekhq.campaign.work.MekEngineReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekEngine extends Part {
    
    protected Engine engine;

    public Engine getEngine() {
        return engine;
    }
    
    public MekEngine(boolean salvage, int tonnage, int faction, Engine e) {
        super(salvage, tonnage);
        this.engine = e;
        this.name = engine.getEngineName() + " Engine" + " (" + getTonnage() + " tons)";
        this.engine = e;

        double c = getEngine().getBaseCost() * getEngine().getRating() * getTonnage() / 75.0;
        this.cost = (int) Math.round(c);

        // Increase cost for Clan parts when player is IS faction
        // Increase cost for Clan parts when player is IS faction
        if (isClanTechBase() && !Faction.isClanFaction(faction))
            this.cost *= CampaignOptions.clanPriceModifier;
    }

    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        if(task instanceof MekEngineReplacement && task.getUnit().getEntity() instanceof Mech) {
            Engine eng = task.getUnit().getEntity().getEngine();
            if(null != eng) {
                return getEngine().getEngineType() == eng.getEngineType()
                        && getEngine().getRating() == eng.getRating()
                        && getEngine().getTechType() == eng.getTechType()
                        && getTonnage() == task.getUnit().getEntity().getWeight();
            }
        }
        return false;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekEngine
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getEngine().getEngineType() == ((MekEngine) part).getEngine().getEngineType()
                && getEngine().getRating() == ((MekEngine) part).getEngine().getRating()
                && getEngine().getTechType() == ((MekEngine) part).getEngine().getTechType()
                && getTonnage() == ((MekEngine) part).getTonnage();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_ENGINE;
    }

    @Override
    public boolean isClanTechBase() {
        String techBase = TechConstants.getTechName(getEngine().getTechType());

        if (techBase.equals("Clan"))
            return true;
        else if (techBase.equals("Inner Sphere"))
            return false;
        else
            return false;
    }

    @Override
    public int getTech () {
        if (getEngine().getTechType() < 0 || getEngine().getTechType() >= TechConstants.SIZE)
            return TechConstants.T_IS_TW_NON_BOX;
        else
            return getEngine().getTechType();
    }

    @Override
    public ArrayList<String> getPotentialSSWNames(int faction) {
        ArrayList<String> sswNames = new ArrayList<String>();

        // The tech base matters for engines (ie. you can't use a IS XL engine to replace a Clan XL engine
        String techBase = (isClanTechBase() ? "(CL)" : "(IS)");

        String sswName = getName();

        sswNames.add(techBase + " " + sswName);
        sswNames.add(sswName);

        return sswNames;
    }

    @Override
    public String getDesc() {

        // "Clan" already included in super.getDesc()
        // return (getTechBase()==Part.TECH_BASE_CLAN ? "Clan " : "") + super.getDesc();
        
        return super.getDesc();
    }

    @Override
    public String getSaveString () {
        return getName() + ";" + getTonnage()
                + ";" + getEngine().getRating()
                + ";" + getEngine().getEngineType()
                + ";" + (getEngine().hasFlag(Engine.CLAN_ENGINE)?"true":"false")
                + ";" + (getEngine().hasFlag(Engine.TANK_ENGINE)?"true":"false")
                + ";" + (getEngine().hasFlag(Engine.LARGE_ENGINE)?"true":"false");
    }
    
}
