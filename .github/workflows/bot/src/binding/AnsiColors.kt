@file:Suppress("PackageDirectoryMismatch", "ktlint:max-line-length")

object AnsiColors {
  private val instance = js("eval('require')('ansi-colors')")

  fun red(text: String): String = instance.red(text) as String
  fun green(text: String): String = instance.green(text) as String
  fun yellow(text: String): String = instance.yellow(text) as String
  fun blue(text: String): String = instance.blue(text) as String
  fun magenta(text: String): String = instance.magenta(text) as String
  fun cyan(text: String): String = instance.cyan(text) as String
  fun gray(text: String): String = instance.gray(text) as String
  fun redBright(text: String): String = instance.redBright(text) as String
  fun greenBright(text: String): String = instance.greenBright(text) as String
  fun yellowBright(text: String): String = instance.yellowBright(text) as String
  fun blueBright(text: String): String = instance.blueBright(text) as String
  fun magentaBright(text: String): String = instance.magentaBright(text) as String
  fun cyanBright(text: String): String = instance.cyanBright(text) as String
  fun grayBright(text: String): String = instance.grayBright(text) as String
}
