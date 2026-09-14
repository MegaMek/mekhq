package mekhq.campaign.mission.contract.contractGeneration;

/**
 * Generates tournament-circuit contracts ({@link ContractSearchType#TOURNAMENT}), such as the Solaris VII games. It
 * currently uses the shared {@link AbstractContractGeneration} skeleton unchanged, so it only declares its search type;
 * dedicated tournament handlers can be added here as overrides when that generation lands.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class TournamentContractGeneration extends AbstractContractGeneration {
    @Override
    protected ContractSearchType getSearchType() {
        return ContractSearchType.TOURNAMENT;
    }
}
