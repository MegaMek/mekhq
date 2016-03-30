package mekhq.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

class EventListener {
    private final Object handler;
    private final Method method;
    private final Class<? extends HQEvent<?>> eventType;
    private final Subscribe info;

    public EventListener(Object handler, Method method, Class<? extends HQEvent<?>> eventType) {
        this.handler = Objects.requireNonNull(handler);
        this.method = Objects.requireNonNull(method);
        this.eventType = Objects.requireNonNull(eventType);
        this.info = method.getAnnotation(Subscribe.class);
    }
    
    public void trigger(HQEvent<?> event) {
        if(!event.isCancellable() || !event.isCancelled()) {
            try {
                method.invoke(handler, event);
            } catch(IllegalAccessException e) {
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                e.printStackTrace();
            } catch(InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
    
    public int getPriority() {
        return info.priority();
    }

    public Class<? extends HQEvent<?>> getEventType() {
        return eventType;
    }
}
