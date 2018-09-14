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

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Convenience class to process the children (elements and attributes) of an xml element.
 * <p>
 * Unless otherwise noted, all methods respect document-order.
 */
public class DomProcessor {

    private static final DomProcessor EMPTY = new DomProcessor(Optional.empty(), Collections.emptyMap());
    
    /**
     * @return an empty processor, as if {@link #at(Element)} was called
     *         for a leaf element with no attributes
     */
    public static DomProcessor empty() {
        return EMPTY;
    }

    /**
     * @return a processor for the children of the given element
     */
    public static DomProcessor at(Element parent) {
        Map<String,List<Element>> children = new HashMap<>();
        NodeList childNodes = parent.getChildNodes();
        for (int x = 0; x < childNodes.getLength(); x++) {
            Node child = childNodes.item(x);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String tagName = child.getNodeName().toLowerCase();
                children.computeIfAbsent(tagName, k -> new ArrayList<>()).add((Element) child);
            }
        }
        return new DomProcessor(Optional.of(parent), children);
    }

    private DomProcessor(Optional<Element> parent, Map<String,List<Element>> childrenByTagName) {
        this.parent = parent;
        this.childrenByTagName = childrenByTagName;
    }

    private final Optional<Element> parent; 
    private final Map<String,List<Element>> childrenByTagName; // by lower case tag name

    /**
     * @return the only child element with the given tag name (case insensitive), if any
     * @throws DomProcessorException if multiple children match the given tag name
     */
    public Optional<Element> uniqueChildElement(String tagName) {
        List<Element> list = childrenByTagName.getOrDefault(tagName.toLowerCase(), Collections.emptyList());
        if (list.size() > 1) {
            String msg = parent.isPresent()
                       ? String.format("element '%s' has %s '%s' child elements (case insensitive) - no more than one is expected", parent.get().getNodeName(), list.size(), tagName.toLowerCase()) //$NON-NLS-1$
                       : String.format("element has %s '%s' child elements (case insensitive) - no more than one is expected", list.size(), tagName.toLowerCase()); //$NON-NLS-1$
            throw new DomProcessorException(msg);
        }
        return list.isEmpty()
             ? Optional.empty()
             : Optional.of(list.get(0));
    }

    /**
     * @return a processor for the child elements of the only child with the given
     *         name (case insensitive), or an empty processor if no such child exists
     * @throws DomProcessorException if multiple children match the given tag name
     */
    public DomProcessor child(String tagName) {
        Optional<Element> child = uniqueChildElement(tagName);
        return child.isPresent()
             ? DomProcessor.at(child.get())
             : DomProcessor.empty();
    }

    /**
     * @return the only child element with the given name (case insensitive), processed through the given parser function
     * @throws DomProcessorException if multiple children match the given tag name or an exception if thrown by the given parser function
     */
    public <R> R child(String tagName, ParserFunction<Element, R> parserFunction, R defaultValue) {
        Optional<Element> child = uniqueChildElement(tagName);
        if (child.isPresent()) {
            try {
                return parserFunction.apply(child.get());
            } catch (Throwable ex) {
                throw new DomProcessorException(ex);
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * @return the text value of the only child element with the given name (case insensitive), or the given default value
     * @throws DomProcessorException if multiple children match the given tag name
     */
    public String text(String tagName, String defaultValue) {
        return uniqueChildElement(tagName).map(Element::getTextContent).orElse(defaultValue);
    }

    /**
     * @return the text value of the only child element with the given name (case insensitive), processed through the given parser function
     * @throws DomProcessorException if multiple children match the given tag name or an exception if thrown by the given parser function
     */
    public <R> R text(String tagName, ParserFunction<String, R> parserFunction, R defaultValue) {
        return child(tagName, element -> parserFunction.apply(element.getTextContent()), defaultValue);
    }

    /**
     * @return a stream of all child elements
     */
    public Stream<Element> children() {
        if (!parent.isPresent()) {
            return Stream.empty();
        } else {
            NodeListList children = new NodeListList(parent.get().getChildNodes());
            return children.stream()
                           .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                           .map(Element.class::cast);
        }
    }

    /**
     * @return a stream of all child elements with the given tag name (case insensitive)
     */
    public Stream<Element> streamChildren(String tagName) {
        return childrenByTagName.getOrDefault(tagName.toLowerCase(), Collections.emptyList()).stream();
    }

    /**
     * @return a stream of all child elements with the given tag names (case insensitive)
     */
    public Stream<Element> children(String... tagNames) {
        Set<String> names = Stream.of(tagNames).map(String::toLowerCase).collect(toSet());
        return children().filter(e -> names.contains(e.getTagName().toLowerCase()));
    }

    /**
     * {@link java.util.function.Function} variant that is allowed to throw exceptions 
     */
    @FunctionalInterface
    public interface ParserFunction<A, R> {
        /**
         * Applies this function to the given argument.
         */
        public R apply(A a) throws Throwable;
    }

}

