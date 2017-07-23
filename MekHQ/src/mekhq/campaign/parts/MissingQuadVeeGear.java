/**
 * 
 */
package mekhq.campaign.parts;

import org.w3c.dom.Node;

import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.QuadVee;
import mekhq.campaign.Campaign;

/**
 * Missing part for QuadVee conversion gear
 * 
 * @author Neoancient
 *
 */
public class MissingQuadVeeGear extends MissingPart {

    /**
     * 
     */
    private static final long serialVersionUID = -5968768537443435516L;

    public MissingQuadVeeGear(int tonnage, Campaign c) {
        super(tonnage, c);
    }

    @Override
    public int getBaseTime() {
        return 120;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR);
        }
    }

    @Override
    public int getLocation() {
        return QuadVee.LOC_NONE;
    }

    @Override
    public String checkFixable() {
        if(null == unit) {
            return null;
        }
        for(int i = 0; i < unit.getEntity().locations(); i++) {
            if (unit.getEntity().locationIsLeg(i)
                    && unit.isLocationDestroyed(i)) {
                return unit.getEntity().getLocationName(i) + " is destroyed.";
            }
        }
        return null;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof QuadVeeGear && part.getUnitTonnage() == unitTonnage;
    }

    @Override
    public Part getNewPart() {
        return new QuadVeeGear(unitTonnage, campaign);
    }

    @Override
    public double getTonnage() {
        return Math.ceil(unitTonnage / 10);
    }

    @Override
    public int getTechRating() {
        return EquipmentType.RATING_F;
    }

    @Override
    public int getAvailability(int era) {
        if (era == EquipmentType.ERA_DA) {
            return EquipmentType.RATING_F;
        }
        return EquipmentType.RATING_X;
    }

    @Override
    public int getIntroDate() {
        return 3125; // Officially ~3130
    }

    @Override
    public int getExtinctDate() {
        return EquipmentType.DATE_NONE;
    }

    @Override
    public int getReIntroDate() {
        return EquipmentType.DATE_NONE;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // nothing to load
    }

    @Override
    public String getAcquisitionName() {
        return getPartName() + ",  " + getTonnage() + " tons";
    }

    @Override
    public String getLocationName() {
        return null;
    }

}
