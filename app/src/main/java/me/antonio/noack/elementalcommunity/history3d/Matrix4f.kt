package me.antonio.noack.elementalcommunity.history3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Matrix4f {

    var m00 = 1f
    var m01 = 0f
    var m02 = 0f
    var m03 = 0f
    var m10 = 0f
    var m11 = 1f
    var m12 = 0f
    var m13 = 0f
    var m20 = 0f
    var m21 = 0f
    var m22 = 1f
    var m23 = 0f
    var m30 = 0f
    var m31 = 0f
    var m32 = 0f
    var m33 = 1f

    fun identity(): Matrix4f {
        m00 = 1f
        m01 = 0f
        m02 = 0f
        m03 = 0f
        m10 = 0f
        m11 = 1f
        m12 = 0f
        m13 = 0f
        m20 = 0f
        m21 = 0f
        m22 = 1f
        m23 = 0f
        m30 = 0f
        m31 = 0f
        m32 = 0f
        m33 = 1f
        return this
    }

    fun translate(x: Float, y: Float, z: Float): Matrix4f {
        m30 += m00 * x + m10 * y + m20 * z
        m31 += m01 * x + m11 * y + m21 * z
        m32 += m02 * x + m12 * y + m22 * z
        m33 += m03 * x + m13 * y + m23 * z
        return this
    }

    fun scale(x: Float, y: Float, z: Float): Matrix4f {
        m00 *= x
        m01 *= x
        m02 *= x
        m03 *= x

        m10 *= y
        m11 *= y
        m12 *= y
        m13 *= y

        m20 *= z
        m21 *= z
        m22 *= z
        m23 *= z
        return this
    }

    fun rotateX(ang: Float): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        val lm10 = m10
        val lm11 = m11
        val lm12 = m12
        val lm13 = m13
        val lm20 = m20
        val lm21 = m21
        val lm22 = m22
        val lm23 = m23

        m20 = lm10 * -sin + lm20 * cos
        m21 = lm11 * -sin + lm21 * cos
        m22 = lm12 * -sin + lm22 * cos
        m23 = lm13 * -sin + lm23 * cos
        m10 = lm10 * cos + lm20 * sin
        m11 = lm11 * cos + lm21 * sin
        m12 = lm12 * cos + lm22 * sin
        m13 = lm13 * cos + lm23 * sin
        return this
    }

    fun rotateY(ang: Float): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = m00 * cos + m20 * -sin
        val nm01 = m01 * cos + m21 * -sin
        val nm02 = m02 * cos + m22 * -sin
        val nm03 = m03 * cos + m23 * -sin
        m20 = m00 * sin + m20 * cos
        m21 = m01 * sin + m21 * cos
        m22 = m02 * sin + m22 * cos
        m23 = m03 * sin + m23 * cos
        m00 = nm00
        m01 = nm01
        m02 = nm02
        m03 = nm03
        return this
    }

    fun perspective(
        fovy: Float,
        aspect: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
    ): Matrix4f {

        val h = tan(fovy * 0.5f)
        val rm00 = 1f / (h * aspect)
        val rm11 = 1f / h

        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val rm22: Float
        val rm32: Float
        var e: Float
        if (farInf) {
            e = 1.0E-6f
            rm22 = e - 1f
            rm32 = (e - if (zZeroToOne) 1f else 2f) * zNear
        } else if (nearInf) {
            e = 1.0E-6f
            rm22 = (if (zZeroToOne) 0f else 1f) - e
            rm32 = ((if (zZeroToOne) 1f else 2f) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }

        e = m20 * rm22 - m30
        val nm21 = m21 * rm22 - m31
        val nm22 = m22 * rm22 - m32
        val nm23 = m23 * rm22 - m33

        m00 *= rm00
        m01 *= rm00
        m02 *= rm00
        m03 *= rm00

        m10 *= rm11
        m11 *= rm11
        m12 *= rm11
        m13 *= rm11

        m30 = m20 * rm32
        m31 = m21 * rm32
        m32 = m22 * rm32
        m33 = m23 * rm32

        m20 = e
        m21 = nm21
        m22 = nm22
        m23 = nm23
        return this
    }

    fun mul(right: Matrix4f, dst: Matrix4f = this): Matrix4f {
        val nm00 = m00 * right.m00 + m10 * right.m01 + m20 * right.m02 + m30 * right.m03
        val nm01 = m01 * right.m00 + m11 * right.m01 + m21 * right.m02 + m31 * right.m03
        val nm02 = m02 * right.m00 + m12 * right.m01 + m22 * right.m02 + m32 * right.m03
        val nm03 = m03 * right.m00 + m13 * right.m01 + m23 * right.m02 + m33 * right.m03
        val nm10 = m00 * right.m10 + m10 * right.m11 + m20 * right.m12 + m30 * right.m13
        val nm11 = m01 * right.m10 + m11 * right.m11 + m21 * right.m12 + m31 * right.m13
        val nm12 = m02 * right.m10 + m12 * right.m11 + m22 * right.m12 + m32 * right.m13
        val nm13 = m03 * right.m10 + m13 * right.m11 + m23 * right.m12 + m33 * right.m13
        val nm20 = m00 * right.m20 + m10 * right.m21 + m20 * right.m22 + m30 * right.m23
        val nm21 = m01 * right.m20 + m11 * right.m21 + m21 * right.m22 + m31 * right.m23
        val nm22 = m02 * right.m20 + m12 * right.m21 + m22 * right.m22 + m32 * right.m23
        val nm23 = m03 * right.m20 + m13 * right.m21 + m23 * right.m22 + m33 * right.m23
        val nm30 = m00 * right.m30 + m10 * right.m31 + m20 * right.m32 + m30 * right.m33
        val nm31 = m01 * right.m30 + m11 * right.m31 + m21 * right.m32 + m31 * right.m33
        val nm32 = m02 * right.m30 + m12 * right.m31 + m22 * right.m32 + m32 * right.m33
        val nm33 = m03 * right.m30 + m13 * right.m31 + m23 * right.m32 + m33 * right.m33
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m03 = nm03
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m13 = nm13
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m23 = nm23
        dst.m30 = nm30
        dst.m31 = nm31
        dst.m32 = nm32
        dst.m33 = nm33
        return dst
    }

    fun fillInto(arr: java.nio.FloatBuffer) {
        arr.put(0, m00)
        arr.put(1, m01)
        arr.put(2, m02)
        arr.put(3, m03)
        arr.put(4, m10)
        arr.put(5, m11)
        arr.put(6, m12)
        arr.put(7, m13)
        arr.put(8, m20)
        arr.put(9, m21)
        arr.put(10, m22)
        arr.put(11, m23)
        arr.put(12, m30)
        arr.put(13, m31)
        arr.put(14, m32)
        arr.put(15, m33)
    }
}