package com.beema.kernel.domain.metadata;

/**
 * Data types for metadata attributes.
 *
 * Used for validation and UI component selection.
 */
public enum AttributeDataType {
    /** String/text value */
    STRING,

    /** Integer number */
    INTEGER,

    /** Decimal/floating point number */
    DECIMAL,

    /** Boolean true/false */
    BOOLEAN,

    /** Date (no time) */
    DATE,

    /** Date with time */
    DATETIME,

    /** Array/list of values */
    ARRAY,

    /** Nested object */
    OBJECT
}
