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
package mekhq.campaign.parts;

import megamek.common.*;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * Storage for infantry weapon ammo. The AmmoType is a placeholder and distinguishes between
 * standard and inferno munitions, but does not distinguish the type of weapon.
 */
public class InfantryAmmoStorage extends AmmoStorage {

    private InfantryWeapon weaponType;

    @SuppressWarnings("unused")
    public InfantryAmmoStorage() {
        this(0, null, 0, null, null);
    }

    public InfantryAmmoStorage(int tonnage, EquipmentType et, int shots,
                               InfantryWeapon weaponType, Campaign c) {
        super(tonnage, et, shots, c);
        this.weaponType = weaponType;
    }

    @Override
    public void restore() {
        super.restore();
        if (weaponType != null) {
            if (weaponType.hasInfernoAmmo()) {
                this.name = weaponType.getShortName() + getType().getName();
            } else {
                this.name = weaponType.getShortName() + " Ammo";
            }
        }
    }

    /**
     * @return The type of weapon this ammo is for
     */
    public InfantryWeapon getWeaponType() {
        return weaponType;
    }

    public InfantryAmmoStorage clone() {
        InfantryAmmoStorage storage = new InfantryAmmoStorage(0, getType(), shots, weaponType, campaign);
        storage.copyBaseData(this);
        storage.munition = this.munition;
        return storage;
    }


    @Override
    public double getTonnage() {
        return weaponType.getAmmoWeight() * getShots() / weaponType.getShots();
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(weaponType.getAmmoCost() * (double) getShots() / weaponType.getShots());
    }

    @Override
    public Money getCurrentValue() {
        return getStickerPrice();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof InfantryAmmoStorage)
                && (getType() == ((InfantryAmmoStorage) part).getType())
                && (getWeaponType() == ((InfantryAmmoStorage) part).getWeaponType());
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
                weaponType = (InfantryWeapon) EquipmentType.get(wn.getTextContent());
            }
        }
        super.loadFieldsFromXmlNode(node);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return weaponType.getTechAdvancement();
    }

    /**
     * Boolean function that determines if the given part is of the correct ammo.
     * Useful as a predicate for campaign.findSparePart()
     */
    public static boolean isRightAmmo(Part part, AmmoType curType, WeaponType weaponType) {
        return part instanceof InfantryAmmoStorage
                && part.isPresent()
                && (((InfantryAmmoStorage) part).getType()).equals(curType)
                && curType.getMunitionType() == ((AmmoType) ((AmmoStorage) part).getType()).getMunitionType()
                && (((InfantryAmmoStorage) part).getWeaponType()).equals(weaponType);
    }

    public void changeAmountAvailable(int amount, final AmmoType curType) {
        InfantryAmmoStorage a = (InfantryAmmoStorage) campaign.getWarehouse().findSparePart(part ->
                isRightAmmo(part, curType, weaponType));

        if (null != a) {
            a.changeShots(amount);
            if (a.getShots() <= 0) {
                campaign.getWarehouse().removePart(a);
            }
        } else if (amount > 0) {
            campaign.getQuartermaster().addPart(new InfantryAmmoStorage(1, curType, amount, weaponType, campaign), 0);
        }
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return weaponType.getShots() + " shots (1 clip)";
    }

    public Part getNewPart() {
        return new InfantryAmmoStorage(1, type, weaponType.getShots(),
                weaponType, campaign);
    }
}
