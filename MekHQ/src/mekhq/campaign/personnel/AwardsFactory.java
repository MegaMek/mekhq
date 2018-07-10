package mekhq.campaign.personnel;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Award;
import mekhq.campaign.LogEntry;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AwardsFactory {

    private static AwardsFactory instance = null;
    private static ResourceBundle resourceMap = null;

    private static Map<AwardNames, Award> awardsMap;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

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

    public static Award generateNewFromXML(Node node){
        final String METHOD_NAME = "generateNewFromXML(Node)"; //$NON-NLS-1$

        String name = null;
        Date date = null;

        try {
            // Okay, now load fields!
            NodeList nl = node.getChildNodes();

            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    date = DATE_FORMAT.parse(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    name = wn2.getTextContent();
                }
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.getLogger().log(LogEntry.class, METHOD_NAME, ex);
        }

        return GenerateNew(name, date);

    }
}