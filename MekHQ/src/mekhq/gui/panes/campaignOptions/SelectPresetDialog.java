package mekhq.gui.panes.campaignOptions;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.CampaignPreset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;

/**
 * A dialog for selecting campaign presets.
 */
public class SelectPresetDialog extends JDialog {
    private static final Logger log = LogManager.getLogger(SelectPresetDialog.class);
    // region Variable Declarations
    private static String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE,
        MekHQ.getMHQOptions().getLocale());

    final static String OPTION_SELECT_PRESET = resources.getString("presetDialogSelect.name");
    final static String OPTION_CUSTOMIZE_PRESET = resources.getString("presetDialogCustomize.name");
    final static String OPTION_CANCEL = resources.getString("presetDialogCancel.name");

    private static final MMLogger logger = MMLogger.create(SelectPresetDialog.class);
    // endregion Variable Declarations

    // region Constructors

    /**
     * A dialog for selecting campaign presets.
     *
     * @param frame The parent {@link JFrame}.
     */
    private SelectPresetDialog(JFrame frame) {
        super(frame, resources.getString("presetDialog.title"), true); // make the dialog modal

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        ImageIcon image = new ImageIcon("data/images/misc/megamek-splash.png");
        JLabel imageLabel = new JLabel(image);
        add(imageLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        final GroupLayout layout = new GroupLayout(centerPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        centerPanel.setLayout(layout);

        JLabel descriptionLabel = new JLabel(resources.getString("presetDialog.description"));

        final DefaultListModel<CampaignPreset> campaignPresets = new DefaultListModel<>();
        campaignPresets.addAll(CampaignPreset.getCampaignPresets());

        if (campaignPresets.isEmpty()) {
            logger.error("No campaign presets found", "Error");
        }

        JComboBox<CampaignPreset> comboBox = new JComboBox<>();
        comboBox.setModel(convertPresetListModelToComboBoxModel(campaignPresets));

        DefaultListCellRenderer listRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                if (value instanceof CampaignPreset preset) {
                    setText(preset.getTitle());  // Set the name as the display text
                    setToolTipText(wordWrap(preset.getDescription())); // Set the description as the tooltip
                }

                // Align the text to the center
                setHorizontalAlignment(JLabel.CENTER);

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                return this;
            }
        };
        comboBox.setRenderer(listRenderer);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(descriptionLabel)
                .addComponent(comboBox)
        );

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(descriptionLabel)
                .addComponent(comboBox)
        );

        add(centerPanel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();

        JButton button1 = new JButton(OPTION_SELECT_PRESET);
        button1.setToolTipText(resources.getString("presetDialogSelect.tooltip"));
        button1.addActionListener(e -> {
            applyPreset((CampaignPreset) comboBox.getSelectedItem());
            dispose();
        });
        buttonPanel.add(button1);

        JButton button2 = new JButton(OPTION_CUSTOMIZE_PRESET);
        button2.setToolTipText(resources.getString("presetDialogCustomize.tooltip"));
        button2.addActionListener(e -> {
            // handle button 2 click
            // TODO initialize campaign options dialog
            dispose();
        });
        buttonPanel.add(button2);

        JButton button3 = new JButton(OPTION_CANCEL);
        button3.setToolTipText(resources.getString("presetDialogCancel.tooltip"));
        button3.addActionListener(e -> dispose());
        buttonPanel.add(button3);

        add(buttonPanel, BorderLayout.PAGE_END);

        pack();
    }

    /**
     * Converts a {@link DefaultListModel} of {@link CampaignPreset} objects to a {@link DefaultComboBoxModel}.
     *
     * @param listModel The {@link DefaultListModel} to convert.
     * @return The converted {@link DefaultComboBoxModel}.
     */
    private DefaultComboBoxModel<CampaignPreset> convertPresetListModelToComboBoxModel(
        DefaultListModel<CampaignPreset> listModel) {
        // Create a new DefaultComboBoxModel
        DefaultComboBoxModel<CampaignPreset> comboBoxModel = new DefaultComboBoxModel<>();

        // Populate the DefaultComboBoxModel with the elements from the DefaultListModel
        for (int i = 0; i < listModel.size(); i++) {
            comboBoxModel.addElement(listModel.get(i));
        }

        return comboBoxModel;
    }

    /**
     * Displays the dialog for selecting campaign presets.
     */
    public static void displayPresetDialog() {
        JFrame frame = new JFrame();
        SelectPresetDialog dialog = new SelectPresetDialog(frame);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    /**
     * Applies the chosen preset to the campaign.
     *
     * @param preset The {@link CampaignPreset} to apply.
     */
    public void applyPreset(final CampaignPreset preset) {
        // TODO: apply chosen preset
    }
}
