package com.bask0xff.openglisometricscene

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun get(i: Int): Float = when (i) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException()
    }
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)

    operator fun times(f: Float) = Vector3(x * f, y * f, z * f)
    fun normalize(): Vector3 {
        val len = length()
        return Vector3(x / len, y / len, z / len)
    }

    fun length() = kotlin.math.sqrt(x * x + y * y + z * z)

    fun toFloatArray() = floatArrayOf(x, y, z)

    operator fun div(scalar: Float): Vector3 {
        return Vector3(this.x / scalar, this.y / scalar, this.z / scalar)
    }

    // Дополнительные полезные методы для векторов
    fun dot(other: Vector3): Float {
        return this.x * other.x + this.y * other.y + this.z * other.z
    }

    fun cross(other: Vector3): Vector3 {
        return Vector3(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
        )
    }
}
