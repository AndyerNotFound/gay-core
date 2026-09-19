package com.gaycore.app.sdui

import com.gaycore.app.R















internal object SduiIcons {

    
    val map: Map<String, Int> = mapOf(
        
        "home" to R.drawable.ic_home,
        "menu" to R.drawable.ic_menu,
        "arrow_back" to R.drawable.ic_arrow_back,
        "chevron" to R.drawable.ic_chevron_right,
        "chevron_right" to R.drawable.ic_chevron_right,
        "chevron_up" to R.drawable.ic_chevron_up,
        "chevron_down" to R.drawable.ic_chevron_down,
        "more" to R.drawable.ic_more_vert,
        "more_vert" to R.drawable.ic_more_vert,
        "close" to R.drawable.ic_close,
        "add" to R.drawable.ic_add,
        
        "check" to R.drawable.ic_check,
        "refresh" to R.drawable.ic_refresh,
        "search" to R.drawable.ic_search,
        "sort" to R.drawable.ic_sort,
        "filter" to R.drawable.ic_filter,
        "filter_list" to R.drawable.ic_filter,     
        "tune" to R.drawable.ic_tune,
        "grid" to R.drawable.ic_grid,
        "edit" to R.drawable.ic_edit,
        "copy" to R.drawable.ic_copy,
        "delete" to R.drawable.ic_delete,
        "block" to R.drawable.ic_block,
        "send" to R.drawable.ic_send,
        "bolt" to R.drawable.ic_bolt,
        "schedule" to R.drawable.ic_schedule,
        
        "person" to R.drawable.ic_person,
        "group" to R.drawable.ic_group,
        "settings" to R.drawable.ic_settings,
        "apps" to R.drawable.ic_apps,
        "key" to R.drawable.ic_key,
        "wallet" to R.drawable.ic_wallet,
        "star" to R.drawable.ic_star,
        "chat" to R.drawable.ic_chat,
        "info" to R.drawable.ic_info,
        "bug" to R.drawable.ic_bug,
        "palette" to R.drawable.ic_palette,
        "light_mode" to R.drawable.ic_light_mode,
        "dark_mode" to R.drawable.ic_dark_mode,
    )

    
    fun res(name: String?): Int? = name?.removePrefix("msym:")?.let { map[it] }

    
    val names: List<String> get() = map.keys.sorted()
}
