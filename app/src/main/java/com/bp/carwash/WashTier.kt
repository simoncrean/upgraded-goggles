package com.bp.carwash

import androidx.annotation.StringRes

/** The four retail wash products. Prices are in cents to avoid float money maths. */
enum class WashTier(
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    val priceCents: Long,
    val featured: Boolean = false,
) {
    QUICK(R.string.tier_quick_name, R.string.tier_quick_desc, 10_00),
    EXPRESS(R.string.tier_express_name, R.string.tier_express_desc, 20_00),
    DELUXE(R.string.tier_deluxe_name, R.string.tier_deluxe_desc, 30_00),
    ULTIMATE(R.string.tier_ultimate_name, R.string.tier_ultimate_desc, 40_00, featured = true);

    val priceLabel: String
        get() = "$" + (priceCents / 100)
}
