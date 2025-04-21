package com.bask0xff.openglisometricscene

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun get(index: Int): Float {
        return when (index) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw IndexOutOfBoundsException("Invalid index for Vector3")
        }
    }

    operator fun minus(v: Vector3): Vector3 {
        return Vector3(this.x - v.x, this.y - v.y, this.z - v.z)
    }

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
