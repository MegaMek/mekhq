package mekhq.campaign.mission;

import java.util.ArrayList;

public class BotForceStub {
    private String name;
    private ArrayList<String> entityList;

    public BotForceStub(String name, ArrayList<String> entityList) {
        this.name = name;
        this.entityList = entityList;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getEntityList() {
        return entityList;
    }
}
