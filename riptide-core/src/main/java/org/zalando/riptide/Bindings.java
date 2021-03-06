package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class Bindings {

    Bindings() {
        // package private so we can trick code coverage
    }

    public static <A> PartialBinding<A> on(final A attribute) {
        return new PartialBinding<>(attribute);
    }

    /**
     * Creates an <i>any</i> condition for the given type. Note that this method is meant to be
     * used as a base for specialized factory methods, e.g. like {@link #anyStatus()}.
     *
     * @param type attribute type
     * @param <A>  generic attribute type
     * @return an any condition on the given attribute type
     * @see #any(TypeToken)
     * @see #anySeries()
     * @see #anyStatus()
     * @see #anyStatusCode()
     * @see #anyContentType()
     */
    public static <A> PartialBinding<A> any(final Class<A> type) {
        return any(TypeToken.of(type));
    }
    
    /**
     * Creates a wildcard condition for the given type. Note that this method is meant to be
     * used as a base for specialized factory methods, e.g. like {@link #anyStatus()}.
     * 
     * @param type attribute type
     * @param <A> generic attribute type
     * @return an any condition on the given attribute type
     * @see #any(Class) 
     * @see #anySeries() 
     * @see #anyStatus() 
     * @see #anyStatusCode() 
     * @see #anyContentType() 
     */
    public static <A> PartialBinding<A> any(@SuppressWarnings("UnusedParameters") final TypeToken<A> type) {
        return new PartialBinding<>(null);
    }

    public static PartialBinding<HttpStatus.Series> anySeries() {
        return any(HttpStatus.Series.class);
    }

    public static PartialBinding<HttpStatus> anyStatus() {
        return any(HttpStatus.class);
    }
    
    public static PartialBinding<Integer> anyStatusCode() {
        return any(Integer.class);
    }

    public static PartialBinding<MediaType> anyContentType() {
        return any(MediaType.class);
    }

}
