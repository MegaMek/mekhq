package mekhq.campaign.market.contractMarket;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static megamek.common.icons.AbstractIcon.DEFAULT_ICON_FILENAME;

public class ContractAutomation {
    private final static ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.ContractAutomation");
    // Ask the user if they want to mothball their stuff.
    //     Store the units mothballed in this manner, so we can activate them.
    // Ask the user if they want to chart a course to their destination.
    //     Make sure the user knows how much the journey will cost.
    //     Make sure the user knows how to set the journey if they refuse.
    // Ask the user if they want to start their journey.
    //     Make sure the user knows how long the journey will take cost.
    //     Make sure the user knows how to begin the journey if they refuse.

    // When the user enters the target system automatically active the previously mothballed units.

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

        // Chart Course;
        String targetSystem = contract.getSystemName(campaign.getLocalDate());

        Faction employerFaction = Factions.getInstance().getFaction(contract.getEmployer());
        String employerName = resources.getString("generalEmployerFallback");
        if (employerFaction != null) {
            employerName = employerFaction.getFullName(campaign.getGameYear());
        }

        JumpPath jumpPath = contract.getJumpPath(campaign);
        int travelDays = contract.getTravelDays(campaign);

        Money costPerJump = campaign.calculateCostPerJump(true,
                campaign.getCampaignOptions().isEquipmentContractBase());
        String totalCost = costPerJump.multipliedBy(jumpPath.getJumps()).toAmountAndSymbolString();

        message = String.format(resources.getString("chartCourseDescription.text"),
            targetSystem, employerName, travelDays, totalCost);
        boolean calculateJumpPath = createDialog(speakerName, speakerIcon, message);

        if (calculateJumpPath) {
            campaign.getLocation().setJumpPath(jumpPath);
        } else {
            return;
        }

        // Begin Transit
        message = String.format(resources.getString("beginTransitDescription.text"),
            commanderAddress);
        if (createDialog(speakerName, speakerIcon, message)) {
            campaign.getLocation().setJumpPath(jumpPath);
            campaign.getUnits().forEach(unit -> unit.setSite(Unit.SITE_FACILITY_BASIC));
        }
    }

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

    private static String getSpeakerName(Campaign campaign, @Nullable Person speaker) {
        if (speaker == null) {
            return String.format(resources.getString("generalSpeakerNameFallback.text"),
                campaign.getName());
        } else {
            return speaker.getFullTitle();
        }
    }

    private static ImageIcon getSpeakerIcon(Campaign campaign, @Nullable Person speaker) {
        ImageIcon icon;

        if (speaker == null) {
            String fallbackIconFilename = campaign.getUnitIcon().getFilename();

            if (fallbackIconFilename == null || fallbackIconFilename.equals(DEFAULT_ICON_FILENAME)) {
                icon = Factions.getFactionLogo(campaign, campaign.getFaction().getShortName(), true);
            } else {
                icon = new ImageIcon(fallbackIconFilename);
            }
        } else {
            icon = speaker.getPortrait().getImageIcon();
        }

        Image originalImage = icon.getImage();
        Image scaledImage = originalImage.getScaledInstance(100, -1, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    private static String getCommanderAddress(Campaign campaign) {
        Person commander = campaign.getFlaggedCommander();

        if (commander == null) {
            return resources.getString("generalFallbackAddress.text");
        }

        String commanderRank = commander.getRankName();

        if (commanderRank.equalsIgnoreCase("None")) {
            return commander.getFullName();
        }

        return commanderRank;
    }

    private static boolean createDialog(String speakerName, ImageIcon speakerIcon, String message) {
        // Custom button text
        Object[] options = {
            resources.getString("generalConfirm.text"),
            resources.getString("generalDecline.text")
        };

        // Create a custom message with a border
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.PAGE_AXIS));
        JLabel description = new JLabel(message);
        description.setBorder(BorderFactory.createTitledBorder(speakerName));
        descriptionPanel.add(description);

        int response = JOptionPane.showOptionDialog(null,
            descriptionPanel,  // Description
            resources.getString("generalTitle.text"),  // Title
            JOptionPane.YES_NO_OPTION,  // Option type
            JOptionPane.QUESTION_MESSAGE,  // Message type
            speakerIcon,  // Icon
            options,  // Array of options
            options[0]);  // Default button title

        return (response == JOptionPane.YES_OPTION);
    }

    private static List<Unit> performAutomatedMothballing(Campaign campaign) {
        List<Unit> mothballedUnits = new ArrayList<>();

        for (Force force : campaign.getAllForces()) {
            for (UUID unitId : force.getUnits()) {
                Unit unit = campaign.getUnit(unitId);

                if (unit != null) {
                    if (!unit.isMothballed()) {
                        unit.completeMothball();
                        mothballedUnits.add(unit);
                    }
                }
            }
        }

        return mothballedUnits;
    }
}
