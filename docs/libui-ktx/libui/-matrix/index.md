[libui-ktx](../../index.md) / [libui](../index.md) / [Matrix](./index.md)

# Matrix

`class Matrix : `[`Disposable`](../-disposable/index.md)`<<ERROR CLASS>>`

Defines a transformation (e.g. rotation, translation)

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `Matrix()`<br>Defines a transformation (e.g. rotation, translation) |

### Inherited Properties

| Name | Summary |
|---|---|
| [disposed](../-disposable/disposed.md) | `val disposed: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns `true` if object was disposed - in this case [disposed](../-disposable/disposed.md) will do nothing, all other operations are invalid and will `throw Error("Resource is disposed")`. |
| [ptr](../-disposable/ptr.md) | `val ptr: <ERROR CLASS><`[`T`](../-disposable/index.md#T)`>` |

### Inherited Functions

| Name | Summary |
|---|---|
| [dispose](../-disposable/dispose.md) | `open fun dispose(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Dispose and free all allocated native resources. |

### Extension Properties

| Name | Summary |
|---|---|
| [invertible](../invertible.md) | `val `[`Matrix`](./index.md)`.invertible: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns `true` if the matrix is invertible. |
| [point](../point.md) | `val `[`Matrix`](./index.md)`.point: `[`Point`](../-point/index.md)<br>Returns the transformed point. |
| [size](../size.md) | `val `[`Matrix`](./index.md)`.size: `[`Size`](../-size/index.md)<br>Returns the transformed size. |

### Extension Functions

| Name | Summary |
|---|---|
| [invert](../invert.md) | `fun `[`Matrix`](./index.md)`.invert(): <ERROR CLASS>`<br>Inverts the matrix. |
| [multiply](../multiply.md) | `fun `[`Matrix`](./index.md)`.multiply(other: `[`Matrix`](./index.md)`): <ERROR CLASS>`<br>Sets the matrix to the product of itself with [other](../multiply.md#libui$multiply(libui.Matrix, libui.Matrix)/other) matrix. |
| [rotate](../rotate.md) | `fun `[`Matrix`](./index.md)`.rotate(x: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, y: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, amount: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`): <ERROR CLASS>`<br>Rotates paths by [amount](../rotate.md#libui$rotate(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double)/amount) *radians* around ([x](../rotate.md#libui$rotate(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double)/x), [y](../rotate.md#libui$rotate(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double)/y)). |
| [scale](../scale.md) | `fun `[`Matrix`](./index.md)`.scale(xCenter: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, yCenter: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, x: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, y: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`): <ERROR CLASS>`<br>Scales pathes by a factor of [x](../scale.md#libui$scale(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double, kotlin.Double)/x) and [y](../scale.md#libui$scale(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double, kotlin.Double)/y) with ([xCenter](../scale.md#libui$scale(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double, kotlin.Double)/xCenter), [yCenter](../scale.md#libui$scale(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double, kotlin.Double)/yCenter)) as the scale center. |
| [skew](../skew.md) | `fun `[`Matrix`](./index.md)`.skew(x: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, y: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, xamount: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, yamount: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`): <ERROR CLASS>`<br>Skews path by [xamount](../skew.md#libui$skew(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double, kotlin.Double)/xamount) *radians* horizontally and by [yamount](../skew.md#libui$skew(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double, kotlin.Double)/yamount) *radians* vertically around ([x](../skew.md#libui$skew(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double, kotlin.Double)/x), [y](../skew.md#libui$skew(libui.Matrix, kotlin.Double, kotlin.Double, kotlin.Double, kotlin.Double)/y)) |
| [translate](../translate.md) | `fun `[`Matrix`](./index.md)`.translate(x: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`, y: `[`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.html)`): <ERROR CLASS>`<br>Moves paths over by [x](../translate.md#libui$translate(libui.Matrix, kotlin.Double, kotlin.Double)/x) to the right and [y](../translate.md#libui$translate(libui.Matrix, kotlin.Double, kotlin.Double)/y) down. |