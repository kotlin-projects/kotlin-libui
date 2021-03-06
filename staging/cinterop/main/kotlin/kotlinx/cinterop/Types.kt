/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.cinterop

/**
 * The entity which has an associated native pointer.
 * Subtypes are supposed to represent interpretations of the pointed data or code.
 *
 * This interface is likely to be handled by compiler magic and shouldn't be subtyped by arbitrary classes.
 *
 * TODO: the behavior of [equals], [hashCode] and [toString] differs on Native and JVM backends.
 */
open class NativePointed internal constructor(rawPtr: NonNullNativePtr) {
    var rawPtr = rawPtr.toNativePtr()
        internal set
}

// `null` value of `NativePointed?` is mapped to `nativeNullPtr`.
val NativePointed?.rawPtr: NativePtr
    get() = if (this != null) this.rawPtr else nativeNullPtr

/**
 * Returns interpretation of entity with given pointer.
 *
 * @param T must not be abstract
 */
inline fun <reified T : NativePointed> interpretPointed(ptr: NativePtr): T = interpretNullablePointed<T>(ptr)!!

private class OpaqueNativePointed(rawPtr: NativePtr) : NativePointed(rawPtr.toNonNull())

fun interpretOpaquePointed(ptr: NativePtr): NativePointed = interpretPointed<OpaqueNativePointed>(ptr)
fun interpretNullableOpaquePointed(ptr: NativePtr): NativePointed? = interpretNullablePointed<OpaqueNativePointed>(ptr)

/**
 * Changes the interpretation of the pointed data or code.
 */
inline fun <reified T : NativePointed> NativePointed.reinterpret(): T = interpretPointed(this.rawPtr)

/**
 * C data or code.
 */
abstract class CPointed(rawPtr: NativePtr) : NativePointed(rawPtr.toNonNull())

/**
 * Represents a reference to (possibly empty) sequence of C values.
 * It can be either a stable pointer [CPointer] or a sequence of immutable values [CValues].
 *
 * [CValuesRef] is designed to be used as Kotlin representation of pointer-typed parameters of C functions.
 * When passing [CPointer] as [CValuesRef] to the Kotlin binding method, the C function receives exactly this pointer.
 * Passing [CValues] has nearly the same semantics as passing by value: the C function receives
 * the pointer to the temporary copy of these values, and the caller can't observe the modifications to this copy.
 * The copy is valid until the C function returns.
 * There are also other implementations of [CValuesRef] that provide temporary pointer,
 * e.g. Kotlin Native specific [refTo] functions to pass primitive arrays directly to native.
 */
abstract class CValuesRef<T : CPointed> {
    /**
     * If this reference is [CPointer], returns this pointer.
     * Otherwise copies the referenced values to [placement] and returns the pointer to the copy.
     */
    abstract fun getPointer(scope: AutofreeScope): CPointer<T>
}

/**
 * The (possibly empty) sequence of immutable C values.
 * It is self-contained and doesn't depend on native memory.
 */
abstract class CValues<T : CVariable> : CValuesRef<T>() {
    /**
     * Copies the values to [placement] and returns the pointer to the copy.
     */
    override abstract fun getPointer(scope: AutofreeScope): CPointer<T>

    // TODO: optimize
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CValues<*>) return false

        val thisBytes = this.getBytes()
        val otherBytes = other.getBytes()

        if (thisBytes.size != otherBytes.size) {
            return false
        }

        for (index in 0 .. thisBytes.size - 1) {
            if (thisBytes[index] != otherBytes[index]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        for (byte in this.getBytes()) {
            result = result * 31 + byte
        }
        return result
    }

    abstract val size: Int
}

fun <T : CVariable> CValues<T>.placeTo(scope: AutofreeScope) = this.getPointer(scope)

/**
 * The single immutable C value.
 * It is self-contained and doesn't depend on native memory.
 *
 * TODO: consider providing an adapter instead of subtyping [CValues].
 */
abstract class CValue<T : CVariable> : CValues<T>()

/**
 * C pointer.
 */
class CPointer<T : CPointed> internal constructor(@PublishedApi internal val value: NonNullNativePtr) : CValuesRef<T>() {

    // TODO: replace by [value].
    @Suppress("NOTHING_TO_INLINE")
    inline val rawValue: NativePtr get() = value.toNativePtr()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true // fast path
        }

        return (other is CPointer<*>) && (rawValue == other.rawValue)
    }

    override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    override fun toString() = this.cPointerToString()

    override fun getPointer(scope: AutofreeScope) = this
}

/**
 * Returns the pointer to this data or code.
 */
val <T : CPointed> T.ptr: CPointer<T>
    get() = interpretCPointer(this.rawPtr)!!

/**
 * Returns the corresponding [CPointed].
 *
 * @param T must not be abstract
 */
inline val <reified T : CPointed> CPointer<T>.pointed: T
    get() = interpretPointed<T>(this.rawValue)

// `null` value of `CPointer?` is mapped to `nativeNullPtr`
val CPointer<*>?.rawValue: NativePtr
    get() = if (this != null) this.rawValue else nativeNullPtr

fun <T : CPointed> CPointer<*>.reinterpret(): CPointer<T> = interpretCPointer(this.rawValue)!!

fun <T : CPointed> CPointer<T>?.toLong() = this.rawValue.toLong()

fun <T : CPointed> Long.toCPointer(): CPointer<T>? = interpretCPointer(nativeNullPtr + this)

/**
 * The [CPointed] without any specified interpretation.
 */
abstract class COpaque(rawPtr: NativePtr) : CPointed(rawPtr) // TODO: should it correspond to COpaquePointer?

/**
 * The pointer with an opaque type.
 */
typealias COpaquePointer = CPointer<out CPointed> // FIXME

/**
 * The variable containing a [COpaquePointer].
 */
typealias COpaquePointerVar = CPointerVarOf<COpaquePointer>

/**
 * The C data variable located in memory.
 *
 * The non-abstract subclasses should represent the (complete) C data type and thus specify size and alignment.
 * Each such subclass must have a companion object which is a [Type].
 */
abstract class CVariable(rawPtr: NativePtr) : CPointed(rawPtr) {

    /**
     * The (complete) C data type.
     *
     * @param size the size in bytes of data of this type
     * @param align the alignments in bytes that is enough for this data type.
     * It may be greater than actually required for simplicity.
     */
    open class Type(val size: Long, val align: Int) {

        init {
            require(size % align == 0L)
        }

    }
}

inline fun <reified T : CVariable> sizeOf() = typeOf<T>().size
inline fun <reified T : CVariable> alignOf() = typeOf<T>().align

/**
 * Returns the member of this [CStructVar] which is located by given offset in bytes.
 */
inline fun <reified T : CPointed> CStructVar.memberAt(offset: Long): T {
    return interpretPointed<T>(this.rawPtr + offset)
}

inline fun <reified T : CVariable> CStructVar.arrayMemberAt(offset: Long): CArrayPointer<T> {
    return interpretCPointer<T>(this.rawPtr + offset)!!
}

/**
 * The C struct-typed variable located in memory.
 */
abstract class CStructVar(rawPtr: NativePtr) : CVariable(rawPtr) {
    open class Type(size: Long, align: Int) : CVariable.Type(size, align)
}

/**
 * The C primitive-typed variable located in memory.
 */
sealed class CPrimitiveVar(rawPtr: NativePtr) : CVariable(rawPtr) {
    // aligning by size is obviously enough
    open class Type(size: Int) : CVariable.Type(size.toLong(), align = size)
}

interface CEnum {
    val value: Any
}
abstract class CEnumVar(rawPtr: NativePtr) : CPrimitiveVar(rawPtr)

// generics below are used for typedef support
// these classes are not supposed to be used directly, instead the typealiases are provided.

@Suppress("FINAL_UPPER_BOUND")
class BooleanVarOf<T : Boolean>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(1)
}

@Suppress("FINAL_UPPER_BOUND")
class ByteVarOf<T : Byte>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(1)
}

@Suppress("FINAL_UPPER_BOUND")
class ShortVarOf<T : Short>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(2)
}

@Suppress("FINAL_UPPER_BOUND")
class IntVarOf<T : Int>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(4)
}

@Suppress("FINAL_UPPER_BOUND")
class LongVarOf<T : Long>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(8)
}

@Suppress("FINAL_UPPER_BOUND")
class UByteVarOf<T : UByte>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(1)
}

@Suppress("FINAL_UPPER_BOUND")
class UShortVarOf<T : UShort>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(2)
}

@Suppress("FINAL_UPPER_BOUND")
class UIntVarOf<T : UInt>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(4)
}

@Suppress("FINAL_UPPER_BOUND")
class ULongVarOf<T : ULong>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(8)
}

@Suppress("FINAL_UPPER_BOUND")
class FloatVarOf<T : Float>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(4)
}

@Suppress("FINAL_UPPER_BOUND")
class DoubleVarOf<T : Double>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    companion object : Type(8)
}

typealias BooleanVar = BooleanVarOf<Boolean>
typealias ByteVar = ByteVarOf<Byte>
typealias ShortVar = ShortVarOf<Short>
typealias IntVar = IntVarOf<Int>
typealias LongVar = LongVarOf<Long>
typealias UByteVar = UByteVarOf<UByte>
typealias UShortVar = UShortVarOf<UShort>
typealias UIntVar = UIntVarOf<UInt>
typealias ULongVar = ULongVarOf<ULong>
typealias FloatVar = FloatVarOf<Float>
typealias DoubleVar = DoubleVarOf<Double>

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : Boolean> BooleanVarOf<T>.value: T
    get() {
        val byte = nativeMemUtils.getByte(this)
        return byte.toBoolean() as T
    }
    set(value) = nativeMemUtils.putByte(this, value.toByte())

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toByte(): Byte = if (this) 1 else 0

@Suppress("NOTHING_TO_INLINE")
inline fun Byte.toBoolean() = (this - 0 != 0)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : Byte> ByteVarOf<T>.value: T
    get() = nativeMemUtils.getByte(this) as T
    set(value) = nativeMemUtils.putByte(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : Short> ShortVarOf<T>.value: T
    get() = nativeMemUtils.getShort(this) as T
    set(value) = nativeMemUtils.putShort(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : Int> IntVarOf<T>.value: T
    get() = nativeMemUtils.getInt(this) as T
    set(value) = nativeMemUtils.putInt(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : Long> LongVarOf<T>.value: T
    get() = nativeMemUtils.getLong(this) as T
    set(value) = nativeMemUtils.putLong(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : UByte> UByteVarOf<T>.value: T
    get() = nativeMemUtils.getByte(this).toUByte() as T
    set(value) = nativeMemUtils.putByte(this, value.toByte())

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : UShort> UShortVarOf<T>.value: T
    get() = nativeMemUtils.getShort(this).toUShort() as T
    set(value) = nativeMemUtils.putShort(this, value.toShort())

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : UInt> UIntVarOf<T>.value: T
    get() = nativeMemUtils.getInt(this).toUInt() as T
    set(value) = nativeMemUtils.putInt(this, value.toInt())

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : ULong> ULongVarOf<T>.value: T
    get() = nativeMemUtils.getLong(this).toULong() as T
    set(value) = nativeMemUtils.putLong(this, value.toLong())

// TODO: ensure native floats have the appropriate binary representation

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : Float> FloatVarOf<T>.value: T
    get() = nativeMemUtils.getFloat(this) as T
    set(value) = nativeMemUtils.putFloat(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
var <T : Double> DoubleVarOf<T>.value: T
    get() = nativeMemUtils.getDouble(this) as T
    set(value) = nativeMemUtils.putDouble(this, value)


class CPointerVarOf<T : CPointer<*>>(rawPtr: NativePtr) : CVariable(rawPtr) {
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

/**
 * The C data variable containing the pointer to `T`.
 */
typealias CPointerVar<T> = CPointerVarOf<CPointer<T>>

/**
 * The value of this variable.
 */
@Suppress("UNCHECKED_CAST")
inline var <P : CPointer<*>> CPointerVarOf<P>.value: P?
    get() = interpretCPointer<CPointed>(nativeMemUtils.getNativePtr(this)) as P?
    set(value) = nativeMemUtils.putNativePtr(this, value.rawValue)

/**
 * The code or data pointed by the value of this variable.
 * 
 * @param T must not be abstract
 */
inline var <reified T : CPointed, reified P : CPointer<T>> CPointerVarOf<P>.pointed: T?
    get() = this.value?.pointed
    set(value) {
        this.value = value?.ptr as P?
    }

inline operator fun <reified T : CVariable> CPointer<T>.get(index: Long): T {
    val offset = if (index == 0L) {
        0L // optimization for JVM impl which uses reflection for now.
    } else {
        index * sizeOf<T>()
    }
    return interpretPointed(this.rawValue + offset)
}

inline operator fun <reified T : CVariable> CPointer<T>.get(index: Int): T = this.get(index.toLong())

@Suppress("NOTHING_TO_INLINE")
@JvmName("plus\$CPointer")
inline operator fun <T : CPointerVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * pointerSize)

@Suppress("NOTHING_TO_INLINE")
@JvmName("plus\$CPointer")
inline operator fun <T : CPointerVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.get(index: Int): T? =
        (this + index)!!.pointed.value

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.set(index: Int, value: T?) {
    (this + index)!!.pointed.value = value
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.get(index: Long): T? =
        (this + index)!!.pointed.value

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.set(index: Long, value: T?) {
    (this + index)!!.pointed.value = value
}

typealias CArrayPointer<T> = CPointer<T>
typealias CArrayPointerVar<T> = CPointerVar<T>

/**
 * The C function.
 */
class CFunction<T : Function<*>>(rawPtr: NativePtr) : CPointed(rawPtr)

/**
 * Returns a pointer to C function which calls given Kotlin *static* function.
 *
 * @param function must be *static*, i.e. an (unbound) reference to a Kotlin function or
 * a closure which doesn't capture any variable
 */
@Deprecated("The function type is too general. Supply argument with known arity.", level = DeprecationLevel.ERROR)
external fun <F : Function<*>> staticCFunction(function: F): CPointer<CFunction<F>>