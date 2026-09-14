package mekhq.campaign.mission.contract.contractGeneration;

/**
 * Generates mercenary-work contracts ({@link ContractSearchType#MERCENARY}). It uses the shared
 * {@link AbstractContractGeneration} skeleton unchanged - a semi-random themed employer, the general objective table,
 * and the standard covert-status rules - so it only declares its search type.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class MercenaryContractGeneration extends AbstractContractGeneration {
    @Override
    protected ContractSearchType getSearchType() {
        return ContractSearchType.MERCENARY;
    }
}
