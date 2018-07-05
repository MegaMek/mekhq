package mekhq.campaign.personnel;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Award;

import java.util.*;

public class AwardsFactory {

    private static AwardsFactory instance = null;
    private static ResourceBundle resourceMap = null;

    public static Map<Awards, Award> awardsMap;

    private AwardsFactory(){}

    public static AwardsFactory getInstance(){
        if(instance == null){
            instance = new AwardsFactory();
            resourceMap = ResourceBundle.getBundle("mekhq.resources.Awards", new EncodeControl());

            awardsMap = new HashMap<>();

             for(Awards award : Awards.values()){
                 String longName = resourceMap.getString(award.toString() + ".text");
                 String description = resourceMap.getString(award.toString() + ".description");
                 int xp = Integer.parseInt(resourceMap.getString(award.toString() + ".xp"));

                 awardsMap.put(award, new Award(award, longName, description, xp));
             }
        }

        return instance;
    }

    public static Award GenerateNew(String awardName, Date date){
        Award blueprintAward = awardsMap.get(Awards.valueOf(awardName));

        return blueprintAward.createCopy(date);
    }
}
