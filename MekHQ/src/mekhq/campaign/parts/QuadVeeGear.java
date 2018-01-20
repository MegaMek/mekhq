/**
 *
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.QuadVee;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 * Conversion gear for QuadVees
 *
 * @author Neoancient
 *
 */
public class QuadVeeGear extends Part {

    /**
     *
     */
    private static final long serialVersionUID = -382649905317675957L;

    static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TECH_BASE_CLAN)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
            .setClanAdvancement(3130, 3135, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(true).setPrototypeFactions(F_CHH)
            .setProductionFactions(F_CHH)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);


    public QuadVeeGear() {
        this(0, null);
    }

    public QuadVeeGear(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Conversion Gear";
    }

    public QuadVeeGear clone() {
        QuadVeeGear clone = new QuadVeeGear(0, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {
            hits = unit.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                        QuadVee.SYSTEM_CONVERSION_GEAR);
        }
    }

    @Override
    public int getBaseTime() {
        // Using value for 'Mech "weapons and other equipment"
        if(isSalvaging()) {
            return 120;
        }
        if(hits == 1) {
            return 100;
        } else if(hits == 2) {
            return 150;
        } else if(hits == 3) {
            return 200;
        } else if(hits > 3) {
            return 250;
        }
        return 0;
    }

    @Override
    public int getDifficulty() {
        if(isSalvaging()) {
            return 0;
        }
        if(hits == 1) {
            return -3;
        } else if(hits == 2) {
            return -2;
        } else if(hits == 3) {
            return 0;
        } else if(hits > 3) {
            return 2;
        }
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR);
            } else {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR, hits);
            }
        }
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            unit.damageSystem(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR, 4);
            Part spare = campaign.checkForExistingSparePart(this);
            if(!salvage) {
                campaign.removePart(this);
            } else if(null != spare) {
                spare.incrementQuantity();
                campaign.removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.addPart(missing, 0);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingQuadVeeGear(getUnitTonnage(), campaign);
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public String checkFixable() {
        if(null == unit) {
            return null;
        }
        if(isSalvaging()) {
            return null;
        }
        for (int i = 0; i < unit.getEntity().locations(); i++) {
            if (unit.getEntity().locationIsLeg(i)) {
                if (unit.isLocationBreached(i)) {
                    return unit.getEntity().getLocationName(i) + " is breached.";
                }
                if (unit.isLocationDestroyed(i)) {
                    return unit.getEntity().getLocationName(i) + " is destroyed.";
                }
            }
        }
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public long getStickerPrice() {
        /*
         * The cost for conversion equipment is calculated as 10% of the total cost of weapons/equipment
         * and structure. This is unworkable for the conversion gear sticker price, since this
         * would make the cost of the conversion gear in OmniQuadVees vary with the configuration.
         * We will use a general 10,000 * part tonnage and assume the remainder is part of the
         * turret mechanism that is only destroyed if the center torso is destroyed.
         */
        return (long)Math.ceil(getTonnage() * 10000);
    }

    @Override
    public double getTonnage() {
        return Math.ceil(unitTonnage * 0.1);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof QuadVeeGear && part.unitTonnage == unitTonnage;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // nothing to load
    }

    @Override
    public String getLocationName() {
        return null;
    }

}
