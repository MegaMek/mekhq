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
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.CriticalSlot;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.parts.missing.MissingTurret;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Turret extends TankLocation {
    private static final MMLogger LOGGER = MMLogger.create(Turret.class);

    protected double weight;

    public Turret() {
        this(0, 0, null);
    }

    public Turret(int loc, int tonnage, Campaign c) {
        super(loc, tonnage, c);
        weight = 0;
        this.name = "Turret";
        this.unitTonnageMatters = true;
    }

    @Override
    public Turret clone() {
        Turret clone = new Turret(0, getUnitTonnage(), weight, campaign);
        clone.copyBaseData(this);
        clone.loc = this.loc;
        clone.damage = this.damage;
        clone.breached = this.breached;
        return clone;
    }

    public Turret(int loc, int tonnage, double weight, Campaign c) {
        super(loc, tonnage, c);
        this.weight = weight;
        this.name = "Turret";
    }

    @Override
    public void setUnit(Unit u) {
        super.setUnit(u);
        if (null != unit) {
            weight = 0;
            for (Mounted<?> m : unit.getEntity().getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (m.getLocation() == this.loc) {
                    weight += wt.getTonnage(unit.getEntity()) / 10.0;
                }
            }
            weight = Math.ceil(weight * 2) / 2;
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof Turret
                     && getLoc() == ((Turret) part).getLoc()
                     && getTonnage() == part.getTonnage();
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loc", loc);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "damage", damage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "weight", weight);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("weight")) {
                    weight = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                    loc = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("damage")) {
                    damage = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingTurret(getUnitTonnage(), weight, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.changeQuantity(1);
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0, false);
            ((Tank) unit.getEntity()).unlockTurret();
        }
        setUnit(null);
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 160;
        }
        return 60;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 1;
        }
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.getEntity().setInternal(unit.getEntity().getOInternal(loc) - damage, loc);
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (isSalvaging()) {
            // check for armor
            if (unit.getEntity().getArmorForReal(loc, false) > 0) {
                return "must salvage armor in this location first";
            }
            // you can only salvage a location that has nothing left on it
            for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty & non-hittable slots
                if ((slot == null) || !slot.isEverHittable()) {
                    continue;
                }
                if (slot.isRepairable()) {
                    return "Repairable parts in " + unit.getEntity().getLocationName(loc)
                                 + " must be salvaged or scrapped first.";
                }
            }
        }
        return null;
    }

    @Override
    public String checkScrappable() {
        // check for armor
        if (unit.getEntity().getArmor(loc, false) != IArmorState.ARMOR_DESTROYED) {
            return "You must scrap armor in the turret first";
        }
        // you can only scrap a location that has nothing left on it
        for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }
            if (slot.isRepairable()) {
                return "You must scrap all equipment in the turret first";
            }
        }
        return null;
    }

    @Override
    public boolean canNeverScrap() {
        return false;
    }

    @Override
    public double getTonnage() {
        return weight;
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(5000 * weight);
    }
}
