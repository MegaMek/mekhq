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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ContractMarket;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.contract.io.ContractXmlCodec;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.reputation.camOpsReputation.ForceReputationController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

    public static final String CONTRACTS_TAG = "contracts";

    private final Detachment forceDetachment = new Detachment();

    /** The force's contracts, keyed by {@link AbstractContract#getId()} and iterated in insertion order. */
    private final Map<UUID, AbstractContract> contractHistory = new LinkedHashMap<>();

    /** The force's contract market: the currently available offers, split by {@code ContractSearchType}. */
    private final ContractMarket contractMarket = new ContractMarket();

    /**
     * @param faction                 the force's starting faction
     * @param techFaction             the resolved MegaMek tech faction
     * @param rankSystem              the force's rank system
     * @param finances                the force's finances ledger
     * @param reputationController    the force's reputation controller
     * @param chaosCampaignReputation the force's overall reputation
     * @param factionStandings        the force's standings with the wider universe
     * @param campaignOptions         the campaign options the force's {@link ForceOptions} passes through to
     */
    public PlayerForce(Faction faction, megamek.common.enums.Faction techFaction, RankSystem rankSystem,
          Finances finances, ForceReputationController reputationController, int chaosCampaignReputation,
          FactionStandings factionStandings, CampaignOptions campaignOptions) {
        super(new ForceOptions(campaignOptions, faction),
              techFaction,
              rankSystem,
              finances,
              reputationController,
              chaosCampaignReputation,
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

    // region Contracts

    /**
     * @return this force's contract market: the pool of currently available offers, split by search type
     */
    public ContractMarket getContractMarket() {
        return contractMarket;
    }

    /**
     * @return the live, insertion-ordered map of this force's contracts, keyed by contract id
     */
    public Map<UUID, AbstractContract> getContractHistory() {
        return contractHistory;
    }

    /**
     * @param contractId the id to look up
     *
     * @return the contract with the given id, or {@code null} if this force holds no such contract
     */
    public @Nullable AbstractContract getContract(final UUID contractId) {
        return contractHistory.get(contractId);
    }

    /**
     * Adds (or replaces) a contract, keyed by its own {@link AbstractContract#getId()}.
     *
     * @param contract the contract to store
     */
    public void addContract(final AbstractContract contract) {
        contractHistory.put(contract.getId(), contract);
    }

    /**
     * Writes this force's contracts as a single {@code <contracts>} block. Emits nothing when there are none.
     *
     * @param printWriter the writer to emit to
     * @param indent      the indentation level of the {@code <contracts>} element
     * @param campaign    the owning campaign (threaded through to serialize nested personnel)
     */
    public void writeContractsToXML(final PrintWriter printWriter, int indent, final Campaign campaign) {
        if (contractHistory.isEmpty()) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(printWriter, indent++, CONTRACTS_TAG);
        for (final AbstractContract contract : contractHistory.values()) {
            ContractXmlCodec.writeContract(printWriter, indent, contract, campaign);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(printWriter, --indent, CONTRACTS_TAG);
    }

    /**
     * Repopulates this force's contracts from a {@code <contracts>} node previously written by
     * {@link #writeContractsToXML(PrintWriter, int, Campaign)}.
     *
     * @param contractsNode the {@code <contracts>} element
     * @param campaign      the owning campaign
     * @param version       the save file's version
     */
    public void loadContractsFromXML(final Node contractsNode, final Campaign campaign, final Version version) {
        contractHistory.clear();
        final NodeList children = contractsNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE
                      || !ContractXmlCodec.CONTRACT_TAG.equals(child.getNodeName())) {
                continue;
            }
            final AbstractContract contract = ContractXmlCodec.readContract(child, campaign, version);
            if ((contract != null) && (contract.getId() != null)) {
                // A contract in the history was accepted, so it has a status. Saves written before acceptance set one
                // carry none; treat those as active rather than letting them drop out of every status filter.
                if (contract.getStatus() == null) {
                    contract.setStatus(MissionStatus.ACTIVE);
                }
                contractHistory.put(contract.getId(), contract);
            }
        }
    }
    // endregion Contracts
}
