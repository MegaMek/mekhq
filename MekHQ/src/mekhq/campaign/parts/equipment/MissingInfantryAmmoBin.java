/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.annotations.Nullable;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;

/**
 * Ammo bin missing from a small support vehicle
 */
public class MissingInfantryAmmoBin extends MissingAmmoBin {
    private static final MMLogger logger = MMLogger.create(MissingInfantryAmmoBin.class);

    private InfantryWeapon weaponType;

    // Used in deserialization
    public MissingInfantryAmmoBin() {
        this(0, null, 0, null, 0, false, null);
    }

    /**
     * Construct a new placeholder for a missing infantry ammo bin
     *
     * @param tonnage    The weight of the unit it's installed on
     * @param ammoType   The type of ammo
     * @param equipNum   The equipment index on the unit
     * @param weaponType The weapon this ammo is for
     * @param clips      The number of clips of ammo
     * @param omniPodded Whether the weapon is pod-mounted on an omnivehicle
     * @param c          The campaign instance
     */
    public MissingInfantryAmmoBin(int tonnage, @Nullable AmmoType ammoType, int equipNum,
            @Nullable InfantryWeapon weaponType, int clips, boolean omniPodded, @Nullable Campaign c) {
        super(tonnage, ammoType, equipNum, false, omniPodded, c);
        this.weaponType = weaponType;
        this.size = clips;
        if (weaponType != null) {
            name = weaponType.getName() + " Ammo Bin";
        }
    }

    @Override
    public void restore() {
        super.restore();
        if (getWeaponType() != null) {
            name = getWeaponType().getName() + " Ammo Bin";
        } else {
            logger.error("MissingInfantryAmmoBin does not have a weapon type!");
        }
    }

    public @Nullable InfantryWeapon getWeaponType() {
        return weaponType;
    }

    /**
     * Gets the number of clips stored in this ammo bin.
     */
    public int getClips() {
        return (int) getSize();
    }

    @Override
    public String getLocationName() {
        int loc = getLocation();
        if ((loc >= 0) && (loc < unit.getEntity().locations())) {
            return unit.getEntity().getLocationName(loc);
        } else {
            return null;
        }
    }

    @Override
    public int getLocation() {
        if (unit != null) {
            Mounted<?> m = unit.getEntity().getEquipment(equipmentNum);
            while (m.getLinkedBy() != null) {
                m = m.getLinkedBy();
            }
            return m.getLocation();
        }
        return Entity.LOC_NONE;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        // Do not try to replace a MissingInfantryAmmoBin with anything other
        // than an InfantryAmmoBin. Subclasses should use a similar check, which
        // breaks Composability to a degree but in this case we've used
        // subclasses where they're not truly composable.
        if ((part instanceof InfantryAmmoBin)
                && (part.getClass() == InfantryAmmoBin.class)) {
            InfantryAmmoBin bin = (InfantryAmmoBin) part;
            return getType().equals(bin.getType())
                    && getWeaponType().equals(bin.getWeaponType())
                    && (getClips() == bin.getClips());
        }
        return false;
    }

    @Override
    protected int getFullShots() {
        return getWeaponType().getShots() * getClips();
    }

    @Override
    public InfantryAmmoBin getNewPart() {
        return new InfantryAmmoBin(getUnitTonnage(), getType(), -1, getFullShots(),
                getWeaponType(), getClips(), omniPodded, campaign);
    }

    @Override
    public void writeToXMLEnd(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "weaponType", getWeaponType().getInternalName());
        super.writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node node) {
        NodeList nl = node.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            if (wn.getNodeName().equals("weaponType")) {
                this.weaponType = (InfantryWeapon) EquipmentType.get(wn.getTextContent().trim());
            }
        }

        super.loadFieldsFromXmlNode(node);
    }

}
