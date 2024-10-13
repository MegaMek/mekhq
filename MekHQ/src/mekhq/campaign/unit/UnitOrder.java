/*
 * Unit.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.unit;

import java.io.PrintWriter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.*;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Availability;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;

/**
 * We use an extension of unit to create a unit order acquisition work
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitOrder extends Unit implements IAcquisitionWork {
    private static final MMLogger logger = MMLogger.create(UnitOrder.class);

    int quantity;
    int daysToWait;

    public UnitOrder() {
        super(null, null);
    }

    public UnitOrder(Entity en, Campaign c) {
        super(en, c);
        initializeParts(false);
        quantity = 1;
        daysToWait = 0;
    }

    @Override
    public boolean needsFixing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getDifficulty() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public TargetRoll getAllMods(Person admin) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String succeed() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String fail(int rating) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Person getTech() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAcquisitionName() {
        // This cannot be hyperlinked name due to the fact that we have a null unit ID
        // Also, the field this goes into does not currently support html, and would
        // need our listener attached
        // - Dylan
        return getName();
    }

    /**
     * @param quantity - the number of parts of this type
     * @return a string that gives a grammatical correct name based on the quantity
     */
    @Override
    public String getQuantityName(int quantity) {
        String answer = "" + quantity + " " + getName();
        if (quantity > 1) {
            answer += "s";
        }
        return answer;
    }

    @Override
    public Object getNewEquipment() {
        String name = getEntity().getShortNameRaw();
        MekSummary summary = MekSummaryCache.getInstance().getMek(name);
        if (null == summary) {
            logger.error("Could not find a mek summary for " + name);
            return null;
        }
        try {
            return new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
        } catch (EntityLoadingException e) {
            logger.error("Could not load " + summary.getEntryName());
            return null;
        }
    }

    @Override
    public String getAcquisitionDesc() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAcquisitionDisplayName() {
        return null;
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return null;
    }

    @Override
    public String getAcquisitionBonus() {
        return null;
    }

    @Override
    public Part getAcquisitionPart() {
        return null;
    }

    @Override
    public Unit getUnit() {
        return this;
    }

    @Override
    public int getDaysToWait() {
        return daysToWait;
    }

    @Override
    public void resetDaysToWait() {
        this.daysToWait = getCampaign().getCampaignOptions().getWaitingPeriod();
    }

    @Override
    public void decrementDaysToWait() {
        if (daysToWait > 0) {
            daysToWait--;
        }
    }

    @Override
    public String find(int transitDays) {
        // TODO: probably get a duplicate entity
        if (getCampaign().getQuartermaster().buyUnit((Entity) getNewEquipment(), transitDays)) {
            return "<font color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor()
                    + "'><b> unit found</b>.</font> It will be delivered in " + transitDays + " days.";
        } else {
            return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor()
                    + "'><b> You cannot afford this unit. Transaction cancelled</b>.</font>";
        }
    }

    @Override
    public String failToFind() {
        return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor()
                + "'><b> unit not found</b>.</font>";
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        if (!entity.isCanon()) {
            // TODO: custom job
        }
        if (entity.isClan() && getCampaign().getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(getCampaign().getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        } else if (getCampaign().getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(getCampaign().getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        // TODO: Fix weight classes
        // TODO: aero large craft
        // TODO: support vehicles
        if (entity instanceof Mek) {
            if (!((Mek) entity).isIndustrial()) {
                target.addModifier(0, "BattleMek");
            } else {
                target.addModifier(-1, "IndustrialMek");
            }
            switch (entity.getWeightClass()) {
                case EntityWeightClass.WEIGHT_LIGHT:
                    target.addModifier(-1, "Light");
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    target.addModifier(0, "Medium");
                    break;
                case EntityWeightClass.WEIGHT_HEAVY:
                    target.addModifier(1, "Heavy");
                    break;
                case EntityWeightClass.WEIGHT_ASSAULT:
                default:
                    target.addModifier(3, "Assault");
            }
        } else if (entity instanceof BattleArmor) {
            target.addModifier(0, "BattleArmor");
        } else if (entity instanceof Infantry) {
            if (entity.getMovementMode() == EntityMovementMode.INF_LEG) {
                target.addModifier(-3, "Foot Infantry");
            } else if (entity.getMovementMode() == EntityMovementMode.INF_JUMP) {
                target.addModifier(-1, "Jump Infantry");
            } else if (entity.getMovementMode() == EntityMovementMode.INF_MOTORIZED) {
                target.addModifier(-2, "Motorized Infantry");
            } else {
                target.addModifier(-1, "Mechanized Infantry");
            }
        } else if (entity instanceof Tank) {
            target.addModifier(-1, "Vehicle");
            switch (entity.getWeightClass()) {
                case EntityWeightClass.WEIGHT_LIGHT:
                    target.addModifier(-1, "Light");
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    target.addModifier(0, "Medium");
                    break;
                case EntityWeightClass.WEIGHT_HEAVY:
                    target.addModifier(1, "Heavy");
                    break;
                case EntityWeightClass.WEIGHT_ASSAULT:
                default:
                    target.addModifier(3, "Assault");
            }
        } else if (entity instanceof ConvFighter) {
            target.addModifier(+0, "Conventional Fighter");
        } else if (entity instanceof Aero) {
            target.addModifier(0, "Aerospace Fighter");
            switch (entity.getWeightClass()) {
                case EntityWeightClass.WEIGHT_LIGHT:
                    target.addModifier(-1, "Light");
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    target.addModifier(0, "Medium");
                    break;
                case EntityWeightClass.WEIGHT_HEAVY:
                    target.addModifier(1, "Heavy");
                    break;
                case EntityWeightClass.WEIGHT_ASSAULT:
                default:
                    target.addModifier(3, "Assault");
            }
        } else if (entity instanceof ProtoMek) {
            target.addModifier(+1, "ProtoMek");
        }
        // parts need to be initialized for this to work
        int avail = getAvailability();
        if (this.isExtinctIn(getCampaign().getGameYear())) {
            avail = EquipmentType.RATING_X;
        }
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ")");
        return target;
    }

    @Override
    public int getAvailability() {
        return calcYearAvailability(getCampaign().getGameYear(), getCampaign().useClanTechBase(),
                getCampaign().getTechFaction());
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public void incrementQuantity() {
        quantity++;
    }

    @Override
    public void decrementQuantity() {
        quantity--;
    }

    @Override
    public String getShoppingListReport(int quantity) {
        return getHyperlinkedName() + " added to procurement list.";
    }

    /**
     * Don't need as much info as unit to re-create
     */
    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "unitOrder");
        pw.println(MHQXMLUtility.writeEntityToXmlString(getEntity(), indent, getCampaign().getEntities()));
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "quantity", quantity);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysToWait", daysToWait);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "unitOrder");
    }

    public static UnitOrder generateInstanceFromXML(Node wn, Campaign c) {
        UnitOrder retVal = new UnitOrder();
        retVal.setCampaign(c);

        NodeList nl = wn.getChildNodes();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("quantity")) {
                    retVal.quantity = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                    retVal.daysToWait = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
                    retVal.entity = MHQXMLUtility.parseSingleEntityMul((Element) wn2, c);
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        retVal.initializeParts(false);

        return retVal;
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return isExtinct(year, clan, techFaction);
    }

    /**
     * @return TechConstants tech level
     */
    @Override
    public int getTechLevel() {
        return getSimpleTechLevel().getCompoundTechLevel(getCampaign().getFaction().isClan());
    }
}
