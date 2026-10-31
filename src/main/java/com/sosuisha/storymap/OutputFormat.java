package com.sosuisha.storymap;

import java.util.Locale;
import java.util.Objects;

/**
 * An output format of the story map.
 */
// Not named Format: Maven turns each <format> element of <formats> into the class Format of
// the mojo's package, if there is one, and the plain string would fail to convert.
public enum OutputFormat {
    /** A standalone HTML page. */
    HTML("html"),
    /** An SVG document. */
    SVG("svg"),
    /** A PNG image. */
    PNG("png");

    private final String extension;

    OutputFormat(String extension) {
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
    public static OutputFormat fromName(String name) {
        Objects.requireNonNull(name, "name");
        for (OutputFormat format : values()) {
            if (format.extension.equals(name.trim().toLowerCase(Locale.ROOT))) { return format; }
        }
        throw new IllegalArgumentException("Unknown format: " + name + ". Use html, svg, or png.");
    }
}
