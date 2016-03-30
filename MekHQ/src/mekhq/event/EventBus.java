package mekhq.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class EventBus {
    private static final Object INSTANCE_LOCK = new Object[0];
    private static final Object REGISTER_LOCK = new Object[0];
    
    private static EventBus instance;
    private static final EventSorter EVENT_SORTER = new EventSorter();
    
    private ConcurrentHashMap<Object, List<EventListener>> handlerMap
        = new ConcurrentHashMap<Object, List<EventListener>>();
    private ConcurrentHashMap<Class<? extends HQEvent<?>>, List<EventListener>> eventMap
        = new ConcurrentHashMap<Class<? extends HQEvent<?>>, List<EventListener>>();
    
    public static EventBus getInstance() {
        synchronized(INSTANCE_LOCK) {
            if(null == instance) {
                instance = new EventBus();
            }
        }
        return instance;
    }
    
    public static void registerHandler(Object handler) {
        getInstance().register(handler);
    }
    
    public static void unregisterHandler(Object handler) {
        getInstance().unregister(handler);
    }
    
    private EventBus() {}
    
    private List<Class<?>> getClasses(Class<?> leaf) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        while(null != leaf) {
            result.add(leaf);
            leaf = leaf.getSuperclass();
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public void register(Object handler) {
        if(handlerMap.containsKey(handler)) {
            return;
        }
        
        for(Method method : handler.getClass().getMethods()) {
            for(Class<?> cls : getClasses(handler.getClass())) {
                try {
                    Method realMethod = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    if(realMethod.isAnnotationPresent(Subscribe.class)) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if(parameterTypes.length != 1) {
                            throw new IllegalArgumentException(
                                String.format("@Subscribe annotation requires single-argument method; %s has %d", //$NON-NLS-1$
                                    method, parameterTypes.length));
                        }
                        Class<?> eventType = parameterTypes[0];
                        if(!HQEvent.class.isAssignableFrom(eventType)) {
                            throw new IllegalArgumentException(
                                String.format("@Subscribe annotation of %s requires the argument type to be some subtype of HQEvent, not %s", //$NON-NLS-1$
                                    method, eventType));
                        }
                        internalRegister(handler, realMethod, (Class<? extends HQEvent<?>>) eventType);
                    }
                } catch (NoSuchMethodException e) {
                    // ignore
                }
            }
        }
    }

    private void internalRegister(Object handler, Method method, Class<? extends HQEvent<?>> eventType) {
        synchronized(REGISTER_LOCK) {
            EventListener listener = new EventListener(handler, method, eventType);
            List<EventListener> handlerListeners = handlerMap.get(handler);
            if(null == handlerListeners) {
                handlerListeners = new ArrayList<EventListener>();
                handlerMap.put(handler, handlerListeners);
            }
            handlerListeners.add(listener);
            List<EventListener> eventListeners = eventMap.get(eventType);
            if(null == eventListeners) {
                eventListeners = new ArrayList<EventListener>();
                eventMap.put(eventType, eventListeners);
            }
            eventListeners.add(listener);
        }
    }
    
    public void unregister(Object handler) {
        synchronized(REGISTER_LOCK) {
            List<EventListener> listenerList = handlerMap.remove(handler);
            if(null != listenerList) {
                for(EventListener listener : listenerList) {
                    List<EventListener> eventListeners = eventMap.get(listener.getEventType());
                    if(null != eventListeners) {
                        eventListeners.remove(listener);
                    }
                }
            }
        }
    }
    
    /** @return true if the event was cancelled along the way */
    public boolean trigger(HQEvent<?> event) {
        List<EventListener> eventListeners = eventMap.get(event.getClass());
        if(null != eventListeners) {
            Collections.sort(eventListeners, EVENT_SORTER);
            for(EventListener listener : eventListeners) {
                listener.trigger(event);
            }
        }
        return event.isCancellable() ? event.isCancelled() : false;
    }
    
    private static class EventSorter implements Comparator<EventListener> {
        @Override
        public int compare(EventListener el1, EventListener el2) {
            // Highest to lowest, by priority
            return Integer.compare(el2.getPriority(), el1.getPriority());
        }
    }
}
