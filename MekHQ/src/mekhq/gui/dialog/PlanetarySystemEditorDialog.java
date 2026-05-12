/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySystemEvent;
import mekhq.campaign.universe.PlanetarySystemYamlIO;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.SourceableValue;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.gui.baseComponents.AbstractMHQDialogBasic;
import mekhq.utilities.PlanetarySystemChangeSummary;
import mekhq.utilities.PlanetarySystemValidator;
import mekhq.utilities.ValidationMessage;
import mekhq.utilities.ValidationResult;

/**
 * GM-facing editor for campaign planetary system overrides.
 *
 * <p>The dialog edits working copies of loaded {@link PlanetarySystem} records, validates the result, and saves full
 * system overrides into the campaign save. Unsaved dialog edits stay local to the dialog until the GM saves them.
 */
public class PlanetarySystemEditorDialog extends AbstractMHQDialogBasic {
    private static final MMLogger LOGGER = MMLogger.create(PlanetarySystemEditorDialog.class);

    private static final int PADDING = UIUtil.scaleForGUI(8);
    private static final Dimension DEFAULT_SIZE = UIUtil.scaleForGUI(1320, 780);
    private static final Dimension SYSTEM_LIST_MINIMUM_SIZE = UIUtil.scaleForGUI(280, 400);
    private static final Dimension SYSTEM_LIST_PREFERRED_SIZE = UIUtil.scaleForGUI(400, 600);
    private static final Dimension DETAILS_MINIMUM_SIZE = UIUtil.scaleForGUI(760, 500);
    private static final double PLANET_LIST_SPLIT_WEIGHT = 0.30;
    private static final DateTimeFormatter EVENT_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Campaign campaign;
    private final List<PlanetarySystem> systems;
    private final List<FactionChoice> factionChoices;
    private final Set<String> knownFactionCodes;
    private final Map<String, PlanetarySystem> baselineSystems = new HashMap<>();
    private final Set<String> workingSystemIds = new HashSet<>();
    private final Set<String> unsavedSystemIds = new HashSet<>();
    private final Set<String> overrideSystemIds = new HashSet<>();
    private final PlanetarySystemValidator validator = new PlanetarySystemValidator();

    private DefaultListModel<PlanetarySystem> systemListModel;
    private JList<PlanetarySystem> systemList;
    private JTextField txtSearch;
    private JCheckBox chkFilterUnsaved;
    private JCheckBox chkFilterOverrides;
    private JComboBox<SystemSortOption> cboSort;
    private JTextArea txtSystemDetails;
    private JTextArea txtValidation;
    private JLabel lblSelection;
    private JLabel lblEditStatus;
    // Planet Events and Planet Properties each have their own visible list, but they share one model and selection.
    private DefaultListModel<Planet> planetListModel;
    private JList<Planet> planetList;
    private JList<Planet> propertiesPlanetList;
    private boolean updatingPlanetSelection;
    private PlanetEventTableModel eventTableModel;
    private JTable tblEvents;
    private JTextField txtEventDate;
    private JTextField txtEventFactions;
    private JTextField txtEventPopulation;
    private JComboBox<String> cboEventHpg;
    private JTextField txtEventSocioIndustrial;
    private JTextField txtEventSource;
    private JTextField txtEventVersion;
    private JTextField txtEventMessage;
    private JCheckBox chkEventCustom;
    private JButton btnPickFactions;
    private JButton btnAddUpdateEvent;
    private JButton btnDuplicateEvent;
    private JButton btnRemoveEvent;
    private JButton btnClearEvent;
    private JButton btnTransferOwnership;
    private JButton btnRevertPlanet;
    private JButton btnRevertPropertiesPlanet;
    private JButton btnReviewChanges;
    private JButton btnSave;

    // System Events tab
    private SystemEventTableModel systemEventTableModel;
    private JTable tblSystemEvents;
    private JButton btnAddSystemEvent;
    private JButton btnRemoveSystemEvent;
    private JButton btnRevertChanges;
    private JButton btnDeleteOverride;
    private PlanetarySystemPropertiesPanel propertiesPanel;

    public PlanetarySystemEditorDialog(final JFrame frame, final Campaign campaign) {
        super(frame, false, "PlanetarySystemEditorDialog", "PlanetarySystemEditorDialog.title");
        this.campaign = campaign;
        systems = new ArrayList<>(campaign.getSystems());
        systems.removeIf(Objects::isNull);
        systems.sort(Comparator.comparing(system -> systemDisplayName(system).toLowerCase(Locale.ROOT)));
        factionChoices = loadFactionChoices();
        knownFactionCodes = factionChoices.stream().map(FactionChoice::code).collect(Collectors.toSet());
        loadCampaignOverrideSystemIds();
        initialize();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        selectCampaignSystem();
    }

    @Override
    protected Container createCenterPane() {
        JPanel panel = new JPanel(new BorderLayout(PADDING, PADDING));
        panel.setName("pnlPlanetarySystemEditor");
        panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        panel.setPreferredSize(DEFAULT_SIZE);
        panel.add(createMainPane(), BorderLayout.CENTER);
        panel.add(createButtonPane(), BorderLayout.SOUTH);

        filterSystems();
        installKeyboardShortcuts(panel);
        return panel;
    }

    private Component createMainPane() {
        Component systemListPane = createSystemListPane();
        Component detailsPane = createDetailsPane();
        detailsPane.setMinimumSize(DETAILS_MINIMUM_SIZE);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, systemListPane, detailsPane);
        splitPane.setName("splitPlanetarySystemEditor");
        splitPane.setResizeWeight(0.32);
        splitPane.setDividerSize(PADDING);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        return splitPane;
    }

    private Component createSystemListPane() {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING));
        panel.setName("pnlPlanetarySystemList");
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(
              "PlanetarySystemEditorDialog.systems")));
        panel.setMinimumSize(SYSTEM_LIST_MINIMUM_SIZE);
        panel.setPreferredSize(SYSTEM_LIST_PREFERRED_SIZE);

        panel.add(createSystemFilterPane(), BorderLayout.NORTH);

        systemListModel = new DefaultListModel<>();
        systemList = new JList<>(systemListModel) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int index = locationToIndex(event.getPoint());
                if ((index < 0) || !getCellBounds(index, index).contains(event.getPoint())) {
                    return null;
                }
                Object value = getModel().getElementAt(index);
                return value instanceof PlanetarySystem system ? systemListTooltip(system) : null;
            }
        };
        systemList.setName("lstPlanetarySystems");
        systemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        systemList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlanetarySystem system) {
                    setText(systemListDisplayName(system));
                }
                return component;
            }
        });
        systemList.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                updateSelectedSystem();
            }
        });
        panel.add(new FastJScrollPane(systemList), BorderLayout.CENTER);

        return panel;
    }

    private Component createSystemFilterPane() {
        JPanel filterPanel = new JPanel();
        filterPanel.setName("pnlPlanetarySystemFilter");
        filterPanel.setLayout(new javax.swing.BoxLayout(filterPanel, javax.swing.BoxLayout.Y_AXIS));

        JPanel searchRow = new JPanel(new BorderLayout(PADDING, 0));
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.add(new JLabel(resources.getString("PlanetarySystemEditorDialog.search")), BorderLayout.WEST);
        txtSearch = new JTextField();
        txtSearch.setName("txtPlanetarySystemSearch");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent evt) {
                filterSystems();
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                filterSystems();
            }

            @Override
            public void changedUpdate(DocumentEvent evt) {
                filterSystems();
            }
        });
        searchRow.add(txtSearch, BorderLayout.CENTER);
        filterPanel.add(searchRow);
        filterPanel.add(javax.swing.Box.createVerticalStrut(PADDING / 2));

        JPanel filtersRow = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, 0));
        filtersRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkFilterUnsaved = new JCheckBox(resources.getString(
              "PlanetarySystemEditorDialog.filter.onlyUnsaved"));
        chkFilterUnsaved.setName("chkPlanetarySystemFilterUnsaved");
        chkFilterUnsaved.setToolTipText(resources.getString(
              "PlanetarySystemEditorDialog.filter.onlyUnsaved.toolTipText"));
        chkFilterUnsaved.addActionListener(evt -> filterSystems());
        filtersRow.add(chkFilterUnsaved);

        chkFilterOverrides = new JCheckBox(resources.getString(
              "PlanetarySystemEditorDialog.filter.onlyOverrides"));
        chkFilterOverrides.setName("chkPlanetarySystemFilterOverrides");
        chkFilterOverrides.setToolTipText(resources.getString(
              "PlanetarySystemEditorDialog.filter.onlyOverrides.toolTipText"));
        chkFilterOverrides.addActionListener(evt -> filterSystems());
        filtersRow.add(chkFilterOverrides);
        filterPanel.add(filtersRow);

        JPanel sortRow = new JPanel(new BorderLayout(PADDING, 0));
        sortRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sortRow.add(new JLabel(resources.getString("PlanetarySystemEditorDialog.sort.label")), BorderLayout.WEST);
        cboSort = new JComboBox<>(SystemSortOption.values());
        cboSort.setName("cboPlanetarySystemSort");
        cboSort.setToolTipText(resources.getString("PlanetarySystemEditorDialog.sort.toolTipText"));
        cboSort.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SystemSortOption option) {
                    setText(resources.getString(option.labelKey()));
                }
                return component;
            }
        });
        cboSort.addActionListener(evt -> filterSystems());
        sortRow.add(cboSort, BorderLayout.CENTER);
        filterPanel.add(sortRow);

        return filterPanel;
    }

    private Component createDetailsPane() {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING));
        panel.setName("pnlPlanetarySystemDetails");

        lblSelection = new JLabel(resources.getString("PlanetarySystemEditorDialog.noSelection"));
        lblSelection.setName("lblPlanetarySystemSelection");
        lblEditStatus = new JLabel(" ");
        lblEditStatus.setName("lblPlanetarySystemEditStatus");

        JPanel headerPanel = new JPanel(new BorderLayout(0, PADDING / 2));
        headerPanel.add(lblSelection, BorderLayout.NORTH);
        headerPanel.add(lblEditStatus, BorderLayout.SOUTH);
        panel.add(headerPanel, BorderLayout.NORTH);

        txtSystemDetails = createReadOnlyTextArea("txtPlanetarySystemDetails");
        txtValidation = createReadOnlyTextArea("txtPlanetarySystemValidation");

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setName("tabPlanetarySystemEditor");
        tabbedPane.addTab(resources.getString("PlanetarySystemEditorDialog.details"),
              new FastJScrollPane(txtSystemDetails));
        propertiesPanel = new PlanetarySystemPropertiesPanel(resources, campaign::getLocalDate, this::defaultEditorSource,
              this::onPropertiesChanged);
        tabbedPane.addTab(resources.getString("PlanetarySystemEditorDialog.systemEvents"),
              createSystemEventsPane());
        tabbedPane.addTab(resources.getString("PlanetarySystemEditorDialog.eventEditor"), createEventEditorPane());
        tabbedPane.addTab(resources.getString("PlanetarySystemEditorDialog.properties"), createPropertiesEditorPane());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane,
              createTitledPane("PlanetarySystemEditorDialog.validation", txtValidation));
        splitPane.setName("splitPlanetarySystemEditorTabs");
        splitPane.setResizeWeight(0.78);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private Component createEventEditorPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createPlanetListPane(),
              createPlanetEventPane());
        splitPane.setName("splitPlanetEvents");
        splitPane.setResizeWeight(PLANET_LIST_SPLIT_WEIGHT);
        return splitPane;
    }

    private Component createPropertiesEditorPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createPropertiesPlanetListPane(),
              propertiesPanel);
        splitPane.setName("splitPlanetProperties");
        splitPane.setResizeWeight(PLANET_LIST_SPLIT_WEIGHT);
        return splitPane;
    }

    private Component createSystemEventsPane() {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING));
        panel.setName("pnlPlanetarySystemSystemEvents");

        JLabel warning = new JLabel(resources.getString(
              "PlanetarySystemEditorDialog.systemEvents.warning"));
        warning.setForeground(java.awt.Color.RED);
        warning.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        panel.add(warning, BorderLayout.NORTH);

        systemEventTableModel = new SystemEventTableModel();
        tblSystemEvents = new JTable(systemEventTableModel);
        tblSystemEvents.setName("tblPlanetarySystemSystemEvents");
        tblSystemEvents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSystemEvents.setRowHeight(UIUtil.scaleForGUI(22));
        tblSystemEvents.getSelectionModel().addListSelectionListener(
              evt -> updateSystemEventButtonState());
        panel.add(createTitledComponentPane("PlanetarySystemEditorDialog.systemEvents.events",
              new FastJScrollPane(tblSystemEvents)), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, PADDING, 0));
        btnAddSystemEvent = new MMButton("btnAddSystemEvent",
              resources.getString("PlanetarySystemEditorDialog.systemEvents.add"),
              resources.getString("PlanetarySystemEditorDialog.systemEvents.add.toolTipText"),
              evt -> addSystemEvent());
        btnRemoveSystemEvent = new MMButton("btnRemoveSystemEvent",
              resources.getString("PlanetarySystemEditorDialog.systemEvents.remove"),
              resources.getString("PlanetarySystemEditorDialog.systemEvents.remove.toolTipText"),
              evt -> removeSelectedSystemEvent());
        buttons.add(btnAddSystemEvent);
        buttons.add(btnRemoveSystemEvent);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private Component createPlanetListPane() {
        planetList = createPlanetSelectionList("lstPlanetarySystemPlanets");
        btnRevertPlanet = createRevertPlanetButton("btnRevertPlanetaryPlanet");
        return createPlanetListWrapper(planetList, btnRevertPlanet);
    }

    private Component createPropertiesPlanetListPane() {
        propertiesPlanetList = createPlanetSelectionList("lstPlanetarySystemPropertiesPlanets");
        btnRevertPropertiesPlanet = createRevertPlanetButton("btnRevertPlanetaryPropertyPlanet");
        return createPlanetListWrapper(propertiesPlanetList, btnRevertPropertiesPlanet);
    }

    private JList<Planet> createPlanetSelectionList(String name) {
        ensurePlanetListModel();
        JList<Planet> list = new JList<>(planetListModel) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int index = locationToIndex(event.getPoint());
                if ((index < 0) || !getCellBounds(index, index).contains(event.getPoint())) {
                    return null;
                }
                Object value = getModel().getElementAt(index);
                return value instanceof Planet planet ? planetListTooltip(planet) : null;
            }
        };
        list.setName(name);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Planet planet) {
                    setText(planetListDisplayName(planet));
                }
                return component;
            }
        });
        list.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                synchronizePlanetSelection((JList<?>) evt.getSource());
            }
        });
        return list;
    }

    private void ensurePlanetListModel() {
        if (planetListModel == null) {
            planetListModel = new DefaultListModel<>();
        }
    }

    private Component createPlanetListWrapper(JList<Planet> list, JButton revertButton) {
        Component planetPane = createTitledComponentPane("PlanetarySystemEditorDialog.planetsHeader",
              new FastJScrollPane(list));
        planetPane.setMinimumSize(UIUtil.scaleForGUI(140, 120));
        planetPane.setPreferredSize(UIUtil.scaleForGUI(155, 320));

        JPanel wrapper = new JPanel(new BorderLayout(0, PADDING / 2));
        wrapper.add(planetPane, BorderLayout.CENTER);

        JPanel planetButtonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING / 2, 0));
        planetButtonRow.add(revertButton);
        wrapper.add(planetButtonRow, BorderLayout.SOUTH);

        return wrapper;
    }

    private JButton createRevertPlanetButton(String name) {
        return new MMButton(name,
              resources.getString("PlanetarySystemEditorDialog.revertPlanet"),
              resources.getString("PlanetarySystemEditorDialog.revertPlanet.toolTipText"),
              evt -> revertSelectedPlanetChanges());
    }

    private Component createPlanetEventPane() {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING));
        eventTableModel = new PlanetEventTableModel();
        tblEvents = new JTable(eventTableModel);
        tblEvents.setName("tblPlanetarySystemEvents");
        tblEvents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEvents.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                loadSelectedEvent();
            }
        });
        panel.add(createTitledComponentPane("PlanetarySystemEditorDialog.eventEditor.events",
              new FastJScrollPane(tblEvents)), BorderLayout.CENTER);
        panel.add(createEventFormPane(), BorderLayout.SOUTH);
        return panel;
    }

    private Component createEventFormPane() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("pnlPlanetarySystemEventForm");
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(
              "PlanetarySystemEditorDialog.eventEditor.editEvent")));

        txtEventDate = new JTextField(10);
        txtEventFactions = new JTextField(18);
        txtEventPopulation = new JTextField(12);
        attachPopulationFormatter(txtEventPopulation);
        cboEventHpg = new JComboBox<>();
        cboEventHpg.addItem("");
        for (HPGRating hpgRating : HPGRating.values()) {
            cboEventHpg.addItem(hpgRating.name());
        }
        txtEventSocioIndustrial = new JTextField(12);
        attachSocioIndustrialFormatter(txtEventSocioIndustrial);
        txtEventSource = new JTextField(18);
        txtEventSource.setText(defaultEditorSource());
        txtEventVersion = new JTextField(10);
        txtEventMessage = new JTextField(24);
        chkEventCustom = new JCheckBox(resources.getString("PlanetarySystemEditorDialog.eventEditor.custom"));

        int row = 0;
        addEventField(panel, row, 0, "PlanetarySystemEditorDialog.eventEditor.date",
              createDateFieldPane(txtEventDate, "btnPickPlanetaryEventDate"));
        addEventField(panel, row, 2, "PlanetarySystemEditorDialog.eventEditor.factions",
              createFactionFieldPane(txtEventFactions));
        addEventField(panel, row, 4, "PlanetarySystemEditorDialog.eventEditor.population", txtEventPopulation);
        row++;
        addEventField(panel, row, 0, "PlanetarySystemEditorDialog.eventEditor.hpg", cboEventHpg);
        addEventField(panel, row, 2, "PlanetarySystemEditorDialog.eventEditor.socioIndustrial",
              txtEventSocioIndustrial);
        addEventField(panel, row, 4, "PlanetarySystemEditorDialog.eventEditor.source", txtEventSource);
        row++;
        addEventField(panel, row, 0, "PlanetarySystemEditorDialog.eventEditor.version", txtEventVersion);
        addEventField(panel, row, 2, "PlanetarySystemEditorDialog.eventEditor.message", txtEventMessage, 3);
        row++;

        GridBagConstraints constraints = createGridBagConstraints(row, 0, 1);
        constraints.gridwidth = 2;
        panel.add(chkEventCustom, constraints);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, PADDING, 0));
        btnAddUpdateEvent = new MMButton("btnAddUpdatePlanetaryEvent",
              resources.getString("PlanetarySystemEditorDialog.eventEditor.addUpdate"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.addUpdate.toolTipText"),
              evt -> addOrUpdateSelectedEvent());
        buttonPanel.add(btnAddUpdateEvent);

        btnDuplicateEvent = new MMButton("btnDuplicatePlanetaryEvent",
              resources.getString("PlanetarySystemEditorDialog.eventEditor.duplicate"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.duplicate.toolTipText"),
              evt -> duplicateSelectedEvent());
        buttonPanel.add(btnDuplicateEvent);

        btnRemoveEvent = new MMButton("btnRemovePlanetaryEvent",
              resources.getString("PlanetarySystemEditorDialog.eventEditor.remove"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.remove.toolTipText"),
              evt -> removeSelectedEvent());
        buttonPanel.add(btnRemoveEvent);

        btnClearEvent = new MMButton("btnClearPlanetaryEventForm",
              resources.getString("PlanetarySystemEditorDialog.eventEditor.clear"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.clear.toolTipText"),
              evt -> clearEventFields());
        buttonPanel.add(btnClearEvent);

        btnTransferOwnership = new MMButton("btnTransferPlanetaryOwnership",
              resources.getString("PlanetarySystemEditorDialog.eventEditor.transferOwnership"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.transferOwnership.toolTipText"),
              evt -> transferOwnership());
        buttonPanel.add(btnTransferOwnership);

        constraints = createGridBagConstraints(row, 2, 1);
        constraints.gridwidth = 4;
        constraints.anchor = GridBagConstraints.EAST;
        panel.add(buttonPanel, constraints);

        return panel;
    }

    private void attachPopulationFormatter(JTextField textField) {
        if (textField.getDocument() instanceof AbstractDocument document) {
            document.setDocumentFilter(new PopulationDocumentFilter(textField));
        }
    }

    private void attachSocioIndustrialFormatter(JTextField textField) {
        if (textField.getDocument() instanceof AbstractDocument document) {
            document.setDocumentFilter(new SocioIndustrialDocumentFilter(textField));
        }
    }

    private Component createDateFieldPane(JTextField textField, String buttonName) {
        JButton btnPickDate = new MMButton(buttonName,
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickDate"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickDate.toolTipText"),
              evt -> chooseDate(textField));
        Dimension textFieldSize = new Dimension(UIUtil.scaleForGUI(104), textField.getPreferredSize().height);
        Dimension buttonSize = new Dimension(UIUtil.scaleForGUI(32), textField.getPreferredSize().height);
        textField.setMinimumSize(textFieldSize);
        textField.setPreferredSize(textFieldSize);
        btnPickDate.setMargin(new Insets(0, 0, 0, 0));
        btnPickDate.setMinimumSize(buttonSize);
        btnPickDate.setPreferredSize(buttonSize);
        return createInlineFieldWithButton(textField, btnPickDate);
    }

    private Component createFactionFieldPane(JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(PADDING / 2, 0));
        panel.add(textField, BorderLayout.CENTER);

        btnPickFactions = new MMButton("btnPickPlanetaryEventFactions",
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickFactions"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickFactions.toolTipText"),
              evt -> chooseFactionCodes(textField, getEventDateOrCampaignDate(txtEventDate.getText())));
        panel.add(btnPickFactions, BorderLayout.EAST);

        return panel;
    }

    private JTextArea createReadOnlyTextArea(String name) {
        JTextArea textArea = new JTextArea();
        textArea.setName(name);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, textArea.getFont().getSize()));
        return textArea;
    }

    private Component createTitledPane(String titleKey, JTextArea textArea) {
        return createTitledComponentPane(titleKey, new FastJScrollPane(textArea));
    }

    private Component createTitledComponentPane(String titleKey, Component component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(titleKey)));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void addEventField(JPanel panel, int row, int column, String labelKey, Component component) {
        addEventField(panel, row, column, labelKey, component, 1);
    }

    private void addEventField(JPanel panel, int row, int column, String labelKey, Component component, int width) {
        GridBagConstraints labelConstraints = createGridBagConstraints(row, column, 1);
        labelConstraints.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(resources.getString(labelKey)), labelConstraints);

        GridBagConstraints fieldConstraints = createGridBagConstraints(row, column + 1, width);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        panel.add(component, fieldConstraints);
    }

    private GridBagConstraints createGridBagConstraints(int row, int column, int width) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = width;
        constraints.insets = new Insets(0, PADDING / 2, PADDING / 2, PADDING / 2);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    private Component createButtonPane() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        panel.setName("pnlPlanetarySystemEditorButtons");

        btnSave = new MMButton("btnSavePlanetarySystemOverride",
              resources.getString("PlanetarySystemEditorDialog.save"),
              resources.getString("PlanetarySystemEditorDialog.save.toolTipText"),
              evt -> saveUnsavedSystems());
        btnReviewChanges = new MMButton("btnReviewPlanetarySystemChanges",
              resources.getString("PlanetarySystemEditorDialog.reviewChanges"),
              resources.getString("PlanetarySystemEditorDialog.reviewChanges.toolTipText"),
              evt -> reviewAllSystemChanges());
        panel.add(btnReviewChanges);
        panel.add(btnSave);

        btnRevertChanges = new MMButton("btnRevertPlanetarySystemChanges",
              resources.getString("PlanetarySystemEditorDialog.revertChanges"),
              resources.getString("PlanetarySystemEditorDialog.revertChanges.toolTipText"),
              evt -> revertSelectedSystemChanges());
        panel.add(btnRevertChanges);

        btnDeleteOverride = new MMButton("btnDeletePlanetarySystemOverride",
              resources.getString("PlanetarySystemEditorDialog.deleteOverride"),
              resources.getString("PlanetarySystemEditorDialog.deleteOverride.toolTipText"),
              evt -> deleteSelectedSystemOverride());
        panel.add(btnDeleteOverride);

        panel.add(new MMButton("btnClosePlanetarySystemEditor", resources, "Close.text", evt -> closeEditor()));

        updateButtonState(null);
        return panel;
    }

    private void selectCampaignSystem() {
        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        if (currentSystem != null) {
            for (int index = 0; index < systemListModel.size(); index++) {
                PlanetarySystem listedSystem = systemListModel.get(index);
                if (Objects.equals(listedSystem.getId(), currentSystem.getId())) {
                    systemList.setSelectedIndex(index);
                    systemList.ensureIndexIsVisible(index);
                    return;
                }
            }
        }

        if (!systemListModel.isEmpty()) {
            systemList.setSelectedIndex(0);
        }
    }

    /**
     * Selects the system with the given id in the list (if it is currently displayed by the active filter). Safe to
     * call from outside the dialog (e.g. from a right-click menu opening the editor onto a specific system).
     */
    public void selectSystemById(String systemId) {
        if ((systemId == null) || (systemListModel == null)) {
            return;
        }
        for (int index = 0; index < systemListModel.size(); index++) {
            PlanetarySystem listedSystem = systemListModel.get(index);
            if ((listedSystem != null) && Objects.equals(listedSystem.getId(), systemId)) {
                systemList.setSelectedIndex(index);
                systemList.ensureIndexIsVisible(index);
                return;
            }
        }
    }

    private void filterSystems() {
        if (systemListModel == null) {
            return;
        }

        String filter = txtSearch == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        boolean onlyUnsaved = (chkFilterUnsaved != null) && chkFilterUnsaved.isSelected();
        boolean onlyOverrides = (chkFilterOverrides != null) && chkFilterOverrides.isSelected();
        SystemSortOption sortOption = (cboSort != null) && (cboSort.getSelectedItem() instanceof SystemSortOption sort)
                                            ? sort : SystemSortOption.NAME;
        PlanetarySystem selectedSystem = getSelectedSystem();
        systemListModel.clear();

        List<PlanetarySystem> filtered = new ArrayList<>();
        for (PlanetarySystem system : systems) {
            if (!filter.isBlank()
                      && !systemDisplayName(system).toLowerCase(Locale.ROOT).contains(filter)) {
                continue;
            }
            if (onlyUnsaved && !hasUnsavedChanges(system)) {
                continue;
            }
            if (onlyOverrides && !hasCampaignOverride(system)) {
                continue;
            }
            filtered.add(system);
        }

        filtered.sort(sortOption.comparator(this));
        for (PlanetarySystem system : filtered) {
            systemListModel.addElement(system);
        }

        restoreSelection(selectedSystem);
    }

    private void restoreSelection(PlanetarySystem selectedSystem) {
        if (systemListModel.isEmpty()) {
            updateSelectedSystem();
            return;
        }

        if (selectedSystem != null) {
            for (int index = 0; index < systemListModel.size(); index++) {
                if (Objects.equals(systemListModel.get(index).getId(), selectedSystem.getId())) {
                    systemList.setSelectedIndex(index);
                    return;
                }
            }
        }

        systemList.setSelectedIndex(0);
    }

    private void loadCampaignOverrideSystemIds() {
        overrideSystemIds.clear();
        for (PlanetarySystem system : systems) {
            String systemId = planetarySystemId(system);
            if (systemId == null) {
                continue;
            }
            if (campaign.hasPlanetarySystemOverride(systemId)) {
                overrideSystemIds.add(systemId);
            }
        }
    }

    private void repaintSystemList() {
        if (systemList != null) {
            systemList.revalidate();
            systemList.repaint();
        }
    }

    private void repaintPlanetList() {
        if (planetList != null) {
            planetList.revalidate();
            planetList.repaint();
        }
        if (propertiesPlanetList != null) {
            propertiesPlanetList.revalidate();
            propertiesPlanetList.repaint();
        }
    }

    private void ensureBaselineSnapshot(PlanetarySystem system) {
        String systemId = planetarySystemId(system);
        if ((systemId == null) || baselineSystems.containsKey(systemId)) {
            return;
        }

        try {
            baselineSystems.put(systemId, PlanetarySystemYamlIO.copy(system));
        } catch (IOException ex) {
            LOGGER.error(ex, "Could not snapshot planetary system baseline for {}", systemId);
        }
    }

    private void refreshBaselineSnapshot(PlanetarySystem system) {
        String systemId = planetarySystemId(system);
        if (systemId == null) {
            return;
        }
        baselineSystems.remove(systemId);
        ensureBaselineSnapshot(system);
    }

    private PlanetarySystem getSelectedSystem() {
        return systemList == null ? null : systemList.getSelectedValue();
    }

    private void updateSelectedSystem() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        if (selectedSystem == null) {
            lblSelection.setText(resources.getString("PlanetarySystemEditorDialog.noSelection"));
            lblEditStatus.setText(" ");
            txtSystemDetails.setText("");
            txtValidation.setText("");
            populatePlanetList(null);
            updateButtonState(null);
            if (propertiesPanel != null) {
                propertiesPanel.setSelection(null, null);
            }
            refreshSystemEvents(null);
            return;
        }

        ensureBaselineSnapshot(selectedSystem);
        selectedSystem = ensureWorkingCopy(selectedSystem);
        if (selectedSystem == null) {
            return;
        }
        LocalDate currentDate = campaign.getLocalDate();
        lblSelection.setText(MessageFormat.format(resources.getString("PlanetarySystemEditorDialog.selection"),
              systemDisplayName(selectedSystem), currentDate));
        txtSystemDetails.setText(buildSystemDetails(selectedSystem, currentDate));
        txtSystemDetails.setCaretPosition(0);
        setValidationText(validator.validate(selectedSystem, sourceName(selectedSystem)));
        populatePlanetList(selectedSystem);
        updateEditStatus(selectedSystem);
        updateButtonState(selectedSystem);
        refreshSystemEvents(null);
    }

    private PlanetarySystem ensureWorkingCopy(PlanetarySystem system) {
        String systemId = planetarySystemId(system);
        if ((systemId == null) || workingSystemIds.contains(systemId)) {
            return system;
        }

        try {
            PlanetarySystem workingCopy = PlanetarySystemYamlIO.copy(system);
            replaceSystemInEditor(systemId, workingCopy);
            workingSystemIds.add(systemId);
            return workingCopy;
        } catch (IOException ex) {
            LOGGER.error(ex, "Could not create planetary system working copy for {}", systemId);
            JOptionPane.showMessageDialog(this, MessageFormat.format(resources.getString(
                        "PlanetarySystemEditorDialog.workingCopyFailed"), ex.getMessage()),
                  resources.getString("PlanetarySystemEditorDialog.workingCopyFailed.title"),
                  JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void replaceSystemInEditor(String systemId, PlanetarySystem replacement) {
        for (int index = 0; index < systems.size(); index++) {
            if (Objects.equals(planetarySystemId(systems.get(index)), systemId)) {
                systems.set(index, replacement);
                break;
            }
        }
        for (int index = 0; index < systemListModel.size(); index++) {
            PlanetarySystem listed = systemListModel.get(index);
            if (Objects.equals(planetarySystemId(listed), systemId)) {
                systemListModel.set(index, replacement);
                systemList.setSelectedIndex(index);
                break;
            }
        }
    }

    private void populatePlanetList(PlanetarySystem selectedSystem) {
        if (planetListModel == null) {
            return;
        }

        Planet selectedPlanet = getSelectedPlanet();
        planetListModel.clear();
        if (selectedSystem == null) {
            clearPlanetSelection();
            updateSelectedPlanet();
            return;
        }

        for (Planet planet : planetsInDisplayOrder(selectedSystem)) {
            planetListModel.addElement(planet);
        }

        restorePlanetSelection(selectedPlanet);
    }

    private void restorePlanetSelection(Planet selectedPlanet) {
        if (planetListModel.isEmpty()) {
            clearPlanetSelection();
            updateSelectedPlanet();
            return;
        }

        if (selectedPlanet != null) {
            for (int index = 0; index < planetListModel.size(); index++) {
                if (Objects.equals(planetListModel.get(index).getId(), selectedPlanet.getId())) {
                    selectPlanetIndex(index);
                    return;
                }
            }
        }

        selectPlanetIndex(0);
    }

    private Planet getSelectedPlanet() {
        if ((planetList != null) && (planetList.getSelectedValue() != null)) {
            return planetList.getSelectedValue();
        }
        return propertiesPlanetList == null ? null : propertiesPlanetList.getSelectedValue();
    }

    private void synchronizePlanetSelection(JList<?> sourceList) {
        if (updatingPlanetSelection) {
            return;
        }
        updatingPlanetSelection = true;
        try {
            setPlanetSelectionIndex(planetList, sourceList.getSelectedIndex());
            setPlanetSelectionIndex(propertiesPlanetList, sourceList.getSelectedIndex());
        } finally {
            updatingPlanetSelection = false;
        }
        updateSelectedPlanet();
    }

    private void selectPlanetIndex(int index) {
        updatingPlanetSelection = true;
        try {
            setPlanetSelectionIndex(planetList, index);
            setPlanetSelectionIndex(propertiesPlanetList, index);
        } finally {
            updatingPlanetSelection = false;
        }
        updateSelectedPlanet();
    }

    private void clearPlanetSelection() {
        updatingPlanetSelection = true;
        try {
            clearPlanetSelection(planetList);
            clearPlanetSelection(propertiesPlanetList);
        } finally {
            updatingPlanetSelection = false;
        }
    }

    private void setPlanetSelectionIndex(JList<Planet> list, int index) {
        if (list == null) {
            return;
        }
        if (index >= 0) {
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        } else {
            list.clearSelection();
        }
    }

    private void clearPlanetSelection(JList<Planet> list) {
        if (list != null) {
            list.clearSelection();
        }
    }

    private void updateSelectedPlanet() {
        Planet selectedPlanet = getSelectedPlanet();
        if (eventTableModel != null) {
            eventTableModel.setPlanet(selectedPlanet);
        }
        clearEventFields();
        updateEventButtonState();
        if (propertiesPanel != null) {
            propertiesPanel.setSelection(getSelectedSystem(), selectedPlanet);
        }
    }

    private void setValidationText(ValidationResult result) {
        StringBuilder text = new StringBuilder(MessageFormat.format(resources.getString(
              "PlanetarySystemEditorDialog.validationSummary"), result.getErrorCount(), result.getWarningCount()));

        if (result.getMessages().isEmpty()) {
            text.append(System.lineSeparator())
                  .append(System.lineSeparator())
                  .append(resources.getString("PlanetarySystemEditorDialog.validationPasses"));
        } else {
            for (ValidationMessage message : result.getMessages()) {
                text.append(System.lineSeparator()).append(message);
            }
        }

        txtValidation.setText(text.toString());
        txtValidation.setCaretPosition(0);
    }

    private void updateButtonState(PlanetarySystem selectedSystem) {
        boolean canEdit = campaign.isGM();
        boolean hasUnsavedChanges = hasAnyUnsavedChanges();
        btnReviewChanges.setEnabled(canEdit && hasUnsavedChanges);
        btnSave.setEnabled(canEdit && hasUnsavedChanges);
        btnRevertChanges.setEnabled(canEdit && (selectedSystem != null) && hasUnsavedChanges(selectedSystem));
        btnDeleteOverride.setEnabled(canEdit && (selectedSystem != null) && hasCampaignOverride(selectedSystem));
        updateEventButtonState();
        updateSystemEventButtonState();
    }

    private void updateEventButtonState() {
        boolean canEdit = (getSelectedSystem() != null) && (getSelectedPlanet() != null) && campaign.isGM();
        if (btnAddUpdateEvent != null) {
            btnAddUpdateEvent.setEnabled(canEdit);
        }
        if (btnRemoveEvent != null) {
            btnRemoveEvent.setEnabled(canEdit && (getSelectedEvent() != null));
        }
        if (btnDuplicateEvent != null) {
            btnDuplicateEvent.setEnabled(canEdit && (getSelectedEvent() != null));
        }
        if (btnClearEvent != null) {
            btnClearEvent.setEnabled(canEdit);
        }
        if (btnPickFactions != null) {
            btnPickFactions.setEnabled(canEdit);
        }
        if (btnTransferOwnership != null) {
            btnTransferOwnership.setEnabled(canEdit);
        }
        if (btnRevertPlanet != null) {
            btnRevertPlanet.setEnabled(canEdit && hasUnsavedPlanetChanges(getSelectedPlanet()));
        }
        if (btnRevertPropertiesPlanet != null) {
            btnRevertPropertiesPlanet.setEnabled(canEdit && hasUnsavedPlanetChanges(getSelectedPlanet()));
        }
    }

    private void updateEditStatus(PlanetarySystem selectedSystem) {
        if (lblEditStatus == null) {
            return;
        }

        List<String> statuses = new ArrayList<>();
        if (hasUnsavedChanges(selectedSystem)) {
            statuses.add(resources.getString("PlanetarySystemEditorDialog.status.unsaved"));
        }
        if (hasCampaignOverride(selectedSystem)) {
            statuses.add(resources.getString("PlanetarySystemEditorDialog.status.campaignOverride"));
        }

        lblEditStatus.setText(statuses.isEmpty() ? " " : String.join(" | ", statuses));
    }

    private void loadSelectedEvent() {
        PlanetaryEvent selectedEvent = getSelectedEvent();
        if (selectedEvent == null) {
            clearEventFields();
        } else {
            populateEventFields(selectedEvent);
        }
        updateEventButtonState();
    }

    private PlanetaryEvent getSelectedEvent() {
        if ((tblEvents == null) || (eventTableModel == null) || (tblEvents.getSelectedRow() < 0)) {
            return null;
        }
        return eventTableModel.getEventAt(tblEvents.convertRowIndexToModel(tblEvents.getSelectedRow()));
    }

    private void addOrUpdateSelectedEvent() {
        Planet selectedPlanet = getSelectedPlanet();
        if ((selectedPlanet == null) || !campaign.isGM()) {
            return;
        }

        LocalDate previousDate = null;
        PlanetaryEvent selectedEvent = getSelectedEvent();
        if (selectedEvent != null) {
            previousDate = selectedEvent.date;
        }

        try {
            PlanetaryEvent updatedEvent = buildEventFromFields();
            if (!confirmUnknownFactionCodes(getFactionCodes(updatedEvent))) {
                return;
            }
            if ((previousDate != null) && !previousDate.equals(updatedEvent.date)) {
                selectedPlanet.removeEvent(previousDate);
            }
            selectedPlanet.putEvent(updatedEvent);
            markUnsavedChanges();
            refreshEditedSystem(updatedEvent.date);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedEvent() {
        Planet selectedPlanet = getSelectedPlanet();
        PlanetaryEvent selectedEvent = getSelectedEvent();
        if ((selectedPlanet == null) || (selectedEvent == null) || !campaign.isGM()) {
            return;
        }

        selectedPlanet.removeEvent(selectedEvent.date);
        markUnsavedChanges();
        refreshEditedSystem(null);
    }

    private void addSystemEvent() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        if ((selectedSystem == null) || !campaign.isGM()) {
            return;
        }

        JTextField txtNewDate = new JTextField(formatDate(campaign.getLocalDate()), 12);
        JPanel panel = new JPanel(new GridBagLayout());
        addEventField(panel, 0, 0, "PlanetarySystemEditorDialog.eventEditor.date",
              createDateFieldPane(txtNewDate, "btnPickNewSystemEventDate"));

        int choice = JOptionPane.showConfirmDialog(this, panel,
              resources.getString("PlanetarySystemEditorDialog.systemEvents.add.title"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            LocalDate eventDate = parseEventDate(txtNewDate.getText());
            List<PlanetarySystemEvent> existing = nullToEmptyList(selectedSystem.getEvents());
            for (PlanetarySystemEvent ev : existing) {
                if (eventDate.equals(ev.date)) {
                    JOptionPane.showMessageDialog(this, MessageFormat.format(resources.getString(
                                "PlanetarySystemEditorDialog.systemEvents.add.duplicate"), formatDate(eventDate)),
                          resources.getString("PlanetarySystemEditorDialog.eventEditor.invalid.title"),
                          JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            PlanetarySystemEvent event = new PlanetarySystemEvent();
            event.date = eventDate;
            selectedSystem.putEvent(event);
            markUnsavedChanges();
            refreshSystemEvents(eventDate);
            onPropertiesChanged();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                  resources.getString("PlanetarySystemEditorDialog.eventEditor.invalid.title"),
                  JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedSystemEvent() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        PlanetarySystemEvent selectedEvent = getSelectedSystemEvent();
        if ((selectedSystem == null) || (selectedEvent == null) || !campaign.isGM()) {
            return;
        }
        selectedSystem.removeEvent(selectedEvent.date);
        markUnsavedChanges();
        refreshSystemEvents(null);
        onPropertiesChanged();
    }

    private PlanetarySystemEvent getSelectedSystemEvent() {
        if ((tblSystemEvents == null) || (systemEventTableModel == null)
                  || (tblSystemEvents.getSelectedRow() < 0)) {
            return null;
        }
        return systemEventTableModel.getEventAt(
              tblSystemEvents.convertRowIndexToModel(tblSystemEvents.getSelectedRow()));
    }

    private void refreshSystemEvents(LocalDate selectEventDate) {
        if (systemEventTableModel != null) {
            systemEventTableModel.setSystem(getSelectedSystem());
            if (selectEventDate != null) {
                int row = systemEventTableModel.findEventRow(selectEventDate);
                if (row >= 0) {
                    tblSystemEvents.setRowSelectionInterval(row, row);
                    tblSystemEvents.scrollRectToVisible(tblSystemEvents.getCellRect(row, 0, true));
                }
            }
        }
        updateSystemEventButtonState();
    }

    private void updateSystemEventButtonState() {
        boolean canEdit = (getSelectedSystem() != null) && campaign.isGM();
        if (btnAddSystemEvent != null) {
            btnAddSystemEvent.setEnabled(canEdit);
        }
        if (btnRemoveSystemEvent != null) {
            btnRemoveSystemEvent.setEnabled(canEdit && (getSelectedSystemEvent() != null));
        }
        if (tblSystemEvents != null) {
            tblSystemEvents.setEnabled(canEdit);
        }
    }

    private static <T> List<T> nullToEmptyList(List<T> list) {
        return list == null ? List.of() : list;
    }
    private void duplicateSelectedEvent() {
        Planet selectedPlanet = getSelectedPlanet();
        PlanetaryEvent selectedEvent = getSelectedEvent();
        if ((selectedPlanet == null) || (selectedEvent == null) || !campaign.isGM()) {
            return;
        }

        JTextField txtDuplicateDate = new JTextField(formatDate(selectedEvent.date.plusDays(1)), 12);
        JPanel duplicatePanel = new JPanel(new GridBagLayout());
        addEventField(duplicatePanel, 0, 0, "PlanetarySystemEditorDialog.eventEditor.date",
              createDateFieldPane(txtDuplicateDate, "btnPickDuplicatePlanetaryEventDate"));

        int choice = JOptionPane.showConfirmDialog(this, duplicatePanel,
              resources.getString("PlanetarySystemEditorDialog.eventEditor.duplicate.title"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            LocalDate duplicateDate = parseEventDate(txtDuplicateDate.getText());
            if (duplicateDate.equals(selectedEvent.date)) {
                throw new IllegalArgumentException(resources.getString(
                      "PlanetarySystemEditorDialog.eventEditor.duplicate.sameDate"));
            }
            if ((getEventForDate(selectedPlanet, duplicateDate) != null) && !confirmDuplicateEventReplacement(
                  duplicateDate)) {
                return;
            }

            PlanetaryEvent duplicateEvent = new PlanetaryEvent();
            duplicateEvent.date = duplicateDate;
            duplicateEvent.copyDataFrom(selectedEvent);
            selectedPlanet.putEvent(duplicateEvent);
            markUnsavedChanges();
            refreshEditedSystem(duplicateDate);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshEditedSystem(LocalDate selectedEventDate) {
        Planet selectedPlanet = getSelectedPlanet();
        if (eventTableModel != null) {
            eventTableModel.setPlanet(selectedPlanet);
            if (selectedEventDate != null) {
                selectEvent(selectedEventDate);
            }
        }

        PlanetarySystem selectedSystem = getSelectedSystem();
        if (selectedSystem != null) {
            LocalDate currentDate = campaign.getLocalDate();
            txtSystemDetails.setText(buildSystemDetails(selectedSystem, currentDate));
            txtSystemDetails.setCaretPosition(0);
            setValidationText(validator.validate(selectedSystem, sourceName(selectedSystem)));
        }
        updateButtonState(selectedSystem);
        updateEditStatus(selectedSystem);
        repaintPlanetList();
    }

    private void selectEvent(LocalDate eventDate) {
        int row = eventTableModel.findEventRow(eventDate);
        if (row >= 0) {
            tblEvents.setRowSelectionInterval(row, row);
            tblEvents.scrollRectToVisible(tblEvents.getCellRect(row, 0, true));
        }
    }

    private PlanetaryEvent buildEventFromFields() {
        PlanetaryEvent event = new PlanetaryEvent();
        event.date = parseEventDate();
        event.faction = sourceableValue(parseFactionCodes());
        event.population = sourceableValue(parsePopulation());
        event.hpg = sourceableValue(parseHpgRating());
        event.socioIndustrial = sourceableValue(parseSocioIndustrialData());
        event.message = blankToNull(txtEventMessage.getText());
        event.custom = chkEventCustom.isSelected();

        if (event.isEmpty() && !event.custom) {
            throw new IllegalArgumentException(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.empty"));
        }
        return event;
    }

    private LocalDate parseEventDate() {
        return parseEventDate(txtEventDate.getText());
    }

    private LocalDate parseEventDate(String eventDateText) {
        String text = blankToNull(eventDateText);
        if (text == null) {
            throw new IllegalArgumentException(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.dateRequired"));
        }
        try {
            return LocalDate.parse(text, EVENT_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.date"), text), ex);
        }
    }

    private List<String> parseFactionCodes() {
        return parseFactionCodes(txtEventFactions.getText());
    }

    private List<String> parseFactionCodes(String factionText) {
        String text = blankToNull(factionText);
        if (text == null) {
            return null;
        }
        Set<String> factionCodes = new LinkedHashSet<>();
        for (String code : text.split("[,\\s]+")) {
            String trimmedCode = code.trim().toUpperCase(Locale.ROOT);
            if (!trimmedCode.isEmpty()) {
                factionCodes.add(trimmedCode);
            }
        }
        return factionCodes.isEmpty() ? null : new ArrayList<>(factionCodes);
    }

    private Long parsePopulation() {
        String text = blankToNull(txtEventPopulation.getText().trim());
        if (text == null) {
            return null;
        }
        String compactText = compactPopulationInput(text);
        if (!text.equals(compactText)) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.population"), text));
        }
        try {
            return Long.parseLong(compactText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.population"), text), ex);
        }
    }

    private static String compactPopulationInput(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder compactText = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            int digit = Character.digit(text.charAt(index), 10);
            if (digit >= 0) {
                compactText.append(digit);
            }
        }
        return compactText.toString();
    }

    private static String formatPopulationInput(String text) {
        return compactPopulationInput(text);
    }

    private HPGRating parseHpgRating() {
        String text = blankToNull((String) cboEventHpg.getSelectedItem());
        if (text == null) {
            return null;
        }
        return HPGRating.valueOf(text);
    }

    private SocioIndustrialData parseSocioIndustrialData() {
        String text = blankToNull(formatSocioIndustrialInput(txtEventSocioIndustrial.getText()));
        if (text == null) {
            return null;
        }
        validateSocioIndustrialCode(text);
        return SocioIndustrialData.parse(text);
    }

    private void validateSocioIndustrialCode(String text) {
        String[] ratings = text.trim().toUpperCase(Locale.ROOT).split("-");
        if (ratings.length != 5) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.socioIndustrial"), text));
        }

        if (!isValidSocioIndustrialTechCode(ratings[0])) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.socioIndustrial"), text));
        }
        for (int index = 1; index < ratings.length; index++) {
            if (!isValidSocioIndustrialRatingCode(ratings[index])) {
                throw new IllegalArgumentException(MessageFormat.format(resources.getString(
                      "PlanetarySystemEditorDialog.eventEditor.invalid.socioIndustrial"), text));
            }
        }
    }

    private static boolean isValidSocioIndustrialTechCode(String code) {
        return Set.of("ADV", "A", "B", "C", "D", "F", "R", "X").contains(code.trim());
    }

    private static boolean isValidSocioIndustrialRatingCode(String code) {
        return Set.of("A", "B", "C", "D", "F", "X").contains(code.trim());
    }

    private static String formatSocioIndustrialInput(String text) {
        List<String> ratings = parseSocioIndustrialInputTokens(text);
        return ratings.isEmpty() ? "" : String.join("-", ratings);
    }

    private static List<String> parseSocioIndustrialInputTokens(String text) {
        String compactText = compactSocioIndustrialInput(text);
        if (compactText.isEmpty()) {
            return List.of();
        }

        List<String> ratings = new ArrayList<>();
        while (!compactText.isEmpty() && ratings.isEmpty()) {
            if (compactText.startsWith("ADVANCED")) {
                ratings.add("ADV");
                compactText = compactText.substring("ADVANCED".length());
            } else if (compactText.startsWith("REGRESSED")) {
                ratings.add("R");
                compactText = compactText.substring("REGRESSED".length());
            } else if (compactText.startsWith("ADV")) {
                ratings.add("ADV");
                compactText = compactText.substring("ADV".length());
            } else {
                String code = String.valueOf(compactText.charAt(0));
                compactText = compactText.substring(1);
                if (isValidSocioIndustrialTechCode(code)) {
                    ratings.add(code);
                }
            }
        }

        for (int index = 0; (index < compactText.length()) && (ratings.size() < 5); index++) {
            String code = String.valueOf(compactText.charAt(index));
            if (isValidSocioIndustrialRatingCode(code)) {
                ratings.add(code);
            }
        }
        return ratings;
    }

    private static String compactSocioIndustrialInput(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder compactText = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (Character.isLetter(character)) {
                compactText.append(Character.toUpperCase(character));
            }
        }
        return compactText.toString();
    }

    private <T> SourceableValue<T> sourceableValue(T value) {
        if (value == null) {
            return null;
        }
        return sourceableValue(value, txtEventSource.getText(), txtEventVersion.getText());
    }

    private static <T> SourceableValue<T> sourceableValue(T value, String source, String version) {
        if (value == null) {
            return null;
        }
        return SourceableValue.of(blankToNull(source), blankToNull(version), value);
    }

    private void populateEventFields(PlanetaryEvent event) {
        txtEventDate.setText(formatDate(event.date));
        txtEventFactions.setText(formatSourceableList(event.faction));
        txtEventPopulation.setText(formatSourceableValue(event.population));
        cboEventHpg.setSelectedItem(formatSourceableHpg(event.hpg));
        txtEventSocioIndustrial.setText(formatSourceableValue(event.socioIndustrial));
        txtEventSource.setText(firstSource(event));
        txtEventVersion.setText(firstVersion(event));
        txtEventMessage.setText(nullToBlank(event.message));
        chkEventCustom.setSelected(event.custom);
    }

    private void clearEventFields() {
        if (txtEventDate == null) {
            return;
        }
        if ((tblEvents != null) && (tblEvents.getSelectedRow() >= 0)) {
            tblEvents.clearSelection();
        }
        txtEventDate.setText(formatDate(campaign.getLocalDate()));
        txtEventFactions.setText("");
        txtEventPopulation.setText("");
        cboEventHpg.setSelectedItem("");
        txtEventSocioIndustrial.setText("");
        txtEventSource.setText(defaultEditorSource());
        txtEventVersion.setText("");
        txtEventMessage.setText("");
        chkEventCustom.setSelected(false);
    }

    private void chooseFactionCodes(JTextField targetField, LocalDate effectiveDate) {
        DefaultListModel<FactionChoice> listModel = new DefaultListModel<>();
        JList<FactionChoice> factionList = new JList<>(listModel);
        factionList.setName("lstPlanetarySystemFactionPicker");
        factionList.setVisibleRowCount(14);
        factionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JTextField txtFactionFilter = new JTextField(28);
        txtFactionFilter.setName("txtPlanetarySystemFactionPickerFilter");
        txtFactionFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent evt) {
                refreshFactionChoices(listModel, txtFactionFilter.getText(), effectiveDate);
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                refreshFactionChoices(listModel, txtFactionFilter.getText(), effectiveDate);
            }

            @Override
            public void changedUpdate(DocumentEvent evt) {
                refreshFactionChoices(listModel, txtFactionFilter.getText(), effectiveDate);
            }
        });

        refreshFactionChoices(listModel, "", effectiveDate);
        selectFactionChoices(factionList, listModel, parseFactionCodes(targetField.getText()));

        JPanel panel = new JPanel(new BorderLayout(0, PADDING));
        JPanel filterPanel = new JPanel(new BorderLayout(PADDING, 0));
        filterPanel.add(new JLabel(resources.getString("PlanetarySystemEditorDialog.search")), BorderLayout.WEST);
        filterPanel.add(txtFactionFilter, BorderLayout.CENTER);
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new FastJScrollPane(factionList), BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(this, panel,
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickFactions.title"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            List<String> selectedCodes = factionList.getSelectedValuesList()
                                             .stream()
                                             .map(FactionChoice::code)
                                             .toList();
            if (!selectedCodes.isEmpty()) {
                targetField.setText(formatFactionCodes(selectedCodes));
            }
        }
    }

    private void chooseDate(JTextField targetField) {
        DateChooser dateChooser = new DateChooser(this, getEventDateOrCampaignDate(targetField.getText()));
        if (dateChooser.showDateChooser() == DateChooser.OK_OPTION) {
            targetField.setText(formatDate(dateChooser.getDate()));
        }
    }

    private void refreshFactionChoices(DefaultListModel<FactionChoice> listModel, String filter,
          LocalDate effectiveDate) {
        String normalizedFilter = blankToNull(filter) == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        listModel.clear();
        for (FactionChoice choice : factionChoices) {
            if (choice.matches(normalizedFilter, effectiveDate)) {
                listModel.addElement(choice);
            }
        }
    }

    private void selectFactionChoices(JList<FactionChoice> factionList, DefaultListModel<FactionChoice> listModel,
          List<String> selectedCodes) {
        if ((selectedCodes == null) || selectedCodes.isEmpty()) {
            return;
        }

        Set<String> codes = new HashSet<>(selectedCodes);
        for (int index = 0; index < listModel.size(); index++) {
            if (codes.contains(listModel.get(index).code())) {
                factionList.addSelectionInterval(index, index);
            }
        }
    }

    private void transferOwnership() {
        Planet selectedPlanet = getSelectedPlanet();
        if ((selectedPlanet == null) || !campaign.isGM()) {
            return;
        }

        JTextField txtTransferDate = new JTextField(formatDate(getEventDateOrCampaignDate(txtEventDate.getText())), 12);
        JTextField txtTransferFactions = new JTextField(txtEventFactions.getText(), 18);
        JTextField txtTransferSource = new JTextField(defaultEditorSource(), 18);
        JTextField txtTransferVersion = new JTextField(txtEventVersion.getText(), 10);
        JTextField txtTransferMessage = new JTextField(resources.getString(
              "PlanetarySystemEditorDialog.eventEditor.transferOwnership.defaultMessage"), 24);

        JButton btnTransferPickFactions = new MMButton("btnPickTransferOwnershipFactions",
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickFactions"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickFactions.toolTipText"),
              evt -> chooseFactionCodes(txtTransferFactions, getEventDateOrCampaignDate(txtTransferDate.getText())));

        JPanel transferPanel = new JPanel(new GridBagLayout());
        addEventField(transferPanel, 0, 0, "PlanetarySystemEditorDialog.eventEditor.date",
              createDateFieldPane(txtTransferDate, "btnPickTransferOwnershipDate"));
        addEventField(transferPanel, 1, 0, "PlanetarySystemEditorDialog.eventEditor.factions",
              createInlineFieldWithButton(txtTransferFactions, btnTransferPickFactions));
        addEventField(transferPanel, 2, 0, "PlanetarySystemEditorDialog.eventEditor.source", txtTransferSource);
        addEventField(transferPanel, 3, 0, "PlanetarySystemEditorDialog.eventEditor.version", txtTransferVersion);
        addEventField(transferPanel, 4, 0, "PlanetarySystemEditorDialog.eventEditor.message", txtTransferMessage);

        int choice = JOptionPane.showConfirmDialog(this, transferPanel,
              resources.getString("PlanetarySystemEditorDialog.eventEditor.transferOwnership.title"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            LocalDate transferDate = parseEventDate(txtTransferDate.getText());
            List<String> factionCodes = parseFactionCodes(txtTransferFactions.getText());
            if ((factionCodes == null) || factionCodes.isEmpty()) {
                throw new IllegalArgumentException(resources.getString(
                      "PlanetarySystemEditorDialog.eventEditor.invalid.factionRequired"));
            }
            if (!confirmUnknownFactionCodes(factionCodes)) {
                return;
            }

            PlanetaryEvent event = getEventForDate(selectedPlanet, transferDate);
            if (event == null) {
                event = new PlanetaryEvent();
                event.date = transferDate;
            }
            event.faction = sourceableValue(factionCodes, txtTransferSource.getText(), txtTransferVersion.getText());
            String message = blankToNull(txtTransferMessage.getText());
            if (message != null) {
                event.message = message;
            }

            selectedPlanet.putEvent(event);
            markUnsavedChanges();
            refreshEditedSystem(transferDate);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private Component createInlineFieldWithButton(JTextField textField, JButton button) {
        JPanel panel = new JPanel(new BorderLayout(PADDING / 2, 0));
        panel.add(textField, BorderLayout.CENTER);
        panel.add(button, BorderLayout.EAST);
        return panel;
    }

    private PlanetaryEvent getEventForDate(Planet planet, LocalDate date) {
        if ((planet == null) || (date == null)) {
            return null;
        }
        for (PlanetaryEvent event : safeValue(planet::getEvents, List.<PlanetaryEvent>of())) {
            if ((event != null) && date.equals(event.date)) {
                return event;
            }
        }
        return null;
    }

    private boolean confirmUnknownFactionCodes(List<String> factionCodes) {
        List<String> unknownCodes = getUnknownFactionCodes(factionCodes);
        if (unknownCodes.isEmpty()) {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(this, MessageFormat.format(resources.getString(
                    "PlanetarySystemEditorDialog.eventEditor.unknownFactions"), formatFactionCodes(unknownCodes)),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.unknownFactions.title"),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private boolean confirmDuplicateEventReplacement(LocalDate duplicateDate) {
        int choice = JOptionPane.showConfirmDialog(this, MessageFormat.format(resources.getString(
                    "PlanetarySystemEditorDialog.eventEditor.duplicate.replace"), formatDate(duplicateDate)),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.duplicate.title"),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private List<String> getUnknownFactionCodes(List<String> factionCodes) {
        if ((factionCodes == null) || knownFactionCodes.isEmpty()) {
            return List.of();
        }
        return factionCodes.stream()
                     .filter(code -> !knownFactionCodes.contains(code))
                     .toList();
    }

    private List<String> getFactionCodes(PlanetaryEvent event) {
        if ((event == null) || (event.faction == null)) {
            return List.of();
        }
        return event.faction.getValue() == null ? List.of() : event.faction.getValue();
    }

    private LocalDate getEventDateOrCampaignDate(String eventDateText) {
        try {
            return parseEventDate(eventDateText);
        } catch (IllegalArgumentException ex) {
            return campaign.getLocalDate();
        }
    }

    private void markUnsavedChanges() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        String systemId = planetarySystemId(selectedSystem);
        if (systemId != null) {
            unsavedSystemIds.add(systemId);
        }
        updateEditStatus(selectedSystem);
        repaintSystemList();
        repaintPlanetList();
        updateButtonState(selectedSystem);
    }

    private void closeEditor() {
        if (confirmCloseEditor()) {
            dispose();
        }
    }

    private boolean confirmCloseEditor() {
        if (unsavedSystemIds.isEmpty()) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(this,
              resources.getString("PlanetarySystemEditorDialog.unsavedChanges"),
              resources.getString("PlanetarySystemEditorDialog.unsavedChanges.title"),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    @Override
    protected void cancelActionPerformed(final ActionEvent evt) {
        closeEditor();
    }

    @Override
    public void windowClosing(final WindowEvent evt) {
        closeEditor();
    }

    private boolean hasCampaignOverride(PlanetarySystem selectedSystem) {
        String systemId = planetarySystemId(selectedSystem);
        return (systemId != null) && overrideSystemIds.contains(systemId);
    }

    private boolean hasUnsavedChanges(PlanetarySystem selectedSystem) {
        String systemId = planetarySystemId(selectedSystem);
        return (systemId != null) && unsavedSystemIds.contains(systemId);
    }

    private boolean hasAnyUnsavedChanges() {
        return !getUnsavedSystems().isEmpty();
    }

    private List<PlanetarySystem> getUnsavedSystems() {
        return systems.stream()
                     .filter(this::hasUnsavedChanges)
                     .toList();
    }

    private void reviewAllSystemChanges() {
        List<PlanetarySystem> systemsToReview = getUnsavedSystems();
        if (systemsToReview.isEmpty()) {
            return;
        }

        JOptionPane.showMessageDialog(this, createChangeReviewPane(systemsToReview),
              resources.getString("PlanetarySystemEditorDialog.reviewChanges.title"),
              JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean confirmSaveUnsavedSystemChanges(List<PlanetarySystem> systemsToSave) {
        if (systemsToSave.isEmpty()) {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(this, createChangeReviewPane(systemsToSave),
              resources.getString("PlanetarySystemEditorDialog.reviewChanges.saveTitle"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    private Component createChangeReviewPane(List<PlanetarySystem> systemsToReview) {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING));
        String heading = systemsToReview.size() == 1
                               ? MessageFormat.format(resources.getString(
                                     "PlanetarySystemEditorDialog.reviewChanges.heading"),
                                     systemDisplayName(systemsToReview.get(0)))
                               : MessageFormat.format(resources.getString(
                                     "PlanetarySystemEditorDialog.reviewChanges.allHeading"),
                                     systemsToReview.size());
        panel.add(new JLabel(heading), BorderLayout.NORTH);

        JTextArea txtChanges = createReadOnlyTextArea("txtPlanetarySystemChangeReview");
        txtChanges.setRows(12);
        txtChanges.setColumns(72);
        txtChanges.setText(String.join(System.lineSeparator(), summarizeSystemChanges(systemsToReview)));
        txtChanges.setCaretPosition(0);
        panel.add(new FastJScrollPane(txtChanges), BorderLayout.CENTER);
        return panel;
    }

    private List<String> summarizeSystemChanges(List<PlanetarySystem> systemsToReview) {
        if (systemsToReview.isEmpty()) {
            return List.of(resources.getString("PlanetarySystemEditorDialog.reviewChanges.none"));
        }

        List<String> changes = new ArrayList<>();
        for (PlanetarySystem system : systemsToReview) {
            if (!changes.isEmpty()) {
                changes.add("");
            }
            changes.add(systemDisplayName(system));
            summarizeSystemChanges(system).forEach(change -> changes.add("  - " + change));
        }
        return changes;
    }

    private List<String> summarizeSystemChanges(PlanetarySystem system) {
        ensureBaselineSnapshot(system);
        PlanetarySystem baselineSystem = baselineSystems.get(planetarySystemId(system));
        if (baselineSystem == null) {
            return List.of(resources.getString("PlanetarySystemEditorDialog.reviewChanges.unavailable"));
        }

        List<String> changes = PlanetarySystemChangeSummary.summarize(baselineSystem, system,
              campaign.getLocalDate());
        return changes.isEmpty() ? List.of(resources.getString("PlanetarySystemEditorDialog.reviewChanges.none"))
                     : changes;
    }

    private void saveUnsavedSystems() {
        if (!campaign.isGM()) {
            return;
        }

        List<PlanetarySystem> systemsToSave = getUnsavedSystems();
        if (systemsToSave.isEmpty()) {
            return;
        }

        List<String> validationErrors = validateSystemsForSave(systemsToSave);
        if (!validationErrors.isEmpty()) {
            JOptionPane.showMessageDialog(this, createSaveValidationErrorPane(validationErrors),
                  resources.getString("PlanetarySystemEditorDialog.saveBlocked.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!confirmSaveUnsavedSystemChanges(systemsToSave)) {
            return;
        }

        try {
            for (PlanetarySystem system : systemsToSave) {
                campaign.putPlanetarySystemOverride(system);
                markSystemSaved(system);
            }
            updateSelectedSystem();
            repaintSystemList();
            repaintPlanetList();
            JOptionPane.showMessageDialog(this, MessageFormat.format(resources.getString(
                        "PlanetarySystemEditorDialog.saveComplete"), systemsToSave.size()),
                  resources.getString("PlanetarySystemEditorDialog.saveComplete.title"),
                  JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            LOGGER.error(ex, "Failed to save planetary system override");
            updateSelectedSystem();
            repaintSystemList();
            repaintPlanetList();
            JOptionPane.showMessageDialog(this, MessageFormat.format(resources.getString(
                        "PlanetarySystemEditorDialog.saveFailed"), ex.getMessage()),
                  resources.getString("PlanetarySystemEditorDialog.saveFailed.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markSystemSaved(PlanetarySystem system) {
        String systemId = planetarySystemId(system);
        if (systemId != null) {
            unsavedSystemIds.remove(systemId);
            overrideSystemIds.add(systemId);
        }
        refreshBaselineSnapshot(system);
    }

    private List<String> validateSystemsForSave(List<PlanetarySystem> systemsToSave) {
        List<String> validationErrors = new ArrayList<>();
        PlanetarySystem selectedSystem = getSelectedSystem();
        for (PlanetarySystem system : systemsToSave) {
            ValidationResult result = validator.validate(system, sourceName(system));
            if (Objects.equals(planetarySystemId(system), planetarySystemId(selectedSystem))) {
                setValidationText(result);
            }
            if (result.hasErrors()) {
                validationErrors.add(MessageFormat.format(resources.getString(
                      "PlanetarySystemEditorDialog.saveBlocked.system"), systemDisplayName(system),
                      result.getErrorCount()));
                result.getErrors().forEach(error -> validationErrors.add("  - " + error));
            }
        }
        return validationErrors;
    }

    private Component createSaveValidationErrorPane(List<String> validationErrors) {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING));
        panel.add(new JLabel(MessageFormat.format(resources.getString(
              "PlanetarySystemEditorDialog.saveBlocked"), countValidationErrors(validationErrors))),
              BorderLayout.NORTH);

        JTextArea txtErrors = createReadOnlyTextArea("txtPlanetarySystemSaveValidationErrors");
        txtErrors.setRows(12);
        txtErrors.setColumns(72);
        txtErrors.setText(String.join(System.lineSeparator(), validationErrors));
        txtErrors.setCaretPosition(0);
        panel.add(new FastJScrollPane(txtErrors), BorderLayout.CENTER);
        return panel;
    }

    private static long countValidationErrors(List<String> validationErrors) {
        return validationErrors.stream()
                     .filter(error -> error.startsWith("  - "))
                     .count();
    }

    private void revertSelectedSystemChanges() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        if ((selectedSystem == null) || !campaign.isGM() || !hasUnsavedChanges(selectedSystem)) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this, MessageFormat.format(resources.getString(
                    "PlanetarySystemEditorDialog.revertChangesConfirm"), systemDisplayName(selectedSystem)),
              resources.getString("PlanetarySystemEditorDialog.revertChangesConfirm.title"), JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            PlanetarySystem replacement = restoreFromBaseline(selectedSystem);
            if (replacement == null) {
                JOptionPane.showMessageDialog(this, resources.getString(
                            "PlanetarySystemEditorDialog.revertChangesUnavailable"),
                      resources.getString("PlanetarySystemEditorDialog.revertChangesFailed.title"),
                      JOptionPane.WARNING_MESSAGE);
                return;
            }

            unsavedSystemIds.remove(planetarySystemId(selectedSystem));
            updateSelectedSystem();
            repaintSystemList();
            repaintPlanetList();
            updateButtonState(getSelectedSystem());
        } catch (IOException ex) {
            LOGGER.error(ex, "Failed to revert planetary system changes");
            JOptionPane.showMessageDialog(this, MessageFormat.format(resources.getString(
                        "PlanetarySystemEditorDialog.revertChangesFailed"), ex.getMessage()),
                  resources.getString("PlanetarySystemEditorDialog.revertChangesFailed.title"),
                  JOptionPane.ERROR_MESSAGE);
        }
    }

    private PlanetarySystem restoreFromBaseline(PlanetarySystem currentSystem) throws IOException {
        String systemId = planetarySystemId(currentSystem);
        if (systemId == null) {
            return null;
        }
        PlanetarySystem baseline = baselineSystems.get(systemId);
        if (baseline == null) {
            return null;
        }

        PlanetarySystem replacement = PlanetarySystemYamlIO.copy(baseline);
        replaceSystemInEditor(systemId, replacement);
        workingSystemIds.add(systemId);
        return replacement;
    }

    private void revertSelectedPlanetChanges() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        Planet selectedPlanet = getSelectedPlanet();
        if ((selectedSystem == null) || (selectedPlanet == null) || !campaign.isGM()
                  || !hasUnsavedPlanetChanges(selectedPlanet)) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this, MessageFormat.format(resources.getString(
                    "PlanetarySystemEditorDialog.revertPlanetConfirm"),
                    safeValue(() -> selectedPlanet.getPrintableName(campaign.getLocalDate()))),
              resources.getString("PlanetarySystemEditorDialog.revertPlanetConfirm.title"), JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        ensureBaselineSnapshot(selectedSystem);
        PlanetarySystem baseline = baselineSystems.get(planetarySystemId(selectedSystem));
        if (baseline == null) {
            JOptionPane.showMessageDialog(this, resources.getString(
                        "PlanetarySystemEditorDialog.revertChangesUnavailable"),
                  resources.getString("PlanetarySystemEditorDialog.revertChangesFailed.title"),
                  JOptionPane.WARNING_MESSAGE);
            return;
        }

        Planet baselinePlanet = findMatchingPlanet(baseline, selectedPlanet);
        if (baselinePlanet == null) {
            JOptionPane.showMessageDialog(this, resources.getString(
                        "PlanetarySystemEditorDialog.revertPlanetUnavailable"),
                  resources.getString("PlanetarySystemEditorDialog.revertChangesFailed.title"),
                  JOptionPane.WARNING_MESSAGE);
            return;
        }

        selectedPlanet.replaceEvents(baselinePlanet.getEvents() == null ? List.of() : baselinePlanet.getEvents());
        copyPlanetStaticFields(baselinePlanet, selectedPlanet);
        if (!hasUnsavedSystemPlanetChanges(selectedSystem)) {
            unsavedSystemIds.remove(planetarySystemId(selectedSystem));
        }
        refreshEditedSystem(null);
    }

    private void copyPlanetStaticFields(Planet source, Planet target) {
        if ((source == null) || (target == null)) {
            return;
        }
        LocalDate when = campaign.getLocalDate();
        target.setSourcedName(source.getSourcedName(when));
        target.setSourcedPlanetType(source.getSourcedPlanetType());
        target.setSourcedGravity(source.getSourcedGravity());
        target.setSourcedDiameter(source.getSourcedDiameter());
        target.setSourcedDayLength(source.getSourcedDayLength(when));
        target.setSourcedYearLength(source.getSourcedYearLength());
        target.setSourcedTemperature(source.getSourcedTemperature(when));
        target.setSourcedPressure(source.getSourcedPressure(when));
        target.setSourcedAtmosphere(source.getSourcedAtmosphere(when));
        target.setSourcedComposition(source.getSourcedComposition(when));
        target.setSourcedPercentWater(source.getSourcedPercentWater(when));
        target.setSourcedLifeForm(source.getSourcedLifeForm(when));
        target.setSourcedSmallMoons(source.getSourcedSmallMoons());
        target.setSourcedRing(source.getSourcedRing());
        target.setDescription(source.getDescription());
    }

    private boolean hasUnsavedSystemPlanetChanges(PlanetarySystem system) {
        if ((system == null) || !hasUnsavedChanges(system)) {
            return false;
        }
        ensureBaselineSnapshot(system);
        PlanetarySystem baseline = baselineSystems.get(planetarySystemId(system));
        if (baseline == null) {
            return false;
        }
        return !PlanetarySystemChangeSummary.summarize(baseline, system, campaign.getLocalDate()).isEmpty();
    }

    private static Planet findMatchingPlanet(PlanetarySystem system, Planet target) {
        if ((system == null) || (target == null) || (system.getPlanets() == null)) {
            return null;
        }
        for (Planet planet : system.getPlanets()) {
            if ((planet != null) && Objects.equals(planet.getId(), target.getId())) {
                return planet;
            }
        }
        Integer targetPosition = safeValue(target::getSystemPosition, null);
        if (targetPosition != null) {
            for (Planet planet : system.getPlanets()) {
                if ((planet != null) && Objects.equals(safeValue(planet::getSystemPosition, null), targetPosition)) {
                    return planet;
                }
            }
        }
        return null;
    }

    private void deleteSelectedSystemOverride() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        if ((selectedSystem == null) || !campaign.isGM()) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this, MessageFormat.format(resources.getString(
                    "PlanetarySystemEditorDialog.deleteOverrideConfirm"), systemDisplayName(selectedSystem)),
              resources.getString("PlanetarySystemEditorDialog.deleteOverrideConfirm.title"),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String systemId = planetarySystemId(selectedSystem);
        boolean deleted = campaign.removePlanetarySystemOverride(systemId);
        overrideSystemIds.remove(systemId);
        unsavedSystemIds.remove(systemId);

        try {
            PlanetarySystem canonicalSystem = campaign.getSystemById(systemId);
            if (canonicalSystem != null) {
                PlanetarySystem workingCopy = PlanetarySystemYamlIO.copy(canonicalSystem);
                replaceSystemInEditor(systemId, workingCopy);
                refreshBaselineSnapshot(workingCopy);
            }
        } catch (IOException ex) {
            LOGGER.error(ex, "Failed to restore canonical planetary system after deleting override");
        }

        PlanetarySystem updatedSystem = getSelectedSystem();
        updateButtonState(updatedSystem);
        updateEditStatus(updatedSystem);
        updateSelectedSystem();
        repaintSystemList();
        String key = deleted ? "PlanetarySystemEditorDialog.deleteOverrideComplete"
                           : "PlanetarySystemEditorDialog.deleteOverrideNothing";
        JOptionPane.showMessageDialog(this, resources.getString(key),
              resources.getString("PlanetarySystemEditorDialog.deleteOverrideComplete.title"),
              JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildSystemDetails(PlanetarySystem system, LocalDate currentDate) {
        StringBuilder details = new StringBuilder();
        appendLine(details, resources.getString("PlanetarySystemEditorDialog.field.id"), system.getId());
        appendLine(details, resources.getString("PlanetarySystemEditorDialog.field.name"),
              safeValue(() -> system.getName(currentDate)));
        appendLine(details, resources.getString("PlanetarySystemEditorDialog.field.coordinates"),
              formatCoordinates(system));
        appendLine(details, resources.getString("PlanetarySystemEditorDialog.field.star"),
              safeValue(system::getStar));
        appendLine(details, resources.getString("PlanetarySystemEditorDialog.field.primarySlot"),
              safeValue(system::getPrimaryPlanetPosition));
        appendLine(details, resources.getString("PlanetarySystemEditorDialog.field.factions"),
              safeValue(() -> formatFactions(system.getFactions(currentDate))));
        appendLine(details, resources.getString("PlanetarySystemEditorDialog.field.systemEvents"),
              String.valueOf(count(safeValue(system::getEvents, null))));

        List<Planet> planets = planetsInDisplayOrder(system);
        appendLine(details, resources.getString("PlanetarySystemEditorDialog.field.planets"),
              String.valueOf(planets.size()));
        details.append(System.lineSeparator()).append(resources.getString(
              "PlanetarySystemEditorDialog.planetsHeader")).append(System.lineSeparator());

        for (Planet planet : planets) {
            details.append("  ").append(formatPlanetSummary(planet, currentDate)).append(System.lineSeparator());
        }

        return details.toString();
    }

    private List<FactionChoice> loadFactionChoices() {
        Collection<Faction> factions = Factions.getInstance().getFactions();
        if (factions.isEmpty()) {
            factions = Factions.load(false).getFactions();
        }

        int year = campaign.getLocalDate().getYear();
        return factions.stream()
                     .filter(Objects::nonNull)
                     .filter(faction -> blankToNull(faction.getShortName()) != null)
                     .filter(faction -> !faction.isAggregate())
                   .map(faction -> new FactionChoice(faction.getShortName(), factionDisplayName(faction, year),
                       faction.getStartYear(), faction.getEndYear()))
                     .sorted(Comparator.comparing(choice -> choice.displayName().toLowerCase(Locale.ROOT)))
                     .toList();
    }

    private static String factionDisplayName(Faction faction, int year) {
        String displayName = blankToNull(faction.getFullName(year));
        return displayName == null ? faction.getShortName() : displayName;
    }

    private List<Planet> planetsInDisplayOrder(PlanetarySystem system) {
        Collection<Planet> planets = safeValue(system::getPlanets, List.of());
        return planets.stream()
                     .filter(Objects::nonNull)
                     .sorted(Comparator.comparingInt(this::planetPosition))
                     .toList();
    }

    private int planetPosition(Planet planet) {
        Integer position = safeValue(planet::getSystemPosition, Integer.MAX_VALUE);
        return position == null ? Integer.MAX_VALUE : position;
    }

    private String formatPlanetSummary(Planet planet, LocalDate currentDate) {
        String position = safeValue(planet::getDisplayableSystemPosition);
        String name = safeValue(() -> planet.getPrintableName(currentDate));
        String type = safeValue(planet::getPlanetType);
        String factions = safeValue(() -> formatFactions(planet.getFactions(currentDate)));
        String population = safeValue(() -> formatPopulation(planet.getPopulation(currentDate)));
        String hpg = safeValue(() -> planet.getHPG(currentDate));

        return MessageFormat.format(resources.getString("PlanetarySystemEditorDialog.planetSummary"),
              position, name, type, factions, population, hpg);
    }

    private String formatPlanetListName(Planet planet) {
        return MessageFormat.format("{0}: {1}", safeValue(planet::getDisplayableSystemPosition),
              safeValue(() -> planet.getPrintableName(campaign.getLocalDate())));
    }

    private String planetListDisplayName(Planet planet) {
        String displayName = formatPlanetListName(planet);
        if (!hasUnsavedPlanetChanges(planet)) {
            return displayName;
        }
        return MessageFormat.format("{0} [{1}]", displayName, resources.getString(
              "PlanetarySystemEditorDialog.badge.unsavedShort"));
    }

    private boolean hasUnsavedPlanetChanges(Planet planet) {
        PlanetarySystem selectedSystem = getSelectedSystem();
        if ((selectedSystem == null) || !hasUnsavedChanges(selectedSystem)) {
            return false;
        }

        ensureBaselineSnapshot(selectedSystem);
        PlanetarySystem baselineSystem = baselineSystems.get(planetarySystemId(selectedSystem));
        return PlanetarySystemChangeSummary.hasChangesForPlanet(baselineSystem, selectedSystem, planet,
              campaign.getLocalDate());
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : EVENT_DATE_FORMATTER.format(date);
    }

    private static String formatSourceableValue(SourceableValue<?> sourceableValue) {
        if ((sourceableValue == null) || (sourceableValue.getValue() == null)) {
            return "";
        }
        return String.valueOf(sourceableValue.getValue());
    }

    private static String formatSourceableList(SourceableValue<List<String>> sourceableValue) {
        if ((sourceableValue == null) || (sourceableValue.getValue() == null)) {
            return "";
        }
        return String.join(", ", sourceableValue.getValue());
    }

    private static String formatSourceableHpg(SourceableValue<HPGRating> sourceableValue) {
        if ((sourceableValue == null) || (sourceableValue.getValue() == null)) {
            return "";
        }
        return sourceableValue.getValue().name();
    }

    private String defaultEditorSource() {
        String campaignName = blankToNull(campaign.getName());
        return campaignName == null ? "MekHQ GM" : "MekHQ GM: " + campaignName;
    }

    private static String firstSource(PlanetaryEvent event) {
        return firstMetadataValue(true, event.faction, event.population, event.hpg, event.socioIndustrial);
    }

    private static String firstVersion(PlanetaryEvent event) {
        return firstMetadataValue(false, event.faction, event.population, event.hpg, event.socioIndustrial);
    }

    private static String firstMetadataValue(boolean source, SourceableValue<?>... values) {
        for (SourceableValue<?> value : values) {
            if (value == null) {
                continue;
            }
            String metadataValue = source ? value.getSource() : value.getVersion();
            if (blankToNull(metadataValue) != null) {
                return metadataValue;
            }
        }
        return "";
    }

    private String systemDisplayName(PlanetarySystem system) {
        String name = safeValue(() -> system.getName(campaign.getLocalDate()));
        if (Objects.equals(name, system.getId())) {
            return name;
        }
        return MessageFormat.format("{0} ({1})", name, system.getId());
    }

    private String systemListDisplayName(PlanetarySystem system) {
        List<String> badges = new ArrayList<>();
        if (hasUnsavedChanges(system)) {
            badges.add(resources.getString("PlanetarySystemEditorDialog.badge.unsaved"));
        }
        if (hasCampaignOverride(system)) {
            badges.add(resources.getString("PlanetarySystemEditorDialog.badge.campaignOverride"));
        }
        if (badges.isEmpty()) {
            return systemDisplayName(system);
        }
        return MessageFormat.format("{0} [{1}]", systemDisplayName(system), String.join(", ", badges));
    }

    private static String planetarySystemId(PlanetarySystem system) {
        return system == null ? null : blankToNull(system.getId());
    }

    private String sourceName(PlanetarySystem system) {
        String systemId = system.getId();
        return ((systemId == null) || systemId.isBlank()) ? "edited planetary system" : systemId + ".yml";
    }

    private String defaultSourceName() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        return selectedSystem == null ? "" : sourceName(selectedSystem);
    }

    private void onPropertiesChanged() {
        PlanetarySystem selectedSystem = getSelectedSystem();
        if (selectedSystem != null) {
            setValidationText(validator.validate(selectedSystem, sourceName(selectedSystem)));
            txtSystemDetails.setText(buildSystemDetails(selectedSystem, campaign.getLocalDate()));
            txtSystemDetails.setCaretPosition(0);
        }
        markUnsavedChanges();
    }

    private String formatCoordinates(PlanetarySystem system) {
        return MessageFormat.format(resources.getString("PlanetarySystemEditorDialog.coordinates"),
              safeValue(() -> formatDecimal(system.getX())), safeValue(() -> formatDecimal(system.getY())));
    }

    private static String formatDecimal(Double value) {
        return value == null ? "N/A" : String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatPopulation(Long population) {
        return population == null ? "N/A" : String.format(Locale.ROOT, "%,d", population);
    }

    private static String formatFactions(List<String> factions) {
        if ((factions == null) || factions.isEmpty()) {
            return "N/A";
        }
        return factions.stream()
                     .filter(Objects::nonNull)
                     .distinct()
                     .sorted()
                     .collect(Collectors.joining(", "));
    }

    private static String formatFactionCodes(List<String> factions) {
        if ((factions == null) || factions.isEmpty()) {
            return "";
        }
        return factions.stream()
                     .filter(Objects::nonNull)
                     .distinct()
                     .collect(Collectors.joining(", "));
    }

    private static String blankToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmedText = text.trim();
        return trimmedText.isEmpty() ? null : trimmedText;
    }

    private static String nullToBlank(String text) {
        return text == null ? "" : text;
    }

    private static int count(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private static void appendLine(StringBuilder builder, String label, String value) {
        builder.append(label).append(": ").append(value).append(System.lineSeparator());
    }

    private static String safeValue(Supplier<?> supplier) {
        try {
            Object value = supplier.get();
            return value == null ? "N/A" : String.valueOf(value);
        } catch (RuntimeException ex) {
            return "N/A";
        }
    }

    private static <T> T safeValue(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private void installKeyboardShortcuts(JComponent root) {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = root.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask), "savePlanetaryOverrides");
        actionMap.put("savePlanetaryOverrides", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if ((btnSave != null) && btnSave.isEnabled()) {
                    saveUnsavedSystems();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask), "focusPlanetarySearch");
        actionMap.put("focusPlanetarySearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (txtSearch != null) {
                    txtSearch.requestFocusInWindow();
                    txtSearch.selectAll();
                }
            }
        });

        if (tblEvents != null) {
            InputMap tableInput = tblEvents.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap tableAction = tblEvents.getActionMap();
            tableInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removePlanetaryEvent");
            tableAction.put("removePlanetaryEvent", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if ((btnRemoveEvent != null) && btnRemoveEvent.isEnabled()) {
                        removeSelectedEvent();
                    }
                }
            });
            tableInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, menuMask), "duplicatePlanetaryEvent");
            tableAction.put("duplicatePlanetaryEvent", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if ((btnDuplicateEvent != null) && btnDuplicateEvent.isEnabled()) {
                        duplicateSelectedEvent();
                    }
                }
            });
        }
    }

    private String systemListTooltip(PlanetarySystem system) {
        if (system == null) {
            return null;
        }

        List<String> lines = new ArrayList<>();
        lines.add(systemDisplayName(system));
        if (hasCampaignOverride(system)) {
            lines.add(resources.getString("PlanetarySystemEditorDialog.tooltip.override"));
        }
        if (hasUnsavedChanges(system)) {
            lines.add(resources.getString("PlanetarySystemEditorDialog.tooltip.unsavedHeading"));
            for (String change : summarizeSystemChanges(system)) {
                lines.add("  - " + change);
            }
        }
        return formatHtmlTooltip(lines);
    }

    private String planetListTooltip(Planet planet) {
        if (planet == null) {
            return null;
        }

        List<String> lines = new ArrayList<>();
        lines.add(formatPlanetListName(planet));
        if (hasUnsavedPlanetChanges(planet)) {
            lines.add(resources.getString("PlanetarySystemEditorDialog.tooltip.unsavedHeading"));
            for (String change : summarizePlanetChanges(planet)) {
                lines.add("  - " + change);
            }
        }
        return formatHtmlTooltip(lines);
    }

    private List<String> summarizePlanetChanges(Planet planet) {
        PlanetarySystem selectedSystem = getSelectedSystem();
        if ((selectedSystem == null) || (planet == null)) {
            return List.of();
        }

        ensureBaselineSnapshot(selectedSystem);
        PlanetarySystem baselineSystem = baselineSystems.get(planetarySystemId(selectedSystem));
        if (baselineSystem == null) {
            return List.of();
        }
        return PlanetarySystemChangeSummary.summarizeForPlanet(baselineSystem, selectedSystem, planet,
              campaign.getLocalDate());
    }

    private static String formatHtmlTooltip(List<String> lines) {
        if ((lines == null) || lines.isEmpty()) {
            return null;
        }
        StringBuilder html = new StringBuilder("<html>");
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                html.append("<br>");
            }
            html.append(escapeHtml(lines.get(index)));
        }
        html.append("</html>");
        return html.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;");
    }

    private enum SystemSortOption {
        NAME("PlanetarySystemEditorDialog.sort.name"),
        X("PlanetarySystemEditorDialog.sort.x"),
        Y("PlanetarySystemEditorDialog.sort.y");

        private final String labelKey;

        SystemSortOption(String labelKey) {
            this.labelKey = labelKey;
        }

        String labelKey() {
            return labelKey;
        }

        Comparator<PlanetarySystem> comparator(PlanetarySystemEditorDialog dialog) {
            return switch (this) {
                case NAME -> Comparator.comparing(system -> dialog.systemDisplayName(system).toLowerCase(Locale.ROOT));
                case X -> Comparator.comparingDouble((PlanetarySystem system) ->
                                                            system.getX() == null ? Double.MAX_VALUE : system.getX())
                                                          .thenComparing(system -> dialog.systemDisplayName(system)
                                                                                       .toLowerCase(Locale.ROOT));
                case Y -> Comparator.comparingDouble((PlanetarySystem system) ->
                                                            system.getY() == null ? Double.MAX_VALUE : system.getY())
                                                          .thenComparing(system -> dialog.systemDisplayName(system)
                                                                                       .toLowerCase(Locale.ROOT));
            };
        }
    }

    private record FactionChoice(String code, String displayName, int startYear, int endYear) {
        boolean matches(String filter, LocalDate effectiveDate) {
            if ((effectiveDate.getYear() < startYear) || (effectiveDate.getYear() > endYear)) {
                return false;
            }
            return filter.isBlank()
                         || code.toLowerCase(Locale.ROOT).contains(filter)
                         || displayName.toLowerCase(Locale.ROOT).contains(filter);
        }

        @Override
        public String toString() {
            return code + " - " + displayName;
        }
    }

    private static final class PopulationDocumentFilter extends DocumentFilter {
        private final JTextField textField;

        private PopulationDocumentFilter(JTextField textField) {
            this.textField = textField;
        }

        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
              throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
              throws BadLocationException {
            StringBuilder proposedText = getCurrentText(bypass);
            proposedText.replace(offset, offset + length, text == null ? "" : text);
            replaceText(bypass, formatPopulationInput(proposedText.toString()), attributes);
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            StringBuilder proposedText = getCurrentText(bypass);
            proposedText.delete(offset, offset + length);
            replaceText(bypass, formatPopulationInput(proposedText.toString()), null);
        }

        private StringBuilder getCurrentText(FilterBypass bypass) throws BadLocationException {
            return new StringBuilder(bypass.getDocument().getText(0, bypass.getDocument().getLength()));
        }

        private void replaceText(FilterBypass bypass, String text, AttributeSet attributes)
              throws BadLocationException {
            bypass.replace(0, bypass.getDocument().getLength(), text, attributes);
            SwingUtilities.invokeLater(() -> textField.setCaretPosition(textField.getText().length()));
        }
    }

    private static final class SocioIndustrialDocumentFilter extends DocumentFilter {
        private final JTextField textField;

        private SocioIndustrialDocumentFilter(JTextField textField) {
            this.textField = textField;
        }

        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
              throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
              throws BadLocationException {
            StringBuilder proposedText = getCurrentText(bypass);
            proposedText.replace(offset, offset + length, text == null ? "" : text);
            replaceText(bypass, formatSocioIndustrialInput(proposedText.toString()), attributes);
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            StringBuilder proposedText = getCurrentText(bypass);
            proposedText.delete(offset, offset + length);
            replaceText(bypass, formatSocioIndustrialInput(proposedText.toString()), null);
        }

        private StringBuilder getCurrentText(FilterBypass bypass) throws BadLocationException {
            return new StringBuilder(bypass.getDocument().getText(0, bypass.getDocument().getLength()));
        }

        private void replaceText(FilterBypass bypass, String text, AttributeSet attributes)
              throws BadLocationException {
            bypass.replace(0, bypass.getDocument().getLength(), text, attributes);
            SwingUtilities.invokeLater(() -> textField.setCaretPosition(textField.getText().length()));
        }
    }

    private final class PlanetEventTableModel extends AbstractTableModel {
        private static final int COL_DATE = 0;
        private static final int COL_FACTIONS = 1;
        private static final int COL_POPULATION = 2;
        private static final int COL_HPG = 3;
        private static final int COL_SOCIO_INDUSTRIAL = 4;
        private static final int COL_MESSAGE = 5;
        private static final int COLUMN_COUNT = 6;

        private Planet planet;
        private List<PlanetaryEvent> events = new ArrayList<>();

        void setPlanet(Planet planet) {
            this.planet = planet;
            reload();
        }

        private void reload() {
            events = new ArrayList<>();
            if (planet != null) {
                events.addAll(safeValue(planet::getEvents, List.of()));
                events.removeIf(event -> (event == null) || (event.date == null));
                events.sort(Comparator.comparing(event -> event.date));
            }
            fireTableDataChanged();
        }

        PlanetaryEvent getEventAt(int row) {
            if ((row < 0) || (row >= events.size())) {
                return null;
            }
            return events.get(row);
        }

        int findEventRow(LocalDate date) {
            if (date == null) {
                return -1;
            }
            for (int row = 0; row < events.size(); row++) {
                if (date.equals(events.get(row).date)) {
                    return row;
                }
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return events.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_DATE -> resources.getString("PlanetarySystemEditorDialog.eventEditor.date");
                case COL_FACTIONS -> resources.getString("PlanetarySystemEditorDialog.eventEditor.factions");
                case COL_POPULATION -> resources.getString("PlanetarySystemEditorDialog.eventEditor.population");
                case COL_HPG -> resources.getString("PlanetarySystemEditorDialog.eventEditor.hpg");
                case COL_SOCIO_INDUSTRIAL -> resources.getString(
                      "PlanetarySystemEditorDialog.eventEditor.socioIndustrial");
                case COL_MESSAGE -> resources.getString("PlanetarySystemEditorDialog.eventEditor.message");
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            PlanetaryEvent event = getEventAt(row);
            if (event == null) {
                return "";
            }
            return switch (column) {
                case COL_DATE -> formatDate(event.date);
                case COL_FACTIONS -> formatSourceableList(event.faction);
                case COL_POPULATION -> formatSourceableValue(event.population);
                case COL_HPG -> formatSourceableHpg(event.hpg);
                case COL_SOCIO_INDUSTRIAL -> formatSourceableValue(event.socioIndustrial);
                case COL_MESSAGE -> nullToBlank(event.message);
                default -> "";
            };
        }
    }

    private final class SystemEventTableModel extends AbstractTableModel {
        private static final int COL_DATE = 0;
        private static final int COL_NADIR = 1;
        private static final int COL_ZENITH = 2;
        private static final int COLUMN_COUNT = 3;

        private PlanetarySystem system;
        private List<PlanetarySystemEvent> events = new ArrayList<>();

        void setSystem(PlanetarySystem system) {
            this.system = system;
            reload();
        }

        private void reload() {
            events = new ArrayList<>();
            if (system != null) {
                List<PlanetarySystemEvent> source = system.getEvents();
                if (source != null) {
                    for (PlanetarySystemEvent event : source) {
                        if ((event != null) && (event.date != null)) {
                            events.add(event);
                        }
                    }
                    events.sort(Comparator.comparing(event -> event.date));
                }
            }
            fireTableDataChanged();
        }

        PlanetarySystemEvent getEventAt(int row) {
            if ((row < 0) || (row >= events.size())) {
                return null;
            }
            return events.get(row);
        }

        int findEventRow(LocalDate date) {
            if (date == null) {
                return -1;
            }
            for (int row = 0; row < events.size(); row++) {
                if (date.equals(events.get(row).date)) {
                    return row;
                }
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return events.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_DATE -> resources.getString("PlanetarySystemEditorDialog.eventEditor.date");
                case COL_NADIR -> resources.getString("PlanetarySystemEditorDialog.systemEvents.nadirCharge");
                case COL_ZENITH -> resources.getString("PlanetarySystemEditorDialog.systemEvents.zenithCharge");
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case COL_NADIR, COL_ZENITH -> Boolean.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return (system != null) && campaign.isGM();
        }

        @Override
        public Object getValueAt(int row, int column) {
            PlanetarySystemEvent event = getEventAt(row);
            if (event == null) {
                return (column == COL_NADIR || column == COL_ZENITH) ? Boolean.FALSE : "";
            }
            return switch (column) {
                case COL_DATE -> formatDate(event.date);
                case COL_NADIR -> sourceableBoolean(event.nadirCharge);
                case COL_ZENITH -> sourceableBoolean(event.zenithCharge);
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            PlanetarySystemEvent event = getEventAt(row);
            if ((system == null) || (event == null)) {
                return;
            }
            switch (column) {
                case COL_DATE -> updateEventDate(event, value);
                case COL_NADIR -> updateBooleanField(event, value, true);
                case COL_ZENITH -> updateBooleanField(event, value, false);
                default -> {
                    // no-op
                }
            }
        }

        private void updateEventDate(PlanetarySystemEvent event, Object value) {
            String text = value == null ? "" : value.toString();
            try {
                LocalDate newDate = parseEventDate(text);
                if (newDate.equals(event.date)) {
                    return;
                }
                if (findEventRow(newDate) >= 0) {
                    JOptionPane.showMessageDialog(PlanetarySystemEditorDialog.this,
                          MessageFormat.format(resources.getString(
                                "PlanetarySystemEditorDialog.systemEvents.add.duplicate"), formatDate(newDate)),
                          resources.getString("PlanetarySystemEditorDialog.eventEditor.invalid.title"),
                          JOptionPane.ERROR_MESSAGE);
                    return;
                }
                LocalDate oldDate = event.date;
                event.date = newDate;
                system.removeEvent(oldDate);
                system.putEvent(event);
                markUnsavedChanges();
                refreshSystemEvents(newDate);
                onPropertiesChanged();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(PlanetarySystemEditorDialog.this, ex.getMessage(),
                      resources.getString("PlanetarySystemEditorDialog.eventEditor.invalid.title"),
                      JOptionPane.ERROR_MESSAGE);
            }
        }

        private void updateBooleanField(PlanetarySystemEvent event, Object value, boolean nadir) {
            boolean newValue = Boolean.TRUE.equals(value);
            SourceableValue<Boolean> existing = nadir ? event.nadirCharge : event.zenithCharge;
            String source = (existing == null) ? null : existing.getSource();
            String version = (existing == null) ? null : existing.getVersion();
            SourceableValue<Boolean> wrapped = SourceableValue.of(source, version, newValue);
            if (nadir) {
                event.nadirCharge = wrapped;
            } else {
                event.zenithCharge = wrapped;
            }
            markUnsavedChanges();
            fireTableRowsUpdated(events.indexOf(event), events.indexOf(event));
            onPropertiesChanged();
        }

        private Boolean sourceableBoolean(SourceableValue<Boolean> value) {
            return ((value == null) || (value.getValue() == null)) ? Boolean.FALSE : value.getValue();
        }
    }
}
