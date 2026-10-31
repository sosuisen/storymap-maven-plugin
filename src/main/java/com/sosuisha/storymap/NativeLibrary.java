package com.sosuisha.storymap;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Loads the native library of storymap for this platform from the class path.
 */
final class NativeLibrary {
    private static final String CACHE_NAME = "storymap-maven-plugin";

    private NativeLibrary() {}

    /**
     * Loads the native library of this platform. It stays loaded until the JVM exits.
     *
     * @return the lookup of the symbols in the library
     * @throws UnsupportedOperationException if no native library is built for this platform
     * @throws UncheckedIOException if the library cannot be copied to the cache
     */
    static SymbolLookup load() {
        String osName = System.getProperty("os.name");
        String resource = resourcePath(osName, System.getProperty("os.arch"));
        byte[] bytes = readResource(resource);
        Path cacheRoot = Path.of(System.getProperty("java.io.tmpdir"), CACHE_NAME);
        Path file = extract(bytes, fileName(osName), cacheRoot);
        return SymbolLookup.libraryLookup(file, Arena.global());
    }

    /**
     * Returns the class path resource of the native library for a platform.
     *
     * @param osName the value of the {@code os.name} system property
     * @param osArch the value of the {@code os.arch} system property
     * @return the resource path, such as {@code native/linux-x86_64/libstorymap_ffi.so}
     * @throws UnsupportedOperationException if no native library is built for the platform
     */
    static String resourcePath(String osName, String osArch) {
        return "native/" + os(osName) + "-" + arch(osArch) + "/" + fileName(osName);
    }

    /**
     * Copies the library into a folder named after its content, unless the copy is already there.
     *
     * <p>
     * A loaded DLL cannot be deleted on Windows. A temporary file for each run would pile up, so
     * the runs share one copy.
     *
     * @param bytes the content of the library
     * @param fileName the file name of the library
     * @param cacheRoot the folder that holds the copies
     * @return the copy of the library
     * @throws UncheckedIOException if the library cannot be copied
     */
    static Path extract(byte[] bytes, String fileName, Path cacheRoot) {
        Path folder = cacheRoot.resolve(sha256(bytes));
        Path file = folder.resolve(fileName);
        if (Files.exists(file)) { return file; }
        try {
            Files.createDirectories(folder);
            // Another Maven process may copy the same library at the same time.
            // The move makes the copy appear whole or not at all.
            Path partial = Files.createTempFile(folder, fileName, ".partial");
            Files.write(partial, bytes);
            try {
                Files.move(partial, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.deleteIfExists(partial);
                if (!Files.exists(file)) { throw e; }
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String os(String osName) {
        String name = osName.toLowerCase(Locale.ROOT);
        if (name.startsWith("windows")) { return "windows"; }
        if (name.startsWith("mac")) { return "macos"; }
        if (name.startsWith("linux")) { return "linux"; }
        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }

    private static String arch(String osArch) {
        return switch (osArch.toLowerCase(Locale.ROOT)) {
            case "amd64", "x86_64" -> "x86_64";
            case "aarch64", "arm64" -> "aarch64";
            default -> throw new UnsupportedOperationException(
                "Unsupported architecture: " + osArch
            );
        };
    }

    private static String fileName(String osName) {
        return switch (os(osName)) {
            case "windows" -> "storymap_ffi.dll";
            case "macos" -> "libstorymap_ffi.dylib";
            default -> "libstorymap_ffi.so";
        };
    }

    private static byte[] readResource(String resource) {
        try (InputStream in = NativeLibrary.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new UnsupportedOperationException(
                    "No native library for this platform: " + resource
                );
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform must support SHA-256
            throw new AssertionError(e);
        }
    }
}
