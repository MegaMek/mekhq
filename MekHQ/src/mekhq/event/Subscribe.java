package mekhq.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to put on methods which wish to receive events.
 * <p>
 * A method annotated with this needs to have exactly one argument,
 * this being some subclass of {@link HQEvent}.
 * An instance of that class then needs to be registered with the event bus
 * via {@link EventBus#registerHandler(Object)} for it to work. The exact
 * name of the method is not important, and neither is how many of
 * such mathods are packed into a single class.
 * <p>
 * It's a good idea (but not required) to keep a reference to
 * the instance containing the event handlers yourself after registering it,
 * if only to avoid registering it multiple times.
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
public @interface Subscribe {
    /** Priority of the event handler, default 0 */
    public int priority() default 0;
}
