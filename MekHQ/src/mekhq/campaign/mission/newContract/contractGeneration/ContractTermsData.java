package mekhq.campaign.mission.newContract.contractGeneration;

import mekhq.campaign.mission.newContract.contractData.ChaosContractStepsTable;

public record ContractTermsData(ChaosContractStepsTable payRate,
      ChaosContractStepsTable support,
      ChaosContractStepsTable transport,
      ChaosContractStepsTable salvageRights,
      ChaosContractStepsTable commandRights
) {
}
