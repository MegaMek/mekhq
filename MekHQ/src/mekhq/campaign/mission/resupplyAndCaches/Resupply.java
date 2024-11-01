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
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;
import static megamek.common.ITechnology.RATING_A;
import static megamek.common.ITechnology.RATING_C;
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
    private List<Part> partsPool;
    private Random random;
    private boolean usePlayerConvoy;
    private int convoyCount;
    private double totalCargoCapacity;
    private int negotiationModifier;

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
     */
    public Resupply(Campaign campaign, AtBContract contract) {
        this.campaign = campaign;
        this.contract = contract;
        employerFaction = contract.getEmployerFaction();
        enemyFaction = contract.getEnemy();
        usePlayerConvoy = contract.getCommandRights().isIndependent();

        YEAR = campaign.getGameYear();
        EMPLOYER_IS_CLAN = enemyFaction.isClan();
        EMPLOYER_TECH_CODE = getTechFaction(employerFaction);

        partsPool = buildPool(collectParts());
        random = new Random();

        calculateNegotiationModifier();
        calculatePlayerConvoyValues();
    }

    /**
     * @return {@code true} if the player's convoy is being used, {@code false} otherwise.
     */
    public boolean isUsePlayerConvoy() {
        return usePlayerConvoy;
    }

    private void calculateNegotiationModifier() {
        Person negotiator;
        negotiationModifier = -4;

        if (contract.getContractType().isGuerrillaWarfare()) {
            negotiator = campaign.getFlaggedCommander();
        } else {
            negotiator = null;

            for (Person admin : campaign.getAdmins()) {
                if (admin.getPrimaryRole().isAdministratorLogistics()
                    || admin.getSecondaryRole().isAdministratorLogistics()) {
                    if (negotiator == null
                        || (admin.outRanksUsingSkillTiebreaker(campaign, negotiator))) {
                        negotiator = admin;
                    }
                }
            }
        }

        if (negotiator != null) {
            Skill skill = negotiator.getSkill(SkillType.S_NEG);

            if (skill != null) {
                negotiationModifier = Math.min(3, skill.getFinalSkillValue() - 3);
            }
        }
    }

    /**
     * Displays a dialog depicting a resupply of parts and possibly cash.
     *
     * @param droppedItems The items being dropped.
     * @param cashReward The monetary reward from the supply drop.
     * @param isLoot A boolean indicating if the supply drop is categorized as loot.
     * @param isContractEnd A boolean indicating if the supply drop is related to the contract's conclusion.
     */
    private void supplyDropDialog(List<Part> droppedItems, Money cashReward, boolean isLoot, boolean isContractEnd) {
        ImageIcon icon = getFactionLogo(campaign, employerFaction.getShortName(), true);

        if (isLoot || isContractEnd) {
            icon = getFactionLogo(campaign, campaign.getFaction().getShortName(), true);
        }
        icon = scaleImageIconToWidth(icon, 100);

        StringBuilder message = new StringBuilder(getInitialDescription(isLoot, isContractEnd));

        List<String> partsReport = createPartsReport(droppedItems);
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
                    partsReport.add(resources.getString("resourcesRations.text")
                        + " x" + rationPacks);
                }

                if (medicalSupplies > 0) {
                    partsReport.add(resources.getString("resourcesMedical.text")
                        + " x" + medicalSupplies);
                }
            }
        }

        String[] columns = formatColumnData(partsReport);

        if (!cashReward.isZero()) {
            columns[partsReport.size() % 3] += "<br> - " + cashReward.toAmountAndSymbolString();
        }

        message.append("<table><tr valign='top'>")
            .append("<td>").append(columns[0]).append("</td>")
            .append("<td>").append(columns[1]).append("</td>")
            .append("<td>").append(columns[2]).append("</td>")
            .append("</tr></table>");

        createResupplyDialog(icon, message.toString(), droppedItems, cashReward,
            isLoot || isContractEnd, false);
    }

    /**
     * Creates a resupply dialog window with the specified parameters.
     *
     * @param icon            The icon to be displayed in the dialog.
     * @param message         The message to be displayed in the dialog.
     * @param droppedItems    List of items to be dropped. Can be null.
     * @param cashReward      The value of the cash reward.
     * @param isLootOrContractEnd {@link Boolean} value representing if the dialog is being created
     *                        as the result of either loot or contract end.
     * @param isSmuggler      {@link Boolean} value representing if the dialog is an offer from a
     *                        smuggler.
     */
    public void createResupplyDialog(ImageIcon icon, String message, List<Part> droppedItems,
                                     Money cashReward, boolean isLootOrContractEnd, boolean isSmuggler) {
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
                deliverDrop(droppedItems, cashReward);
            } else if (!isSmuggler) {
                processConvoy(droppedItems, cashReward);
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

        List<String> partsReport = new ArrayList<>();
        if (droppedItems != null) {
            partsReport = createPartsReport(droppedItems);
        }

        List<String> entries = new ArrayList<>(partsReport);
        String[] columns = formatColumnData(entries);

        message.append("<table><tr valign='top'>")
            .append("<td>").append(columns[0]).append("</td>")
            .append("<td>").append(columns[1]).append("</td>")
            .append("<td>").append(columns[2]).append("</td>")
            .append("</tr></table>");

        createResupplyDialog(icon, message.toString(), droppedItems, Money.zero(),
            false, true);
    }

    /**
     * Processes a convoy by determining interception chances based on morale level and performs
     * actions accordingly.
     *
     * @param droppedItems    List of items dropped by the convoy.
     * @param cashReward      The amount of cash reward.
     */
    private void processConvoy(List<Part> droppedItems, Money cashReward) {
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
        if (usePlayerConvoy) {
            targetConvoy = getRandomConvoy();

            if (message.isEmpty()) {
                interceptionChance = (int) round((double) interceptionChance / 2);
            }

            if (campaign.getCampaignOptions().isUseFatigue()) {
                increaseFatigue(interceptionChance);
            }
        }

        if (!message.isEmpty()) {
            createConvoyMessage(targetConvoy, droppedItems, cashReward, message, isIntercepted);
        } else {
            campaign.addReport(String.format(resources.getString("convoySuccessful.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            deliverDrop(droppedItems, cashReward);
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
            deliverDrop(droppedItems, Money.zero());
        }
    }

    /**
     * Creates a convoy with provided parameters and a status message to display.
     *
     * @param droppedItems         List of items dropped by the convoy.
     * @param cashReward           The amount of cash reward.
     * @param convoyStatusMessage  The status message to be displayed for the convoy.
     * @param isIntercepted        {@link Boolean} indicating if the convoy has been intercepted.
     */
    public void createConvoyMessage(@Nullable Integer targetConvoy, List<Part> droppedItems,
                                    Money cashReward, String convoyStatusMessage, boolean isIntercepted) {
        createConvoyMessage(targetConvoy, droppedItems, cashReward, convoyStatusMessage,
            isIntercepted, true);
    }

    /**
     * Creates a convoy with provided parameters, status message to display and introduction option.
     *
     * @param droppedItems         List of items dropped by the convoy.
     * @param cashReward           The amount of cash reward.
     * @param convoyStatusMessage  The status message to be displayed for the convoy.
     * @param isIntercepted        {@link Boolean} indicating if the convoy has been intercepted.
     * @param isIntroduction       {@link Boolean} if this dialog is an introduction (i.e., the player
     *                             being informed that they have a message), or whether it's the
     *                             message that follows - informing the player of the message's nature.
     */
    public void createConvoyMessage(@Nullable Integer targetConvoy, List<Part> droppedItems,
                                    Money cashReward, String convoyStatusMessage,
                                    boolean isIntercepted, boolean isIntroduction) {
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
                createConvoyMessage(targetConvoy, droppedItems, cashReward,
                    convoyStatusMessage, isIntercepted, false);
            } else {
                if (isIntercepted) {
                    if (campaign.getCampaignOptions().isUseStratCon()) {
                        processConvoyInterception(droppedItems, cashReward, targetConvoy, track,
                            convoyGridReference);
                    } else {
                        campaign.addReport(String.format(resources.getString("convoyInterceptedAtB.text"),
                            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                            CLOSING_SPAN_TAG));
                    }
                } else {
                    campaign.addReport(String.format(resources.getString("convoySuccessful.text"),
                        spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                        CLOSING_SPAN_TAG));
                    deliverDrop(droppedItems, cashReward);
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

            if (usePlayerConvoy) {
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

    /**
     * Retrieves the speaker's icon for use in message dialogs.
     *
     * @param targetConvoy      The target convoy in the current context, used to determine the convoy
     *                         commander's icon. May be {@code null} if a player convoy is not present.
     * @param isIntroduction    A boolean flag indicating whether the communication is an introduction.
     * @param logisticsOfficer  The logistics officer involved in the communication, may be
     *                          {@code null} if not present.
     *
     * @return The {@link ImageIcon} representing the speaker. If no specific speaker icon is
     * available, the method will return {@code null}.
     */
    @Nullable
    private ImageIcon getSpeakerIcon(@Nullable Integer targetConvoy, boolean isIntroduction,
                                     @Nullable Person logisticsOfficer) {
        ImageIcon speakerIcon = null;
        if (isIntroduction) {
            if (logisticsOfficer == null) {
                speakerIcon = getFactionLogo(campaign, campaign.getFaction().getShortName(),
                    true);
            } else {
                speakerIcon = logisticsOfficer.getPortrait().getImageIcon();
            }
        } else {
            if (usePlayerConvoy) {
                speakerIcon = getConvoyIcon(targetConvoy);
            }

            if (speakerIcon == null) {
                if (usePlayerConvoy) {
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
     * Retrieves the icon of the convoy.
     *
     * @param targetConvoy The convoy target.
     * @return The {@link ImageIcon} of the convoy.
     */
    @Nullable
    private ImageIcon getConvoyIcon(Integer targetConvoy) {
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
     * @param droppedItems         List of items dropped by the convoy.
     * @param cashReward           The amount of cash reward.
     * @param targetConvoy         The target convoy. Can be {@code null}.
     * @param track                The track reference for the convoy's location.
     * @param convoyGridReference  The grid reference for the convoy's location.
     */
    private void processConvoyInterception(List<Part> droppedItems, Money cashReward, @Nullable Integer targetConvoy,
                                           StratconTrackState track, StratconCoords convoyGridReference) {
        String templateAddress = "data/scenariotemplates/Emergency Convoy Defense.xml";

        if (usePlayerConvoy) {
            templateAddress = "data/scenariotemplates/Emergency Convoy Defense - Independent.xml";
        }
        ScenarioTemplate template = ScenarioTemplate.Deserialize(templateAddress);

        if (template == null) {
            campaign.addReport(String.format(resources.getString("convoyErrorTemplate.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                templateAddress, CLOSING_SPAN_TAG));
            deliverDrop(droppedItems, cashReward);
            return;
        }

        if (track == null) {
            campaign.addReport(String.format(resources.getString("convoyErrorTracks.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                templateAddress, CLOSING_SPAN_TAG));
            deliverDrop(droppedItems, cashReward);
            return;
        }
        StratconScenario scenario = generateExternalScenario(campaign, contract, track, convoyGridReference, template);

        // If we successfully generated a scenario, we need to make a couple of final
        // adjustments, including assigning the Resupply contents as loot and
        // assigning a player convoy (if appropriate)
        if (scenario != null) {
            AtBDynamicScenario backingScenario = scenario.getBackingScenario();
            backingScenario.setDate(campaign.getLocalDate());

            if (targetConvoy != null) {
                backingScenario.addForce(targetConvoy, "Player");
                campaign.getForce(targetConvoy).setScenarioId(backingScenario.getId(), campaign);
                scenario.commitPrimaryForces();
            }

            Loot loot = new Loot();

            if (droppedItems != null) {
                for (Part part : droppedItems) {
                    loot.addPart(part);
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
     * @param droppedItems A list of items to be dropped.
     * @param cashReward   The cash reward to be added to the finance ledger.
     */
    private void deliverDrop(List<Part> droppedItems, Money cashReward) {
        if (droppedItems != null) {
            for (Part part : droppedItems) {
                if (part instanceof AmmoBin) {
                    campaign.getQuartermaster().addAmmo(((AmmoBin) part).getType(), ((AmmoBin) part).getFullShots());
                } else if (part instanceof Armor) {
                    int quantity = (int) Math.ceil(((Armor) part).getArmorPointsPerTon());
                    ((Armor) part).setAmount(quantity);
                    campaign.getWarehouse().addPart(part);
                } else {
                    campaign.getWarehouse().addPart(part);
                }
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
    public String[] formatColumnData(List<String> partsReport) {
        String[] columns = new String[3];
        Arrays.fill(columns, "");

        int i = 0;
        for (String entry : partsReport) {
            columns[i % 3] += "<br> - " + entry;
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
            return weight * 10;
        } else {
            return weight;
        }
    }

    /**
     * Extracts parts from the campaign's units and applies warehouse and industry weight modifiers.
     * <p>
     * The method checks each part of each unit in the campaign's units. Only parts from units that
     * are available and not marked as salvage are considered. Parts are ignored if they meet the
     * following criteria:
     * - the part is from a large vessel, super heavy, or conventional infantry unit
     * - the part relates to a certain locations or is extinct
     * - the acquisition is taking place before Battle of Tukayyid and is Clan or Mixed tech, while
     * the employer faction isn't Clan
     * <p>
     * For each valid part, we create a "PartDetails" object which includes the part and its adjusted
     * weight, then add it to a map. If the part is already in the map we increase its weight by the
     * weight of the new part.
     *
     * @return A map of parts and their respective details. If an error occurs during parts collection,
     * an empty map is returned and the error is logged.
     */
    private Map<String, PartDetails> collectParts() {
        final Collection<Unit> units = campaign.getUnits();
        Map<String, PartDetails> processedParts = new HashMap<>();

        try {
            for (Unit unit : units) {
                Entity entity = unit.getEntity();

                if (isShipOrInfantry(entity)) {
                    continue;
                }

                if (!unit.isSalvage() && unit.isAvailable()) {
                    List<Part> parts = unit.getParts();
                    for (Part part : parts) {
                        if (isIneligiblePart(part, unit)) {
                            continue;
                        }

                        int systemIndustry = getSystemIndustry();
                        PartDetails partDetails = getPartDetails(part, systemIndustry);

                        processedParts.merge(part.toString(), partDetails, (oldValue, newValue) -> {
                            oldValue.setWeight(oldValue.getWeight() + newValue.getWeight());
                            return oldValue;
                        });
                    }
                }
            }

            applyWarehouseWeightModifiers(processedParts);
        } catch (Exception exception) {
            logger.error("Aborted parts collection.", exception);
        }

        return processedParts;
    }

    /**
     * Adjusts the weights of the parts based on how many are stored in the warehouse.
     * It takes into consideration both the quantity of part in storage and the multiplier specific
     * for each part based on its type.
     *
     * @param partsList The map of parts and their details to be adjusted.
     */
    private void applyWarehouseWeightModifiers(Map<String, PartDetails> partsList) {
        // Because of how AmmoBins work, we're always considering the campaign to have 0 rounds
        // of ammo in storage, we could avoid this, but I don't think it's necessary.
        for (Part part : campaign.getWarehouse().getSpareParts()) {
            PartDetails targetPart = partsList.get(part.toString());
            if (targetPart != null) {
                int spareCount = part.getQuantity();
                double multiplier = getPartMultiplier(part);

                double targetPartCount = targetPart.getWeight() * multiplier;
                if ((targetPartCount - spareCount) < 1) {
                    partsList.remove(part.toString());
                } else {
                    targetPart.setWeight(targetPartCount);
                }
            }
        }
    }

    /**
     * Checks if the given entity is a large craft, super heavy, or conventional infantry.
     *
     * @param entity The entity to check.
     * @return Boolean value {@code true} if the entity is large craft, super heavy or conventional
     * infantry, otherwise {@code false}.
     */
    private boolean isShipOrInfantry(Entity entity) {
        return entity.isLargeCraft() || entity.isSuperHeavy() || entity.isConventionalInfantry();
    }

    /**
     * Checks if a given part is ineligible for inclusion in a resupply.
     *
     * @param part The part to check for eligibility.
     * @param unit The unit to associate the part with.
     * @return     {@code true} if the part is ineligible; {@code false} otherwise.
     */
    private boolean isIneligiblePart(Part part, Unit unit) {
        return checkExclusionList(part)
            || checkMekLocation(part, unit)
            || checkTankLocation(part)
            || checkTransporter(part)
            || isExtinct(part)
            || isUnavailableBeforeTukayyid(part);
    }

    /**
     * Checks if a given part is of a type that is specifically excluded from resupplies.
     * This method should be expanded as ineligible parts are found.
     *
     * @param part The part to check for eligibility.
     * @return     {@code true} if the part is ineligible; {@code false} otherwise.
     */
    private boolean checkExclusionList(Part part) {
        return false;
    }

    /**
     * Checks whether a part is a center torso location of a Mek or if the Mek is extinct.
     *
     * @param part The part to check.
     * @param mek  The Mek to which the part belongs.
     * @return {@code True} if the part is a central torso location or the Mek is extinct.
     * {@code False} otherwise.
     */
    private boolean checkMekLocation(Part part, Unit mek) {
        return part instanceof MekLocation &&
            (((MekLocation) part).getLoc() == Mek.LOC_CT
                || mek.isExtinct(YEAR, EMPLOYER_IS_CLAN, EMPLOYER_TECH_CODE));
    }

    /**
     * Checks if the given part is a location on a Tank but not a Rotor or Turret.
     *
     * @param part The part to check.
     * @return {@code True} if the part is a location on a Tank but not a Rotor or Turret.
     * {@code False} otherwise.
     */
    private boolean checkTankLocation(Part part) {
        return part instanceof TankLocation && !(part instanceof Rotor || part instanceof Turret);
    }

    /**
     * Checks if the given part is a Transport Bay or door.
     *
     * @param part The part to check.
     * @return {@code True} if the part is a transport bay or door. {@code False} otherwise.
     */
    private boolean checkTransporter(Part part) {
        return part instanceof TransportBayPart;
    }

    /**
     * Checks if the part is extinct.
     *
     * @param part The part to check.
     * @return {@code True} if the part is extinct. {@code False} otherwise.
     */
    private boolean isExtinct(Part part) {
        return part.isExtinct(YEAR, EMPLOYER_IS_CLAN, EMPLOYER_TECH_CODE);
    }

    /**
     * Checks if a part is available before the Battle of Tukayyid.
     *
     * @param part The part to check.
     * @return {@code True} if the part is unavailable before Tukayyid. {@code False} otherwise.
     */
    private boolean isUnavailableBeforeTukayyid(Part part) {
        return (part.isClan() || part.isMixedTech())
            && !employerFaction.isClan()
            && campaign.getLocalDate().isBefore(BATTLE_OF_TUKAYYID);
    }

    /**
     * Gets the industry value of a system based on the contract type.
     *
     * @return The industry rating of the system.
     */
    private int getSystemIndustry() {
        // This default value represents the employer needing to bring in resources from off-planet.
        int systemIndustry = RATING_C;

        if (contract.getContractType().isGuerrillaWarfare()) {
            // This value represents the limited resources available to Smugglers
            systemIndustry = RATING_A;
        } else if (!contract.getContractType().isRaidType()) {
            // Otherwise, we use the planet's industry
            systemIndustry = campaign.getLocation().getPlanet()
                .getSocioIndustrial(campaign.getLocalDate()).industry;
        }
        return systemIndustry;
    }

    /**
     * Creates a new {@link PartDetails} instance for a part adjusting its weight based on system
     * industry and part's tech rating.
     *
     * @param part The part for which to create the PartDetails instance.
     * @param systemIndustry The system industry rating.
     * @return A new {@link PartDetails} instance with the part and its adjusted weight.
     */
    private PartDetails getPartDetails(Part part, double systemIndustry) {
        // The +1 is to avoid having to divide by 0
        double adjustedWeight = (systemIndustry + 1) / ((double) part.getTechRating() + 1);
        return new PartDetails(part, adjustedWeight);
    }

    /**
     * Calculates a multiplier for a part's weight based on the part's type.
     *
     * @param part The part for which to get the multiplier.
     * @return The multiplier for the part's weight.
     */
    private static double getPartMultiplier(Part part) {
        double multiplier = 1;

        // This is based on the Mishra Method, found in the Company Generator
        if (part instanceof HeatSink) {
            multiplier = 2.5;
        } else if (part instanceof MekLocation) {
            if (((MekLocation) part).getLoc() == Mek.LOC_HEAD) {
                multiplier = 2;
            }
        } else if (part instanceof MASC || part instanceof MekGyro || part instanceof EnginePart) {
            multiplier = 0.5;
        }

        return multiplier;
    }

    /**
     * Populates the 'partsPool' instance variable with parts from 'potentialParts' by cloning the
     * {@code HashMap}, thus ensuring that the pool remains independent of future changes to the
     * potential parts {@code HashMap}.
     */
    private List<Part> buildPool(Map<String, PartDetails> potentialParts) {
        List<Part> partsPool = new ArrayList<>();

        for (PartDetails potentialPart : potentialParts.values()) {
            // We need to use ceil here, otherwise well-stocked campaigns will end up with very
            // mediocre Resupplies.
            int weight = (int) Math.ceil(potentialPart.getWeight());
            for (int entry = 0; entry < weight; entry++) {
                partsPool.add(preparePart(potentialPart.getPart()));
            }
        }

        if (!partsPool.isEmpty()) {
            Collections.shuffle(partsPool);
        }

        return partsPool;
    }

    /**
     * Creates a copy of a given part with specific properties adjusted to fit a resupply.
     * The properties adjusted include fixing any damage on the part, setting its quality to a randomly
     * determined value (except for AmmoBins), and marking the part as brand new and not OmniPodded.
     * <p>
     * If the part fails to clone, an error is logged and the method returns {@code null}.
     *
     * @param originPart The {@link Part} object to clone and modify.
     * @return A cloned {@link Part} object , or {@code null} if cloning fails.
     */
    private Part preparePart(Part originPart) {
        Part clonedPart = originPart.clone();

        if (clonedPart == null) {
            logger.error(String.format("Failed to clone part: %s", originPart));
            return null;
        }

        try {
            clonedPart.fix();
        } catch (Exception e) {
            clonedPart.setHits(0);
        }

        clonedPart.setBrandNew(true);
        clonedPart.setOmniPodded(false);

        return clonedPart;
    }

    /**
     * @return A randomly selected {@link Part} from the parts pool.
     */
    private Part getRandomPart() {
        Part randomPart = partsPool.get(random.nextInt(partsPool.size()));

        // We set quality here as it means we only need to parse quality for those items we're
        // specifically picking out of the pool.
        if (!(randomPart instanceof AmmoBin)) {
            randomPart.setQuality(getRandomPartQuality(negotiationModifier));
        }

        return randomPart;
    }
    /**
     * This method initiates to generate the supply drop parts and display them on the dialog.
     * The ‘dropCount’ parameter determines the number of times the part generation loop runs.
     *
     * @param dropCount Number of times the part-generation loop must run.
     * @param bypassConvoyNeeds If {@code true} an NPC convoy will be provided, even for contracts
     *                         where this wouldn't normally be possible.
     */
    public void getResupply(int dropCount, boolean bypassConvoyNeeds) {
        getResupply(dropCount, bypassConvoyNeeds, false, false);
    }

    /**
     * Overloaded method that also allows supply drops to consider whether the drops are classified
     * as 'loot'.
     *
     * @param dropCount Number of times the part-generation loop must run.
     * @param bypassConvoyNeeds If {@code true} an NPC convoy will be provided, even for contracts
     *                         where this wouldn't normally be possible.
     * @param isLoot    Boolean that flags whether drops are classified as loot.
     */
    public void getResupply(int dropCount, boolean bypassConvoyNeeds, boolean isLoot) {
        getResupply(dropCount, isLoot, bypassConvoyNeeds, false);
    }

    /**
     * Overloaded method that also allows supply drops to consider whether the drops are at the end
     * of the contract.
     *
     * @param dropCount Number of times the part-generation loop must run.
     * @param bypassConvoyNeeds If {@code true} an NPC convoy will be provided, even for contracts
     *                         where this wouldn't normally be possible.
     * @param isLoot    Boolean that flags whether drops are classified as loot. Should be set to
     * {@code false} if this is being generated at the end of a contract.
     * @param isContractEnd Boolean that flags whether drops are at the end of a contract.
     */
    public void getResupply(int dropCount, boolean bypassConvoyNeeds, boolean isLoot,
                            boolean isContractEnd) {
        boolean isIndependent = contract.getCommandRights().isIndependent();

        if (!contract.getContractType().isGuerrillaWarfare() && !bypassConvoyNeeds) {
            createPlayerConvoyOptionalDialog();
        }

        if (isIndependent && !usePlayerConvoy && !bypassConvoyNeeds) {
            return;
        }

        List<Part> droppedItems = new ArrayList<>();
        Money cashReward = Money.zero();
        Money targetValue = TARGET_VALUE;

        if (!isIndependent && usePlayerConvoy) {
            targetValue = targetValue.multipliedBy(2);
        }

        for (int i = 0; i < dropCount; i++) {
            Money runningTotal = Money.zero();

            while (runningTotal.isLessThan(targetValue)) {
                if (partsPool.isEmpty()) {
                    break;
                }

                Part potentialPart = getRandomPart();
                boolean partFetched = false;

                // For particularly valuable items, we roll a follow-up die to see if the item
                // is actually picked, or if the supplier substitutes it with another item.
                if (potentialPart.getUndamagedValue().isGreaterThan(targetValue)) {
                    if (Compute.d6(1) == 6) {
                        partFetched = true;
                    }

                    // For really expensive items, the player only has one chance per distinct
                    // part.
                    partsPool.removeAll(Collections.singleton(potentialPart));
                } else {
                    partFetched = true;
                }

                if (partFetched) {
                    partsPool.remove(potentialPart);
                    runningTotal = runningTotal.plus(potentialPart.getActualValue());
                    droppedItems.add(potentialPart);
                }
            }
        }

        if (contract.getContractType().isGuerrillaWarfare() && !(isLoot || isContractEnd)) {
            if (!droppedItems.isEmpty()) {
                smugglerOfferDialog(droppedItems);
                logger.warn("No resupply possible as no items dropped (Smuggler).");
            }
        } else {
            if (!droppedItems.isEmpty()) {
                supplyDropDialog(droppedItems, cashReward, isLoot, isContractEnd);
                logger.warn("No resupply possible as no items dropped (Normal).");
            }
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
    public List<String> createPartsReport(@Nullable List<Part> droppedItems) {
        int year = campaign.getGameYear();
        Faction originFaction = campaign.getFaction();

        Map<String, Integer> entries = droppedItems.stream().collect(Collectors.toMap(
                part -> {
                    String name = part.getName();
                    String quality = part.getQualityName();

                    String append = part.isClan() ? " (Clan)" : "";
                    append = part.isMixedTech() ? " (Mixed)" : append;
                    append += " (" + quality + ')';
                    append += part.isExtinct(year, originFaction.isClan(), getTechFaction(originFaction)) ?
                        " (<b>EXTINCT!</b>)" : "";

                    if (part instanceof AmmoBin) {
                        return ((AmmoBin) part).getType().getName() + append;
                    } else if (part instanceof MekLocation || part instanceof MekActuator) {
                        return name + " (" + part.getUnitTonnage() + "t)" + append;
                    } else {
                        return name + append;
                    }
                },
                part -> {
                    if (part instanceof AmmoBin) {
                        return ((AmmoBin) part).getFullShots();
                    } else if (part instanceof Armor) {
                        return (int) Math.ceil(((Armor) part).getArmorPointsPerTon());
                    } else {
                        return 1;
                    }
                },
                Integer::sum));

        return entries.keySet().stream()
            .map(item -> item + " x" + entries.get(item))
            .sorted()
            .collect(Collectors.toList());
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
     * beginning of a contract.
     *
     * @param campaign The current campaign.
     * @param contract The relevant contract.
     */
    public static void triggerContractStartDialog(Campaign campaign, AtBContract contract) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

        // Retrieves the title from the resources
        String title = resources.getString("dialog.title");

        // An ImageIcon to hold the faction icon
        ImageIcon icon = getFactionLogo(campaign, campaign.getFaction().getShortName(), true);

        // Create a text pane to display the message
        String message = getContractStartMessage(campaign, contract, resources);
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

    /**
     * Calculates the convoy count and total cargo capacity of the player's convoys.
     */
    private void calculatePlayerConvoyValues() {
        convoyCount = 0;
        totalCargoCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (force.isConvoyForce()) {
                boolean hasCargo = false;
                for (UUID unitId : force.getUnits()) {
                    Unit unit = campaign.getUnit(unitId);

                    if (unit != null) {
                        if (!unit.isFullyCrewed()) {
                            continue;
                        }

                        double individualCargo = unit.getCargoCapacity();

                        if (!hasCargo && individualCargo > 0) {
                            hasCargo = true;
                        }

                        totalCargoCapacity += individualCargo;
                    }
                }

                if (hasCargo) {
                    convoyCount++;
                }
            }
        }
    }

    public void createPlayerConvoyOptionalDialog() {
        final int DIALOG_WIDTH = 400;

        // Retrieves the title from the resources
        String title = resources.getString("dialog.title");

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());

        // Establish the speaker
        Person logisticsOfficer = pickLogisticsRepresentative();

        String speaker;
        if (logisticsOfficer != null) {
            speaker = logisticsOfficer.getFullTitle();
        } else {
            speaker = String.format(resources.getString("dialogBorderCampaignSpeaker.text"),
                campaign.getName());
        }

        // An ImageIcon to hold the faction icon
        ImageIcon speakerIcon = getSpeakerIcon(null, true, logisticsOfficer);
        speakerIcon = scaleImageIconToWidth(speakerIcon, UIUtil.scaleForGUI(100));

        // Calculate convoy values
        int convoysNeeded = (int) Math.max(1, Math.floor((double) contract.getRequiredLances() / 3));
        int tonnageNeeded = (int) round(convoysNeeded * RESUPPLY_LOAD_SIZE);

        // Create and display the message
        String pluralizerEmployer = convoysNeeded > 1 ? "s" : "";
        String pluralizerPlayer = convoyCount > 1 || convoyCount == 0 ? "s" : "";
        String messageResource;

        if (isUsePlayerConvoy()) {
            messageResource = resources.getString("usePlayerConvoyForced.text");
        } else {
            messageResource = resources.getString("usePlayerConvoyOptional.text");
        }

        String message = String.format(messageResource, getCommanderTitle(campaign, false),
            pluralizerEmployer, tonnageNeeded, convoysNeeded, pluralizerEmployer, totalCargoCapacity, convoyCount,
            pluralizerPlayer, pluralizerPlayer);

        // Create a panel to display the icon and the message
        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                UIUtil.scaleForGUI(DIALOG_WIDTH), message));
        description.setHorizontalAlignment(JLabel.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
            String.format(resources.getString("dialogBorderTitle.text"), speaker)));
        descriptionPanel.add(description);
        dialog.add(descriptionPanel, BorderLayout.CENTER);

        // Create a panel to display the icon and the message
        JPanel panel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(speakerIcon);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(descriptionPanel, BorderLayout.SOUTH);

        // Create the buttons and add their action listener.
        JButton acceptButton = new JButton(resources.getString("confirmAccept.text"));
        acceptButton.addActionListener(e -> {
            dialog.dispose();
            usePlayerConvoy = true;
        });
        acceptButton.setEnabled(convoyCount > 0);

        JButton refuseButton = new JButton(resources.getString("confirmRefuse.text"));
        refuseButton.addActionListener(e -> {
            dialog.dispose();
            usePlayerConvoy = false;
        });

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(acceptButton);
        buttonPanel.add(refuseButton);

        // Add a WindowListener to handle the close operation
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                usePlayerConvoy = false;
            }
        });

        // Add the original panel and button panel to the dialog
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    /**
     * Creates a start message for a contract based on its type and resupply details.
     * The message is formatted using a specific template according to the type of
     * the contract and whether it involves guerrilla warfare or independent command rights.
     *
     * @param campaign          The current campaign.
     * @param contract          The contract for which the start message is created.
     * @param resources         The resource bundle to retrieve convoy message templates.
     * @return A formatted start message for the contract, enclosed within an HTML div with a defined width.
     */
    private static String getContractStartMessage(Campaign campaign, AtBContract contract,
                                                  ResourceBundle resources) {
        int maximumResupplySize = (int) Math.max(1, Math.floor((double) contract.getRequiredLances() / 3));

        int convoyCount = 0;
        double cargoCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (force.isConvoyForce() && force.getSubForces().isEmpty() && !force.getUnits().isEmpty()) {
                convoyCount++;

                for (UUID unitId : force.getUnits()) {
                    Unit unit = campaign.getUnit(unitId);

                    if (unit != null) {
                        cargoCapacity += unit.getCargoCapacity();
                    }
                }
            }
        }

        String convoyMessage;
        String commanderTitle = getCommanderTitle(campaign, false);

        if (contract.getContractType().isGuerrillaWarfare()) {
            String convoyMessageTemplate = resources.getString("contractStartMessageGuerrilla.text");
            convoyMessage = String.format(convoyMessageTemplate, commanderTitle);
        } else {
            String convoyMessageTemplate = resources.getString("contractStartMessageGeneric.text");
            if (contract.getCommandRights().isIndependent()) {
                convoyMessageTemplate = resources.getString("contractStartMessageIndependent.text");
            }

            String resupplyLoadSize = String.valueOf(maximumResupplySize * RESUPPLY_LOAD_SIZE);
            String convoyPluralSuffix = maximumResupplySize > 1 ? "s" : "";

            convoyMessage = String.format(convoyMessageTemplate, commanderTitle,
                resupplyLoadSize, maximumResupplySize, convoyPluralSuffix, cargoCapacity, convoyCount, convoyPluralSuffix);
        }

        int width = UIUtil.scaleForGUI(500);
        return String.format("<html><i><div style='width: %s; text-align:center;'>%s</div></i></html>",
            width, convoyMessage);
    }
}

/**
 * Class representing details about a part, including the part itself and its drop weight.
 */
class PartDetails {
    private Part part;
    private double weight;

    /**
     * Constructs a {@link PartDetails} object with associated part and weight.
     *
     * @param part The part to be stored in this object.
     * @param weight The weight to be associated with the part.
     */
    public PartDetails(Part part, double weight) {
        this.part = part;
        this.weight = weight;
    }

    /**
     * Returns the part associated with this object.
     *
     * @return the stored {@link Part} object.
     */
    public Part getPart() {
        return part;
    }

    /**
     * Gets the part's drop weight.
     *
     * @return The weight associated with the stored part.
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Sets the drop weight for the part.
     *
     * @param weight The weight to be set.
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }
}
