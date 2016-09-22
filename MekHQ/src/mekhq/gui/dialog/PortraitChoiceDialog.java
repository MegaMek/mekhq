/*
 * CamoChoiceDialog.java
 *
 * Created on October 1, 2009, 3:10 PM
 */

package mekhq.gui.dialog;

import java.awt.Color;
import java.awt.Component;
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

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

import megamek.common.Crew;
import megamek.common.util.DirectoryItems;
import megamek.common.util.EncodeControl;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PortraitChoiceDialog extends javax.swing.JDialog {

     /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/**
     * The categorized camo patterns.
     */
    private DirectoryItems portraits;
    private PortraitTableModel portraitModel = new PortraitTableModel();
    private String category;
    private String filename;
    private LinkedHashMap<String, String> iconMap;
    private PortraitTableMouseAdapter portraitMouseAdapter;
    private boolean force = false;
    private JButton btnCancel;
    private JButton btnSelect;
    private JComboBox<String> comboCategories;
    private JScrollPane scrPortrait;
    private JTable tablePortrait;


    /** Creates new form CamoChoiceDialog */
    public PortraitChoiceDialog(Frame parent, boolean modal, String category, String file, DirectoryItems portraits) {
        this(parent, modal, category, file, portraits, false);
    }


    /** Creates new form CamoChoiceDialog */
    public PortraitChoiceDialog(java.awt.Frame parent, boolean modal, String category, String file, DirectoryItems portraits, boolean force) {
        super(parent, modal);
        this.category = category;
        filename = file;
        portraitMouseAdapter = new PortraitTableMouseAdapter();
        this.portraits = portraits;
        this.force = force;
        // If we're doing forces, initialize the hashmap for use
        if (force) {
            iconMap = new LinkedHashMap<String, String>();
        }
        initComponents();
        fillTable((String) comboCategories.getSelectedItem());
        int rowIndex = 0;
        for(int i = 0; i < portraitModel.getRowCount(); i++) {
            if(((String) portraitModel.getValueAt(i, 0)).equals(filename)) {
                rowIndex = i;
                break;
            }
        }
        tablePortrait.setRowSelectionInterval(rowIndex, rowIndex);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        GridBagConstraints gbc;

        scrPortrait = new JScrollPane();
        tablePortrait = new JTable();
        comboCategories = new JComboBox<String>();
        btnSelect = new JButton();
        btnCancel = new JButton();
        JPanel portraitPanel = new JPanel();
        getContentPane().setLayout(new GridBagLayout());

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PortraitChoiceDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        portraitPanel.setLayout(new GridBagLayout());

        scrPortrait.setName("jScrollPane1"); // NOI18N

        tablePortrait.setModel(portraitModel);
        tablePortrait.setName("tablePortrait"); // NOI18N
        tablePortrait.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePortrait.setRowHeight(76);
        tablePortrait.getColumnModel().getColumn(0).setCellRenderer(portraitModel.getRenderer());
        tablePortrait.addMouseListener(portraitMouseAdapter);
        scrPortrait.setViewportView(tablePortrait);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        portraitPanel.add(scrPortrait, gbc);

        DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<String>();
        String match = null;
        categoryModel.addElement(Crew.ROOT_PORTRAIT);
        if (portraits != null) {
            Iterator<String> names = portraits.getCategoryNames();
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
        portraitPanel.add(comboCategories, gbc);
        
        if (force) {
            JTabbedPane tabbedPane = new JTabbedPane();
            JPanel layeredPanel = new JPanel();

            // Panel for Types (Frames are autoselected based on needs of the complete image)
            JScrollPane scrTypes = new JScrollPane();
            JTable tableTypes = new JTable();
            JPanel panelTypes = new JPanel();

            // Panel for Formations (Frames are autoselected based on needs of the complete image)
            JScrollPane scrFormations = new JScrollPane();
            JTable tableFormations = new JTable();
            JPanel panelFormations = new JPanel();

            // Panel for Adjustments (Frames are autoselected based on needs of the complete image)
            JScrollPane scrAdjustments = new JScrollPane();
            JTable tableAdjustments = new JTable();
            JPanel panelAdjustments = new JPanel();

            // Panel for Alphanumerics (Frames are autoselected based on needs of the complete image)
            JScrollPane scrAlphanumerics = new JScrollPane();
            JTable tableAlphanumerics = new JTable();
            JPanel panelAlphanumerics = new JPanel();

            // Panel for SpecialModifiers (Frames are autoselected based on needs of the complete image)
            JScrollPane scrSpecialModifiers = new JScrollPane();
            JTable tableSpecialModifiers = new JTable();
            JPanel panelSpecialModifiers = new JPanel();

            tabbedPane.addTab("Single Portrait", portraitPanel);
            tabbedPane.addTab("Layered Portrait", layeredPanel);

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
            // Add the portrait panel to the content pane
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            getContentPane().add(portraitPanel, gbc);
        }

        btnSelect.setText(resourceMap.getString("btnSelect.text")); // NOI18N
        btnSelect.setName("btnSelect"); // NOI18N
        btnSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSelectActionPerformed(evt);
            }
        });
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.5;
        portraitPanel.add(btnSelect, gbc);

        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.5;
        portraitPanel.add(btnCancel, gbc);

        pack();
    }

	private void btnCancelActionPerformed(ActionEvent evt) {
	    setVisible(false);
	}

	private void btnSelectActionPerformed(ActionEvent evt) {
	    category = portraitModel.getCategory();
	    if(tablePortrait.getSelectedRow() != -1) {
	        filename = (String) portraitModel.getValueAt(tablePortrait.getSelectedRow(), 0);
	    } else {
	        filename = Crew.PORTRAIT_NONE;
	    }
	    setVisible(false);
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

     private void fillTable(String category) {
        portraitModel.reset();
        portraitModel.setCategory(category);
        // Translate the "root camo" category name.
        Iterator<String> portraitNames;
        if (Crew.ROOT_PORTRAIT.equals(category)) {
            portraitModel.addPortrait(Crew.PORTRAIT_NONE);
            portraitNames = portraits.getItemNames(""); //$NON-NLS-1$
        } else {
            portraitNames = portraits.getItemNames(category);
        }

        // Get the camo names for this category.
        while (portraitNames.hasNext()) {
                portraitModel.addPortrait(portraitNames.next());
        }
        if(portraitModel.getRowCount() > 0) {
            tablePortrait.setRowSelectionInterval(0, 0);
        }
    }

     /**
        * A table model for displaying camos
     */
    public class PortraitTableModel extends AbstractTableModel {

        /**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private String[] columnNames;
        private String category;
        private ArrayList<String> names;
        private ArrayList<Image> images;

        public PortraitTableModel() {
            columnNames = new String[] {"Portraits"};
            category = Crew.ROOT_PORTRAIT;
            names = new ArrayList<String>();
            images = new ArrayList<Image>();
        }

        public int getRowCount() {
            return names.size();
        }

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

        public void addPortrait(String name) {
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

        public PortraitTableModel.Renderer getRenderer() {
            return new PortraitTableModel.Renderer(portraits);
        }


        public class Renderer extends PortraitPanel implements TableCellRenderer {

        	public Renderer(DirectoryItems portraits) {
				super(portraits);
			}

			private static final long serialVersionUID = -6025788865509594987L;

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

    public class PortraitTableMouseAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                if (tablePortrait.equals(evt.getSource())) {
                    int row = tablePortrait.rowAtPoint(evt.getPoint());
                    if(row < portraitModel.getRowCount()) {
                        category = portraitModel.getCategory();
                        filename = (String) portraitModel.getValueAt(row, 0);
                        setVisible(false);
                    }
                }
            }
        }
    }

    public class PortraitPanel extends JPanel {

        /**
    	 *
    	 */
    	private static final long serialVersionUID = -3724175393116586310L;
    	private DirectoryItems portraits;

        /** Creates new form CamoPanel */
        public PortraitPanel(DirectoryItems portraits) {
            this.portraits = portraits;
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

            // Try to get the portrait file.
            try {
                // Translate the root portrait directory name.
                if (Crew.ROOT_PORTRAIT.equals(category))
                    category = ""; //$NON-NLS-1$
                Image portrait = (Image) portraits.getItem(category, name);
                if(null != portrait) {
                    portrait = portrait.getScaledInstance(-1, 76, Image.SCALE_DEFAULT);
                }
                lblImage.setIcon(new ImageIcon(portrait));
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private JLabel lblImage;
        // End of variables declaration//GEN-END:variables

    }
}
