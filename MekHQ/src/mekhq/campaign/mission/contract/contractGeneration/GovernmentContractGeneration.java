package mekhq.campaign.mission.contract.contractGeneration;

/**
 * Generates government-order contracts ({@link ContractSearchType#GOVERNMENT}), issued by the player's own faction. It
 * uses the shared {@link AbstractContractGeneration} skeleton unchanged - the general objective table and the standard
 * covert-status rules, which never conceal a government's own employer - so it only declares its search type.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class GovernmentContractGeneration extends AbstractContractGeneration {
    @Override
    protected ContractSearchType getSearchType() {
        return ContractSearchType.GOVERNMENT;
    }
}
