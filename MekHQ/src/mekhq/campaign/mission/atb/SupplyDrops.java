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
package mekhq.campaign.mission.atb;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.ITechnology;
import megamek.common.Mek;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.apache.commons.math3.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static mekhq.campaign.finances.enums.TransactionType.BONUS_EXCHANGE;
import static mekhq.campaign.unit.Unit.getRandomUnitQuality;
import static mekhq.campaign.universe.Factions.getFactionLogo;

public class SupplyDrops {
    final private Campaign campaign;
    final private Faction employerFaction;
    final boolean isLosTechCache;
    private Map<Part, Integer> potentialParts;
    private List<Part> partsPool;
    private Random random;

    private final int YEAR;
    private final int EMPLOYER_TECH_CODE;
    private final boolean EMPLOYER_IS_CLAN;
    private final Money TARGET_VALUE = Money.of(250000);

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.SupplyDrops");
    private final static MMLogger logger = MMLogger.create(SupplyDrops.class);

    public SupplyDrops(Campaign campaign, Faction employerFaction, boolean isLosTechCache) {
        // Constants
        YEAR = campaign.getGameYear();
        EMPLOYER_IS_CLAN = employerFaction.isClan();
        EMPLOYER_TECH_CODE = getTechFaction(employerFaction);

        this.campaign = campaign;
        this.employerFaction = employerFaction;
        this.isLosTechCache = isLosTechCache;
        initialize();
    }

    private void initialize() {
        collectParts();
        buildPool();

        random = new Random();
    }

    private void collectParts() {
        potentialParts = new HashMap<>();
        final LocalDate BATTLE_OF_TUKAYYID = LocalDate.of(3052, 5, 21);

        try {
            Collection<Unit> units = campaign.getUnits();
            for (Unit unit : units) {
                Entity entity = unit.getEntity();

                if (entity.isLargeCraft() || entity.isSuperHeavy()) {
                    continue;
                }

                if (!unit.isSalvage() && unit.isAvailable()) {
                    List<Part> parts = unit.getParts();
                    for (Part part : parts) {
                        if (part instanceof MekLocation) {
                            if (((MekLocation) part).getLoc() == Mek.LOC_CT) {
                                continue;
                            }
                        }

                        if (part instanceof TankLocation) {
                            continue;
                        }

                        if (part.isExtinct(YEAR, EMPLOYER_IS_CLAN, EMPLOYER_TECH_CODE)) {
                            if (!isLosTechCache) {
                                continue;
                            }
                        } else {
                            if (isLosTechCache) {
                                continue;
                            }
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
                        int weight = getWeight(pair.getValue());
                        potentialParts.merge(part, weight, Integer::sum);
                    }
                }
            }
        } catch (Exception exception) {
            logger.error("Aborted parts collection.", exception);
        }
    }

    private int getWeight(Part part) {
        int weight = 1;

        if (part instanceof MissingPart) {
            return weight * 5;
        } else {
            return weight;
        }
    }

    private void buildPool() {
        partsPool = new ArrayList<>(potentialParts.keySet());

        if (!partsPool.isEmpty()) {
            Collections.shuffle(partsPool);
        }
    }

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

    public void getSupplyDrops(int dropCount) {
        getSupplyDrops(dropCount, null, false);
    }

    public void getSupplyDrops(int dropCount, boolean isLoot) {
        getSupplyDrops(dropCount, null, isLoot);
    }

    public void getSupplyDrops(int dropCount, @Nullable AtBMoraleLevel morale, boolean isLoot) {
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

        supplyDropDialog(droppedItems, cashReward, morale, isLoot);
    }

    public Map<String, Integer> createPartsReport(List<Part> droppedItems) {
        return droppedItems.stream()
            .collect(Collectors.toMap(
                part -> {
                    if (part instanceof AmmoBin) {
                        return ((AmmoBin) part).getType().getName();
                    } else if (part instanceof MekLocation) {
                        return part.getName() + " (" + part.getUnitTonnage() + ')';
                    } else {
                        return part.getName();
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

    public JDialog createSupplyDropDialog(ImageIcon icon, String description, List<Part> droppedItems, Money cashReward) {
        final int DIALOG_WIDTH = 900;
        final int DIALOG_HEIGHT = 500;
        final String title = resources.getString("dialog.title");

        JDialog dialog = new JDialog();
        dialog.setSize(UIUtil.scaleForGUI(DIALOG_WIDTH, DIALOG_HEIGHT));
        dialog.setLocationRelativeTo(null);
        dialog.setTitle(title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        dialog.setLayout(new BorderLayout());

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

        JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton okButton = new JButton(resources.getString("confirmReceipt.text"));
        dialog.add(okButton, BorderLayout.SOUTH);
        okButton.addActionListener(e -> {
            deliveryDrop(droppedItems, cashReward);
            dialog.dispose();
        });

        return dialog;
    }

    private void supplyDropDialog(List<Part> droppedItems, Money cashReward,
                                  @Nullable AtBMoraleLevel morale, boolean isLoot) {
        ImageIcon icon = getFactionLogo(campaign, employerFaction.getShortName(), true);

        StringBuilder description = new StringBuilder(getMessageReferenceNormal(morale, isLoot));

        Map<String, Integer> partsReport = createPartsReport(droppedItems);
        List<Entry<String, Integer>> entries = new ArrayList<>(partsReport.entrySet());

        String[] columns = formatColumnData(entries);

        if (!cashReward.isZero()) {
            columns[entries.size() % 3] += "<br> - " + cashReward.toAmountAndSymbolString();
        }

        description.append("<table><tr valign='top'>")
            .append("<td>").append(columns[0]).append("</td>")
            .append("<td>").append(columns[1]).append("</td>")
            .append("<td>").append(columns[2]).append("</td>")
            .append("</tr></table>");

        JDialog dialog = createSupplyDropDialog(icon, description.toString(), droppedItems, cashReward);
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void deliveryDrop(List<Part> droppedItems, Money cashReward) {
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

        if (!cashReward.isZero()) {
            campaign.getFinances().credit(BONUS_EXCHANGE, campaign.getLocalDate(), cashReward,
                resources.getString("transactionReason.text"));
        }
    }
    private static PartQuality getRandomPartQuality(int modifier) {
        return getRandomUnitQuality(modifier);
    }

    private int getTechFaction(Faction faction) {
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

    private String getMessageReferenceNormal(@Nullable AtBMoraleLevel morale, boolean isLoot) {
        int bodyRoll = Compute.randomInt(10);

        if (isLoot) {
            return resources.getString("routed" + bodyRoll + ".text");
        }

        String moraleName = "adhocSupplies";

        if (morale != null) {
            moraleName = morale.toString().toLowerCase();
        }

        if ((morale == null) || (!morale.isOverwhelming())) {
            return resources.getString(moraleName + bodyRoll + ".text");
        } else {
            StringBuilder body = new StringBuilder(resources.getString(moraleName + bodyRoll + ".text"));
            body.append("<br><br>").append(resources.getString("overwhelmingConnector.text"));

            int goodbyeRoll = Compute.randomInt(30);
            body.append("<br><br><i>").append(resources.getString("overwhelmingGoodbye" + goodbyeRoll + ".text")).append("</i>");

            return body.toString();
        }
    }
}
