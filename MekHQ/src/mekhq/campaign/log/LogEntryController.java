package mekhq.campaign.log;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogEntryController {

    private static LogEntryController logEntryController = null;

    private static ServiceLogEntryController serviceLogEntryController = null;
    private static MedicalLogEntryController medicalLogEntryController = null;
    private static PersonalLogEntryController personalLogEntryController = null;
    private static AwardLogEntryController awardLogEntryController = null;

    private static ResourceBundle logEntriesResourceMap;

    public LogEntryController() {
        logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries", new EncodeControl());
    }

    public static LogEntryController getInstance() {
        if (null == logEntryController){
            logEntryController = new LogEntryController();
        }
        return logEntryController;
    }

    public static ServiceLogEntryController getServiceLogController(){
        if(null == serviceLogEntryController){
            serviceLogEntryController = new ServiceLogEntryController(logEntriesResourceMap);
        }
        return serviceLogEntryController;
    }

    public static MedicalLogEntryController getMedicalLogController(){
        if(null == medicalLogEntryController){
            medicalLogEntryController = new MedicalLogEntryController(logEntriesResourceMap);
        }
        return medicalLogEntryController;
    }

    public static PersonalLogEntryController getPersonalLogEntryController(){
        if(null == personalLogEntryController){
            personalLogEntryController = new PersonalLogEntryController(logEntriesResourceMap);
        }
        return personalLogEntryController;
    }

    public static AwardLogEntryController getAwardLogEntryController(){
        if(null == awardLogEntryController){
            awardLogEntryController = new AwardLogEntryController(logEntriesResourceMap);
        }
        return awardLogEntryController;
    }

    public String generateRankEntryString(Person person){
        String rankEntry = "";
        if (person.getRankNumeric() > 0) {
            String message = logEntriesResourceMap.getString("asA.text");
            rankEntry = MessageFormat.format(message, person.getRankName());
        }

        return rankEntry;
    }

    public LogEntryType determineTypeFromLogDescription(String description){

        if(foundExpressionWithOneVariable("madeBondsmanBy.text", description) ||
           foundExpressionWithOneVariable("madePrisonerBy.text", description) ||
           foundExpressionWithOneVariable("joined.text", description) ||
           foundSingleExpression("freed.text", description) ||
           foundSingleExpression("madePrisoner.text", description) ||
           foundSingleExpression("madeBondsman.text", description) ||
           foundSingleExpression("kia.text", description) ||
           foundSingleExpression("mia.text", description) ||
           foundSingleExpression("recoveredMia.text", description) ||
           foundSingleExpression("retired.text", description) ||
           foundExpressionWithOneVariable("promotedTo.text", description) ||
           foundExpressionWithOneVariable("demotedTo.text", description) ||
           foundExpressionWithTwoVariables("participatedInMission.text", description) ||
           foundExpressionWithTwoVariables("successfullyTreatedForXInjuries.text", description) ||
           foundExpressionWithThreeVariables("successfullyTreatedWithXp.text", description) ||
           foundExpressionWithOneVariable("gainedXpFromMedWork.text", description) ||
           foundExpressionWithOneVariable("retiredDueToWounds.text", description) ||
           foundExpressionWithOneVariable("reassignedTo.text", description) ||
           foundExpressionWithOneVariable("assignedTo.text", description) ||
           foundExpressionWithOneVariable("removedFrom.text", description)
           )
            return LogEntryType.SERVICE;

        if(foundExpressionWithOneVariable("spouseKia.text",description) ||
           foundExpressionWithOneVariable("divorcedFrom.text", description) ||
           foundExpressionWithOneVariable("marries.text", description) ||
           foundExpressionWithOneVariable("gained.text", description) ||
           foundSingleExpression("gainedEdge.text", description)
           )
            return LogEntryType.PERSONAL;

        if(foundExpressionWithOneVariable("removedAward.text", description) ||
          foundExpressionWithTwoVariables("awarded.text", description)
          )
            return LogEntryType.AWARD;


        if(foundExpressionWithTwoVariables("severedSpine.text", description) ||
           foundExpressionWithOneVariable("brokenRibPunctureDead.text", description) ||
           foundExpressionWithOneVariable("brokenRibPuncture.text", description) ||
           foundSingleExpression("developedEncephalopathy.text", description) ||
           foundSingleExpression("concussionWorsened.text", description) ||
           foundSingleExpression("developedCerebralContusion.text", description) ||
           foundSingleExpression("diedDueToBrainTrauma.text", description) ||
           foundSingleExpression("diedOfInternalBleeding.text", description) ||
           foundSingleExpression("internalBleedingWorsened.text", description) ||
           foundBeginningOfExpressionEndingWithMultilineAndTab("returnedWithInjuries.text", description) ||
           foundExpressionWithTwoVariables("docMadeAMistake.text", description) ||
           foundExpressionWithThreeVariables("docAmazingWork.text", description) ||
           foundExpressionWithTwoVariables("successfullyTreated.text", description) ||
           foundExpressionWithOneVariable("didntHealProperly.text", description) ||
           foundExpressionWithOneVariable("healed.text", description) ||
           foundExpressionWithOneVariable("becamePermanent.text", description) ||
           foundSingleExpression("diedInInfirmary.text", description) ||
           foundSingleExpression("abductedFromInfirmary.text", description) ||
           foundSingleExpression("retiredAndTransferedFromInfirmary.text", description) ||
           foundSingleExpression("dismissedFromInfirmary.text", description) ||
           foundExpressionWithOneVariable("deliveredBaby.text", description) ||
           foundSingleExpression("hasConceived.text", description) ||
           foundExpressionWithOneVariable("hasConceived.text", description)
           )
            return LogEntryType.MEDICAL;

        return LogEntryType.CUSTOM;
    }

    private boolean foundSingleExpression(String logEntryProperty, String description){
        Pattern pattern = Pattern.compile(logEntriesResourceMap.getString(logEntryProperty));
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    private boolean foundExpressionWithOneVariable(String logEntryProperty, String description){
        String message = logEntriesResourceMap.getString(logEntryProperty);
        Pattern pattern = Pattern.compile(MessageFormat.format(message, "(.*)"));
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    private boolean foundBeginningOfExpressionEndingWithMultilineAndTab(String logEntryProperty, String description){
        String message = logEntriesResourceMap.getString(logEntryProperty);
        Pattern pattern = Pattern.compile(message + "((.|\\n)*)");
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    private boolean foundExpressionWithTwoVariables(String logEntryProperty, String description){
        String message = logEntriesResourceMap.getString(logEntryProperty);
        Pattern pattern = Pattern.compile(MessageFormat.format(message, "(.*)", "(.*)"));
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    private boolean foundExpressionWithThreeVariables(String logEntryProperty, String description){
        String message = logEntriesResourceMap.getString(logEntryProperty);
        Pattern pattern = Pattern.compile(MessageFormat.format(message, "(.*)", "(.*)", "(.*)"));
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    public String updateOldDescription(String description){
        String newDescription = "";

        newDescription = updatePrisonerDescription(description);
        if(!newDescription.isEmpty()) return newDescription;

        newDescription = updateBondsmanDescription(description);
        if(!newDescription.isEmpty()) return newDescription;

        return "";
    }

    private String updatePrisonerDescription(String description){
        Pattern pattern = Pattern.compile("Made Prisoner (.*)");
        Matcher matcher = pattern.matcher(description);

        if(matcher.matches()){
            return MessageFormat.format(logEntriesResourceMap.getString("madePrisonerBy.text"), matcher.group(1));
        }

        pattern = Pattern.compile("Made Prisoner");
        matcher = pattern.matcher(description);

        if(matcher.matches()){
            return MessageFormat.format(logEntriesResourceMap.getString("madePrisoner.text"), matcher.group(1));
        }

        return "";
    }

    private String updateBondsmanDescription(String description){
        Pattern pattern = Pattern.compile("Made Bondsman (.*)");
        Matcher matcher = pattern.matcher(description);

        if(matcher.matches()){
            return MessageFormat.format(logEntriesResourceMap.getString("madeBondsmanBy.text"), matcher.group(1));
        }

        pattern = Pattern.compile("Made Bondsman");
        matcher = pattern.matcher(description);

        if(matcher.matches()){
            return MessageFormat.format(logEntriesResourceMap.getString("madeBondsman.text"), matcher.group(1));
        }

        return "";
    }
}
