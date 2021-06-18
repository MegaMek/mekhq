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
import megamek.common.annotations.Nullable;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * Storage for infantry weapon ammo. The AmmoType is a placeholder and distinguishes between
 * standard and inferno munitions, but does not distinguish the type of weapon.
 */
public class InfantryAmmoStorage extends AmmoStorage {

    private static final long serialVersionUID = 930316771091252049L;

    private InfantryWeapon weaponType;

    @SuppressWarnings("unused")
    public InfantryAmmoStorage() {
        this(0, null, 0, null, null);
    }

    public InfantryAmmoStorage(int tonnage, @Nullable AmmoType et, int shots,
            @Nullable InfantryWeapon weaponType, @Nullable Campaign c) {
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
        } else {
            MekHQ.getLogger().error("InfantryAmmoStorage does not have a weapon type!");
        }
    }

    /**
     * @return The type of weapon this ammo is for
     */
    public InfantryWeapon getWeaponType() {
        return weaponType;
    }

    public InfantryAmmoStorage clone() {
        InfantryAmmoStorage storage = new InfantryAmmoStorage(0, getType(), getShots(), getWeaponType(), getCampaign());
        storage.copyBaseData(this);
        return storage;
    }


    @Override
    public double getTonnage() {
        return getWeaponType().getAmmoWeight() * getShots() / getWeaponType().getShots();
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(getWeaponType().getAmmoCost() * (double) getShots() / getWeaponType().getShots());
    }

    @Override
    public Money getCurrentValue() {
        return getStickerPrice();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return getClass().equals(part.getClass())
                && isSameAmmoType(((InfantryAmmoStorage) part).getType(), ((InfantryAmmoStorage) part).getWeaponType());
    }

    /**
     * Gets a value indicating whether or not the {@code AmmoType} for the {@code InfantryWeapon}
     * is the same as this instance.
     * @param ammoType The {@code AmmoType}.
     * @param weaponType The {@code InfantryWeapon} carrying the ammo.
     */
    public boolean isSameAmmoType(AmmoType ammoType, InfantryWeapon weaponType) {
        return isSameAmmoType(ammoType)
                && Objects.equals(getWeaponType(), weaponType);
    }

    @Override
    public boolean isCompatibleAmmo(AmmoType otherAmmoType) {
        // Cannot change between infantry ammo types
        return false;
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
        return getWeaponType().getTechAdvancement();
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return getWeaponType().getShots() + " shots (1 clip)";
    }

    @Override
    public InfantryAmmoStorage getNewPart() {
        return new InfantryAmmoStorage(1, getType(), getWeaponType().getShots(),
                getWeaponType(), campaign);
    }
}
