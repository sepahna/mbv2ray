package com.v2ray.ang.ui.main

import java.util.UUID

/** Main-thread request ownership; a late reply must not complete a newer test. */
internal class MainTestRequests {
    data class Bulk(val id: String, val groupId: String)

    private var current: String? = null
    var bulk: Bulk? = null
        private set
    private val singles = LinkedHashMap<String, String>()
    val isTesting: Boolean get() = current != null || bulk != null || singles.isNotEmpty()

    fun beginCurrent(): String = UUID.randomUUID().toString().also { current = it }

    fun completeCurrent(id: String): Boolean {
        if (id != current) return false
        current = null
        return true
    }

    fun invalidateCurrent() {
        current = null
    }

    fun beginBulk(groupId: String): Bulk = Bulk(UUID.randomUUID().toString(), groupId).also { bulk = it }

    fun hasSingleGuid(guid: String): Boolean = singles.containsValue(guid)

    fun beginSingle(guid: String): String =
        UUID.randomUUID().toString().also { singles[it] = guid }

    fun singleOrNull(id: String): String? = singles[id]

    fun completeSingle(id: String): String? = singles.remove(id)

    fun cancelSingles() {
        singles.clear()
    }

    fun completeBulk(id: String): Bulk? {
        val request = bulk?.takeIf { it.id == id } ?: return null
        bulk = null
        return request
    }

    fun cancelBulk() {
        bulk = null
    }
}
