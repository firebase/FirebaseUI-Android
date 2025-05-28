/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.data.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import java.text.Collator
import java.util.Locale

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
        var result = locale?.hashCode() ?: 0
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