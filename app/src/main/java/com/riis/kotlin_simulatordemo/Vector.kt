package com.riis.kotlin_simulatordemo

class Vector {
    /**
     * entries contains the entries in the vector
     */
    private var entries: DoubleArray

    /**
     * Constructor makes a copy of the array passed.
     * @param entries an array containing the entries in the vector
     */
    constructor(vararg entries: Double) {
        this.entries = DoubleArray(entries.size)
        for (i in entries.indices) {
            this.entries[i] = entries[i]
        }
    }

    /**
     * copy constructor copies the entires in vect into v
     * @param u a Vector object
     */
    constructor(u: Vector) {
        entries = DoubleArray(u.entries.size)
        for (i in u.entries.indices) {
            entries[i] = u.entries[i]
        }
    }

    /**
     * add method accepts a vector and adds it to the current vector
     * @param u the vector to add onto the calling vector.
     * @return a Vector object whose entries are the element-wise sums
     * of the calling vector and the argument
     */
    fun add(u: Vector): Vector {
        return add(this, u)
    }

    /**
     * cross computes the cross product (this x u)
     * @param u the vector to cross the calling vector with
     * @return the cross product this x u
     */
    fun cross(u: Vector): Vector {
        return cross(this, u)
    }

    /**
     * dot method computes the dot product of the calling vector and
     * the passed vectored.
     * assumes vectors have the same length.
     * @param u a Vector object
     * @return the sum of the products of corresponding elements
     */
    fun dot(u: Vector): Double {
        return dot(this, u)
    }

    /**
     * Returns the entry in the specified position.
     * @param position the position to return
     * @return the value in entries[position]
     */
    operator fun get(position: Int): Double {
        return Companion[this, position]
    }

    /**
     * returns a copy of entries, not a reference to entries.
     * @return a copy of the array entries
     */
    fun getEntries(): DoubleArray {
        val entries = DoubleArray(entries.size)
        for (i in this.entries.indices) {
            entries[i] = this.entries[i]
        }
        return entries
    }

    /**
     * inverseVector returns the additive inverse of the calling vector.
     * @return a Vector with the signs flipped on all entries
     */
    fun inverseVector(): Vector {
        return this.multiply(-1.0)
    }

    /**
     * checks to see if the Vector object is a canonical basis Vector,
     * i.e. it has a one in exactly one entry and zeroes everywhere
     * else.
     * @return true if Vector contains all zeroes and a single one
     */
    val isCanonicalBasisVector: Boolean
        get() = isCanonicalBasisVector(this)

    /**
     * isZero checks to see if all entries are zero.
     * @return true if all entries are zero, false otherwise
     */
    val isZero: Boolean
        get() = isZero(this)

    /**
     * length method returns the number of entries in the
     * vector.
     * @return the length of v
     */
    fun length(): Int {
        return length(this)
    }

    /**
     * magnitude method is a wrapper for pnorm, with p=2
     * @return the magnitude of the vector
     */
    fun magnitude(): Double {
        return magnitude(this)
    }

    /**
     * multiply method accepts a scalar to and multiplies each element of
     * entries by that value.
     * @param scalar the real number to multiply the entries by
     * @return a Vector object whose entries are the element-wise sums
     * of the calling vector and the argument
     */
    fun multiply(scalar: Double): Vector {
        return multiply(this, scalar)
    }

    /**
     * normalize scales the calling vector by dividing it by its
     * magnitude. if the zero vector is passed, an IllegalArgumentException
     * is thrown.
     * @return a Vector object
     */
    fun normalize(): Vector {
        return normalize(this)
    }

    /**
     * The outer product is matrix multiplication on this and
     * the transpose of u.
     * @param u a Vector
     * @return the outer product
     */
    fun outerProduct(u: Vector?): Matrix {
        return outerProduct(this, u)
    }

    /**
     * an instance method that calls normL1 on the current object.
     * @param p a real number greater than or equal to 1
     * @return the L2 norm of the calling vector.
     */
    fun pnorm(p: Double): Double {
        return pnorm(this, p)
    }

    /**
     * projects the calling Vector onto the passed Vector
     * @param u the Vector we want to project this one onto
     * @return the orthogonal projection of this vector onto u
     */
    fun orthogonalProjection(u: Vector): Vector {
        return orthogonalProjection(this, u)
    }

    /**
     * set method modifies the element at index to equal value.
     * @param index the index we want to modify
     * @param value the new value
     * @return a Vector with the value at index updated
     */
    operator fun set(index: Int, value: Double): Vector {
        return set(this, index, value)
    }

    /**
     * Sets the values in the entries array.
     * @param entries an array of doubles
     */
    fun setEntries(entries: DoubleArray) {
        this.entries = DoubleArray(entries.size)
        for (i in entries.indices) {
            this.entries[i] = entries[i]
        }
    }

    /**
     * subtract method subtracts the passed Vector from the calling Vector.
     * @param u a Vector object
     * @return a Vector object whose entries are the difference of the
     * entries in the calling Vector and the respective entries
     * in v
     */
    fun subtract(u: Vector): Vector {
        return subtract(this, u)
    }

    /**
     * Return a String containing the vector represented as a row in brackets, e.g.
     * [1.0, 2.2, 3.1, 4.9, 5.7]
     * @return a String representation of the vector
     */
    override fun toString(): String {
        var str = "["
        val sep = ", "
        for (i in entries.indices) {
            str += entries[i]
            if (i < entries.size - 1) { // if we're not at the last entry
                str += sep
            }
        }
        return "$str]"
    }

    companion object {
        /*
    * threshold for double comparisons
    */
        const val THRESHOLD = Double.MIN_VALUE * 1000

        /**
         * add method accepts two vectors and returns their element-wise
         * sum in a new Vector object. Assumes v1 and v2 have the same
         * length.
         * @param u1 a Vector object
         * @param u2 a Vector object
         * @return a Vector objects whose entries are the sums of corresponding
         * entries in u1 and u2
         */
        fun add(u1: Vector, u2: Vector): Vector {
            checkLengths(u1, u2)
            val sums = DoubleArray(u1.length())
            for (i in sums.indices) {
                sums[i] = u1[i] + u2[i]
            }
            return Vector(*sums)
        }

        /**
         * angleDegrees method computes the angle between the two vectors,
         * computed as arccos(u1.dot(u2) / (u1.magnitude() * u2.magnitude())
         * @param u1 a Vector object
         * @param u2 a Vector object
         * @return the angle between u1 and u2 (in radians)
         */
        fun angleDegrees(u1: Vector, u2: Vector): Double {
            checkLengths(u1, u2)
            return angleRadians(u1, u2) * 180 / Math.PI
        }

        /**
         * angleRadians method computes the angle between the two vectors,
         * computed as arccos(u1.dot(u2) / (u1.magnitude() * u2.magnitude())
         * @param u1 a Vector object
         * @param u2 a Vector object
         * @return the angle between u1 and u2 (in radians)
         */
        fun angleRadians(u1: Vector, u2: Vector): Double {
            checkLengths(u1, u2)
            return Math.acos(dot(u1, u2) / (u1.magnitude() * u2.magnitude()))
        }

        /**
         * checkLengths method accepts two vectors and throws and
         * IllegalArgumentException if they are not the same lengths.
         * @param u1 a Vector object
         * @param u2 a Vector object
         */
        fun checkLengths(u1: Vector, u2: Vector) {
            require(u1.length() == u2.length()) { "Vectors are different lengths" }
        }

        /**
         * cross method takes two vectors of length 3 and returns their
         * cross product. Note that this operation is anticommutative, so
         * cross(a, b) = -cross(b, a)
         * @param a the left vector Vector
         * @param b the right vector Vector
         * @return the cross product a X b
         */
        fun cross(a: Vector, b: Vector): Vector {
            // check to make sure both vectors are the right length
            require(a.length() == 3) { "Invalid vector length (first vector)" }
            require(a.length() == 3) { "Invalid vector length (second vector)" }
            checkLengths(a, b) // just in case
            val entries = doubleArrayOf(
                a.entries[1] * b.entries[2] - a.entries[2] * b.entries[1],
                a.entries[2] * b.entries[0] - a.entries[0] * b.entries[2],
                a.entries[0] * b.entries[1] - a.entries[1] * b.entries[0]
            )
            return Vector(*entries)
        }

        /**
         * dot method computes the dot product of two vectors.
         * assumes vectors have the same length.
         * @param u1 a Vector object
         * @param u2 a Vector object
         * @return the sum of the products of corresponding elements
         */
        fun dot(u1: Vector, u2: Vector): Double {
            checkLengths(u1, u2)
            var sum = 0.0
            for (i in 0 until u1.length()) {
                sum += u1[i] * u2[i]
            }
            return sum
        }

        /**
         * Returns the entry in the specified position.
         * @param u a Vector object
         * @param position the position to return
         * @return the value in u[position]
         */
        operator fun get(u: Vector, position: Int): Double {
            return u.entries[position]
        }

        /**
         * identityVector returns an additive identity vector (whose entries are
         * all zeros).
         * @param length the length of the vector
         * @return a vector with all zeros
         */
        fun identityVector(length: Int): Vector {
            return Vector(*DoubleArray(length))
        }

        /**
         * inverseVector returns the additive inverse of the vector passed.
         * @param u a Vector
         * @return a Vector whose entries have the signs flipped
         */
        fun inverseVector(u: Vector): Vector {
            return multiply(u, -1.0)
        }

        /**
         * checks to see if the Vector object is a canonical basis Vector,
         * i.e. it has a one in exactly one entry and zeroes everywhere
         * else.
         * @param u a Vector object
         * @return true if Vector contains all zeroes and a single one
         */
        fun isCanonicalBasisVector(u: Vector): Boolean {
            var numOnes = 0
            val numZeros = 0
            for (i in 0 until u.length()) {
                if (Math.abs(1 - u[i]) < THRESHOLD) {
                    numOnes++
                }
            }
            return numOnes == 1
        }

        /**
         * isZero checks to see if all entries are zero.
         * @param u a Vector object
         * @return true if all entries in u are zero, false otherwise
         */
        fun isZero(u: Vector): Boolean {
            for (entry in u.entries) {
                if (Math.abs(entry) > THRESHOLD) { // if a non-zero entry is found
                    return false
                }
            }
            return true
        }

        /**
         * length method returns the number of entries in the
         * vector.
         * @param u a Vector object
         * @return the length of u
         */
        fun length(u: Vector): Int {
            return u.entries.size
        }

        /**
         * Creates a linear combination (weighted sum) of the Vector objects
         * and the weights. Throws IllegalArgumentException if the length of
         * the weights array does not match the length of the vectors array
         * @param vectors an array of Vector objects
         * @param weights an array of doubles to weight the sum
         * @return the linear combination of the vectors with the weights
         */
        fun linearCombination(vectors: Array<Vector>, weights: DoubleArray): Vector {
            require(vectors.size == weights.size) { "Number of vectors does not match number of weights." }

            // start the sum by weighting the first vector
            var sum = Vector(vectors[0].multiply(weights[0]))

            // weight and add each Vector onto sum
            for (i in 1 until vectors.size) {
                sum = sum.add(vectors[i].multiply(weights[i]))
            }
            return sum
        }

        /**
         * magnitude method is a wrapper for pnorm, with p=2
         * @param u a Vector object
         * @return the magnitude of the vector
         */
        fun magnitude(u: Vector): Double {
            return pnorm(u, 2.0)
        }

        /**
         * multiply accepts a Vector object and a scalar and returns
         * a Vector whose entries are the entries of the Vector, multiplied
         * by the scalar.
         * @param u a Vector object
         * @param scalar a real number
         * @return the scalar product of the vector and the scalar
         */
        fun multiply(u: Vector, scalar: Double): Vector {
            val products = DoubleArray(u.length())
            for (i in products.indices) {
                products[i] = scalar * u[i]
            }
            return Vector(*products)
        }

        /**
         * normalize scales the passed vector by dividing it by its
         * magnitude. if the zero vector is passed, an IllegalArgumentException
         * is thrown.
         * @param u a Vector object
         * @return a Vector object
         */
        fun normalize(u: Vector): Vector {
            return if (u.isZero) {
                throw IllegalArgumentException()
            } else {
                u.multiply(1.0 / u.magnitude())
            }
        }

        /**
         * scalarTripleProduct computes a.dot(b.cross(c))
         * @param a a Vector object
         * @param b a Vector object
         * @param c a Vector object
         * @return the scalar triple product a.dot(b.cross(c))
         */
        fun scalarTripleProduct(a: Vector, b: Vector, c: Vector): Double {
            return dot(a, cross(b, c))
        }

        /**
         * The outer product is matrix multiplication on u1 and
         * the transpose of u2.
         * @param u1 a Vector
         * @param u2 a Vector
         * @return the outer product
         */
        fun outerProduct(u1: Vector?, u2: Vector?): Matrix {
            val m1: Matrix = Matrix.fromColumnVectors(u1!!)
            val m2: Matrix = Matrix.fromRowVectors(u2!!)
            return Matrix.multiply(m1, m2)
        }

        /**
         * pnorm accepts a Vector and a value for p and returns the Lp norm
         * (the p-th root of the sum of the p-th power of the absolute value of
         * the enttries
         * @param u a Vector object
         * @param p a real number greater than or equal to 1
         * @return the Lp norm of the vector
         */
        fun pnorm(u: Vector, p: Double): Double {
            require(p >= 1) { "p must be >= 1" }
            var sum = 0.0
            for (i in 0 until u.length()) {
                sum += Math.pow(Math.abs(u[i]), p)
            }
            return Math.pow(sum, 1 / p)
        }

        /**
         * projects u1 onto u2
         * @param u1 the Vector whose orthogonal projection we want
         * @param u2 the Vector we are projecting u1 onto
         * @return the orthogonal projection
         */
        fun orthogonalProjection(u1: Vector, u2: Vector): Vector {
            checkLengths(u1, u2)
            require(!u2.isZero) { "Cannot project onto zero vector" }
            return multiply(u2, u1.dot(u2) / u2.dot(u2))
        }

        /**
         * set method modifies the element at index to equal value.
         * @param u a Vector object
         * @param index the index we want to modify
         * @param value the new value
         * @return a Vector with the value at index updated
         */
        operator fun set(u: Vector, index: Int, value: Double): Vector {
            require(!(index < 0 || index >= u.length())) { "Index is out of range" }
            val entries = DoubleArray(u.entries.size)
            entries[index] = value
            return Vector(*entries)
        }

        /**
         * subtract method returns the difference of two vectors. note
         * that difference is a special case of sum (v1 + (-1)*v2)
         * @param v1 a Vector object
         * @param v2 a Vector object
         * @return a new Vector object whose entries are the differences of
         * the entries in v1 and v2 (v1 - v2)
         */
        fun subtract(v1: Vector, v2: Vector): Vector {
            return add(v1, v2.multiply(-1.0))
        }
    }
}