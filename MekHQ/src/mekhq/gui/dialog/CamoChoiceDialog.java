/*
 * CamoChoiceDialog.java
 *
 * Created on October 1, 2009, 3:10 PM
 */
package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Player;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.util.EncodeControl;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.utilities.MekHqTableCellRenderer;
import mekhq.preferences.PreferencesNode;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CamoChoiceDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -6482869865801093811L;
	/**
     * The categorized camo patterns.
     */
    private CamoTableModel camoModel = new CamoTableModel();
    private String category;
    private String filename;
    private boolean clickedSelect;
    private int colorIndex;
    private CamoTableMouseAdapter camoMouseAdapter;

    /** Creates new form CamoChoiceDialog */
    public CamoChoiceDialog(JFrame parent, boolean modal, String category, String file, int color) {
        super(parent, modal);
        this.category = category;
        filename = file;
        colorIndex = color;
        clickedSelect = false;
        camoMouseAdapter = new CamoTableMouseAdapter();
        initComponents();
        fillTable((String) comboCategories.getSelectedItem());
        int rowIndex = 0;
        for (int i = 0; i < camoModel.getRowCount(); i++) {
            if (camoModel.getValueAt(i, 0).equals(filename)) {
                rowIndex = i;
                break;
            }
        }
        tableCamo.setRowSelectionInterval(rowIndex, rowIndex);
        setUserPreferences();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        scrCamo = new javax.swing.JScrollPane();
        tableCamo = new javax.swing.JTable();
        comboCategories = new javax.swing.JComboBox<>();
        btnSelect = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CamoChoiceDialog", new EncodeControl()); //$NON-NLS-1$

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("title.text"));
        getContentPane().setLayout(new java.awt.GridBagLayout());

        scrCamo.setName("scrCamo"); // NOI18N

        tableCamo.setModel(camoModel);
        tableCamo.setName("tableCamo"); // NOI18N
        tableCamo.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableCamo.setRowHeight(76);
        tableCamo.getColumnModel().getColumn(0).setCellRenderer(camoModel.getRenderer());
        tableCamo.addMouseListener(camoMouseAdapter);
        scrCamo.setViewportView(tableCamo);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(scrCamo, gridBagConstraints);

        DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<>();
        categoryModel.addElement(Camouflage.NO_CAMOUFLAGE);
        String match = null;
        if (MHQStaticDirectoryManager.getCamouflage().getItemNames("").hasNext()) {
            categoryModel.addElement(AbstractIcon.ROOT_CATEGORY);
            if (category.equals(AbstractIcon.ROOT_CATEGORY)) {
                match = category;
            }
        }
        Iterator<String> names = MHQStaticDirectoryManager.getCamouflage().getCategoryNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!"".equals(name)) {
                categoryModel.addElement(name);
                if (category.equals(name)) {
                    match = name;
                }
            }
        }

        if (null != match) {
            categoryModel.setSelectedItem(match);
        } else {
            categoryModel.setSelectedItem(Camouflage.NO_CAMOUFLAGE);
        }
        comboCategories.setModel(categoryModel);
        comboCategories.setName("comboCategories");
        comboCategories.addItemListener(this::comboCategoriesItemStateChanged);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(comboCategories, gridBagConstraints);

        btnSelect.setText(resourceMap.getString("btnSelect.text")); // NOI18N
        btnSelect.setName("btnSelect"); // NOI18N
        btnSelect.addActionListener(this::btnSelectActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(btnSelect, gridBagConstraints);

        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(this::btnCancelActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(btnCancel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CamoChoiceDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

	private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
	    setVisible(false);
	}//GEN-LAST:event_btnCancelActionPerformed

	private void btnSelectActionPerformed(java.awt.event.ActionEvent evt) {
	    category = camoModel.getCategory();
	    if (category.equals(Camouflage.NO_CAMOUFLAGE)) {
	        colorIndex = tableCamo.getSelectedRow();
	        filename = null;
	    } else if (tableCamo.getSelectedRow() != -1) {
	        filename = camoModel.getValueAt(tableCamo.getSelectedRow(), 0);
	    }
	    clickedSelect = true;
	    setVisible(false);
	}//GEN-LAST:event_btnSelectActionPerformed

	private void comboCategoriesItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comboCategoriesItemStateChanged
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

    public int getColorIndex() {
        return colorIndex;
    }

    public boolean clickedSelect() {
        return clickedSelect;
    }

     private void fillTable(String category) {
        camoModel.reset();
        camoModel.setCategory(category);
        if (Camouflage.NO_CAMOUFLAGE.equals(category)) {
            for (String color : Player.colorNames) {
                camoModel.addCamo(color);
            }
        } else {
            // Translate the "root camo" category name.
            Iterator<String> camoNames;
            if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
                camoNames = MHQStaticDirectoryManager.getCamouflage().getItemNames("");
            } else {
                camoNames = MHQStaticDirectoryManager.getCamouflage().getItemNames(category);
            }

            // Get the camo names for this category.
            while (camoNames.hasNext()) {
                camoModel.addCamo(camoNames.next());
            }
        }
        if (camoModel.getRowCount() > 0) {
            tableCamo.setRowSelectionInterval(0, 0);
        }
    }

     /**
        * A table model for displaying camos
     */
    public class CamoTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 6335604397167681830L;
		private String[] columnNames;
        private String category;
        private ArrayList<String> names;
        private ArrayList<Image> images;

        public CamoTableModel() {
            columnNames = new String[] {"Camos"};
            category = Camouflage.NO_CAMOUFLAGE;
            names = new ArrayList<>();
            images = new ArrayList<>();
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
            category = Camouflage.NO_CAMOUFLAGE;
            names = new ArrayList<>();
            images = new ArrayList<>();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public String getValueAt(int row, int col) {
            return names.get(row);
        }

        public Image getImageAt(int row) {
            return images.get(row);
        }

        public void setCategory(String c) {
            category = c;
        }

        public String getCategory() {
            return category;
        }

        public void addCamo(String name) {
            names.add(name);
            fireTableDataChanged();
        }

        @Override
        public Class<? extends String> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public CamoTableModel.Renderer getRenderer() {
            return new CamoTableModel.Renderer();
        }

        public class Renderer extends CamoPanel implements TableCellRenderer {

        	public Renderer() {
				super();
			}

			private static final long serialVersionUID = -7106605749246434963L;

			@Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                String name = getValueAt(row, column);
                setText(getValueAt(row, column));
                setImage(category, name, row);

                MekHqTableCellRenderer.setupTableColors(this, table, isSelected, hasFocus, row);
                return this;
            }
       }
    }

    public class CamoTableMouseAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                int row = tableCamo.rowAtPoint(e.getPoint());
                category = camoModel.getCategory();
                if (Camouflage.NO_CAMOUFLAGE.equals(category)) {
                    colorIndex = tableCamo.getSelectedRow();
                    filename = null;
                } else {
                    filename = camoModel.getValueAt(row, 0);
                }
                clickedSelect = true;
                setVisible(false);
            }
        }
    }

    public static class CamoPanel extends javax.swing.JPanel {
    	private static final long serialVersionUID = -4106360800407452822L;

        /** Creates new form CamoPanel */
        public CamoPanel() {
            initComponents();
        }

        private void initComponents() {
            java.awt.GridBagConstraints gridBagConstraints;

            lblImage = new javax.swing.JLabel();

            setName("Form");
            setLayout(new java.awt.GridBagLayout());

            lblImage.setText("");
            lblImage.setName("lblImage");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            add(lblImage, gridBagConstraints);
        }

        public void setText(String text) {
            lblImage.setText(text);
        }

        public void setImage(String category, String name, int colorInd) {
            if (null == category) {
                return;
            }

            if (Camouflage.NO_CAMOUFLAGE.equals(category)) {
                if (colorInd == -1) {
                    colorInd = 0;
                }
                BufferedImage tempImage = new BufferedImage(84, 72,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = tempImage.createGraphics();
                graphics.setColor(PlayerColors.getColor(colorInd));
                graphics.fillRect(0, 0, 84, 72);
                lblImage.setIcon(new ImageIcon(tempImage));
                return;
            }

            // Try to get the camo file.
            try {

                // Translate the root camo directory name.
                if (AbstractIcon.ROOT_CATEGORY.equals(category))
                    category = ""; //$NON-NLS-1$
                Image camo = (Image) MHQStaticDirectoryManager.getCamouflage().getItem(category, name);
                lblImage.setIcon(new ImageIcon(camo));
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
        private javax.swing.JLabel lblImage;
    }

    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnSelect;
    private javax.swing.JComboBox<String> comboCategories;
    private javax.swing.JScrollPane scrCamo;
    private javax.swing.JTable tableCamo;
}
