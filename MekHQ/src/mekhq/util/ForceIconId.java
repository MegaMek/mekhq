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
package mekhq.util;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.text.StringEscapeUtils.escapeXml10;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Stream;

import org.w3c.dom.Element;

import mekhq.MekHqXmlUtil;
import mekhq.util.dom.DomProcessor;

/**
 * Immutable class that identifies a force icon (whether a single image or a
 * layered icon).
 */
public class ForceIconId implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * This is for internal use only (ok, tests too): in general, the assumption
     * is that a ForceIconId is NEVER empty.
     */
    static final ForceIconId EMPTY = new ForceIconId(emptySet());

    private static final ForceIconId DEFAULT_FRAME = new ForceIconId(singleton(ImageId.of("Pieces/Frames/","Frame.png"))); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * @return a {@linkplain ForceIconId} with a blank frame
     */
    public static ForceIconId defatultFrame() {
        return DEFAULT_FRAME;
    }

    /**
     * Convenience for {@code ofImage(ImageId.of(category, imageName))}.
     */
    public static ForceIconId of(String category, String imageName) {
        return new ForceIconId(singleton(ImageId.of(category, imageName)));
    }

    /**
     * @return a {@linkplain ForceIconId} with the given image as the only layer.
     */
    public static ForceIconId of(ImageId baseImage) {
        return new ForceIconId(singleton(requireNonNull(baseImage)));
    }

    /**
     * @return a {@linkplain ForceIconId} with the given image as the only layer.
     */
    public static ForceIconId of(ImageId... layers) {
        Set<ImageId> s = new LinkedHashSet<>();
        for (ImageId l : layers) {
            s.add(requireNonNull(l));
        }
        return new ForceIconId(s);
    }

    private ForceIconId(Set<ImageId> layers) {
        this.layers = layers;
    }

    private Set<ImageId> layers; // note layers are NOT rendered in order

    /**
     * Iterates the layers of this icon.
     * <p>
     * Please note layers are not necessarily rendered in the order they appear
     * here.
     */
    public Stream<ImageId> layers() {
        return layers.stream();
    }

    /**
     * Returns a new {@linkplain ForceIconId} that has the given layers on top
     * of the current ones.
     * <p>
     * Please note layers are not necessarily rendered in the order they appear
     * here.
     */
    public ForceIconId withAddedLayers(ImageId... additionalImages) {
        if (additionalImages.length == 0) {
            return this;
        } else {
            Set<ImageId> s = new LinkedHashSet<>(layers);
            for (ImageId l : additionalImages) {
                s.add(requireNonNull(l));
            }
            return new ForceIconId(s);
        }
    }

    @Override
    public int hashCode() {
        return layers.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ForceIconId other = (ForceIconId) obj;
        return layers.equals(other.layers);
    }

    /**
     * Builds a {@linkplain ForceIconId} from the "old" information one can find in .cpnx files
     */
    @SuppressWarnings("nls")
    public static Optional<ForceIconId> cleanupLegacyIconId(String iconCategory, String iconFileName) {

        // This cleanup logic is derived from code originally in
        // mekhq.IconPackage.buildForceIcon() (look it up at tag 0.45.1)

        String category = iconCategory;
        String fileName = iconFileName;

        if ("-- General --".equals(category)) {
            // Crew.ROOT_PORTRAIT or Force.ROOT_ICON ("-- General --") is what
            // the UI shows for the root category and should really not make it
            // into the model.
            category = "";
        }

        if ("Layered".equals(category)) {
            // Force.ROOT_LAYERED ("Layered") was used as a magic value to
            // indicate category and filename should be disregarded and iconMap
            // used instead.
            category = null;
            fileName = null;
        }

        if ("None".equals(fileName)) {
            // Force.ICON_NONE or Crew.PORTRAIT_NONE ("None") is a magic value
            // to signify there is no picture. Let's replace it with null.
            category = null;
            fileName = null;
        }

        if ("".equals(category) && "empty.png".equals(fileName)) {
            // this is the default/fallback icon and should not have been saved
            category = null;
            fileName = null;
        }

        return (category == null) || (fileName == null)
             ? Optional.empty()
             : Optional.of(ForceIconId.of(category, fileName));
    }

    /**
     * Builds a {@linkplain ForceIconId} from the "old" information one can find in .cpnx files
     */
    public static Optional<ForceIconId> cleanupLegacyIconId(String iconCategory, String iconFileName, Map<String, Vector<String>> iconMap) {

        Optional<ForceIconId> base = cleanupLegacyIconId(iconCategory, iconFileName);

        ImageId[] layers = iconMap.entrySet()
                                  .stream()
                                  .flatMap( entry -> entry.getValue().stream().map(img -> ImageId.of(entry.getKey(), img)) )
                                  .toArray(l -> new ImageId[l]);

        return base.isPresent() || (layers.length != 0)
             ? Optional.of(base.orElse(EMPTY).withAddedLayers(layers))
             : Optional.empty();
    }

    /**
     * Reads information on a force icon from XML, handling both the format used
     * up to 0.45.1 and the "new" one introduced with 0.45.2.
     * <p>
     * For reference, up to 0.45.1, force icons were saved like:
     * <pre>{@literal
     * <iconCategory>Layered</iconCategory>
     * <iconFileName>None</iconFileName>
     * <iconHashMap>
     *     <iconentry key="Pieces/Frames/">
     *         <value name="Frame.png"/>
     *     </iconentry>
     *     <iconentry key="Pieces/Type/">
     *         <value name="BattleMech.png"/>
     *     </iconentry>
     * </iconHashMap>
     * }</pre>
     * from 0.45.2, there is no distinction between layered and
     * non-layered icons and layers are saved like:
     * <pre>{@literal
     * <forceIcon>
     *     <layer category="category1" filename="filename1" />
     *     <layer category="category2" filename="filename2" />
     * </forceIcon>
     * }</pre>
     */
    public static Optional<ForceIconId> fromXML(Element parent) {
        return fromXML(DomProcessor.at(parent));
    }

    /**
     * @see #fromXML(Element)
     */
    @SuppressWarnings("nls")
    public static Optional<ForceIconId> fromXML(DomProcessor atParent) {

        List<ImageId> layers = new ArrayList<>();

        atParent.child("iconHashMap").children().forEach(iconentry -> {
            String category = iconentry.getAttribute("key");
            DomProcessor.at(iconentry).children().forEach(value -> {
                layers.add(ImageId.of(category, value.getAttribute("name")));
            });
        });

        atParent.child("forceIcon").children().forEach(layer -> {
            layers.add(ImageId.of(layer.getAttribute("category"), layer.getAttribute("filename")));
        });

        Optional<ForceIconId> base = cleanupLegacyIconId(atParent.text("iconCategory", null), atParent.text("iconFileName", null));

        return base.isPresent() || !layers.isEmpty()
             ? Optional.of(base.orElse(EMPTY).withAddedLayers(layers.toArray(new ImageId[layers.size()])))
             : Optional.empty();

    }

    /**
     * Prints to the given {@linkplain PrintWriter} something like:
     *
     * <pre>{@literal
     * <forceIcon>
     *     <layer category="category1" filename="filename1" />
     *     <layer category="category2" filename="filename2" />
     * </forceIcon>
     * }</pre>
     *
     * @see #fromXML(Element)
     */
    @SuppressWarnings("nls")
    public void printXML(PrintWriter out, int indent) {
        String indent0 = MekHqXmlUtil.indentStr(indent);
        String indent1 = MekHqXmlUtil.indentStr(indent + 1);
        out.println(indent0 + "<forceIcon>");
        layers().forEach(imageId -> {
            String tag = String.format( "<layer category=\"%s\" filename=\"%s\" />",
                                        escapeXml10(imageId.getCategory()),
                                        escapeXml10(imageId.getFileName()) );
            out.println(indent1 + tag);
        });
        out.println(indent0 + "</forceIcon>");
    }

}
