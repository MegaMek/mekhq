package mekhq.campaign.mission.newContract.contractData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

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

    /**
     * @return missions sorted with active missions from oldest to newest, followed by completed missions from newest to
     *       oldest; active missions without a start date use the campaign date, while completed missions without one
     *       sort last
     */
    public List<AbstractContract> getSortedMissions() {
        List<AbstractContract> sortedMissions = new ArrayList<>(contractHistory.values());
        sortedMissions.sort(Comparator.comparing((AbstractContract mission) -> mission.getStatus().isCompleted())
                                  .thenComparingLong(this::getMissionSortKey));
        return sortedMissions;
    }

    private long getMissionSortKey(AbstractContract mission) {
        LocalDate startDate = mission.getStartDate();
        if (startDate == null) {
            return mission.getStatus().isCompleted() ? Long.MAX_VALUE : getLocalDate().toEpochDay();
        }
        long startDay = startDate.toEpochDay();
        return mission.getStatus().isCompleted() ? -startDay : startDay;
    }
}
