package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import megamek.common.util.EncodeControl;
import mekhq.campaign.parts.PartInUse;

public class PartsInUseTableModel extends DataTableModel {
    private static final long serialVersionUID = -7166100476703184175L;
    
    private static final DecimalFormat FORMATTER = new DecimalFormat();
    static {
        FORMATTER.setMaximumFractionDigits(3);
    }
    private static final String EMPTY_CELL = ""; //$NON-NLS-1$

    public final static int COL_PART = 0;
    public final static int COL_IN_USE = 1;
    public final static int COL_STORED = 2;
    public final static int COL_TONNAGE = 3;
    public final static int COL_IN_TRANSFER  = 4;
    public final static int COL_COST = 5;
    public final static int COL_BUTTON_BUY  = 6;
    public final static int COL_BUTTON_BUY_BULK  = 7;
    public final static int COL_BUTTON_GMADD  = 8;
    public final static int COL_BUTTON_GMADD_BULK  = 9;

    private ResourceBundle resourceMap;

    public PartsInUseTableModel () {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PartsInUseTableModel", new EncodeControl()); //$NON-NLS-1$
        data = new ArrayList<PartInUse>();
    }
    
    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COL_BUTTON_GMADD_BULK + 1;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
        case COL_PART:
            return resourceMap.getString("part.heading"); //$NON-NLS-1$
        case COL_IN_USE:
            return resourceMap.getString("inUse.heading"); //$NON-NLS-1$
        case COL_STORED:
            return resourceMap.getString("stored.heading"); //$NON-NLS-1$
        case COL_TONNAGE:
            return resourceMap.getString("storedTonnage.heading"); //$NON-NLS-1$
        case COL_IN_TRANSFER:
            return resourceMap.getString("ordered.heading"); //$NON-NLS-1$
        case COL_COST:
            return resourceMap.getString("cost.heading"); //$NON-NLS-1$
        default:
            return EMPTY_CELL;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        PartInUse piu = getPartInUse(row);
        switch(column) {
            case COL_PART:
                return piu.getDescription();
            case COL_IN_USE:
                return FORMATTER.format(piu.getUseCount());
            case COL_STORED:
                return (piu.getStoreCount() > 0) ? FORMATTER.format(piu.getStoreCount()) : EMPTY_CELL;
            case COL_TONNAGE:
                return (piu.getStoreTonnage() > 0) ? FORMATTER.format(piu.getStoreTonnage()) : EMPTY_CELL;
            case COL_IN_TRANSFER:
                if( piu.getTransferCount() > 0 && piu.getPlannedCount() <= 0 ) {
                    return FORMATTER.format(piu.getTransferCount());
                } else if( piu.getPlannedCount() > 0 ) {
                    return String.format("%s [+%s]", //$NON-NLS-1$
                        FORMATTER.format(piu.getTransferCount()), FORMATTER.format(piu.getPlannedCount()));
                } else {
                    return EMPTY_CELL;
                }
            case COL_COST:
                return FORMATTER.format(piu.getCost());
            case COL_BUTTON_BUY:
                return resourceMap.getString("buy.text"); //$NON-NLS-1$
            case COL_BUTTON_BUY_BULK:
                return resourceMap.getString("buyInBulk.text"); //$NON-NLS-1$
            case COL_BUTTON_GMADD:
                return resourceMap.getString("add.text"); //$NON-NLS-1$
            case COL_BUTTON_GMADD_BULK:
                return resourceMap.getString("addInBulk.text"); //$NON-NLS-1$
            default:
                return EMPTY_CELL;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        switch(col) {
            case COL_BUTTON_BUY:
            case COL_BUTTON_BUY_BULK:
            case COL_BUTTON_GMADD:
            case COL_BUTTON_GMADD_BULK:
                return true;
            default:
                return false;
        }
    }
    
    public void setData(Set<PartInUse> data) {
        setData(new ArrayList<PartInUse>(data));
    }
    
    @SuppressWarnings("unchecked")
    public void updateRow(int row, PartInUse piu) {
        ((ArrayList<PartInUse>) data).set(row, piu);
        fireTableRowsUpdated(row, row);
    }

    public PartInUse getPartInUse(int row) {
        if((row < 0) || (row >= data.size())) {
            return null;
        }
        return (PartInUse) data.get(row);
    }
    
    public boolean isBuyable(int row) {
        return (row >= 0) && (row < data.size())
            && (null != ((PartInUse) data.get(row)).getPartToBuy());
    }

    public int getAlignment(int column) {
        switch(column) {
            case COL_PART:
                return SwingConstants.LEFT;
            case COL_IN_USE:
            case COL_STORED:
            case COL_TONNAGE:
            case COL_IN_TRANSFER:
            case COL_COST:
                return SwingConstants.RIGHT;
            default:
                return SwingConstants.CENTER;
        }
    }
    
    public int getPreferredWidth(int column) {
        switch(column) {
            case COL_PART:
                return 300;
            case COL_IN_USE:
            case COL_STORED:
            case COL_TONNAGE:
            case COL_IN_TRANSFER:
            case COL_COST:
                return 20;
            case COL_BUTTON_BUY:
                return 50;
            case COL_BUTTON_GMADD:
                return 70;
            case COL_BUTTON_BUY_BULK:
                return 80;
            default:
                return 100;
        }
    }
    
    public boolean hasConstantWidth(int col) {
        switch(col) {
            case COL_BUTTON_BUY:
            case COL_BUTTON_BUY_BULK:
            case COL_BUTTON_GMADD:
            case COL_BUTTON_GMADD_BULK:
                return true;
            default:
                return false;
        }
    }
    
    public int getWidth(int col) {
        switch(col) {
            case COL_BUTTON_BUY:
            case COL_BUTTON_BUY_BULK:
            case COL_BUTTON_GMADD:
            case COL_BUTTON_GMADD_BULK:
                // Calculate from button width, respecting style
                JButton btn = new JButton(getValueAt(0, col).toString());
                return btn.getPreferredSize().width;
            default:
                return Integer.MAX_VALUE;
        }
    }
    
    public PartsInUseTableModel.Renderer getRenderer() {
        return new PartsInUseTableModel.Renderer();
    }

    public static class Renderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1403740113670268591L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setHorizontalAlignment(((PartsInUseTableModel)table.getModel()).getAlignment(column));

            setForeground(Color.BLACK);
            if (isSelected) {
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
            } else {
                // tiger stripes
                if (row % 2 == 1) {
                    setBackground(new Color(230,230,230));
                } else {
                    setBackground(Color.WHITE);
                }
            }
            return this;
        }
    }
    
    public static class ButtonColumn extends AbstractCellEditor
        implements TableCellRenderer, TableCellEditor, ActionListener, MouseListener {

        private static final long serialVersionUID = 5632710519408125751L;
        
        private JTable table;
        private Action action;
        private Border originalBorder;
        private Border focusBorder;

        private JButton renderButton;
        private JButton editButton;
        private Object editorValue;
        private boolean isButtonColumnEditor;
        private boolean enabled;

        public ButtonColumn(JTable table, Action action, int column) {
            this.table = table;
            this.action = action;

            renderButton = new JButton();
            editButton = new JButton();
            editButton.setFocusPainted(false);
            editButton.addActionListener(this);
            originalBorder = editButton.getBorder();
            enabled = true;

            TableColumnModel columnModel = table.getColumnModel();
            columnModel.getColumn(column).setCellRenderer(this);
            columnModel.getColumn(column).setCellEditor(this);
            table.addMouseListener(this);
        }
        
        public Border getFocusBorder()
        {
            return focusBorder;
        }

        public void setFocusBorder(Border focusBorder)
        {
            this.focusBorder = focusBorder;
            editButton.setBorder(focusBorder);
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            editButton.setEnabled(enabled);
            renderButton.setEnabled(enabled);
        }
        
        @Override
        public Object getCellEditorValue() {
            return editorValue;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if(table.isEditing() && (this == table.getCellEditor())) {
                isButtonColumnEditor = true;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(isButtonColumnEditor && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            isButtonColumnEditor = false;
        }

        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = table.convertRowIndexToModel(table.getEditingRow());
            fireEditingStopped();

            //  Invoke the Action
            ActionEvent event = new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "" + row); //$NON-NLS-1$
            action.actionPerformed(event);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            boolean buyable = ((PartsInUseTableModel) table.getModel()).isBuyable(table.getRowSorter().convertRowIndexToModel(row));
            
            if(value == null) {
                editButton.setText(EMPTY_CELL);
                editButton.setIcon(null);
            } else if(value instanceof Icon) {
                editButton.setText(EMPTY_CELL);
                editButton.setIcon((Icon)value);
            } else {
                editButton.setText(value.toString());
                editButton.setIcon(null);
            }
            editButton.setEnabled(enabled && buyable);
            
            this.editorValue = value;
            return editButton;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            boolean buyable = ((PartsInUseTableModel) table.getModel()).isBuyable(table.getRowSorter().convertRowIndexToModel(row));
            
            if(isSelected && enabled && buyable) {
                renderButton.setForeground(table.getSelectionForeground());
                 renderButton.setBackground(table.getSelectionBackground());
            } else {
                renderButton.setForeground(table.getForeground());
                renderButton.setBackground(UIManager.getColor("Button.background")); //$NON-NLS-1$
            }

            if(hasFocus && enabled && buyable) {
                renderButton.setBorder(focusBorder);
            } else {
                renderButton.setBorder(originalBorder);
            }

            if(value == null)
            {
                renderButton.setText(EMPTY_CELL);
                renderButton.setIcon(null);
            } else if (value instanceof Icon) {
                renderButton.setText(EMPTY_CELL);
                renderButton.setIcon((Icon)value);
            } else {
                renderButton.setText(value.toString());
                renderButton.setIcon(null);
            }
            renderButton.setEnabled(enabled && buyable);
            
            return renderButton;
        }
    }
}
