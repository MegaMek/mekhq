package mekhq.campaign.personnel.enums;

import mekhq.MekHQ;

public enum RankSystemType {
    DEFAULT,
    USER_DATA,
    CAMPAIGN;

    //region Boolean Comparison Methods
    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isUserData() {
        return this == USER_DATA;
    }

    public boolean isCampaign() {
        return this == CAMPAIGN;
    }
    //endregion Boolean Comparison Methods

    public String getFilePath() {
        switch (this) {
            case DEFAULT:
                return MekHQ.getMekHQOptions().getRanksDirectoryPath();
            case USER_DATA:
                return MekHQ.getMekHQOptions().getUserRanksDirectoryPath();
            case CAMPAIGN:
            default:
                return "";
        }
    }
}
