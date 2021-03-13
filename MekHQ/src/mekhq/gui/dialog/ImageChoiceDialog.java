/*
 * Copyright (c) 2009, 2016, 2020 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.common.icons.AbstractIcon;
import megamek.common.util.fileUtils.DirectoryItems;
import megamek.common.util.EncodeControl;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.campaign.force.Force;
import mekhq.gui.enums.LayeredForceIcon;
import megamek.client.ui.preferences.JWindowPreference;
import mekhq.gui.utilities.MekHqTableCellRenderer;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ImageChoiceDialog extends JDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 7316667282566479439L;

    private static final String PANEL_IMAGES = "panel_images";
    private static final String PANEL_LAYERED = "panel_layered";

    /**
     * The categorized image patterns.
     */
    private DirectoryItems imageItems;
    private ImageTableModel imageTableModel = new ImageTableModel();
    private String category;
    private String filename;
    private LinkedHashMap<String, Vector<String>> iconMap; // Key = Image Category, Value = Vector of Image Filenames
    private ImageTableMouseAdapter imagesMouseAdapter;
    private boolean force;
    private JTable tableImages;
    private boolean changed = false;

    //region Layered Images Support
    private JLabel preview = new JLabel();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private JTabbedPane layerTabs = new JTabbedPane();
    private JPanel layerPanel = new JPanel();

    // Combined array format
    private JTable[] layeredTables;
    //endregion Layered Images Support
    //endregion Variable Declarations

    //region Constructors
    public ImageChoiceDialog(Frame parent, boolean modal, String category, String filename,
                             DirectoryItems items) {
        this(parent, modal, category, filename, null, items, false);
    }

    public ImageChoiceDialog(Frame parent, boolean modal, String category, String filename,
                             LinkedHashMap<String, Vector<String>> iconMap, DirectoryItems items,
                             boolean force) {
        super(parent, modal);
        this.category = category;
        this.filename = filename;
        imagesMouseAdapter = new ImageTableMouseAdapter();
        this.imageItems = items;
        this.force = force;

        // Clone the input iconMap
        this.iconMap = new LinkedHashMap<>();
        if ((iconMap != null) && !iconMap.isEmpty()) {
            for (Map.Entry<String, Vector<String>> entry : iconMap.entrySet()) {
                if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                    this.iconMap.put(entry.getKey(), new Vector<>(entry.getValue()));
                }
            }
        }

        initComponents();

        setLocationRelativeTo(parent);
        setUserPreferences();
    }
    //endregion Constructors

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ImageChoiceDialog", new EncodeControl()); //$NON-NLS-1$
        GridBagConstraints gbc;

        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(force ? resourceMap.getString("Force.title") : resourceMap.getString("Portrait.title"));

        JPanel imagesPanel = new JPanel();
        imagesPanel.setLayout(new GridBagLayout());
        imagesPanel.setName(PANEL_IMAGES);

        DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<>();
        String match = null;
        categoryModel.addElement(AbstractIcon.ROOT_CATEGORY);
        Iterator<String> names = (imageItems != null)
                ? imageItems.getCategoryNames() : Collections.emptyIterator();
        while (names.hasNext()) {
            String name = names.next();
            if (!"".equals(name)) {
                categoryModel.addElement(name);
                if (category.equals(name)) {
                    match = name;
                }
            }
        }
        categoryModel.setSelectedItem((match != null) ? match : AbstractIcon.ROOT_CATEGORY);
        JComboBox<String> comboCategories = new JComboBox<>(categoryModel);
        comboCategories.setName("comboCategories"); // NOI18N
        comboCategories.addItemListener(this::comboCategoriesItemStateChanged);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        imagesPanel.add(comboCategories, gbc);

        tableImages = new JTable(imageTableModel);
        tableImages.setName("tableImages"); // NOI18N
        tableImages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableImages.setRowHeight(76);
        tableImages.getColumnModel().getColumn(0).setCellRenderer(imageTableModel.getRenderer());
        tableImages.addMouseListener(imagesMouseAdapter);
        JScrollPane scrImages = new JScrollPane();
        scrImages.setName("scrImages"); // NOI18N
        scrImages.setViewportView(tableImages);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        imagesPanel.add(scrImages, gbc);

        // Initialize the imageTableModel
        fillTable((String) comboCategories.getSelectedItem());
        // Determine the initial value for the selected image, if any
        for (int i = 0; i < imageTableModel.getRowCount(); i++) {
            if (imageTableModel.getValueAt(i, 0).equals(filename)) {
                tableImages.setRowSelectionInterval(i, i);
                break;
            }
        }

        if (force) {
            // Background setup for the layered options
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;

            // We need to ensure that the state that it comes into is saved, as we need to add the
            // default frame icon otherwise
            final boolean emptyInitialIconMap = iconMap.isEmpty();

            // Add the default frame icon
            if (emptyInitialIconMap) {
                Vector<String> initFrame = new Vector<>();
                initFrame.add("Frame.png");
                iconMap.put(LayeredForceIcon.FRAME.getLayerPath(), initFrame);
            }

            LayeredForceIcon[] layeredForceIcons = LayeredForceIcon.values();
            layeredTables = new JTable[layeredForceIcons.length];

            for (int i = 0; i < layeredForceIcons.length; i++) {
                JScrollPane scrollPane = new JScrollPane();
                layeredTables[i] = new JTable();
                ImageTableModel tableModel = new ImageTableModel();
                JPanel panel = new JPanel();

                layeredTables[i].setModel(tableModel);
                layeredTables[i].setName(layeredForceIcons[i].getTableName());
                layeredTables[i].setSelectionMode(layeredForceIcons[i].getListSelectionModel());
                layeredTables[i].setRowHeight(76);
                layeredTables[i].getColumnModel().getColumn(0).setCellRenderer(tableModel.getRenderer());
                layeredTables[i].addMouseListener(new ImageTableMouseAdapter());
                scrollPane.setViewportView(layeredTables[i]);
                panel.add(scrollPane, gbc);
                tableModel.reset();
                tableModel.setCategory(layeredForceIcons[i].getLayerPath());
                tableModel.addImage(AbstractIcon.DEFAULT_ICON_FILENAME);
                Iterator<String> imageIterator = (imageItems != null)
                        ? imageItems.getItemNames(layeredForceIcons[i].getLayerPath())
                        : Collections.emptyIterator();
                while (imageIterator.hasNext()) {
                    tableModel.addImage(imageIterator.next());
                }
                layerTabs.addTab(layeredForceIcons[i].toString(), panel);

                // Initialize Initial Values, provided the Icon Map is not empty on input or it
                // is the frame (as we set that value otherwise)
                if (!emptyInitialIconMap || (layeredForceIcons[i] == LayeredForceIcon.FRAME)) {
                    if (iconMap.containsKey(layeredForceIcons[i].getLayerPath())) {
                        if (layeredForceIcons[i].getListSelectionModel() == ListSelectionModel.SINGLE_SELECTION) {
                            // Determine the current selected value
                            String selected = iconMap.get(layeredForceIcons[i].getLayerPath()).get(0);
                            for (int k = 0; k < tableModel.getRowCount(); k++) {
                                if (tableModel.getValueAt(k, 0).equals(selected)) {
                                    // This adds k as a selected row, with the backend considering it
                                    // as selecting the interval between k and k, inclusively
                                    layeredTables[i].setRowSelectionInterval(k, k);
                                    break;
                                }
                            }
                        } else {
                            Vector<String> mapVector = iconMap.get(layeredForceIcons[i].getLayerPath());
                            for (int k = 0; k < tableModel.getRowCount(); k++) {
                                if (mapVector.contains((String) tableModel.getValueAt(k, 0))) {
                                    // This adds k as a selected row, with the backend considering it
                                    // as selecting the interval between k and k, inclusively
                                    layeredTables[i].addRowSelectionInterval(k, k);
                                }
                            }
                        }
                    }
                }
            }

            // Put it all together nice and pretty on the layerPanel
            layerPanel.setLayout(new GridBagLayout());
            layerPanel.add(layerTabs, gbc);
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.SOUTH;
            gbc.weighty = 0.0;
            preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 225));
            preview.setMinimumSize(new Dimension(300, 225));
            layerPanel.add(preview, gbc);
            layerPanel.setName(PANEL_LAYERED);

            // Add single and layered options to the dialog
            tabbedPane.addTab(resourceMap.getString("Force.single"), imagesPanel);
            tabbedPane.addTab(resourceMap.getString("Force.layered"), layerPanel);

            // Set currently selected tab based on the initial category
            if (!emptyInitialIconMap) {
                tabbedPane.setSelectedComponent(layerPanel);
            }

            // Add the tabbed pane to the content pane
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            getContentPane().add(tabbedPane, gbc);

            // Then trigger the initial refresh
            refreshLayeredPreview();

            // And add the missing ListSelectionListeners, which must be done after the initial refresh
            for (JTable table : layeredTables) {
                table.getSelectionModel().addListSelectionListener(event -> refreshLayeredPreview());
            }
        } else {
            // Add the image panel to the content pane
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            getContentPane().add(imagesPanel, gbc);
        }

        JButton btnSelect = new JButton(resourceMap.getString("btnSelect.text")); // NOI18N
        btnSelect.setName("btnSelect"); // NOI18N
        btnSelect.addActionListener(this::btnSelectActionPerformed);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.5;
        getContentPane().add(btnSelect, gbc);

        JButton btnCancel = new JButton(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(this::btnCancelActionPerformed);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.5;
        getContentPane().add(btnCancel, gbc);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(ImageChoiceDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnCancelActionPerformed(ActionEvent evt) {
        setVisible(false);
    }

    private void btnSelectActionPerformed(ActionEvent evt) {
        category = ((null != tabbedPane.getSelectedComponent())
                && PANEL_LAYERED.equals(tabbedPane.getSelectedComponent().getName()))
            ? Force.ROOT_LAYERED : imageTableModel.getCategory();
        if (tableImages.getSelectedRow() != -1) {
            filename = (String) imageTableModel.getValueAt(tableImages.getSelectedRow(), 0);
        } else {
            filename = AbstractIcon.DEFAULT_ICON_FILENAME;
        }
        changed = true;
        setVisible(false);
    }

    /**
     * @return the changed
     */
    public boolean isChanged() {
        return changed;
    }

    private void comboCategoriesItemStateChanged(ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            fillTable((String) evt.getItem());
        }
    }

    public String getCategory() {
        return category;
    }

    public String getFileName() {
        return filename;
    }

    public LinkedHashMap<String, Vector<String>> getIconMap() {
        return iconMap;
    }

    private void refreshLayeredPreview() {
        // Clear the icon map
        iconMap.clear();

        // Check each table for what is, or is not, selected
        Vector<String> temp;
        LayeredForceIcon[] layeredForceIcons = LayeredForceIcon.values();
        for (int i = 0; i < layeredTables.length; i++) {
            // If we are in the first row, we have the None option selected. Therefore, we need to
            // Ignore the selected index.
            if (layeredTables[i].getSelectedRow() <= 0) {
                iconMap.remove(layeredForceIcons[i].getLayerPath());
            } else {
                temp = new Vector<>();
                for (int index : layeredTables[i].getSelectedRows()) {
                    temp.add((String) layeredTables[i].getValueAt(index, 0));
                }
                iconMap.put(layeredForceIcons[i].getLayerPath(), temp);
            }
        }

        category = Force.ROOT_LAYERED;
        filename = AbstractIcon.DEFAULT_ICON_FILENAME;

        // Build the layered image
        Image forceImage = MHQStaticDirectoryManager.buildForceIcon(category, filename, iconMap);
        ImageIcon imageIcon = new ImageIcon(forceImage);

        // Disable selection of a static icon
        tableImages.clearSelection();

        // Update the preview
        preview.setIcon(imageIcon);
        preview.validate();
    }

    private void fillTable(String category) {
        imageTableModel.reset();
        imageTableModel.setCategory(category);
        // Translate the "root image" category name.
        Iterator<String> imageNames;
        if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
            imageTableModel.addImage(AbstractIcon.DEFAULT_ICON_FILENAME);
            imageNames = (imageItems != null)
                    ? imageItems.getItemNames("") : Collections.emptyIterator();
        } else {
            imageNames = (imageItems != null)
                    ? imageItems.getItemNames(category) : Collections.emptyIterator();
        }

        // Get the image names for this category.
        while (imageNames.hasNext()) {
            imageTableModel.addImage(imageNames.next());
        }
        if (imageTableModel.getRowCount() > 0) {
            tableImages.setRowSelectionInterval(0, 0);
        }
    }

    /**
     * A table model for displaying images
     */
    public class ImageTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -7469653910161174678L;
        private String[] columnNames;
        private String category;
        private List<String> names;

        public ImageTableModel() {
            columnNames = new String[] {"Images"};
            category = AbstractIcon.ROOT_CATEGORY;
            names = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return names.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        public void reset() {
            category = AbstractIcon.ROOT_CATEGORY;
            names = new ArrayList<>();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return names.get(row);
        }

        public void setCategory(String c) {
            category = c;
        }

        public String getCategory() {
            return category;
        }

        public void addImage(String name) {
            names.add(name);
            fireTableDataChanged();
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public ImageTableModel.Renderer getRenderer() {
            return new ImageTableModel.Renderer(imageItems);
        }

        public class Renderer extends ImagePanel implements TableCellRenderer {
            private static final long serialVersionUID = -6025788865509594987L;

            public Renderer(DirectoryItems images) {
                super(images);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String name = getValueAt(row, column).toString();
                setText(getValueAt(row, column).toString());
                setImage(category, name);

                MekHqTableCellRenderer.setupTableColors(this, table, isSelected, hasFocus, row);
                return this;
            }
        }
    }

    public class ImageTableMouseAdapter extends MouseInputAdapter {
        @Override
        public void mouseClicked(MouseEvent evt) {
            if ((evt.getButton() == MouseEvent.BUTTON1) && (evt.getClickCount() == 2)) {
                if (tableImages.equals(evt.getSource())) {
                    int row = tableImages.rowAtPoint(evt.getPoint());
                    if (row < imageTableModel.getRowCount()) {
                        category = imageTableModel.getCategory();
                        filename = (String) imageTableModel.getValueAt(row, 0);
                        changed = true;
                        setVisible(false);
                    }
                }
            }
        }
    }

    public class ImagePanel extends JPanel {
        private static final long serialVersionUID = -3724175393116586310L;
        private DirectoryItems items;
        private JLabel lblImage;

        public ImagePanel(DirectoryItems items) {
            this.items = items;
            initComponents();
        }

        private void initComponents() {
            GridBagConstraints gbc;

            lblImage = new JLabel();

            setName("Form"); // NOI18N
            setLayout(new GridBagLayout());

            lblImage.setText(""); // NOI18N
            lblImage.setName("lblImage"); // NOI18N
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            add(lblImage, gbc);
        }

        public void setText(String text) {
            lblImage.setText(text);
        }

        public void setImage(String category, String name) {
            if ((null == category) || AbstractIcon.DEFAULT_ICON_FILENAME.equals(name)) {
                int width = force ? 110 : 76;
                int height = 76;

                BufferedImage noImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = noImage.createGraphics();
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, width, height);
                lblImage.setIcon(new ImageIcon(noImage));
                return;
            }

            // Try to get the image file.
            try {
                // Translate the root image directory name.
                if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
                    category = ""; //$NON-NLS-1$
                }
                Image image = (Image) items.getItem(category, name);
                if (image != null) {
                    if (category.startsWith("Pieces/")) {
                        image = image.getScaledInstance(110, -1, Image.SCALE_SMOOTH);
                    } else {
                        image = image.getScaledInstance(-1, 76, Image.SCALE_SMOOTH);
                    }

                    lblImage.setIcon(new ImageIcon(image));
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
    }
}
