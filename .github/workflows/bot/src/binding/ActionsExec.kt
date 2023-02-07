@file:Suppress("PackageDirectoryMismatch")
@file:JsModule("@actions/exec")

package actions.exec

import node.buffer.Buffer
import kotlin.js.Promise

external fun exec(
  commandLine: String,
  args: Array<String> = definedExternally,
  options: ExecOptions = definedExternally,
): Promise<Number>

external interface ExecOptions {
  var outStream: dynamic
    get() = definedExternally
    set(value) = definedExternally
  var listeners: ExecListeners?
    get() = definedExternally
    set(value) = definedExternally
}

external interface ExecOutput {
  var exitCode: Number
  var stdout: String
  var stderr: String
}

external interface ExecListeners {
  var stdout: (data: Buffer) -> Unit
  var stderr: (data: Buffer) -> Unit
}
