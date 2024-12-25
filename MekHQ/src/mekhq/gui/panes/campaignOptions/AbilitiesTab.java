package mekhq.gui.panes.campaignOptions;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillPerquisite;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.dialog.EditSpecialAbilityDialog;
import mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.CampaignOptionsButton;
import mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.CampaignOptionsGridBagConstraints;
import mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.CampaignOptionsHeaderPanel;
import mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.CampaignOptionsStandardPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static mekhq.campaign.personnel.SpecialAbility.getDefaultSpecialAbilities;
import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.resources;

public class AbilitiesTab {
    JFrame frame;
    String name;

    private Map<String, SpecialAbility> temporarySPATable;

    AbilitiesTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;
    }

    public enum AbilityCategory {
        COMBAT_ABILITIES,
        MANEUVERING_ABILITIES,
        UTILITY_ABILITIES
    }

    JPanel createAbilitiesTab(AbilityCategory abilityCategory) {
        // Header
        JPanel headerPanel = switch (abilityCategory) {
            case COMBAT_ABILITIES -> new CampaignOptionsHeaderPanel("CombatAbilitiesTab",
                getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
            case MANEUVERING_ABILITIES -> new CampaignOptionsHeaderPanel("ManeuveringAbilitiesTab",
                getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
            case UTILITY_ABILITIES -> new CampaignOptionsHeaderPanel("UtilityAbilitiesTab",
                getImageDirectory() + "logo_clan_goliath_scorpion.png", true);
        };

        // Contents
        Map<String, SpecialAbility> specialAbilities = SpecialAbility.getSpecialAbilities();
        // We need to create a temporary hash of special abilities that we can modify
        // without changing the underlying one in case the user cancels the changes

        temporarySPATable = new Hashtable<>();
        for (SpecialAbility ability : specialAbilities.values()) {
            temporarySPATable.put(ability.getDisplayName(), ability.clone());
        }

        HashMap<String, SpecialAbility> unusedAbilities = getUnusedSPAs();

        for (SpecialAbility ability : unusedAbilities.values()) {
            temporarySPATable.put(ability.getName(), ability);
        }

        List<SpecialAbility> eligibleAbilities = new ArrayList<>();
        for (SpecialAbility ability : temporarySPATable.values()) {
            for (SkillPerquisite skillPerquisite : ability.getPrereqSkills()) {
                // Is the ability classified as a Combat Ability?
                boolean isCombatAbility = Stream.of("Gunnery", "Artillery", "Small Arms")
                    .anyMatch(word -> Pattern.compile("\\b" + word)
                        .matcher(skillPerquisite.toString())
                        .find());

                // Is the ability classified as a Maneuvering Ability?
                boolean isManeuveringAbility = Stream.of("Piloting", "Anti-Mek")
                    .anyMatch(word -> Pattern.compile("\\b" + word)
                        .matcher(skillPerquisite.toString())
                        .find());

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
        return createParentPanel(panel, "AbilitiesGeneralTab");
    }

    private HashMap<String, SpecialAbility> getUnusedSPAs() {
        final Map<String, SpecialAbility> defaultSpecialAbilities = getDefaultSpecialAbilities();

        HashMap<String, SpecialAbility> unusedAbilities = new HashMap<>();
        PersonnelOptions personnelOptions = new PersonnelOptions();

        // Find the specific group ("Level 3 Advantages") directly
        IOptionGroup lvl3AdvantagesGroup = getGroup(personnelOptions);

        if (lvl3AdvantagesGroup != null) {
            for (String key : defaultSpecialAbilities.keySet()) {
                // Check if the ability is unused
                if (temporarySPATable.get(key) == null && !unusedAbilities.containsKey(key)) {
                    SpecialAbility unusedAbility = defaultSpecialAbilities.get(key).clone();
                    unusedAbilities.put(unusedAbility.getDisplayName(), unusedAbility);
                }
            }
        }
        return unusedAbilities;
    }

    private IOptionGroup getGroup(PersonnelOptions personnelOptions) {
        for (Enumeration<IOptionGroup> i = personnelOptions.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            if (group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)) {
                return group;
            }

        }
        return null; // Return null if the specified group is not found
    }

    private JPanel createSPAPanel(SpecialAbility ability, HashMap<String, SpecialAbility> unusedAbilities) {
        // Initialization
        final JPanel panel = new AbilitiesTabStandardPanel(ability);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel,
            GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL);

        // Contents
        JCheckBox chkAbility = new JCheckBox(resources.getString("abilityEnable.text"));
        chkAbility.setSelected(!unusedAbilities.containsValue(ability));

        JLabel lblCost = getAbilityCost(ability);

        JLabel lblDescription = new JLabel();
        lblDescription.setText(String.format("<html><div style='width: %s; text-align:justify;'><i>%s</i></div></html>",
            UIUtil.scaleForGUI(400), ability.getDescription()));

        JLabel lblPrerequisites = createAbilityLabel("prerequisites.text", ability.getAllPrereqDesc());
        JLabel lblIncompatible = createAbilityLabel("incompatible.text", ability.getInvalidDesc());
        JLabel lblRemoves = createAbilityLabel("removes.text", ability.getRemovedDesc());

        JButton btnCustomizeAbility = new CampaignOptionsButton("CustomizeAbility",
            null);
        btnCustomizeAbility.addActionListener(e -> {
            if (editSPA(ability)) {
                // This will run on the SWT thread
                SwingUtilities.invokeLater(() -> {
                    // Remove old panel
                    panel.removeAll();

                    // Compose replacement components
                    final JLabel lblNewCost = getAbilityCost(ability);
                    final JLabel lblNewPrerequisites = createAbilityLabel("prerequisites.text",
                        ability.getAllPrereqDesc());
                    final JLabel lblNewIncompatible = createAbilityLabel("incompatible.text",
                        ability.getInvalidDesc());
                    final JLabel lblNewRemoves = createAbilityLabel("removes.text",
                        ability.getRemovedDesc());

                    // Perform layout changes
                    arrangeSPAPanelLayout(layout, panel, chkAbility, lblNewCost, lblDescription,
                        lblNewPrerequisites, lblNewIncompatible, lblNewRemoves, btnCustomizeAbility);

                    // Refresh display
                    panel.revalidate();
                    panel.repaint();
                });
            }
        });

        // Layout
        arrangeSPAPanelLayout(layout, panel, chkAbility, lblCost, lblDescription, lblPrerequisites,
            lblIncompatible, lblRemoves, btnCustomizeAbility);

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
        return new JLabel(String.format(resources.getString("abilityCost.text"),
            ability.getCost()));
    }

    private boolean editSPA(SpecialAbility ability) {
        EditSpecialAbilityDialog dialog = new EditSpecialAbilityDialog(null, ability, temporarySPATable);
        dialog.setVisible(true);

        return !dialog.wasCancelled();
    }

    private static String buildAbilityDescription(String description) {
        return ("<html>" + description + "</html>")
            .replaceAll("\\{", "")
            .replaceAll("}", "");
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

            setBorder(BorderFactory.createTitledBorder(
                String.format("<html>%s</html>", name)));

            setName("pnl" + name);
        }
    }
}
