package com.sosuisha.storymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NativeLibraryTest {
    @Nested
    @DisplayName("ライブラリの場所")
    class ResourcePath {
        @ParameterizedTest(name = "{0} {1} → {2}")
        @CsvSource({
                "Windows 11, amd64, native/windows-x86_64/storymap_ffi.dll",
                "Linux, amd64, native/linux-x86_64/libstorymap_ffi.so",
                "Linux, aarch64, native/linux-aarch64/libstorymap_ffi.so",
                "Mac OS X, x86_64, native/macos-x86_64/libstorymap_ffi.dylib",
                "Mac OS X, aarch64, native/macos-aarch64/libstorymap_ffi.dylib",
        })
        @DisplayName("OSとアーキテクチャから、クラスパス上のライブラリの場所が決まる")
        void resource_path_follows_os_and_arch(String osName, String osArch, String expected) {
            assertEquals(expected, NativeLibrary.resourcePath(osName, osArch));
        }

        @Test
        @DisplayName("対応していないOSでは、ライブラリの場所が決まらない")
        void unsupported_os_has_no_resource_path() {
            assertThrows(
                UnsupportedOperationException.class,
                () -> NativeLibrary.resourcePath("FreeBSD", "amd64")
            );
        }

        @Test
        @DisplayName("対応していないアーキテクチャでは、ライブラリの場所が決まらない")
        void unsupported_arch_has_no_resource_path() {
            assertThrows(
                UnsupportedOperationException.class,
                () -> NativeLibrary.resourcePath("Linux", "riscv64")
            );
        }
    }

    @Nested
    @DisplayName("ライブラリの書き出し")
    class Extract {
        private static final byte[] LIBRARY = "library".getBytes(StandardCharsets.UTF_8);

        @TempDir
        Path cacheRoot;

        @Test
        @DisplayName("ライブラリの内容を、指定した名前のファイルに書き出す")
        void library_is_written_with_its_file_name() throws IOException {
            Path file = NativeLibrary.extract(LIBRARY, "storymap_ffi.dll", cacheRoot);

            assertEquals("storymap_ffi.dll", file.getFileName().toString());
            assertArrayEquals(LIBRARY, Files.readAllBytes(file));
        }

        @Test
        @DisplayName("同じ内容のライブラリは、書き直さずに前のコピーを使う")
        void same_library_reuses_the_copy() throws IOException {
            Path first = NativeLibrary.extract(LIBRARY, "storymap_ffi.dll", cacheRoot);
            // A loaded DLL cannot be overwritten on Windows, so the copy must stay as it is
            Files.writeString(first, "in use");

            Path second = NativeLibrary.extract(LIBRARY, "storymap_ffi.dll", cacheRoot);

            assertEquals(first, second);
            assertEquals("in use", Files.readString(second));
        }

        @Test
        @DisplayName("内容の違うライブラリは、別の場所に書き出す")
        void different_library_gets_its_own_copy() {
            Path first = NativeLibrary.extract(LIBRARY, "storymap_ffi.dll", cacheRoot);
            Path second = NativeLibrary.extract(
                "other".getBytes(StandardCharsets.UTF_8), "storymap_ffi.dll",
                cacheRoot
            );

            assertNotEquals(first, second);
        }
    }
}
