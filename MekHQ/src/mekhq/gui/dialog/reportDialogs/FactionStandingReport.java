package mekhq.gui.dialog.reportDialogs;

import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import megamek.client.ui.swing.util.UIUtil;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MekHQ;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import mekhq.gui.dialog.GlossaryDialog;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.RoundedLineBorder;
import mekhq.gui.utilities.WrapLayout;

/**
 * Displays a dialog window that visualizes a report on faction standings for the current campaign year. Shows
 * individual faction panels with images, standing levels, fame sliders, and interactive details on standing effects.
 *
 * <p>This dialog provides a convenient summary as well as documentation and GM tool links related to faction
 * standings.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingReport extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingReport.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    /**
     * The amount of padding, scaled for the user interface, used to ensure consistent spacing within components of the
     * Faction Standing Report dialog.
     */
    private static final int PADDING = UIUtil.scaleForGUI(10);
    /**
     * A label name constant for the Effects Panel in the Faction Standing Report dialog.
     *
     * <p>This value is used to reference the specific UI component and allow us to dynamically update the faction
     * standing effects display.</p>
     */
    private static final String EFFECTS_PANEL_LABEL_NAME = "lblFactionStandingEffects";

    private final JFrame frame;
    private final int gameYear;
    private final FactionStandings factionStandings;
    private final Factions factions;
    private final boolean isGM;

    private final List<String> innerSphereFactions = new ArrayList<>();
    private final List<String> clanFactions = new ArrayList<>();
    private final List<String> peripheryFactions = new ArrayList<>();
    private final List<String> deadFactions = new ArrayList<>();

    /**
     * Constructs a new {@code FactionStandingReport} dialog.
     *
     * @param frame            the parent frame for this dialog
     * @param factionStandings the object containing faction standing values to report on
     * @param gameYear         the current campaign year
     * @param isGM             whether the player currently has GM Mode enabled
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingReport(final JFrame frame, final FactionStandings factionStandings, final int gameYear,
          final boolean isGM) {
        this.frame = frame;
        this.gameYear = gameYear;
        this.isGM = isGM;
        this.factionStandings = factionStandings;
        factions = Factions.getInstance();

        sortFactions();
        createReportPanel();
        initializeDialogParameters();
    }

    /**
     * Initializes dialog window parameters, such as title, size, modality, and visibility.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void initializeDialogParameters() {
        setTitle(getTextAt(RESOURCE_BUNDLE, "factionStandingReport.title"));
        setSize(1000, 1000);
        setLocationRelativeTo(frame);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        setModal(true);
        setVisible(true);
    }

    /**
     * Creates the main report panel. Contains the scrollable faction standings panel, effects panel, and buttons
     * panel.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createReportPanelForFactionGroup(List<String> factions) {
        if (factions.isEmpty()) {
            return new JPanel();
        }

        JPanel pnlFactionReport = new JPanel();
        pnlFactionReport.setName("factionReportPanel" + factions);
        pnlFactionReport.setLayout(new BoxLayout(pnlFactionReport, BoxLayout.Y_AXIS));

        JPanel groupPanel = new JPanel(new WrapLayout(WrapLayout.LEFT, PADDING, PADDING));
        groupPanel.setName("factionReportGroupPanel" + factions);
        for (String faction : factions) {
            JPanel factionPanel = createFactionPanel(faction);
            groupPanel.add(factionPanel);
        }
        JScrollPane groupScrollPane = new JScrollPaneWithSpeed(groupPanel,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        groupScrollPane.setName("factionReportGroupScrollPane" + factions);
        groupScrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());

        pnlFactionReport.add(groupScrollPane);

        return pnlFactionReport;
    }

    private void createReportPanel() {
        // Create the tabbed pane
        String innerSphereTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.innerSphere");
        String clanTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.clan");
        String peripheryTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.periphery");
        String deadTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.dead");

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setName("tabbedPane");
        tabbedPane.addTab(innerSphereTabTitle, createReportPanelForFactionGroup(innerSphereFactions));
        tabbedPane.addTab(clanTabTitle, createReportPanelForFactionGroup(clanFactions));
        tabbedPane.addTab(peripheryTabTitle, createReportPanelForFactionGroup(peripheryFactions));
        tabbedPane.addTab(deadTabTitle, createReportPanelForFactionGroup(deadFactions));
        setFontScaling(tabbedPane, true, 1.5);

        // If a tab only contains an empty Container, disable it. This will occur if the relevant faction list is empty.
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            boolean isEmpty = (component instanceof Container) && ((Container) component).getComponentCount() == 0;
            tabbedPane.setEnabledAt(i, !isEmpty);
        }

        // Create effects and buttons panels
        JPanel pnlEffects = createEffectsPanel();
        pnlEffects.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JPanel pnlButtons = createButtonsPanel();
        pnlButtons.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Main report panel with vertical stacking
        JPanel pnlReport = new JPanel();
        pnlReport.setLayout(new BoxLayout(pnlReport, BoxLayout.Y_AXIS));
        pnlReport.add(tabbedPane);
        pnlReport.add(pnlEffects);
        pnlReport.add(pnlButtons);

        setContentPane(pnlReport);
    }

    /**
     * Constructs the panel that shows standing effects or explanatory text.
     *
     * @return a configured {@link JPanel} containing the standing effects text area
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createEffectsPanel() {
        JPanel pnlEffects = new JPanel(new BorderLayout());
        pnlEffects.setName("pnlFactionStandingEffects");

        JTextArea lblStandingEffects = new JTextArea();
        lblStandingEffects.setName(EFFECTS_PANEL_LABEL_NAME);
        lblStandingEffects.setEditable(false);
        lblStandingEffects.setWrapStyleWord(true);
        lblStandingEffects.setLineWrap(true);
        lblStandingEffects.setOpaque(false);
        lblStandingEffects.setBorder(null);
        lblStandingEffects.setFocusable(false);
        lblStandingEffects.setText("");
        lblStandingEffects.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIUtil.scaleForGUI(30)));

        pnlEffects.add(lblStandingEffects, BorderLayout.CENTER);
        pnlEffects.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIUtil.scaleForGUI(30)));

        return pnlEffects;
    }

    /**
     * Constructs a panel with documentation and GM tools buttons.
     *
     * @return a configured {@link JPanel} containing dialog buttons
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createButtonsPanel() {
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlButtons.setName("pnlButtons");
        pnlButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIUtil.scaleForGUI(30)));

        JButton btnDocumentation = new JButton(getTextAt(RESOURCE_BUNDLE,
              "factionStandingReport.button.documentation"));
        btnDocumentation.setName("btnDocumentation");
        btnDocumentation.addActionListener(e -> new GlossaryDialog(this, "FACTION_STANDING"));
        btnDocumentation.setFocusable(false);
        btnDocumentation.setBorder(RoundedLineBorder.createRoundedLineBorder());
        pnlButtons.add(btnDocumentation);

        pnlButtons.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(50)));

        JButton btnGmTools = new JButton(getTextAt(RESOURCE_BUNDLE, "factionStandingReport.button.gmTools"));
        btnGmTools.setName("btnGmTools");
        btnGmTools.setFocusable(false);
        btnGmTools.setBorder(RoundedLineBorder.createRoundedLineBorder());
        btnGmTools.setEnabled(isGM);
        btnGmTools.addActionListener(e -> {
            // TODO GM Tools Dialog
        });
        pnlButtons.add(btnGmTools);

        return pnlButtons;
    }

    private void sortFactions() {
        Set<String> allFactionStandingsSet = factionStandings.getAllFactionStandings().keySet();
        List<String> sortedFactionStandings = new ArrayList<>(allFactionStandingsSet);
        for (String factionCode : factionStandings.getAllDynamicTemporaryFame().keySet()) {
            if (!allFactionStandingsSet.contains(factionCode)) {
                sortedFactionStandings.add(factionCode);
            }
        }
        Collections.sort(sortedFactionStandings);

        for (String factionCode : sortedFactionStandings) {
            Faction faction = factions.getFaction(factionCode);
            if (faction == null) {
                LOGGER.error(new NullPointerException(), "Failed to find faction with code: {}", factionCode);
                continue;
            }

            if (!faction.validIn(gameYear)) {
                deadFactions.add(factionCode);
            } else if (faction.isClan()) {
                clanFactions.add(factionCode);
            } else if (faction.isPeriphery()) {
                peripheryFactions.add(factionCode);
            } else {
                innerSphereFactions.add(factionCode);
            }
        }
    }

    /**
     * Constructs a panel describing the specified faction, including logo, description, and fame slider.
     *
     * @param factionCode the code of the faction to be displayed
     * @return a {@link JPanel} representing the faction's standing information
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createFactionPanel(final String factionCode) {
        final Faction faction = factions.getFaction(factionCode);
        if (faction == null) {
            LOGGER.error(new NullPointerException(),
                  "Failed to find faction with code: {} - skipping faction panel",
                  factionCode);
            JPanel lblEmptyPanelFromNullFaction = new JPanel();
            lblEmptyPanelFromNullFaction.setName("lblEmptyPanelFromNullFaction" + factionCode);
            return lblEmptyPanelFromNullFaction;
        }

        final double factionFame = factionStandings.getFameForFaction(factionCode, true);
        final FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(factionFame);

        // Parent panel
        JPanel pnlFactionStanding = new JPanel();
        pnlFactionStanding.setName("pnlFactionStanding" + factionCode);
        pnlFactionStanding.setLayout(new BoxLayout(pnlFactionStanding, BoxLayout.Y_AXIS));
        pnlFactionStanding.setBorder(createStandingColoredRoundedTitledBorder(factionStanding.getStandingLevel()));
        pnlFactionStanding.setPreferredSize(UIUtil.scaleForGUI(500, 250));
        pnlFactionStanding.setMaximumSize(UIUtil.scaleForGUI(500, 250));
        pnlFactionStanding.addMouseListener(createEffectsPanelUpdater(getEffectsDescription(factionFame)));

        // Faction Logo
        ImageIcon icon = Factions.getFactionLogo(gameYear, factionCode);
        icon = ImageUtilities.scaleImageIcon(icon, UIUtil.scaleForGUI(100), true);
        JLabel lblFactionImage = new JLabel(icon);
        lblFactionImage.setName("lblFactionImage" + factionCode);
        lblFactionImage.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblFactionImage.getPreferredSize().height));
        lblFactionImage.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        pnlFactionStanding.add(lblFactionImage);

        // Faction Descriptions
        String factionDescription = getDescriptionForFaction(faction, factionFame);
        JLabel lblDetails = new JLabel(factionDescription);
        lblDetails.setName("lblFactionDetails" + factionCode);
        lblDetails.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblDetails.getPreferredSize().height));
        lblDetails.setAlignmentX(CENTER_ALIGNMENT);
        lblDetails.setHorizontalAlignment(SwingConstants.CENTER);
        pnlFactionStanding.add(lblDetails);

        // Fame Slider
        JSlider sldFame = getFameSlider(factionCode, factionFame);
        pnlFactionStanding.add(sldFame);

        return pnlFactionStanding;
    }

    private static JSlider getFameSlider(String factionCode, double factionFame) {
        int roundedFame = (int) Math.round(factionFame); // JSlider doesn't accept doubles, so we round.
        int minimumFame = (int) Math.floor(FactionStandings.getMinimumFame());
        int maximumFame = (int) Math.ceil(FactionStandings.getMaximumFame());
        JSlider sldFame = new JSlider(minimumFame, maximumFame, roundedFame);
        sldFame.setName("sldFactionFame" + factionCode);
        sldFame.setEnabled(false);
        sldFame.setMaximumSize(new Dimension(Integer.MAX_VALUE, sldFame.getPreferredSize().height));
        sldFame.setAlignmentX(JSlider.CENTER_ALIGNMENT);
        return sldFame;
    }

    /**
     * Calculates the standing effects description string for a given faction fame value.
     *
     * @param factionFame the fame value of the faction
     * @return the standing effects description for the corresponding {@link FactionStandingLevel}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getEffectsDescription(double factionFame) {
        FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(factionFame);
        return factionStanding.getEffectsDescription();
    }

    public MouseAdapter createEffectsPanelUpdater(String replacementText) {
        final String effectsText = replacementText == null ? "" : replacementText;
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                JTextArea effectsArea = findComponentByName(getContentPane(),
                      EFFECTS_PANEL_LABEL_NAME,
                      JTextArea.class);
                if (effectsArea != null) {
                    LOGGER.debug("Updating effects panel with text: {}", effectsText);
                    effectsArea.setText(effectsText);
                }
            }
        };
    }

    private <T extends Component> T findComponentByName(Container container, String name, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component) && name.equals(component.getName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findComponentByName(child, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Constructs the HTML markup string used to describe a faction's details and standing.
     *
     * @param faction      the faction object
     * @param factionFame  the fame value for this faction
     * @return a HTML string for displaying faction details, standing, and description
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getDescriptionForFaction(Faction faction, double factionFame) {
        String factionName = faction.getFullName(gameYear);
        FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(factionFame);
        String factionStandingLabel = factionStanding.getLabel(faction);
        String factionStandingDescription = factionStanding.getDescription(faction);

        return String.format("<html><div style='text-align: center;'><h1>%s</h1><h2>%s</h2><i>%s</i></div></html>",
              factionName,
              factionStandingLabel,
              factionStandingDescription);
    }

    /**
     * Creates a {@link CompoundBorder} consisting of a {@code RoundedLineBorder} colored according to the specified
     * faction standing level, combined with internal padding.
     *
     * <p>The color selection is determined by the faction standing level:<br>
     * <ul>
     *     <li>≤ 1: Negative color</li>
     *     <li>≤ 3: Warning color</li>
     *     <li>≥ 7: Elite color</li>
     *     <li>≥ 5: Positive color</li>
     * </ul>
     *
     * <p>If standing is neutral, gray is used by default.</p>
     *
     * @param factionStandingLevel the numeric standing level of the faction which determines the border color
     *
     * @return a compound border with a colored rounded line border and padding
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static Border createStandingColoredRoundedTitledBorder(final int factionStandingLevel) {
        Color color = Color.GRAY;
        if (factionStandingLevel <= 1) {
            color = MekHQ.getMHQOptions().getFontColorNegative();
        } else if (factionStandingLevel <= 3) {
            color = MekHQ.getMHQOptions().getFontColorWarning();
        } else if (factionStandingLevel >= 7) {
            color = MekHQ.getMHQOptions().getFontColorSkillElite();
        } else if (factionStandingLevel >= 5) {
            color = MekHQ.getMHQOptions().getFontColorPositive();
        }

        Border rounded = new RoundedLineBorder(color, 2, 16);
        Border padding = BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING);
        Border compound = BorderFactory.createCompoundBorder(rounded, padding);

        int stars = factionStandingLevel + 1;
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            title.append("\u2605 ");
        }

        return BorderFactory.createTitledBorder(compound, title.toString());
    }
}
