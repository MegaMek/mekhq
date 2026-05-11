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

import megamek.client.generator.RandomNameGenerator;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.loaders.MekSummaryCache;
import megamek.logging.MMLogger;

/**
 * Idempotent initialization of the MegaMek Force Generator engine for use from the MekHQ side.
 *
 * <p>The engine has several pieces of static state that need to be ready before
 * {@link Ruleset#processRoot(megamek.client.ratgenerator.ForceDescriptor, megamek.client.ratgenerator.Ruleset.ProgressListener)}
 * will produce useful output: the RAT tables for the requested year, the unit-summary cache, the parsed
 * faction rulesets, and the random name generator (used by {@code Ruleset.processRoot} when assigning
 * commander names). MegaMek's tab-6 dialog wires this up incidentally because everything is loaded as
 * the lobby starts; calling the engine from MekHQ has to do it explicitly.</p>
 *
 * <p>Calls are safe to repeat. Each piece of state checks its own initialization flag.</p>
 */
public final class RulesetEngineBootstrap {

    private static final MMLogger LOGGER = MMLogger.create(RulesetEngineBootstrap.class);

    private RulesetEngineBootstrap() {
        // utility class
    }

    /**
     * Ensures every piece of MegaMek-side state the Force Generator engine reads is loaded for the given
     * generation year. Synchronous. Logs at info level on first-time initialization of each component.
     *
     * @param year the year to load RAT tables for; passed straight to {@link RATGenerator#loadYear(int)}
     */
    public static synchronized void ensureLoaded(int year) {
        LOGGER.info("[CompanyGen] RulesetEngineBootstrap.ensureLoaded(year={}) START", year);

        // RATGenerator caches per-year availability internally; loadYear is idempotent.
        long t0 = System.currentTimeMillis();
        RATGenerator ratGenerator = RATGenerator.getInstance();
        ratGenerator.loadYear(year);
        LOGGER.info("[CompanyGen]   RATGenerator.loadYear({}) -> {}ms", year, System.currentTimeMillis() - t0);

        // MekSummaryCache scans .mtf/.blk files on first access and reuses the result.
        t0 = System.currentTimeMillis();
        MekSummaryCache cache = MekSummaryCache.getInstance();
        LOGGER.info("[CompanyGen]   MekSummaryCache.getInstance() -> {}ms (initialized={})",
              System.currentTimeMillis() - t0, cache != null);

        // Ruleset.loadData is idempotent (guards on its initialized flag).
        if (!Ruleset.isInitialized()) {
            LOGGER.info("[CompanyGen]   Loading Force Generator rulesets from data/forcegenerator/faction_rules/ ...");
            t0 = System.currentTimeMillis();
            Ruleset.loadData();
            LOGGER.info("[CompanyGen]   Ruleset.loadData() -> {}ms", System.currentTimeMillis() - t0);
        } else {
            LOGGER.info("[CompanyGen]   Ruleset already initialized; skipping loadData()");
        }

        // RandomNameGenerator is also lazily initialized; calling getInstance is enough.
        RandomNameGenerator.getInstance();
        LOGGER.info("[CompanyGen]   RandomNameGenerator.getInstance() ready");

        LOGGER.info("[CompanyGen] RulesetEngineBootstrap.ensureLoaded(year={}) DONE", year);
    }
}
