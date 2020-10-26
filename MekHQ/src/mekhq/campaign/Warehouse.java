/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartNewEvent;
import mekhq.campaign.event.PartRemovedEvent;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;

/**
 * Stores parts for a Campaign.
 */
public class Warehouse {
    private final TreeMap<Integer, Part> parts = new TreeMap<>();

    /**
     * Adds a part to the warehouse.
     * @param part The part to add to the warehouse.
     * @return The part added to the warehouse.
     */
    public Part addPart(Part part) {
        return addPart(part, false);
    }

    /**
     * Adds a part to the warehouse, optionally merging it with
     * any existing spare part.
     * @param part The part to add to the warehouse.
     * @param mergeWithExisting If true and the part is spare, it may
     *                          be merged with an existing spare part.
     * @return The part itself or the spare part it was merged with.
     */
    public Part addPart(Part part, boolean mergeWithExisting) {
        Objects.requireNonNull(part);

        if (mergeWithExisting && part.isSpare()) {
            Part mergedPart = mergePartWithExisting(part);

            // CAW: intentional reference equality
            if (mergedPart != part) {
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

            // ...we did not merge parts, so fall through to the
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
     * @param id The unique ID of the part.
     * @return The part with the given ID, or null if no matching part exists.
     */
    public @Nullable Part getPart(int id) {
        return parts.get(id);
    }

    /**
     * Executes a function for each part in the warehouse.
     * @param consumer A function to apply to each part.
     */
    public void forEachPart(Consumer<Part> consumer) {
        for (Part part : parts.values()) {
            consumer.accept(part);
        }
    }

    /**
     * Removes a part from the warehouse.
     * @param part The part to remove.
     * @return A value indicating whether or not the part was removed.
     */
	public boolean removePart(Part part) {
        Objects.requireNonNull(part);

        boolean didRemove = (parts.remove(part.getId()) != null);

        if (didRemove) {
            MekHQ.triggerEvent(new PartRemovedEvent(part));
        }

        // Remove child parts as well
        for (Part childPart : part.getChildParts()) {
            removePart(childPart);
        }

        return didRemove;
	}

    /**
     * Merges a part with an existing part, if possible.
     * @param part The part to try and merge with an existing part.
     * @return The part itself, or the spare part the part was merged with.
     */
    private Part mergePartWithExisting(Part part) {
        Objects.requireNonNull(part);

        if ((null == part.getUnit()) && !part.hasParentPart()
                && !part.isReservedForRefit() && !part.isReservedForReplacement()) {
            Part spare = checkForExistingSparePart(part);
            if (null != spare) {
                if (part instanceof Armor) {
                    if (spare instanceof Armor) {
                        ((Armor) spare).setAmount(((Armor) spare).getAmount()
                                + ((Armor) part).getAmount());
                        return spare;
                    }
                } else if (part instanceof AmmoStorage) {
                    if (spare instanceof AmmoStorage) {
                        ((AmmoStorage) spare).changeShots(((AmmoStorage) part)
                                .getShots());
                        return spare;
                    }
                } else {
                    spare.incrementQuantity();
                    return spare;
                }
            }
        }

        return part;
    }

    /**
     * Checks for an existing spare part.
     * @param part The part to search for in the warehouse.
     * @return The matching spare part or null if none were found.
     */
    public @Nullable Part checkForExistingSparePart(Part part) {
        for (Part spare : parts.values()) {
            if (!spare.isSpare() || spare.getId() == part.getId()) {
                continue;
            }
            if (part.isSamePartTypeAndStatus(spare)) {
                return spare;
            }
        }
        return null;
    }

	public void writeToXml(PrintWriter pw1, int indent, String tag) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, tag);

        forEachPart(part -> {
            part.writeToXml(pw1, indent + 1);
        });

        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, tag);
	}
}
