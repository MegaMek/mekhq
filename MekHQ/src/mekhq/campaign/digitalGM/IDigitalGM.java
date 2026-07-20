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
package mekhq.campaign.digitalGM;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.NewDayEvent;

/**
 * The outward-facing contract for a "digital GM": a self-contained campaign-management engine that drives the automated
 * generation and resolution of missions on the strategic layer.
 *
 * <p>Historically MekHQ had exactly one such engine, hard-wired as
 * {@link mekhq.campaign.digitalGM.stratCon.StratConRulesManager StratConRulesManager}, which branched internally on the
 * {@code isUseStratConMaplessMode} / {@code isUseStratConSinglesMode} campaign options. This interface abstracts that
 * engine so multiple digital GMs can coexist &mdash; the existing StratCon play types (Normal, Mapless, Singles) become
 * concrete implementations, and entirely new GMs can be added alongside them.</p>
 *
 * <p>An implementation is expected to be a long-lived singleton registered on the MekHQ event bus (see
 * {@link #startup()} / {@link #shutdown()}). The behaviour that varies between GMs is expressed through the strategy
 * interfaces alongside this one (for example {@link IForceDeploymentStrategy}); common wiring lives in
 * {@link AbstractDigitalGM}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IDigitalGM {

    /**
     * @return a stable, human-readable identifier for this digital GM (used for logging and, eventually, selection).
     */
    String getName();

    /**
     * Indicates whether this digital GM should act for the supplied campaign, based on its campaign options. The engine
     * consults this before running any per-day work so that a disabled GM is inert.
     *
     * @param campaignOptions the options that decide activation
     *
     * @return {@code true} if this GM governs the given campaign
     */
    boolean isEnabled(CampaignOptions campaignOptions);

    /**
     * Registers this GM to begin receiving lifecycle events (typically the MekHQ event bus).
     */
    void startup();

    /**
     * Unregisters this GM so it stops receiving lifecycle events.
     */
    void shutdown();

    /**
     * The core daily lifecycle hook. Invoked once per campaign day for the active campaign; implementations advance the
     * strategic state &mdash; expiring stale scenarios, generating new ones, resolving returns, and so on.
     *
     * @param event the new-day event carrying the campaign to process
     */
    void handleNewDay(NewDayEvent event);
}
