/*
 * MissingAmmoBin.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;
import java.util.Objects;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.annotations.Nullable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;

import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingAmmoBin extends MissingEquipmentPart {
    private static final long serialVersionUID = 2892728320891712304L;

    protected boolean oneShot;

    public MissingAmmoBin() {
        this(0, null, -1, false, false, null);
    }

    public MissingAmmoBin(int tonnage, @Nullable AmmoType et, int equipNum, boolean singleShot,
                          boolean omniPodded, @Nullable Campaign c) {
        super(tonnage, et, equipNum, c, 1.0, 1.0, omniPodded);
        this.oneShot = singleShot;
        if (null != name) {
            this.name += " Bin";
        }
    }

    @Override
    public AmmoType getType() {
        return (AmmoType) super.getType();
    }

    /* Per TM, ammo for fighters is stored in the fuselage. This makes a difference for omnifighter
     * pod space, so we're going to stick them in LOC_NONE where the heat sinks are */
    @Override
    public String getLocationName() {
        if (unit.getEntity() instanceof Aero
                && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))) {
            return "Fuselage";
        }
        return super.getLocationName();
    }

    @Override
    public int getLocation() {
        if (unit.getEntity() instanceof Aero
                && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))) {
            return Aero.LOC_NONE;
        }
        return super.getLocation();
    }

    @Override
    public int getDifficulty() {
        return -2;
    }

    @Override
    public boolean hasReplacementPart() {
        return true;
    }

    @Override
    public Part getReplacementPart() {
        return getNewPart();
    }

    @Override
    public void reservePart() {
        // No need to reserve a part for a missing AmmoBin, they're free.
    }

    @Override
    public void cancelReservation() {
        // We do not need to return a replacement part, they're free/fake
        setReplacementPart(null); // CAW: clears out anything from a prior version
    }

    @Override
    public void fix() {
        AmmoBin replacement = getNewPart();
        unit.addPart(replacement);
        campaign.getQuartermaster().addPart(replacement, 0);

        remove(false);

        // Add the replacement part to the unit
        replacement.setEquipmentNum(getEquipmentNum());
        replacement.updateConditionFromPart();
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        // Do not try to replace a MissingAmmoBin with anything other
        // than an AmmoBin. Subclasses should use a similar check, which
        // breaks Composability to a degree but in this case we've used
        // subclasses where they're not truly composable.
        return Objects.equals(part.getClass(), AmmoBin.class)
                && getType().equals(((AmmoBin) part).getType())
                && (isOneShot() == ((AmmoBin) part).isOneShot());
    }

    public boolean isOneShot() {
        return oneShot;
    }

    protected int getFullShots() {
        return oneShot ? 1 : getType().getShots();
    }

    @Override
    public AmmoBin getNewPart() {
        return new AmmoBin(getUnitTonnage(), getType(), -1, getFullShots(), oneShot, omniPodded, campaign);
    }

    @Override
    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        if (oneShot) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "oneShot", oneShot);
        }

        super.writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {

        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("oneShot")) {
                oneShot = Boolean.parseBoolean(wn2.getTextContent().trim());
            }
        }

        super.loadFieldsFromXmlNode(wn);
    }

    @Override
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.AMMO;
    }
}
