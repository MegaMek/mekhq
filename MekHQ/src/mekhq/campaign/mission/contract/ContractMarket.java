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
package mekhq.campaign.mission.contract;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.io.ContractXmlCodec;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The contract market owned by a {@link PlayerForce}: the pool of currently available {@link AbstractContract} offers,
 * split into one map per {@link ContractSearchType}.
 *
 * <p>Each map is keyed by {@link AbstractContract#getId()} and iterated in insertion order. The four maps are:
 * mercenary work ({@link ContractSearchType#MERCENARY}), acts of piracy ({@link ContractSearchType#PIRATE}), government
 * orders ({@link ContractSearchType#GOVERNMENT}), and tournament bouts ({@link ContractSearchType#TOURNAMENT}). The
 * market UI shows exactly one of these at a time, chosen by the player's selected search type.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractMarket {
    private static final MMLogger LOGGER = MMLogger.create(ContractMarket.class);

    public static final String MARKET_TAG = "contractMarket";

    /**
     * The on-disk sections of the market, each mapping its element tag name to the map it fills. Loading dispatches a
     * section to its target map by tag name (map-lookup); writing iterates these same entries, so the two stay in
     * lock-step and a new section is added in exactly one place.
     */
    private static final Map<String, Function<ContractMarket, Map<UUID, AbstractContract>>> SECTION_ACCESSORS =
          createSectionAccessors();

    private static Map<String, Function<ContractMarket, Map<UUID, AbstractContract>>> createSectionAccessors() {
        Map<String, Function<ContractMarket, Map<UUID, AbstractContract>>> accessors = new LinkedHashMap<>();
        accessors.put("mercenaryWork", ContractMarket::getMercenaryWork);
        accessors.put("actsOfPiracy", ContractMarket::getActsOfPiracy);
        accessors.put("governmentOrders", ContractMarket::getGovernmentOrders);
        accessors.put("tournament", ContractMarket::getTournament);
        return accessors;
    }

    private final Map<UUID, AbstractContract> mercenaryWork = new LinkedHashMap<>();
    private final Map<UUID, AbstractContract> actsOfPiracy = new LinkedHashMap<>();
    private final Map<UUID, AbstractContract> governmentOrders = new LinkedHashMap<>();
    private final Map<UUID, AbstractContract> tournament = new LinkedHashMap<>();

    /** @return the mercenary-work offers ({@link ContractSearchType#MERCENARY}) */
    public Map<UUID, AbstractContract> getMercenaryWork() {
        return mercenaryWork;
    }

    /** @return the acts-of-piracy offers ({@link ContractSearchType#PIRATE}) */
    public Map<UUID, AbstractContract> getActsOfPiracy() {
        return actsOfPiracy;
    }

    /** @return the government-order offers ({@link ContractSearchType#GOVERNMENT}) */
    public Map<UUID, AbstractContract> getGovernmentOrders() {
        return governmentOrders;
    }

    /** @return the tournament offers ({@link ContractSearchType#TOURNAMENT}) */
    public Map<UUID, AbstractContract> getTournament() {
        return tournament;
    }

    /**
     * Returns the live offer map backing a given search type. Mutating the returned map mutates the market.
     *
     * @param searchType the search type whose offers are wanted
     *
     * @return the map of offers for that search type
     */
    public Map<UUID, AbstractContract> getContracts(final ContractSearchType searchType) {
        return switch (searchType) {
            case MERCENARY -> mercenaryWork;
            case PIRATE -> actsOfPiracy;
            case GOVERNMENT -> governmentOrders;
            case TOURNAMENT -> tournament;
        };
    }

    /**
     * Adds (or replaces) an offer under the given search type, keyed by its {@link AbstractContract#getId()}.
     *
     * @param searchType the search type the offer belongs to
     * @param contract   the offer to store
     */
    public void addContract(final ContractSearchType searchType, final AbstractContract contract) {
        getContracts(searchType).put(contract.getId(), contract);
    }

    /**
     * Removes an offer from the given search type's map.
     *
     * @param searchType the search type the offer belongs to
     * @param contract   the offer to remove
     */
    public void removeContract(final ContractSearchType searchType, final AbstractContract contract) {
        getContracts(searchType).remove(contract.getId());
    }

    /**
     * @param searchType the search type to test
     *
     * @return {@code true} if there are no offers for that search type
     */
    public boolean isEmpty(final ContractSearchType searchType) {
        return getContracts(searchType).isEmpty();
    }

    public void clear(final ContractSearchType searchType) {
        getContracts(searchType).clear();
    }

    // region File I/O

    /**
     * Writes the whole market as a single {@code <contractMarket>} block, one section per non-empty map (see
     * {@link #SECTION_ACCESSORS}). Emits nothing when every map is empty. Each contract is delegated to
     * {@link ContractXmlCodec#writeContract(PrintWriter, int, AbstractContract, Campaign)}.
     *
     * @param printWriter the writer to emit to
     * @param indent      the indentation level of the {@code <contractMarket>} element
     * @param campaign    the owning campaign (threaded through to serialize nested personnel)
     */
    public void writeToXML(final PrintWriter printWriter, int indent, final Campaign campaign) {
        final boolean hasAnyOffer = SECTION_ACCESSORS.values().stream().anyMatch(accessor -> !accessor.apply(this)
                                                                                                    .isEmpty());
        if (!hasAnyOffer) {
            return;
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(printWriter, indent++, MARKET_TAG);
        for (final Map.Entry<String, Function<ContractMarket, Map<UUID, AbstractContract>>> section :
              SECTION_ACCESSORS.entrySet()) {
            final Map<UUID, AbstractContract> offers = section.getValue().apply(this);
            if (offers.isEmpty()) {
                continue;
            }
            MHQXMLUtility.writeSimpleXMLOpenTag(printWriter, indent++, section.getKey());
            for (final AbstractContract contract : offers.values()) {
                ContractXmlCodec.writeContract(printWriter, indent, contract, campaign);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(printWriter, --indent, section.getKey());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(printWriter, --indent, MARKET_TAG);
    }

    /**
     * Repopulates the market from a {@code <contractMarket>} node previously written by
     * {@link #writeToXML(PrintWriter, int, Campaign)}. Every map is cleared first, so a load fully replaces the current
     * market. Each child section is dispatched to its target map by tag name via {@link #SECTION_ACCESSORS}
     * (map-lookup); an unrecognized section is logged and skipped.
     *
     * @param marketNode the {@code <contractMarket>} element
     * @param campaign   the owning campaign
     * @param version    the save file's version
     */
    public void loadFromXML(final Node marketNode, final Campaign campaign, final Version version) {
        SECTION_ACCESSORS.values().forEach(accessor -> accessor.apply(this).clear());

        final NodeList sections = marketNode.getChildNodes();
        for (int i = 0; i < sections.getLength(); i++) {
            final Node section = sections.item(i);
            if (section.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            final Function<ContractMarket, Map<UUID, AbstractContract>> accessor =
                  SECTION_ACCESSORS.get(section.getNodeName());
            if (accessor == null) {
                LOGGER.warn("Unexpected contract-market section ignored: {}", section.getNodeName());
                continue;
            }

            final Map<UUID, AbstractContract> offers = accessor.apply(this);
            final NodeList contractNodes = section.getChildNodes();
            for (int j = 0; j < contractNodes.getLength(); j++) {
                final Node contractNode = contractNodes.item(j);
                if ((contractNode.getNodeType() != Node.ELEMENT_NODE)
                          || !ContractXmlCodec.CONTRACT_TAG.equals(contractNode.getNodeName())) {
                    continue;
                }
                final AbstractContract contract = ContractXmlCodec.readContract(contractNode, campaign, version);
                if ((contract != null) && (contract.getId() != null)) {
                    offers.put(contract.getId(), contract);
                }
            }
        }
    }
    // endregion File I/O
}
