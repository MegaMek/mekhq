/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;
import java.util.Objects;

import megamek.common.units.Aero;
import megamek.common.equipment.AmmoType;
import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingAmmoBin extends MissingEquipmentPart {
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
        if ((null != unit) && (unit.getEntity() instanceof Aero)
                  && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))) {
            return "Fuselage";
        }
        return super.getLocationName();
    }

    @Override
    public int getLocation() {
        if ((null != unit) && (unit.getEntity() instanceof Aero)
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
    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        if (oneShot) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "oneShot", oneShot);
        }

        super.writeToXMLEnd(pw, indent);
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
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.AMMUNITION;
    }
}
