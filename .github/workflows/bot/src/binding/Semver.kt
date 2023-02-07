@file:Suppress("PackageDirectoryMismatch")
@file:JsModule("semver")

/**
 * Compares two versions excluding build identifiers (the bit after `+` in
 * the semantic version string).
 *
 * @return
 * - `0` if `v1` == `v2`
 * - `1` if `v1` is greater
 * - `-1` if `v2` is greater.
 */

@JsName("compare")
external fun semverCompare(v1: String, v2: String): Int

@JsName("valid")
external fun semverValid(version: String): String?
