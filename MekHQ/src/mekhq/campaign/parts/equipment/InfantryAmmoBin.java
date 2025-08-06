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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;
import java.util.Objects;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.InfantryAmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Ammo bin for infantry weapons used by small support vehicles
 */
public class InfantryAmmoBin extends AmmoBin {
    private InfantryWeapon weaponType;

    // Used in deserialization

    public InfantryAmmoBin() {
        this(0, null, 0, 0, null, 0, false, null);
    }

    /**
     * Construct a new bin for infantry ammo
     *
     * @param tonnage    The weight of the unit it's installed on
     * @param ammoType   The type of ammo
     * @param equipNum   The equipment index on the unit
     * @param shots      The number of shots of ammo needed to refill the bin
     * @param weaponType The weapon this ammo is for
     * @param clips      The number of clips of ammo
     * @param omniPodded Whether the weapon is pod-mounted on an omnivehicle
     * @param c          The campaign instance
     */
    public InfantryAmmoBin(int tonnage, @Nullable AmmoType ammoType, int equipNum, int shots,
          @Nullable InfantryWeapon weaponType, int clips, boolean omniPodded, @Nullable Campaign c) {
        super(tonnage, ammoType, equipNum, shots, false, omniPodded, c);
        this.size = clips;
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

    /**
     * Gets the number of clips stored in this ammo bin.
     */
    public int getClips() {
        return (int) getSize();
    }

    @Override
    public InfantryAmmoBin clone() {
        InfantryAmmoBin clone = new InfantryAmmoBin(getUnitTonnage(), getType(), getEquipmentNum(), shotsNeeded,
              getWeaponType(), getClips(), omniPodded, campaign);
        clone.copyBaseData(this);
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
        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            while (mounted.getLinkedBy() != null) {
                mounted = mounted.getLinkedBy();
            }
            return mounted.getLocation();
        }
        return Entity.LOC_NONE;
    }

    @Override
    public double getTonnage() {
        return getWeaponType().getAmmoWeight();
    }

    @Override
    public int getFullShots() {
        return getWeaponType().getShots() * getClips();
    }

    /**
     * Changes the capacity of this bin. This is done when redistributing capacity between standard and inferno
     * munitions.
     *
     * @param clips The new capacity in number of clips
     */
    public void changeCapacity(int clips) {
        int current = getCurrentShots();
        size = clips;
        shotsNeeded = getFullShots() - current;
        // Wait until loading/unloading to change the full number of shots on the
        // Entity.
    }

    /**
     * Sets the number of shots needed in the {@code InfantryAmmoBin}.
     * <p>
     * NB: this can be negative if the capacity has changed.
     *
     * @param shots The number of shots needed.
     */
    @Override
    public void setShotsNeeded(int shots) {
        this.shotsNeeded = shots;
    }

    @Override
    public void loadBin() {
        Mounted<?> mounted = getMounted();

        // Check if we have too much ammo in the bin ...
        if (shotsNeeded < 0) {
            // ... and if so, unload the bin first.
            unload();
        }

        super.loadBin();

        if (mounted != null) {
            mounted.setOriginalShots(getFullShots());
        }
    }

    @Override
    protected int requisitionAmmo(AmmoType ammoType, int shotsNeeded) {
        Objects.requireNonNull(ammoType);

        return getCampaign().getQuartermaster().removeAmmo(ammoType, getWeaponType(), shotsNeeded);
    }

    @Override
    protected void returnAmmo(AmmoType ammoType, int shotsUnloaded) {
        Objects.requireNonNull(ammoType);

        if (shotsUnloaded > 0) {
            getCampaign().getQuartermaster().addAmmo(ammoType, getWeaponType(), shotsUnloaded);
        }
    }

    @Override
    protected Money getPricePerTon() {
        return Money.of(getWeaponType().getAmmoCost() / getWeaponType().getAmmoWeight());
    }

    @Override
    protected int getShotsPerTon() {
        return (int) Math.floor(getWeaponType().getShots() / getWeaponType().getAmmoWeight());
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        super.updateConditionFromEntity(checkForDestruction);

        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            shotsNeeded = mounted.getOriginalShots() - mounted.getBaseShotsLeft();
        }
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

    @Override
    public void fix() {
        // If we have reconfigured the distribution between standard and inferno ammo,
        // there may be extra that needs to be removed from the partner bin to make
        // room.
        // We'll do that automatically to make it simpler.
        InfantryAmmoBin partner = findPartnerBin();
        if ((partner != null) && (partner.getShotsNeeded() < 0)) {
            partner.loadBin();
        }

        loadBin();
    }

    @Override
    public MissingInfantryAmmoBin getMissingPart() {
        return new MissingInfantryAmmoBin(getUnitTonnage(), getType(), getEquipmentNum(), getWeaponType(),
              getClips(), omniPodded, campaign);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (getClass() == part.getClass())
                     && getType().equals(((InfantryAmmoBin) part).getType())
                     && Objects.equals(getWeaponType(), ((InfantryAmmoBin) part).getWeaponType())
                     && getClips() == ((InfantryAmmoBin) part).getClips();
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (shotsNeeded < 0) {
            return getType().getDesc() + ", remove " + (-getShotsNeeded());
        } else {
            return super.getDetails(includeRepairDetails);
        }
    }

    @Override
    public int getAmountAvailable() {
        return getCampaign().getQuartermaster().getAmmoAvailable(getType(), getWeaponType());
    }

    /**
     * Weapons with configurable ammo have two ammo bin parts.
     *
     * @return The other bin for the same weapon, or null if there isn't one.
     */
    public @Nullable InfantryAmmoBin findPartnerBin() {
        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            int index = -1;
            if (mounted.getLinked() != null) {
                index = unit.getEntity().getEquipmentNum(mounted.getLinked());
            } else if ((mounted.getLinkedBy() != null)
                             && (mounted.getLinkedBy().getType() instanceof AmmoType)) {
                index = unit.getEntity().getEquipmentNum(mounted.getLinkedBy());
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
        return getWeaponType().getName() + ": " + getType().getName();
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return getWeaponType().getShots() + " shots (1 clip)";
    }

    @Override
    public InfantryAmmoStorage getNewPart() {
        return new InfantryAmmoStorage(1, getType(), getFullShots(), getWeaponType(), getCampaign());
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return getWeaponType().getTechAdvancement();
    }
}
