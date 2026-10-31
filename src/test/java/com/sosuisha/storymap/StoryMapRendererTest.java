package com.sosuisha.storymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StoryMapRendererTest {
    private static final String OUTLINE = """
                                          # Shop & Pay

                                          - Find items
                                            - Search by name
                                          - 支払う
                                            - カードで払う
                                          """;

    private static final byte[] PNG_SIGNATURE =
        {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

    @Nested
    @DisplayName("HTML")
    class Html {
        @Test
        @DisplayName("スタイルを含む、単独で開けるHTMLページになる")
        void html_is_a_standalone_page() {
            String html = renderText(OutputFormat.HTML);

            assertTrue(html.startsWith("<!DOCTYPE html>"));
            assertTrue(html.contains("<style>"));
            assertTrue(html.contains("Search by name"));
        }

        @Test
        @DisplayName("ページのタイトルは、マップのタイトルをエスケープしたものである")
        void page_title_is_the_escaped_map_title() {
            assertTrue(renderText(OutputFormat.HTML).contains("<title>Shop &amp; Pay</title>"));
        }

        @Test
        @DisplayName("日本語のカードが、文字化けせずに出力される")
        void japanese_text_survives() {
            assertTrue(renderText(OutputFormat.HTML).contains("カードで払う"));
        }

        @Test
        @DisplayName("カードの幅の上限を指定しなければ、160pxになる")
        void card_width_limit_is_160px_by_default() {
            assertTrue(renderText(OutputFormat.HTML).contains("--card-max-width: 160px;"));
        }

        @Test
        @DisplayName("カードの幅の上限を変えられる")
        void card_width_limit_can_be_changed() {
            byte[] html = StoryMapRenderer.render(OUTLINE, OutputFormat.HTML, 1.0f, 320f);

            assertTrue(
                new String(html, StandardCharsets.UTF_8).contains("--card-max-width: 320px;")
            );
        }
    }

    @Nested
    @DisplayName("画像")
    class Image {
        @Test
        @DisplayName("SVGは、SVG文書になる")
        void svg_is_an_svg_document() {
            String svg = renderText(OutputFormat.SVG);

            assertTrue(svg.startsWith("<svg "));
            assertTrue(svg.endsWith("</svg>"));
        }

        @Test
        @DisplayName("PNGは、PNG画像になる")
        void png_is_a_png_image() {
            byte[] png = StoryMapRenderer.render(OUTLINE, OutputFormat.PNG, 1.0f);

            assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(png, PNG_SIGNATURE.length));
        }

        @Test
        @DisplayName("PNGの幅は、倍率に比例する")
        void png_width_follows_the_scale() {
            int width = pngWidth(StoryMapRenderer.render(OUTLINE, OutputFormat.PNG, 1.0f));
            int doubled = pngWidth(StoryMapRenderer.render(OUTLINE, OutputFormat.PNG, 2.0f));

            // The width is rounded up after scaling, so it may be one pixel off
            assertEquals(2 * width, doubled, 1);
        }

        @Test
        @DisplayName("カードの幅の上限を狭くすると、長い文のカードが折り返され、画像が狭くなる")
        void narrower_card_width_limit_makes_a_narrower_image() {
            String outline =
                "- A card with a rather long text that does not fit in a narrow card\n";

            int narrow = pngWidth(StoryMapRenderer.render(outline, OutputFormat.PNG, 1.0f, 120f));
            int wide = pngWidth(StoryMapRenderer.render(outline, OutputFormat.PNG, 1.0f, 480f));

            assertTrue(narrow < wide);
        }

        @Test
        @DisplayName("大きすぎる画像は、描画の失敗になる")
        void too_large_image_fails() {
            assertThrows(
                RenderException.class,
                () -> StoryMapRenderer.render(OUTLINE, OutputFormat.PNG, 1_000_000f)
            );
        }
    }

    @ParameterizedTest(name = "倍率 {0}")
    @ValueSource(floats = {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY})
    @DisplayName("倍率は、正の有限の数でなければならない")
    void scale_must_be_positive_and_finite(float scale) {
        assertThrows(
            IllegalArgumentException.class,
            () -> StoryMapRenderer.render(OUTLINE, OutputFormat.PNG, scale)
        );
    }

    @ParameterizedTest(name = "上限 {0}")
    @ValueSource(floats = {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY})
    @DisplayName("カードの幅の上限は、正の有限の数でなければならない")
    void card_width_limit_must_be_positive_and_finite(float maxCardWidth) {
        assertThrows(
            IllegalArgumentException.class,
            () -> StoryMapRenderer.render(OUTLINE, OutputFormat.HTML, 1.0f, maxCardWidth)
        );
    }

    private static String renderText(OutputFormat format) {
        return new String(StoryMapRenderer.render(OUTLINE, format, 1.0f), StandardCharsets.UTF_8);
    }

    // The IHDR chunk starts with the width, at byte 16 of the file
    private static int pngWidth(byte[] png) {
        return ByteBuffer.wrap(png, 16, 4).getInt();
    }
}
