package mekhq.campaign.mission.mission;

import java.util.UUID;

public abstract class AbstractContractObjective {
    String name;
    UUID objectiveId;
    AbstractContractManager parentContract;
}
