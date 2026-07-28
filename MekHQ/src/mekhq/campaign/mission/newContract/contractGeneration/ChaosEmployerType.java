package mekhq.campaign.mission.newContract.contractGeneration;

public enum ChaosEmployerType {
    ANY_PLANETARY_GOVERNMENT,
    ANY_SYSTEM_OWNER,
    CIVILIAN_ORGANIZATION_BUSINESS,
    CIVILIAN_ORGANIZATION_MILITIA,
    CIVILIAN_ORGANIZATION_REBELS,
    CORPORATION,
    LOCAL_PLANETARY_GOVERNMENT,
    LOCAL_SYSTEM_OWNER,
    MERCENARY_SUBCONTRACT,
    NOBLE;

    public boolean isCurrentSystemEmployer() {
        return this == LOCAL_SYSTEM_OWNER ||
                     this == LOCAL_PLANETARY_GOVERNMENT;
    }

    public boolean isSystemOwner() {
        return this == LOCAL_SYSTEM_OWNER ||
                     this == ANY_SYSTEM_OWNER;
    }
}
