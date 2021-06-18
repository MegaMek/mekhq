/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.baseComponents;

import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * This is a generic model that sorts objects extending E based on the provided comparator, or using
 * Java's natural sort (which is NOT the same as a {@link NaturalOrderComparator}) if a comparator
 * is not provided.
 *
 * @param <E> The parameter class to sort based on. This class must implement {@link Comparable} if
 *           they are using Java's natural sort. Further, do NOT use this to sort Collections, as
 *           that will break this class.
 */
public class SortedComboBoxModel<E> extends DefaultComboBoxModel<E> {
    //region Variable Declarations
    private static final long serialVersionUID = 3728158859609424148L;
    private final Comparator<E> comparator;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Create an empty model that will use the natural sort order of any data added
     */
    public SortedComboBoxModel() {
        // We can't use this because of the varargs below, so just call super and initialize the
        // comparator as null
        super();
        this.comparator = null;
    }

    /**
     * Create a model that will use the natural sort order of any data added, starting with the
     * provided data.
     * @param elements the elements to add to this model
     */
    @SafeVarargs
    public SortedComboBoxModel(final E... elements) {
        this(null, elements);
    }

    /**
     * Create a model that will use the natural sort order of any data added, starting with the
     * provided data.
     * @param elements the elements to add to this model
     */
    public SortedComboBoxModel(final Collection<E> elements) {
        this(null, elements);
    }

    /**
     * Create an empty model using the specified Comparator.
     * @param comparator the comparator to use in sorting any data added.
     */
    public SortedComboBoxModel(final Comparator<E> comparator) {
        this(comparator, new ArrayList<>());
    }

    /**
     * Create a model using the specified Comparator and fills it with the provided data.
     * @param comparator the comparator to use in sorting any data added.
     * @param elements the elements to add to this model.
     */
    @SafeVarargs
    public SortedComboBoxModel(final Comparator<E> comparator, final E... elements) {
        this(comparator, List.of(elements));
    }

    /**
     * Create a model using the specified Comparator and fills it with the provided data, if any.
     * @param comparator the comparator to use in sorting any data added.
     * @param elements the elements to add to this model.
     */
    public SortedComboBoxModel(final Comparator<E> comparator, final Collection<E> elements) {
        super();
        this.comparator = comparator;
        setElements(elements);
    }
    //endregion Constructors

    //region Getters
    /**
     * @return the comparator being used, if any
     */
    public @Nullable Comparator<E> getComparator() {
        return comparator;
    }
    //endregion Getters

    /**
     * Adds a single element to the current model, sorted based on the declared sorting method. This
     * is identical to {@link #insertElementAt(Object, int)}, as we ignore the latter parameter in
     * that method.
     *
     * @param element the element to add
     */
    @Override
    public void addElement(final E element) {
        insertElementAt(element, 0);
    }

    /**
     * Adds a single element to the current model, sorted based on the declared sorting method.
     * @param element the element to add
     * @param index this parameter is ignored to ensure the data is sorted using the provided sort
     *              order
     */
    @Override
    @SuppressWarnings(value = "unchecked")
    public void insertElementAt(final E element, int index) {
        index = 0; // we want to ignore the provided index to ensure we keep the data sorted.
        final int size = getSize();
        //  Determine where to insert element to keep model in sorted order
        for (; index < size; index++) {
            if (getComparator() != null) {
                final E object = getElementAt(index);

                if (getComparator().compare(object, element) > 0) {
                    break;
                }
            } else {
                final Comparable<E> comparable = (Comparable<E>) getElementAt(index);
                if (comparable.compareTo(element) > 0) {
                    break;
                }
            }
        }

        super.insertElementAt(element, index);
    }

    /**
     * Adds the elements to the current model, sorted based on the declared sorting method. This
     * is identical to {@link #addAll(int, Collection)}, as we ignore the first parameter in
     * that method.
     *
     * @param elements the elements to add
     */
    @Override
    public void addAll(final Collection<? extends E> elements) {
        addAll(0, elements);
    }

    /**
     * Adds the elements to the current model, sorted based on the declared sorting method.
     *
     * @param index this parameter is ignored to ensure the data is sorted using the provided sort
     *              order
     * @param elements the elements to add
     */
    @Override
    public void addAll(final int index, final Collection<? extends E> elements) {
        for (E element : elements) {
            addElement(element);
        }
    }

    /**
     * This is used to clear and then set all elements based on a new input collection of elements,
     * retaining the selected item if possible, but otherwise reverting to a selected index of 0.
     *
     * @param elements the elements to replace the current elements with, which may be null to remove
     *                 all elements, although this is not recommended.
     */
    public void setElements(final @Nullable Collection<E> elements) {
        final E selectedElement = getSelectedItem();
        removeAllElements();

        if (elements == null) {
            return;
        }

        for (final E element : elements) {
            addElement(element);
        }

        if ((selectedElement != null) && elements.contains(selectedElement)) {
            setSelectedItem(selectedElement);
        } else if (elements.size() > 0) {
            setSelectedItem(getElementAt(0));
        }
    }

    /**
     * @return the selected item, cast to the proper class stored by the model
     */
    @Override
    @SuppressWarnings(value = "unchecked")
    public @Nullable E getSelectedItem() {
        final Object item = super.getSelectedItem();
        return (item == null) ? null : (E) item;
    }
}
