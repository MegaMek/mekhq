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
package mekhq.campaign.universe.companyGeneration.ratgen;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

/**
 * Single entry point for the ratgen-driven starting-force pipeline.
 *
 * <p>Composes the helpers in this package — {@link RulesetEngineBootstrap},
 * {@link ForceDescriptorWalker}, {@link MultiCrewAssembler}, {@link CrewDescriptorAdapter},
 * {@link RankAssigner} — into the 8-step pipeline described in
 * {@code docs/plans/force-generator-company-generation.md} (megamek repo). Phase 1 lands steps 1-7
 * (bootstrap → buildDescriptor → processRoot → walk → personnel → units → tree) but defers the
 * polish stage (parts, spares, finances, contracts, naming) until the static helpers are extracted
 * from {@link mekhq.campaign.universe.generators.companyGenerators.AbstractCompanyGenerator}.</p>
 *
 * <p>This class is not yet wired into the public dialog. It is reachable only from the dev-gated
 * {@code RULESET_BASED} branch and from unit tests.</p>
 */
public final class CompanyGenerator {

    private static final MMLogger LOGGER = MMLogger.create(CompanyGenerator.class);

    private CompanyGenerator() {
        // utility entry point
    }

    /**
     * Runs the ratgen pipeline end-to-end and applies the result to the given campaign. Returns the
     * descriptor tree the engine built so callers can inspect the structure (count leaves, log the
     * hierarchy, drive verification checks).
     *
     * @param campaign the target {@link Campaign}; receives the generated Formations, Units, and Persons
     * @param options  the user's {@link CompanyGenerationOptions}; the
     *                 {@link CompanyGenerationOptions#getForceDescriptorSnapshot()} block supplies the
     *                 ratgen inputs
     * @return the generated {@link ForceDescriptor} tree, or {@code null} if generation failed at the
     *         engine layer
     */
    public static ForceDescriptor generate(Campaign campaign, CompanyGenerationOptions options) {
        ForceDescriptorSnapshot snap = options.getForceDescriptorSnapshot();

        // 1. Bootstrap MegaMek-side state for the target year.
        RulesetEngineBootstrap.ensureLoaded(snap.getYear());

        // 2. Build a fresh ForceDescriptor from the snapshot. The Force Generator panel does this
        // server-side via buildForceDescriptor(); we mirror its inputs here so we never depend on the
        // panel being instantiated.
        ForceDescriptor fd = new ForceDescriptor();
        fd.setTopLevel(true);
        fd.setFaction(snap.getFaction());
        fd.setYear(snap.getYear());
        if (snap.getEchelon() != null) {
            fd.setEchelon(snap.getEchelon());
        }
        if (snap.getUnitType() != null) {
            fd.setUnitType(snap.getUnitType());
        }
        if (snap.getRating() != null) {
            fd.setRating(snap.getRating());
        }
        if (snap.getExperience() != null) {
            fd.setExperience(snap.getExperience());
        }
        if (snap.getWeightClass() != null) {
            fd.setWeightClass(snap.getWeightClass());
        }
        fd.setAugmented(snap.isAugmented());
        if (snap.getSizeMod() != null) {
            fd.setSizeMod(snap.getSizeMod());
        }
        fd.setDropshipPct(snap.getDropshipPct());

        // 3. Run the engine. Null listener is safe per Ruleset.processRoot's internal guards.
        Ruleset.findRuleset(fd).processRoot(fd, null);

        // 4-7. Walk the resulting tree; for each leaf, materialize a Unit, attach a crew, and place
        // the unit under the current Formation.
        Formation root = campaign.getFormations();
        int[] leafCount = { 0 };
        ForceDescriptorWalker.walk(fd, campaign, root, (leaf, parent) -> {
            Entity entity = leaf.getEntity();
            if (entity == null) {
                LOGGER.warn("Leaf descriptor produced no entity: {}", leaf.getName());
                return;
            }
            Unit unit = campaign.addNewUnit(entity, false, 0);
            if (unit == null) {
                LOGGER.warn("Failed to add unit for entity {}", entity.getDisplayName());
                return;
            }
            java.util.List<Person> crew = MultiCrewAssembler.assemble(unit, leaf.getCo(), campaign,
                  /* overrideName */ true);
            if (!crew.isEmpty()) {
                RankAssigner.apply(leaf.getCo(), crew.get(0));
            }
            parent.addUnit(unit.getId());
            leafCount[0]++;
        });

        LOGGER.info("Ratgen company generation produced {} leaf units", leafCount[0]);

        // 8. Polish stage (parts, spares, finances, contracts, naming) — wired into existing
        // AbstractCompanyGenerator helpers in a follow-up commit. Phase 1 stops here so the
        // tree-walk integration can be verified end-to-end against a Mek-only scenario.

        return fd;
    }
}
