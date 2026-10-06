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
package mekhq.campaign.mission.contract.contractData;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import megamek.Version;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.missions.MissionNewEvent;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.io.ContractXmlCodec;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The campaign's single store of every contract it has ever held, keyed by {@link AbstractContract#getId()} and
 * iterated in insertion order.
 *
 * <p>This is the only place contract history lives. A campaign's contracts are global, not a property of any one
 * force, so they are stored, queried and serialized here.</p>
 *
 * @param contractHistory the live, insertion-ordered map of contracts, keyed by contract id
 */
public record ContractHistoryData(LinkedHashMap<UUID, AbstractContract> contractHistory) {

    public static final String CONTRACTS_TAG = "contracts";

    public ContractHistoryData() {
        this(new LinkedHashMap<>());
    }

    public AbstractContract get(UUID contractId) {
        return contractHistory.get(contractId);
    }

    public void add(AbstractContract contract) {
        contractHistory.put(contract.getId(), contract);

        MekHQ.triggerEvent(new MissionNewEvent(contract));
    }

    public void remove(UUID contractId) {
        contractHistory.remove(contractId);
    }

    public void clear() {
        contractHistory.clear();
    }

    public boolean isEmpty() {
        return contractHistory.isEmpty();
    }

    public int size() {
        return contractHistory.size();
    }

    public List<AbstractContract> getCompleted() {
        List<AbstractContract> completedContracts = new ArrayList<>();

        for (AbstractContract contract : contractHistory.values()) {
            if (hasStatus(contract) && !contract.getStatus().isActive()) {
                if (!completedContracts.contains(contract)) {
                    completedContracts.add(contract);
                }
            }
        }

        return completedContracts;
    }

    public List<AbstractContract> getActiveIncludingNotYetStarted() {
        List<AbstractContract> activeContracts = new ArrayList<>();

        for (AbstractContract contract : contractHistory.values()) {
            if (isActive(contract)) {
                if (!activeContracts.contains(contract)) {
                    activeContracts.add(contract);
                }
            }
        }

        return activeContracts;
    }

    /**
     * @param currentDate the date to test against
     *
     * @return the contracts running on {@code currentDate}, that is, those whose status is active and whose schedule
     *       covers the date inclusive of both its start and end - a contract counts from the day it starts through the
     *       day it ends
     */
    public List<AbstractContract> getActiveAndStarted(LocalDate currentDate) {
        List<AbstractContract> activeContracts = new ArrayList<>();

        for (AbstractContract contract : contractHistory.values()) {
            if (isActive(contract) && !activeContracts.contains(contract)) {
                if (contract.isActiveOn(currentDate)) {
                    activeContracts.add(contract);
                }
            }
        }

        return activeContracts;
    }

    /**
     * @param currentDate the date to test against
     *
     * @return the accepted contracts that have not begun yet, that is, those whose status is active and whose start
     *       date is strictly after {@code currentDate} - a contract starting today has started, so it is excluded, as
     *       is one with no start date at all, which counts as already begun
     */
    public List<AbstractContract> getActiveAndNotYetStarted(LocalDate currentDate) {
        List<AbstractContract> activeContracts = new ArrayList<>();

        for (AbstractContract contract : contractHistory.values()) {
            if (isActive(contract) && !activeContracts.contains(contract)) {
                LocalDate startDate = contract.getStartDate();
                if ((startDate != null) && startDate.isAfter(currentDate)) {
                    activeContracts.add(contract);
                }
            }
        }

        return activeContracts;
    }

    /**
     * @return missions sorted with active missions from oldest to newest, followed by completed missions from newest to
     *       oldest; active missions without a start date use the campaign date, while completed missions without one
     *       sort last
     */
    public List<AbstractContract> getSortedMissions(LocalDate currentDate) {
        List<AbstractContract> sortedMissions = new ArrayList<>(contractHistory.values());
        sortedMissions.sort(Comparator.comparing(ContractHistoryData::isCompleted)
                                  .thenComparingLong((AbstractContract mission) -> this.getMissionSortKey(mission,
                                        currentDate)));
        return sortedMissions;
    }

    private long getMissionSortKey(AbstractContract mission, LocalDate currentDate) {
        LocalDate startDate = mission.getStartDate();
        if (startDate == null) {
            return isCompleted(mission) ? Long.MAX_VALUE : currentDate.toEpochDay();
        }
        long startDay = startDate.toEpochDay();
        return isCompleted(mission) ? -startDay : startDay;
    }

    // region I/O

    /**
     * Writes the whole history as a single {@code <contracts>} block. Emits nothing when there are no contracts.
     *
     * @param printWriter the writer to emit to
     * @param indent      the indentation level of the {@code <contracts>} element
     * @param campaign    the owning campaign (threaded through to serialize nested personnel)
     */
    public void writeToXML(final PrintWriter printWriter, int indent, final Campaign campaign) {
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
     * Repopulates the campaign's contract history from a {@code <contracts>} node previously written by
     * {@link #writeToXML(PrintWriter, int, Campaign)}.
     *
     * <p>Each contract is handed to {@link Campaign#importMission(AbstractContract)} rather than put straight into
     * the map, because importing also registers the contract's scenarios with the campaign and re-hooks StratCon's
     * backing scenario pointers. A bare map put would restore the contract with neither.</p>
     *
     * @param contractsNode the {@code <contracts>} element
     * @param campaign      the owning campaign, whose history is cleared and refilled
     * @param version       the save file's version
     */
    public static void loadFromXML(final Node contractsNode, final Campaign campaign, final Version version) {
        campaign.getContractHistoryData().clear();

        final NodeList children = contractsNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node contractNode = children.item(i);
            if ((contractNode.getNodeType() != Node.ELEMENT_NODE)
                      || !ContractXmlCodec.CONTRACT_TAG.equals(contractNode.getNodeName())) {
                continue;
            }

            final AbstractContract contract = ContractXmlCodec.readContract(contractNode, campaign, version);
            if ((contract == null) || (contract.getId() == null)) {
                continue;
            }

            // A contract in the history was accepted, so it has a status. Saves written before acceptance set one
            // carry none; treat those as active rather than letting them drop out of every status filter.
            if (contract.getStatus() == null) {
                contract.setStatus(MissionStatus.ACTIVE);
            }

            campaign.importMission(contract);
        }
    }

    // endregion I/O

    /**
     * A contract only has a status once it has been accepted; an un-accepted market offer has none. These treat a
     * missing status as neither active nor completed, so an offer that somehow reaches this collection is skipped
     * rather than throwing.
     */
    private static boolean hasStatus(AbstractContract contract) {
        return contract.getStatus() != null;
    }

    private static boolean isActive(AbstractContract contract) {
        return hasStatus(contract) && contract.getStatus().isActive();
    }

    private static boolean isCompleted(AbstractContract contract) {
        return hasStatus(contract) && contract.getStatus().isCompleted();
    }
}
