package com.riis.kotlin_simulatordemo

/**
 * The Matrix class stores a 2D array of doubles and provides
 * common linear algebra operations on that array, in addition
 * to producing special matrices and checking for specific conditions.
 */
class Matrix {
    /**
     * entries is a 2D array of doubles to hold the entries in the Matrix
     */
    private var entries: Array<DoubleArray>

    /**
     * accepts a 2D array of doubles and copies them into the entries field.
     * @param entries a 2D array of doubles
     */
    constructor(entries: Array<DoubleArray>) {
        this.entries = Array(entries.size) {
            DoubleArray(
                entries[0].size
            )
        }
        for (row in entries.indices) {
            for (col in entries[row].indices) {
                this.entries[row][col] = entries[row][col]
            }
        }
    }

    /**
     * copy constructor accepts a Matrix object and duplicates it.
     * @param m the Matrix object we want to copy
     */
    constructor(m: Matrix) {
        entries = Array(m.numRows) { DoubleArray(m.numColumns) }
        for (row in m.entries.indices) {
            for (col in m.entries[row].indices) {
                entries[row][col] = m.entries[row][col]
            }
        }
    }

    /**
     * adds corresponding entries in the two matrices. Calling Matrix and Matrix b
     * must have the same dimensions to be compatible.
     * @param b an m-by-n Matrix object
     * @return an m-by-n Matrix object whose entries are sums of corresponding
     * entries in the calling Matrix and b
     */
    fun add(b: Matrix): Matrix {
        return add(this, b)
    }

    /**
     * adds the passed Vector to the specified column of the calling Matrix.
     * throws an IllegalArgumentException if the dimensions don't line up.
     * @param u a Vector object
     * @param col the column in m to which we want to add u
     * @return a Matrix with the values in u added to the specified column
     */
    fun addVectorToColumn(u: Vector, col: Int): Matrix {
        return addVectorToColumn(this, u, col)
    }

    /**
     * adds the passed Vector to the specified row of the calling Matrix.
     * throws an IllegalArgumentException if the dimensions don't line up.
     * @param u a Vector object
     * @param row the row in m to which we want to add u
     * @return a Matrix with the values in u added to the specified row
     */
    fun addVectorToRow(u: Vector, row: Int): Matrix {
        return addVectorToRow(this, u, row)
    }

    /**
     * minorMatrix accepts a row and a column, and returns the Matrix object
     * with that row and column dropped.
     * @param row an int
     * @param col an int
     * @return a copy of m, with the specified row and column dropped
     */
    fun minorMatrix(row: Int, col: Int): Matrix {
        return minorMatrix(this, row, col)
    }

    /**
     * determinant is an instance wrapper for static determinant method.
     * @return the determinant of the calling matrix.
     */
    fun determinant(): Double {
        return determinant(this)
    }

    /**
     * dropColumn accepts a non-negative int corresponding to a column index, and
     * returns the Matrix, with that column dropped.
     * It does this by passing all columns except the column to be dropped
     * to fromColumnVectors.
     * @param col a column index (0 &lt; col &lt; entries[0].length)
     * @return a Matrix with the specified column dropped
     */
    fun dropColumn(col: Int): Matrix {
        return dropColumn(this, col)
    }

    /**
     * dropRow accepts a non-negative int corresponding to a row index, and
     * returns the Matrix, with that row dropped.
     * It does this by passing all rows except the row to be dropped
     * to fromRowVectors.
     * @param row a row index (0 &lt; row &lt; entries.length)
     * @return a Matrix with the specified row dropped
     */
    fun dropRow(row: Int): Matrix {
        return dropRow(this, row)
    }

    /**
     * returns the entry in the row-th row and col-th column of m.
     * @param row the row of the desired entry
     * @param col the column of the desired entry
     * @return the entry in the row-th row and col-th col of m
     */
    fun getEntry(row: Int, col: Int): Double {
        return getEntry(this, row, col)
    }

    /**
     * @return the number of columns in the Matrix
     */
    val numColumns: Int
        get() = getNumColumns(this)

    /**
     * @return the number of rows in the Matrix
     */
    val numRows: Int
        get() = getNumRows(this)

    /**
     * accepts a column number (starting at 0) and returns the entries in
     * the column as a Vector object
     * @param col the number of the desired column
     * (starting at 0, up to this.entries[0].length-1)
     * @return a Vector containing the entries in the desired column
     */
    fun getColumn(col: Int): Vector {
        return getColumn(this, col)
    }

    /**
     * accepts a row number (starting at 0) and returns the entries in
     * the row as a Vector object
     * @param row the number of the desired row (starting at 0, up to this.entries.length-1)
     * @return a Vector containing the entries in the desired row
     */
    fun getRow(row: Int): Vector {
        return getRow(this, row)
    }

    /**
     * isDiagonal checks to see if all values except for the diagonal
     * are zero.
     * @return true if only the diagonal contains non-zero values,
     * false otherwise
     */
    val isDiagonal: Boolean
        get() = isDiagonal(this)

    /**
     * isLowerTriangular checks to see if all values below the diagonal
     * are zero.
     * @return true if all entries below the diagonal are zero, false otherwise
     */
    val isLowerTriangular: Boolean
        get() = isLowerTriangular(this)

    /**
     * checks to see if there is exactly one 1 in each row and column,
     * and only zeroes everywhere else. Matrix must be square.
     * an n-by-n permutation Matrix rearranges the entries of an n-by-1
     * Vector
     * @return true if Matrix is a permutation Matrix, false otherwise
     */
    val isPermutationMatrix: Boolean
        get() = isPermutationMatrix(this)

    /**
     * isSparse checks to see if most of the Matrix entries are zero.
     * "most" is defined as no more than max(num columns, num rows)
     * @return true if the Matrix contains very few nonzero entries,
     * false otherwise
     */
    val isSparse: Boolean
        get() = isSparse(this)

    /**
     * isSparse checks to see if most of the Matrix entries are zero.
     * "most" is defined as no more the specified proportion
     * @param p the desired proportion (0 &lt; p &lt;= 1)
     * @return true if the Matrix contains very few nonzero entries,
     * false otherwise
     */
    fun isSparse(p: Double): Boolean {
        return isSparse(this, p)
    }

    /**
     * isSquare checks the dimensions of the Matrix to see if they are
     * equal. if so, the Matrix is square, and the method returns true.
     * @return true if the number of rows equals the number of columns,
     * false otherwise
     */
    val isSquare: Boolean
        get() = isSquare(this)

    /**
     * isUpperTriangular checks to see if all values above the diagonal
     * are zero.
     * @return true if all entries above the diagonal are zero, false otherwise
     */
    val isUpperTriangular: Boolean
        get() = isUpperTriangular(this)

    /**
     * multiplies each entry in the Matrix m by a real number.
     * @param x a real number (double)
     * @return a Matrix whose entries are the entries in the calling entries,
     * multiplied by x
     */
    fun multiply(x: Double): Matrix {
        return multiply(this, x)
    }

    /**
     * multplies the given vector by the calling entries. Number of entries in the
     * vector must match the number of columns in calling Matrix.
     *
     * M = | a b c |
     * | d e f |
     *
     * u = | x |
     * | y |
     * | z |
     *
     * Mu = | ax + by + cz |
     * | dx + ey + fz |
     *
     * The n-th entry in the returned vector is the dot product of the nth-row of m
     * with u.
     * @param u a Vector object
     * @return a Vector object containing the linear combination mu
     */
    fun multiply(u: Vector): Vector {
        return multiply(this, u)
    }

    /**
     * If the calling Matrix is a, returns the product ab.
     * Number of columns of calling entries must match number of
     * rows of passed Matrix.
     * @param b a Matrix object
     * @return the product ab, where a is the calling entries
     */
    fun multiply(b: Matrix): Matrix {
        return multiply(this, b)
    }

    /**
     * accepts a Vector object and a column index and overwrites the entries
     * in the specified row with the entries in the Vector object
     * if the column length and the Vector length do not match, an
     * IllegalArgumentException will be thrown.
     * @param col an index for the column
     * @param u a Vector object
     * @return a Matrix object with the col-th row replaced with u
     */
    fun setColumn(col: Int, u: Vector): Matrix {
        return setColumn(this, col, u)
    }

    /**
     * accepts an array of doubles, and a column index and overwrites the entries
     * in the specified column with the entries in the given array.
     * if the clumn length and the array length do not match, an
     * IllegalArgumentException will be thrown.
     * @param col an index for the column
     * @param v a an array of doubles
     * @return a Matrix object with the col-th column replaced with v
     */
    fun setColumn(col: Int, v: DoubleArray): Matrix {
        return setColumn(this, col, v)
    }

    /**
     * accepts an Vector object and a row index and overwrites the entries
     * in the specified row with the entries in the Vector object
     * if the row length and the Vector length do not match, an
     * IllegalArgumentException will be thrown.
     * @param row an index for the row
     * @param u a Vector object
     * @return a Matrix object with the row-th row replaced with u
     */
    fun setRow(row: Int, u: Vector): Matrix {
        return setRow(this, row, u)
    }

    /**
     * accepts an array of doubles, and a row index and overwrites the entries
     * in the specified row with the entries in the given array.
     * if the row length and the array length do not match, an
     * IllegalArgumentException will be thrown.
     * @param row an index for the row
     * @param v a an array of doubles
     * @return a Matrix object with the row-th row replaced with v
     */
    fun setRow(row: Int, v: DoubleArray): Matrix {
        return setRow(this, row, v)
    }

    /**
     * Subtracts the entries of n from the corresponding entries in m, and
     * returns the matrix this-n. this and n must have the same shape, or an
     * IllegalArgumentException will be thrown.
     * @param n the matrix on the right of m-n
     * @return a Matrix where each entry is this[row][col] - n[row][col]
     */
    fun subtract(n: Matrix): Matrix {
        return subtract(this, n)
    }

    /**
     * swaps the entries in col1 with the entries in col2.
     * uses two intermediary Vector objects to hold the entries
     * @param col1 the index of the first column to be swapped
     * @param col2 the index of the second column to be swapped
     * @return a Matrix with col1 and col2 swapped
     */
    fun swapColumns(col1: Int, col2: Int): Matrix {
        return swapColumns(this, col1, col2)
    }

    /**
     * swaps the entries in row1 with the entries in row2.
     * uses two intermediary Vector objects to hold the entries
     * @param row1 the index of the first row to be swapped
     * @param row2 the index of the second row to be swapped
     * @return a Matrix with row1 and row2 swapped
     */
    fun swapRows(row1: Int, row2: Int): Matrix {
        return swapRows(this, row1, row2)
    }

    /**
     * decomposes the Matrix object into column vectors
     * @return an array of Vector objects
     */
    fun toColumnVectors(): Array<Vector?> {
        return toColumnVectors(this)
    }

    /**
     * decomposes the Matrix object into row vectors
     * @return an array of Vector objects
     */
    fun toRowVectors(): Array<Vector?> {
        return toRowVectors(this)
    }

    /**
     * returns the sum of the entries on the diagonal of a square entries, which is
     * also the sum of the entries's eigenvalues
     * @return the sum of the entries on the diagonal
     */
    fun trace(): Double {
        return trace(this)
    }

    /**
     * transposes the entries. swaps the rows and columns.
     * @return the transpose of the Matrix object
     */
    fun transpose(): Matrix {
        return transpose(this)
    }

    /**
     * returns a String containing the entries entries in the following format:
     * [[1.2, 3.4, ..., 8.9],
     * [9.7, 7.5, ..., 3.1]]
     * @return a String representation of the Matrix's entries
     */
    override fun toString(): String {
        var str = "["
        for (i in entries.indices) {
            str += getRow(this, i)
            str += if (i < entries.size - 1) {
                ",\n "
            } else {
                "]"
            }
        }
        return str
    }

    companion object {
        /*
    * threshold for double comparisons
    */
        const val THRESHOLD = Double.MIN_VALUE * 1000

        /**
         * adds corresponding entries in the two matrices. Matrix a and Matrix b
         * must have the same dimensions to be compatible.
         * @param a an m-by-n Matrix object
         * @param b an m-by-n Matrix object
         * @return an m-by-n Matrix object whose entries are sums of corresponding
         * entries in a and b
         */
        fun add(a: Matrix, b: Matrix): Matrix {
            require(
                !(a.numRows != b.numRows ||
                        a.numColumns != b.numColumns)
            ) { "Matrix objects have different dimensions." }
            val entries = Array(a.numRows) {
                DoubleArray(
                    a.numColumns
                )
            }
            for (row in a.entries.indices) {
                for (col in a.entries[0].indices) {
                    entries[row][col] = a.entries[row][col] + b.entries[row][col]
                }
            }
            return Matrix(entries)
        }

        /**
         * adds the passed Vector to the specified column of the passed Matrix.
         * throws an IllegalArgumentException if the dimensions don't line up.
         * @param m a Matrix object
         * @param u a Vector object
         * @param col the column in m to which we want to add u
         * @return a Matrix with the values in u added to the specified column
         */
        fun addVectorToColumn(m: Matrix, u: Vector, col: Int): Matrix {
            require(!(col < 0 || col >= m.numColumns)) { "col is not in the correct range" }
            require(
                u.length() == m.getColumn(col).length()
            ) { "Vector length does not match row length" }
            return m.setColumn(col, m.getColumn(col).add(u))
        }

        /**
         * adds the passed Vector to the specified row of the passed Matrix.
         * throws an IllegalArgumentException if the dimensions don't line up.
         * @param m a Matrix object
         * @param u a Vector object
         * @param row the row in m to which we want to add u
         * @return a Matrix with the values in u added to the specified row
         */
        fun addVectorToRow(m: Matrix, u: Vector, row: Int): Matrix {
            require(!(row < 0 || row >= m.numRows)) { "row is not in the correct range" }
            require(
                u.length() == m.getRow(row).length()
            ) { "Vector length does not match row length" }
            return m.setRow(row, m.getRow(row).add(u))
        }

        /**
         * minorMatrix accepts a row and a column, and returns the Matrix object
         * with that row and column dropped.
         * @param m a Matrix object
         * @param row an int
         * @param col an int
         * @return a copy of m, with the specified row and column dropped
         */
        fun minorMatrix(m: Matrix, row: Int, col: Int): Matrix {
            require(!(row < 0 || row >= m.numRows)) { "row is not in the correct range" }
            require(!(col < 0 || col >= m.numColumns)) { "col is not in the correct range" }
            return m.dropRow(row).dropColumn(col)
        }

        /**
         * determinant computes the determinant of a square Matrix object.
         * The determinant is not defined for non-square matrices.
         * Determinant is computed recursively via cofactor expansion across
         * the first row:
         * https://en.wikipedia.org/wiki/Determinant#Laplace's_expansion_and_the_adjugate_entries
         * For a 1x1 Matrix, the determinant is the only entry.
         * @param m a Matrix object
         * @return the determinant of the Matrix (a double)
         */
        fun determinant(m: Matrix): Double {
            require(isSquare(m)) { "Determinant undefined for non-square matrices" }
            var determinant = 0.0
            if (m.isDiagonal || m.isUpperTriangular || m.isLowerTriangular) {
                determinant = 1.0

                // determinant is the product of the entries on the diagonal
                for (i in 0 until m.numRows) {
                    determinant *= m.getEntry(i, i)
                }
            } else {
                if (getNumRows(m) == 1) {
                    determinant = getEntry(m, 0, 0)
                } else {
                    for (col in 0 until m.numColumns) {
                        determinant += Math.pow(-1.0, col.toDouble()) *
                                m.getEntry(0, col) *
                                m.minorMatrix(0, col).determinant()
                    }
                }
            }
            return determinant
        }

        /**
         * dropColumn accepts a Matrix object and a non-negative int corresponding
         * to a column index, and returns the Matrix, with that column dropped.
         * It does this by passing all columns except the column to be dropped
         * to fromColumnVectors.
         * @param m a Matrix object
         * @param col a column index (0 &lt; col &lt; entries[0].length)
         * @return a Matrix with the specified column dropped
         */
        fun dropColumn(m: Matrix, col: Int): Matrix {
            if (col < 0 || col >= m.numColumns) {
                println("col = $col")
                println("num columns = " + m.numColumns)
                throw IllegalArgumentException("col is out of range")
            }
            val columns = arrayOfNulls<Vector>(m.numColumns - 1)
            for (i in m.entries[0].indices) {
                // if we're to the left of the column to be dropped, columns[i] = m.getColumn(i)
                if (i < col) {
                    columns[i] = m.getColumn(i)
                }

                // if we're to the right of the column to be dropped, columns[i-1] = m.getColumn(i)
                if (i > col) {
                    columns[i - 1] = m.getColumn(i)
                }
            }
            return fromColumnVectors()
        }

        /**
         * dropRow accepts a Matrix object and a non-negative int corresponding
         * to a row index, and returns the Matrix, with that row dropped. It does
         * this by passing all row except the row to be dropped to fromRowVectors.
         * @param m a Matrix object
         * @param row a row index (0 &lt; col &lt; entries.length)
         * @return a Matrix with the specified row dropped
         */
        fun dropRow(m: Matrix, row: Int): Matrix {
            require(!(row < 0 || row >= m.numRows)) { "row is out of range" }
            val u = arrayOfNulls<Vector>(m.numRows - 1)
            for (i in 0 until m.numRows) {
                if (i < row) {
                    u[i] = m.getRow(i)
                }
                if (i > row) {
                    u[i - 1] = m.getRow(i)
                }
            }
            return fromRowVectors()
        }

        /**
         * Constructs a Matrix object from column vectors.
         * Vectors must all have the same length (the length of the first Vector in
         * the array), or an IllegalArgumentException will be thrown.
         * @param vectors an array of Vector objects
         * @return a Matrix whose rows are the passed vectors
         */
        fun fromColumnVectors(vararg vectors: Vector): Matrix {
            return transpose(fromRowVectors(*vectors))
        }

        /**
         * Constructs a Matrix object from row vectors.
         * Vectors must all have the same length (the length of the first Vector in
         * the array), or an IllegalArgumentException will be thrown.
         * @param vectors an array of Vector objects
         * @return a Matrix whose rows are the passed vectors
         */
        fun fromRowVectors(vararg vectors: Vector): Matrix {
            for (u in vectors) {
                require(u.length() == vectors[0].length()) { "Vectors do not have the same length." }
            }
            val entries = Array(vectors.size) {
                DoubleArray(
                    vectors[0].length()
                )
            }
            for (row in vectors.indices) {
                for (col in 0 until vectors[row].length()) {
                    entries[row][col] = vectors[row][col]
                }
            }
            return Matrix(entries)
        }

        /**
         * returns the entry in the row-th row and col-th column of m.
         * @param m a Matrix object
         * @param row the row of the desired entry
         * @param col the column of the desired entry
         * @return the entry in the row-th row and col-th col of m
         */
        fun getEntry(m: Matrix, row: Int, col: Int): Double {
            require(!(row < 0 || row > m.entries.size)) { "Invalid value for row." }
            require(!(col < 0 || col > m.entries[0].size)) { "Invalid value for col." }
            return m.entries[row][col]
        }

        /**
         * @param m a Matrix object
         * @return the number of columns in the Matrix
         */
        fun getNumColumns(m: Matrix): Int {
            return m.entries[0].size
        }

        /**
         * @param m a Matrix object
         * @return the number of columns in the Matrix
         */
        fun getNumRows(m: Matrix): Int {
            return m.entries.size
        }

        /**
         * accepts a column number (starting at 0) and returns the entries in
         * the column as a Vector object
         * @param m the Matrix object we want the column from
         * @param col the number of the desired column
         * (starting at 0, up to this.entries[0].length-1)
         * @return a Vector containing the entries in the desired column
         */
        fun getColumn(m: Matrix, col: Int): Vector {
            require(!(col >= m.entries[0].size || col < 0)) { "Column is out of range" }
            val columnVector = DoubleArray(m.entries.size)
            for (row in m.entries.indices) {
                columnVector[row] = m.entries[row][col]
            }
            return Vector(*columnVector)
        }

        /**
         * accepts a row number (starting at 0) and returns the entries in
         * the row as a Vector object
         * @param m the Matrix object we want the row from
         * @param row the number of the desired row (starting at 0, up to this.entries.length-1)
         * @return a Vector containing the entries in the desired row
         */
        fun getRow(m: Matrix, row: Int): Vector {
            require(!(row >= m.entries.size || row < 0)) { "Row does not exist in Matrix" }
            return Vector(*m.entries[row])
        }

        /**
         * getIdentityMatrix returns an n by n Matrix with all zeroes
         * and ones on the diagonal.
         * @param n an integer greater than or equal to 1
         * @return an n-by-n Matrix with ones on the diagonal and zeroes
         * everywhere else
         */
        fun identityMatrix(n: Int): Matrix {
            require(n >= 1) { "n must be >= 1" }
            val entries = Array(n) {
                DoubleArray(
                    n
                )
            }
            for (i in 0 until n) {
                entries[i][i] = 1.0
            }
            return Matrix(entries)
        }

        /**
         * isDiagonal checks to see if all values except for the diagonal
         * are zero.
         * @param m a square Matrix object
         * @return true if only the diagonal contains non-zero values,
         * false otherwise
         */
        fun isDiagonal(m: Matrix): Boolean {
            require(isSquare(m)) { "Matrix is not square." }
            for (row in m.entries.indices) {
                for (col in m.entries[row].indices) {
                    if (row != col) { // if we're not on the diagonal

                        // if the value is non-zero
                        if (Math.abs(m.entries[row][col]) > THRESHOLD) {
                            return false
                        }
                    }
                }
            }
            return true
        }

        /**
         * isLowerTriangular checks to see if all values below the diagonal
         * are zero.
         * @param m a Matrix object
         * @return true if all entries below the diagonal are zero, false otherwise
         */
        fun isLowerTriangular(m: Matrix): Boolean {
            for (row in 1 until m.entries.size) {
                for (col in 0 until row) { // only below the diagonal
                    if (Math.abs(m.entries[row][col]) > THRESHOLD) {
                        return false
                    }
                }
            }
            return true
        }

        /**
         * checks to see if there is exactly one 1 in each row and column,
         * and only zeroes everywhere else. Matrix must be square.
         * an n-by-n permutation Matrix rearranges the entries of an n-by-1
         * Vector
         * @param m a Matrix object
         * @return true if Matrix is a permutation Matrix, false otherwise
         */
        fun isPermutationMatrix(m: Matrix): Boolean {
            require(isSquare(m)) { "Matrix is not square" }
            for (i in 0 until m.numRows) {
                if (!(m.getRow(i).isCanonicalBasisVector
                            && m.getColumn(i).isCanonicalBasisVector)
                ) {
                    return false
                }
            }
            return true
        }

        /**
         * isSparse checks to see if most of the Matrix entries are zero.
         * "most" is defined as no more than max(num columns, num rows)
         * @param m a Matrix object
         * @return true if the Matrix contains very few nonzero entries,
         * false otherwise
         */
        fun isSparse(m: Matrix): Boolean {
            var numNonzero = 0
            for (row in m.entries.indices) {
                for (col in m.entries[row].indices) {
                    if (Math.abs(m.entries[row][col]) > THRESHOLD) {
                        numNonzero++
                    }
                }
            }
            return numNonzero <= Math.max(m.numRows, m.numColumns)
        }

        /**
         * isSparse checks to see if most of the Matrix entries are zero.
         * "most" is defined as no more the specified proportion
         * @param m a Matrix object
         * @param p the desired proportion (0 &lt; p &lt;= 1)
         * @return true if the Matrix contains very few nonzero entries,
         * false otherwise
         */
        fun isSparse(m: Matrix, p: Double): Boolean {
            require(!(p < THRESHOLD || p > 1)) { "p is not in the correct range (0,1]" }
            var numNonzero = 0
            for (row in m.entries.indices) {
                for (col in m.entries[row].indices) {
                    if (Math.abs(m.entries[row][col]) > THRESHOLD) {
                        numNonzero++
                    }
                }
            }
            return numNonzero.toDouble() / (m.numRows * m.numColumns) <= p
        }

        /**
         * isSquare checks the dimensions of the Matrix to see if they are
         * equal. if so, the Matrix is square, and the method returns true.
         * @param m a Matrix object
         * @return true if the number of rows equals the number of columns,
         * false otherwise
         */
        fun isSquare(m: Matrix): Boolean {
            return m.numRows == m.numColumns
        }

        /**
         * isUpperTriangular checks to see if all values above the diagonal
         * are zero.
         * @param m a Matrix object
         * @return true if all entries above the diagonal are zero, false otherwise
         */
        fun isUpperTriangular(m: Matrix): Boolean {
            for (row in m.entries.indices) {
                for (col in row + 1 until m.entries[row].size) { // only below the diagonal
                    if (Math.abs(m.entries[row][col]) > THRESHOLD) {
                        return false
                    }
                }
            }
            return true
        }

        /**
         * multiplies each entry in the Matrix m by a real number.
         * @param m a Matrix object
         * @param x a real number (double)
         * @return a Matrix whose entries are the entries in m,
         * multiplied by x
         */
        fun multiply(m: Matrix, x: Double): Matrix {
            val entries = Array(m.numRows) {
                DoubleArray(
                    m.numColumns
                )
            }
            for (row in 0 until m.numRows) {
                for (col in 0 until m.numColumns) {
                    entries[row][col] = m.getEntry(row, col) * x
                }
            }
            return Matrix(entries)
        }

        /**
         * multiplies the given vector by the given entries. this method treats the
         * vector as a column vector. Number of entries in the vector must match the
         * number of columns in the entries passed.
         *
         * M = | a b c |
         * | d e f |
         *
         * u = | x |
         * | y |
         * | z |
         *
         * Mu = | ax + by + cz |
         * | dx + ey + fz |
         *
         * The n-th entry in the returned vector is the dot product of the nth-row of m
         * with u.
         *
         * @param m the Matrix object
         * @param u the Vector object
         * @return a Vector object containing the linear combination mu
         */
        fun multiply(m: Matrix, u: Vector): Vector {
            require(u.length() == m.numRows) { "Incompatible shapes.\n" }
            val result = DoubleArray(m.entries.size)
            for (i in m.entries.indices) {
                result[i] = m.getRow(i).dot(u)
            }
            return Vector(*result)
        }

        /**
         * multiplies two matrices together via entries multiplication.
         * the [i][j]-th entry is the dot product of row i from Matrix a
         * and column j from Matrix b.
         * if a is an m-by-n Matrix and b is an n-by-p Matrix, then the
         * returned entries ab will be an m-by-p Matrix.
         * The number of columns in a must match the number of rows in b,
         * or else an IllegalArgumentException will be thrown.
         * @param a an m-by-n Matrix object
         * @param b an n-by-p Matrix object
         * @return ab: an m-by-p Matrix object
         */
        fun multiply(a: Matrix, b: Matrix): Matrix {
            require(a.numColumns == b.numRows) { "Matrix dimensions are incompatible." }
            val entries = Array(a.numRows) {
                DoubleArray(
                    b.numColumns
                )
            }
            for (row in 0 until a.numRows) {
                for (col in 0 until b.numColumns) {
                    entries[row][col] = Vector.dot(a.getRow(row), b.getColumn(col))
                }
            }
            return Matrix(entries)
        }

        /**
         * accepts a Matrix object, a Vector object and a column index and
         * overwrites the entries in the specified column with the entries in
         * the given Vector.
         * if the column length and the Vector length do not match, an
         * IllegalArgumentException will be thrown.
         * @param m a Matrix object
         * @param col an index for the col
         * @param u a Vector object containing the entries we want
         * @return a Matrix object with the col-th column replaced with u
         */
        fun setColumn(m: Matrix, col: Int, u: Vector): Matrix {
            require(m.entries.size == u.length()) { "Vector length and does not match column length." }
            require(!(col < 0 || col >= m.numColumns)) { "Invalid column: $col" }
            val n = Matrix(m)
            for (i in n.entries.indices) {
                n.entries[i][col] = u[i]
            }
            return n
        }

        /**
         * accepts an array of doubles, a Vector object, and a column index and
         * overwrites the entries in the specified column with the entries in
         * the given array
         * if the column length and the array length do not match, an
         * IllegalArgumentException will be thrown.
         * @param m a Matrix object
         * @param col an index for the col
         * @param v an array of doubles containing the entries we want
         * @return a Matrix object with the col-th column replaced with v
         */
        fun setColumn(m: Matrix, col: Int, v: DoubleArray): Matrix {
            require(m.entries.size == v.size) { "Array length and does not match row length." }
            return setColumn(m, col, Vector(*v))
        }

        /**
         * Changes the entry in the row-th row and the col-th column to value.
         * @param m a Matrix object
         * @param value a double––the value we want to set
         * @param row the index of the row
         * @param col the index of the column
         * @return the Matrix m, modified so the entry at [row][col] is value
         */
        fun setEntry(m: Matrix, value: Double, row: Int, col: Int): Matrix {
            require(!(row < 0 || row >= m.numRows)) { "row is out of range" }
            require(!(col < 0 || col >= m.numColumns)) { "col is out of range" }
            val n = Matrix(m)
            n.entries[row][col] = value
            return n
        }

        /**
         * accepts a Matrix object, a Vector object and a row index and
         * overwrites the entries in the specified row with the entries in
         * the given Vector.
         * if the row length and the Vector length do not match, an
         * IllegalArgumentException will be thrown.
         * @param m a Matrix object
         * @param row an index for the row
         * @param u a Vector object containing the entries we want
         * @return a Matrix object with the row-th row replaced with u
         */
        fun setRow(m: Matrix, row: Int, u: Vector): Matrix {
            require(m.entries[row].size == u.length()) { "Vector length and does not match row length." }
            require(!(row < 0 || row >= m.numRows)) { "Invalid row: $row" }
            val n = Matrix(m)
            for (i in n.entries[row].indices) {
                n.entries[row][i] = u[i]
            }
            return n
        }

        /**
         * accepts a Matrix object, an array of doubles, and a row index and
         * overwrites the entries in the specified row with the entries in
         * the given array.
         * if the row length and the Vector length do not match, an
         * IllegalArgumentException will be thrown.
         * @param m a Matrix object
         * @param row an index for the row
         * @param v a an array of doubles
         * @return a Matrix object with the row-th row replaced with v
         */
        fun setRow(m: Matrix, row: Int, v: DoubleArray): Matrix {
            require(!(row < 0 || row >= m.numRows)) { "Invalid row: $row" }
            require(m.entries[row].size == v.size) { "Array length and does not match row length." }
            return setRow(m, row, Vector(*v))
        }

        /**
         * returns an array of two ints containing the dimensions of the Matrix
         * @param m a Matrix object
         * @return an array containing {numRows, numCols}
         */
        fun shape(m: Matrix): IntArray {
            return intArrayOf(m.numRows, m.numColumns)
        }

        /**
         * Subtracts the entries of n from the corresponding entries in m, and
         * returns the matrix m-n. m and n must have the same shape, or an
         * IllegalArgumentException will be thrown.
         * @param m the matrix on the left of m-n
         * @param n the matrix on the right of m-n
         * @return a Matrix where each entry is m[row][col] - n[row][col]
         */
        fun subtract(m: Matrix, n: Matrix): Matrix {
            return add(m, multiply(n, -1.0))
        }

        /**
         * swaps the entries in col1 with the entries in col2.
         * uses two intermediary Vector objects to hold the entries
         * @param m a Matrix object
         * @param col1 the index of the first column to be swapped
         * @param col2 the index of the second column to be swapped
         * @return a Matrix with col1 and col2 swapped
         */
        fun swapColumns(m: Matrix, col1: Int, col2: Int): Matrix {
            require(!(col1 < 0 || col1 >= m.numColumns)) { "Invalid column: $col1" }
            require(!(col2 < 0 || col2 >= m.numColumns)) { "Invalid column: $col2" }
            return Matrix(m).setColumn(col1, m.getColumn(col2))
                .setColumn(col2, m.getColumn(col1))
        }

        /**
         * swaps the entries in row1 with the entries in row2.
         * uses two intermediary Vector objects to hold the entries
         * @param m a Matrix object
         * @param row1 the index of the first row to be swapped
         * @param row2 the index of the second row to be swapped
         * @return a Matrix with row1 and row2 swapped
         */
        fun swapRows(m: Matrix, row1: Int, row2: Int): Matrix {
            require(!(row1 < 0 || row1 >= m.numRows)) { "Invalid row: $row1" }
            require(!(row2 < 0 || row2 >= m.numRows)) { "Invalid row: $row2" }
            return Matrix(m).setRow(row1, m.getRow(row2))
                .setRow(row2, m.getRow(row1))
        }

        /**
         * decomposes the Matrix object into column vectors
         * @param m a Matrix object
         * @return an array of Vector objects
         */
        fun toColumnVectors(m: Matrix): Array<Vector?> {
            return toRowVectors(m.transpose())
        }

        /**
         * decomposes the Matrix object into row vectors
         * @param m a Matrix object
         * @return an array of Vector objects
         */
        fun toRowVectors(m: Matrix): Array<Vector?> {
            val v = arrayOfNulls<Vector>(m.numRows)
            for (i in 0 until m.numRows) {
                v[i] = Vector(m.getRow(i))
            }
            return v
        }

        /**
         * returns the sum of the entries on the diagonal of a square entries, which is
         * also the sum of the entries's eigenvalues
         * @param m a square Matrix object
         * @return the sum of the entries on the diagonal
         */
        fun trace(m: Matrix): Double {
            require(isSquare(m)) { "Trace is not defined for non-square entries" }
            var trace = 0.0
            for (i in 0 until getNumRows(m)) {
                trace += m.getEntry(i, i)
            }
            return trace
        }

        /**
         * transposes the entries. swaps the rows and columns.
         * @param m a Matrix object
         * @return the transpose of the Matrix object
         */
        fun transpose(m: Matrix): Matrix {
            val n = Array(m.numColumns) {
                DoubleArray(
                    m.numRows
                )
            }
            for (row in n.indices) {
                for (col in n[row].indices) {
                    n[row][col] = m.entries[col][row]
                }
            }
            return Matrix(n)
        }
    }
}