package com.sosuisha.storymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RenderMojoTest {
    @TempDir
    Path folder;

    @Test
    @DisplayName("指定した形式ごとに、入力ファイルと同じ名前のファイルを書く")
    void each_format_is_written_after_the_input_name() throws Exception {
        RenderMojo mojo = mojo(writeOutline("storymap.md"), List.of("html", "svg", "png"));

        mojo.execute();

        assertEquals(
            Set.of("storymap.html", "storymap.svg", "storymap.png"),
            fileNames(mojo.outputDirectory.toPath())
        );
    }

    @Test
    @DisplayName("入力ファイルのアウトラインが、出力に描画される")
    void outline_of_the_input_is_rendered() throws Exception {
        RenderMojo mojo = mojo(writeOutline("storymap.md"), List.of("html"));

        mojo.execute();

        assertTrue(
            Files.readString(mojo.outputDirectory.toPath().resolve("storymap.html"))
                .contains("Task A1")
        );
    }

    @Test
    @DisplayName("出力先のフォルダーがなければ作る")
    void output_directory_is_created() throws Exception {
        RenderMojo mojo = mojo(writeOutline("storymap.md"), List.of("html"));
        mojo.outputDirectory = folder.resolve("target/storymap").toFile();

        mojo.execute();

        assertTrue(Files.isRegularFile(folder.resolve("target/storymap/storymap.html")));
    }

    @Test
    @DisplayName("拡張子のない入力ファイルは、その名前のまま出力ファイルの名前になる")
    void input_without_extension_keeps_its_name() throws Exception {
        RenderMojo mojo = mojo(writeOutline("STORYMAP"), List.of("html"));

        mojo.execute();

        assertEquals(Set.of("STORYMAP.html"), fileNames(mojo.outputDirectory.toPath()));
    }

    @Test
    @DisplayName("未知の形式を指定すると、ビルドが失敗する")
    void unknown_format_fails_the_build() throws IOException {
        RenderMojo mojo = mojo(writeOutline("storymap.md"), List.of("pdf"));

        assertThrows(MojoFailureException.class, mojo::execute);
    }

    @Test
    @DisplayName("倍率が正でなければ、ビルドが失敗する")
    void non_positive_scale_fails_the_build() throws IOException {
        RenderMojo mojo = mojo(writeOutline("storymap.md"), List.of("png"));
        mojo.scale = 0f;

        assertThrows(MojoFailureException.class, mojo::execute);
    }

    @Test
    @DisplayName("カードの幅の上限を、パラメーターで変えられる")
    void card_width_limit_follows_the_parameter() throws Exception {
        RenderMojo mojo = mojo(writeOutline("storymap.md"), List.of("html"));
        mojo.maxCardWidth = 320f;

        mojo.execute();

        assertTrue(
            Files.readString(mojo.outputDirectory.toPath().resolve("storymap.html"))
                .contains("--card-max-width: 320px;")
        );
    }

    @Test
    @DisplayName("入力ファイルがなければ、実行エラーになる")
    void missing_input_is_an_execution_error() {
        RenderMojo mojo = mojo(folder.resolve("missing.md"), List.of("html"));

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    @DisplayName("描画に失敗すると、実行エラーになる")
    void render_failure_is_an_execution_error() throws IOException {
        RenderMojo mojo = mojo(writeOutline("storymap.md"), List.of("png"));
        mojo.scale = 1_000_000f;

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    private Path writeOutline(String fileName) throws IOException {
        return Files.writeString(folder.resolve(fileName), "- Activity A\n  - Task A1\n");
    }

    // Sets the fields as Maven does, with the default values of the parameters
    private RenderMojo mojo(Path input, List<String> formats) {
        RenderMojo mojo = new RenderMojo();
        mojo.input = input.toFile();
        mojo.outputDirectory = folder.resolve("out").toFile();
        mojo.formats = formats;
        mojo.scale = 1.0f;
        return mojo;
    }

    private static Set<String> fileNames(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString()).collect(Collectors.toSet());
        }
    }
}
