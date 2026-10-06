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
package mekhq.campaign.universe.commandGeneration.ratgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.universe.Faction;

/**
 * Awards a rolled Clan force its Bloodnames in the draft, so the preview shows the warriors who will carry
 * one and the build keeps exactly those.
 *
 * <p>The share is {@link BloodnameQuota}'s, from the roll's experience level. In the draft there are no people
 * yet, only the crews the engine rolled, so the Bloodnames go to the crews with the best gunnery and piloting,
 * chance breaking ties. Each is written onto the crew descriptor, whose name gains the Bloodname so the preview
 * shows it; the build reads the Bloodname back off the descriptor and its own quota counts it as held.</p>
 */
public final class DraftBloodnames {

    private static final MMLogger LOGGER = MMLogger.create(DraftBloodnames.class);
    private static final String LOG_TAG = "[CompanyGen][Bloodname][Draft]";

    private DraftBloodnames() {
    }

    /**
     * Awards the roll its Bloodnames.
     *
     * @param campaign the campaign, for the year when the roll names none
     * @param roll     the force just rolled
     * @param faction  the command's faction; nothing happens unless it is a Clan
     *
     * @return how many crews were given a Bloodname
     */
    public static int award(Campaign campaign, ForceDescriptor roll, @Nullable Faction faction) {
        if ((roll == null) || (faction == null) || !faction.isClan()) {
            return 0;
        }
        List<CrewDescriptor> crews = new ArrayList<>();
        List<CrewDescriptor> unnamed = new ArrayList<>();
        collectCrews(roll, crews);
        for (CrewDescriptor crew : crews) {
            if (!hasBloodname(crew)) {
                unnamed.add(crew);
            }
        }
        if (crews.isEmpty()) {
            LOGGER.info("{} the roll has no crews; nothing to award", LOG_TAG);
            return 0;
        }
        double share = BloodnameQuota.share(roll.getExperience());
        int quota = BloodnameQuota.quota(crews.size(), share);
        int toAward = Math.max(0, quota - (crews.size() - unnamed.size()));
        int year = (roll.getYear() == null) ? campaign.getGameYear() : roll.getYear();
        int awarded = awardTo(unnamed, toAward, crew -> pickBloodname(faction, crew, year));
        LOGGER.info("{} share {}% for the roll's calibre: {} of {} crew(s) may hold a Bloodname; {} awarded in the"
                    + " draft", LOG_TAG, Math.round(share * 100), quota, crews.size(), awarded);
        return awarded;
    }

    /**
     * Gives the best of the crews a Bloodname each: lowest gunnery and piloting first, chance breaking ties.
     *
     * @param crews   crews without a Bloodname
     * @param toAward how many to name
     * @param namer   supplies the Bloodname for a crew, or {@code null} when none can be found for it
     *
     * @return how many were named
     */
    static int awardTo(List<CrewDescriptor> crews, int toAward, Function<CrewDescriptor, String> namer) {
        List<CrewDescriptor> ranked = new ArrayList<>(crews);
        Collections.shuffle(ranked);
        ranked.sort(Comparator.comparingInt(crew -> crew.getGunnery() + crew.getPiloting()));
        int awarded = 0;
        for (CrewDescriptor crew : ranked) {
            if (awarded >= toAward) {
                break;
            }
            String bloodname = namer.apply(crew);
            if ((bloodname == null) || bloodname.isBlank()) {
                continue;
            }
            crew.setBloodname(bloodname);
            crew.setName(crew.getName() + " " + bloodname);
            awarded++;
        }
        return awarded;
    }

    private static @Nullable String pickBloodname(Faction faction, CrewDescriptor crew, int year) {
        Bloodname bloodname = Bloodname.randomBloodname(faction.getShortName(), phenotypeOf(crew), year);
        return (bloodname == null) ? null : bloodname.getName();
    }

    /** The Clan phenotype a crew's unit calls for, from the unit's type. */
    static Phenotype phenotypeOf(CrewDescriptor crew) {
        Entity entity = crew.getAssignment().getEntity();
        if (entity == null) {
            return Phenotype.MEKWARRIOR;
        }
        return switch (entity.getUnitType()) {
            case UnitType.BATTLE_ARMOR -> Phenotype.ELEMENTAL;
            case UnitType.AEROSPACE_FIGHTER, UnitType.CONV_FIGHTER -> Phenotype.AEROSPACE;
            case UnitType.TANK, UnitType.VTOL -> Phenotype.VEHICLE;
            case UnitType.NAVAL -> Phenotype.NAVAL;
            case UnitType.PROTOMEK -> Phenotype.PROTOMEK;
            default -> Phenotype.MEKWARRIOR;
        };
    }

    static boolean hasBloodname(CrewDescriptor crew) {
        String bloodname = crew.getBloodname();
        return (bloodname != null) && !bloodname.isBlank();
    }

    /** Every crew of a unit in the roll, in tree order. */
    private static void collectCrews(ForceDescriptor node, List<CrewDescriptor> into) {
        boolean isUnit = (node.getEntity() != null) && (node.getCo() != null);
        if (isUnit) {
            into.add(node.getCo());
        }
        for (ForceDescriptor child : node.getSubForces()) {
            collectCrews(child, into);
        }
        for (ForceDescriptor child : node.getAttached()) {
            collectCrews(child, into);
        }
    }
}
