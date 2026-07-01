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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

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
 * A single one-shot Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing) carried by one trooper. A platoon/squad
 * gets one of these per trooper (like the primary/secondary {@link InfantryWeaponPart}), so the loadout is valued,
 * refit, and bought/sold as that many individual weapons. The whole platoon fires its disposables together in one
 * volley; this part tracks whether that has happened (and not yet been reloaded) via the {@code spent} flag, which is
 * driven by the platoon's single fireable Disposable Weapon mount ({@link WeaponMounted#isFired()}).
 */
public class InfantryDisposableWeaponPart extends InfantryWeaponPart {
    private static final MMLogger LOGGER = MMLogger.create(InfantryDisposableWeaponPart.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    /** Reloading the platoon's disposables is a quick resupply task, not a repair (minutes). */
    private static final int RELOAD_BASE_TIME_MINUTES = 15;

    private boolean spent;

    public InfantryDisposableWeaponPart() {
        this(0, null, -1, null);
    }

    public InfantryDisposableWeaponPart(int tonnage, EquipmentType equipmentType, int equipNum, Campaign campaign) {
        super(tonnage, equipmentType, equipNum, campaign, false);
        if (equipmentType != null) {
            name = buildDisposableName(equipmentType);
        }
    }

    /**
     * @param weaponType the disposable weapon's equipment type, never null
     *
     * @return the part name for a disposable weapon of the given type, e.g. "Rocket Launcher (LAW) (Disposable)"
     */
    static String buildDisposableName(EquipmentType weaponType) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "InfantryDisposableWeaponPart.name.format", weaponType.getName());
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
            name = buildDisposableName(type);
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

    /**
     * Finds the platoon's fireable Disposable Weapon mount of this part's weapon type. A unit carries at most one
     * disposable mount per weapon type (a conventional infantry platoon carries at most one in total -
     * {@code ConvInfantry#equipDisposableWeapon} replaces any existing disposable mount), so the first match is the
     * mount the whole platoon shares.
     *
     * @return the platoon's single fireable Disposable Weapon mount of this weapon type, or null
     */
    private @Nullable WeaponMounted findDisposableMount() {
        if ((unit == null) || (unit.getEntity() == null) || (type == null)) {
            return null;
        }
        for (WeaponMounted weaponMounted : unit.getEntity().getWeaponList()) {
            if (weaponMounted.isDisposableWeapon() && isSameWeaponType(weaponMounted.getType())) {
                return weaponMounted;
            }
        }
        return null;
    }

    /**
     * Compares weapon types by internal name - the unique key equipment is registered under
     * ({@link EquipmentType#get(String)}), so it is stable across save/load and equipment-singleton restores.
     *
     * @param otherType the candidate equipment type, or null
     *
     * @return true if the candidate is the same weapon type as this part
     */
    private boolean isSameWeaponType(@Nullable EquipmentType otherType) {
        return (type != null) && (otherType != null) && type.getInternalName().equals(otherType.getInternalName());
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        super.updateConditionFromEntity(checkForDestruction);
        // Read-only: the whole platoon shares one fireable mount, so every per-trooper disposable part is spent
        // exactly when that mount has been fired. Do NOT mutate the entity here - that would corrupt a refit diff
        // computed off the same entity.
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
        int count = 0;
        for (Part part : unit.getParts()) {
            if (part instanceof InfantryDisposableWeaponPart) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    /** @return total spare weapons of this disposable type available in the warehouse. */
    private int availableReloadStock() {
        if (type == null) {
            return 0;
        }
        // The warehouse can hold a very large number of parts, so a stream is appropriate here.
        return getWarehouse().getSpareParts().stream()
              .filter(spare -> (spare instanceof EquipmentPart equipmentSpare)
                    && isSameWeaponType(equipmentSpare.getType()))
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
        if (type == null) {
            return null;
        }
        return getWarehouse().findSparePart(this::isUsableReloadSpare);
    }

    /**
     * @param spare a warehouse spare candidate, never null (supplied by the warehouse)
     *
     * @return true if the spare is another part of this weapon type with stock remaining
     */
    private boolean isUsableReloadSpare(Part spare) {
        boolean isOtherPart = spare.getId() != getId();
        boolean hasStock = spare.getQuantity() > 0;
        boolean isSameWeapon = (spare instanceof EquipmentPart equipmentSpare)
              && isSameWeaponType(equipmentSpare.getType());
        return isOtherPart && hasStock && isSameWeapon;
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
        boolean needsReload = spent && (mount != null) && mount.isFired();
        boolean lacksStock = availableReloadStock() < trooperCount();
        if (needsReload && lacksStock) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "InfantryDisposableWeaponPart.notEnoughSpares.format",
                  getName());
        }
        return super.checkFixable();
    }

    @Override
    public int getBaseTime() {
        return spent ? RELOAD_BASE_TIME_MINUTES : super.getBaseTime();
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
        if (spent) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "InfantryDisposableWeaponPart.details.spent.format", getName());
        }
        return super.getDetails(includeRepairDetails);
    }

    @Override
    public void writeToXML(final PrintWriter printWriter, int indent) {
        indent = writeToXMLBegin(printWriter, indent);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "equipmentNum", getEquipmentNum());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "spent", spent);
        writeToXMLEnd(printWriter, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);
            String nodeName = childNode.getNodeName();
            try {
                if (nodeName.equalsIgnoreCase("equipmentNum")) {
                    equipmentNum = Integer.parseInt(childNode.getTextContent());
                } else if (nodeName.equalsIgnoreCase("typeName")) {
                    typeName = childNode.getTextContent();
                } else if (nodeName.equalsIgnoreCase("equipTonnage")) {
                    equipTonnage = Double.parseDouble(childNode.getTextContent());
                } else if (nodeName.equalsIgnoreCase("spent")) {
                    spent = Boolean.parseBoolean(childNode.getTextContent().trim());
                }
            } catch (Exception exception) {
                LOGGER.error(nodeName, exception);
            }
        }
        restore();
    }
}
