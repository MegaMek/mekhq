/*
 * ContractAutomation.java
 *
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
package mekhq.campaign.market.contractMarket;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.ActivateUnitAction;
import mekhq.campaign.unit.actions.MothballUnitAction;
import mekhq.campaign.universe.Factions;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static megamek.common.icons.AbstractIcon.DEFAULT_ICON_FILENAME;

/**
 * The ContractAutomation class provides a suite of methods
 * used in automating actions when a contract starts.
 * This includes actions like mothballing of units,
 * transit to mission location and the automated activation of units when arriving in system.
 */
public class ContractAutomation {
    private final static ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.ContractAutomation");

    /**
     * Main function to initiate a sequence of automated tasks when a contract is started.
     * The tasks include prompt and execution for unit mothballing, calculating and starting the
     * journey to the target system.
     *
     * @param campaign The current campaign.
     * @param contract Selected contract.
     */
    public static void contractStartPrompt(Campaign campaign, Contract contract) {
        // If we're already in the right system there is no need to automate these actions
        if (Objects.equals(campaign.getLocation().getCurrentSystem(), contract.getSystem())) {
            return;
        }

        // Initial setup
        final Person speaker = getSpeaker(campaign);
        final String speakerName = getSpeakerName(campaign, speaker);
        final ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);

        final String commanderAddress = getCommanderAddress(campaign);

        // Mothballing
        String message = String.format(resources.getString("mothballDescription.text"), commanderAddress);

        if (createDialog(speakerName, speakerIcon, message)) {
            campaign.setAutomatedMothballUnits(performAutomatedMothballing(campaign));
        }

        // Transit
        String targetSystem = contract.getSystemName(campaign.getLocalDate());
        String employerName = contract.getEmployer();

        if (!employerName.contains("Clan")) {
            employerName = String.format(resources.getString("generalNonClan.text"), employerName);
        }

        JumpPath jumpPath = contract.getJumpPath(campaign);
        int travelDays = contract.getTravelDays(campaign);

        Money costPerJump = campaign.calculateCostPerJump(true,
                campaign.getCampaignOptions().isEquipmentContractBase());
        String totalCost = costPerJump.multipliedBy(jumpPath.getJumps()).toAmountAndSymbolString();

        message = String.format(resources.getString("transitDescription.text"),
            targetSystem, employerName, travelDays, totalCost);
        if (createDialog(speakerName, speakerIcon, message)) {
            campaign.getLocation().setJumpPath(jumpPath);
            campaign.getUnits().forEach(unit -> unit.setSite(Unit.SITE_FACILITY_BASIC));
            campaign.getApp().getCampaigngui().refreshAllTabs();
            campaign.getApp().getCampaigngui().refreshLocation();
        }
    }

    /**
     * @param campaign The current campaign
     * @return The highest ranking Admin/Transport character. If none are found, returns {@code null}.
     */
    private static @Nullable Person getSpeaker(Campaign campaign) {
        List<Person> admins = campaign.getAdmins();

        if (admins.isEmpty()) {
            return null;
        }

        List<Person> transportAdmins = new ArrayList<>();

        for (Person admin : admins) {
            if (admin.getPrimaryRole().isAdministratorTransport()
                || admin.getSecondaryRole().isAdministratorTransport()) {
                transportAdmins.add(admin);
            }
        }

        if (transportAdmins.isEmpty()) {
            return null;
        }

        Person speaker = transportAdmins.get(0);

        for (Person admin : transportAdmins) {
            if (admin.outRanksUsingSkillTiebreaker(campaign, speaker)) {
                speaker = admin;
            }
        }

        return speaker;
    }

    /**
     * Gets the name of the individual to be displayed in the dialog.
     * If the person is {@code null}, it uses the campaign's name.
     *
     * @param campaign The current campaign
     * @param speaker The person who will be speaking, or {@code null}.
     * @return The name to be displayed.
     */
    private static String getSpeakerName(Campaign campaign, @Nullable Person speaker) {
        if (speaker == null) {
            return campaign.getName();
        } else {
            return speaker.getFullTitle();
        }
    }

    /**
     * Gets the icon representing the speaker.
     * If the speaker is {@code null}, it defaults to displaying the campaign's icon, or the
     * campaign's faction icon.
     *
     * @param campaign The current campaign
     * @param speaker The person who is speaking, or {@code null}.
     * @return The icon of the speaker, campaign, or faction.
     */
    private static ImageIcon getSpeakerIcon(Campaign campaign, @Nullable Person speaker) {
        ImageIcon icon;

        if (speaker == null) {
            String fallbackIconFilename = campaign.getUnitIcon().getFilename();

            if (fallbackIconFilename == null || fallbackIconFilename.equals(DEFAULT_ICON_FILENAME)) {
                icon = Factions.getFactionLogo(campaign, campaign.getFaction().getShortName(), true);
            } else {
                icon = campaign.getUnitIcon().getImageIcon();
            }
        } else {
            icon = speaker.getPortrait().getImageIcon();
        }

        Image originalImage = icon.getImage();
        Image scaledImage = originalImage.getScaledInstance(100, -1, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    /**
     * Gets a string to use for addressing the commander.
     * If no commander is flagged, returns a default address.
     *
     * @param campaign The current campaign
     * @return The title of the commander, or a default string if no commander.
     */
    private static String getCommanderAddress(Campaign campaign) {
        Person commander = campaign.getFlaggedCommander();

        if (commander == null) {
            return resources.getString("generalFallbackAddress.text");
        }

        String commanderRank = commander.getRankName();

        if (commanderRank.equalsIgnoreCase("None") || commanderRank.isBlank()) {
            return commander.getFullName();
        }

        return commanderRank;
    }

    /**
     * Displays a dialog for user interaction.
     * The dialog uses a custom formatted message and includes options for user to confirm or decline.
     *
     * @param speakerName The title of the speaker to be displayed.
     * @param speakerIcon The {@link ImageIcon} of the person speaking.
     * @param message The message to be displayed in the dialog.
     * @return {@code true} if the user confirms, {@code false} otherwise.
     */
    private static boolean createDialog(String speakerName, ImageIcon speakerIcon, String message) {
        final int WIDTH = UIUtil.scaleForGUI(400);

        // Custom button text
        Object[] options = {
            resources.getString("generalConfirm.text"),
            resources.getString("generalDecline.text")
        };

        // Create a custom message with a border
        String descriptionTitle = String.format("<html><b>%s</b></html>", speakerName);

        // Create ImageIcon JLabel
        JLabel iconLabel = new JLabel(speakerIcon);
        iconLabel.setHorizontalAlignment(JLabel.CENTER);

        // Create description JPanel
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.PAGE_AXIS));
        JLabel description = new JLabel(String.format("<html><div style='width: %s; text-align:justify;'>%s</div></html>",
            WIDTH, message));
        description.setBorder(BorderFactory.createTitledBorder(descriptionTitle));
        descriptionPanel.add(description);

        // Create main JPanel and add icon and description
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(iconLabel, BorderLayout.NORTH);
        mainPanel.add(descriptionPanel, BorderLayout.CENTER);

        // Create JOptionPane
        JOptionPane optionPane = new JOptionPane(mainPanel,
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.YES_NO_OPTION,
            null,
            options,
            options[0]);

        // Create JDialog
        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("generalTitle.text"));
        dialog.setModal(true);
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(null);

        optionPane.addPropertyChangeListener(evt -> {
            if (JOptionPane.VALUE_PROPERTY.equals(evt.getPropertyName())) {
                dialog.dispose();
            }
        });

        dialog.setVisible(true);

        int response = (Objects.equals(optionPane.getValue(), options[0]) ?
            JOptionPane.YES_OPTION : JOptionPane.NO_OPTION);

        return (response == JOptionPane.YES_OPTION);
    }

    /**
     * This method identifies all non-mothballed units within a campaign that are currently
     * assigned to a {@code Force}. Those units are then GM Mothballed.
     *
     * @param campaign The current campaign.
     * @return A list of all newly mothballed units.
     */
    private static List<Unit> performAutomatedMothballing(Campaign campaign) {
        List<Unit> mothballTargets = new ArrayList<>();
        List<Unit> mothballedUnits = new ArrayList<>();
        MothballUnitAction mothballUnitAction = new MothballUnitAction(null, true);

        for (Force force : campaign.getAllForces()) {
            for (UUID unitId : force.getUnits()) {
                Unit unit = campaign.getUnit(unitId);

                if (unit != null) {
                    if (!unit.isMothballed()) {
                        mothballTargets.add(unit);
                    }
                }
            }
        }

        // This needs to be a separate list as the act of mothballing the unit removes it from the
        // list of units attached to the relevant force, resulting in a ConcurrentModificationException
        for (Unit unit : mothballTargets) {
            mothballUnitAction.execute(campaign, unit);
            MekHQ.triggerEvent(new UnitChangedEvent(unit));
            mothballedUnits.add(unit);
        }

        return mothballedUnits;
    }

    /**
     * Perform automated activation of units.
     * Identifies all units that were mothballed previously and are now needing activation.
     * The activation action is executed for each unit, and they are returned to their prior Force
     * if it still exists.
     *
     * @param campaign The current campaign.
     */
    public static void performAutomatedActivation(Campaign campaign) {
        List<Unit> units = campaign.getAutomatedMothballUnits();

        if (units.isEmpty()) {
            return;
        }

        ActivateUnitAction activateUnitAction = new ActivateUnitAction(null, true);

        for (Unit unit : units) {
            if (unit.isMothballed()) {
                activateUnitAction.execute(campaign, unit);
                MekHQ.triggerEvent(new UnitChangedEvent(unit));
            }
        }
    }
}
