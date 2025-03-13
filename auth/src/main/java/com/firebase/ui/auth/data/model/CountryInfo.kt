package com.firebase.ui.auth.data.model

import android.os.Parcel
import android.os.Parcelable
import java.text.Collator
import java.util.Locale
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CountryInfo(val locale: Locale?, val countryCode: Int) : Comparable<CountryInfo>, Parcelable {

    // Use a collator initialized to the default locale.
    private val collator: Collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CountryInfo> = object : Parcelable.Creator<CountryInfo> {
            override fun createFromParcel(source: Parcel): CountryInfo = CountryInfo(source)
            override fun newArray(size: Int): Array<CountryInfo?> = arrayOfNulls(size)
        }

        fun localeToEmoji(locale: Locale?): String {
            if (locale == null) return ""
            val countryCode = locale.country
            // 0x41 is Letter A, 0x1F1E6 is Regional Indicator Symbol Letter A.
            // For example, for "US": 'U' => (0x55 - 0x41) + 0x1F1E6, 'S' => (0x53 - 0x41) + 0x1F1E6.
            val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
        }
    }

    // Secondary constructor to recreate from a Parcel.
    constructor(parcel: Parcel) : this(
        parcel.readSerializable() as? Locale,
        parcel.readInt()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryInfo) return false
        return countryCode == other.countryCode && locale == other.locale
    }

    override fun hashCode(): Int {
        if (locale == null) return 1
        var result = locale.hashCode()
        result = 31 * result + countryCode
        return result
    }

    override fun toString(): String {
        return "${localeToEmoji(locale)} ${locale?.displayCountry ?: ""} +$countryCode"
    }

    fun toShortString(): String {
        return "${localeToEmoji(locale)} +$countryCode"
    }

    override fun compareTo(other: CountryInfo): Int {
        val defaultLocale = Locale.getDefault()
        return collator.compare(
            locale?.displayCountry?.uppercase(defaultLocale) ?: "",
            other.locale?.displayCountry?.uppercase(defaultLocale) ?: ""
        )
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(locale)
        dest.writeInt(countryCode)
    }
}