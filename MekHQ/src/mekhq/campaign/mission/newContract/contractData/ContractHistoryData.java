package mekhq.campaign.mission.newContract.contractData;

import java.util.ArrayList;
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
        contractHistory.put(contract.getContractId(), contract);
    }

    public void remove(AbstractContract contract) {
        contractHistory.remove(contract.getContractId());
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
            if (!contract.getMissionStatus().isActive()) {
                if (!completedContracts.contains(contract)) {
                    completedContracts.add(contract);
                }
            }
        }

        return completedContracts;
    }
}
