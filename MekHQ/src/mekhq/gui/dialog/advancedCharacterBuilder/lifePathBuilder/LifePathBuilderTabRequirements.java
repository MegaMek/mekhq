package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.EnhancedTabbedPane;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathRecord;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

public class LifePathBuilderTabRequirements {
    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    private final LifePathBuilderDialog parent;

    public LifePathBuilderTabRequirements(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain, int gameYear) {
        this.parent = parent;

        JPanel tabRequirements = new JPanel(new BorderLayout());
        tabRequirements.setName("requirements");
        String titleRequirements = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.requirements");
        tabMain.addTab(titleRequirements, tabRequirements);

        // Panel for the two buttons at the top
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        String titleAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addGroup.label");
        String tooltipAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addGroup.tooltip");
        RoundedJButton btnAddRequirementGroup = new RoundedJButton(titleAddGroup);
        btnAddRequirementGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddGroup)
        );
        buttonPanel.add(btnAddRequirementGroup);

        String titleRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.removeGroup.label");
        String tooltipRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.removeGroup.tooltip");
        RoundedJButton btnRemoveRequirementGroup = new RoundedJButton(titleRemoveGroup);
        btnRemoveRequirementGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipRemoveGroup)
        );
        buttonPanel.add(btnRemoveRequirementGroup);

        // The actual tabbed pane and any action listeners (we add them here to avoid a situation where they're
        // called before the pane has been initialized)
        EnhancedTabbedPane tabbedPane = new EnhancedTabbedPane();
        tabbedPane.addChangeListener(e -> btnRemoveRequirementGroup.setEnabled(tabbedPane.getSelectedIndex() != 0));
        btnAddRequirementGroup.addActionListener(e -> createRequirementsTab(tabbedPane, gameYear));
        btnRemoveRequirementGroup.addActionListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();

            // Remove the current tab, unless it's Group 0
            if (selectedIndex > 0) {
                tabbedPane.removeTabAt(selectedIndex);
            } else {
                return;
            }

            // Update the names of the remaining tabs
            int tabCount = tabbedPane.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                String titleTab = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.labelFormat", i);
                tabbedPane.setTitleAt(i, titleTab);
            }
        });

        // Add 'Group 0' - this group is required
        createRequirementsTab(tabbedPane, gameYear);

        tabRequirements.add(buttonPanel, BorderLayout.NORTH);
        tabRequirements.add(tabbedPane, BorderLayout.CENTER);
    }

    private void createRequirementsTab(EnhancedTabbedPane tabbedPane, int gameYear) {
        // Create the panel to be used in the tab
        JPanel requirementGroupPanel = new JPanel();
        requirementGroupPanel.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        Map<SkillAttribute, Integer> attributes = new HashMap<>();
        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> traits = new HashMap<>();
        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        // Skills
        Map<String, Integer> skills = new HashMap<>();
        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        // SPAs
        PersonnelOptions spas = new PersonnelOptions();
        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Factions
        List<Faction> factions = new ArrayList<>();
        String titleAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addFaction.label");
        String tooltipAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addFaction.tooltip");
        RoundedJButton btnAddFaction = new RoundedJButton(titleAddFaction);
        btnAddFaction.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddFaction)
        );
        buttonsPanel.add(btnAddFaction);

        // Life Paths
        List<LifePathRecord> lifePaths = new ArrayList<>();
        String titleAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addLifePath.label");
        String tooltipAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addLifePath.tooltip");
        RoundedJButton btnAddLifePath = new RoundedJButton(titleAddLifePath);
        btnAddLifePath.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddLifePath)
        );
        buttonsPanel.add(btnAddLifePath);

        // Categories
        Map<LifePathCategory, Integer> categories = new HashMap<>();
        String titleAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addCategory.label");
        String tooltipAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addCategory.tooltip");
        RoundedJButton btnAddCategory = new RoundedJButton(titleAddCategory);
        btnAddCategory.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddCategory)
        );
        buttonsPanel.add(btnAddCategory);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane txtRequirements = new JEditorPane();
        txtRequirements.setContentType("text/html");
        txtRequirements.setEditable(false);
        String progressText = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits,
              skills, spas);
        txtRequirements.setText(progressText);

        FastJScrollPane scrollRequirements = new FastJScrollPane(txtRequirements);
        scrollRequirements.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollRequirements.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollRequirements.setBorder(null);

        pnlDisplay.add(scrollRequirements, BorderLayout.CENTER);

        // Action Listeners
        btnAddAttribute.addActionListener(e -> {
            parent.setVisible(false);
            LifePathAttributePicker picker = new LifePathAttributePicker(attributes);
            attributes.clear();
            attributes.putAll(picker.getSelectedAttributeScores());
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            parent.setVisible(true);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that lists the Traits and allows the user to set levels needed
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            parent.setVisible(true);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that lists the Skills and allows the user to specify levels for each
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            parent.setVisible(true);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that lists the currently enabled SPAs and allows the user to pick x
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            parent.setVisible(true);
        });
        btnAddFaction.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that allows the player to add and remove factions from the list of playable factions
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            parent.setVisible(true);
        });
        btnAddLifePath.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that lists the curreent requirements and allows the user to remove one
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            parent.setVisible(true);
        });
        btnAddCategory.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that lists the categories and allows the user to pick x
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            parent.setVisible(true);
        });

        // Add panels and then add Tab
        requirementGroupPanel.add(buttonsPanel, BorderLayout.NORTH);
        requirementGroupPanel.add(pnlDisplay, BorderLayout.CENTER);

        int count = tabbedPane.getComponentCount();
        String titleTab = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.labelFormat", count);
        tabbedPane.addTab(titleTab, requirementGroupPanel);
    }

    private static String buildRequirementText(int gameYear, List<Faction> factions, List<LifePathRecord> lifePaths,
          Map<LifePathCategory, Integer> categories, Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<String, Integer> skills, PersonnelOptions spas) {
        StringBuilder progressText = new StringBuilder();

        // Factions
        if (!factions.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.factions");
            progressText.append(title);
            for (int i = 0; i < factions.size(); i++) {
                Faction faction = factions.get(i);
                progressText.append(faction.getFullName(gameYear));
                if (i != factions.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        // Life Paths
        if (!lifePaths.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.lifePaths");
            progressText.append(title);
            for (int i = 0; i < lifePaths.size(); i++) {
                LifePathRecord lifePath = lifePaths.get(i);
                progressText.append(lifePath.name());
                if (i != lifePaths.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        // Categories
        if (!categories.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.categories");
            progressText.append(title);

            int counter = 0;
            int length = categories.size();
            for (Map.Entry<LifePathCategory, Integer> entry : categories.entrySet()) {
                progressText.append("<b>");
                progressText.append(entry.getKey().getDisplayName());
                progressText.append(":</b> ");
                progressText.append(entry.getValue());
                progressText.append('+');
                counter++;
                if (counter != length) {
                    progressText.append("<br>");
                }
            }
        }

        // Attributes
        if (!attributes.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.attributes");
            progressText.append(title);

            int counter = 0;
            int length = attributes.size();
            for (Map.Entry<SkillAttribute, Integer> entry : attributes.entrySet()) {
                progressText.append("<b>");
                progressText.append(entry.getKey().getLabel());
                progressText.append(":</b> ");
                progressText.append(entry.getValue());
                progressText.append('+');
                counter++;
                if (counter != length) {
                    progressText.append("<br>");
                }
            }
        }

        // Traits
        if (!traits.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.traits");
            progressText.append(title);

            int counter = 0;
            int length = traits.size();
            for (Map.Entry<LifePathEntryDataTraitLookup, Integer> entry : traits.entrySet()) {
                progressText.append("<b>");
                progressText.append(entry.getKey().getDisplayName());
                progressText.append(":</b> ");
                progressText.append(entry.getValue());
                progressText.append('+');
                counter++;
                if (counter != length) {
                    progressText.append("<br>");
                }
            }
        }

        // Skills
        if (!skills.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.skills");
            progressText.append(title);

            int counter = 0;
            int length = traits.size();
            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                progressText.append("<b>");
                progressText.append(entry.getKey());
                progressText.append(":</b> ");
                progressText.append(entry.getValue());
                progressText.append('+');
                counter++;
                if (counter != length) {
                    progressText.append("<br>");
                }
            }
        }

        // SPAs
        List<String> selectedSPAs = new ArrayList<>();
        for (final Enumeration<IOptionGroup> i = spas.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (final Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                String name = option.getName();
                if (spas.booleanOption(name)) {
                    selectedSPAs.add(option.getDisplayableName());
                }
            }
        }

        if (!selectedSPAs.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.spas");
            progressText.append(title);

            for (int i = 0; i < selectedSPAs.size(); i++) {
                String spa = selectedSPAs.get(i);
                progressText.append(spa);
                if (i != selectedSPAs.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        return progressText.toString();
    }
}
