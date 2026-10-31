package com.sosuisha.storymap;

import java.util.Locale;
import java.util.Objects;

/**
 * An output format of the story map.
 */
public enum Format {
    /** A standalone HTML page. */
    HTML("html"),
    /** An SVG document. */
    SVG("svg"),
    /** A PNG image. */
    PNG("png");

    private final String extension;

    Format(String extension) {
        this.extension = extension;
    }

    /**
     * Returns the file name extension of the format, without the dot.
     *
     * @return the extension, such as {@code html}
     */
    public String extension() {
        return extension;
    }

    /**
     * Returns the format that has the given name. The case of the name does not matter.
     *
     * @param name the name of the format, such as {@code html} or {@code PNG}
     * @return the format
     * @throws NullPointerException if {@code name} is null
     * @throws IllegalArgumentException if no format has the name
     */
    public static Format fromName(String name) {
        Objects.requireNonNull(name, "name");
        for (Format format : values()) {
            if (format.extension.equals(name.trim().toLowerCase(Locale.ROOT))) { return format; }
        }
        throw new IllegalArgumentException("Unknown format: " + name + ". Use html, svg, or png.");
    }
}
