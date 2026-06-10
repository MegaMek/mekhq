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
import megamek.common.equipment.Mounted;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A conventional infantry platoon's one-shot Disposable Weapon loadout (TO:AR p.106). One part represents the whole
 * platoon's disposables (one per trooper), so it is valued at the weapon cost times the number of troopers. Because it
 * is mounted on the real Disposable Weapon equipment slot, it tracks whether the platoon fired its disposables this
 * scenario ({@link Mounted#isFired()}); a fired loadout is "spent" until reloaded.
 */
public class InfantryDisposableWeaponPart extends InfantryWeaponPart {
    private static final MMLogger LOGGER = MMLogger.create(InfantryDisposableWeaponPart.class);
    private static final String DISPOSABLE_SUFFIX = " (Disposable)";

    private int troopers;
    private boolean spent;

    public InfantryDisposableWeaponPart() {
        this(0, null, -1, 0, null);
    }

    public InfantryDisposableWeaponPart(int tonnage, EquipmentType et, int equipNum, int troopers, Campaign c) {
        super(tonnage, et, equipNum, c, false);
        this.troopers = troopers;
        if (et != null) {
            name = et.getName() + DISPOSABLE_SUFFIX;
        }
    }

    public int getTroopers() {
        return troopers;
    }

    /**
     * @return true if the platoon has fired (and not yet reloaded) its Disposable Weapons this scenario
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
              getEquipmentNum(), troopers, campaign);
        clone.copyBaseData(this);
        clone.spent = spent;
        return clone;
    }

    /** The platoon carries one Disposable Weapon per trooper, so the loadout is valued at weapon cost x troopers. */
    @Override
    public Money getStickerPrice() {
        return super.getStickerPrice().multipliedBy(Math.max(1, troopers));
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        super.updateConditionFromEntity(checkForDestruction);
        final Mounted<?> mounted = getMounted();
        spent = (mounted != null) && mounted.isFired();
    }

    @Override
    public void updateConditionFromPart() {
        super.updateConditionFromPart();
        final Mounted<?> mounted = getMounted();
        if (mounted != null) {
            mounted.setFired(spent);
        }
    }

    @Override
    public boolean needsFixing() {
        return spent || super.needsFixing();
    }

    /**
     * Finds a spare Disposable Weapon of the same weapon type in the warehouse to reload from. Like infantry ammo
     * ({@code Quartermaster.findSpareAmmo}) this matches by weapon type only - NOT by the strict
     * {@code isSamePartType}/sticker-price equality used by {@code checkForExistingSparePart}, which fails because an
     * on-unit part and a warehouse spare compute price/tonnage differently.
     *
     * @return a matching spare with stock available, or null if none
     */
    private @Nullable Part findSpareReload() {
        final String weaponInternalName = (type == null) ? null : type.getInternalName();
        if (weaponInternalName == null) {
            return null;
        }
        // Match any warehouse spare of the same weapon type - the parts store stocks a disposable weapon as a plain
        // EquipmentPart, so we cannot require our own InfantryDisposableWeaponPart subclass here. findSparePart already
        // restricts this to warehouse spares (not parts mounted on a unit), so the platoon's own loadout is excluded.
        return getWarehouse().findSparePart(spare -> (spare instanceof EquipmentPart equipmentSpare)
              && (spare.getId() != getId())
              && (spare.getQuantity() > 0)
              && (equipmentSpare.getType() != null)
              && weaponInternalName.equals(equipmentSpare.getType().getInternalName()));
    }

    /**
     * Reloading consumes one matching spare from the warehouse (the resupply cost) and clears the fired/spent state.
     * Mirrors {@code InfantryAmmoBin.fix()}: this is pure resupply and deliberately does NOT call
     * {@code super.fix()} (the {@code EquipmentPart} repair-system flow), so the platoon reloads its own disposables
     * without needing an external tech. {@link #checkFixable()} ensures this is only reached when a spare exists.
     */
    @Override
    public void fix() {
        Part spare = findSpareReload();
        if (spare != null) {
            spare.changeQuantity(-1);
        }
        final Mounted<?> mounted = getMounted();
        if (mounted != null) {
            mounted.setFired(false);
        }
        spent = false;
    }

    @Override
    public @Nullable String checkFixable() {
        // A spent loadout can only be reloaded if there is a spare Disposable Weapon in the warehouse to draw from.
        if (spent && (findSpareReload() == null)) {
            return getName() + " has no spare in stock to reload";
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
        return new MissingInfantryDisposableWeaponPart(getUnitTonnage(), type, getEquipmentNum(), troopers, campaign);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        return spent ? (getName() + " - spent") : super.getDetails(includeRepairDetails);
    }

    /** A spent loadout is reloaded, not repaired, so present the task as "Reload ..." (like {@code AmmoBin}). */
    @Override
    public String getDesc() {
        if (!spent || isSalvaging()) {
            return super.getDesc();
        }
        return "<html><b>Reload " + getName() + "</b><br/>" + getDetails() + "<br/>" + getTimeLeft() + " minutes"
              + ((getTech() != null) ? " (scheduled) " : "") + "</html>";
    }

    @Override
    public boolean isSamePartType(@Nullable Part part) {
        return super.isSamePartType(part) && (troopers == ((InfantryDisposableWeaponPart) part).troopers);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", getEquipmentNum());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "troopers", troopers);
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
                } else if (wn2.getNodeName().equalsIgnoreCase("troopers")) {
                    troopers = Integer.parseInt(wn2.getTextContent());
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
