# storymap-maven-plugin

English | [日本語](README.ja.md)

A Maven plugin that renders a Markdown outline as a user story map.
It writes the map as HTML, SVG, or PNG.

The Rust crate [storymap](https://crates.io/crates/storymap) does the rendering.
The plugin calls it from Java through FFM.
See the README of storymap for the outline format.

## Requirements

- Maven 3.9 or later, running on Java 25 or later
- One of these platforms:
  - Windows (x86_64)
  - macOS (x86_64, aarch64)
  - Linux (x86_64, aarch64), with glibc 2.28 or later

## Usage

Add the plugin to `pom.xml`:

```xml
<plugin>
  <groupId>com.sosuisha</groupId>
  <artifactId>storymap-maven-plugin</artifactId>
  <version>1.0.0</version>
  <configuration>
    <input>${project.basedir}/docs/storymap.md</input>
    <formats>
      <format>html</format>
      <format>png</format>
    </formats>
    <scale>2.0</scale>
  </configuration>
</plugin>
```

Run it:

```sh
mvn storymap:render
```

You get `target/storymap/storymap.html` and `target/storymap/storymap.png`.

You can also set the parameters on the command line:

```sh
mvn storymap:render -Dstorymap.input=docs/map.md -Dstorymap.formats=html,svg,png
```

### Parameters

| Parameter | Property | Default | Meaning |
|---|---|---|---|
| `input` | `storymap.input` | `${project.basedir}/docs/storymap.md` | The outline to read. It is read as UTF-8 |
| `outputDirectory` | `storymap.outputDirectory` | `${project.build.directory}/storymap` | The folder to write the files to |
| `formats` | `storymap.formats` | `html` | The output formats: `html`, `svg`, and `png` |
| `scale` | `storymap.scale` | `1.0` | The size of the PNG image. At 1.0, one CSS pixel is one pixel |
| `maxCardWidth` | `storymap.maxCardWidth` | `160` | The widest that a card may be, in CSS pixels. A longer text wraps onto more lines |

Each output file is named after the input file, without its extension.

### Turn off the warning

The plugin calls a native library through FFM.
Recent versions of Maven allow this already.
With an older version, Java prints this warning:

```
WARNING: A restricted method in java.lang.foreign.SymbolLookup has been called
```

To turn it off, add this line to `.mvn/jvm.config` in your project:

```
--enable-native-access=ALL-UNNAMED
```

### Fonts in images

SVG and PNG measure the text with the fonts of the OS.
So the image may look a little different on each OS.

## Development

The build needs JDK 25 and the Rust toolchain.

```sh
mvn formatter:validate
mvn clean test
cd native && cargo fmt --check && cargo clippy --all-targets
```

The Maven build runs `cargo build --release` first.
The jar gets only the library for your own OS.

## Release

- The Git tag alone decides the version. `revision` in `pom.xml` is only a placeholder for local builds. Do not change it.
- Push a tag to start the release workflow:

  ```sh
  git tag v1.0.0
  git push origin v1.0.0
  ```

- The workflow does these steps:
  1. It builds the native library for five platforms on Windows, macOS, and Linux runners.
  2. It collects them in `native-libs/`, runs the tests again, and builds one jar.
  3. It checks that the jar holds all five libraries.
  4. It signs the jars and uploads them to the Central Portal.
  5. It creates a GitHub Release with the jar and the pom.
- The tag must look like `v1.2.0`. With any other form, the workflow stops.
- The upload is not public yet. Open the deployment in the [Central Portal](https://central.sonatype.com/publishing/deployments), check it, and press "Publish".
  - A version on Maven Central can never be changed or removed. That is why this step is done by hand.

The workflow needs these secrets in the repository:

| Secret | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | The name of a user token of the Central Portal |
| `MAVEN_CENTRAL_PASSWORD` | The password of the same token |
| `GPG_PRIVATE_KEY` | The output of `gpg --armor --export-secret-keys <key ID>` |
| `GPG_PASSPHRASE` | The passphrase of the key |

## License

MIT
