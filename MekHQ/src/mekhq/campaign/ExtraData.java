/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.DateTime;
import org.w3c.dom.Node;

import mekhq.MekHQ;

/**
 * Class for holding extra data/properties with free-form strings as keys.
 * <p>
 * Example usage:
 * <p>
 * - creating keys
 * <pre>
 * ExtraData.Key<Integer> INTKEY = new ExtraData.IntKey("int_key");
 * ExtraData.Key<Double> DOUBLEKEY = new ExtraData.DoubleKey("double_key");
 * ExtraData.Key<DateTime> DATEKEY = new ExtraData.DateKey("current date");
 * ExtraData.Key<Boolean> BOOLEANKEY = new ExtraData.BooleanKey("realy?");
 * ExtraData.Key<String> PLAIN_OLD_BORING_KEY = new ExtraData.StringKey("stuff");
 * </pre>
 * - setting and getting data
 * <pre>
 * ed.set(INTKEY, 75);
 * ed.set(DOUBLEKEY, 12.5);
 * ed.set(DATEKEY, new DateTime());
 * Integer intVal = ed.get(INTKEY));
 * Double doubleVal = ed.get(DOUBLEKEY));
 * DateTime date = ed.get(DATEKEY));
 * // the next one guarantees to not return null, but -1 if the value is not set
 * int anotherIntVal = ed.get(INTKEY, -1);
 * </pre>
 * - saving to XML and creating from XML
 * <pre>
 * ed.writeToXml(System.out);
 * ExtraData newEd = ExtraData.createFromXml(xmlNode);
 * </pre>
 */
@XmlRootElement(name="extraData")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExtraData {
    private static final Marshaller marshaller;
    private static final Unmarshaller unmarshaller;
    static {
        Marshaller m = null;;
        Unmarshaller u = null;
        try {
            JAXBContext context = JAXBContext.newInstance(ExtraData.class);
            m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            u = context.createUnmarshaller();
        } catch(Exception ex) {
            MekHQ.getLogger().log(ExtraData.class, "<init>", ex);
        }
        marshaller = m;
        unmarshaller = u;
    }

    private static final Map<Class<?>, StringAdapter<?>> ADAPTERS = new HashMap<>();
    static {
        ADAPTERS.put(String.class, new StringAdapter<String>() {
            @Override
            public String adapt(String str) { return str; }
        });
        ADAPTERS.put(Integer.class, new StringAdapter<Integer>() {
            @Override
            public Integer adapt(String str) { return Integer.valueOf(str); }
        });
        ADAPTERS.put(Double.class, new StringAdapter<Double>() {
            @Override
            public Double adapt(String str) { return Double.valueOf(str); }
        });
        ADAPTERS.put(Boolean.class, new StringAdapter<Boolean>() {
            @Override
            public Boolean adapt(String str) { return Boolean.valueOf(str); }
        });
        ADAPTERS.put(DateTime.class, new StringAdapter<DateTime>() {
            @Override
            public DateTime adapt(String str) { return new DateTime(str); }
        });
    }

    @XmlElement(name="map")
    @XmlJavaTypeAdapter(JAXBValueAdapter.class)
    private Map<Class<?>, Map<String, Object>> values = new HashMap<>();

    private Map<String, Object> getOrCreateClassMap(Class<?> cls) {
        return ExtraData.getOrCreateClassMap(values, cls);
    }

    /**
     * Set the given value for the given key.
     *
     * @return The previous value if there was one.
     */
    public <T> T set(Key<T> key, T value) {
        if(null == key) {
            return null;
        }
        Map<String, Object> map = getOrCreateClassMap(key.type);
        return key.type.cast(map.put(key.name, value));
    }

    /**
     * Set the given value parsed from the string for the given key, if possible.
     *
     * @return The previous value if there was one.
     */
    public <T> T setString(Key<T> key, String value) {
        if(null == key) {
            return null;
        }
        // Prevent unneeded loops and lookups for straight strings
        if(key.type == String.class) {
            Map<String, Object> map = getOrCreateClassMap(key.type);
            return key.type.cast(map.put(key.name, value));
        }
        return set(key, key.fromString(value));
    }

    /**
     * @return the value associated with the given key, or <code>null</code> if there isn't one
     */
    public <T> T get(Key<T> key) {
        if(!values.containsKey(key.type)) {
            return null;
        }
        return key.type.cast(values.get(key.type).get(key.name));
    }

    /**
     * @return the value associated with the given key, or the default value if there isn't one
     */
    public <T> T get(Key<T> key, T defaultValue) {
        T result = get(key);
        return (null != result) ? result : defaultValue;
    }

    public void writeToXml(Writer writer) {
        try {
            marshaller.marshal(this, writer);
        } catch(JAXBException e) {
            MekHQ.getLogger().log(getClass(), "writeToXml(Writer)", e);
        }
    }

    public void writeToXml(OutputStream os) {
        try {
            marshaller.marshal(this, os);
        } catch(JAXBException e) {
            MekHQ.getLogger().log(getClass(), "writeToXml(OutputStream)", e);
        }
    }

    public static ExtraData createFromXml(Node wn) {
        try {
            return (ExtraData) unmarshaller.unmarshal(wn);
        } catch(JAXBException e) {
            MekHQ.getLogger().log(ExtraData.class, "createFromXml(Node)", e);
            return null;
        }
    }

    public static ExtraData createFromXml(InputStream is) {
        try {
            return (ExtraData) unmarshaller.unmarshal(is);
        } catch(JAXBException e) {
            MekHQ.getLogger().log(ExtraData.class, "createFromXml(InputStream)", e);
            return null;
        }
    }

    private static Map<String, Object> getOrCreateClassMap(Map<Class<?>, Map<String, Object>> baseMap, Class<?> cls) {
        Map<String, Object> map = baseMap.get(cls);
        if(null == map) {
            map = new HashMap<>();
            baseMap.put(cls, map);
        }
        return map;
    }

    // XML marshalling/unmarshalling support classes and methods

    /**
     * Register an adapter translating from String to the given value.
     * Already existing adapters are not overwritten.
     */
    public static <T> void registerAdapter(Class<T> cls, StringAdapter<T> adapter) {
        if((null != cls) && (null != adapter) && !ADAPTERS.containsKey(cls)) {
            ADAPTERS.put(cls, adapter);
        }
    }

    private static <T> T adapt(Class<T> cls, String val) {
        if(!ADAPTERS.containsKey(cls)) {
            return null;
        }
        try {
            return cls.cast(ADAPTERS.get(cls).adapt(val));
        } catch(ClassCastException cce) {
            return null;
        }
    }

    private static <T> String toString(T val) {
        if(null == val) {
            return null;
        }
        if(!ADAPTERS.containsKey(val.getClass())) {
            return val.toString();
        }
        @SuppressWarnings("unchecked")
        StringAdapter<T> adapter = (StringAdapter<T>) ADAPTERS.get(val.getClass());
        return adapter.toString(val);
    }

    public static abstract class StringAdapter<T> {
        public abstract T adapt(String str);
        public String toString(T val) {
            return (null != val) ? val.toString() : null;
        }
    }

    private static class JAXBValueAdapter
        extends XmlAdapter<XmlValueListArray, Map<Class<?>, Map<String, Object>>> {
        @Override
        public Map<Class<?>, Map<String, Object>> unmarshal(XmlValueListArray v) throws Exception {
            if((null == v) || (null == v.list) || v.list.isEmpty()) {
                return null;
            }
            Map<Class<?>, Map<String, Object>> result = new HashMap<>();
            for(XmlValueList list : v.list) {
                if(null == list.type) {
                    continue;
                }
                Class<?> type = null;
                try {
                    type = Class.forName(list.type);
                } catch(ClassNotFoundException cnfe) {}
                if(null == type) {
                    continue;
                }
                Map<String, Object> map = ExtraData.getOrCreateClassMap(result, type);
                for(XmlValueEntry item : list.entries) {
                    if((null != item) && (null != item.key) && (null != item.value)) {
                        map.put(item.key, adapt(type, item.value));
                    }
                }
            }
            return result;
        }

        @Override
        public XmlValueListArray marshal(Map<Class<?>, Map<String, Object>> v) throws Exception {
            if((null == v) || v.isEmpty()) {
                return null;
            }
            ArrayList<XmlValueList> result = new ArrayList<>();
            for(Entry<Class<?>, Map<String, Object>> entry : v.entrySet()) {
                Map<String, Object> value = entry.getValue();
                if((null == value) || value.isEmpty()) {
                    continue;
                }
                XmlValueList val = new XmlValueList();
                val.type = entry.getKey().getName();
                val.entries = new ArrayList<>();
                for(Entry<String, Object> data : value.entrySet()) {
                    if(null != data.getValue()) {
                        XmlValueEntry newEntry = new XmlValueEntry();
                        newEntry.key = data.getKey();
                        newEntry.value = ExtraData.toString(data.getValue());
                        val.entries.add(newEntry);
                    }
                }
                if(!val.entries.isEmpty()) {
                    result.add(val);
                }
            }
            XmlValueListArray arrayResult = new XmlValueListArray();
            if(!result.isEmpty()) {
                arrayResult.list = result;
            }
            return arrayResult;
        }
    }

    private static class XmlValueListArray {
        public List<XmlValueList> list;
    }

    private static class XmlValueList {
        @XmlAttribute
        public String type;
        @XmlElement(name="entry")
        public List<XmlValueEntry> entries;
    }

    private static class XmlValueEntry {
        @XmlAttribute
        public String key;
        @XmlAttribute
        public String value;
    }

    // Predefined key types

    public static abstract class Key<T> {
        private final String name;
        private final Class<T> type;

        protected Key(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Class<T> getType() {
            return type;
        }

        public T fromString(String str) {
            return ExtraData.adapt(type, str);
        }

        public String toString(T val) {
            return (null != val) ? val.toString() : null;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public boolean equals(Object object) {
            if(this == object) {
                return true;
            }
            if((null == object) || (getClass() != object.getClass())) {
                return false;
            }
            @SuppressWarnings("unchecked")
            final Key<T> other = (Key<T>) object;
            return Objects.equals(name, other.name) && (type == other.type);
        }
    }

    /** A key referencing a String value */
    public static class StringKey extends Key<String> {
        public StringKey(String name) {
            super(name, String.class);
        }
    }

    /** A key referencing an Integer or int value */
    public static class IntKey extends Key<Integer> {
        public IntKey(String name) {
            super(name, Integer.class);
        }
    }

    /** A key referencing a Double or double value */
    public static class DoubleKey extends Key<Double> {
        public DoubleKey(String name) {
            super(name, Double.class);
        }
    }

    /** A key referencing a Boolean or boolean value */
    public static class BooleanKey extends Key<Boolean> {
        public BooleanKey(String name) {
            super(name, Boolean.class);
        }
    }

    /** A key referencing a joda-time DateTime value */
    public static class DateKey extends Key<DateTime> {
        public DateKey(String name) {
            super(name, DateTime.class);
        }
    }
}
