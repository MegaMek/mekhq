package mekhq.campaign.mission.newContract.contractData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import mekhq.MekHQ;
import mekhq.campaign.events.missions.MissionNewEvent;
import mekhq.campaign.mission.newContract.AbstractContract;

public record ContractHistoryData(LinkedHashMap<UUID, AbstractContract> contractHistory) {
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
            if (!contract.getStatus().isActive()) {
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
            if (contract.getStatus().isActive()) {
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
            if (contract.getStatus().isActive() && !activeContracts.contains(contract)) {
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
     *       date is strictly after {@code currentDate} - a contract starting today has started, so it is excluded
     */
    public List<AbstractContract> getActiveAndNotYetStarted(LocalDate currentDate) {
        List<AbstractContract> activeContracts = new ArrayList<>();

        for (AbstractContract contract : contractHistory.values()) {
            if (contract.getStatus().isActive() && !activeContracts.contains(contract)) {
                LocalDate startDate = contract.getStartDate();
                if (startDate.isAfter(currentDate)) {
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
        sortedMissions.sort(Comparator.comparing((AbstractContract mission) -> mission.getStatus().isCompleted())
                                  .thenComparingLong((AbstractContract mission) -> this.getMissionSortKey(mission,
                                        currentDate)));
        return sortedMissions;
    }

    private long getMissionSortKey(AbstractContract mission, LocalDate currentDate) {
        LocalDate startDate = mission.getStartDate();
        if (startDate == null) {
            return mission.getStatus().isCompleted() ? Long.MAX_VALUE : currentDate.toEpochDay();
        }
        long startDay = startDate.toEpochDay();
        return mission.getStatus().isCompleted() ? -startDay : startDay;
    }
}
