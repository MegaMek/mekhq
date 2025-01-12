package mekhq.gui.dialog.campaignOptions;

import megamek.client.ui.swing.util.UIUtil;
import mekhq.campaign.personnel.SkillPerquisite;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.dialog.EditSpecialAbilityDialog;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsButton;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsGridBagConstraints;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsHeaderPanel;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsStandardPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static mekhq.campaign.personnel.SpecialAbility.getDefaultSpecialAbilities;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.resources;

public class AbilitiesTab {
    private Map<String, SpecialAbility> allAbilities;
    private HashMap<String, SpecialAbility> unusedAbilities;
    private Map<SpecialAbility, Boolean> abilityUsageTable;
    private JPanel combatTab;
    private JPanel maneuveringTab;
    private JPanel utilityTab;

    AbilitiesTab() {
        allAbilities = new Hashtable<>();
        abilityUsageTable = new HashMap<>();
        unusedAbilities = new HashMap<>();
        combatTab = new JPanel();
        maneuveringTab = new JPanel();
        utilityTab = new JPanel();
    }

    public enum AbilityCategory {
        COMBAT_ABILITIES, MANEUVERING_ABILITIES, UTILITY_ABILITIES
    }

    void setAllAbilities(Map<String, SpecialAbility> abilities) {
        buildAbilityMaps(abilities);
    }

    JPanel createAbilitiesTab(AbilityCategory abilityCategory) {
        // Header
        JPanel headerPanel = switch (abilityCategory) {
            case COMBAT_ABILITIES ->
                new CampaignOptionsHeaderPanel("CombatAbilitiesTab", getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
            case MANEUVERING_ABILITIES ->
                new CampaignOptionsHeaderPanel("ManeuveringAbilitiesTab", getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
            case UTILITY_ABILITIES ->
                new CampaignOptionsHeaderPanel("UtilityAbilitiesTab", getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
        };

        // Contents
        List<SpecialAbility> eligibleAbilities = getEligibleAbilities(abilityCategory);

        // Layout the Panels
        final JPanel panel = new CampaignOptionsStandardPanel("AbilitiesGeneralTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        int abilityCount = eligibleAbilities.size();

        for (int i = 0; i < abilityCount; i++) {
            SpecialAbility ability = eligibleAbilities.get(i);

            if (ability != null) {
                JPanel abilityPanel = createSPAPanel(ability, unusedAbilities);

                layout.gridx = i % 4;
                layout.gridy = 1 + (i / 4);

                layout.gridwidth = 1;
                panel.add(abilityPanel, layout);
            }
        }

        // Create Parent Panel and return
        return switch (abilityCategory) {
            case COMBAT_ABILITIES -> {
                combatTab = panel;
                yield createParentPanel(combatTab, "AbilitiesGeneralTab");
            }
            case MANEUVERING_ABILITIES -> {
                maneuveringTab = panel;
                yield createParentPanel(maneuveringTab, "AbilitiesGeneralTab");
            }
            case UTILITY_ABILITIES -> {
                utilityTab = panel;
                yield createParentPanel(utilityTab, "AbilitiesGeneralTab");
            }
        };
    }

    private List<SpecialAbility> getEligibleAbilities(AbilityCategory abilityCategory) {
        // This fetches all currently enabled special abilities
        Map<String, SpecialAbility> specialAbilities = SpecialAbility.getSpecialAbilities();

        if (allAbilities.isEmpty()) {
            buildAbilityMaps(specialAbilities);
        }

        List<SpecialAbility> eligibleAbilities = new ArrayList<>();
        for (SpecialAbility ability : allAbilities.values()) {
            for (SkillPerquisite skillPerquisite : ability.getPrereqSkills()) {
                // Is the ability classified as a Combat Ability?
                boolean isCombatAbility = Stream.of("Gunnery", "Artillery", "Small Arms").anyMatch(word -> Pattern.compile("\\b" + word).matcher(skillPerquisite.toString()).find());

                // Is the ability classified as a Maneuvering Ability?
                boolean isManeuveringAbility = Stream.of("Piloting", "Anti-Mek").anyMatch(word -> Pattern.compile("\\b" + word).matcher(skillPerquisite.toString()).find());

                switch (abilityCategory) {
                    case COMBAT_ABILITIES -> {
                        if (isCombatAbility) {
                            eligibleAbilities.add(ability);
                        }
                    }
                    case MANEUVERING_ABILITIES -> {
                        if (!isCombatAbility && isManeuveringAbility) {
                            eligibleAbilities.add(ability);
                        }
                    }
                    case UTILITY_ABILITIES -> {
                        if (!isCombatAbility && !isManeuveringAbility) {
                            eligibleAbilities.add(ability);
                        }
                    }
                }
            }
        }

        eligibleAbilities.sort(Comparator.comparing(SpecialAbility::getDisplayName));

        return eligibleAbilities;
    }

    void buildAbilityMaps(Map<String, SpecialAbility> specialAbilities) {
        // We need to create a temporary hash of special abilities that we can modify
        // without changing the underlying one in case the user cancels the changes
        for (SpecialAbility ability : specialAbilities.values()) {
            allAbilities.put(ability.getDisplayName(), ability.clone());
        }

        buildMapOfUnusedSPAs();

        for (SpecialAbility ability : unusedAbilities.values()) {
            allAbilities.put(ability.getName(), ability);
        }
    }

    private void buildMapOfUnusedSPAs() {
        final Map<String, SpecialAbility> defaultSpecialAbilities = getDefaultSpecialAbilities();

        for (SpecialAbility ability : defaultSpecialAbilities.values()) {
            String name = ability.getDisplayName();
            // Check if the ability is unused
            if (!allAbilities.containsKey(name)) {
                SpecialAbility unusedAbility = ability.clone();
                unusedAbilities.put(name, unusedAbility);
            }
        }
    }

    private JPanel createSPAPanel(SpecialAbility ability, HashMap<String, SpecialAbility> unusedAbilities) {
        // Initialization
        final JPanel panel = new AbilitiesTabStandardPanel(ability);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL);

        // Contents
        JCheckBox chkAbility = new JCheckBox(resources.getString("abilityEnable.text"));
        chkAbility.setSelected(!unusedAbilities.containsValue(ability));
        // This sets the initial value, while the action listener ensures the SPAs presence in the
        // table is kept up to date with player changes.
        abilityUsageTable.put(ability, !unusedAbilities.containsValue(ability));
        chkAbility.addActionListener(e -> abilityUsageTable.put(ability, chkAbility.isSelected()));

        JLabel lblCost = getAbilityCost(ability);

        JLabel lblDescription = new JLabel();
        lblDescription.setText(String.format("<html><div style='width: %s; text-align:justify;'><i>%s</i></div></html>", UIUtil.scaleForGUI(400), ability.getDescription()));

        JLabel lblPrerequisites = createAbilityLabel("prerequisites.text", ability.getAllPrereqDesc());
        JLabel lblIncompatible = createAbilityLabel("incompatible.text", ability.getInvalidDesc());
        JLabel lblRemoves = createAbilityLabel("removes.text", ability.getRemovedDesc());

        JButton btnCustomizeAbility = new CampaignOptionsButton("CustomizeAbility", null);
        btnCustomizeAbility.addActionListener(e -> {
            if (editSPA(ability)) {
                // This will run on the SWT thread
                SwingUtilities.invokeLater(() -> {
                    // Remove old panel
                    panel.removeAll();

                    // Compose replacement components
                    final JLabel lblNewCost = getAbilityCost(ability);
                    final JLabel lblNewPrerequisites = createAbilityLabel("prerequisites.text", ability.getAllPrereqDesc());
                    final JLabel lblNewIncompatible = createAbilityLabel("incompatible.text", ability.getInvalidDesc());
                    final JLabel lblNewRemoves = createAbilityLabel("removes.text", ability.getRemovedDesc());

                    // Perform layout changes
                    arrangeSPAPanelLayout(layout, panel, chkAbility, lblNewCost, lblDescription, lblNewPrerequisites, lblNewIncompatible, lblNewRemoves, btnCustomizeAbility);

                    // Refresh display
                    panel.revalidate();
                    panel.repaint();
                });
            }
        });

        // Layout
        arrangeSPAPanelLayout(layout, panel, chkAbility, lblCost, lblDescription, lblPrerequisites, lblIncompatible, lblRemoves, btnCustomizeAbility);

        return panel;
    }

    private static void arrangeSPAPanelLayout(GridBagConstraints layout, JPanel panel, JCheckBox chkAbility, JLabel lblCost, JLabel lblDescription, JLabel lblPrerequisites, JLabel lblIncompatible, JLabel lblRemoves, JButton btnCustomizeAbility) {
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkAbility, layout);
        layout.gridx++;
        panel.add(lblCost, layout);

        layout.gridwidth = 3;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblDescription, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPrerequisites, layout);
        layout.gridx++;
        panel.add(lblIncompatible, layout);
        layout.gridx++;
        panel.add(lblRemoves, layout);

        layout.gridwidth = 3;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(btnCustomizeAbility, layout);
    }

    private JLabel createAbilityLabel(String resourceKey, String descriptionFromAbility) {
        String description = resources.getString(resourceKey) + descriptionFromAbility;
        return new JLabel(buildAbilityDescription(description));
    }

    private static JLabel getAbilityCost(SpecialAbility ability) {
        return new JLabel(String.format(resources.getString("abilityCost.text"), ability.getCost()));
    }

    private boolean editSPA(SpecialAbility ability) {
        EditSpecialAbilityDialog dialog = new EditSpecialAbilityDialog(null, ability, allAbilities);
        dialog.setVisible(true);

        return !dialog.wasCancelled();
    }

    private static String buildAbilityDescription(String description) {
        return ("<html>" + description + "</html>").replaceAll("\\{", "").replaceAll("}", "");
    }

    static class AbilitiesTabStandardPanel extends JPanel {
        public AbilitiesTabStandardPanel(SpecialAbility ability) {
            String name = ability.getDisplayName();

            new JPanel() {
                @Override
                public Dimension getPreferredSize() {
                    Dimension standardSize = super.getPreferredSize();
                    return UIUtil.scaleForGUI((Math.max(standardSize.width, 500)), standardSize.height);
                }
            };

            setBorder(BorderFactory.createTitledBorder(String.format("<html>%s</html>", name)));

            setName("pnl" + name);
        }
    }

    void applyCampaignOptionsToCampaign() {
        Map<String, SpecialAbility> enabledAbilities = getEnabledAbilities();
        SpecialAbility.replaceSpecialAbilities(enabledAbilities);
    }

    private Map<String, SpecialAbility> getEnabledAbilities() {
        Map<String, SpecialAbility> enabledAbilities = new HashMap<>();

        for (SpecialAbility ability : abilityUsageTable.keySet()) {
            if (abilityUsageTable.get(ability)) {
                enabledAbilities.put(ability.getName(), ability);
            }
        }
        return enabledAbilities;
    }
}
