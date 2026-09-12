package com.gaycore.app.data

                              
object UserFilter {

    data class Result(val users: List<AdminUser>, val orphanKeys: List<AdminKey>)

    private fun hit(q: String, vararg fields: String?): Boolean =
        fields.any { it != null && it.lowercase().contains(q) }

                                                
    fun matchUser(u: AdminUser, keysOf: (String) -> List<AdminKey>, q: String): Boolean {
        if (q.isEmpty()) return true
        if (hit(q, u.uid, u.name, u.nickname, u.email, u.note)) return true
        return keysOf(u.uid).any { hit(q, it.key, it.name, it.note) }
    }

    fun matchKey(k: AdminKey, q: String): Boolean =
        q.isEmpty() || hit(q, k.key, k.name, k.note, k.uid)

                                                           
    fun filter(users: List<AdminUser>, allKeys: List<AdminKey>, orphanKeys: List<AdminKey>, q: String): Result {
        val qq = q.trim().lowercase()
        if (qq.isEmpty()) return Result(users, orphanKeys)
        val byUid = allKeys.groupBy { it.uid }
        return Result(
            users.filter { matchUser(it, { uid -> byUid[uid] ?: emptyList() }, qq) },
            orphanKeys.filter { matchKey(it, qq) },
        )
    }
}
