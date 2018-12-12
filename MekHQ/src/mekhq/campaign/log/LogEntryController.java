/*
 * Copyright (C) 2018 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.log;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible of some general control of log entries.
 * @author Miguel Azevedo
 */
public class LogEntryController {

    private static ResourceBundle logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries", new EncodeControl());

    private static final Pattern madePrisonerPattern = Pattern.compile("Made Prisoner (.*)");
    private static final Pattern madeBondsmanPattern = Pattern.compile("Made Bondsman (.*)");
    private static final Map<String, Pattern> patternCache = new HashMap<>();

    /**
     * Generates a string with the rank of the person
     * @param person whose rank we want to generate the string
     * @return string with the rank
     */
    public static String generateRankEntryString(Person person) {
        String rankEntry = "";
        if (person.getRankNumeric() > 0) {
            String message = logEntriesResourceMap.getString("asA.text");
            rankEntry = MessageFormat.format(message, person.getRankName());
        }

        return rankEntry;
    }

    private static Pattern compilePattern(String key, Supplier<String> regex) {
        Pattern pattern = patternCache.get(key);
        if (null == pattern) {
            pattern = Pattern.compile(regex.get());
            patternCache.put(key, pattern);
        }

        return pattern;
    }

    /**
     * Used for backward compatibility, determines the LogEntryType based solely on the description of the log entry
     * @param description of the log entry
     * @return type of the log entry.
     */
    public static LogEntryType determineTypeFromLogDescription(String description) {

        if (foundExpressionWithOneVariable("madeBondsmanBy.text", description) ||
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
                foundExpressionWithOneVariable("removedFrom.text", description)) {
            return LogEntryType.SERVICE;
        }

        if (foundExpressionWithOneVariable("spouseKia.text", description) ||
                foundExpressionWithOneVariable("divorcedFrom.text", description) ||
                foundExpressionWithOneVariable("marries.text", description) ||
                foundExpressionWithOneVariable("gained.text", description) ||
                foundSingleExpression("gainedEdge.text", description)) {
            return LogEntryType.PERSONAL;
        }

        if (foundExpressionWithOneVariable("removedAward.text", description) ||
                foundExpressionWithTwoVariables("awarded.text", description)) {
            return LogEntryType.AWARD;
        }

        if (foundExpressionWithTwoVariables("severedSpine.text", description) ||
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
                foundExpressionWithOneVariable("hasConceived.text", description)) {
            return LogEntryType.MEDICAL;
        }

        return LogEntryType.CUSTOM;
    }

    private static boolean foundSingleExpression(String logEntryProperty, String description) {
        Pattern pattern = compilePattern(logEntryProperty, 
            () -> logEntriesResourceMap.getString(logEntryProperty));
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    private static boolean foundExpressionWithOneVariable(String logEntryProperty, String description) {
        Pattern pattern = compilePattern(logEntryProperty,
            () -> MessageFormat.format(logEntriesResourceMap.getString(logEntryProperty), "(.*)"));
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    private static boolean foundBeginningOfExpressionEndingWithMultilineAndTab(String logEntryProperty, String description) {
        Pattern pattern = compilePattern(logEntryProperty,
            () -> logEntriesResourceMap.getString(logEntryProperty) + "((.|\\n)*)");
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    private static boolean foundExpressionWithTwoVariables(String logEntryProperty, String description) {
        Pattern pattern = compilePattern(logEntryProperty,
            () -> MessageFormat.format(logEntriesResourceMap.getString(logEntryProperty), "(.*)", "(.*)"));
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    private static boolean foundExpressionWithThreeVariables(String logEntryProperty, String description) {
        Pattern pattern = compilePattern(logEntryProperty,
            () -> MessageFormat.format(logEntriesResourceMap.getString(logEntryProperty), "(.*)", "(.*)", "(.*)"));
        Matcher matcher = pattern.matcher(description);

        return matcher.matches();
    }

    /**
     * Updates the description of an old log entry with a new one. This is used to maintain backward compatibilty,
     * by substituting old log entries with improved ones.
     * @param description to be changes
     * @return string with the new description.
     */
    public static String updateOldDescription(String description) {
        String newDescription = updatePrisonerDescription(description);
        if (!newDescription.isEmpty()) {
            return newDescription;
        }

        newDescription = updateBondsmanDescription(description);
        if (!newDescription.isEmpty()) {
            return newDescription;
        }

        return "";
    }

    private static String updatePrisonerDescription(String description) {

        Matcher matcher = madePrisonerPattern.matcher(description);

        if (matcher.matches()) {
            return MessageFormat.format(logEntriesResourceMap.getString("madePrisonerBy.text"), matcher.group(1));
        }

        if (description.contains("Made Prisoner")) {
            return logEntriesResourceMap.getString("madePrisoner.text");
        }

        return "";
    }

    private static String updateBondsmanDescription(String description) {

        Matcher matcher = madeBondsmanPattern.matcher(description);

        if (matcher.matches()) {
            return MessageFormat.format(logEntriesResourceMap.getString("madeBondsmanBy.text"), matcher.group(1));
        }

        if (description.contains("Made Bondsman")) {
            return logEntriesResourceMap.getString("madeBondsman.text");
        }

        return "";
    }
}
