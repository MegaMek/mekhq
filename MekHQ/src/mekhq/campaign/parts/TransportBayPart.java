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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Bay;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 * @author Neoancient
 *
 */
public class TransportBayPart extends Part {
    
    /**
     * 
     */
    private static final long serialVersionUID = 6555762303379877899L;
    
    private int bayNumber;
    private double size;
    
    public TransportBayPart() {
        this(0, 0, 0, null);
    }
    
    public TransportBayPart(int tonnage, int bayNumber, double size, Campaign c) {
        super(tonnage, c);
        this.bayNumber = bayNumber;
        this.size = size;
        name = "Bay #" + bayNumber;
    }
    
    public int getBayNumber() {
        return bayNumber;
    }
    
    public Bay getBay() {
        if (null != unit) {
            return unit.getEntity().getBayById(bayNumber);
        }
        return null;
    }
    
    @Override
    public String getName() {
        if (null != getBay()) {
            return getBay().getType() + " Bay #" + bayNumber;
        }
        return super.getName();
    }

    @Override
    public int getBaseTime() {
        // Repair time is 4 hours. Replacement time is 1 month; using replacement time for refits if the
        // bay size changes.
        return 240;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        Bay bay = getBay();
        if (null != bay) {
            int prevHits = hits;
            hits = (int) bay.getBayDamage();
            int prevDoorHits = 0;
            for (int id : childPartIds) {
                final Part p = campaign.getPart(id);
                if ((p instanceof MissingBayDoor)
                        || ((p instanceof BayDoor) && p.needsFixing())) {
                    prevDoorHits++;
                }
            }
            if (prevDoorHits < bay.getDoors() - bay.getCurrentDoors()) {
                // We need an independent list of child parts so we don't get concurrent modification
                // exceptions from removing destroyed doors. We might as well filter anything that's
                // not an undamaged door at the same time.
                List<Part> doors = childPartIds.stream()
                        .map(id -> campaign.getPart(id))
                        .filter(p -> (p instanceof BayDoor) && !p.needsFixing())
                        .collect(Collectors.toList());
                for (Part door : doors) {
                    if (checkForDestruction
                            && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                        door.remove(false);
                    } else {
                        door.hits = 1;
                    }
                    prevDoorHits++;
                    if (prevDoorHits >= bay.getDoors() - bay.getCurrentDoors()) {
                        break;
                    }
                }
            }
            // If checking for destruction we need to remove a number of cubicles (if any)
            // equal to the increase in damage.
            if ((hits > prevHits) && checkForDestruction) {
                List<Part> cubicles = childPartIds.stream()
                        .map(id -> campaign.getPart(id))
                        .filter(p -> (null != p) && (p instanceof Cubicle))
                        .collect(Collectors.toList());
                while ((hits > prevHits) && !cubicles.isEmpty()) {
                    Part cubicle = cubicles.get(Compute.randomInt(cubicles.size()));
                    cubicle.remove(false);
                    prevHits++;
                }
            }
        }
    }

    @Override
    public void updateConditionFromPart() {
        Bay bay = getBay();
        if (null != bay) {
            int goodDoors = 0;
            int badCubicles = 0;
            for (int id : childPartIds) {
                final Part p = campaign.getPart(id);
                if (null != p) {
                    if ((p instanceof BayDoor) && !p.needsFixing()) {
                        goodDoors++;
                    } else if (p instanceof MissingCubicle) {
                        badCubicles++;
                    }
                }
            }
            bay.setCurrentDoors(goodDoors);
            // Even if the bay is repaired, it still has reduced capacity until the cubicles are replaced. 
            bay.setBayDamage(Math.max(hits, badCubicles));
        }
        
    }
    
    @Override
    public void fix() {
        super.fix();
        Bay bay = getBay();
        if (null != bay) {
            bay.setBayDamage(0);
        }
    }

    @Override
    public void remove(boolean salvage) {
        // Can't remove a bay
    }
    
    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    public MissingPart getMissingPart() {
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public int getDifficulty() {
        return -3;
    }

    @Override
    public long getStickerPrice() {
        // Considered part of the structure, though cubicles and doors can add to this value
        return 0;
    }

    @Override
    public double getTonnage() {
        return size;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return false;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "bayNumber", bayNumber);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "size", size);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("bayNumber")) {
                bayNumber = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("size")) {
                size = Double.parseDouble(wn2.getTextContent());
            }
        }
    }

    @Override
    public Part clone() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        Bay bay = getBay();
        if (null != bay) {
            return bay.getTechAdvancement();
        }
        return Bay.techAdvancement();
    }

}
