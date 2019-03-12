package com.firebase.ui.firestore.paging

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Query.Direction.*

/**
 * Factory for direct and reverse [Query].
 *
 * @property collectionReference The collection to query.
 * @property filters The filters to apply.
 */
class QueryFactory private constructor(
        private val collectionReference: CollectionReference,
        private val filters: List<Filter>
) {
    /**
     * Creates and returns a new direct [Query] that's filtered by the specified filters.
     *
     * @return The created [Query].
     */
    fun create(): Query = filters.fold(collectionReference as Query) { query, filter -> filter.apply(query) }

    /**
     * Creates and returns a new reverse [Query] that's filtered by the specified filters.
     *
     * @return The created [Query].
     */
    fun createReverse(): Query =
            filters.fold(collectionReference as Query) { query, filter -> filter.applyReverse(query) }

    /**
     * Base class for filters.
     *
     * @property fieldPath The field to sort by.
     */
    private abstract class Filter(
            protected val fieldPath: FieldPath
    ) {
        /**
         * Creates and returns a new direct [Query] by applying direct filter.
         *
         * @param query The [Query] to apply filter.
         * @return The created [Query].
         */
        abstract fun apply(query: Query): Query

        /**
         * Creates and returns a new direct [Query] by applying direct filter.
         *
         * @param query The [Query] to apply filter.
         * @return The created [Query].
         */
        abstract fun applyReverse(query: Query): Query

        /**
         * Implementation of [Filter] that sorts [Query] by provided [fieldPath] with specified [direction].
         *
         * @param fieldPath The field to sort by.
         * @property direction The sort direction.
         */
        private class OrderByFilter(
                fieldPath: FieldPath,
                private val direction: Query.Direction
        ) : Filter(fieldPath) {

            private val reverseDirection = if (direction == ASCENDING) DESCENDING else ASCENDING

            override fun apply(query: Query): Query = query.orderBy(fieldPath, direction)
            override fun applyReverse(query: Query): Query = query.orderBy(fieldPath, reverseDirection)
        }

        /**
         * Implementation of [Filter] that filters [Query] by provided [fieldPath] with specified [value].
         *
         * @param fieldPath The field name to compare.
         * @property value The value for comparison.
         * @property filter The filter function.
         */
        private class ByValueFilter(
                fieldPath: FieldPath,
                private val value: Any,
                private val filter: Query.(FieldPath, Any) -> Query
        ) : Filter(fieldPath) {
            override fun apply(query: Query): Query = query.filter(fieldPath, value)
            override fun applyReverse(query: Query): Query = apply(query)
        }

        companion object {
            @JvmStatic
            fun orderBy(fieldPath: FieldPath, direction: Query.Direction): Filter = OrderByFilter(fieldPath, direction)

            @JvmStatic
            fun whereEqualTo(fieldPath: FieldPath, value: Any): Filter =
                    ByValueFilter(fieldPath, value) { p, v -> whereEqualTo(p, v) }

            @JvmStatic
            fun whereGreaterThan(fieldPath: FieldPath, value: Any): Filter =
                    ByValueFilter(fieldPath, value) { p, v -> whereGreaterThan(p, v) }

            @JvmStatic
            fun whereGreaterThanOrEqualTo(fieldPath: FieldPath, value: Any): Filter =
                    ByValueFilter(fieldPath, value) { p, v -> whereGreaterThanOrEqualTo(p, v) }

            @JvmStatic
            fun whereLessThan(fieldPath: FieldPath, value: Any): Filter =
                    ByValueFilter(fieldPath, value) { p, v -> whereLessThan(p, v) }

            @JvmStatic
            fun whereLessThanOrEqualTo(fieldPath: FieldPath, value: Any): Filter =
                    ByValueFilter(fieldPath, value) { p, v -> whereLessThanOrEqualTo(p, v) }

            @JvmStatic
            fun whereArrayContains(fieldPath: FieldPath, value: Any): Filter =
                    ByValueFilter(fieldPath, value) { p, v -> whereArrayContains(p, v) }
        }
    }

    /**
     * Builder for [QueryFactory].
     *
     * [Query.limit], [Query.startAt], [Query.startAfter], [Query.endAt] and [Query.endBefore] applied
     * by [FirestoreItemKeyedDataSource] to paginate data and not available in [Builder].
     *
     * @property collection The collection for query.
     * @property filters The filters to apply.
     */
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    class Builder(private val collection: CollectionReference) {
        private val filters = mutableListOf<Filter>()

        /**
         * Creates and returns [QueryFactory] for provided [collection] and [filters].
         *
         * @return The created [QueryFactory].
         */
        fun build() = QueryFactory(collection, filters)

        /**
         * Adds [Query.orderBy] filter with specified field name and direction.
         *
         * @param field The field name.
         * @param direction The sort direction.
         */
        @JvmOverloads
        fun orderBy(field: String, direction: Query.Direction = ASCENDING) = orderBy(FieldPath.of(field), direction)

        /**
         * Adds [Query.orderBy] filter with specified field name and direction.
         *
         * @param fieldPath The field name.
         * @param direction The sort direction.
         */
        @JvmOverloads
        fun orderBy(fieldPath: FieldPath, direction: Query.Direction = ASCENDING) =
                apply { filters.add(Filter.orderBy(fieldPath, direction)) }

        /**
         * Adds [Query.whereEqualTo] filter with specified field name and value.
         *
         * @param field The field name.
         * @param value The value for comparison.
         */
        fun whereEqualTo(field: String, value: Any) = whereEqualTo(FieldPath.of(field), value)

        /**
         * Adds [Query.whereEqualTo] filter with specified field name and value.
         *
         * @param fieldPath The field name.
         * @param value The value for comparison.
         */
        fun whereEqualTo(fieldPath: FieldPath, value: Any) =
                apply { filters.add(Filter.whereEqualTo(fieldPath, value)) }

        /**
         * Adds [Query.whereGreaterThan] filter with specified field name and value.
         *
         * @param field The field name.
         * @param value The value for comparison.
         */
        fun whereGreaterThan(field: String, value: Any) = whereGreaterThan(FieldPath.of(field), value)

        /**
         * Adds [Query.whereGreaterThan] filter with specified field name and value.
         *
         * @param fieldPath The field name.
         * @param value The value for comparison.
         */
        fun whereGreaterThan(fieldPath: FieldPath, value: Any) =
                apply { filters.add(Filter.whereGreaterThan(fieldPath, value)) }

        /**
         * Adds [Query.whereGreaterThanOrEqualTo] filter with specified field name and value.
         *
         * @param field The field name.
         * @param value The value for comparison.
         */
        fun whereGreaterThanOrEqualTo(field: String, value: Any) = whereGreaterThanOrEqualTo(FieldPath.of(field), value)

        /**
         * Adds [Query.whereGreaterThanOrEqualTo] filter with specified field name and value.
         *
         * @param fieldPath The field name.
         * @param value The value for comparison.
         */
        fun whereGreaterThanOrEqualTo(fieldPath: FieldPath, value: Any) =
                apply { filters.add(Filter.whereGreaterThanOrEqualTo(fieldPath, value)) }

        /**
         * Adds [Query.whereLessThan] filter with specified field name and value.
         *
         * @param field The field name.
         * @param value The value for comparison.
         */
        fun whereLessThan(field: String, value: Any) = whereLessThan(FieldPath.of(field), value)

        /**
         * Adds [Query.whereLessThan] filter with specified field name and value.
         *
         * @param fieldPath The field name.
         * @param value The value for comparison.
         */
        fun whereLessThan(fieldPath: FieldPath, value: Any) =
                apply { filters.add(Filter.whereLessThan(fieldPath, value)) }

        /**
         * Adds [Query.whereLessThanOrEqualTo] filter with specified field name and value.
         *
         * @param field The field name.
         * @param value The value for comparison.
         */
        fun whereLessThanOrEqualTo(field: String, value: Any) = whereLessThanOrEqualTo(FieldPath.of(field), value)

        /**
         * Adds [Query.whereLessThanOrEqualTo] filter with specified field name and value.
         *
         * @param fieldPath The field name.
         * @param value The value for comparison.
         */
        fun whereLessThanOrEqualTo(fieldPath: FieldPath, value: Any) =
                apply { filters.add(Filter.whereLessThanOrEqualTo(fieldPath, value)) }

        /**
         * Adds [Query.whereArrayContains] filter with specified field name and value.
         *
         * @param field The field name.
         * @param value The value for comparison.
         */
        fun whereArrayContains(field: String, value: Any) = whereArrayContains(FieldPath.of(field), value)

        /**
         * Adds [Query.whereArrayContains] filter with specified field name and value.
         *
         * @param fieldPath The field name.
         * @param value The value for comparison.
         */
        fun whereArrayContains(fieldPath: FieldPath, value: Any) =
                apply { filters.add(Filter.whereArrayContains(fieldPath, value)) }
    }
}