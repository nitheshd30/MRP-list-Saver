package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.ProductDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class CategoryRepository(
    private val context: Context,
    private val productDao: ProductDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mrp_categories_prefs", Context.MODE_PRIVATE)

    // Known example categories to remove
    private val exampleCategories = setOf(
        "Grocery",
        "Dairy",
        "Beverages",
        "Snacks",
        "Bakery",
        "Personal Care",
        "Household",
        "General"
    )

    private val _customCategories = MutableStateFlow<Set<String>>(loadCategories())
    val customCategories = _customCategories.asStateFlow()

    private fun loadCategories(): Set<String> {
        val saved = prefs.getStringSet(KEY_CUSTOM_CATEGORIES, null)
        val result = linkedSetOf<String>()
        if (saved != null) {
            result.addAll(saved.filter { it.trim().isNotBlank() && it !in exampleCategories })
            // Clean up persisted preferences so old example categories are purged from disk
            prefs.edit().putStringSet(KEY_CUSTOM_CATEGORIES, result).apply()
        }
        return result
    }

    private fun persistCategories(categories: Set<String>) {
        val filtered = categories.filter { it.trim().isNotBlank() && it !in exampleCategories }.toSet()
        prefs.edit().putStringSet(KEY_CUSTOM_CATEGORIES, filtered).apply()
        _customCategories.value = filtered
    }

    val allCategories: Flow<List<String>> = combine(
        productDao.getCategories(),
        _customCategories
    ) { dbCategories, customCats ->
        val combined = linkedSetOf<String>()
        combined.addAll(customCats.filter { it !in exampleCategories })
        combined.addAll(dbCategories.filter { it.isNotBlank() && it !in exampleCategories })
        combined.toList().sorted()
    }

    fun addCategory(categoryName: String): Boolean {
        val clean = categoryName.trim()
        if (clean.isBlank()) return false
        val current = _customCategories.value.toMutableSet()
        current.add(clean)
        persistCategories(current)
        return true
    }

    fun removeCategory(categoryName: String): Boolean {
        val current = _customCategories.value.toMutableSet()
        val removed = current.remove(categoryName)
        if (removed) {
            persistCategories(current)
        }
        return removed
    }

    companion object {
        private const val KEY_CUSTOM_CATEGORIES = "user_added_categories"
    }
}
