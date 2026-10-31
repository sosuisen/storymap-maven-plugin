//! A C interface to the storymap crate, for the Java side to call through FFM.
//!
//! Each render function reads a UTF-8 outline and returns a new buffer.
//! It writes the length of the buffer to `out_len`.
//! It returns null if the rendering fails. The caller frees the buffer with [`storymap_free`].

use std::panic::{self, AssertUnwindSafe};
use std::sync::{Arc, OnceLock};
use std::{ptr, slice};

use storymap::StoryMap;
use storymap::html::{escape_html, map_css, render_map_content};
use storymap::image::{ImageOptions, fontdb, render_png, render_svg};

/// Render the outline as a standalone HTML page, with cards up to `max_card_width` CSS pixels wide
///
/// # Safety
///
/// `input` must point to `input_len` readable bytes, and `out_len` must be writable.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn storymap_render_html(
    input: *const u8,
    input_len: usize,
    max_card_width: f32,
    out_len: *mut usize,
) -> *mut u8 {
    let outline = unsafe { read_outline(input, input_len) };
    let rendered = catch(|| html_page(&storymap::parse(&outline), max_card_width).into_bytes());
    unsafe { hand_over(rendered, out_len) }
}

/// Render the outline as an SVG document, with cards up to `max_card_width` CSS pixels wide
///
/// # Safety
///
/// `input` must point to `input_len` readable bytes, and `out_len` must be writable.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn storymap_render_svg(
    input: *const u8,
    input_len: usize,
    max_card_width: f32,
    out_len: *mut usize,
) -> *mut u8 {
    let outline = unsafe { read_outline(input, input_len) };
    // The SVG is measured in CSS pixels, so the scale of the options does not matter
    let rendered = catch(|| {
        render_svg(
            &storymap::parse(&outline),
            &image_options(1.0, max_card_width),
        )
        .into_bytes()
    });
    unsafe { hand_over(rendered, out_len) }
}

/// Render the outline as a PNG image, where a `scale` of 1.0 is one pixel per CSS pixel.
/// Cards are up to `max_card_width` CSS pixels wide
///
/// # Safety
///
/// `input` must point to `input_len` readable bytes, and `out_len` must be writable.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn storymap_render_png(
    input: *const u8,
    input_len: usize,
    scale: f32,
    max_card_width: f32,
    out_len: *mut usize,
) -> *mut u8 {
    let outline = unsafe { read_outline(input, input_len) };
    let rendered = catch(|| {
        let map = storymap::parse(&outline);
        let options = image_options(scale, max_card_width);
        png_fits(&render_svg(&map, &options), scale).then(|| render_png(&map, &options))
    })
    .flatten();
    unsafe { hand_over(rendered, out_len) }
}

// A failed allocation aborts the process, and catch_unwind cannot stop it.
// So a PNG that is too large is refused before it is drawn. The limit is 512 MiB of RGBA pixels
const MAX_PNG_PIXELS: f64 = (1u64 << 27) as f64;

// A scale that is not a positive finite number fails the comparison, and is refused too
fn png_fits(svg: &str, scale: f32) -> bool {
    let scaled = |name| svg_size(svg, name).map(|size| (size * f64::from(scale)).ceil());
    match (scaled("width"), scaled("height")) {
        (Some(width), Some(height)) => width > 0.0 && width * height <= MAX_PNG_PIXELS,
        _ => false,
    }
}

// storymap writes the root element first, so the first attribute of the name belongs to the root
fn svg_size(svg: &str, name: &str) -> Option<f64> {
    let start = svg.find(&format!(" {name}=\""))? + name.len() + 3;
    let length = svg[start..].find('"')?;
    svg[start..start + length].parse().ok()
}

/// Free a buffer returned by a render function
///
/// # Safety
///
/// `ptr` and `len` must come from one call of a render function, and the buffer must not be freed twice.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn storymap_free(ptr: *mut u8, len: usize) {
    if !ptr.is_null() {
        drop(unsafe { Box::from_raw(ptr::slice_from_raw_parts_mut(ptr, len)) });
    }
}

// An empty slice may come with a null pointer, and from_raw_parts does not allow it
unsafe fn read_outline(input: *const u8, input_len: usize) -> String {
    if input_len == 0 {
        return String::new();
    }
    let bytes = unsafe { slice::from_raw_parts(input, input_len) };
    String::from_utf8_lossy(bytes).into_owned()
}

// A panic must not unwind into the JVM. Rust would abort the whole Maven process
fn catch<T>(render: impl FnOnce() -> T) -> Option<T> {
    panic::catch_unwind(AssertUnwindSafe(render)).ok()
}

unsafe fn hand_over(rendered: Option<Vec<u8>>, out_len: *mut usize) -> *mut u8 {
    match rendered {
        Some(bytes) => {
            let boxed = bytes.into_boxed_slice();
            unsafe { *out_len = boxed.len() };
            Box::into_raw(boxed).cast()
        }
        None => ptr::null_mut(),
    }
}

// The style sheet of storymap reads the maximum card width from a CSS variable
fn html_page(map: &StoryMap, max_card_width: f32) -> String {
    format!(
        "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<title>{}</title>\n<style>\n:root {{ --card-max-width: {max_card_width}px; }}\n{}\n</style>\n</head>\n<body>\n<div class=\"map-zoom\">{}</div>\n</body>\n</html>\n",
        escape_html(&map.title),
        map_css(),
        render_map_content(map)
    )
}

// Loading the fonts of the OS takes a long time, so it is done once per process
fn image_options(scale: f32, max_card_width: f32) -> ImageOptions {
    static FONTS: OnceLock<Arc<fontdb::Database>> = OnceLock::new();
    ImageOptions {
        fonts: FONTS.get_or_init(|| ImageOptions::default().fonts).clone(),
        scale,
        max_card_width,
    }
}
