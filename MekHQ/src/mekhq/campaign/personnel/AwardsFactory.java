package mekhq.campaign.personnel;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Award;

import java.util.*;

public class AwardsFactory {

    private static AwardsFactory instance = null;
    private static ResourceBundle resourceMap = null;

    private static Map<AwardNames, Award> awardsMap;

    private AwardsFactory(){}

    public static AwardsFactory getInstance(){
        if(instance == null){
            instance = new AwardsFactory();
            resourceMap = ResourceBundle.getBundle("mekhq.resources.Awards", new EncodeControl());

            awardsMap = new HashMap<>();

             for(AwardNames award : AwardNames.values()){
                 String longName = resourceMap.getString(award.toString() + ".text");
                 String description = resourceMap.getString(award.toString() + ".description");

                 String medalFile = null;
                 if(resourceMap.containsKey(award.toString() + ".medal"))
                     medalFile = resourceMap.getString(award.toString() + ".medal");

                 String ribbonFile = null;
                 if(resourceMap.containsKey(award.toString() + ".ribbon"))
                    ribbonFile = resourceMap.getString(award.toString() + ".ribbon");

                 int xp = Integer.parseInt(resourceMap.getString(award.toString() + ".xp"));

                 awardsMap.put(award, new Award(award, longName, description, xp, medalFile, ribbonFile));
             }
        }

        return instance;
    }

    public Collection<Award> getAllAwards(){
        return awardsMap.values();
    }

    public static Award GenerateNew(String awardName, Date date){
        Award blueprintAward = awardsMap.get(AwardNames.valueOf(awardName));

        return blueprintAward.createCopy(date);
    }
}
