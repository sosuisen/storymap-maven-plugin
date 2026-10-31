package com.sosuisha.storymap;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Renders a Markdown outline as a user story map, with the storymap crate in the native library.
 */
public final class StoryMapRenderer {
    /** The maximum card width when none is given, in CSS pixels. */
    public static final float DEFAULT_MAX_CARD_WIDTH = 240f;

    private StoryMapRenderer() {}

    /**
     * Renders the outline in the format.
     *
     * @param outline the Markdown outline
     * @param format the output format
     * @param scale the size of the PNG image, where 1.0 is one pixel per CSS pixel. The other
     *        formats do not use it
     * @return the rendered bytes. HTML and SVG are UTF-8 text
     * @throws NullPointerException if {@code outline} or {@code format} is null
     * @throws IllegalArgumentException if {@code scale} is not a positive finite number
     * @throws RenderException if the native library fails, such as for an image that is too large
     * @throws UnsupportedOperationException if no native library is built for this platform
     */
    public static byte[] render(String outline, OutputFormat format, float scale) {
        return render(outline, format, scale, DEFAULT_MAX_CARD_WIDTH);
    }

    /**
     * Renders the outline in the format, with cards up to the given width.
     *
     * @param outline the Markdown outline
     * @param format the output format
     * @param scale the size of the PNG image, where 1.0 is one pixel per CSS pixel. The other
     *        formats do not use it
     * @param maxCardWidth the widest that a card may be, in CSS pixels. A longer text wraps
     * @return the rendered bytes. HTML and SVG are UTF-8 text
     * @throws NullPointerException if {@code outline} or {@code format} is null
     * @throws IllegalArgumentException if {@code scale} or {@code maxCardWidth} is not a positive
     *         finite number
     * @throws RenderException if the native library fails, such as for an image that is too large
     * @throws UnsupportedOperationException if no native library is built for this platform
     */
    public static byte[] render(String outline, OutputFormat format, float scale,
        float maxCardWidth) {
        Objects.requireNonNull(outline, "outline");
        Objects.requireNonNull(format, "format");
        if (!(Float.isFinite(scale) && scale > 0)) {
            throw new IllegalArgumentException(
                "The scale must be a positive finite number: " + scale
            );
        }
        if (!(Float.isFinite(maxCardWidth) && maxCardWidth > 0)) {
            throw new IllegalArgumentException(
                "The maximum card width must be a positive finite number: " + maxCardWidth
            );
        }
        try (Arena arena = Arena.ofConfined()) {
            byte[] bytes = outline.getBytes(StandardCharsets.UTF_8);
            MemorySegment input = arena.allocateFrom(JAVA_BYTE, bytes);
            long inputLength = bytes.length;
            MemorySegment outputLength = arena.allocate(Natives.SIZE);
            MemorySegment output = Natives.invoke(() -> switch (format) {
                case HTML -> (MemorySegment) Natives.RENDER_HTML
                    .invokeExact(input, inputLength, maxCardWidth, outputLength);
                case SVG -> (MemorySegment) Natives.RENDER_SVG
                    .invokeExact(input, inputLength, maxCardWidth, outputLength);
                case PNG -> (MemorySegment) Natives.RENDER_PNG.invokeExact(
                    input, inputLength, scale,
                    maxCardWidth, outputLength
                );
            });
            if (output.equals(MemorySegment.NULL)) {
                throw new RenderException(
                    "The native library failed to render the map as " + format
                );
            }
            long length = outputLength.get(Natives.SIZE, 0);
            try {
                return output.reinterpret(length).toArray(JAVA_BYTE);
            } finally {
                Natives.invoke(() -> {
                    Natives.FREE.invokeExact(output, length);
                    return output;
                });
            }
        }
    }

    // A holder class, so that the library is loaded on the first render, not when the plugin is
    // loaded
    private static final class Natives {
        // Every supported platform is 64-bit, so usize is a Java long
        static final ValueLayout.OfLong SIZE = JAVA_LONG;

        private static final SymbolLookup LIBRARY = NativeLibrary.load();
        private static final Linker LINKER = Linker.nativeLinker();

        static final MethodHandle RENDER_HTML = downcall(
            "storymap_render_html",
            FunctionDescriptor.of(ADDRESS, ADDRESS, SIZE, JAVA_FLOAT, ADDRESS)
        );
        static final MethodHandle RENDER_SVG = downcall(
            "storymap_render_svg",
            FunctionDescriptor.of(ADDRESS, ADDRESS, SIZE, JAVA_FLOAT, ADDRESS)
        );
        static final MethodHandle RENDER_PNG = downcall(
            "storymap_render_png",
            FunctionDescriptor.of(ADDRESS, ADDRESS, SIZE, JAVA_FLOAT, JAVA_FLOAT, ADDRESS)
        );
        static final MethodHandle FREE =
            downcall("storymap_free", FunctionDescriptor.ofVoid(ADDRESS, SIZE));

        private Natives() {}

        private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = LIBRARY.find(name)
                .orElseThrow(() -> new IllegalStateException("The native library has no " + name));
            return LINKER.downcallHandle(symbol, descriptor);
        }

        // invokeExact declares Throwable, but a downcall throws only what the JVM itself throws
        static MemorySegment invoke(Call call) {
            try {
                return call.run();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new AssertionError(e);
            }
        }
    }

    @FunctionalInterface
    private interface Call {
        MemorySegment run() throws Throwable;
    }
}
