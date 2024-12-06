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
import megamek.common.Mek;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.StrategicFormation;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.market.procurement.Procurement;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.*;
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
import java.math.BigInteger;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;
import static megamek.common.MiscType.F_SPONSON_TURRET;
import static megamek.common.enums.SkillLevel.NONE;
import static mekhq.campaign.finances.enums.TransactionType.EQUIPMENT_PURCHASE;
import static mekhq.campaign.market.procurement.Procurement.getFactionTechCode;
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
    private final Campaign campaign;
    private final AtBContract contract;
    private final Faction employerFaction;
    private final Faction enemyFaction;
    private final int currentYear;
    private final int employerTechCode;
    private final boolean employerIsClan;
    private List<Part> ammoBinPool;
    private double focusAmmo;
    private List<Part> armorPool;
    private double focusArmor;
    private List<Part> partsPool;
    private double focusParts;
    private final Random random;
    private boolean usePlayerConvoy;
    private Map<Force, Double> playerConvoys;
    private double totalPlayerCargoCapacity;
    private double targetCargoTonnage;
    private int negotiatorSkill;

    private static final Money HIGH_VALUE_ITEM = Money.of(250000);
    private static final int CARGO_MULTIPLIER = 2;
    private static final double INTERCEPTION_LOAD_INFLUENCE = 50;

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");
    private static final MMLogger logger = MMLogger.create(Resupply.class);

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
        random = new Random();

        employerFaction = contract.getEmployerFaction();
        enemyFaction = contract.getEnemy();
        usePlayerConvoy = contract.getCommandRights().isIndependent();
        targetCargoTonnage = calculateTargetCargoTonnage(campaign, contract);

        currentYear = campaign.getGameYear();
        employerIsClan = enemyFaction.isClan();
        employerTechCode = getFactionTechCode(employerFaction);

        focusAmmo = 0.25;
        focusArmor = 0.25;
        focusParts = 0.5;

        buildPartsPools(collectParts());
        calculateNegotiationSkill();
        calculatePlayerConvoyValues();
    }

    /**
     * @return {@code true} if the player's convoy is being used, {@code false} otherwise.
     */
    public boolean isUsePlayerConvoy() {
        return usePlayerConvoy;
    }

    /**
     * Enumeration of the different item types that can be dropped by a Resupply
     */
    public enum DropType {
        PARTS,
        ARMOR,
        AMMO
    }

    /**
     * Calculate the target cargo tonnage for the Resupply.
     * <p>
     * This method iterates over all forces in the campaign.
     * For each combat force, it includes the weight of each unit (that is not of a prohibited type)
     * in the total tonnage and also counts the unit.
     * The target cargo tonnage is calculated as the average unit tonnage, rounded down to the
     * nearest 5 tons.
     * <p>
     * If a getter method returns null during this process, the exception is silently
     * caught and ignored, effectively excluding the current unit from the results.
     * <p>
     * @return the total cargo tonnage that will be used as the target value for Resupplies
     */
    private static double calculateTargetCargoTonnage(Campaign campaign, AtBContract contract) {
        double unitTonnage = 0;
        int unitCount = 0;

        for (StrategicFormation formation : campaign.getStrategicFormationsTable().values()) {
            Force force = campaign.getForce(formation.getForceId());

            if (force == null) {
                continue;
            }

            for (UUID unitId : force.getAllUnits(true)) {
                try {
                    Unit unit = campaign.getUnit(unitId);

                    Entity entity = unit.getEntity();

                    if (isProhibitedUnitType(entity, false)) {
                        continue;
                    }

                    unitTonnage += entity.getWeight();
                    unitCount++;
                } catch (Exception ignored) {
                    // If we get an exception, it's because one of the getters returned null,
                    // at which point we'd just continue anyway, so we might as well just ignore the exception.
                }
            }
        }

        final int CARGO_DIVIDER = 2;
        double averageTonnage = (unitTonnage / unitCount) / CARGO_DIVIDER;

        double dropCount = (double) contract.getRequiredLances() / 3;

        return round(averageTonnage / 20.0 * 5.0 * dropCount);
    }

    /**
     * Calculate the estimated cargo requirements for a given campaign and contract.
     *
     * @param campaign The campaign for which the cargo requirements are being estimated.
     * @param contract The contract associated with the campaign.
     * @return The estimated cargo requirements in tons as a formatted string, e.g., "50t".
     */
    public static String getEstimatedCargoRequirements(Campaign campaign, AtBContract contract) {
        double baseCapacity = calculateTargetCargoTonnage(campaign, contract);

        return (baseCapacity * CARGO_MULTIPLIER) + "t";
    }

    /**
     * Calculate the negotiationModifier for the Resupply.
     * <p>
     * If the contract type is Guerrilla Warfare, then the flagged commander of the campaign
     * will be set as the negotiator. For other contract types, this method searches for the best
     * Logistics Administrator among campaign admins to represent as the negotiator. If one is found
     * and their negotiation skill is determined, the negotiationModifier is calculated based on
     * their skill level, with a max value capped at 3.
     * <p>
     * Resulting negotiationModifier is stored in {@code negotiationModifier} instance variable.
     */
    private void calculateNegotiationSkill() {
        Person negotiator;
        negotiatorSkill = NONE.ordinal();

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
                int skillLevel = skill.getFinalSkillValue();
                negotiatorSkill = skill.getType().getExperienceLevel(skillLevel);
            }
        }
    }

    /**
     * Displays a dialog depicting a resupply of parts and possibly cash.
     *
     * @param droppedItems The items being dropped.
     * @param isLoot A boolean indicating if the supply drop is categorized as loot.
     * @param isContractEnd A boolean indicating if the supply drop is related to the contract's conclusion.
     */
    private void supplyDropDialog(List<Part> droppedItems, boolean isLoot, boolean isContractEnd) {
        ImageIcon icon = getFactionLogo(campaign, employerFaction.getShortName(), true);

        if (isLoot || isContractEnd) {
            icon = getCampaignFactionIcon(campaign);
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

                partsReport.add(resources.getString("resourcesRoleplay" + Compute.randomInt(50)
                    + ".text") + " x" + (int) Math.ceil((double) rationPacks / 5));
            }
        }

        String[] columns = formatColumnData(partsReport);

        message.append("<table><tr valign='top'>")
            .append("<td>").append(columns[0]).append("</td>")
            .append("<td>").append(columns[1]).append("</td>")
            .append("<td>").append(columns[2]).append("</td>")
            .append("</tr></table>");

        createResupplyDialog(icon, message.toString(), droppedItems, isLoot || isContractEnd,
            false);
    }

    /**
     * This method checks if the current campaign has a defined unit icon and returns it.
     * If the campaign doesn't have a defined icon, it retrieves the campaign faction logo.
     *
     * @param campaign The current campaign.
     *
     * @return an {@link ImageIcon} representing the faction of the current campaign.
     */
    private static ImageIcon getCampaignFactionIcon(Campaign campaign) {
        ImageIcon icon;
        StandardForceIcon campaignIcon = campaign.getUnitIcon();

        if (campaignIcon.getFilename() == null) {
            icon = getFactionLogo(campaign, campaign.getFaction().getShortName(),
                true);
        } else {
            icon = new ImageIcon(campaignIcon.getFilename());
        }
        return icon;
    }

    /**
     * Creates a resupply dialog window with the specified parameters.
     *
     * @param icon            The icon to be displayed in the dialog.
     * @param message         The message to be displayed in the dialog.
     * @param droppedItems    List of items to be dropped. Can be null.
     * @param isLootOrContractEnd {@link Boolean} value representing if the dialog is being created
     *                        as the result of either loot or contract end.
     * @param isSmuggler      {@link Boolean} value representing if the dialog is an offer from a
     *                        smuggler.
     */
    public void createResupplyDialog(ImageIcon icon, String message, List<Part> droppedItems,
                                     boolean isLootOrContractEnd, boolean isSmuggler) {
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(700);
        final String title = resources.getString("dialog.title");

        JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        dialog.setTitle(title);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        ActionListener dialogActionListener = e -> {
            dialog.dispose();

            if (isLootOrContractEnd) {
                deliverDrop(droppedItems);
            // Smuggler deliveries are handled elsewhere
            } else if (!isSmuggler) {
                if (isUsePlayerConvoy()) {
                    while (!droppedItems.isEmpty() && !playerConvoys.isEmpty()) {
                        Force chosenConvoy = getRandomConvoy();
                        double cargoCapacity = getTotalCargoCapacity(chosenConvoy);

                        List<Part> delivery = new ArrayList<>();

                        boolean loadingUp = true;
                        while (loadingUp) {
                            Part randomItem = droppedItems.get(random.nextInt(droppedItems.size()));

                            double itemWeight = randomItem.getTonnage();
                            // The multiplier is there as the convoy is assumed to also be running
                            // supplies to allies in the AO.
                            cargoCapacity -= itemWeight * 2;

                            if (cargoCapacity <= 0) {
                                loadingUp = false;
                            }

                            delivery.add(randomItem);
                        }

                        processSingleConvoy(delivery, chosenConvoy);
                        playerConvoys.remove(chosenConvoy);
                    }
                // If we're not using a player convoy, only make a single delivery.
                // While not necessarily realistic, we don't want the player getting spammed with
                // NPC interception missions.
                } else {
                    processSingleConvoy(droppedItems, null);
                }
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

        dialog.setResizable(false);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Calculates the total cargo capacity of a given convoy.
     * <p>
     * This method iterates over each unit in the convoy. If the unit is fully crewed, not damaged,
     * and not of a prohibited type, its cargo capacity is added to the total cargo capacity.
     * <p>
     * If an exception occurs while retrieving the unit or its entity,
     * that particular unit is skipped and not factored into the total cargo capacity.
     *
     * @param convoy the convoy whose total cargo capacity is to be calculated.
     * @return the total cargo capacity of the convoy.
     */
    private double getTotalCargoCapacity(Force convoy) {
        double cargoCapacity = 0;

        for (UUID unitId : convoy.getAllUnits(false)) {
            try {
                Unit unit = campaign.getUnit(unitId);
                Entity entity = unit.getEntity();

                if (!unit.isFullyCrewed() || unit.isDamaged()
                    || isProhibitedUnitType(entity, true)) {
                    cargoCapacity += unit.getCargoCapacity();
                }
            } catch (Exception ignored) {
                // If we fail to get entity or unit, we just skip that unit.
            }
        }

        return cargoCapacity;
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

        createResupplyDialog(icon, message.toString(), droppedItems, false, true);
    }

    /**
     * Processes a convoy by determining interception chances based on morale level and performs
     * actions accordingly.
     *
     * @param droppedItems    List of items dropped by the convoy.
     */
    private void processSingleConvoy(List<Part> droppedItems, @Nullable Force playerConvoy) {
        final String STATUS_FORWARD = "statusUpdate";
        final String STATUS_AFTERWARD = ".text";

        AtBMoraleLevel morale = contract.getMoraleLevel();
        int interceptionChance = morale.ordinal();

        // This allows a convoy to weigh up to 200t before suffering a detection Malus.
        // It also ensures that very small convoys will only get a maximum detection bonus of -3.
        // We deliberately do not want a cap on detection Malus, as eventually convoys will reach
        // such a size that even a blind OpFor will be able to find them.
        double convoyWeight = -200;
        if (playerConvoy != null) {
            for (UUID unitId : playerConvoy.getAllUnits(false)) {
                try {
                    Unit unit = campaign.getUnit(unitId);
                    Entity entity = unit.getEntity();
                    convoyWeight += entity.getWeight();
                } catch (Exception ignored) {
                    // If we fail to fetch any necessary information, we just skip the unit.
                }
            }

            interceptionChance += (int) Math.ceil(convoyWeight / INTERCEPTION_LOAD_INFLUENCE);
        }

        if (morale.isRouted()) {
            // If the enemy has been Routed, there is zero chance of Interception.
            // Even if there are still enemy forces in the AO, they are more concerned with immediate
            // survival than preventing the player from getting new toys.
            interceptionChance = 0;
        } else {
            // If the enemy hasn't been Routed, there is always a 1in10 chance of Interception,
            // no matter how stealthy the convoy.
            interceptionChance = Math.max(0, interceptionChance);
        }

        boolean isIntercepted = false;
        String message = "";

        if (Compute.randomInt(10) < interceptionChance) {
            message = resources.getString(STATUS_FORWARD + "Intercepted" +
                Compute.randomInt(20) + STATUS_AFTERWARD);
            isIntercepted = true;
        // We don't include roleplay events for NPC convoys
        } else if ((playerConvoy != null) && (Compute.randomInt(10) < interceptionChance)) {
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

        if (playerConvoy != null) {
            if (message.isEmpty()) {
                interceptionChance = (int) round((double) interceptionChance / 2);
            }

            if (campaign.getCampaignOptions().isUseFatigue()) {
                increaseFatigue(playerConvoy, interceptionChance);
            }
        }

        if (!message.isEmpty()) {
            if (isIntercepted
                || (!forceContainsOnlyVTOLForces(playerConvoy) && !forceContainsOnlyAerialForces(playerConvoy))) {
                createConvoyMessage(playerConvoy, droppedItems, message, isIntercepted);
            }
        } else {
            campaign.addReport(String.format(resources.getString("convoySuccessful.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            deliverDrop(droppedItems);
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
            deliverDrop(droppedItems);
        }
    }

    public void createConvoyMessage(@Nullable Force targetConvoy, List<Part> droppedItems,
                                    String convoyStatusMessage, boolean isIntercepted) {
        createConvoyMessage(targetConvoy, droppedItems, convoyStatusMessage, isIntercepted, true);
    }

    public void createConvoyMessage(@Nullable Force targetConvoy, List<Part> droppedItems,
                                    String convoyStatusMessage, boolean isIntercepted,
                                    boolean isIntroduction) {
        StratconTrackState track = getRandomTrack(contract);

        StratconCoords convoyGridReference;
        if (track != null) {
            convoyGridReference = getUnoccupiedCoords(track, false);
        } else {
            convoyGridReference = null;
        }

        // Dialog dimensions and representative
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

        // Creates and sets up the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("dialog.title"));
        dialog.setLayout(new BorderLayout());

        // Defines the action when the dialog is being dismissed
        ActionListener dialogDismissActionListener = e -> {
            dialog.dispose();
            if (isIntroduction) {
                createConvoyMessage(targetConvoy, droppedItems, convoyStatusMessage, isIntercepted,
                    false);
            } else {
                if (isIntercepted) {
                    if (campaign.getCampaignOptions().isUseStratCon()) {
                        processConvoyInterception(droppedItems, targetConvoy, track, convoyGridReference);
                    } else {
                        campaign.addReport(String.format(resources.getString("convoyInterceptedAtB.text"),
                            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                            CLOSING_SPAN_TAG));
                    }
                } else {
                    campaign.addReport(String.format(resources.getString("convoySuccessful.text"),
                        spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                        CLOSING_SPAN_TAG));
                    deliverDrop(droppedItems);
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
        dialog.setResizable(false);
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
    private @Nullable ImageIcon getSpeakerIcon(@Nullable Force targetConvoy, boolean isIntroduction,
                                     @Nullable Person logisticsOfficer) {
        ImageIcon speakerIcon = null;
        if (isIntroduction) {
            if (logisticsOfficer == null) {
                speakerIcon = getCampaignFactionIcon(campaign);
            } else {
                speakerIcon = logisticsOfficer.getPortrait().getImageIcon();
            }
        } else {
            if (usePlayerConvoy) {
                speakerIcon = getConvoyIcon(targetConvoy);
            }

            if (speakerIcon == null) {
                if (usePlayerConvoy) {
                    speakerIcon = getCampaignFactionIcon(campaign);
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
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

        // Creates and sets up the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("dialog.title"));
        dialog.setLayout(new BorderLayout());

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
        dialog.setResizable(false);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Increases the fatigue convoy crews.
     *
     * @param playerConvoy The id of the convoy whose crew's fatigue should be increased.
     * @param fatigueIncrease How much fatigue should be adjusted.
     */
    private void increaseFatigue(Force playerConvoy, int fatigueIncrease) {
        for (UUID unitId : playerConvoy.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);

            if (unit != null) {
                for (Person crewMember : unit.getCrew()) {
                    crewMember.increaseFatigue(fatigueIncrease);
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
    private String getConvoySpeaker(@Nullable Force targetConvoy) {
        String speaker = null;

        if (targetConvoy != null) {
            UUID convoyCommanderId = targetConvoy.getForceCommanderID();

            if (convoyCommanderId != null) {
                Person convoyCommander = campaign.getPerson(convoyCommanderId);

                if (convoyCommander != null) {
                    speaker = convoyCommander.getFullTitle();
                }
            }
        }

        if (targetConvoy != null) {
            if (speaker == null) {
                speaker = targetConvoy.getName();
            } else {
                speaker = speaker + ", " + targetConvoy.getName();
            }
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
    private @Nullable ImageIcon getConvoyIcon(@Nullable Force targetConvoy) {
        if (targetConvoy == null) {
            return null;
        }

        UUID convoyCommanderId = targetConvoy.getForceCommanderID();

        if (convoyCommanderId == null) {
            return null;
        }

        Person convoyCommander = campaign.getPerson(convoyCommanderId);

        if (convoyCommander == null) {
            return null;
        }

        return convoyCommander.getPortrait().getImageIcon();
    }

    /**
     * Processes the interception of a convoy.
     *
     * @param droppedItems         List of items dropped by the convoy.
     * @param targetConvoy         The target convoy. Can be {@code null}.
     * @param track                The track reference for the convoy's location.
     * @param convoyGridReference  The grid reference for the convoy's location.
     */
    private void processConvoyInterception(List<Part> droppedItems, @Nullable Force targetConvoy,
                                           StratconTrackState track, StratconCoords convoyGridReference) {
        final String DIRECTORY = "data/scenariotemplates/";
        final String GENERIC = DIRECTORY + "Emergency Convoy Defense.xml";
        final String PLAYER_AEROSPACE_CONVOY = DIRECTORY + "Emergency Convoy Defense - Player - Low-Atmosphere.xml";
        final String PLAYER_VTOL_CONVOY = DIRECTORY + "Emergency Convoy Defense - Player - VTOL.xml";
        final String PLAYER_CONVOY = DIRECTORY + "Emergency Convoy Defense - Player.xml";

        String templateAddress = GENERIC;

        if (targetConvoy != null) {
            if (forceContainsOnlyAerialForces(targetConvoy)) {
                templateAddress = PLAYER_AEROSPACE_CONVOY;
            } else if (forceContainsMajorityVTOLForces(targetConvoy)) {
                templateAddress = PLAYER_VTOL_CONVOY;
            } else {
                templateAddress = PLAYER_CONVOY;
            }
        }
        ScenarioTemplate template = ScenarioTemplate.Deserialize(templateAddress);

        if (template == null) {
            campaign.addReport(String.format(resources.getString("convoyErrorTemplate.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                templateAddress, CLOSING_SPAN_TAG));
            deliverDrop(droppedItems);
            return;
        }

        if (track == null) {
            campaign.addReport(String.format(resources.getString("convoyErrorTracks.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                templateAddress, CLOSING_SPAN_TAG));
            deliverDrop(droppedItems);
            return;
        }
        StratconScenario scenario = generateExternalScenario(campaign, contract, track,
            convoyGridReference, template, false);

        // If we successfully generated a scenario, we need to make a couple of final
        // adjustments, including assigning the Resupply contents as loot and
        // assigning a player convoy (if appropriate)
        if (scenario != null) {
            AtBDynamicScenario backingScenario = scenario.getBackingScenario();
            backingScenario.setDate(campaign.getLocalDate());

            if (targetConvoy != null) {
                backingScenario.addForce(targetConvoy.getId(), "Player");
                targetConvoy.setScenarioId(backingScenario.getId(), campaign);
                scenario.commitPrimaryForces();
            }

            Loot loot = new Loot();

            if (droppedItems != null) {
                for (Part part : droppedItems) {
                    loot.addPart(part);
                }
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
     * This method iterates over all units in the given force and checks if each unit
     * is an aerospace unit or a conventional fighter. If it finds a unit that isn't
     * one of these types, it immediately returns {@code false}, indicating that the force
     * contains non-aerial units. If it successfully goes through all units without
     * finding a non-aerial unit, it returns {@code true}.
     * <p>
     * If a unit cannot be accounted for (for example, due to a {@code null} reference),
     * it will be ignored and the method will proceed to the next unit.
     *
     * @param targetConvoy The force that is being checked for unit types.
     * @return A boolean indicating whether all units in the force are Aerospace or Conventional
     * Fighter units.
     */
    private boolean forceContainsOnlyAerialForces(@Nullable Force targetConvoy) {
        if (targetConvoy == null) {
            return false;
        }

        for (UUID unitId : targetConvoy.getAllUnits(false)) {
            try {
                Unit unit = campaign.getUnit(unitId);
                Entity entity = unit.getEntity();

                if (!entity.isAerospace() && !entity.isConventionalFighter()) {
                    return false;
                }
            } catch (Exception ignored) {
                // If we run into issues, we can just skip that unit and check the others.
            }
        }

        return true;
    }

    /**
     * This method iterates over all units in the given force and counts the total number of units
     * and the number of VTOL (or WIGE) units. If an error occurs while processing a unit (for
     * example, due to a {@code null} reference), it gets ignored and the method proceeds with the
     * next unit.
     * <p>
     * Once all units are processed, it checks if the number of VTOL (or WIGE) units is more than
     * or equal to a third the total number of units in the force.
     *
     * @param targetConvoy The force that is being checked for unit types.
     * @return A boolean indicating whether the majority of units in the force are of VTOL (or
     * WIGE) types.
     */
    private boolean forceContainsMajorityVTOLForces(@Nullable Force targetConvoy) {
        if (targetConvoy == null) {
            return false;
        }

        int convoySize = 0;
        int vtolCount = 0;

        for (UUID unitId : targetConvoy.getAllUnits(false)) {
            try {
                Unit unit = campaign.getUnit(unitId);
                Entity entity = unit.getEntity();

                if (entity.isAirborneVTOLorWIGE()) {
                    vtolCount++;
                }

                convoySize++;
            } catch (Exception ignored) {
                // If we run into issues, we can just skip that unit and check the others.
            }
        }

        return vtolCount >= Math.floor((double) convoySize / 3);
    }

    /**
     * This method iterates over every unit in the given force and checks if it's a VTOL (or WIGE)
     * unit. If it encounters a unit that is not a VTOL (or WIGE), it immediately returns {@code false},
     * indicating that there exist non-VTOL units in the force. Upon checking all units without
     * finding a non-VTOL (or WIGE) unit, it will return {@code true}.
     * <p>
     * If an error occurs while processing a unit (for example, due to a {@code null} reference),
     * that unit will be ignored and the method proceeds with the next unit.
     *
     * @param targetConvoy The force to be verified if all units are of VTOL (or WIGE) types.
     * @return A boolean indicating whether all units in the force are of VTOL (or WIGE) types or not.
     */
    private boolean forceContainsOnlyVTOLForces(@Nullable Force targetConvoy) {
        if (targetConvoy == null) {
            return false;
        }

        for (UUID unitId : targetConvoy.getAllUnits(false)) {
            try {
                Unit unit = campaign.getUnit(unitId);
                Entity entity = unit.getEntity();

                if (!entity.isAirborneVTOLorWIGE()) {
                    return false;
                }
            } catch (Exception ignored) {
                // If we run into issues, we can just skip that unit and check the others.
            }
        }

        return true;
    }

    /**
     * Picks a random convoy from available ones.
     *
     * @return The ID of the randomly selected convoy.
     */
    @Nullable
    private Force getRandomConvoy() {
        Force[] keys = playerConvoys.keySet().toArray(new Force[0]);
        return keys[random.nextInt(keys.length)];
    }

    /**
     * Creates the final message dialog for the convoy.
     *
     * @param campaign        The current campaign.
     * @param employerFaction The employing faction.
     */
    public static void convoyFinalMessageDialog(Campaign campaign, Faction employerFaction) {
        // Dialog dimensions and representative
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

        // Creates and sets up the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("dialog.title"));
        dialog.setLayout(new BorderLayout());

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
        dialog.setResizable(false);
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
     */
    private void deliverDrop(List<Part> droppedItems) {
        for (Part part : droppedItems) {
            if (part instanceof AmmoBin) {
                campaign.getQuartermaster().addAmmo(((AmmoBin) part).getType(),
                    ((AmmoBin) part).getFullShots() * 5);
            } else if (part instanceof Armor) {
                int quantity = (int) Math.ceil(((Armor) part).getArmorPointsPerTon() * 5);
                ((Armor) part).setAmount(quantity);
                campaign.getWarehouse().addPart(part, true);
            } else {
                campaign.getWarehouse().addPart(part, true);
            }
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

                if (isProhibitedUnitType(entity, false)) {
                    continue;
                }

                if (!unit.isSalvage() && unit.isAvailable()) {
                    List<Part> parts = unit.getParts();
                    for (Part part : parts) {
                        if (isIneligiblePart(part, unit)) {
                            continue;
                        }

                        PartDetails partDetails = new PartDetails(part, 1);

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
     * @param excludeDropShips Whether to exclude DropShips from the check
     * @return Boolean value {@code true} if the entity is large craft, super heavy or conventional
     * infantry, otherwise {@code false}.
     */
    private static boolean isProhibitedUnitType(Entity entity, boolean excludeDropShips) {
        if (entity.isDropShip() && excludeDropShips) {
            return false;
        }

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
            || checkTransporter(part);
    }

    /**
     * Checks if a given part is of a type that is specifically excluded from resupplies.
     * This method should be expanded as ineligible parts are found.
     *
     * @param part The part to check for eligibility.
     * @return     {@code true} if the part is ineligible; {@code false} otherwise.
     */
    private boolean checkExclusionList(Part part) {
        if (part instanceof EquipmentPart) {
            List<BigInteger> excludedTypes = List.of(F_SPONSON_TURRET);
            for (BigInteger excludedType : excludedTypes) {
                if (((EquipmentPart) part).getType().hasFlag(excludedType)) {
                    return true;
                }
            }
        }
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
                || mek.isExtinct(currentYear, employerIsClan, employerTechCode));
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
        } else if (part instanceof MASC
            || part instanceof MekGyro
            || part instanceof EnginePart
            || checkEquipmentSubType(part)) {
            multiplier = 0.5;
        } else if (part instanceof AmmoBin || part instanceof Armor) {
            multiplier = 5;
        }

        return multiplier;
    }

    /**
     * Checks if a given part is a specific class of equipment subtype.
     *
     * <p>The function returns false if the part is an instance of any one of the following:
     * <ul>
     * <li>AmmoBin</li>
     * <li>AmmoStorage</li>
     * <li>BattleArmorEquipmentPart</li>
     * <li>HeatSink</li>
     * <li>JumpJet</li>
     * </ul>
     *
     * @param part The part to check for equipment subtype.
     * @return If the part is not an instance of {@link EquipmentPart}, or it's an instance of
     * {@link EquipmentPart} but not an instance of any one of the above classes, {@code true} is returned.
     */
    private static boolean checkEquipmentSubType(Part part) {
        if (part instanceof EquipmentPart) {
            if (part instanceof AmmoBin) {
                return false;
            }

            if (part instanceof AmmoStorage) {
                return false;
            }

            if (part instanceof BattleArmorEquipmentPart) {
                return false;
            }

            if (part instanceof HeatSink) {
                return false;
            }

            return !(part instanceof JumpJet);
        }

        return true;
    }

    /**
     * Populates the 'partsPool' instance variable with parts from 'potentialParts'.
     */
    private void buildPartsPools(Map<String, PartDetails> potentialParts) {
        partsPool = new ArrayList<>();
        armorPool = new ArrayList<>();
        ammoBinPool = new ArrayList<>();

        for (PartDetails potentialPart : potentialParts.values()) {
            int weight = (int) Math.round(potentialPart.getWeight());
            for (int entry = 0; entry < weight; entry++) {
                Part part = potentialPart.getPart();
                Part preparedPart = preparePart(part);

                // We don't need null protection for 'part' as if 'part' is null preparedPart will
                // just return 'null', which we catch here.
                if (preparedPart == null) {
                    continue;
                }

                if (preparedPart instanceof Armor) {
                    armorPool.add(preparedPart);
                    continue;
                }

                if (preparedPart instanceof AmmoBin) {
                    ammoBinPool.add(preparedPart);
                    continue;
                }

                partsPool.add(preparedPart);
            }
        }

        // Make procurement checks for each of the items in the individual pools
        Procurement procurement = new Procurement(negotiatorSkill, currentYear, employerFaction);

        partsPool = procurement.makeProcurementChecks(partsPool, true, true);
        Collections.shuffle(partsPool);

        armorPool = procurement.makeProcurementChecks(armorPool, true, true);
        Collections.shuffle(armorPool);

        ammoBinPool = procurement.makeProcurementChecks(ammoBinPool, true, true);
        Collections.shuffle(ammoBinPool);
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
    private @Nullable Part preparePart(Part originPart) {
        Part clonedPart = originPart.clone();

        // If we failed to clone a part, it's likely because the part doesn't exist.
        // This means it's been destroyed, and what we're detecting is the absence of a part.
        // This is a major limitation of cloning parts, and one I've not fathomed a solution to.
        if (clonedPart == null) {
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
    private @Nullable Part getRandomPart() {
        if (partsPool.isEmpty()) {
            return null;
        }

        Part randomPart = partsPool.get(random.nextInt(partsPool.size()));
        randomPart.setQuality(getRandomPartQuality(negotiatorSkill));
        return randomPart;
    }

    /**
     * @return A randomly selected {@link Armor} from the armor pool.
     */
    private @Nullable Part getRandomArmor() {
        if (armorPool.isEmpty()) {
            return null;
        }

        Part randomArmor = armorPool.get(random.nextInt(armorPool.size()));
        randomArmor.setQuality(getRandomPartQuality(negotiatorSkill));
        return randomArmor;
    }

    /**
     * @return A randomly selected {@link AmmoBin} from the ammo bin pool.
     */
    private @Nullable Part getRandomAmmoBin() {
        if (ammoBinPool.isEmpty()) {
            return null;
        }

        return ammoBinPool.get(random.nextInt(ammoBinPool.size()));
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
        // If the player hasn't negotiated for straight support,
        // and they are not on a Guerrilla contract,
        // then there is no need to handle resupplies beyond this point.
        if (targetCargoTonnage == 0) {
            return;
        }

        boolean isIndependent = contract.getCommandRights().isIndependent();

        if (!contract.getContractType().isGuerrillaWarfare() && !bypassConvoyNeeds) {
            createPlayerConvoyOptionalDialog();
        }

        if (isIndependent && !usePlayerConvoy && !bypassConvoyNeeds) {
            return;
        }

        if (usePlayerConvoy) {
            targetCargoTonnage = targetCargoTonnage * CARGO_MULTIPLIER;
        }

        if (!isLoot) {
            createResupplyFocusDialog();
        }

        List<Part> droppedItems = new ArrayList<>();
        for (int i = 0; i < dropCount; i++) {
            droppedItems.addAll(getDrops(DropType.ARMOR));
            droppedItems.addAll(getDrops(DropType.AMMO));
            droppedItems.addAll(getDrops(DropType.PARTS));
        }

        if (droppedItems.isEmpty()) {
            campaign.addReport(String.format(resources.getString("convoyUnsuccessful.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));
            return;
        }

        if (contract.getContractType().isGuerrillaWarfare() && !(isLoot || isContractEnd)) {
            smugglerOfferDialog(droppedItems);
        } else {
            supplyDropDialog(droppedItems, isLoot, isContractEnd);
        }
    }

    /**
     * Gets a list of dropped items based on the specified drop type.
     * <p>
     * This method generates a list of dropped parts, armor, or ammo items until a target weight
     * is reached, which is calculated as a certain percentage of the total cargo tonnage, depending
     * on the drop type. The target percentages are 50% for PARTS, 25% for ARMOR, and 25% for AMMO.
     * <p>
     * The parts are chosen randomly, and for high value items, a die roll determines if the item is
     * selected or substituted with another item. High value items are only given one chance per
     * distinct part. The loop continues until the total tonnage of the dropped items reaches
     * the target value.
     * <p>
     * This method modifies the partsPool by removing the dropped items.
     * If the partsPool becomes empty during the generation, the method ends early and returns
     * the list of dropped items so far.
     *
     * @param dropType the type of items to drop, represented as an ItemType.
     * @return a list of dropped parts.
     */
    private List<Part> getDrops(DropType dropType) {
        List<Part> droppedItems = new ArrayList<>();
        double runningTotal = 0;

        double targetValue = switch (dropType) {
            case PARTS -> targetCargoTonnage * focusParts;
            case ARMOR -> targetCargoTonnage * focusArmor;
            case AMMO -> targetCargoTonnage * focusAmmo;
        };

        if (targetValue == 0) {
            return droppedItems;
        }

        while (runningTotal < targetValue) {
            Part potentialPart = switch(dropType) {
                case PARTS -> getRandomPart();
                case ARMOR -> getRandomArmor();
                case AMMO -> getRandomAmmoBin();
            };

            // If we failed to get a potential part, it likely means the pool is empty.
            // Even if the pool isn't empty, it's highly unlikely we'll get a successful pull on
            // future iterations, so we end generation early.
            if (potentialPart == null) {
                return droppedItems;
            }

            boolean partFetched = false;

            // For particularly valuable items, we roll a follow-up die to see if the item
            // is actually picked, or if the supplier substitutes it with another item.
            if (potentialPart.getUndamagedValue().isGreaterThan(HIGH_VALUE_ITEM)) {
                if (Compute.d6(1) == 6) {
                    partFetched = true;
                }

                // For really expensive items, the player only has one chance per distinct part.
                switch (dropType) {
                    case PARTS -> partsPool.removeAll(Collections.singleton(potentialPart));
                    case ARMOR -> armorPool.removeAll(Collections.singleton(potentialPart));
                    case AMMO -> ammoBinPool.removeAll(Collections.singleton(potentialPart));
                }
            } else {
                partFetched = true;
            }

            if (partFetched) {
                switch (dropType) {
                    case PARTS -> partsPool.remove(potentialPart);
                    case ARMOR -> armorPool.remove(potentialPart);
                    case AMMO -> ammoBinPool.remove(potentialPart);
                }

                runningTotal += potentialPart.getTonnage();
                droppedItems.add(potentialPart);
            }
        }
        return droppedItems;
    }

    /**
     * This method creates a mapping report based on the provided dropped items. The report indicates
     * the types of parts that have been dropped, the quantity of each type of part, and makes
     * special note of those parts that are considered 'extinct' according to the year and the origin
     * faction.
     *
     * @param droppedItems List of {@link Part} objects that were included in the supply drop.
     * @return A map containing the dropped parts and their quantities.
     */
    public List<String> createPartsReport(List<Part> droppedItems) {
        int year = campaign.getGameYear();
        Faction originFaction = campaign.getFaction();

        Map<String, Integer> entries = droppedItems.stream().collect(Collectors.toMap(
                part -> {
                    String name = part.getName();
                    String quality = part.getQualityName();

                    String append = part.isClan() ? " (Clan)" : "";
                    append = part.isMixedTech() ? " (Mixed)" : append;
                    append += " (" + quality + ')';
                    append += part.isExtinct(year, originFaction.isClan(), getFactionTechCode(originFaction)) ?
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
                        return ((AmmoBin) part).getFullShots() * 5;
                    } else if (part instanceof Armor) {
                        return (int) Math.ceil(((Armor) part).getArmorPointsPerTon() * 5);
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
        // Retrieves the title from the resources
        String title = resources.getString("dialog.title");

        // An ImageIcon to hold the faction icon
        ImageIcon icon = getCampaignFactionIcon(campaign);

        // Create a text pane to display the message
        String message = getContractStartMessage(campaign, contract);
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

        dialog.setResizable(false);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Calculates the convoy count and total cargo capacity of the player's convoys.
     */
    private void calculatePlayerConvoyValues() {
        playerConvoys = new HashMap<>();
        totalPlayerCargoCapacity = 0;

        for (Force force : campaign.getAllForces()) {

            if (!force.isConvoyForce()) {
                continue;
            }

            double cargoCapacitySubTotal = 0;
            if (force.isConvoyForce()) {
                boolean hasCargo = false;
                for (UUID unitId : force.getAllUnits(false)) {
                    try {
                        Unit unit = campaign.getUnit(unitId);
                        Entity entity = unit.getEntity();

                        if (unit.isDamaged()
                            || !unit.isFullyCrewed()
                            || isProhibitedUnitType(entity, true)) {
                            continue;
                        }

                        double individualCargo = unit.getCargoCapacity();

                        if (individualCargo > 0) {
                            hasCargo = true;
                        }

                        cargoCapacitySubTotal += individualCargo;
                    } catch (Exception ignored) {
                        // If we run into an exception, it's because we failed to get Unit or Entity.
                        // In either case, we just ignore that unit.
                    }
                }

                if (hasCargo) {
                    if (cargoCapacitySubTotal > 0) {
                        totalPlayerCargoCapacity += cargoCapacitySubTotal;
                        playerConvoys.put(force, cargoCapacitySubTotal);
                    }
                }
            }
        }
    }

    public void createPlayerConvoyOptionalDialog() {
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

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

        // Create and display the message
        String pluralizer = playerConvoys.size() != 1 ? "s" : "";
        String messageResource;

        if (isUsePlayerConvoy()) {
            messageResource = resources.getString("usePlayerConvoyForced.text");
        } else {
            messageResource = resources.getString("usePlayerConvoyOptional.text");
        }

        String message = String.format(messageResource, getCommanderTitle(campaign, false),
            calculateTargetCargoTonnage(campaign, contract) * CARGO_MULTIPLIER,
            totalPlayerCargoCapacity, playerConvoys.size(), pluralizer, pluralizer);

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
        acceptButton.setEnabled(!playerConvoys.isEmpty());

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

        dialog.setResizable(false);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    public void createResupplyFocusDialog() {
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

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

        // Create and display the message
        String messageResource = resources.getString("focusDescription.text");
        String message = String.format(messageResource, getCommanderTitle(campaign, false));

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
        JButton optionBalanced = new JButton(resources.getString("optionBalanced.text"));
        optionBalanced.addActionListener(e -> {
            dialog.dispose();
            // The class initialization assumes a balanced approach
        });

        // The player should not be able to focus on parts for game balance reasons.
        // If the player could pick parts, the optimum choice would be to always pick parts.

        JButton optionArmor = new JButton(resources.getString("optionArmor.text"));
        optionArmor.addActionListener(e -> {
            dialog.dispose();
            focusAmmo = 0;
            focusArmor = 0.75;
            focusParts = 0;
        });

        JButton optionAmmo = new JButton(resources.getString("optionAmmo.text"));
        optionAmmo.addActionListener(e -> {
            dialog.dispose();
            focusAmmo = 0.75;
            focusArmor = 0;
            focusParts = 0;
        });

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(optionBalanced);
        buttonPanel.add(optionArmor);
        buttonPanel.add(optionAmmo);

        // Add the original panel and button panel to the dialog
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setResizable(false);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Creates a start message for a contract based on its type and resupply details.
     * The message is formatted using a specific template according to the type of
     * the contract and whether it involves guerrilla warfare or independent command rights.
     *
     * @param campaign          The current campaign.
     * @param contract          The contract for which the start message is created.
     * @return A formatted start message for the contract, enclosed within an HTML div with a defined width.
     */
    private static String getContractStartMessage(Campaign campaign, AtBContract contract) {
        int convoyCount = 0;
        double totalCargoCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (force.isConvoyForce() && force.isStrategicFormation()) {
                boolean hasCargo = false;
                for (UUID unitId : force.getAllUnits(false)) {
                    Unit unit = campaign.getUnit(unitId);

                    if (unit != null) {
                        if (unit.isDamaged()) {
                            continue;
                        }

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

            convoyMessage = String.format(convoyMessageTemplate, commanderTitle,
                getEstimatedCargoRequirements(campaign, contract), totalCargoCapacity, convoyCount,
                convoyCount != 1 ? "s" : "");
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
    private final Part part;
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
