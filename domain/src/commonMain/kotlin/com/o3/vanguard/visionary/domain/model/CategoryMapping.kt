package com.o3.vanguard.visionary.domain.model

/** Categories shown in this collection. `ALL` returns every category. */
fun CollectionKind.categories(): Set<MediaCategory> = when (this) {
    CollectionKind.ALL -> MediaCategory.entries.toSet()
    CollectionKind.SCREENSHOTS -> setOf(MediaCategory.SCREENSHOT)
    CollectionKind.DOCUMENTS -> setOf(MediaCategory.DOCUMENT)
    CollectionKind.RECEIPTS -> setOf(MediaCategory.RECEIPT)
    CollectionKind.SCANS -> setOf(MediaCategory.SCAN)
}

/** Categories this filter allows. `null` means any category. */
fun SearchFilter.categories(): Set<MediaCategory>? = when (this) {
    SearchFilter.ALL, SearchFilter.HAS_PHONE_OR_URL -> null
    SearchFilter.SCREENSHOTS -> setOf(MediaCategory.SCREENSHOT)
    SearchFilter.RECEIPTS -> setOf(MediaCategory.RECEIPT)
    SearchFilter.DOCUMENTS -> setOf(MediaCategory.DOCUMENT)
    SearchFilter.SCANS -> setOf(MediaCategory.SCAN)
}

/** Entity bits an item must have at least one of. `0` means no entity requirement. */
fun SearchFilter.requiredEntityFlags(): Int = when (this) {
    SearchFilter.HAS_PHONE_OR_URL -> SmartEntityType.PHONE.flagBit or SmartEntityType.URL.flagBit
    else -> 0
}
