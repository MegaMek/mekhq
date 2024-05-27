package mekhq.gui.dialog;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class MutinySupportDialog {
    /**
     * Displays a dialog with options to either support loyalists or rebels.
     *
     * @param resources the resource bundle containing the dialog messages
     * @param isViolentRebellion a boolean indicating whether the rebellion is violent or not
     * @param loyalistForces a list of loyalist units
     * @param rebelForces a list of rebel units
     * @return an integer representing the user's choice:
     *         - 1 if the user chooses to support the loyalists,
     *         - 0 if the user chooses to support the rebels,
     *         - -1 if the user cancels the dialog
     */
    public static int supportDialog(Campaign campaign, ResourceBundle resources, boolean isViolentRebellion,
                                    Integer bystanderPersonnelCount,
                                    Person loyalistLeader, Integer loyalistPersonnelCount, List<Unit> loyalistForces, Integer loyalistBv,
                                    Person rebelLeader, Integer rebelPersonnelCount, List<Unit> rebelForces, Integer rebelBv) {

        AtomicInteger choice = new AtomicInteger(-1);
        JOptionPane pane = new JOptionPane(buildMutinyDescription(
                campaign, resources, isViolentRebellion,
                bystanderPersonnelCount,
                loyalistLeader, loyalistPersonnelCount, loyalistForces, loyalistBv,
                rebelLeader, rebelPersonnelCount, rebelForces, rebelBv
        ),
                JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);

        Object[] options = {
                resources.getString("dialogSupportLoyalists.text"),
                resources.getString("dialogSupportRebels.text")
        };

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        for (Object option : options){
            JButton button = new JButton(option.toString());
            button.addActionListener(e -> {
                choice.set(Arrays.asList(options).indexOf(option));

                Window window = SwingUtilities.getWindowAncestor(button);

                if (window != null) {
                    window.setVisible(false);
                }
            });

            buttonPanel.add(button);
            buttonPanel.add(Box.createHorizontalStrut(15));
        }

        pane.setOptions(new Object[]{buttonPanel});
        JDialog dialog;

        if (isViolentRebellion) {
            dialog = pane.createDialog(null, resources.getString("dialogTitleViolentUprising.text"));
        } else {
            dialog = pane.createDialog(null, resources.getString("dialogTitleRegimeChange.text"));
        }

        dialog.setVisible(true);

        return choice.get();
    }

    private static String buildMutinyDescription(Campaign campaign,  ResourceBundle resources, boolean isViolentRebellion,
                                                 Integer bystanderPersonnelCount,
                                                 Person loyalistLeader, Integer loyalistPersonnelCount, List<Unit> loyalistForces, Integer loyalistBv,
                                                 Person rebelLeader, Integer rebelPersonnelCount, List<Unit> rebelForces, Integer rebelBv) {

        StringBuilder situationDescription = new StringBuilder(String.format(
                resources.getString("dialogDescriptionIntroduction.text"),
                loyalistLeader.getFullTitle(),
                campaign.getName())
        );

        if (isViolentRebellion) {
            situationDescription.append(' ').append(String.format(
                    resources.getString("dialogDescriptionViolentUprising.text"),
                    rebelLeader.getFullTitle()
            ));
        } else {
            situationDescription.append(' ').append(String.format(
                    resources.getString("dialogDescriptionRegimeChange.text"),
                    rebelLeader.getFullTitle())
            );
        }

        HashMap<String, Integer> unitMap = mapUnitCounts(loyalistForces);
        situationDescription.append(getForceSummaryString(resources, unitMap, true, loyalistBv, loyalistPersonnelCount));

        unitMap = mapUnitCounts(rebelForces);
        situationDescription.append(getForceSummaryString(resources, unitMap, false, rebelBv, rebelPersonnelCount));

        situationDescription.append(String.format(resources.getString("dialogDescriptionBystanders.text"), bystanderPersonnelCount));

        situationDescription.append(resources.getString("dialogDescriptionDecision.text"));

        return situationDescription.toString();
    }

    /**
     * Maps the counts of different units in the given list of units.
     *
     * @param units the list of units
     * @return a HashMap containing the counts of each unit type, where the key is the unit type and the value is the count
     */
    private static HashMap<String, Integer> mapUnitCounts(List<Unit> units) {
        HashMap<String, Integer> unitCounts = new HashMap<>();

        int mekCount = 0;
        int fighterCount = 0;
        int protoMekCount = 0;
        int baCount = 0;
        int dropShipCount = 0;
        int infantryCount = 0;
        int vehicleCount = 0;
        int otherCount = 0;

        for (Unit unit : units) {
            Entity entity = unit.getEntity();

            if (entity.isMek()) {
                mekCount++;
            } else if (entity.isFighter()) {
                fighterCount++;
            } else if (entity.isProtoMek()) {
                protoMekCount++;
            } else if (entity.isBattleArmor()) {
                baCount++;
            } else if (entity.isDropShip()) {
                dropShipCount++;
            } else if (entity.isInfantry()) {
                infantryCount++;
            } else if (entity.isVehicle()) {
                vehicleCount++;
            } else {
                otherCount++;
            }
        }

        unitCounts.put("mek", mekCount);
        unitCounts.put("fighter", fighterCount);
        unitCounts.put("protoMek", protoMekCount);
        unitCounts.put("battleArmor", baCount);
        unitCounts.put("dropShip", dropShipCount);
        unitCounts.put("infantry", infantryCount);
        unitCounts.put("vehicle", vehicleCount);
        unitCounts.put("other", otherCount);

        return unitCounts;
    }

    /**
     * Retrieves the summary string for the forces based on the given resources, unit map, and loyalty faction.
     *
     * @param resources the ResourceBundle containing the necessary strings for formatting the summary string
     * @param unitMap   the HashMap containing the count of each unit type
     * @param isLoyalist the flag indicating if the forces are loyalist or rebel
     * @param battleValue the force's estimated Battle Value
     * @param personnelCount the number of personnel supporting the faction
     * @return the formatted summary string
     */
    private static String getForceSummaryString(ResourceBundle resources, HashMap<String, Integer> unitMap, boolean isLoyalist, int battleValue, int personnelCount) {
        StringBuilder forceSummaryString = new StringBuilder();
        String faction;

        if (isLoyalist) {
            faction = resources.getString("dialogDescriptionLoyalist.text");
        } else {
            faction = resources.getString("dialogDescriptionRebels.text");
        }

        forceSummaryString.append(String.format(resources.getString("dialogDescriptionForces.text"),
                faction,
                personnelCount));

        if (unitMap.get("mek") > 0) {
            forceSummaryString.append(String.format(resources.getString("dialogDescriptionForcesMeks.text"),
                    unitMap.get("mek"),
                    pluralizer(resources, unitMap.get("mek"))));
        }

        if (unitMap.get("fighter") > 0) {
            forceSummaryString.append(String.format(resources.getString("dialogDescriptionForcesFighters.text"),
                    unitMap.get("fighter"),
                    pluralizer(resources, unitMap.get("fighter"))));
        }

        if (unitMap.get("protoMek") > 0) {
            forceSummaryString.append(String.format(resources.getString("dialogDescriptionForcesProtoMechs.text"),
                    unitMap.get("protoMek"),
                    pluralizer(resources, unitMap.get("protoMek"))));
        }

        if (unitMap.get("battleArmor") > 0) {
            forceSummaryString.append(String.format(resources.getString("dialogDescriptionForcesBattleArmor.text"),
                    pluralizer(resources, unitMap.get("battleArmor")),
                    unitMap.get("battleArmor")));
        }

        if (unitMap.get("vehicle") > 0) {
            forceSummaryString.append(String.format(resources.getString("dialogDescriptionForcesVehicles.text"),
                    unitMap.get("vehicle"),
                    pluralizer(resources, unitMap.get("vehicle"))));
        }

        if (unitMap.get("infantry") > 0) {
            forceSummaryString.append(String.format(resources.getString("dialogDescriptionForcesInfantry.text"),
                    pluralizer(resources, unitMap.get("infantry")),
                    unitMap.get("infantry")));
        }

        if (unitMap.get("dropShip") > 0) {
            forceSummaryString.append(String.format(resources.getString("dialogDescriptionForcesDropShips.text"),
                    unitMap.get("dropShip"),
                    pluralizer(resources, unitMap.get("dropShip"))));
        }

        if (unitMap.get("other") > 0) {
            forceSummaryString.append(String.format(resources.getString("dialogDescriptionForcesOther.text"),
                    unitMap.get("other"),
                    pluralizer(resources, unitMap.get("other"))));
        }

        forceSummaryString.append(' ').append(String.format(resources.getString("dialogDescriptionForcesBv.text"), battleValue));

        return forceSummaryString.toString();
    }

    /**
     * This method is used to determine whether a string should be plural based on the given unit count.
     *
     * @param resources The ResourceBundle object containing the string resource for pluralizer text.
     * @param unitCount The number of units.
     * @return The pluralizer text based on the unit count. Returns an empty string if the unit count is less than or equal to 1.
     */
    private static String pluralizer(ResourceBundle resources, int unitCount) {
        String pluralizer = "";

        if ((unitCount > 1) || (unitCount == 0)) {
            pluralizer = resources.getString("dialogDescriptionPluralizer.text");
        }

        return pluralizer;
    }
}