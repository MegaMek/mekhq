/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A single one-shot Disposable Weapon (TO:AR p.106) carried by one trooper. A platoon/squad gets one of these per
 * trooper (like the primary/secondary {@link InfantryWeaponPart}), so the loadout is valued, refit, and bought/sold as
 * that many individual weapons. The whole platoon fires its disposables together in one volley; this part tracks
 * whether that has happened (and not yet been reloaded) via the {@code spent} flag, which is driven by the platoon's
 * single fireable Disposable Weapon mount ({@link WeaponMounted#isFired()}).
 */
public class InfantryDisposableWeaponPart extends InfantryWeaponPart {
    private static final MMLogger LOGGER = MMLogger.create(InfantryDisposableWeaponPart.class);
    private static final String DISPOSABLE_SUFFIX = " (Disposable)";

    private boolean spent;

    public InfantryDisposableWeaponPart() {
        this(0, null, -1, null);
    }

    public InfantryDisposableWeaponPart(int tonnage, EquipmentType et, int equipNum, Campaign c) {
        super(tonnage, et, equipNum, c, false);
        if (et != null) {
            name = et.getName() + DISPOSABLE_SUFFIX;
        }
    }

    /**
     * @return true if this trooper's Disposable Weapon has been fired and not yet reloaded
     */
    public boolean isSpent() {
        return spent;
    }

    @Override
    public void restore() {
        super.restore();
        if (type != null) {
            name = type.getName() + DISPOSABLE_SUFFIX;
        }
    }

    @Override
    public InfantryDisposableWeaponPart clone() {
        InfantryDisposableWeaponPart clone = new InfantryDisposableWeaponPart(getUnitTonnage(), getType(),
              getEquipmentNum(), campaign);
        clone.copyBaseData(this);
        clone.spent = spent;
        return clone;
    }

    /** @return the platoon's single fireable Disposable Weapon mount of this weapon type, or null */
    private @Nullable WeaponMounted findDisposableMount() {
        if ((unit == null) || (unit.getEntity() == null) || (type == null)) {
            return null;
        }
        return unit.getEntity().getWeaponList().stream()
              .filter(WeaponMounted::isDisposableWeapon)
              .filter(weaponMounted -> (weaponMounted.getType() != null)
                    && type.getInternalName().equals(weaponMounted.getType().getInternalName()))
              .findFirst()
              .orElse(null);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        super.updateConditionFromEntity(checkForDestruction);
        // Read-only: the whole platoon shares one fireable mount, so every per-trooper disposable part is spent iff
        // that mount has been fired. Do NOT mutate the entity here - that would corrupt a refit diff computed off the
        // same entity.
        WeaponMounted mount = findDisposableMount();
        spent = (mount != null) && mount.isFired();
    }

    @Override
    public boolean needsFixing() {
        return spent || super.needsFixing();
    }

    /** @return the number of troopers carrying this disposable weapon (i.e. matching parts on the unit). */
    private int trooperCount() {
        if (unit == null) {
            return 1;
        }
        return (int) Math.max(1, unit.getParts().stream()
              .filter(part -> part instanceof InfantryDisposableWeaponPart)
              .count());
    }

    /** @return total spare weapons of this disposable type available in the warehouse. */
    private int availableReloadStock() {
        final String weaponInternalName = (type == null) ? null : type.getInternalName();
        if (weaponInternalName == null) {
            return 0;
        }
        return getWarehouse().getSpareParts().stream()
              .filter(spare -> (spare instanceof EquipmentPart equipmentSpare)
                    && (equipmentSpare.getType() != null)
                    && weaponInternalName.equals(equipmentSpare.getType().getInternalName()))
              .mapToInt(Part::getQuantity)
              .sum();
    }

    private void consumeReloadStock(int count) {
        int remaining = count;
        while (remaining > 0) {
            Part spare = findSpareReload();
            if (spare == null) {
                return;
            }
            int taken = Math.min(remaining, spare.getQuantity());
            spare.changeQuantity(-taken);
            remaining -= taken;
        }
    }

    /**
     * Finds a spare of this disposable weapon in the warehouse, matching by weapon type only (the parts store stocks a
     * disposable weapon as a plain {@link EquipmentPart}, so we cannot require our own subclass). Mirrors how infantry
     * ammo finds spares.
     */
    private @Nullable Part findSpareReload() {
        final String weaponInternalName = (type == null) ? null : type.getInternalName();
        if (weaponInternalName == null) {
            return null;
        }
        return getWarehouse().findSparePart(spare -> (spare instanceof EquipmentPart equipmentSpare)
              && (spare.getId() != getId())
              && (spare.getQuantity() > 0)
              && (equipmentSpare.getType() != null)
              && weaponInternalName.equals(equipmentSpare.getType().getInternalName()));
    }

    /**
     * Reloading restocks the whole platoon's disposables at once: the shared fireable mount is cleared and one spare
     * weapon per trooper is drawn from the warehouse. Pure resupply (no {@code super.fix()} repair flow), like infantry
     * ammo, so a self-crewed platoon reloads itself. Only the first reload while the mount is fired consumes stock; the
     * remaining per-trooper parts simply clear once their shared mount is no longer fired.
     */
    @Override
    public void fix() {
        WeaponMounted mount = findDisposableMount();
        if ((mount != null) && mount.isFired()) {
            consumeReloadStock(trooperCount());
            mount.setFired(false);
        }
        spent = false;
    }

    @Override
    public @Nullable String checkFixable() {
        WeaponMounted mount = findDisposableMount();
        if (spent && (mount != null) && mount.isFired() && (availableReloadStock() < trooperCount())) {
            return getName() + ": not enough spares in stock to reload the platoon";
        }
        return super.checkFixable();
    }

    @Override
    public int getBaseTime() {
        return spent ? 15 : super.getBaseTime();
    }

    @Override
    public int getDifficulty() {
        return spent ? 0 : super.getDifficulty();
    }

    @Override
    public MissingInfantryDisposableWeaponPart getMissingPart() {
        return new MissingInfantryDisposableWeaponPart(getUnitTonnage(), type, getEquipmentNum(), campaign);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        return spent ? (getName() + " - spent") : super.getDetails(includeRepairDetails);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", getEquipmentNum());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "spent", spent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
                    equipmentNum = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
                    typeName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
                    equipTonnage = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("spent")) {
                    spent = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
        restore();
    }
}
