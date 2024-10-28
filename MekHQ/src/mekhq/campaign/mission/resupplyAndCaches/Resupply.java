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
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
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

import static mekhq.campaign.finances.enums.TransactionType.BONUS_EXCHANGE;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.CRITICAL;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.DOMINATING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.stratcon.StratconContractInitializer.getUnoccupiedCoords;
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
        this.employerFaction = contract.getEmployerFaction();
        this.enemyFaction = contract.getEnemy();

        YEAR = campaign.getGameYear();
        EMPLOYER_IS_CLAN = enemyFaction.isClan();
        EMPLOYER_TECH_CODE = getTechFaction(employerFaction);

        if (!skipParts) {
            this.potentialParts = new HashMap<>();
            collectParts(campaign.getUnits());
            buildPool();
        }

        if (!skipUnits) {
            this.potentialUnits = new ArrayList<>();
        }

        this.random = new Random();
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

        StringBuilder description = new StringBuilder(getDescription(isLoot, isContractEnd));

        Map<String, Integer> partsReport = new HashMap<>();
        if (droppedItems != null) {
            partsReport = createPartsReport(droppedItems, campaign.getGameYear(), enemyFaction);
        }

        Map<String, Integer> unitsReport = new HashMap<>();
        if (droppedUnits != null) {
            unitsReport = createUnitsReport(droppedUnits, campaign.getGameYear(), enemyFaction);
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

        description.append("<table><tr valign='top'>")
            .append("<td>").append(columns[0]).append("</td>")
            .append("<td>").append(columns[1]).append("</td>")
            .append("<td>").append(columns[2]).append("</td>")
            .append("</tr></table>");

        JDialog dialog = createResupplyDialog(icon, description.toString(), droppedItems, droppedUnits,
            cashReward, isLoot || isContractEnd);

        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);
    }

    public JDialog createResupplyDialog(ImageIcon icon, String description, @Nullable List<Part> droppedItems,
                                        @Nullable List<Unit> droppedUnits, Money cashReward,
                                        boolean isLootOrContractEnd) {
        final int DIALOG_WIDTH = 900;
        final int DIALOG_HEIGHT = 500;
        final String title = resources.getString("dialog.title");

        JDialog dialog = new JDialog();
        dialog.setSize(UIUtil.scaleForGUI(DIALOG_WIDTH, DIALOG_HEIGHT));
        dialog.setLocationRelativeTo(null);
        dialog.setTitle(title);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout());

        ActionListener dialogActionListener = e -> {
            dialog.dispose();

            if (isLootOrContractEnd) {
                deliverDrop(droppedItems, droppedUnits, cashReward);
            } else {
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

        JLabel label = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                UIUtil.scaleForGUI(DIALOG_WIDTH), description));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);

        JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton okButton = new JButton(resources.getString("confirmReceipt.text"));
        dialog.add(okButton, BorderLayout.SOUTH);
        okButton.addActionListener(dialogActionListener);

        return dialog;
    }

    private void processConvoy(@Nullable List<Part> droppedItems, @Nullable List<Unit> droppedUnits,
                               Money cashReward) {
        final String STATUS_FORWARD = "statusUpdate";
        final String STATUS_AFTERWARD = ".text";

        AtBMoraleLevel morale = contract.getMoraleLevel();

        int interceptionChance = switch (morale) {
            case ROUTED -> 0;
            case CRITICAL -> 1;
            case WEAKENED -> 2;
            case STALEMATE -> 3;
            case ADVANCING -> 4;
            case DOMINATING -> 5;
            case OVERWHELMING -> 6;
        };

        boolean isIntercepted = false;
        String message = "";

        if (Compute.randomInt(10) < interceptionChance) {
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
        } else if (Compute.randomInt(10) < interceptionChance) {
            message = resources.getString(STATUS_FORWARD + "Intercepted" +
                Compute.randomInt(20) + STATUS_AFTERWARD);
            isIntercepted = true;
        }

        if (!message.isEmpty()) {
            createConvoyMessage(droppedItems, droppedUnits, cashReward, message, isIntercepted);
        } else {
            campaign.addReport(String.format(resources.getString("convoySuccessful.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            deliverDrop(droppedItems, droppedUnits, cashReward);
        }
    }

    public void createConvoyMessage(@Nullable List<Part> droppedItems, @Nullable List<Unit> droppedUnits,
                                    Money cashReward, String convoyStatusMessage, boolean isIntercepted) {
        createConvoyMessage(droppedItems, droppedUnits, cashReward, convoyStatusMessage, isIntercepted,
            true);
    }

    public void createConvoyMessage(@Nullable List<Part> droppedItems, @Nullable List<Unit> droppedUnits,
                                       Money cashReward, String convoyStatusMessage, boolean isIntercepted,
                                    boolean isIntroduction) {
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
                createConvoyMessage(droppedItems, droppedUnits, cashReward, convoyStatusMessage,
                    isIntercepted, false);
            } else {
                if (isIntercepted) {
                    if (campaign.getCampaignOptions().isUseStratCon()) {
                        StratconCampaignState campaignState = contract.getStratconCampaignState();

                        // Pick a random track
                        List<StratconTrackState> track = campaignState.getTracks();
                        StratconTrackState randomTrack = track.get(random.nextInt(track.size()));

                        // Select a set of unoccupied coordinates
                        StratconCoords coords = getUnoccupiedCoords(randomTrack);



                        campaign.addReport(String.format(resources.getString("convoyInterceptedStratCon.text"),
                            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                            CLOSING_SPAN_TAG, randomTrack.getDisplayableName()));
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
        if (isIntroduction) {
            if (logisticsOfficer == null) {
                iconLabel.setIcon(Factions.getFactionLogo(campaign, campaign.getFaction().getShortName(),
                    true));
            } else {
                iconLabel.setIcon(logisticsOfficer.getPortrait().getImageIcon());
            }
        } else {
            iconLabel.setIcon(Factions.getFactionLogo(campaign, employerFaction.getShortName(),
                true));
        }
        dialog.add(iconLabel, BorderLayout.NORTH);

        // Prepares and adds the description
        String message = convoyStatusMessage;
        String speaker = "";
        if (isIntroduction) {
            message = String.format(resources.getString("logisticsMessage.text"),
                getCommanderTitle(false)) + "<br>";

            if (logisticsOfficer != null) {
                speaker = "<b>" + logisticsOfficer.getFullTitle() + "</b><br><br>";
            }
        } else {
            message = String.format(message, getCommanderTitle(false));
        }

        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s%s</div></html>",
            UIUtil.scaleForGUI(DIALOG_WIDTH), speaker, message));
        description.setHorizontalAlignment(JLabel.CENTER);
        dialog.add(description, BorderLayout.CENTER);

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

    private String getCommanderTitle(boolean includeSurname) {
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
    private int getDropWeight(Part part) {
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
     *
     * @param  units A collection of {@code Unit} objects from which parts will be extracted.
     */
    private void collectParts(Collection<Unit> units) {
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
    private void buildPool() {
        partsPool = new ArrayList<>(potentialParts.keySet());

        if (!partsPool.isEmpty()) {
            Collections.shuffle(partsPool);
        }
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

        supplyDropDialog(droppedItems, cashReward, isLoot, isContractEnd);
    }

    /**
     * This method creates a mapping report based on the provided dropped items. The report indicates
     * the types of parts that have been dropped, the quantity of each type of part, and makes
     * special note of those parts that are considered 'extinct' according to the year and the origin
     * faction.
     *
     * @param droppedItems List of {@link Part} objects that were included in the supply drop. Can be
     * {@code null}.
     * @param year The current year in the game.
     * @param originFaction The faction associated with the supply drop.
     * @return A map containing the dropped parts and their quantities.
     */
    public Map<String, Integer> createPartsReport(@Nullable List<Part> droppedItems, int year,
                                                  Faction originFaction) {
        return droppedItems.stream()
            .collect(Collectors.toMap(
                part -> {
                    String name = part.getName();

                    String append = part.isClan() ? " (Clan)" : "";
                    append = part.isMixedTech() ? " (Mixed)" : append;
                    append += part.isExtinct(year, originFaction.isClan(), EMPLOYER_TECH_CODE) ?
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
     * @param year The current year in the game.
     * @param originFaction The faction associated with the supply drop.
     * @return A map containing the dropped units and their quantities.
     */
    public Map<String, Integer> createUnitsReport(@Nullable List<Unit> droppedUnits, int year, Faction originFaction) {
        return droppedUnits.stream()
            .collect(Collectors.toMap(
                unit -> {
                    String append = unit.isClan() ? " (Clan)" : "";
                    append = unit.isMixedTech() ? " (Mixed)" : append;
                    append += unit.isExtinct(year, originFaction.isClan(), EMPLOYER_TECH_CODE) ?
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
    private String getDescription(boolean isLoot, boolean isContractEnd) {
        if (isLoot) {
            return resources.getString("salvaged" + Compute.randomInt(10) + ".text");
        }

        if (isContractEnd) {
            return resources.getString("looted" + Compute.randomInt(10) + ".text");
        }

        AtBMoraleLevel morale = contract.getMoraleLevel();

        if (morale.isRouted()) {
            return resources.getString("adhocSupplies" + Compute.randomInt(20) + ".text");
        } else {
            return resources.getString(morale.toString().toLowerCase() + Compute.randomInt(10) + ".text");
        }
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
    private static PartQuality getRandomPartQuality(int modifier) {
        return getRandomUnitQuality(modifier);
    }
}
