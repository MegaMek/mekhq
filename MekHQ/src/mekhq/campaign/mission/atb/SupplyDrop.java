package mekhq.campaign.mission.atb;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.ITechnology;
import megamek.common.Mek;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.apache.commons.math3.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static mekhq.campaign.finances.enums.TransactionType.BONUS_EXCHANGE;
import static mekhq.campaign.unit.Unit.getRandomUnitQuality;
import static mekhq.campaign.universe.Factions.getFactionLogo;

public class SupplyDrop {
    private final static MMLogger logger = MMLogger.create(SupplyDrop.class);
    private Campaign campaign;
    private Faction employerFaction;
    private Map<Part, Integer> potentialParts;
    private List<Part> partsPool;
    private Random random;


    private final int YEAR;
    private final int EMPLOYER_TECH_CODE;
    private final boolean EMPLOYER_IS_CLAN;
    private final Money TARGET_VALUE = Money.of(500000);
    private final int LOSTECH_CHANCE;

    public SupplyDrop(Campaign campaign, Faction employerFaction, int LosTechModifier) {
        // Constants
        YEAR = campaign.getGameYear();
        EMPLOYER_IS_CLAN = employerFaction.isClan();
        EMPLOYER_TECH_CODE = getTechFaction(employerFaction);
        LOSTECH_CHANCE = Math.max(1, 50 - LosTechModifier);


        this.campaign = campaign;
        this.employerFaction = employerFaction;
        initialize();
    }

    private void initialize() {
        collectParts();
        buildPool();

        random = new Random();
    }

    private void collectParts() {
        potentialParts = new HashMap<>();

        try {
            Collection<Unit> units = campaign.getUnits();
            for (Unit unit : units) {
                if (!unit.isSalvage() && unit.isAvailable()) {
                    List<Part> parts = unit.getParts();
                    for (Part part : parts) {
                        Pair<Unit, Part> pair = new Pair<>(unit, part);
                        int weight = getWeight(pair.getKey(), pair.getValue());
                        potentialParts.merge(part, weight, Integer::sum);
                    }
                }
            }
        } catch (Exception exception) {
            logger.error("Aborted parts collection.", exception);
        }
    }

    private int getWeight(Unit unit, Part part) {
        int weight = 1;

        if (unit.getForceId() != Force.FORCE_NONE) {
            weight = 2;
        }

        if (part instanceof MissingPart) {
            return weight * 2;
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
        List<Part> droppedItems = new ArrayList<>();
        Money cashReward = Money.zero();

        for (int i = 0; i < dropCount; i++) {
            Money runningTotal = Money.zero();

            while (runningTotal.isLessThan(TARGET_VALUE)) {
                Part potentialPart = getPart();
                runningTotal = runningTotal.plus(potentialPart.getActualValue());

                if (potentialPart instanceof MekLocation) {
                    if (((MekLocation) potentialPart).getLoc() == Mek.LOC_CT) {
                        cashReward = cashReward.plus(potentialPart.getActualValue());
                        continue;
                    }
                }

                if (potentialPart.isExtinct(YEAR, EMPLOYER_IS_CLAN, EMPLOYER_TECH_CODE)) {
                    if (Compute.randomInt(LOSTECH_CHANCE) == 0) {
                        droppedItems.add(potentialPart);
                    } else {
                        cashReward = cashReward.plus(potentialPart.getActualValue());
                    }
                    continue;
                }

                droppedItems.add(potentialPart);
            }
        }

        supplyDropDialog(droppedItems, cashReward);
    }

    public Map<String, Integer> createPartsReport(List<Part> droppedItems) {
        return droppedItems.stream()
            .collect(Collectors.toMap(
                part -> {
                    if (part instanceof AmmoBin) {
                        return ((AmmoBin) part).getType().getName();
                    } else {
                        return part.getName();
                    }
                },
                part -> {
                    if (part instanceof AmmoBin) {
                        return ((AmmoBin) part).getFullShots();
                    } else if (part instanceof Armor) {
                        return (int) Math.floor(((Armor) part).getArmorPointsPerTon());
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
        final String title = "++ INCOMING TRANSMISSION ++";

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

        JButton okButton = new JButton("OK");
        dialog.add(okButton, BorderLayout.SOUTH);
        okButton.addActionListener(e -> {
            deliveryDrop(droppedItems, cashReward);
            dialog.dispose();
        });

        return dialog;
    }


    private void supplyDropDialog(List<Part> droppedItems, Money cashReward) {
        ImageIcon icon = getFactionLogo(campaign, employerFaction.getShortName(), true);

        StringBuilder description = new StringBuilder("Our employer has arranged for the following items to be delivered to our encampment:<br>");

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
        dialog.setVisible(true);
    }

    private void deliveryDrop(List<Part> droppedItems, Money cashReward) {
        for (Part part : droppedItems) {
            if (part instanceof AmmoBin) {
                campaign.getQuartermaster().addAmmo(((AmmoBin) part).getType(), ((AmmoBin) part).getFullShots());
            } else if (part instanceof Armor) {
                int quantity = (int) Math.floor(((Armor) part).getArmorPointsPerTon());
                ((Armor) part).setAmount(quantity);
                campaign.getWarehouse().addPart(part);
            } else {
                campaign.getWarehouse().addPart(part);
            }
        }

        if (!cashReward.isZero()) {
            campaign.getFinances().credit(BONUS_EXCHANGE, campaign.getLocalDate(), cashReward, "Supply Drop");
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
}
