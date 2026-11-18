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
package mekhq.campaign.parts;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import jakarta.annotation.Nullable;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;

/**
 * The other units waiting on a part like one being salvaged. Stripping a unit is often cannibalising it to repair
 * another, so a salvage task says how many units need what it would take ({@code Needed by 2 units}) and names them
 * in its tooltip.
 *
 * <p>A unit needs the part when one of its missing parts would accept it as a replacement and is waiting to be
 * repaired: the unit is not itself being stripped, mothballed or refitted, and has no spare set aside for that gap
 * already.</p>
 *
 * @param needs each unit that needs the part and how many, sorted by unit name; empty when nothing does
 */
public record PartDemand(List<UnitNeed> needs) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    /** Nothing needs the part. */
    public static final PartDemand NONE = new PartDemand(List.of());

    /**
     * One unit that needs the part.
     *
     * @param unit  the unit missing the part
     * @param count how many of its missing parts would take it
     */
    public record UnitNeed(Unit unit, int count) {}

    /**
     * Finds the missing parts among {@code units} that are waiting to be repaired. Gathering them once lets every
     * salvage task in a list be matched against the same gaps, rather than each going through every unit's parts.
     *
     * @param units the units that draw on one stock, such as those in one hangar
     *
     * @return the gaps waiting for a part, each still on its unit
     */
    public static List<MissingPart> findGaps(Collection<Unit> units) {
        List<MissingPart> gaps = new ArrayList<>();
        for (Unit unit : units) {
            if (isWaitingForRepairs(unit)) {
                for (Part part : unit.getParts()) {
                    if ((part instanceof MissingPart missingPart)
                              && missingPart.needsFixing()
                              && !missingPart.hasReplacementPart()) {
                        gaps.add(missingPart);
                    }
                }
            }
        }
        return gaps;
    }

    // A unit being stripped wants nothing, a mothballed one is not worked on, and a refit brings its own parts
    private static boolean isWaitingForRepairs(Unit unit) {
        return (unit != null) && !unit.isSalvage() && !unit.isMothballed() && !unit.isRefitting();
    }

    /**
     * Finds the units with a gap that {@code salvaged} could fill. The unit it is being salvaged from does not count,
     * and a part damaged beyond repair can fill no one's gap.
     *
     * @param salvaged the part being salvaged
     * @param gaps     the gaps waiting for a part, from {@link #findGaps(Collection)}
     *
     * @return the units that need it, or {@link #NONE}
     */
    public static PartDemand findFor(Part salvaged, List<MissingPart> gaps) {
        if (salvaged.isDamagedBeyondRepair()) {
            return NONE;
        }

        Map<Unit, Integer> countByUnit = new LinkedHashMap<>();
        for (MissingPart gap : gaps) {
            Unit unit = gap.getUnit();
            if ((unit != null) && (unit != salvaged.getUnit()) && gap.isAcceptableReplacement(salvaged, false)) {
                countByUnit.merge(unit, 1, Integer::sum);
            }
        }

        if (countByUnit.isEmpty()) {
            return NONE;
        }
        List<UnitNeed> needs = new ArrayList<>();
        countByUnit.forEach((unit, count) -> needs.add(new UnitNeed(unit, count)));
        needs.sort(Comparator.comparing(need -> need.unit().getName()));
        return new PartDemand(List.copyOf(needs));
    }

    /**
     * @return {@code true} if any unit needs the part
     */
    public boolean isNeeded() {
        return !needs.isEmpty();
    }

    /**
     * Counts the units that need the part, short enough for the task's own line: {@code Needed by 2 units}. Unit names
     * can be long, so they are left to {@link #describeUnits()}.
     *
     * @return the count, or an empty string when nothing needs the part
     */
    public String describe() {
        if (needs.isEmpty()) {
            return "";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "PartDemand.count", needs.size());
    }

    /**
     * Names each unit that needs the part and how many, one per line, for the task's tooltip: {@code Wolverine WVR-6R
     * #3 needs one}.
     *
     * @return the units, as lines joined by {@code <br>}, or {@code null} when nothing needs the part
     */
    public @Nullable String describeUnits() {
        if (needs.isEmpty()) {
            return null;
        }
        StringJoiner lines = new StringJoiner("<br>");
        for (UnitNeed need : needs) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "PartDemand.unit", need.unit().getName(), need.count()));
        }
        return lines.toString();
    }
}
