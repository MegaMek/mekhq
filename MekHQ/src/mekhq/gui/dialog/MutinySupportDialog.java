package mekhq.gui.dialog;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
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
        // Initialize a JOptionPane
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
        // buttonPanel.add(Box.createHorizontalGlue());

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

        JDialog dialog = pane.createDialog(null, resources.getString("dialogTitle.text"));
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
                    resources.getString("dialogDescriptionViolentTakeover.text"),
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
     * @param bv the force's estimated Battle Value
     * @param personnelCount the number of personnel supporting the faction
     * @return the formatted summary string
     */
    private static String getForceSummaryString(ResourceBundle resources, HashMap<String, Integer> unitMap, boolean isLoyalist, Integer battleValue, Integer personnelCount) {
        String faction;

        if (isLoyalist) {
            faction = resources.getString("dialogDescriptionLoyalist.text");
        } else {
            faction = resources.getString("dialogDescriptionRebels.text");
        }

        return String.format(resources.getString("dialogDescriptionForces.text"),
                faction,
                unitMap.get("mek"),
                unitMap.get("fighter"),
                unitMap.get("protoMek"),
                unitMap.get("battleArmor"),
                unitMap.get("dropShip"),
                unitMap.get("infantry"),
                unitMap.get("vehicle"),
                unitMap.get("other"),
                personnelCount,
                battleValue);
    }
}