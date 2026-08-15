package mekhq.campaign.mission.newContract.utilities;

import java.util.List;

import mekhq.campaign.mission.enums.ContractObjectiveType;
import mekhq.campaign.mission.newContract.AbstractContract;
import mekhq.campaign.unit.Unit;

public class ContractRepairLocation {
    /**
     * Determines the repair location for the contract based on the contract type.
     *
     * <p>The returned repair location corresponds to the type of operation:</p>
     *
     * <ul>
     *   <li>Guerrilla warfare contracts: {@link Unit#SITE_IMPROVISED}</li>
     *   <li>Raid-type contracts: {@link Unit#SITE_FIELD_WORKSHOP}</li>
     *   <li>All other contracts: {@link Unit#SITE_FACILITY_BASIC}</li>
     * </ul>
     *
     * @return the repair location constant based on the contract type
     */
    public static int getRepairLocation(ContractObjectiveType contractType) {
        int repairLocation = Unit.SITE_FACILITY_BASIC;

        if (contractType.isGuerrillaType()) {
            repairLocation = Unit.SITE_IMPROVISED;
        } else if (contractType.isRaidType()) {
            repairLocation = Unit.SITE_FIELD_WORKSHOP;
        }

        return repairLocation;
    }

    /**
     * Determines the best available repair location from a list of active contracts.
     *
     * <p>This method evaluates all active contracts and returns the highest quality repair facility available.
     * Repair locations are ranked numerically, with higher values representing better facilities. If no active
     * contracts exist, a basic facility is assumed to be available.</p>
     *
     * @param activeContracts the list of active contracts to evaluate for repair facilities
     *
     * @return the numeric value of the best available repair location; returns {@link Unit#SITE_FACILITY_BASIC} if no
     *       contracts are active
     */
    public static int getBestRepairLocation(List<AbstractContract> activeContracts) {
        if (activeContracts.isEmpty()) {
            return Unit.SITE_FACILITY_BASIC;
        }

        int bestSite = Unit.SITE_IMPROVISED;
        for (AbstractContract contract : activeContracts) {
            int repairLocation = getRepairLocation(contract.getObjectiveType());
            if (repairLocation > bestSite) {
                bestSite = repairLocation;
            }
        }

        return bestSite;
    }
}
