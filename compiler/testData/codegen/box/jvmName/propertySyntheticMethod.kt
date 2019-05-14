// TARGET_BACKEND: JVM
// WITH_REFLECT

@Deprecated("int")
@get:JvmName("mapIntIntHex")
val Map<Int, Int>.hex: String get() = "O"

@Deprecated("byte")
@get:JvmName("mapIntByteHex")
val Map<Int, Byte>.hex: String get() = "K"

fun box(): String {
    val a1 = Map<Int, Int>::hex.annotations
    if ((a1.single() as Deprecated).message != "int")
        return "Fail annotations on Map<Int, Int>::hex: $a1"

    val a2 = Map<Int, Byte>::hex.annotations
    if ((a2.single() as Deprecated).message != "byte")
        return "Fail annotations on Map<Int, Int>::hex: $a2"

    return mapOf(0 to 0).hex + mapOf(0 to 0.toByte()).hex
}
