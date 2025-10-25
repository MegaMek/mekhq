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
package mekhq.campaign;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.events.parts.PartNewEvent;
import mekhq.campaign.events.parts.PartRemovedEvent;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;

/**
 * Stores parts for a Campaign.
 */
public class Warehouse {
    private static final MMLogger LOGGER = MMLogger.create(Warehouse.class);

    private final TreeMap<Integer, Part> parts = new TreeMap<>();

    /**
     * Adds a part to the warehouse.
     *
     * @param part The part to add to the warehouse.
     *
     * @return The part added to the warehouse.
     */
    public Part addPart(Part part) {
        return addPart(part, false);
    }

    /**
     * Adds a part to the warehouse, optionally merging it with any existing spare part.
     *
     * @param part              The part to add to the warehouse.
     * @param mergeWithExisting If true and the part is spare, it may be merged with an existing spare part.
     *
     * @return The part itself or the spare part it was merged with.
     */
    public Part addPart(Part part, boolean mergeWithExisting) {
        Objects.requireNonNull(part);

        if (mergeWithExisting && part.isSpare()) {
            Part mergedPart = mergePartWithExisting(part);

            // CAW: intentional reference equality
            if (!mergedPart.equals(part)) {
                // We've merged parts, so let interested parties know we've
                // updated the merged part.
                MekHQ.triggerEvent(new PartChangedEvent(mergedPart));

                // Check if the part being added exists, and if so
                // remove it from the warehouse
                if (part.getId() > 0) {
                    removePart(part);
                }

                return mergedPart;
            }

            // ... we did not merge parts, so fall through to the
            // normal addPart logic.
        }

        // is this a new part? if so set its next ID
        if (part.getId() <= 0) {
            part.setId(parts.isEmpty() ? 1 : (parts.lastKey() + 1));
        }

        // Is this a part we've never tracked before?
        boolean isNewPart = !parts.containsKey(part.getId());

        parts.put(part.getId(), part);

        if (isNewPart) {
            MekHQ.triggerEvent(new PartNewEvent(part));
        } else {
            // Part was removed from a unit, or something similar
            MekHQ.triggerEvent(new PartChangedEvent(part));
        }

        return part;
    }

    /**
     * Gets a collection of parts within the warehouse.
     */
    public Collection<Part> getParts() {
        return parts.values();
    }

    /**
     * Gets a part from the warehouse by its ID.
     *
     * @param id The unique ID of the part.
     *
     * @return The part with the given ID, or null if no matching part exists.
     */
    public @Nullable Part getPart(int id) {
        return parts.get(id);
    }

    /**
     * Executes a function for each part in the warehouse.
     *
     * @param consumer A function to apply to each part.
     */
    public void forEachPart(Consumer<Part> consumer) {
        for (Part part : parts.values()) {
            consumer.accept(part);
        }
    }

    /**
     * Removes a part from the warehouse.
     *
     * @param part The part to remove.
     *
     * @return A value indicating whether the part was removed.
     */
    public boolean removePart(Part part) {
        Objects.requireNonNull(part);

        boolean didRemove = (parts.remove(part.getId()) != null);

        if (didRemove) {
            MekHQ.triggerEvent(new PartRemovedEvent(part));
        }

        // Clear the part's ID
        part.setId(-1);

        if (didRemove && !part.getChildParts().isEmpty()) {
            // Remove child parts as well
            List<Part> childParts = new ArrayList<>(part.getChildParts());
            for (Part childPart : childParts) {
                part.removeChildPart(childPart);

                removePart(childPart);
            }
        }

        return didRemove;
    }

    /**
     * Removes one or more parts from the warehouse.
     *
     * @param part     The part to remove.
     * @param quantity The amount of the part to remove.
     *
     * @return A value indicating whether the part was removed.
     */
    public boolean removePart(Part part, int quantity) {
        Objects.requireNonNull(part);

        // Only allow removing spare parts.
        if (!part.isSpare()) {
            return false;
        }

        if (part instanceof AmmoStorage) {
            return removeAmmo((AmmoStorage) part, quantity);
        } else if (part instanceof Armor) {
            return removeArmor((Armor) part, quantity);
        }

        if (quantity >= part.getQuantity()) {
            removePart(part);
        } else {
            if (quantity > 0) {
                part.changeQuantity(-quantity);
            }

            MekHQ.triggerEvent(new PartChangedEvent(part));
        }

        return true;
    }

    /**
     * Attempts to merge a given part with an existing spare part in stock. The merge is only possible if a compatible
     * spare part is found, and both parts have the same "brand new" state.
     *
     * <p>The merge process follows these steps:</p>
     * <ul>
     *   <li>If the part has no associated unit, no parent part, and is not reserved for replacement,
     *       the method searches for an existing spare part to merge with.</li>
     *   <li>A merge can only occur if the spare part exists and the {@code isBrandNew}
     *       property matches for both the part and the spare.</li>
     *   <li>Specific handling is applied based on the type of the part:
     *     <ul>
     *       <li>If the part is of type {@code Armor}, the quantities are added together.</li>
     *       <li>If the part is of type {@code AmmoStorage}, the shot counts are updated.</li>
     *       <li>For other part types, the quantities are incremented.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param part The part to attempt to merge with an existing spare part. Cannot be {@code null}.
     *
     * @return The original part if no merge occurs, or the existing spare part if the parts are merged.
     *
     * @throws NullPointerException If the {@code part} parameter is {@code null}.
     */
    private Part mergePartWithExisting(Part part) {
        Objects.requireNonNull(part);

        // Check if the part has no unit, no parent part, and is not reserved for replacement
        if ((null == part.getUnit()) && !part.hasParentPart() && !part.isReservedForReplacement()) {
            Part spare = checkForExistingSparePart(part, true);

            // Ensure a matching spare exists and both parts share the same isBrandNew state
            if (spare != null && part.isBrandNew() == spare.isBrandNew()) {
                // Handle specific part types
                if (part instanceof Armor && spare instanceof Armor) {
                    ((Armor) spare).setAmount(((Armor) spare).getAmount() + ((Armor) part).getAmount());
                    return spare;
                } else if (part instanceof AmmoStorage && spare instanceof AmmoStorage) {
                    ((AmmoStorage) spare).changeShots(((AmmoStorage) part).getShots());
                    return spare;
                } else {
                    // Handle generic parts by incrementing the quantity
                    spare.changeQuantity(part.getQuantity());
                    return spare;
                }
            }
        }

        return part;
    }

    /**
     * Checks for an existing spare part.
     *
     * @param part The part to search for in the warehouse.
     *
     * @return The matching spare part or null if none were found.
     */
    public @Nullable Part checkForExistingSparePart(Part part) {
        if (part == null) {
            LOGGER.error(new NullPointerException("Part is null"), "checkForExistingSparePart(Part): Part is null");
            return null;
        }

        return findSparePart(spare ->
                                   (spare.getId() != part.getId())
                                         && part.isSamePartTypeAndStatus(spare));
    }

    /**
     * Checks for an existing spare part in inventory that matches the given {@code part}.
     *
     * <p>In addition to comparing type and status, this method can optionally consider whether both parts are
     * equally brand new based on the {@code includeNewnessCheck} parameter.</p>
     *
     * <ul>
     *     <li>If {@code includeNewnessCheck} is {@code true}, this method defers to
     *     {@link #checkForExistingSparePart(Part)} for a match based on type and status.</li>
     *     <li>If {@code part} is {@code null}, an error is logged and {@code null} is returned.</li>
     *     <li>Otherwise, returns a matching spare part or {@code null} if none is found.</li>
     * </ul>
     *
     * @param part                the part to find a match for; may not be {@code null}
     * @param includeNewnessCheck whether to require matching "brand new" status as part of the comparison
     *
     * @return an existing spare part matching the given part and criteria, or {@code null} if no match is found
     *
     * @author Illiani
     * @since 0.50.06
     */
    public @Nullable Part checkForExistingSparePart(Part part, boolean includeNewnessCheck) {
        if (part == null) {
            LOGGER.error(new NullPointerException("Part is null"),
                  "checkForExistingSparePart(Part, boolean): Part is null");
            return null;
        }

        if (!includeNewnessCheck) {
            return checkForExistingSparePart(part);
        }

        return findSparePart(spare -> (spare.getId() != part.getId()) &&
                                            part.isSamePartTypeAndStatus(spare) &&
                                            (part.isBrandNew() == spare.isBrandNew()));
    }

    /**
     * Gets a list of spare parts in the warehouse.
     *
     * @return A list of spare parts in the warehouse.
     */
    public List<Part> getSpareParts() {
        return getParts().stream()
                     .filter(Part::isSpare)
                     .collect(Collectors.toList());
    }

    public int getSparePartsCount(Part targetPart) {
        int count = 0;
        for (Part warehousePart : getParts()) {
            if (warehousePart.isSpare() && warehousePart.isSamePartType(targetPart)) {
                count += getPartQuantity(warehousePart);
            }
        }

        return count;
    }

    //TODO: getPartQuantity should be an overloaded method in Part.java, I'm just getting it out of campaign
    public int getPartQuantity(Part p) {
        if (p instanceof Armor) {
            return ((Armor) p).getAmount();
        }
        if (p instanceof AmmoStorage) {
            return ((AmmoStorage) p).getShots();
        }
        return (p.getUnit() != null) ? 1 : p.getQuantity();
    }

    /**
     * Executes a method for each spare part in the warehouse.
     *
     * @param consumer The method to apply to each spare part in the warehouse.
     */
    public void forEachSparePart(Consumer<Part> consumer) {
        for (Part part : getParts()) {
            if (part.isSpare()) {
                consumer.accept(part);
            }
        }
    }

    /**
     * Finds the first spare part matching a predicate.
     *
     * @param predicate The predicate to use when searching for a suitable spare part.
     *
     * @return A matching spare {@link Part} or {@code null} if no suitable match was found.
     */
    public @Nullable Part findSparePart(Predicate<Part> predicate) {
        for (Part part : getParts()) {
            if (part.isSpare() && predicate.test(part)) {
                return part;
            }
        }
        return null;
    }

    /**
     * Streams the spare parts in the campaign.
     *
     * @return A stream of spare parts in the campaign.
     */
    public Stream<Part> streamSpareParts() {
        return getParts().stream().filter(Part::isSpare);
    }

    /**
     * Adds ammo to the warehouse.
     *
     * @param ammo  Ammo in the warehouse.
     * @param shots The amount of ammo to add to the warehouse.
     */
    public void addAmmo(AmmoStorage ammo, int shots) {
        Objects.requireNonNull(ammo);

        ammo.changeShots(shots);
        MekHQ.triggerEvent(new PartChangedEvent(ammo));
    }

    /**
     * Removes ammo from the warehouse.
     *
     * @param ammo  Ammo in the warehouse.
     * @param shots The amount of ammo to remove from the warehouse.
     */
    public boolean removeAmmo(AmmoStorage ammo, int shots) {
        Objects.requireNonNull(ammo);

        if (shots >= ammo.getShots()) {
            removePart(ammo);
        } else {
            ammo.changeShots(-shots);
            MekHQ.triggerEvent(new PartChangedEvent(ammo));
        }

        return true;
    }

    public boolean removeArmor(Armor armor, int points) {
        Objects.requireNonNull(armor);

        if (points >= armor.getAmount()) {
            removePart(armor);
        } else {
            armor.changeAmountAvailable(-points);
            MekHQ.triggerEvent(new PartChangedEvent(armor));
        }

        return true;
    }

    public void writeToXML(final PrintWriter pw, final int indent, final String tag) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, tag);
        forEachPart(part -> part.writeToXML(pw, indent + 1));
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, tag);
    }
}
