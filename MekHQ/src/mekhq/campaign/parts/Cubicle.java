/*
 * Copyright (c) 2017 - The MegaMek Team. All rights reserved.
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
package mekhq.campaign.parts;

import megamek.common.BayType;
import megamek.common.Entity;
import megamek.common.ITechnology;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * A transport bay cubicle for a Mek, ProtoMek, vehicle, fighter, or small
 * craft.
 *
 * @author Neoancient
 */
public class Cubicle extends Part {
    private static final MMLogger logger = MMLogger.create(Cubicle.class);

    private BayType bayType;

    public Cubicle() {
        this(0, null, null);
    }

    public Cubicle(int tonnage, BayType bayType, Campaign c) {
        super(tonnage, false, c);
        this.bayType = bayType;
        if (null != bayType) {
            name = bayType.getDisplayName() + " Cubicle";
        }
    }

    public BayType getBayType() {
        return bayType;
    }

    @Override
    public String getName() {
        if (null != parentPart) {
            return parentPart.getName() + " Cubicle";
        }
        return super.getName();
    }

    @Override
    public int getBaseTime() {
        // replacement time 1 week
        return 3360;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        // This is handled by the transport bay part to coordinate all the cubicles
    }

    @Override
    public void updateConditionFromPart() {
        // This is handled by the transport bay part to coordinate all the cubicles
    }

    @Override
    public void remove(boolean salvage) {
        // Grab a reference to our parent part so that we don't accidentally NRE
        // when we remove the parent part reference.
        Part parentPart = getParentPart();
        if (null != parentPart) {
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.incrementQuantity();
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0);
            parentPart.removeChildPart(this);
            parentPart.addChildPart(missing);
            parentPart.updateConditionFromPart();
        }
        setUnit(null);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingCubicle(getUnitTonnage(), bayType, campaign);
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        // Per replacement repair tables in SO, cubicles are replaced rather than
        // repaired.
        return false;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(bayType.getCost());
    }

    @Override
    public double getTonnage() {
        return bayType.getWeight();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof Cubicle)
                && (((Cubicle) part).getBayType() == bayType);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bayType", bayType.toString());
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("bayType")) {
                // <50.01 compatibility handler
                String bayRawValue = wn2.getTextContent();

                if (Objects.equals(bayRawValue, "MECH")) {
                    bayRawValue = "MEK";
                }

                if (Objects.equals(bayRawValue, "PROTOMECH")) {
                    bayRawValue = "PROTOMEK";
                }

                bayType = BayType.parse(bayRawValue);
                if (null == bayType) {
                    logger.error(String.format("Could not parse bay type %s treating as BayType.Mek",
                        wn2.getTextContent()));
                    bayType = BayType.MEK;
                }
                name = bayType.getDisplayName() + " Cubicle";
            }
        }
    }

    @Override
    public Part clone() {
        Part part = new Cubicle(getUnitTonnage(), bayType, campaign);
        copyBaseData(part);
        return part;
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public ITechnology getTechAdvancement() {
        return bayType;
    }
}
