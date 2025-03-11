/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.mission.resupplyAndCaches;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import org.apache.commons.math3.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

import static megamek.common.EntityWeightClass.WEIGHT_ASSAULT;
import static megamek.common.EntityWeightClass.WEIGHT_HEAVY;
import static megamek.common.EntityWeightClass.WEIGHT_LIGHT;
import static megamek.common.EntityWeightClass.WEIGHT_MEDIUM;
import static megamek.common.Mek.LOC_CT;
import static megamek.common.UnitType.AEROSPACEFIGHTER;
import static megamek.common.UnitType.INFANTRY;
import static megamek.common.UnitType.MEK;
import static megamek.common.UnitType.TANK;
import static mekhq.campaign.finances.enums.TransactionType.MISCELLANEOUS;
import static mekhq.campaign.mission.BotForceRandomizer.UNIT_WEIGHT_UNSPECIFIED;
import static mekhq.campaign.unit.Unit.getRandomUnitQuality;
import static mekhq.campaign.universe.Factions.getFactionLogo;

public class StarLeagueCache {
    private final Campaign campaign;
    private final AtBContract contract;
    private final int cacheType;
    private final Random random = new Random();
    private Faction originFaction;
    private boolean didGenerationFail = false;
    private final int ruinedChance;
    private Map<Part, Integer> partsPool;
    private List<Unit> intactUnits;

    // We use year -1 as otherwise MHQ considers the SL to no longer exist.
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");
    private final LocalDate FALL_OF_STAR_LEAGUE = LocalDate.of(
        Factions.getInstance().getFaction("SL").getEndYear() - 1, 1, 1);

    public enum CacheType {
        TRASH_CACHE, // The cache contains only trash or roleplay items.
        CLUE_CACHE, // The cache contains a clue that will lead the player to another cache.
        DATA_CACHE, // The cache contains a memory core
        LEGACY_CACHE, // The cache contains a message from the past, improving Loyalty across the campaign
        TRAP_CACHE, // The cache is a trap
        COMBAT_CACHE // The cache contains units and parts
    }

    private final static MMLogger logger = MMLogger.create(StarLeagueCache.class);

    public static int getCacheType() {
        return Compute.randomInt(CacheType.values().length);
    }

    public Faction getFaction() {
        return originFaction;
    }

    public StarLeagueCache(Campaign campaign, AtBContract contract, int cacheType) {
        this.campaign = campaign;
        this.contract = contract;
        this.cacheType = cacheType;

        ruinedChance = campaign.getGameYear() - FALL_OF_STAR_LEAGUE.getYear();

        determineOriginFaction();
    }

    private void generateCombatCacheContents() {
        if (!didGenerationFail) {
            intactUnits = getCacheContents();
            processUnits();
        }

        if (partsPool.isEmpty() && intactUnits.isEmpty()) {
            didGenerationFail = true;
        }
    }

    public boolean didGenerationFail() {
        return didGenerationFail;
    }

    private void processUnits() {
        int intactUnitCount = 0;

        for (int lance = 0; lance < contract.getRequiredCombatTeams(); lance++) {
            // This will generate a number between 1 and 4 with an average roll of 3
            intactUnitCount += Compute.randomInt(3) + Compute.randomInt(3);
        }

        intactUnitCount = Math.min(intactUnitCount, intactUnits.size());

        for (int individualUnit = 0; individualUnit < ruinedChance; individualUnit++) {
            if (Compute.randomInt(500) < ruinedChance) {
                intactUnitCount--;
            }
        }

        List<Unit> actuallyIntactUnits = new ArrayList<>();
        for (int i = 0; i < intactUnitCount; i++) {
            int randomIndex = random.nextInt(intactUnits.size());
            actuallyIntactUnits.add(intactUnits.get(randomIndex));
            intactUnits.remove(randomIndex);
        }

        // This uses the end state of intact units as the list of units too ruined for salvage
        collectParts();

        // We then replace 'intactUnits' with the actually intact units.
        intactUnits = actuallyIntactUnits;
    }

    private void collectParts() {
        partsPool = new HashMap<>();

        try {
            for (Unit unit : intactUnits) {
                List<Part> parts = unit.getParts();
                for (Part part : parts) {
                    if (part instanceof MekLocation) {
                        if (((MekLocation) part).getLoc() == LOC_CT) {
                            continue;
                        }
                    }

                    if (part instanceof TankLocation) {
                        continue;
                    }

                    // Is the part too damaged to be salvaged?
                    if (Compute.randomInt(500) < ruinedChance) {
                        continue;
                    }

                    Pair<Unit, Part> pair = new Pair<>(unit, part);
//                    int weight = getDropWeight(pair.getValue());
//                    partsPool.merge(part, weight, Integer::sum);
                }
            }
        } catch (Exception exception) {
            logger.error("Aborted parts collection.", exception);
        }
    }

    public void determineOriginFaction() {
        final int sphereOfInfluence = 650;
        final PlanetarySystem contractSystem = contract.getSystem();
        final double distanceToTerra = contractSystem.getDistanceTo(campaign.getSystemById("Terra"));

        // This is a fallback to better ensure something drops, even if it isn't a SLDF Depot
        // This value was reached by 'eye-balling' the map of the Inner Sphere
        if (distanceToTerra > sphereOfInfluence) {
            List<String> factions = contractSystem.getFactions(FALL_OF_STAR_LEAGUE);

            if (factions.isEmpty()) {
                didGenerationFail = true;
            } else {
                Collections.shuffle(factions);
                originFaction = Factions.getInstance().getFaction(factions.get(0));
            }
        } else {
            originFaction = Factions.getInstance().getFaction("SL");
        }
    }

    private List<Unit> getCacheContents() {
        Map<Integer, List<Integer>> unitsPresent = buildUnitWeightMap();
        List<MekSummary> unitSummaries = getUnitSummaries(unitsPresent);

        List<Unit> units = new ArrayList<>();
        for (MekSummary summary : unitSummaries) {
            Entity entity = getEntity(summary);

            if (entity == null) {
                continue;
            }

            Unit unit = getUnit(entity);

            if (unit != null) {
                units.add(unit);
            }
        }

        return units;
    }

    @Nullable
    private Entity getEntity(MekSummary unitData) {
        try {
            return new MekFileParser(unitData.getSourceFile(), unitData.getEntryName()).getEntity();
        } catch (Exception ex) {
            logger.error("Unable to load entity: {}: {}",
                unitData.getSourceFile(),
                unitData.getEntryName(), ex);
            return null;
        }
    }

    public Unit getUnit(Entity entity) {
        PartQuality quality = getRandomUnitQuality(0);
        Unit unit = new Unit(entity, campaign);
        unit.initializeParts(true);
        unit.setQuality(quality);

        return unit;
    }

    private List<MekSummary> getUnitSummaries(Map<Integer, List<Integer>> unitsPresent) {
        final List<Integer> potentialUnitTypes = List.of(INFANTRY, TANK, MEK, AEROSPACEFIGHTER);

        List<MekSummary> unitSummaries = new ArrayList<>();
        for (int unitType : potentialUnitTypes) {
            for (int unitWeight : unitsPresent.get(unitType)) {
                unitSummaries.add(campaign.getUnitGenerator().generate(originFaction.getShortName(), unitType, unitWeight,
                    FALL_OF_STAR_LEAGUE.getYear(), getRandomUnitQuality(0).toNumeric()));
            }
        }

        return unitSummaries;
    }

    private Map<Integer, List<Integer>> buildUnitWeightMap() {
        final int COMPANY_COUNT = 3;
        Map<Integer, List<Integer>> unitsPresent = new HashMap<>();

        for (int company = 0; company < COMPANY_COUNT; company++) {
            int unitType = getCompanyUnitType();

            if (unitType == INFANTRY) {
                unitsPresent.put(INFANTRY, List.of(UNIT_WEIGHT_UNSPECIFIED, UNIT_WEIGHT_UNSPECIFIED,
                    UNIT_WEIGHT_UNSPECIFIED));
            } else {
                for (int lance : getCompanyLances()) {
                    if (unitsPresent.containsKey(unitType)) {
                        unitsPresent.get(unitType).addAll(getUnitWeights(lance));
                    } else {
                        unitsPresent.put(unitType, getUnitWeights(lance));
                    }
                }
            }
        }
        return unitsPresent;
    }

    private int getCompanyUnitType() {
        int roll = Compute.d6();
        // This table is based on the one found on p265 of Total Warfare.
        // We increased the chance of rolling 'Meks, because players will expect to get 'Meks out
        // of these caches
        return switch (roll) {
            case 1 -> INFANTRY;
            case 2, 3 -> TANK;
            case 4, 5 -> MEK;
            case 6 -> AEROSPACEFIGHTER;
            default -> throw new IllegalStateException("Unexpected value in getCompanyUnitType: "
                + roll);
        };

    }

    private List<Integer> getCompanyLances() {
        List<Integer> companyLances = new ArrayList<>();

        int roll = Compute.d6(1);
        // This table is based on the one found on p265 of Total Warfare
        switch (roll) {
            case 1 -> {
                companyLances.add(WEIGHT_LIGHT);
                companyLances.add(WEIGHT_MEDIUM);
                companyLances.add(WEIGHT_MEDIUM);
            }
            case 2 -> {
                companyLances.add(WEIGHT_LIGHT);
                companyLances.add(WEIGHT_MEDIUM);
                companyLances.add(WEIGHT_HEAVY);
            }
            case 3 -> {
                companyLances.add(WEIGHT_MEDIUM);
                companyLances.add(WEIGHT_MEDIUM);
                companyLances.add(WEIGHT_HEAVY);
            }
            case 4 -> {
                companyLances.add(WEIGHT_LIGHT);
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_HEAVY);
            }
            case 5 -> {
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_HEAVY);
            }
            case 6 -> {
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_ASSAULT);
            }
            default -> throw new IllegalStateException("Unexpected value in getCompanyLances(): "
                + roll);
        }

        return companyLances;
    }

    private List<Integer> getUnitWeights(int weight) {
        List<Integer> unitWeights = new ArrayList<>();
        final int[] rollOutcome;

        int roll = Compute.d6(2);
        // This table is based on the one found on p265 of Total Warfare
        switch (roll) {
            case 1 -> rollOutcome = switch (weight) {
                case WEIGHT_LIGHT -> new int[]{WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_LIGHT};
                case WEIGHT_MEDIUM -> new int[]{WEIGHT_LIGHT, WEIGHT_MEDIUM, WEIGHT_MEDIUM, WEIGHT_HEAVY};
                case WEIGHT_HEAVY -> new int[]{WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_HEAVY};
                case WEIGHT_ASSAULT -> new int[]{WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_ASSAULT, WEIGHT_ASSAULT};
                default -> throw new IllegalStateException("Unexpected weight: " + weight);
            };
            case 2, 3 -> rollOutcome = switch (weight) {
                case WEIGHT_LIGHT -> new int[]{WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_MEDIUM};
                case WEIGHT_MEDIUM, WEIGHT_HEAVY -> new int[]{weight, weight, weight, weight};
                case WEIGHT_ASSAULT -> new int[]{WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_ASSAULT, WEIGHT_ASSAULT};
                default -> throw new IllegalStateException("Unexpected weight: " + weight);
            };
            case 4, 5 -> rollOutcome = switch (weight) {
                case WEIGHT_LIGHT -> new int[]{WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_MEDIUM, WEIGHT_MEDIUM};
                case WEIGHT_MEDIUM -> new int[]{WEIGHT_MEDIUM, WEIGHT_MEDIUM, WEIGHT_MEDIUM, WEIGHT_HEAVY};
                case WEIGHT_HEAVY -> new int[]{WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_ASSAULT};
                case WEIGHT_ASSAULT -> new int[]{WEIGHT_HEAVY, WEIGHT_ASSAULT, WEIGHT_ASSAULT, WEIGHT_ASSAULT};
                default -> throw new IllegalStateException("Unexpected weight: " + weight);
            };
            case 6 -> rollOutcome = switch (weight) {
                case WEIGHT_LIGHT -> new int[]{WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_MEDIUM, WEIGHT_HEAVY};
                case WEIGHT_MEDIUM -> new int[]{WEIGHT_MEDIUM, WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_HEAVY};
                case WEIGHT_HEAVY -> new int[]{WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_ASSAULT};
                case WEIGHT_ASSAULT -> new int[]{WEIGHT_ASSAULT, WEIGHT_ASSAULT, WEIGHT_ASSAULT, WEIGHT_ASSAULT};
                default -> throw new IllegalStateException("Unexpected weight: " + weight);
            };
            default -> throw new IllegalStateException("Unexpected value in getlanceWeights(): " + roll);
        }

        for (int outcome : rollOutcome) {
            unitWeights.add(outcome);
        }

        return unitWeights;
    }

    public void createDudDialog(StratconTrackState track, StratconScenario scenario) {
        StratconCoords stratconCoords = scenario.getCoords();

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
        ActionListener dialogDismissActionListener = e -> dialog.dispose();

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

        ImageIcon speakerIcon = getSpeakerIcon(false);
//        speakerIcon = scaleImageIconToWidth(speakerIcon, UIUtil.scaleForGUI(100));
        iconLabel.setIcon(speakerIcon);
        dialog.add(iconLabel, BorderLayout.NORTH);

        // Prepares and adds the description
//        JLabel description = new JLabel(
//            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
//                UIUtil.scaleForGUI(DIALOG_WIDTH), getDudDialogText(track, stratconCoords)));
//        description.setHorizontalAlignment(JLabel.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
            String.format(resources.getString("dialogBorderTitle.text"), "PLACEHOLDER")));
//        descriptionPanel.add(description);
        dialog.add(descriptionPanel, BorderLayout.CENTER);

        // Prepares and adds the confirm button
        JButton confirmButton = new JButton(resources.getString("confirmDud.text"));
        confirmButton.addActionListener(dialogDismissActionListener);
        dialog.add(confirmButton,  BorderLayout.SOUTH);

        // Pack, position and display the dialog
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    public void createProposalDialog() {
        Money proposal = calculateProposal();

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
            campaign.getFinances().credit(MISCELLANEOUS, campaign.getLocalDate(), proposal,
                resources.getString("transaction.text"));
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

        ImageIcon speakerIcon = getSpeakerIcon(true);
//        speakerIcon = scaleImageIconToWidth(speakerIcon, UIUtil.scaleForGUI(100));
        iconLabel.setIcon(speakerIcon);
        dialog.add(iconLabel, BorderLayout.NORTH);

        // Prepares and adds the description
//        JLabel description = new JLabel(
//            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
//                UIUtil.scaleForGUI(DIALOG_WIDTH), getProposalText(proposal)));
//        description.setHorizontalAlignment(JLabel.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
            String.format(resources.getString("dialogBorderTitle.text"),
                resources.getString("senderUnknown.text"))));
//        descriptionPanel.add(description);
        dialog.add(descriptionPanel, BorderLayout.CENTER);

        // Prepares and adds the accept button
        JButton acceptDialog = new JButton(resources.getString("propositionAccept.text"));
        acceptDialog.addActionListener(dialogDismissActionListener);

        // Prepares and adds the refuse button
        JButton refuseDialog = new JButton(resources.getString("propositionRefuse.text"));
        refuseDialog.addActionListener(e -> {
            dialog.dispose();
            createProposalRefusalConfirmationDialog(proposal);
        });

        // Creates a panel to house both buttons
        JPanel actionsPanel = new JPanel(new FlowLayout());
        actionsPanel.add(acceptDialog);
        actionsPanel.add(refuseDialog);
        dialog.add(actionsPanel, BorderLayout.SOUTH);

        // Pack, position and display the dialog
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    public void createProposalRefusalConfirmationDialog(Money proposal) {
        // Dialog dimensions and representative
        final int DIALOG_WIDTH = 300;
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
            campaign.getFinances().credit(MISCELLANEOUS, campaign.getLocalDate(), proposal,
                resources.getString("transaction.text"));
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

        ImageIcon speakerIcon = getSpeakerIcon(true);
//        speakerIcon = scaleImageIconToWidth(speakerIcon, UIUtil.scaleForGUI(100));
        iconLabel.setIcon(speakerIcon);
        dialog.add(iconLabel, BorderLayout.NORTH);

        // Prepares and adds the description
        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                UIUtil.scaleForGUI(DIALOG_WIDTH), resources.getString("warning.text")));
        description.setHorizontalAlignment(JLabel.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
            String.format(resources.getString("dialogBorderTitle.text"),
                resources.getString("senderUnknown.text"))));
        descriptionPanel.add(description);
        dialog.add(descriptionPanel, BorderLayout.CENTER);

        // Prepares and adds the accept button
        JButton acceptDialog = new JButton(resources.getString("propositionAccept.text"));
        acceptDialog.addActionListener(dialogDismissActionListener);

        // Prepares and adds the refuse button
        JButton refuseDialog = new JButton(resources.getString("propositionRefuse.text"));
        refuseDialog.addActionListener(e -> {
            dialog.dispose();
            createProposalRefusalConfirmationDialog(proposal);
        });

        // Creates a panel to house both buttons
        JPanel actionsPanel = new JPanel(new FlowLayout());
        actionsPanel.add(acceptDialog);
        actionsPanel.add(refuseDialog);
        dialog.add(actionsPanel, BorderLayout.SOUTH);

        // Pack, position and display the dialog
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private Money calculateProposal() {
        Money proposal = contract.getTotalAmount();
        double proposalValue = proposal.getAmount().doubleValue();
        double roundedValue = Math.ceil(proposalValue / 1_000_000) * 1_000_000;
        return Money.of(roundedValue);
    }

//    private String getProposalText(Money proposal) {
//        String commanderTitle = getCommanderTitle(campaign, true);
//
//        return String.format(resources.getString("proposition" + Compute.randomInt(100) + ".text"),
//            commanderTitle) + "<br><br>" + String.format(resources.getString("propositionValue.text"),
//            proposal.toAmountAndSymbolString());
//    }

//    private String getDudDialogText(StratconTrackState track, StratconCoords stratconCoords) {
//        final String DUD_FORWARD = "dud";
//        final String DUD_AFTERWARD = ".text";
//
//        String commanderTitle = getCommanderTitle(campaign, false);
//        String gridReference = track.toString() + '-' + stratconCoords.toBTString();
//
//        int roll = Compute.d6(1);
//        if ((roll <= 2) || !(Objects.equals(originFaction.getShortName(), "SL"))) {
//            return String.format(resources.getString(DUD_FORWARD + "Generic" +
//                Compute.randomInt(100) + DUD_AFTERWARD), commanderTitle, gridReference,
//                originFaction.getFullName(campaign.getGameYear()));
//        } else {
//            return String.format(resources.getString(DUD_FORWARD + "StarLeague"
//                + Compute.randomInt(100) + DUD_AFTERWARD), commanderTitle, gridReference);
//        }
//    }

    @Nullable
    private ImageIcon getSpeakerIcon(boolean isAnon) {
        if (isAnon) {
            return new ImageIcon("data/images/portraits/default.gif");
        } else {
            return getFactionLogo(campaign, campaign.getFaction().getShortName(), true);
        }
    }
}
