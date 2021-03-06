package com.rockspin.bargainbits.ui.search.detail

/**
 * Created by valentin.hinov on 24/04/2017.
 */
data class AbbreviatedDealViewModel(
    val storeImageUrl: String,
    val storeName: String,
    val normalPrice: String? = null,
    val dealPrice: String,
    val savingPercentage: Double? = null
) {
    val hasDeal: Boolean
        get() {
            return savingPercentage != null
        }
}