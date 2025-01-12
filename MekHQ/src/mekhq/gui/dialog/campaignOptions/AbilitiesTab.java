package mekhq.gui.dialog.campaignOptions;

import megamek.client.ui.swing.util.UIUtil;
import mekhq.campaign.personnel.SkillPerquisite;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.dialog.EditSpecialAbilityDialog;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsAbilityInfo.AbilityCategory;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsButton;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsGridBagConstraints;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsHeaderPanel;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsStandardPanel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static mekhq.gui.dialog.campaignOptions.CampaignOptionsAbilityInfo.AbilityCategory.COMBAT_ABILITY;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsAbilityInfo.AbilityCategory.MANEUVERING_ABILITY;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsAbilityInfo.AbilityCategory.UTILITY_ABILITY;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.resources;

public class AbilitiesTab {
    private Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();
    private JPanel combatTab;
    private JPanel maneuveringTab;
    private JPanel utilityTab;

    AbilitiesTab() {
        initialize();
    }

    private void initialize() {
        allAbilityInfo = new HashMap<>();
        combatTab = new JPanel();
        maneuveringTab = new JPanel();
        utilityTab = new JPanel();
        buildAllAbilityInfo(SpecialAbility.getSpecialAbilities());
    }

    /**
     * Initializes all ability information, setting their active/inactive status based
     * on the provided map of abilities and ensuring all abilities are covered.
     *
     * @param abilities A map of active abilities keyed by their names.
     */
    void buildAllAbilityInfo(Map<String, SpecialAbility> abilities) {
        // Remove old data
        allAbilityInfo.clear();

        // Build abilities
        buildAbilityInfo(abilities, true);

        Map<String, SpecialAbility> allSpecialAbilities = SpecialAbility.getDefaultSpecialAbilities();
        Map<String, SpecialAbility> missingAbilities = new HashMap<>();

        for (SpecialAbility ability : allSpecialAbilities.values()) {
            if (!allAbilityInfo.containsKey(ability.getName())) {
                missingAbilities.put(ability.getName(), ability);
            }
        }

        if (!missingAbilities.isEmpty()) {
            buildAbilityInfo(missingAbilities, false);
        }

        // Clear and update tabs in-place
        refreshAll();
    }

    private void refreshAll() {
        refreshTabContents(combatTab, AbilityCategory.COMBAT_ABILITY);
        refreshTabContents(maneuveringTab, AbilityCategory.MANEUVERING_ABILITY);
        refreshTabContents(utilityTab, AbilityCategory.UTILITY_ABILITY);
    }

    private void refreshTabContents(JPanel tab, AbilityCategory category) {
        tab.removeAll();
        JPanel newContents = createAbilitiesTab(category);

        // Add new content to the same panel
        tab.setLayout(new BorderLayout());
        tab.add(newContents, BorderLayout.CENTER);

        tab.revalidate();
        tab.repaint();
    }

    private void buildAbilityInfo(Map<String, SpecialAbility> abilities, boolean isEnabled) {
        for (Entry<String, SpecialAbility> entry : abilities.entrySet()) {
            SpecialAbility clonedAbility = entry.getValue().clone();
            String abilityName = clonedAbility.getName();
            AbilityCategory category = getCategory(clonedAbility);

            // Mark the ability as active
            allAbilityInfo.put(abilityName, new CampaignOptionsAbilityInfo(abilityName, clonedAbility, isEnabled, category));
        }
    }

    private AbilityCategory getCategory(SpecialAbility ability) {
        for (SkillPerquisite skillPerquisite : ability.getPrereqSkills()) {
            // Is the ability classified as a Combat Ability?
            boolean isCombatAbility = Stream.of("Gunnery", "Artillery", "Small Arms")
                .anyMatch(word -> Pattern.compile("\\b" + word)
                    .matcher(skillPerquisite.toString())
                    .find());

            if (isCombatAbility) {
                return COMBAT_ABILITY;
            }

            // Is the ability classified as a Maneuvering Ability?
            boolean isManeuveringAbility = Stream.of("Piloting", "Anti-Mek")
                .anyMatch(word -> Pattern.compile("\\b" + word)
                    .matcher(skillPerquisite.toString())
                    .find());

            if (isManeuveringAbility) {
                return MANEUVERING_ABILITY;
            }
        }

        // If it isn't a Combat or Maneuvering ability, it's a utility ability
        return UTILITY_ABILITY;
    }

    JPanel createAbilitiesTab(AbilityCategory abilityCategory) {
        // Header
        JPanel headerPanel = switch (abilityCategory) {
            case COMBAT_ABILITY ->
                new CampaignOptionsHeaderPanel("CombatAbilitiesTab",
                    getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
            case MANEUVERING_ABILITY ->
                new CampaignOptionsHeaderPanel("ManeuveringAbilitiesTab",
                    getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
            case UTILITY_ABILITY ->
                new CampaignOptionsHeaderPanel("UtilityAbilitiesTab",
                    getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
        };

        // Contents
        JButton btnEnableAll = new CampaignOptionsButton("AddAll");
        btnEnableAll.addActionListener(e -> {
            for (CampaignOptionsAbilityInfo abilityInfo : allAbilityInfo.values()) {
                abilityInfo.setEnabled(true);
            }

            refreshAll();
        });

        JButton btnRemoveAll = new CampaignOptionsButton("RemoveAll");
        btnRemoveAll.addActionListener(e -> {
            for (CampaignOptionsAbilityInfo abilityInfo : allAbilityInfo.values()) {
                abilityInfo.setEnabled(false);
            }

            refreshAll();
        });

        // Layout the Panels
        final JPanel panel = new CampaignOptionsStandardPanel("AbilitiesGeneralTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(btnEnableAll, layout);
        layout.gridx++;
        panel.add(btnRemoveAll, layout);

        int abilityCounter = 0;

        for (String abilityName : allAbilityInfo.keySet()) {
            CampaignOptionsAbilityInfo abilityInfo = allAbilityInfo.get(abilityName);

            if (abilityInfo.getCategory() == abilityCategory) {
                abilityCounter++;

                JPanel abilityPanel = createSPAPanel(abilityInfo);

                layout.gridx = (abilityCounter % 4) - 1;
                layout.gridy = 2 + (abilityCounter / 4);

                layout.gridwidth = 1;
                panel.add(abilityPanel, layout);
            }
        }

        // Create Parent Panel and return
        JPanel parentPanel = createParentPanel(panel, "AbilitiesTab" + COMBAT_ABILITY.name());

        return switch (abilityCategory) {
            case COMBAT_ABILITY -> {
                combatTab = parentPanel;
                yield combatTab;
            }
            case MANEUVERING_ABILITY -> {
                maneuveringTab = parentPanel;
                yield maneuveringTab;
            }
            case UTILITY_ABILITY -> {
                utilityTab = parentPanel;
                yield utilityTab;
            }
        };
    }

    private JPanel createSPAPanel(CampaignOptionsAbilityInfo abilityInfo) {
        SpecialAbility ability = abilityInfo.getAbility();

        // Initialization
        final JPanel panel = new AbilitiesTabStandardPanel(ability);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel,
            GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL);

        // Contents
        JCheckBox chkAbility = new JCheckBox(resources.getString("abilityEnable.text"));
        chkAbility.setSelected(abilityInfo.isEnabled());
        chkAbility.addActionListener(e -> abilityInfo.setEnabled(chkAbility.isSelected()));

        JLabel lblCost = new JLabel(String.format(resources.getString("abilityCost.text"),
            ability.getCost()));

        JLabel lblDescription = new JLabel();
        lblDescription.setText(String.format("<html><div style='width: %s; text-align:justify;'><i>%s</i></div></html>",
            UIUtil.scaleForGUI(400), ability.getDescription()));

        JLabel lblPrerequisites = createAbilityLabel("prerequisites.text", ability.getAllPrereqDesc());
        JLabel lblIncompatible = createAbilityLabel("incompatible.text", ability.getInvalidDesc());
        JLabel lblRemoves = createAbilityLabel("removes.text", ability.getRemovedDesc());

        JButton btnCustomizeAbility = new CampaignOptionsButton("CustomizeAbility", null);
        btnCustomizeAbility.addActionListener(e -> {
            if (editSPA(ability)) {
                // This will run on the SWT thread
                SwingUtilities.invokeLater(() -> {
                    // Update components with new values
                    lblCost.setText(String.format(resources.getString("abilityCost.text"),
                        ability.getCost()));

                    String prerequisitesDescription = resources.getString("prerequisites.text")
                        + ability.getAllPrereqDesc();
                    lblPrerequisites.setText(buildAbilityDescription(prerequisitesDescription));

                    String incompatibleDescription = resources.getString("incompatible.text")
                        + ability.getInvalidDesc();
                    lblIncompatible.setText(buildAbilityDescription(incompatibleDescription));

                    String removesDescription = resources.getString("removes.text")
                        + ability.getRemovedDesc();
                    lblRemoves.setText(buildAbilityDescription(removesDescription));
                });
            }
        });

        // Layout
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

        return panel;
    }

    private boolean editSPA(SpecialAbility ability) {
        EditSpecialAbilityDialog dialog = new EditSpecialAbilityDialog(null, ability,
            Map.of(ability.getName(), ability.clone()));
        dialog.setVisible(true);

        return !dialog.wasCancelled();
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

            setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                String.format("<html>%s</html>", name)));
            setName("pnl" + name);
        }
    }

    private JLabel createAbilityLabel(String resourceKey, String descriptionFromAbility) {
        String description = resources.getString(resourceKey) + descriptionFromAbility;
        return new JLabel(buildAbilityDescription(description));
    }

    private static String buildAbilityDescription(String description) {
        return ("<html>" + description + "</html>")
            .replaceAll("\\{", "")
            .replaceAll("}", "");
    }

    void applyCampaignOptionsToCampaign() {
        Map<String, SpecialAbility> enabledAbilities = new HashMap<>();

        for (CampaignOptionsAbilityInfo abilityInfo : allAbilityInfo.values()) {
            if (abilityInfo.isEnabled()) {
                enabledAbilities.put(abilityInfo.getAbility().getName(), abilityInfo.getAbility());
            }
        }
        SpecialAbility.replaceSpecialAbilities(enabledAbilities);
    }
}

