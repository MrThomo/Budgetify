package com.budgetify.app.core

import androidx.annotation.StringRes
import com.budgetify.app.R

/**
 * The API seeds default categories in English. This maps those names to translated string
 * resources so the category labels follow the app language (English / isiZulu / Sesotho).
 * Custom categories created by the user keep the name they typed.
 */
object CategoryNames {
    @StringRes
    fun resFor(name: String): Int? = when (name.trim().lowercase()) {
        "groceries" -> R.string.cat_groceries
        "transport" -> R.string.cat_transport
        "eating out" -> R.string.cat_eating_out
        "utilities" -> R.string.cat_utilities
        "entertainment" -> R.string.cat_entertainment
        "other" -> R.string.cat_other
        else -> null
    }
}
