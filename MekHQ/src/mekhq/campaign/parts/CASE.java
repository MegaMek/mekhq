package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.ITechnology;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;


/**
 * This is a ghost part to be used in refits only at this point. Do not attempt to use it as a real
 * part on a unit at this time.
 */
public class CASE extends Part {
    
    public final static int CASE_I = 1;
    public final static int CASE_II = 2;

    private int type;
    private Unit unit;
    private int location;

    /**
     * This is a ghost part to be used in refits only at this point. Do not attempt to use it as a
     * real part on a unit at this time.
     */
    public CASE() {
    }

    /**
     * This is a ghost part to be used in refits only at this point. Do not attempt to use it as a
     * real part on a unit at this time.
     */
    public CASE(int type, int location, Unit unit, Campaign campaign) {
        super(0, false, campaign);
        if (!((type == CASE_I) || (type == CASE_II))) {
            throw new IllegalArgumentException("Invalid CASE type");
        }
        this.type = type;
        this.location = location;
        this.unit = unit;
        if (type == CASE_I) {
            name = "CASE";
        } else {
            name = "CASE II";
        }
    }

    /**
     * Get the CASE for a location on a unit
     * @param location - location on unit to check for CASE
     * @param unit - unit to check for CASE
     * @param campaign - campaign to attach the CASE Part to
     * @return an instance of CASE
     */
    public static CASE getCaseFor(int location, Unit unit, Campaign campaign) {
        if (unit.getEntity().locationHasCase(location)) {
            return new CASE(CASE_I, location, unit, campaign);
        } else if (unit.getEntity().hasCASEII(location)) {
            return new CASE(CASE_II, location, unit, campaign);
        } else {
            return null;
        }
    }

    /**
     * Does nothing.
     */
    @Override
    public int getBaseTime() {
        return 0;
    }

    /**
     * Does nothing.
     */
    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
    }

    /**
     * Does nothing.
     */
    @Override
    public void updateConditionFromPart() {
    }

    /**
     * Does nothing.
     */
    @Override
    public void remove(boolean salvage) {
    }

    /**
     * There is no missing part.
     */
    @Override
    public MissingPart getMissingPart() {
        return null;
    }

    /**
     * @return the location this is mounted at.
     */
    @Override
    public int getLocation() {
        return location;
    }

    /**
     * This does nothing
     */
    @Override
    public String checkFixable() {
        return "This is a fake item you should never see";
    }

    /**
     * This does nothing
     */
    @Override
    public Money getStickerPrice() {
        return Money.of(0);
    }

    /**
     * This does nothing
     */
    @Override
    public boolean needsFixing() {
        return false;
    }

    /**
     * This does nothing
     */
    @Override
    public int getDifficulty() {
        return 0;
    }

    /**
     * This does nothing
     */
    @Override
    public double getTonnage() {
        return 0;
    }

    /**
     * Checks for part equality
     */
    @Override
    public boolean isSamePartType(Part part) {
        if (part instanceof CASE) {
            if (((CASE) part).type == this.type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("type")) {
                type = Integer.parseInt(wn2.getTextContent());
                if (type == CASE_I) {
                    this.name = "CASE";
                }
                if (type == CASE_II) {
                    this.name = "CASE II";
                }
            }
        }
    }

    /**
     * Makes a new one of these attached to nothing
     */
    @Override
    public Part clone() {
        return new CASE(type, -1, null, campaign);
    }

    /**
     * Get the name of our location
     */
    @Override
    public String getLocationName() {
        if (null != unit) {
            return unit.getEntity().getLocationName(location);
        }
        return null;
    }

    /**
     * Does nothing
     */
    @Override
    public ITechnology getTechAdvancement() {
        return null;
    }
    
}
