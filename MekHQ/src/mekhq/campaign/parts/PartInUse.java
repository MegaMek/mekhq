/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import megamek.common.enums.TechBase;
import megamek.common.equipment.AmmoType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingBattleArmorSuit;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;

public class PartInUse {
    private String description;
    private final IAcquisitionWork partToBuy;
    private int useCount;
    private int storeCount;
    private double tonnagePerItem;
    private int transferCount;
    private int plannedCount;
    private Money cost = Money.zero();
    private final List<Part> spares = new ArrayList<>();
    private double requestedStock;
    private boolean isBundle;

    private void appendDetails(StringBuilder sb, Part part) {
        String details = part.getDetails(false);
        if (!details.isEmpty()) {
            sb.append(" (").append(details).append(")");
        }
    }

    public PartInUse(Part part) {
        StringBuilder sb = new StringBuilder(part.getName());
        Unit u = part.getUnit();
        if (!(part instanceof MissingBattleArmorSuit)) {
            part.setUnit(null);
        }
        if (!(part instanceof Armor) && !(part instanceof AmmoStorage)) {
            appendDetails(sb, part);
        }
        part.setUnit(u);
        this.description = sb.toString();
        this.partToBuy = part.getAcquisitionWork();
        this.tonnagePerItem = part.getTonnage();
        this.isBundle = false;
        // AmmoBin are special: They aren't buyable (yet?), but instead buy you the ammo inside
        // We redo the description based on that
        if (partToBuy instanceof AmmoStorage) {
            sb.setLength(0);
            sb.append(((AmmoStorage) partToBuy).getName());
            appendDetails(sb, (Part) ((AmmoStorage) partToBuy).getAcquisitionWork());
            this.description = sb.toString();
            AmmoType ammoType = ((AmmoStorage) partToBuy).getType();
            if (ammoType.getKgPerShot() > 0) {
                this.tonnagePerItem = ammoType.getKgPerShot() / 1000.0;
            } else {
                this.tonnagePerItem = 1.0 / ammoType.getShots();
            }
        }
        if (part instanceof Armor) {
            // Armor needs different tonnage values
            this.tonnagePerItem = 1.0 / ((Armor) part).getArmorPointsPerTon();
            this.isBundle = true;
        }
        if (null != partToBuy) {
            this.cost = partToBuy.getBuyCost();
            String descString = partToBuy.getAcquisitionName();
            if (!(descString.contains("(") && descString.contains(")")) && !(part instanceof EnginePart)) {
                descString = descString.split(",")[0];
                descString = descString.split("<")[0];
            }
            if (descString.equals("Turret")) {
                descString += " " + part.getTonnage() + " tons";
            }
            this.description = descString;
        }
        this.requestedStock = 0;
    }

    public String getDescription() {
        return description;
    }

    public TechBase getTechBase() {
        return partToBuy.getTechBase();
    }

    /**
     * Returns a list of "spares" for this part in the warehouse that can be sold
     *
     * @return a list of spare Part references in the Warehouse sorted by quality in ascending order
     */
    public List<Part> getSpares() {
        return spares.stream()
                     .sorted(Comparator.comparing(Part::getQuality))
                     .collect(Collectors.toList());
    }

    /**
     * Returns an Optional containing the lowest quality spare part in the warehouse, if one exists.
     *
     * @return The lowest quality spare part, if available
     */
    public Optional<Part> getSpare() {
        return getSpares().stream().findFirst();
    }

    public void addSpare(Part part) {
        spares.add(part);
    }

    public IAcquisitionWork getPartToBuy() {
        return partToBuy;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }

    public void incUseCount() {
        ++useCount;
    }

    public int getStoreCount() {
        return storeCount;
    }

    public double getStoreTonnage() {
        return storeCount * tonnagePerItem;
    }

    public void setStoreCount(int storeCount) {
        this.storeCount = storeCount;
    }

    public void incStoreCount() {
        ++storeCount;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public void incTransferCount() {
        ++transferCount;
    }

    public void setTransferCount(int transferCount) {
        this.transferCount = transferCount;
    }

    public int getPlannedCount() {
        return plannedCount;
    }

    public void setPlannedCount(int plannedCount) {
        this.plannedCount = plannedCount;
    }

    public void incPlannedCount() {
        ++plannedCount;
    }

    public Money getCost() {
        return cost;
    }

    public double getRequestedStock() {
        return requestedStock;
    }

    public void setRequestedStock(double requestedStock) {
        this.requestedStock = requestedStock;
    }

    public boolean getIsBundle() {
        return isBundle;
    }

    public void setIsBundle(boolean isBundle) {
        this.isBundle = isBundle;
    }

    public double getTonnagePerItem() {
        return tonnagePerItem;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(description);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final PartInUse other = (PartInUse) obj;
        return Objects.equals(description, other.description) &&
                     Objects.equals(getTechBase(), other.getTechBase());
    }
}
