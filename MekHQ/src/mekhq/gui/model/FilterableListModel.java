/*
 * Copyright (c) 2016 MegaMek team
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
package mekhq.gui.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class FilterableListModel<E> extends AbstractListModel<E> implements ListDataListener {
    private static final long serialVersionUID = -281985654755862982L;

    private ListModel<E> peerModel;
    private List<Integer> indices;
    private Predicate<E> filter;
    
    public FilterableListModel() {
        this(null, null);
    }
    
    public FilterableListModel(ListModel<E> model) {
        this(model, null);
    }
    
    public FilterableListModel(ListModel<E> model, Predicate<E> filter) {
        indices = new ArrayList<>();
        setModel(model);
        setFilter(filter);
    }
    
    public void setModel(ListModel<E> parent) {
        if((null == peerModel) || !peerModel.equals(parent)) {
            if(null != peerModel) {
                fireIntervalRemoved(this, 0, peerModel.getSize() - 1);
                peerModel.removeListDataListener(this);
            }

            peerModel = parent;
            indices.clear();
            if(null != peerModel) {
                peerModel.addListDataListener(this);
            }
            filterModel(true);
        }
    }

    public ListModel<E> getModel() {
        return peerModel;
    }
    
    public void setFilter(Predicate<E> value) {
        if((null == filter) || !filter.equals(value)) {
            filter = value;
            if(null != peerModel) {
                if(peerModel.getSize() > 0) {
                    fireIntervalRemoved(this, 0, peerModel.getSize() - 1);
                }
            }
            filterModel(true);
        }
    }

    public Predicate<E> getFilter() {
        return filter;
    }

    protected void filterModel(boolean fireEvent) {
        if((getSize() > 0) && fireEvent) {
            fireIntervalRemoved(this, 0, getSize() - 1);
        }
        indices.clear();

        if((null != filter) && (null != peerModel)) {
            IntStream.range(0, peerModel.getSize()).filter(i -> filter.test(peerModel.getElementAt(i)))
                .forEach(i -> {
                    indices.add(i);
                    if (fireEvent) {
                        fireIntervalAdded(this, getSize() - 1, getSize() - 1);
                    }
                });
        }
    }

    public void updateFilter() {
        if((null != filter) && (null != peerModel)) {
            IntStream.range(0, peerModel.getSize()).forEach(i -> {
                E value = peerModel.getElementAt(i);
                if (filter.test(value)) {
                    if (!indices.contains(i)) {
                        indices.add(i);
                        fireIntervalAdded(this, getSize() - 1, getSize() - 1);
                    }
                } else if (indices.contains(i)) {
                    int oldIndex = indices.indexOf(i);
                    indices.remove(oldIndex);
                    fireIntervalRemoved(this, oldIndex, oldIndex);
                }
            });
        }
    }
    
    @Override
    public int getSize() {
        return (null == filter) ? ((null == peerModel) ? 0 : peerModel.getSize()) : indices.size();
    }

    @Override
    public E getElementAt(int index) {
        if(null == filter) {
            if(null != peerModel) {
                return peerModel.getElementAt(index);
            }
        } else {
            return peerModel.getElementAt(indices.get(index));
        }
        return null;
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        if(null != peerModel) {
            if(null != filter) {
                int startIndex = Math.min(e.getIndex0(), e.getIndex1());
                int endIndex = Math.max(e.getIndex0(), e.getIndex1());
                for (int index = startIndex; index <= endIndex; index++) {
                    E value = peerModel.getElementAt(index);
                    if(filter.test(value)) {
                        indices.add(index);
                        int modelIndex = indices.indexOf(index);
                        fireIntervalAdded(this, modelIndex, modelIndex);
                    }
                }
            } else {
                fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
            }
        }
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        if(null != peerModel) {
            if(null != filter) {
                int oldRange = indices.size();
                filterModel(false);
                fireIntervalRemoved(this, 0, oldRange);
                if(indices.size() > 0) {
                    fireIntervalAdded(this, 0, indices.size());
                }
            } else {
                fireIntervalRemoved(this, e.getIndex0(), e.getIndex1());
            }
        }
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        filterModel(true);
    }

}
