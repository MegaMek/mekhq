package mekhq.gui.preferences;

import mekhq.preferences.PreferenceElement;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;

public class JTablePreference extends PreferenceElement implements MouseListener {
    private WeakReference<JTable> weakRef;
    private int columnIndex;
    private SortOrder sortOrder;

    public JTablePreference(JTable table){
        super(table.getName());

        table.getTableHeader().addMouseListener(this);
        this.weakRef = new WeakReference<>(table);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JTable table = this.weakRef.get();

        if (table != null) {
            int uiIndex = table.getColumnModel().getColumnIndexAtX(e.getX());
            this.columnIndex = table.getColumnModel().getColumn(uiIndex).getModelIndex();

            for (RowSorter.SortKey key : table.getRowSorter().getSortKeys()) {
                if (key.getColumn() == this.columnIndex) {
                    this.sortOrder = key.getSortOrder();
                    break;
                }
            }
        }
    }

    @Override
    protected String getValue() {
        return this.columnIndex + "|" + this.sortOrder.toString();
    }

    @Override
    protected void protectedSetInitialValue(String value) {
        assert value != null && value.trim().length() > 0;

        JTable element = weakRef.get();
        if (element != null) {
            this.columnIndex = Integer.parseInt(value.substring(0, value.indexOf("|")));
            this.sortOrder = SortOrder.valueOf(value.substring(value.indexOf("|") + 1));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
