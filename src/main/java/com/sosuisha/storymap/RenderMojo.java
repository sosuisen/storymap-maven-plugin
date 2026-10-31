package com.sosuisha.storymap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Renders a Markdown outline as a user story map.
 *
 * <p>
 * Each format is written to the output directory, named after the input file. For example,
 * {@code storymap.md} becomes {@code storymap.html}.
 */
@Mojo(name = "render", threadSafe = true)
public class RenderMojo extends AbstractMojo {
    /** The Markdown outline to read. It is read as UTF-8. */
    @Parameter(property = "storymap.input", defaultValue = "${project.basedir}/docs/storymap.md")
    File input;

    /** The directory to write the rendered files to. */
    @Parameter(property = "storymap.outputDirectory",
        defaultValue = "${project.build.directory}/storymap")
    File outputDirectory;

    /** The formats to write: {@code html}, {@code svg}, and {@code png}. */
    @Parameter(property = "storymap.formats", defaultValue = "html")
    List<String> formats;

    /** The size of the PNG image, where 1.0 is one pixel per CSS pixel. */
    @Parameter(property = "storymap.scale", defaultValue = "1.0")
    float scale;

    /**
     * Renders the input in each format and writes the files.
     *
     * @throws MojoFailureException if a format is unknown or the scale is not positive
     * @throws MojoExecutionException if a file cannot be read or written, or the rendering fails
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<OutputFormat> parsedFormats = parseFormats();
        String outline = readInput();
        Path directory = createOutputDirectory();
        for (OutputFormat format : parsedFormats) {
            Path output = directory.resolve(baseName() + "." + format.extension());
            write(output, render(outline, format));
            getLog().info("Rendered the story map: " + output);
        }
    }

    private List<OutputFormat> parseFormats() throws MojoFailureException {
        List<OutputFormat> parsed = new ArrayList<>();
        for (String name : formats) {
            try {
                parsed.add(OutputFormat.fromName(name));
            } catch (IllegalArgumentException e) {
                throw new MojoFailureException(e.getMessage(), e);
            }
        }
        return parsed;
    }

    private String readInput() throws MojoExecutionException {
        try {
            return Files.readString(input.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read the outline: " + input, e);
        }
    }

    private byte[] render(String outline, OutputFormat format)
        throws MojoExecutionException, MojoFailureException {
        try {
            return StoryMapRenderer.render(outline, format, scale);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (RenderException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Path createOutputDirectory() throws MojoExecutionException {
        try {
            return Files.createDirectories(outputDirectory.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot create " + outputDirectory, e);
        }
    }

    private static void write(Path output, byte[] bytes) throws MojoExecutionException {
        try {
            Files.write(output, bytes);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write " + output, e);
        }
    }

    private String baseName() {
        String name = input.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
