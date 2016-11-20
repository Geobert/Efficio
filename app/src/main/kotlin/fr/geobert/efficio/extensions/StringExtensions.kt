package fr.geobert.efficio.extensions

val PLAIN_ASCII = "AaEeIiOoUu" /* grave */ +
        "AaEeIiOoUuYy"  /* acute */ +
        "AaEeIiOoUuYy" /* circumflex */ +
        "AaOoNn" /* tilde */ +
        "AaEeIiOoUuYy" /* umlaut */ +
        "Aa" /* ring */ +
        "Cc" /* cedilla */ +
        "OoUu" /* double acute */
val UNICODE = "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9" +
        "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD" +
        "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177" +
        "\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1" +
        "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" +
        "\u00C5\u00E5" +
        "\u00C7\u00E7" +
        "\u0150\u0151\u0170\u0171"
val DOUBLE_UNICODE = "\u0152\u0153\u04D4\u04D5"
val DOUBLE_ASCII = arrayOf("OE", "oe", "AE", "ae")

// remove accentuated from a string and replace with ascii equivalent
fun String.convertNonAscii(): String {
    val sb = StringBuilder()
    val n = this.length
    for (i in 0..n - 1) {
        val c = this[i]
        val pos = UNICODE.indexOf(c)
        if (pos > -1) {
            sb.append(PLAIN_ASCII[pos])
        } else {
            val dpos = DOUBLE_UNICODE.indexOf(c)
            if (dpos > -1) {
                sb.append(DOUBLE_ASCII[dpos])
            } else {
                sb.append(c)
            }
        }
    }
    return sb.toString()
}

fun String.normalize(): String {
    return this.convertNonAscii().toLowerCase()
}