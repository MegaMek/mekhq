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
package mekhq.campaign.force;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.universe.Faction;

/**
 * Force-scoped options for an {@link AbstractForce}, modeled loosely on {@link CampaignOptions}.
 *
 * <p>This is deliberately thin for now: most settings still live on {@link CampaignOptions} and are reached through the
 * {@link #getCampaignOptions() wrapped} instance (pass-through accessors are added here as force logic needs them). What
 * it <em>does</em> own outright is the force's {@link Faction}, stored as a typed {@link ForceOption}. Over time,
 * force-specific settings will migrate off {@link CampaignOptions} and become real {@link ForceOption}s here.</p>
 */
public class ForceOptions {
    /** Identifier for the {@link Faction} option. */
    public static final String FACTION = "faction";

    private CampaignOptions campaignOptions;
    private final ForceOption<Faction> faction;

    /**
     * @param campaignOptions the campaign options this force passes non-force settings through to
     * @param initialFaction  the force's starting faction
     */
    public ForceOptions(CampaignOptions campaignOptions, Faction initialFaction) {
        this.campaignOptions = campaignOptions;
        this.faction = new ForceOption<>(FACTION, initialFaction);
    }

    public CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }

    /** Keeps the pass-through target in sync when the campaign swaps its options wholesale. */
    public void setCampaignOptions(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;
    }

    public Faction getFaction() {
        return faction.getValue();
    }

    public void setFaction(Faction faction) {
        this.faction.setValue(faction);
    }

    // region CampaignOptions pass-throughs

    public boolean isFactionIntroDate() {
        return campaignOptions.isFactionIntroDate();
    }

    // endregion CampaignOptions pass-throughs
}
