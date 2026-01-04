package app.bloque.sdk.core

/**
 * Marks declarations that are internal to Bloque SDK and should not be used by external code.
 * Such declarations may be changed or removed in future versions without notice.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Bloque SDK API and should not be used. It may be changed or removed in future versions."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR
)
annotation class InternalBloqueApi
