/*
 * MekHQ - Copyright (C) 2018 - The MekHQ Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekhq.util.dom;

import static java.util.Objects.requireNonNull;

import java.util.AbstractList;
import java.util.RandomAccess;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Adapts a {@linkplain NodeList} to a regular {@linkplain List}.
 */
class NodeListList extends AbstractList<Node> implements RandomAccess {

    public static NodeListList wrap(NodeList nl) {
        return new NodeListList(requireNonNull(nl));
    }
    
    public NodeListList(NodeList delegate) {
        this.delegate = delegate;
    }

    private final NodeList delegate;
    
    @Override
    public Node get(int index) {
        return delegate.item(index);
    }

    @Override
    public int size() {
        return delegate.getLength();
    }

}
