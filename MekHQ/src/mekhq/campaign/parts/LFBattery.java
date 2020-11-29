/*
 * LFBattery.java
 *
 * Copyright (c) 2019, The MegaMek Team
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

import java.io.PrintWriter;
import java.util.StringJoiner;

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.Jumpship;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author MKerensky
 */
public class LFBattery extends Part {

    /**
     *
     */
    private static final long serialVersionUID = 6590685996383689912L;

    //Not specified in IO - use SO p158
    public static final TechAdvancement TA_LF_BATTERY = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2519, 2529, 2600).setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH).setTechRating(RATING_D)
            .setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    //Standard, primitive, compact, subcompact...
    private int coreType;

    public int getCoreType() {
        return coreType;
    }

    //How many docking collars does this drive support?
    private int docks;

    public int getDocks() {
        return docks;
    }

    public LFBattery() {
        this(0, Jumpship.DRIVE_CORE_STANDARD, 0, null);
    }

    public LFBattery(int tonnage, int coreType, int docks, Campaign c) {
        super(tonnage, c);
        this.coreType = coreType;
        this.docks = docks;
        this.name = "L-F Battery";
    }

    public LFBattery clone() {
        LFBattery clone = new LFBattery(0, coreType, docks, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if(null != unit) {
            if (unit.getEntity() instanceof Jumpship) {
                if(((Jumpship)unit.getEntity()).getLFBatteryHit()) {
                    hits = 1;
                } else {
                    hits = 0;
                }
            }
            if(checkForDestruction
                    && hits > priorHits
                    && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        int time;
        if(isSalvaging()) {
            //SO KF Drive times, p184-5
            time = 28800;
        } else {
            time = 4800;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        //SO Difficulty Mods
        if(isSalvaging()) {
            return 2;
        }
        return 5;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit && unit.getEntity() instanceof Jumpship) {
                ((Jumpship)unit.getEntity()).setLFBatteryHit(needsFixing());
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            Jumpship js = ((Jumpship)unit.getEntity());
            js.setLFBatteryHit(false);
            //Also repair your KF Drive integrity - +1 point if you have other components to fix
            //Otherwise, fix it all.
            if (js.isKFDriveDamaged()) {
                js.setKFIntegrity(Math.min((js.getKFIntegrity() + 1), js.getOKFIntegrity()));
            } else {
                js.setKFIntegrity(js.getOKFIntegrity());
            }
        }
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            if (unit.getEntity() instanceof Jumpship) {
                Jumpship js = ((Jumpship)unit.getEntity());
                js.setKFIntegrity(Math.max(0, js.getKFIntegrity() - 1));
                js.setLFBatteryHit(true);
            }
            //All the BT lore says you can't jump while carrying around another KF Drive, therefore
            //you can't salvage and keep this in the warehouse, just remove/scrap and replace it
            //See SO p130 for reference
            campaign.getWarehouse().removePart(this);
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingLFBattery(getUnitTonnage(), coreType, docks, campaign);
    }

    @Override
    public String checkFixable() {
        if (isSalvaging()) {
            // Can't salvage this part of the K-F Drive.
            return "You cannot salvage an L-F Battery. You must scrap it instead.";
        }
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public Money getStickerPrice() {
        //No cost per SO p158 - multiplies other components instead
        return Money.zero();
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof LFBattery
                && coreType == ((LFBattery)part).getCoreType()
                && docks == ((LFBattery)part).getDocks();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<coreType>"
                +coreType
                +"</coreType>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<docks>"
                +docks
                +"</docks>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("coreType")) {
                coreType = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("docks")) {
                docks = Integer.parseInt(wn2.getTextContent());
            }
        }
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringJoiner joiner = new StringJoiner(", ");
        String details = super.getDetails(includeRepairDetails);
        if (!details.isEmpty()) {
            joiner.add(details);
        }
        joiner.add(getUnitTonnage() + " tons")
              .add(getDocks() + " collars");
        return joiner.toString();
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_VESSEL);
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public int getLocation() {
        return Jumpship.LOC_HULL;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_LF_BATTERY;
    }
}
