/*
 * ImageChoiceDialog.java
 *
 * Created on October 1, 2009, 3:10 PM
 */

package mekhq.gui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.Vector;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.common.Crew;
import megamek.common.util.DirectoryItems;
import megamek.common.util.EncodeControl;
import mekhq.IconPackage;
import mekhq.campaign.force.Force;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ImageChoiceDialog extends JDialog {

    private static final String PANEL_IMAGES = "panel_images";
    private static final String PANEL_LAYERED = "panel_layered";
    
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * The categorized image patterns.
     */
    private DirectoryItems imageItems;
    private ImageTableModel imageTableModel = new ImageTableModel();
    private String category;
    private String filename;
    private LinkedHashMap<String, Vector<String>> iconMap; // Key = Image Category, Value = Vector of Image Filenames 
    private ImageTableMouseAdapter imagesMouseAdapter;
    private boolean force = false;
    private JButton btnCancel;
    private JButton btnSelect;
    private JComboBox<String> comboCategories;
    private JScrollPane scrImages;
    private JTable tableImages;
    private boolean changed = false;
    
    // BEGIN: Layered Images Support
    private ImageIcon imageIcon = null;
    private JLabel preview = new JLabel();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private JTabbedPane layerTabs = new JTabbedPane();
    private JPanel layerPanel = new JPanel();
    // Types
    private JScrollPane scrTypes = new JScrollPane();
    private JTable tableTypes = new JTable();
    private JPanel panelTypes = new JPanel();
    private ImageTableModel typesModel = new ImageTableModel();
    // Formations
    private JScrollPane scrFormations = new JScrollPane();
    private JTable tableFormations = new JTable();
    private JPanel panelFormations = new JPanel();
    private ImageTableModel formationsModel = new ImageTableModel();
    // Adjustments
    private JScrollPane scrAdjustments = new JScrollPane();
    private JTable tableAdjustments = new JTable();
    private JPanel panelAdjustments = new JPanel();
    private ImageTableModel adjustmentsModel = new ImageTableModel();
    // Alphanumerics
    private JScrollPane scrAlphanumerics = new JScrollPane();
    private JTable tableAlphanumerics = new JTable();
    private JPanel panelAlphanumerics = new JPanel();
    private ImageTableModel alphanumericsModel = new ImageTableModel();
    // Special Modifiers
    private JScrollPane scrSpecialModifiers = new JScrollPane();
    private JTable tableSpecialModifiers = new JTable();
    private JPanel panelSpecialModifiers = new JPanel();
    private ImageTableModel specialModel = new ImageTableModel();
    // Backgrounds
    private JScrollPane scrBackgrounds = new JScrollPane();
    private JTable tableBackgrounds = new JTable();
    private JPanel panelBackgrounds = new JPanel();
    private ImageTableModel backgroundsModel = new ImageTableModel();
    // Logos
    private JScrollPane scrLogos = new JScrollPane();
    private JTable tableLogos = new JTable();
    private JPanel panelLogos = new JPanel();
    private ImageTableModel logosModel = new ImageTableModel();
    // END: Layered Images Support

    /** Creates new form ImageChoiceDialog */
    public ImageChoiceDialog(Frame parent, boolean modal, String category, String file, DirectoryItems items) {
        this(parent, modal, category, file, items, false);
    }


    /** Creates new form ImageChoiceDialog */
    public ImageChoiceDialog(Frame parent, boolean modal, String category, String file, DirectoryItems items, boolean force) {
        super(parent, modal);
        this.category = category;
        filename = file;
        imagesMouseAdapter = new ImageTableMouseAdapter();
        this.imageItems = items;
        this.force = force;
        // If we're doing forces, initialize the hashmap for use
        if (force) {
            iconMap = new LinkedHashMap<String, Vector<String>>();
        }
        initComponents();
        fillTable((String) comboCategories.getSelectedItem());
        int rowIndex = 0;
        for(int i = 0; i < imageTableModel.getRowCount(); i++) {
            if(((String) imageTableModel.getValueAt(i, 0)).equals(filename)) {
                rowIndex = i;
                break;
            }
        }
        tableImages.setRowSelectionInterval(rowIndex, rowIndex);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        GridBagConstraints gbc;

        scrImages = new JScrollPane();
        tableImages = new JTable();
        comboCategories = new JComboBox<String>();
        btnSelect = new JButton();
        btnCancel = new JButton();
        JPanel imagesPanel = new JPanel();
        getContentPane().setLayout(new GridBagLayout());

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ImageChoiceDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(force ? resourceMap.getString("Force.title") : resourceMap.getString("Portrait.title"));
        imagesPanel.setLayout(new GridBagLayout());
        imagesPanel.setName(PANEL_IMAGES);

        scrImages.setName("scrImages"); // NOI18N

        tableImages.setModel(imageTableModel);
        tableImages.setName("tableImages"); // NOI18N
        tableImages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableImages.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                // Clear selections on the layered tables
                tableAdjustments.clearSelection();
                tableAlphanumerics.clearSelection();
                tableFormations.clearSelection();
                tableSpecialModifiers.clearSelection();
                tableBackgrounds.clearSelection();
                tableLogos.clearSelection();
                tableTypes.clearSelection();
            }
        });
        tableImages.setRowHeight(76);
        tableImages.getColumnModel().getColumn(0).setCellRenderer(imageTableModel.getRenderer());
        tableImages.addMouseListener(imagesMouseAdapter);
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

        DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<String>();
        String match = null;
        categoryModel.addElement(Crew.ROOT_PORTRAIT);
        if (imageItems != null) {
            Iterator<String> names = imageItems.getCategoryNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!"".equals(name)) { //$NON-NLS-1$
                    categoryModel.addElement(name);
                    if(category.equals(name)) {
                        match = name;
                    }
                }
            }
        }
        if(null != match) {
            categoryModel.setSelectedItem(match);
        } else {
            categoryModel.setSelectedItem(Crew.ROOT_PORTRAIT);
        }
        comboCategories.setModel(categoryModel);
        comboCategories.setName("comboCategories"); // NOI18N
        comboCategories.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent evt) {
                comboCategoriesItemStateChanged(evt);
            }
        });
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        imagesPanel.add(comboCategories, gbc);

        if (force) {
            // Background setup for the layered options
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;

            // Panel for Types
            tableTypes.setModel(typesModel);
            tableTypes.setName("tableTypes"); // NOI18N
            tableTypes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tableTypes.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent event) {
                    refreshLayeredPreview();
                }
            });
            tableTypes.setRowHeight(76);
            tableTypes.getColumnModel().getColumn(0).setCellRenderer(typesModel.getRenderer());
            tableTypes.addMouseListener(new ImageTableMouseAdapter());
            scrTypes.setViewportView(tableTypes);
            panelTypes.add(scrTypes, gbc);
            typesModel.reset();
            typesModel.setCategory(IconPackage.FORCE_TYPE);
            Iterator<String> imageNames = imageItems.getItemNames(IconPackage.FORCE_TYPE);
            while (imageNames.hasNext()) {
                typesModel.addImage(imageNames.next());
            }
            layerTabs.addTab(resourceMap.getString("Force.types"), panelTypes);

            // Panel for Formations
            tableFormations.setModel(formationsModel);
            tableFormations.setName("tableFormations"); // NOI18N
            tableFormations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tableFormations.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent event) {
                    refreshLayeredPreview();
                }
            });
            tableFormations.setRowHeight(76);
            tableFormations.getColumnModel().getColumn(0).setCellRenderer(formationsModel.getRenderer());
            tableFormations.addMouseListener(new ImageTableMouseAdapter());
            scrFormations.setViewportView(tableFormations);
            panelFormations.add(scrFormations, gbc);
            formationsModel.reset();
            formationsModel.setCategory(IconPackage.FORCE_FORMATIONS);
            Iterator<String> imageNamesTypes = imageItems.getItemNames(IconPackage.FORCE_FORMATIONS);
            while (imageNamesTypes.hasNext()) {
                formationsModel.addImage(imageNamesTypes.next());
            }
            layerTabs.addTab(resourceMap.getString("Force.formations"), panelFormations);

            // Panel for Adjustments
            tableAdjustments.setModel(adjustmentsModel);
            tableAdjustments.setName("tableAdjustments"); // NOI18N
            tableAdjustments.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tableAdjustments.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent event) {
                    refreshLayeredPreview();
                }
            });
            tableAdjustments.setRowHeight(76);
            tableAdjustments.getColumnModel().getColumn(0).setCellRenderer(adjustmentsModel.getRenderer());
            tableAdjustments.addMouseListener(new ImageTableMouseAdapter());
            scrAdjustments.setViewportView(tableAdjustments);
            panelAdjustments.add(scrAdjustments, gbc);
            adjustmentsModel.reset();
            adjustmentsModel.setCategory(IconPackage.FORCE_ADJUSTMENTS);
            Iterator<String> imageNamesAdjustments = imageItems.getItemNames(IconPackage.FORCE_ADJUSTMENTS);
            while (imageNamesAdjustments.hasNext()) {
                adjustmentsModel.addImage(imageNamesAdjustments.next());
            }
            layerTabs.addTab(resourceMap.getString("Force.adjustments"), panelAdjustments);

            // Panel for Alphanumerics
            tableAlphanumerics.setModel(alphanumericsModel);
            tableAlphanumerics.setName("tableAalphanumerics"); // NOI18N
            tableAlphanumerics.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tableAlphanumerics.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent event) {
                    refreshLayeredPreview();
                }
            });
            tableAlphanumerics.setRowHeight(76);
            tableAlphanumerics.getColumnModel().getColumn(0).setCellRenderer(alphanumericsModel.getRenderer());
            tableAlphanumerics.addMouseListener(new ImageTableMouseAdapter());
            scrAlphanumerics.setViewportView(tableAlphanumerics);
            panelAlphanumerics.add(scrAlphanumerics, gbc);
            alphanumericsModel.reset();
            alphanumericsModel.setCategory(IconPackage.FORCE_ALPHANUMERICS);
            Iterator<String> imageNamesAlphanumerics = imageItems.getItemNames(IconPackage.FORCE_ALPHANUMERICS);
            while (imageNamesAlphanumerics.hasNext()) {
                alphanumericsModel.addImage(imageNamesAlphanumerics.next());
            }
            layerTabs.addTab(resourceMap.getString("Force.alphanumerics"), panelAlphanumerics);

            // Panel for SpecialModifiers
            tableSpecialModifiers.setModel(specialModel);
            tableSpecialModifiers.setName("tableSpecialModifiers"); // NOI18N
            tableSpecialModifiers.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tableSpecialModifiers.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent event) {
                    refreshLayeredPreview();
                }
            });
            tableSpecialModifiers.setRowHeight(76);
            tableSpecialModifiers.getColumnModel().getColumn(0).setCellRenderer(specialModel.getRenderer());
            tableSpecialModifiers.addMouseListener(new ImageTableMouseAdapter());
            scrSpecialModifiers.setViewportView(tableSpecialModifiers);
            panelSpecialModifiers.add(scrSpecialModifiers, gbc);
            specialModel.reset();
            specialModel.setCategory(IconPackage.FORCE_SPECIAL_MODIFIERS);
            Iterator<String> imageNamesSpecial = imageItems.getItemNames(IconPackage.FORCE_SPECIAL_MODIFIERS);
            while (imageNamesSpecial.hasNext()) {
                specialModel.addImage(imageNamesSpecial.next());
            }
            layerTabs.addTab(resourceMap.getString("Force.special"), panelSpecialModifiers);

            // Panel for Backgrounds
            tableBackgrounds.setModel(backgroundsModel);
            tableBackgrounds.setName("tableBackgrounds"); // NOI18N
            tableBackgrounds.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tableBackgrounds.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent event) {
                    refreshLayeredPreview();
                }
            });
            tableBackgrounds.setRowHeight(76);
            tableBackgrounds.getColumnModel().getColumn(0).setCellRenderer(backgroundsModel.getRenderer());
            tableBackgrounds.addMouseListener(new ImageTableMouseAdapter());
            scrBackgrounds.setViewportView(tableBackgrounds);
            panelBackgrounds.add(scrBackgrounds, gbc);
            backgroundsModel.reset();
            backgroundsModel.setCategory(IconPackage.FORCE_BACKGROUNDS);
            Iterator<String> imageNamesBackgrounds = imageItems.getItemNames(IconPackage.FORCE_BACKGROUNDS);
            while (imageNamesBackgrounds.hasNext()) {
                backgroundsModel.addImage(imageNamesBackgrounds.next());
            }
            layerTabs.addTab(resourceMap.getString("Force.backgrounds"), panelBackgrounds);

            // Panel for Logos
            tableLogos.setModel(logosModel);
            tableLogos.setName("tableLogos"); // NOI18N
            tableLogos.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tableLogos.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent event) {
                    refreshLayeredPreview();
                }
            });
            tableLogos.setRowHeight(76);
            tableLogos.getColumnModel().getColumn(0).setCellRenderer(logosModel.getRenderer());
            tableLogos.addMouseListener(new ImageTableMouseAdapter());
            scrLogos.setViewportView(tableLogos);
            panelLogos.add(scrLogos, gbc);
            logosModel.reset();
            logosModel.setCategory(IconPackage.FORCE_LOGOS);
            Iterator<String> imageNamesLogos = imageItems.getItemNames(IconPackage.FORCE_LOGOS);
            while (imageNamesLogos.hasNext()) {
                logosModel.addImage(imageNamesLogos.next());
            }
            layerTabs.addTab(resourceMap.getString("Force.logos"), panelLogos);
            
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
            refreshLayeredPreview();

            // Add single and layered options to the dialog
            tabbedPane.addTab(resourceMap.getString("Force.single"), imagesPanel);
            tabbedPane.addTab(resourceMap.getString("Force.layered"), layerPanel);

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

        btnSelect.setText(resourceMap.getString("btnSelect.text")); // NOI18N
        btnSelect.setName("btnSelect"); // NOI18N
        btnSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                btnSelectActionPerformed(evt);
            }
        });
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.5;
        getContentPane().add(btnSelect, gbc);

        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.5;
        getContentPane().add(btnCancel, gbc);

        pack();
    }

    private void btnCancelActionPerformed(ActionEvent evt) {
        setVisible(false);
    }

    private void btnSelectActionPerformed(ActionEvent evt) {
        category = (null != tabbedPane.getSelectedComponent()) && PANEL_LAYERED.equals(tabbedPane.getSelectedComponent().getName())
            ? Force.ROOT_LAYERED : imageTableModel.getCategory();
        if(tableImages.getSelectedRow() != -1) {
            filename = (String) imageTableModel.getValueAt(tableImages.getSelectedRow(), 0);
        } else {
            filename = Crew.PORTRAIT_NONE;
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
    }//GEN-LAST:event_comboCategoriesItemStateChanged

    public String getCategory() {
        return category;
    }

    public String getFileName() {
        return filename;
    }

    /**
     * @return the iconMap
     */
    public LinkedHashMap<String, Vector<String>> getIconMap() {
        return iconMap;
    }

    /**
     * @param iconMap the iconMap to set
     */
    public void setIconMap(LinkedHashMap<String, Vector<String>> iconMap) {
        this.iconMap = iconMap;
    }
    
    private void refreshLayeredPreview() {
        // Add the image frame
        iconMap.clear();
        Vector<String> frameVector = new Vector<String>();
        frameVector.add("Frame.png");
        iconMap.put(IconPackage.FORCE_FRAME, frameVector);
        // Check each table for what is, or is not, selected
        Vector<String> tmp;
        if (tableTypes.getSelectedRow() == -1) {
            iconMap.remove(IconPackage.FORCE_TYPE, iconMap.get(IconPackage.FORCE_TYPE));
        } else {
            tmp = new Vector<String>();
            for (int index : tableTypes.getSelectedRows()) {
                tmp.add((String) tableTypes.getValueAt(index, 0));
                iconMap.put(IconPackage.FORCE_TYPE, tmp);
            }
        }
        if (tableFormations.getSelectedRow() == -1) {
            iconMap.remove(IconPackage.FORCE_FORMATIONS, iconMap.get(IconPackage.FORCE_FORMATIONS));
        } else {
            tmp = new Vector<String>();
            for (int index : tableFormations.getSelectedRows()) {
                tmp.add((String) tableFormations.getValueAt(index, 0));
                iconMap.put(IconPackage.FORCE_FORMATIONS, tmp);
            }
        }
        if (tableAdjustments.getSelectedRow() == -1) {
            iconMap.remove(IconPackage.FORCE_ADJUSTMENTS, iconMap.get(IconPackage.FORCE_ADJUSTMENTS));
        } else {
            tmp = new Vector<String>();
            for (int index : tableAdjustments.getSelectedRows()) {
                tmp.add((String) tableAdjustments.getValueAt(index, 0));
                iconMap.put(IconPackage.FORCE_ADJUSTMENTS, tmp);
            }
        }
        if (tableAlphanumerics.getSelectedRow() == -1) {
            iconMap.remove(IconPackage.FORCE_ALPHANUMERICS, iconMap.get(IconPackage.FORCE_ALPHANUMERICS));
        } else {
            tmp = new Vector<String>();
            for (int index : tableAlphanumerics.getSelectedRows()) {
                tmp.add((String) tableAlphanumerics.getValueAt(index, 0));
                iconMap.put(IconPackage.FORCE_ALPHANUMERICS, tmp);
            }
        }
        if (tableSpecialModifiers.getSelectedRow() == -1) {
            iconMap.remove(IconPackage.FORCE_SPECIAL_MODIFIERS, iconMap.get(IconPackage.FORCE_SPECIAL_MODIFIERS));
        } else {
            tmp = new Vector<String>();
            for (int index : tableSpecialModifiers.getSelectedRows()) {
                tmp.add((String) tableSpecialModifiers.getValueAt(index, 0));
                iconMap.put(IconPackage.FORCE_SPECIAL_MODIFIERS, tmp);
            }
        }
        if (tableBackgrounds.getSelectedRow() == -1) {
            iconMap.remove(IconPackage.FORCE_BACKGROUNDS, iconMap.get(IconPackage.FORCE_BACKGROUNDS));
        } else {
            tmp = new Vector<String>();
            for (int index : tableBackgrounds.getSelectedRows()) {
                tmp.add((String) tableBackgrounds.getValueAt(index, 0));
                iconMap.put(IconPackage.FORCE_BACKGROUNDS, tmp);
            }
        }
        if (tableLogos.getSelectedRow() == -1) {
            iconMap.remove(IconPackage.FORCE_LOGOS, iconMap.get(IconPackage.FORCE_LOGOS));
        } else {
            tmp = new Vector<String>();
            for (int index : tableLogos.getSelectedRows()) {
                tmp.add((String) tableLogos.getValueAt(index, 0));
                iconMap.put(IconPackage.FORCE_LOGOS, tmp);
            }
        }
        // Set the category to layered
        category = Force.ROOT_LAYERED;
        filename = Force.ICON_NONE;
        // Build the layered image
        Image forceImage = IconPackage.buildForceIcon(category, filename, imageItems, iconMap);
        imageIcon = new ImageIcon(forceImage);
        // Disable selection of a static icon
        tableImages.clearSelection();
        // Update the preview
        if (null == imageIcon) {
            preview.setText("");
        }
        preview.setIcon(imageIcon);
        preview.validate();
    }

    private void fillTable(String category) {
        imageTableModel.reset();
        imageTableModel.setCategory(category);
        // Translate the "root image" category name.
        Iterator<String> imageNames;
        if (Crew.ROOT_PORTRAIT.equals(category)) {
            imageTableModel.addImage(Crew.PORTRAIT_NONE);
            imageNames = imageItems.getItemNames(""); //$NON-NLS-1$
        } else {
            imageNames = imageItems.getItemNames(category);
        }

        // Get the image names for this category.
        while (imageNames.hasNext()) {
            imageTableModel.addImage(imageNames.next());
        }
        if(imageTableModel.getRowCount() > 0) {
            tableImages.setRowSelectionInterval(0, 0);
        }
    }

    /**
     * A table model for displaying images
     */
    public class ImageTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private String[] columnNames;
        private String category;
        private ArrayList<String> names;
        private ArrayList<Image> images;

        public ImageTableModel() {
            columnNames = new String[] {"Images"};
            category = Crew.ROOT_PORTRAIT;
            names = new ArrayList<String>();
            images = new ArrayList<Image>();
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
            category = Crew.ROOT_PORTRAIT;
            names = new ArrayList<String>();
            images = new ArrayList<Image>();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return names.get(row);
        }

        public Object getImageAt(int row) {
            return images.get(row);
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
        public Class<? extends Object> getColumnClass(int c) {
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

            public Renderer(DirectoryItems images) {
                super(images);
            }

            private static final long serialVersionUID = -6025788865509594987L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = this;
                setOpaque(true);
                String name = getValueAt(row, column).toString();
                setText(getValueAt(row, column).toString());
                setImage(category, name);
                if(isSelected) {
                    setBackground(new Color(220,220,220));
                } else {
                    setBackground(Color.WHITE);
                }

                return c;
            }
        }
    }

    public class ImageTableMouseAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                if (tableImages.equals(evt.getSource())) {
                    int row = tableImages.rowAtPoint(evt.getPoint());
                    if(row < imageTableModel.getRowCount()) {
                        category = imageTableModel.getCategory();
                        filename = (String) imageTableModel.getValueAt(row, 0);
                        setVisible(false);
                    }
                }
            }
        }
    }

    public class ImagePanel extends JPanel {

        /**
         *
         */
        private static final long serialVersionUID = -3724175393116586310L;
        private DirectoryItems items;

        /** Creates new form ImagePanel */
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
        }// </editor-fold>//GEN-END:initComponents

        public void setText(String text) {
            lblImage.setText(text);
        }

        public void setImage(String category, String name) {

            if (null == category
                    || name.equals(Crew.PORTRAIT_NONE)) {
                return;
            }

            // Try to get the image file.
            try {
                // Translate the root image directory name.
                if (Crew.ROOT_PORTRAIT.equals(category))
                    category = ""; //$NON-NLS-1$
                Image image = (Image) items.getItem(category, name);
                if(null != image) {
                    if((null != category) && category.startsWith("Pieces/")) {
                        image = image.getScaledInstance(110, -1, Image.SCALE_SMOOTH);
                    } else {
                        image = image.getScaledInstance(-1, 76, Image.SCALE_SMOOTH);
                    }
                }
                lblImage.setIcon(new ImageIcon(image));
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private JLabel lblImage;
        // End of variables declaration//GEN-END:variables

    }
}
