package mekhq.campaign.mission.mission;

import java.util.List;
import java.util.UUID;

public abstract class AbstractContractManager {
    String name;
    UUID contractId;
    List<AbstractContractObjective> contractObjectives;
}
