/*
 * Copyright (c) 2020 - The MegaMek Team
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

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * Ammo bin for infantry weapons used by small support vehicles
 */
public class InfantryAmmoBin extends AmmoBin {

    private InfantryWeapon weaponType;

    // Used in deserialization
    @SuppressWarnings("unused")
    public InfantryAmmoBin() {
        this(0, null, 0, 0, null, 0, false, null);
    }

    /**
     * Construct a new bin for infantry ammo
     *
     * @param tonnage     The weight of the unit it's installed on
     * @param ammoType    The type of ammo
     * @param equipNum    The equipment index on the unit
     * @param shots       The number of shots of ammo needed to refill the bin
     * @param weaponType  The weapon this ammo is for
     * @param omniPodded  Whether the weapon is pod-mounted on an omnivehicle
     * @param c           The campaign instance
     */
    public InfantryAmmoBin(int tonnage, EquipmentType ammoType, int equipNum, int shots,
                           InfantryWeapon weaponType, double size, boolean omniPodded, Campaign c) {
        super(tonnage, ammoType, equipNum, shots, false, omniPodded, c);
        this.size = size;
        if (weaponType != null) {
            this.weaponType = weaponType;
            name = weaponType.getName() + " Ammo Bin";
        }
    }

    @Override
    public void restore() {
        super.restore();
        name = weaponType.getName() + " Ammo Bin";
    }

    /**
     * @return The weapon this ammo is for
     */
    public InfantryWeapon getWeaponType() {
        return weaponType;
    }

    @Override
    public InfantryAmmoBin clone() {
        InfantryAmmoBin clone = new InfantryAmmoBin(getUnitTonnage(), getType(), getEquipmentNum(), shotsNeeded,
                weaponType, size, omniPodded, campaign);
        clone.copyBaseData(this);
        clone.munition = this.munition;
        return clone;
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
            Mounted m = unit.getEntity().getEquipment(equipmentNum);
            while (m.getLinkedBy() != null) {
                m = m.getLinkedBy();
            }
            return m.getLocation();
        }
        return Entity.LOC_NONE;
    }

    @Override
    public double getTonnage() {
        return weaponType.getAmmoWeight();
    }

    @Override
    public int getFullShots() {
        return weaponType.getShots() * (int) getSize();
    }

    /**
     * Changes the capacity of this bin. This is done when redistributing capacity between
     * standard and inferno munitions.
     *
     * @param clips The new capacity in number of clips
     */
    public void changeCapacity(int clips) {
        int current = getCurrentShots();
        size = clips;
        shotsNeeded = getFullShots() - current;
        // Wait until loading/unloading to change the full number of shots on the Entity.
    }

    @Override
    public void loadBin() {
        super.loadBin();
        if (unit != null) {
            Mounted mount = unit.getEntity().getEquipment(getEquipmentNum());
            mount.setOriginalShots(getFullShots());
        }
    }

    @Override
    protected Money getPricePerTon() {
        return Money.of(weaponType.getAmmoCost() / weaponType.getAmmoWeight());
    }

    @Override
    protected int getShotsPerTon() {
        return (int) Math.floor(weaponType.getShots() / weaponType.getAmmoWeight());
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        super.updateConditionFromEntity(checkForDestruction);
        if (unit != null) {
            Mounted mount = unit.getEntity().getEquipment(getEquipmentNum());
            shotsNeeded = mount.getOriginalShots() - mount.getBaseShotsLeft();
        }
    }

    @Override
    public void writeToXmlEnd(PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, "weaponType", weaponType.getInternalName());
        super.writeToXmlEnd(pw, indent);
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
    }

    @Override
    public void fix() {
        // If we have reconfigured the distribution between standard and inferno ammo,
        // there may be extra that needs to be removed from the partner bin to make room.
        // We'll do that automatically to make it simpler.
        InfantryAmmoBin partner = findPartnerBin();
        if ((partner != null) && (partner.getShotsNeeded() < 0)) {
            partner.loadBin();
        }
        loadBin();
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingInfantryAmmoBin(getUnitTonnage(), type, equipmentNum, weaponType,
                size, omniPodded, campaign);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return  (part instanceof InfantryAmmoBin)
                && super.isSamePartType(part)
                && (weaponType == ((InfantryAmmoBin) part).weaponType)
                && size == ((InfantryAmmoBin) part).size;
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (shotsNeeded < 0) {
            return type.getDesc() + ", remove " + (-getShotsNeeded());
        } else {
            return super.getDetails(includeRepairDetails);
        }
    }

    @Override
    public void changeAmountAvailable(int amount, final AmmoType curType) {
        InfantryAmmoStorage a = (InfantryAmmoStorage) campaign.getWarehouse().findSparePart(part ->
            InfantryAmmoStorage.isRightAmmo(part, (AmmoType) getType(), getWeaponType()));

        if (a != null) {
            a.changeShots(amount);
            if (a.getShots() <= 0) {
                campaign.getWarehouse().removePart(a);
            }
        } else if (amount > 0) {
            campaign.getQuartermaster().addPart(new InfantryAmmoStorage(1, curType, amount, weaponType, campaign), 0);
        }
    }

    @Override
    public boolean isCompatibleAmmo(AmmoType a1, AmmoType a2) {
        return false;
    }

    public int getAmountAvailable() {
        final AmmoType thisType = (AmmoType) getType();
        return campaign.getWarehouse().streamSpareParts()
            .filter(part -> part instanceof InfantryAmmoStorage && part.isPresent()
                    && InfantryAmmoStorage.isRightAmmo(part, thisType, weaponType))
            .mapToInt(part -> ((InfantryAmmoStorage) part).getShots())
            .sum();
    }

    /**
     * Weapons with configurable ammo have two ammo bin parts.
     * @return The other bin for the same weapon, or null if there isn't one.
     */
    public @Nullable InfantryAmmoBin findPartnerBin() {
        if (unit != null) {
            Mounted mount = unit.getEntity().getEquipment(getEquipmentNum());
            int index = -1;
            if (mount.getLinked() != null) {
                index = unit.getEntity().getEquipmentNum(mount.getLinked());
            } else if (mount.getLinkedBy().getType() instanceof AmmoType) {
                index = unit.getEntity().getEquipmentNum(mount.getLinkedBy());
            }
            for (Part part : unit.getParts()) {
                if ((part instanceof InfantryAmmoBin) && (((InfantryAmmoBin) part).getEquipmentNum() == index)) {
                    return (InfantryAmmoBin) part;
                }
            }
        }
        return null;
    }

    @Override
    public String getAcquisitionDisplayName() {
        return weaponType.getName() + ": " + type.getName();
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return weaponType.getShots() + " shots (1 clip)";
    }

    public Part getNewPart() {
        return new InfantryAmmoStorage(1, type, weaponType.getShots() * (int) getSize(),
                weaponType, campaign);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return weaponType.getTechAdvancement();
    }
}
