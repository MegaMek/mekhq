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

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Player;
import megamek.common.util.fileUtils.DirectoryItems;
import megamek.common.util.EncodeControl;
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
    private DirectoryItems camos;
    private CamoTableModel camoModel = new CamoTableModel();
    private String category;
    private String filename;
    private boolean clickedSelect;
    private int colorIndex = -1;
    private CamoTableMouseAdapter camoMouseAdapter;


    /** Creates new form CamoChoiceDialog */
    public CamoChoiceDialog(java.awt.Frame parent, boolean modal, String category, String file, int color, DirectoryItems camos) {
        super(parent, modal);
        this.category = category;
        this.camos = camos;
        filename = file;
        colorIndex = color;
        clickedSelect = false;
        camoMouseAdapter = new CamoTableMouseAdapter();
        initComponents();
        fillTable((String) comboCategories.getSelectedItem());
        int rowIndex = 0;
        for(int i = 0; i < camoModel.getRowCount(); i++) {
            if(((String) camoModel.getValueAt(i, 0)).equals(filename)) {
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
        comboCategories = new javax.swing.JComboBox<String>();
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

        DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<String>();
        categoryModel.addElement(Player.NO_CAMO);
        String match = null;
        if (camos != null) {
            if (camos.getItemNames("").hasNext()) { //$NON-NLS-1$
                categoryModel.addElement(Player.ROOT_CAMO);
                if(category.equals(Player.ROOT_CAMO)) {
                    match = Player.ROOT_CAMO;
                }
            }
            Iterator<String> names = camos.getCategoryNames();
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
            categoryModel.setSelectedItem(Player.NO_CAMO);
        }
        comboCategories.setModel(categoryModel);
        comboCategories.setName("comboCategories"); // NOI18N
        comboCategories.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comboCategoriesItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(comboCategories, gridBagConstraints);

        btnSelect.setText(resourceMap.getString("btnSelect.text")); // NOI18N
        btnSelect.setName("btnSelect"); // NOI18N
        btnSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(btnSelect, gridBagConstraints);

        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
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

	private void btnSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectActionPerformed
	    category = camoModel.getCategory();
	    if(category.equals(Player.NO_CAMO)) {
	        colorIndex = tableCamo.getSelectedRow();
	        filename = null;
	    }
	    else if(tableCamo.getSelectedRow() != -1) {
	        filename = (String) camoModel.getValueAt(tableCamo.getSelectedRow(), 0);
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
        if(Player.NO_CAMO.equals(category)) {
            for (String color : Player.colorNames) {
                camoModel.addCamo(color);
            }
        } else {
            // Translate the "root camo" category name.
            Iterator<String> camoNames;
            if (Player.ROOT_CAMO.equals(category)) {
                camoNames = camos.getItemNames(""); //$NON-NLS-1$
            } else {
                camoNames = camos.getItemNames(category);
            }

            // Get the camo names for this category.
            while (camoNames.hasNext()) {
                camoModel.addCamo(camoNames.next());
            }
        }
        if(camoModel.getRowCount() > 0) {
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
            category = Player.NO_CAMO;
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
            category = Player.NO_CAMO;
            names = new ArrayList<String>();
            images = new ArrayList<Image>();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

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
            return new CamoTableModel.Renderer(camos);
        }

        public class Renderer extends CamoPanel implements TableCellRenderer {

        	public Renderer(DirectoryItems camos) {
				super(camos);
			}

			private static final long serialVersionUID = -7106605749246434963L;

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String name = getValueAt(row, column).toString();
                setText(getValueAt(row, column).toString());
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
                if(category.equals(Player.NO_CAMO)) {
                    colorIndex = tableCamo.getSelectedRow();
                    filename = null;
                }
                else {
                    filename = (String) camoModel.getValueAt(row, 0);
                }
                clickedSelect = true;
                setVisible(false);
            }
        }
    }

    public class CamoPanel extends javax.swing.JPanel {
    	private static final long serialVersionUID = -4106360800407452822L;
    	private DirectoryItems camos;

        /** Creates new form CamoPanel */
        public CamoPanel(DirectoryItems camos) {
            this.camos = camos;
            initComponents();
        }

        private void initComponents() {
            java.awt.GridBagConstraints gridBagConstraints;

            lblImage = new javax.swing.JLabel();

            setName("Form"); // NOI18N
            setLayout(new java.awt.GridBagLayout());

            lblImage.setText(""); // NOI18N
            lblImage.setName("lblImage"); // NOI18N
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            add(lblImage, gridBagConstraints);
        }// </editor-fold>//GEN-END:initComponents

        public void setText(String text) {
            lblImage.setText(text);
        }

        //public void setImage(Image image) {
          //  lblImage.setIcon(new ImageIcon(image));
        //}

        public void setImage(String category, String name, int colorInd) {

            if (null == category) {
                return;
            }

            if(Player.NO_CAMO.equals(category)) {
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
                if (Player.ROOT_CAMO.equals(category))
                    category = ""; //$NON-NLS-1$
                Image camo = (Image) camos.getItem(category, name);
                lblImage.setIcon(new ImageIcon(camo));
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "setImage", e);
            }
        }
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel lblImage;
        // End of variables declaration//GEN-END:variables
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnSelect;
    private javax.swing.JComboBox<String> comboCategories;
    private javax.swing.JScrollPane scrCamo;
    private javax.swing.JTable tableCamo;
    // End of variables declaration//GEN-END:variables



}
