@file:Suppress("PackageDirectoryMismatch")
@file:JsModule("@actions/core")

package actions.core

external interface InputOptions {
  var required: Boolean?
  var trimWhitespace: Boolean?
}

external fun getInput(name: String, options: InputOptions?): String

external fun setFailed(message: String)
external fun setFailed(message: Throwable)

external fun startGroup(name: String)
external fun endGroup()

external fun isDebug(): Boolean

external fun debug(message: String)
external fun error(message: String)
external fun error(message: Error)
external fun warning(message: String)
external fun warning(message: Error)

@JsName("info")
external fun infoImpl(message: String)
