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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mekhq.campaign.camOpsReputation.ForceReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * The human player's active force: the single {@link AbstractForce} a {@link mekhq.campaign.Campaign} is played
 * through.
 *
 * <p>For now a player force owns exactly one {@link Detachment} — hence it implements
 * {@link SingleDetachmentForce}, which supplies the located-resource passthroughs (hangar, warehouse, personnel, …).
 * Multiple detachments per force is a later refactor, at which point a multi-detachment variant will simply not
 * implement {@code SingleDetachmentForce} and the compiler will flag every single-detachment assumption.</p>
 */
public class PlayerForce extends AbstractForce implements SingleDetachmentForce {

    private final Detachment forceDetachment = new Detachment();

    /**
     * @param faction              the force's starting faction
     * @param techFaction          the resolved MegaMek tech faction
     * @param rankSystem           the force's rank system
     * @param finances             the force's finances ledger
     * @param reputationController the force's reputation controller
     * @param factionStandings     the force's standings with the wider universe
     * @param campaignOptions      the campaign options the force's {@link ForceOptions} passes through to
     */
    public PlayerForce(Faction faction, megamek.common.enums.Faction techFaction, RankSystem rankSystem,
          Finances finances, ForceReputationController reputationController, FactionStandings factionStandings,
          CampaignOptions campaignOptions) {
        super(new ForceOptions(campaignOptions, faction), techFaction, rankSystem, finances, reputationController,
              factionStandings);
    }

    @Override
    public Detachment getForceDetachment() {
        return forceDetachment;
    }

    @Override
    public Collection<Detachment> getDetachments() {
        return new ArrayList<>(List.of(forceDetachment));
    }
}
