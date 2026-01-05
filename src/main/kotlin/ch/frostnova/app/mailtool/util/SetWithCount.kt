package ch.frostnova.app.mailtool.util

typealias SetWithCount<T> = HashMap<T, Int>

fun <T : Any> SetWithCount<T>.add(item: T) {
    put(item, (get(item) ?: 0) + 1)
}

fun <T : Any> SetWithCount<T>.topItems(): List<Pair<Int, T>> =
    entries.sortedBy { it.key.toString() }.sortedByDescending { it.value }.map { it.value to it.key }
