package com.sosuisha.storymap;

/**
 * Thrown when the native library fails to render a story map.
 */
public class RenderException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the detail message
     */
    public RenderException(String message) {
        super(message);
    }
}
