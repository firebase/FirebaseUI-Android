/*
 * Copyright (C) 2015 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2017 Google Inc
 *
 */
package com.firebase.ui.auth.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RestrictTo;

import java.text.Collator;
import java.util.Locale;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class CountryInfo implements Comparable<CountryInfo>, Parcelable {

    public static final Parcelable.Creator<CountryInfo> CREATOR = new Parcelable.Creator<CountryInfo>() {
        @Override
        public CountryInfo createFromParcel(Parcel source) {
            return new CountryInfo(source);
        }

        @Override
        public CountryInfo[] newArray(int size) {
            return new CountryInfo[size];
        }
    };

    private final Collator mCollator;
    private final Locale mLocale;
    private final int mCountryCode;

    public CountryInfo(Locale locale, int countryCode) {
        mCollator = Collator.getInstance(Locale.getDefault());
        mCollator.setStrength(Collator.PRIMARY);
        mLocale = locale;
        mCountryCode = countryCode;
    }

    protected CountryInfo(Parcel in) {
        mCollator = Collator.getInstance(Locale.getDefault());
        mCollator.setStrength(Collator.PRIMARY);

        mLocale = (Locale) in.readSerializable();
        mCountryCode = in.readInt();
    }

    public static String localeToEmoji(Locale locale) {
        String countryCode = locale.getCountry();
        // 0x41 is Letter A
        // 0x1F1E6 is Regional Indicator Symbol Letter A
        // Example :
        // firstLetter U => 20 + 0x1F1E6
        // secondLetter S => 18 + 0x1F1E6
        // See: https://en.wikipedia.org/wiki/Regional_Indicator_Symbol
        int firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars
                (secondLetter));
    }

    public Locale getLocale() {
        return mLocale;
    }

    public int getCountryCode() {
        return mCountryCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CountryInfo that = (CountryInfo) o;

        return mCountryCode == that.mCountryCode
                && (mLocale != null ? mLocale.equals(that.mLocale) : that.mLocale == null);
    }

    @Override
    public int hashCode() {
        int result = mLocale != null ? mLocale.hashCode() : 0;
        result = 31 * result + mCountryCode;
        return result;
    }

    @Override
    public String toString() {
        return localeToEmoji(mLocale) + " " + mLocale.getDisplayCountry() + " +" + mCountryCode;
    }

    @Override
    public int compareTo(CountryInfo info) {
        return mCollator.compare(mLocale.getDisplayCountry(), info.mLocale.getDisplayCountry());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mLocale);
        dest.writeInt(mCountryCode);
    }
}
