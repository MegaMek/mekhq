package mekhq.campaign.personnel;

import mekhq.MekHQ;
import mekhq.campaign.Award;
import mekhq.campaign.AwardSet;
import mekhq.campaign.LogEntry;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class AwardsFactory {

    private static AwardsFactory instance = null;

    private static Map<String, Map<String,Award>> awardsMap;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private AwardsFactory(){}

    public static AwardsFactory getInstance(){
        if(instance == null){
            instance = new AwardsFactory();
            awardsMap = new HashMap<>();
            loadAwards();
        }

        return instance;
    }

    public static List<String> getAllSetNames(){
        return new ArrayList<>(awardsMap.keySet());
    }

    public static List<Award> getAllAwardsForSet(String setName){
        return new ArrayList<>(awardsMap.get(setName).values());
    }

    public static Award GenerateNew(String setName, String awardName, Date date){
        Map<String, Award> awardSet = awardsMap.get(setName);
        Award blueprintAward = awardSet.get(awardName);
        return blueprintAward.createCopy(date);
    }

    public static Award generateNewFromXML(Node node){
        final String METHOD_NAME = "generateNewFromXML(Node)"; //$NON-NLS-1$

        String name = null;
        String set = null;
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
                } else if (wn2.getNodeName().equalsIgnoreCase("set")){
                    set = wn2.getTextContent();
                }
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.getLogger().log(LogEntry.class, METHOD_NAME, ex);
        }

        return GenerateNew(set, name, date);
    }

    private static void loadAwards(){
        File dir = new File("data/awards/");
        File[] files =  dir.listFiles((dir1, filename) -> filename.endsWith(".xml"));

        for(File file : files){

            AwardSet awardSet = null;

            try {
                InputStream inputStream = new FileInputStream(file);
                JAXBContext jaxbContext = JAXBContext.newInstance(AwardSet.class, Award.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                awardSet = unmarshaller.unmarshal(new StreamSource(inputStream), AwardSet.class).getValue();

                Map<String, Award> tempAwardMap = new HashMap<>();
                String currentSetName = file.getName().replaceFirst("[.][^.]+$", "");
                for (Award award : awardSet.getAwards()){
                    award.setSet(currentSetName);
                    tempAwardMap.put(award.getName(), award);
                }
                awardsMap.put(currentSetName, tempAwardMap);

            } catch (JAXBException var4) {
                System.err.println("Error loading XML for awards: " + var4.getMessage());
                var4.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

