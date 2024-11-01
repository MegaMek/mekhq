/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission.resupplyAndCaches;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.ITechnology;
import megamek.common.Mek;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.apache.commons.math3.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.lang.Math.round;
import static mekhq.campaign.finances.enums.TransactionType.BONUS_EXCHANGE;
import static mekhq.campaign.finances.enums.TransactionType.EQUIPMENT_PURCHASE;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.CRITICAL;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.DOMINATING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.stratcon.StratconContractInitializer.getUnoccupiedCoords;
import static mekhq.campaign.stratcon.StratconRulesManager.generateExternalScenario;
import static mekhq.campaign.stratcon.StratconRulesManager.getRandomTrack;
import static mekhq.campaign.unit.Unit.getRandomUnitQuality;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

public class Resupply {
    final private Campaign campaign;
    final private AtBContract contract;
    final private Faction employerFaction;
    final private Faction enemyFaction;
    private Map<Part, Integer> potentialParts;
    private List<Part> partsPool;

    private List<Unit> potentialUnits;
    private Random random;

    private final int YEAR;
    private final int EMPLOYER_TECH_CODE;
    private final boolean EMPLOYER_IS_CLAN;
    private final Money TARGET_VALUE = Money.of(250000);
    private final LocalDate BATTLE_OF_TUKAYYID = LocalDate.of(3052, 5, 21);
    public final static double RESUPPLY_LOAD_SIZE = 25;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");
    private final static MMLogger logger = MMLogger.create(Resupply.class);

    /**
     * Resupply constructor, initializing the supply drop with a provided campaign and contract.
     * It also sets up this instance with default values, and collects potential parts for the supply
     * drop.
     *
     * @param campaign  The campaign this supply drop is part of.
     * @param contract  The relevant contract.
     * @param skipParts Whether to skip generating parts, if {@code true} this call must be followed
     *                 by {@code setPotentialParts}
     * @param skipUnits Whether to skip generating parts, if {@code true} this call must be followed
     *                 by {@code setPotentialUnits}
     */
    public Resupply(Campaign campaign, AtBContract contract, boolean skipParts, boolean skipUnits) {
        this.campaign = campaign;
        this.contract = contract;
        employerFaction = contract.getEmployerFaction();
        enemyFaction = contract.getEnemy();

        YEAR = campaign.getGameYear();
        EMPLOYER_IS_CLAN = enemyFaction.isClan();
        EMPLOYER_TECH_CODE = getTechFaction(employerFaction);

        if (!skipParts) {
            collectParts();
            partsPool = buildPool(potentialParts);
        }

        if (!skipUnits) {
            potentialUnits = new ArrayList<>();
        }

        random = new Random();
    }

    /**
     * Method to create a supply drop dialog with the given parameters and display it to the user.
     *
     * @param droppedItems A list of items to be dropped.
     * @param cashReward   The cash reward for the supply drop.
     * @param isLoot       A flag indicating whether the drop is considered loot.
     * @param isContractEnd A flag indicating whether the drop is related to the contract end.
     */
    private void supplyDropDialog(List<Part> droppedItems, Money cashReward, boolean isLoot, boolean isContractEnd) {
        supplyDropDialog(droppedItems, null, cashReward, isLoot, isContractEnd);
    }

    /**
     * Overloaded method to additionally support supply drop dialogs that include units.
     *
     * @param droppedItems A list of items to be dropped.
     * @param droppedUnits A list of units to be dropped.
     * @param cashReward   The cash reward for the supply drop.
     * @param isLoot       A flag indicating whether the drop is considered loot.
     * @param isContractEnd A flag indicating whether the drop is related to the contract end.
     */
    private void supplyDropDialog(@Nullable List<Part> droppedItems, @Nullable List<Unit> droppedUnits,
                                  Money cashReward, boolean isLoot, boolean isContractEnd) {
        ImageIcon icon = getFactionLogo(campaign, employerFaction.getShortName(), true);

        if (isLoot || isContractEnd) {
            icon = getFactionLogo(campaign, campaign.getFaction().getShortName(), true);
        }
        icon = scaleImageIconToWidth(icon, 100);

        StringBuilder message = new StringBuilder(getInitialDescription(isLoot, isContractEnd));

        Map<String, Integer> partsReport = new HashMap<>();
        if (droppedItems != null) {
            partsReport = createPartsReport(droppedItems);
        }

        Map<String, Integer> unitsReport = new HashMap<>();
        if (droppedUnits != null) {
            unitsReport = createUnitsReport(droppedUnits);
        }

        List<Entry<String, Integer>> entries = new ArrayList<>(partsReport.entrySet());

        if (!partsReport.isEmpty()) {
            if (!isLoot && !isContractEnd) {
                int rationPacks = 0;
                int medicalSupplies = 0;

                for (Person person : campaign.getActivePersonnel()) {
                    PersonnelRole primaryRole = person.getPrimaryRole();
                    PersonnelRole secondaryRole = person.getSecondaryRole();

                    if (primaryRole.isCombat() || secondaryRole.isCombat()) {
                        rationPacks++;
                    }

                    if (primaryRole.isDoctor() || secondaryRole.isDoctor()) {
                        medicalSupplies++;
                    }
                }

                rationPacks *= (int) Math.ceil((double) campaign.getLocalDate().lengthOfMonth() / 4);

                if (rationPacks > 0) {
                    entries.add(Map.entry(resources.getString("resourcesRations.text"),
                        rationPacks));
                }

                if (medicalSupplies > 0) {
                    entries.add(Map.entry(resources.getString("resourcesMedical.text"),
                        medicalSupplies));
                }
            }
        }

        entries.addAll(unitsReport.entrySet());

        String[] columns = formatColumnData(entries);

        if (!cashReward.isZero()) {
            columns[entries.size() % 3] += "<br> - " + cashReward.toAmountAndSymbolString();
        }

        message.append("<table><tr valign='top'>")
            .append("<td>").append(columns[0]).append("</td>")
            .append("<td>").append(columns[1]).append("</td>")
            .append("<td>").append(columns[2]).append("</td>")
            .append("</tr></table>");

        createResupplyDialog(icon, message.toString(), droppedItems, droppedUnits, cashReward,
            isLoot || isContractEnd, false);
    }

    /**
     * Creates a resupply dialog window with the specified parameters.
     *
     * @param icon            The icon to be displayed in the dialog.
     * @param message         The message to be displayed in the dialog.
     * @param droppedItems    List of items to be dropped. Can be null.
     * @param droppedUnits    List of units to be dropped. Can be null.
     * @param cashReward      The value of the cash reward.
     * @param isLootOrContractEnd {@link Boolean} value representing if the dialog is being created
     *                        as the result of either loot or contract end.
     * @param isSmuggler      {@link Boolean} value representing if the dialog is an offer from a
     *                        smuggler.
     */
    public void createResupplyDialog(ImageIcon icon, String message, @Nullable List<Part> droppedItems,
                                        @Nullable List<Unit> droppedUnits, Money cashReward,
                                        boolean isLootOrContractEnd, boolean isSmuggler) {
        final int DIALOG_WIDTH = 700;
        final int DIALOG_HEIGHT = 500;
        final String title = resources.getString("dialog.title");

        JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        dialog.setSize(UIUtil.scaleForGUI(DIALOG_WIDTH, DIALOG_HEIGHT));
        dialog.setTitle(title);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        ActionListener dialogActionListener = e -> {
            dialog.dispose();

            if (isLootOrContractEnd) {
                deliverDrop(droppedItems, droppedUnits, cashReward);
            } else if (!isSmuggler) {
                processConvoy(droppedItems, droppedUnits, cashReward);
            }
        };

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                dialogActionListener.actionPerformed(null);
            }
        });

        JLabel labelIcon = new JLabel("", SwingConstants.CENTER);
        labelIcon.setIcon(icon);
        labelIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel panel = new JPanel();
        BoxLayout boxlayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(boxlayout);
        panel.add(labelIcon);

        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                UIUtil.scaleForGUI(DIALOG_WIDTH), message));
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(description);

        JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton confirmButton = new JButton(resources.getString("confirmAccept.text"));
        confirmButton.addActionListener(e -> {
            dialog.dispose();
            campaign.getFinances().debit(EQUIPMENT_PURCHASE, campaign.getLocalDate(),
                getSmugglerFee(droppedItems), resources.getString("smugglerFee.text"));
            processSmuggler(droppedItems);
        });

        JButton refuseButton = new JButton(resources.getString("confirmRefuse.text"));
        refuseButton.addActionListener(dialogActionListener);

        JButton okButton = new JButton(resources.getString("confirmReceipt.text"));
        okButton.addActionListener(dialogActionListener);

        if (isSmuggler) {
            buttonPanel.add(confirmButton);
            buttonPanel.add(refuseButton);
        } else {
            buttonPanel.add(okButton);
        }

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    /**
     * Generates and displays a dialog detailing the smuggler's offer.
     * The dialog features a formatted description of the offer, along with a parts report.
     *
     * @param droppedItems The list of items offered by the smuggler.
     */
    private void smugglerOfferDialog(List<Part> droppedItems) {
        ImageIcon icon = Factions.getFactionLogo(campaign, "PIR", true);
        icon = scaleImageIconToWidth(icon, 100);

        StringBuilder message = new StringBuilder(getSmugglerDescription(droppedItems, false));

        Map<String, Integer> partsReport = new HashMap<>();
        if (droppedItems != null) {
            partsReport = createPartsReport(droppedItems);
        }

        List<Entry<String, Integer>> entries = new ArrayList<>(partsReport.entrySet());
        String[] columns = formatColumnData(entries);

        message.append("<table><tr valign='top'>")
            .append("<td>").append(columns[0]).append("</td>")
            .append("<td>").append(columns[1]).append("</td>")
            .append("<td>").append(columns[2]).append("</td>")
            .append("</tr></table>");

        createResupplyDialog(icon, message.toString(), droppedItems, null, Money.zero(),
            false, true);
    }

    /**
     * Processes a convoy by determining interception chances based on morale level and performs
     * actions accordingly.
     *
     * @param droppedItems    List of items dropped by the convoy. Can be {@code null}.
     * @param droppedUnits    List of units dropped by the convoy. Can be {@code null}.
     * @param cashReward      The amount of cash reward.
     */
    private void processConvoy(@Nullable List<Part> droppedItems, @Nullable List<Unit> droppedUnits,
                               Money cashReward) {
        final String STATUS_FORWARD = "statusUpdate";
        final String STATUS_AFTERWARD = ".text";

        AtBMoraleLevel morale = contract.getMoraleLevel();
        int interceptionChance = morale.ordinal();

        boolean isIntercepted = false;
        String message = "";

        if (Compute.randomInt(10) < interceptionChance) {
            message = resources.getString(STATUS_FORWARD + "Intercepted" +
                Compute.randomInt(20) + STATUS_AFTERWARD);
            isIntercepted = true;
        } else if (Compute.randomInt(10) < interceptionChance) {
            if (Compute.d6() == 1) {
                message = resources.getString(STATUS_FORWARD + Compute.randomInt(100)
                    + STATUS_AFTERWARD);
            } else {
                int roll = Compute.randomInt(2);

                if (morale.isAdvancing() || morale.isWeakened()) {
                    morale = roll == 0 ? (morale.isAdvancing() ? DOMINATING : CRITICAL) : STALEMATE;
                }

                message = resources.getString(STATUS_FORWARD + "Enemy" + morale
                    + Compute.randomInt(50) + STATUS_AFTERWARD);
            }
        }

        Integer targetConvoy = null;
        if (contract.getCommandRights().isIndependent()) {
            targetConvoy = getRandomConvoy();

            if (message.isEmpty()) {
                interceptionChance = (int) round((double) interceptionChance / 2);
            }

            if (campaign.getCampaignOptions().isUseFatigue()) {
                increaseFatigue(interceptionChance);
            }
        }

        if (!message.isEmpty()) {
            createConvoyMessage(targetConvoy, droppedItems, droppedUnits, cashReward, message,
                isIntercepted);
        } else {
            campaign.addReport(String.format(resources.getString("convoySuccessful.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            deliverDrop(droppedItems, droppedUnits, cashReward);
        }
    }

    /**
     * Processes a smuggler's offer, including whether the player has been swindled.
     * Swindle chance is based on contract morale.
     * If the player is swindled a follow-up dialog is triggered.
     *
     * @param droppedItems The list of items dropped by smuggler.
     */
    private void processSmuggler(List<Part> droppedItems) {
        AtBMoraleLevel morale = contract.getMoraleLevel();
        int swindleChance = morale.ordinal();

        if (Compute.randomInt(10) < swindleChance) {
            String message = getSmugglerDescription(null, true);
            createSwindledMessage(message);
        } else {
            campaign.addReport(String.format(resources.getString("convoySuccessfulSmuggler.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            deliverDrop(droppedItems, null, Money.zero());
        }
    }

    /**
     * Creates a convoy with provided parameters and a status message to display.
     *
     * @param droppedItems         List of items dropped by the convoy. Can be {@code null}.
     * @param droppedUnits         List of units dropped by the convoy. Can be {@code null}.
     * @param cashReward           The amount of cash reward.
     * @param convoyStatusMessage  The status message to be displayed for the convoy.
     * @param isIntercepted        {@link Boolean} indicating if the convoy has been intercepted.
     */
    public void createConvoyMessage(@Nullable Integer targetConvoy, @Nullable List<Part> droppedItems,
                                    @Nullable List<Unit> droppedUnits, Money cashReward,
                                    String convoyStatusMessage, boolean isIntercepted) {
        createConvoyMessage(targetConvoy, droppedItems, droppedUnits, cashReward, convoyStatusMessage,
            isIntercepted, true);
    }

    /**
     * Creates a convoy with provided parameters, status message to display and introduction option.
     *
     * @param droppedItems         List of items dropped by the convoy. Can be {@code null}.
     * @param droppedUnits         List of units dropped by the convoy. Can be {@code null}.
     * @param cashReward           The amount of cash reward.
     * @param convoyStatusMessage  The status message to be displayed for the convoy.
     * @param isIntercepted        {@link Boolean} indicating if the convoy has been intercepted.
     * @param isIntroduction       {@link Boolean} if this dialog is an introduction (i.e., the player
     *                             being informed that they have a message), or whether it's the
     *                             message that follows - informing the player of the message's nature.
     */
    public void createConvoyMessage(@Nullable Integer targetConvoy, @Nullable List<Part> droppedItems,
                                    @Nullable List<Unit> droppedUnits, Money cashReward,
                                    String convoyStatusMessage, boolean isIntercepted,
                                    boolean isIntroduction) {
        boolean isIndependent = contract.getCommandRights().isIndependent();
        StratconTrackState track = getRandomTrack(contract);

        StratconCoords convoyGridReference;
        if (track != null) {
            convoyGridReference = getUnoccupiedCoords(track);
        } else {
            convoyGridReference = null;
        }

        // Dialog dimensions and representative
        final int DIALOG_WIDTH = 400;
        final int DIALOG_HEIGHT = 200;

        // Creates and sets up the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("dialog.title"));
        dialog.setLayout(new BorderLayout());
        dialog.setSize(UIUtil.scaleForGUI(DIALOG_WIDTH, DIALOG_HEIGHT));
        dialog.setLocationRelativeTo(null);

        // Defines the action when the dialog is being dismissed
        ActionListener dialogDismissActionListener = e -> {
            dialog.dispose();
            if (isIntroduction) {
                createConvoyMessage(targetConvoy, droppedItems, droppedUnits, cashReward,
                    convoyStatusMessage, isIntercepted, false);
            } else {
                if (isIntercepted) {
                    if (campaign.getCampaignOptions().isUseStratCon()) {
                        processConvoyInterception(droppedItems, droppedUnits, cashReward,
                            isIndependent, targetConvoy, track, convoyGridReference);
                    } else {
                        campaign.addReport(String.format(resources.getString("convoyInterceptedAtB.text"),
                            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                            CLOSING_SPAN_TAG));
                    }
                } else {
                    campaign.addReport(String.format(resources.getString("convoySuccessful.text"),
                        spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                        CLOSING_SPAN_TAG));
                    deliverDrop(droppedItems, droppedUnits, cashReward);
                }
            }
        };

        // Associates the dismiss action to the dialog window close event
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                dialogDismissActionListener.actionPerformed(null);
            }
        });

        // Prepares and adds the icon of the representative as a label
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(JLabel.CENTER);

        Person logisticsOfficer = pickLogisticsRepresentative();
        ImageIcon speakerIcon = getSpeakerIcon(targetConvoy, isIntroduction, logisticsOfficer);
        speakerIcon = scaleImageIconToWidth(speakerIcon, UIUtil.scaleForGUI(100));
        iconLabel.setIcon(speakerIcon);
        dialog.add(iconLabel, BorderLayout.NORTH);

        // Prepares and adds the description
        String message = convoyStatusMessage;
        String speaker = null;
        if (isIntroduction) {
            message = String.format(resources.getString("logisticsMessage.text"),
                getCommanderTitle(campaign, false)) + "<br>";

            if (logisticsOfficer != null) {
                speaker = logisticsOfficer.getFullTitle();
            } else {
                speaker = String.format(resources.getString("dialogBorderCampaignSpeaker.text"),
                    campaign.getName());
            }
        } else {
            if (isIntercepted) {
                String coords = resources.getString("static.text");
                if (convoyGridReference != null) {
                    coords = convoyGridReference.toBTString();
                }

                String sector = resources.getString("hiss.text");
                if (track != null) {
                    sector = track.getDisplayableName();
                }

                message = String.format(message, getCommanderTitle(campaign, false),
                    sector, coords);
            } else {
                message = String.format(message, getCommanderTitle(campaign, false));
            }

            if (contract.getCommandRights().isIndependent()) {
                if (targetConvoy != null) {
                    speaker = getConvoySpeaker(targetConvoy);
                }

                if (speaker == null) {
                    speaker = String.format(resources.getString("dialogBorderConvoySpeakerDefault.text"),
                        campaign.getName());
                }
            } else {
                speaker = String.format(resources.getString("dialogBorderConvoySpeakerDefault.text"),
                    contract.getEmployer());
            }
        }

        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
            UIUtil.scaleForGUI(DIALOG_WIDTH), message));
        description.setHorizontalAlignment(JLabel.CENTER);
        dialog.add(description, BorderLayout.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
            String.format(resources.getString("dialogBorderTitle.text"), speaker)));
        descriptionPanel.add(description);
        dialog.add(descriptionPanel, BorderLayout.CENTER);

        // Prepares and adds the confirm button
        JButton confirmButton = new JButton(resources.getString("logisticsPatch.text"));
        if (!isIntroduction) {
            if (isIntercepted) {
                confirmButton.setText(resources.getString("logisticsDestroyed.text"));
            } else {
                confirmButton.setText(resources.getString("logisticsReceived.text"));
            }
        }
        confirmButton.addActionListener(dialogDismissActionListener);
        dialog.add(confirmButton,  BorderLayout.SOUTH);

        // Pack, position and display the dialog
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    @Nullable
    private ImageIcon getSpeakerIcon(Integer targetConvoy, boolean isIntroduction, Person logisticsOfficer) {
        ImageIcon speakerIcon = null;
        if (isIntroduction) {
            if (logisticsOfficer == null) {
                speakerIcon = getFactionLogo(campaign, campaign.getFaction().getShortName(),
                    true);
            } else {
                speakerIcon = logisticsOfficer.getPortrait().getImageIcon();
            }
        } else {
            if (contract.getCommandRights().isIndependent()) {
                speakerIcon = getIndependentIconLabel(targetConvoy);
            }

            if (speakerIcon == null) {
                if (contract.getCommandRights().isIndependent()) {
                    speakerIcon = getFactionLogo(campaign, campaign.getFaction().getShortName(),
                        true);
                } else {
                    speakerIcon = getFactionLogo(campaign, employerFaction.getShortName(),
                        true);
                }
            }
        }
        return speakerIcon;
    }

    /**
     * Generates and displays the 'swindled' follow-up dialog.
     *
     * @param message The text message to be displayed in the dialog detailing the swindle event.
     */
    public void createSwindledMessage(String message) {
        // Dialog dimensions and representative
        final int DIALOG_WIDTH = 400;
        final int DIALOG_HEIGHT = 200;

        // Creates and sets up the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("dialog.title"));
        dialog.setLayout(new BorderLayout());
        dialog.setSize(UIUtil.scaleForGUI(DIALOG_WIDTH, DIALOG_HEIGHT));
        dialog.setLocationRelativeTo(null);

        // Prepares and adds the icon of the representative as a label
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(JLabel.CENTER);

        ImageIcon factionLogo = getFactionLogo(campaign, "PIR", true);
        factionLogo = scaleImageIconToWidth(factionLogo, UIUtil.scaleForGUI(100));
        iconLabel.setIcon(factionLogo);
        dialog.add(iconLabel, BorderLayout.NORTH);

        // Prepares and adds the description
        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
            UIUtil.scaleForGUI(DIALOG_WIDTH), message));
        description.setHorizontalAlignment(JLabel.CENTER);
        dialog.add(description, BorderLayout.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
            String.format(resources.getString("dialogBorderTitle.text"), "")));
        descriptionPanel.add(description);
        dialog.add(descriptionPanel, BorderLayout.CENTER);

        // Prepares and adds the confirm button
        JButton confirmButton = new JButton(resources.getString("logisticsDestroyed.text"));
        confirmButton.addActionListener(e -> dialog.dispose());
        dialog.add(confirmButton,  BorderLayout.SOUTH);

        // Pack, position and display the dialog
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Increases the fatigue convoy crews.
     *
     * @param targetConvoy The id of the convoy whose crew's fatigue should be increased.
     */
    private void increaseFatigue(Integer targetConvoy) {
        if (targetConvoy != null) {
            Force convoy = campaign.getForce(targetConvoy);

            if (convoy != null) {
                for (UUID unitId : convoy.getUnits()) {
                    Unit unit = campaign.getUnit(unitId);

                    if (unit != null) {
                        for (Person crewMember : unit.getCrew()) {
                            crewMember.increaseFatigue(1);
                        }
                    }
                }
            }
        }
    }

    /**
     * Scales an {@link ImageIcon} to the specified width while maintaining its aspect ratio.
     *
     * @param icon  The {@link ImageIcon} to be scaled.
     * @param width The desired width.
     * @return The scaled {@link ImageIcon}.
     */
    static ImageIcon scaleImageIconToWidth(ImageIcon icon, int width) {
        int height = (int) Math.ceil((double) width * icon.getIconHeight() / icon.getIconWidth());
        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    /**
     * Retrieves the speaker of the convoy.
     *
     * @param targetConvoy    The relevant convoy.
     * @return The speaker of the convoy.
     */
    private String getConvoySpeaker(Integer targetConvoy) {
        Force convoy = campaign.getForce(targetConvoy);

        String speaker = null;

        if (convoy != null) {
            UUID convoyCommanderId = convoy.getForceCommanderID();

            if (convoyCommanderId != null) {
                Person convoyCommander = campaign.getPerson(convoyCommanderId);

                if (convoyCommander != null) {
                    speaker = convoyCommander.getFullTitle();
                }
            }
        }

        if (convoy != null) {
            speaker = speaker + ", " + convoy.getName();
        } else {
            speaker = String.format(resources.getString("dialogBorderConvoySpeakerDefault.text"),
                campaign.getName());
        }
        return speaker;
    }

    /**
     * Retrieves the icon of the convoy. This should only be used when the contract has Independent
     * command level (i.e., if the convoy is player owned).
     *
     * @param targetConvoy The convoy target.
     * @return The {@link ImageIcon} of the convoy.
     */
    @Nullable
    private ImageIcon getIndependentIconLabel(Integer targetConvoy) {
        if (targetConvoy != null) {
            Force convoy = campaign.getForce(targetConvoy);

            if (convoy != null) {
                UUID convoyCommanderId = convoy.getForceCommanderID();

                if (convoyCommanderId != null) {
                    Person convoyCommander = campaign.getPerson(convoyCommanderId);

                    if (convoyCommander != null) {
                        return convoyCommander.getPortrait().getImageIcon();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Processes the interception of a convoy.
     *
     * @param droppedItems         List of items dropped by the convoy. Can be {@link null}.
     * @param droppedUnits         List of units dropped by the convoy. Can be {@link null}.
     * @param cashReward           The amount of cash reward.
     * @param isIndependent        Boolean indicating if contract command level is independent.
     * @param targetConvoy         The target convoy.
     * @param track                The track reference for the convoy's location.
     * @param convoyGridReference  The grid reference for the convoy's location.
     */
    private void processConvoyInterception(List<Part> droppedItems, List<Unit> droppedUnits,
                                           Money cashReward, boolean isIndependent, Integer targetConvoy,
                                           StratconTrackState track, StratconCoords convoyGridReference) {
        String templateAddress = "data/scenariotemplates/Emergency Convoy Defense.xml";

        if (isIndependent) {
            templateAddress = "data/scenariotemplates/Emergency Convoy Defense - Independent.xml";
        }
        ScenarioTemplate template = ScenarioTemplate.Deserialize(templateAddress);

        if (template == null) {
            campaign.addReport(String.format(resources.getString("convoyErrorTemplate.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                templateAddress, CLOSING_SPAN_TAG));
            deliverDrop(droppedItems, droppedUnits, cashReward);
            return;
        }

        if (track == null) {
            campaign.addReport(String.format(resources.getString("convoyErrorTracks.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                templateAddress, CLOSING_SPAN_TAG));
            deliverDrop(droppedItems, droppedUnits, cashReward);
            return;
        }
        StratconScenario scenario = generateExternalScenario(campaign, contract, track, convoyGridReference, template);

        // If we successfully generated a scenario, we need to make a couple of final
        // adjustments, including assigning the Resupply contents as loot and
        // assigning a player convoy (if appropriate)
        if (scenario != null) {
            AtBDynamicScenario backingScenario = scenario.getBackingScenario();
            backingScenario.setDate(campaign.getLocalDate());

            if (targetConvoy == null) {
                campaign.addReport(String.format(resources.getString("convoyErrorPlayerConvoy.text"),
                    spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                    templateAddress, CLOSING_SPAN_TAG));
                deliverDrop(droppedItems, droppedUnits, cashReward);
                return;
            }

            backingScenario.addForce(targetConvoy, "Player");
            campaign.getForce(targetConvoy).setScenarioId(backingScenario.getId(), campaign);
            scenario.commitPrimaryForces();

            Loot loot = new Loot();

            if (droppedItems != null) {
                for (Part part : droppedItems) {
                    loot.addPart(part);
                }
            }

            if (droppedUnits != null) {
                for (Unit unit : droppedUnits) {
                    loot.addUnit(unit.getEntity());
                }
            }

            if (!cashReward.isZero()) {
                loot.setCash(cashReward);
            }

            backingScenario.addLoot(loot);

            // Announce the situation to the player
            campaign.addReport(String.format(resources.getString("convoyInterceptedStratCon.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));
        } else {
            // If we failed to generate a scenario, for whatever reason, we don't
            // want the player confused why there isn't a scenario, so we offer
            // this fluffy response.
            campaign.addReport(String.format(resources.getString("convoyDestroyed.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));
        }
    }

    /**
     * Picks a random convoy from available ones.
     *
     * @return The ID of the randomly selected convoy.
     */
    @Nullable
    private Integer getRandomConvoy() {
        // First, we gather a set of all forces that are already deployed to a track, so we can
        // eliminate them later in the next step
        Set<Integer> forcesInTracks = campaign.getActiveAtBContracts().stream()
            .flatMap(contract -> contract.getStratconCampaignState().getTracks().stream())
            .flatMap(track -> track.getAssignedForceCoords().keySet().stream())
            .collect(Collectors.toSet());

        // Then, we build a list of all valid convoys.
        List<Integer> validConvoys = new ArrayList<>();
        for (Integer key : campaign.getLances().keySet()) {
            Force force = campaign.getForce(key);
            if (force != null
                && !force.isDeployed()
                && force.isConvoyForce()
                && !forcesInTracks.contains(force.getId())
                && force.getSubForces().isEmpty()) {
                validConvoys.add(force.getId());
            }
        }

        // Then we return the chosen Force ID
        if (validConvoys.isEmpty()) {
            return null;
        } else {
            int randomIndex = random.nextInt(validConvoys.size());
            return validConvoys.get(randomIndex);
        }
    }

    /**
     * Creates the final message dialog for the convoy.
     *
     * @param campaign        The current campaign.
     * @param employerFaction The employing faction.
     */
    public static void convoyFinalMessageDialog(Campaign campaign, Faction employerFaction) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

        // Dialog dimensions and representative
        final int DIALOG_WIDTH = 400;
        final int DIALOG_HEIGHT = 200;

        // Creates and sets up the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("dialog.title"));
        dialog.setLayout(new BorderLayout());
        dialog.setSize(UIUtil.scaleForGUI(DIALOG_WIDTH, DIALOG_HEIGHT));
        dialog.setLocationRelativeTo(null);

        // Prepares and adds the icon of the representative as a label
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        iconLabel.setIcon(Factions.getFactionLogo(campaign, employerFaction.getShortName(),
            true));
        dialog.add(iconLabel, BorderLayout.NORTH);

        // Prepares and adds the description
        String convoyStatusMessage = resources.getString("statusUpdateAbandoned"
            + Compute.randomInt(20) + ".text");
        String message = String.format(convoyStatusMessage, getCommanderTitle(campaign, false));

        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
            UIUtil.scaleForGUI(DIALOG_WIDTH), message));
        description.setHorizontalAlignment(JLabel.CENTER);
        dialog.add(description, BorderLayout.CENTER);

        // Prepares and adds the confirm button
        JButton confirmButton = new JButton(resources.getString("logisticsPatch.text"));
        confirmButton.setText(resources.getString("logisticsDestroyed.text"));
        confirmButton.addActionListener(e -> dialog.dispose());
        dialog.add(confirmButton,  BorderLayout.SOUTH);

        // Pack, position and display the dialog
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Picks a logistics representative from available personnel.
     *
     * @return The chosen logistics representative.
     */
    @Nullable
    private Person pickLogisticsRepresentative() {
        Person highestRankedCharacter = null;

        for (Person person : campaign.getLogisticsPersonnel()) {
            if (highestRankedCharacter == null) {
                highestRankedCharacter = person;
                continue;
            }

            if (person.outRanksUsingSkillTiebreaker(campaign, highestRankedCharacter)) {
                highestRankedCharacter = person;
            }
        }

        return highestRankedCharacter;
    }

    /**
     * Retrieves the commander's title.
     *
     * @param campaign       The current campaign.
     * @param includeSurname {@link Boolean} indicating if the surname is to be included.
     * @return The title of the commander.
     */
    static String getCommanderTitle(Campaign campaign, boolean includeSurname) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");
        String placeholder = resources.getString("commander.text");
        Person commander = campaign.getFlaggedCommander();

        if (commander == null) {
            return placeholder;
        }

        String rank = commander.getRankName();
        String title = (rank == null || rank.contains("None")) ? "" : rank;

        return (includeSurname) ? title + ' ' + commander.getSurname() : title;
    }

    /**
     * This method is responsible for delivering the actual supply drop and updating the campaign's
     * inventory and finances.
     *
     * @param droppedItems A list of items to be dropped. Can be {@code null} if no items are to be
     *                    dropped.
     * @param droppedUnits A list of units to be dropped. Can be {@code null} if no units are to be
     *                    dropped.
     * @param cashReward   The cash reward to be added to the finance ledger.
     */
    private void deliverDrop(@Nullable List<Part> droppedItems, @Nullable List<Unit> droppedUnits,
                             Money cashReward) {
        if (droppedItems != null) {
            for (Part part : droppedItems) {
                if (part instanceof AmmoBin) {
                    campaign.getQuartermaster().addAmmo(((AmmoBin) part).getType(), ((AmmoBin) part).getFullShots());
                } else if (part instanceof Armor) {
                    int quantity = (int) Math.floor(((Armor) part).getArmorPointsPerTon() * 5);
                    ((Armor) part).setAmount(quantity);
                    campaign.getWarehouse().addPart(part);
                } else {
                    campaign.getWarehouse().addPart(part);
                }
            }
        }

        if (droppedUnits != null) {
            for (Unit unit : droppedUnits) {
                campaign.addNewUnit(unit.getEntity(), false, 0);
            }
        }

        if (!cashReward.isZero()) {
            campaign.getFinances().credit(BONUS_EXCHANGE, campaign.getLocalDate(), cashReward,
                resources.getString("transactionReason.text"));
        }
    }

    /**
     * This function formats parts into column data for display purposes.
     *
     * @param  partsReport A list of parts.
     * @return An array of strings representing columns for display.
     */
    public String[] formatColumnData(List<Entry<String, Integer>> partsReport) {
        String[] columns = new String[3];
        Arrays.fill(columns, "");

        int i = 0;
        for (Entry<String, Integer> entry : partsReport) {
            columns[i % 3] += "<br> - " + entry.getKey() + " x" + entry.getValue();
            i++;
        }

        return columns;
    }

    /**
     * Method that returns the drop weight of a given part. The weight is determined based on the type
     * of part, with "MissingPart" instances given a higher weight.
     *
     * @param part Part object whose weight is to be determined.
     * @return int representing the weight of the part.
     */
    static int getDropWeight(Part part) {
        int weight = 1;

        if (part instanceof MissingPart) {
            return weight * 5;
        } else {
            return weight;
        }
    }

    /**
     * Populates the 'potentialParts' instance variable with parts obtained from provided units. The
     * parts are filtered based on their salvage status, year of availability, unit type, whether the
     * unit is extinct, and before the battle of Tukayyid Inner Sphere factions are unwilling to share
     * Clan Tech.
     */
    private void collectParts() {
        final Collection<Unit> units = campaign.getUnits();
        potentialParts = new HashMap<>();

        try {
            for (Unit unit : units) {
                Entity entity = unit.getEntity();

                if (entity.isLargeCraft() || entity.isSuperHeavy() || entity.isConventionalInfantry()) {
                    continue;
                }

                if (!unit.isSalvage() && unit.isAvailable()) {
                    List<Part> parts = unit.getParts();
                    for (Part part : parts) {
                        if (part instanceof MekLocation) {
                            if (((MekLocation) part).getLoc() == Mek.LOC_CT) {
                                continue;
                            // If the unit itself is extinct, it's impossible to find replacement locations
                            } else if (unit.isExtinct(YEAR, EMPLOYER_IS_CLAN, EMPLOYER_TECH_CODE)) {
                                continue;
                            }
                        }

                        if (part instanceof TankLocation) {
                            continue;
                        }

                        // If the individual part is extinct, it's impossible to find
                        if (part.isExtinct(YEAR, EMPLOYER_IS_CLAN, EMPLOYER_TECH_CODE)) {
                            continue;
                        }

                        // Prior to the Battle of Tukayyid IS factions are unlikely to be willing to
                        // share Clan Tech
                        if (part.isClan() || part.isMixedTech()) {
                            if (!employerFaction.isClan()) {
                                if (campaign.getLocalDate().isBefore(BATTLE_OF_TUKAYYID)) {
                                    continue;
                                }
                            }
                        }

                        Pair<Unit, Part> pair = new Pair<>(unit, part);
                        int weight = getDropWeight(pair.getValue());
                        potentialParts.merge(part, weight, Integer::sum);
                    }
                }
            }
        } catch (Exception exception) {
            logger.error("Aborted parts collection.", exception);
        }
    }

    /**
     * Populates the 'partsPool' instance variable with parts from 'potentialParts' by cloning the
     * {@code HashMap}, thus ensuring that the pool remains independent of future changes to the
     * potential parts {@code HashMap}.
     */
    static List<Part> buildPool(Map<Part, Integer> potentialParts) {
        List<Part> partsPool = new ArrayList<>(potentialParts.keySet());

        if (!partsPool.isEmpty()) {
            Collections.shuffle(partsPool);
        }

        return partsPool;
    }

    /**
     * Retrieves a part from the parts pool by picking one at random, creating a clone of it and
     * adjusting some properties of the cloned part to fit the requirements of a supply drop. The
     * cloned part is then returned.
     *
     * @return A {@link Part} object representing the selected part from pool. {@code Null} is returned
     * if cloning fails.
     */
    private Part getPart() {
        Part sourcePart = partsPool.get(random.nextInt(partsPool.size()));
        Part clonedPart = sourcePart.clone();

        if (clonedPart == null) {
            logger.error(String.format("Failed to clone part: %s", sourcePart));
            logger.error(String.format(sourcePart.getName()));
            return null;
        }

        try {
            clonedPart.fix();
        } catch (Exception e) {
            clonedPart.setHits(0);
        }

        if (!(clonedPart instanceof AmmoBin)) {
            clonedPart.setQuality(getRandomPartQuality(0));
        }

        clonedPart.setBrandNew(true);
        clonedPart.setOmniPodded(false);

        return clonedPart;
    }

    /**
     * This method initiates to generate the supply drop parts and display them on the dialog.
     * The ‘dropCount’ parameter determines the number of times the part generation loop runs.
     *
     * @param dropCount Number of times the part-generation loop must run.
     */
    public void getResupplyParts(int dropCount) {
        getResupplyParts(dropCount, false, false);
    }

    /**
     * Overloaded method that also allows supply drops to consider whether the drops are classified
     * as 'loot'.
     *
     * @param dropCount Number of times the part-generation loop must run.
     * @param isLoot    Boolean that flags whether drops are classified as loot.
     */
    public void getResupplyParts(int dropCount, boolean isLoot) {
        getResupplyParts(dropCount, isLoot, false);
    }

    /**
     * Overloaded method that also allows supply drops to consider whether the drops are at the end
     * of the contract.
     *
     * @param dropCount Number of times the part-generation loop must run.
     * @param isLoot    Boolean that flags whether drops are classified as loot. Should be set to
     * {@code false} if this is being generated at the end of a contract.
     * @param isContractEnd Boolean that flags whether drops are at the end of a contract.
     */
    public void getResupplyParts(int dropCount, boolean isLoot, boolean isContractEnd) {
        List<Part> droppedItems = new ArrayList<>();
        Money cashReward = Money.zero();

        for (int i = 0; i < dropCount; i++) {
            Money runningTotal = Money.zero();

            while (runningTotal.isLessThan(TARGET_VALUE)) {
                if (partsPool.isEmpty()) {
                    cashReward = cashReward.plus(TARGET_VALUE);
                    runningTotal = cashReward.plus(TARGET_VALUE);
                    continue;
                }

                Part potentialPart = getPart();

                if (potentialPart == null) {
                    continue;
                }

                runningTotal = runningTotal.plus(potentialPart.getUndamagedValue());
                droppedItems.add(potentialPart);
            }
        }

        logger.info(droppedItems.toString());

        if (contract.getContractType().isGuerrillaWarfare() && !(isLoot || isContractEnd)) {
            if (!droppedItems.isEmpty()) {
                smugglerOfferDialog(droppedItems);
            }
        } else {
            supplyDropDialog(droppedItems, cashReward, isLoot, isContractEnd);
        }
    }

    /**
     * This method creates a mapping report based on the provided dropped items. The report indicates
     * the types of parts that have been dropped, the quantity of each type of part, and makes
     * special note of those parts that are considered 'extinct' according to the year and the origin
     * faction.
     *
     * @param droppedItems List of {@link Part} objects that were included in the supply drop. Can be
     * {@code null}.
     * @return A map containing the dropped parts and their quantities.
     */
    public Map<String, Integer> createPartsReport(@Nullable List<Part> droppedItems) {
        int year = campaign.getGameYear();
        Faction originFaction = campaign.getFaction();

        return droppedItems.stream()
            .collect(Collectors.toMap(
                part -> {
                    String name = part.getName();

                    String append = part.isClan() ? " (Clan)" : "";
                    append = part.isMixedTech() ? " (Mixed)" : append;
                    append += part.isExtinct(year, originFaction.isClan(), getTechFaction(originFaction)) ?
                        " (<b>Extinct</b>)" : "";

                    if (part instanceof AmmoBin) {
                        return ((AmmoBin) part).getType().getName() + append;
                    } else if (part instanceof MekLocation) {
                        return name + " (" + part.getUnitTonnage() + "t)" + append;
                    } else {
                        return name + append;
                    }
                },
                part -> {
                    if (part instanceof AmmoBin) {
                        return ((AmmoBin) part).getFullShots();
                    } else if (part instanceof Armor) {
                        return (int) Math.floor(((Armor) part).getArmorPointsPerTon() * 5);
                    } else {
                        return 1;
                    }
                },
                Integer::sum));
    }

    /**
     * The unit based counterpart of {@code createPartsReport()} - instead of parts, it makes a
     * similar report based on unit objects.
     *
     * @param droppedUnits List of {@link Unit} objects that were included in the supply drop. Can be
     * {@code null}.
     * @return A map containing the dropped units and their quantities.
     */
    public Map<String, Integer> createUnitsReport(@Nullable List<Unit> droppedUnits) {
        int year = campaign.getGameYear();
        Faction originFaction = campaign.getFaction();

        return droppedUnits.stream()
            .collect(Collectors.toMap(
                unit -> {
                    String append = unit.isClan() ? " (Clan)" : "";
                    append = unit.isMixedTech() ? " (Mixed)" : append;
                    append += unit.isExtinct(year, originFaction.isClan(), getTechFaction(originFaction)) ?
                        " (<b>Extinct</b>)" : "";

                    return unit.getName() + " (" + unit.getQualityName() + ')' + append;
                },
                part -> 1,
                Integer::sum));
    }

    /**
     * Retrieves the faction-specific tech level.
     *
     * @param faction The target {@link Faction} to get the technology level of.
     * @return An integer representing the faction's technology level.
     */
    int getTechFaction(Faction faction) {
        for (int i = 0; i < ITechnology.MM_FACTION_CODES.length; i++) {
            if (ITechnology.MM_FACTION_CODES[i].equals(faction.getShortName())) {
                return i;
            }
        }

        logger.warn("Unable to retrieve Tech Faction. Using fallback.");

        if (faction.isClan()) {
            for (int i = 0; i < ITechnology.MM_FACTION_CODES.length; i++) {
                if (ITechnology.MM_FACTION_CODES[i].equals("CLAN")) {
                    return i;
                }
            }
        } else if (faction.isInnerSphere()) {
            for (int i = 0; i < ITechnology.MM_FACTION_CODES.length; i++) {
                if (ITechnology.MM_FACTION_CODES[i].equals("IS")) {
                    return i;
                }
            }
        }

        logger.error("Fallback failed. Using 0 (IS)");
        return 0;
    }

    /**
     * This method generates a description for the supply drop, with text selection based on whether
     * the drop is considered loot, whether it's tied to a contract ending or it's tied to a specific
     * morale and contract type situation. It fetches a specific string from the resource bundle via
     * a key that's built dynamically based on these factors.
     *
     * @param isLoot         Boolean flag indicating whether the supply drop is considered as 'loot'.
     *                      This should be set to {@code false} if the supply drop is being made at
     *                      the end of the contract.
     * @param isContractEnd  Boolean flag indicating whether the supply drop occurs at the end of a contract.
     * @return               A {@link String} message picked from the resources, based on the given
     * input parameters.
     */
    private String getInitialDescription(boolean isLoot, boolean isContractEnd) {
        if (isLoot) {
            return resources.getString("salvaged" + Compute.randomInt(10) + ".text");
        }

        if (isContractEnd) {
            return resources.getString("looted" + Compute.randomInt(10) + ".text");
        }

        AtBMoraleLevel morale = contract.getMoraleLevel();
        return resources.getString(morale.toString().toLowerCase() + "Supplies"
                + Compute.randomInt(20) + ".text");
    }
    /**
     * Retrieves a formatted string indended for Smuggler dialogs.
     *
     * @param droppedItems List of items offered by the smuggler. Can be {@code null} if
     *                     {@code wasSwindled} is {@code true}.
     * @param wasSwindled  Boolean indicating if the player has been swindled.
     * @return A formatted string representing the smuggler's address to the player.
     */
    private String getSmugglerDescription(@Nullable List<Part> droppedItems, boolean wasSwindled) {
        String address = resources.getString("guerrillaAddressGeneric.text");

        Person commander = campaign.getFlaggedCommander();
        if (commander != null) {
            if (commander.getGender().isFemale()) {
                address = resources.getString("guerrillaAddressFemale.text");
            }

            if (commander.getGender().isMale()) {
                address = resources.getString("guerrillaAddressMale.text");
            }
        }

        String enemyFactionReference = enemyFaction.getFullName(campaign.getGameYear());
        if (!enemyFactionReference.contains("Clan")) {
            enemyFactionReference = "the " + enemyFactionReference;
        }

        if (wasSwindled) {
            return String.format(
                resources.getString("guerrillaSwindled" + Compute.randomInt(25) + ".text"),
                address, enemyFactionReference);
        } else {
            Money value = getSmugglerFee(droppedItems);

            return String.format(
                resources.getString("guerrillaSupplies" + Compute.randomInt(25) + ".text"),
                address, enemyFactionReference, value.toAmountAndSymbolString());
        }
    }

    /**
     * Calculates the smuggler's fee for a list of dropped items.
     * The fee is 2 times the total actual value of all items.
     *
     * @param droppedItems The list of items dropped by smuggler.
     * @return The calculated smuggler's fee as a {@link Money} object.
     */
    private static Money getSmugglerFee(List<Part> droppedItems) {
        Money value = Money.zero();
        for (Part part : droppedItems) {
            value = value.plus(part.getActualValue());
        }

        value = value.multipliedBy(2);
        return value;
    }

    /**
     * This method generates a randomized {@link PartQuality} entry. The randomness is adjusted by
     * the modifier. The result describes the quality of a part that's delivered in the supply drop
     * - it could range from poor to excellent, modelled in the {@link PartQuality} enum. Note that
     * it uses the imported static {@code getRandomUnitQuality()} method.
     *
     * @param  modifier An integer that will be used to adjust the range or distribution of the random
     *                 part quality that's returned.
     * @return          A {@link PartQuality} object representing the quality of a part.
     */
    static PartQuality getRandomPartQuality(int modifier) {
        return getRandomUnitQuality(modifier);
    }

    /**
     * Triggers a dialog window providing convoy related information meant to be triggered at the
     * beginning of an Independent contract.
     *
     * @param campaign The current campaign.
     * @param contract The relevant contract.
     */
    public static void triggerConvoyDialog(Campaign campaign, AtBContract contract) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

        // Retrieves the title from the resources
        String title = resources.getString("dialog.title");

        // An ImageIcon to hold the faction icon
        ImageIcon icon = getFactionLogo(campaign, campaign.getFaction().getShortName(), true);

        // Format the HTML message
        int maximumResupplySize = (int) Math.max(1, Math.floor((double) contract.getRequiredLances() / 3));

        String message = String.format("<html><i><div style='width: %s; text-align:center;'>%s</div></i></html>",
            UIUtil.scaleForGUI(500),
            String.format(resources.getString("convoyMessage.text"),
                getCommanderTitle(campaign, false),
                maximumResupplySize * RESUPPLY_LOAD_SIZE, maximumResupplySize,
                maximumResupplySize > 1 ? "s" : ""));

        // Create a text pane to display the message
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(message);
        textPane.setEditable(false);

        // Create a panel to display the icon and the message
        JPanel panel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(icon);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(textPane, BorderLayout.SOUTH);

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());

        // Create an accept button and add its action listener.
        // When clicked, it will set the result to true and close the dialog
        JButton acceptButton = new JButton(resources.getString("convoyConfirm.text"));
        acceptButton.addActionListener(e -> dialog.dispose());

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(acceptButton);

        // Add the original panel and button panel to the dialog
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);
        dialog.setVisible(true);
    }
}
