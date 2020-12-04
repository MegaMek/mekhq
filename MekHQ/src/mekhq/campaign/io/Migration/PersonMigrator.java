package mekhq.campaign.io.Migration;

public class PersonMigrator {
    public static String awardDefaultSetMigrator(String text) {
        switch (text) {
            case "Armed Forces":
                return "Combat Cross";
            case "Combat Commendation":
                return "Combat Unit Commendation";
            case "Combat Unit Commendation":
                return "Meritorious Service";
            case "Distinguished Service":
                return "House Superior Service";
            case "Fedcom Civil War Campaign":
                return "FedCom Civil War Campaign";
            case "House Meritorious Service":
                return "Legion of Merit";
            case "House Superior Service":
                return "House Unit Citation";
            case "Meritourious Service":
                return "Combat Commendation";
            case "Meritourious Unit Commendation":
                return "Meritorious Unit Commendation";
            case "Bronze Star":
            case "Clan Invasion Campaign":
            case "Combat Achievement":
            case "Combat Action":
            case "Combat Cross":
            case "Expeditionary":
            case "Fourth Succession War Campaign":
            case "Galactic Service":
            case "Galactic Service Deployment":
            case "Galactic War on Pirating":
            case "Good Conduct":
            case "House Defense":
            case "House Distinguished Service":
            case "House Unit Citation":
            case "Humanitarian Service":
            case "Legion of Merit":
            case "Periphery Expeditionary":
            case "Prisoner of War":
            case "Purple Heart":
            case "Silver Star":
            case "Task Force Serpent Campaign":
            case "Third Succession War Campaign":
            case "War of 3039 Campaign":
                return text;
            default:
                return null;
        }
    }
}
