@file:Suppress("PackageDirectoryMismatch")
@file:JsModule("string_decoder")

import node.buffer.Buffer

external class StringDecoder(encoding: String = definedExternally) {
  fun write(buffer: Buffer): String
  fun end(): String
}
