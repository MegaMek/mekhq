package mekhq.campaign.stratcon;

public class StratconTrackState {
    // a track has the following characteristics:
    // width/height
    // [future]: terrain information by coordinates
    // scenario information by coordinates
    // active facilities by coordinates
    private String displayableName;
    
    
    public String getDisplayableName() {
        return displayableName;
    }
    
    public void setDisplayableName(String name) {
        displayableName = name;
    }
}
