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

package com.firebase.ui.auth.compose.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.data.ALL_COUNTRIES
import com.firebase.ui.auth.compose.data.CountryData
import com.firebase.ui.auth.compose.data.CountryUtils
import kotlinx.coroutines.launch

/**
 * A country selector component that displays the selected country's flag and dial code with a dropdown icon.
 * Designed to be used as a leadingIcon in a TextField.
 *
 * @param selectedCountry The currently selected country.
 * @param onCountrySelected Callback when a country is selected.
 * @param enabled Whether the selector is enabled.
 * @param allowedCountries Optional set of allowed country codes to filter the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelector(
    selectedCountry: CountryData,
    onCountrySelected: (CountryData) -> Unit,
    enabled: Boolean = true,
    allowedCountries: Set<String>? = null,
) {
    val context = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(context)
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val countriesList = remember(allowedCountries) {
        if (allowedCountries != null) {
            CountryUtils.filterByAllowedCountries(allowedCountries)
        } else {
            ALL_COUNTRIES
        }
    }

    val filteredCountries = remember(searchQuery, countriesList) {
        if (searchQuery.isEmpty()) {
            countriesList
        } else {
            CountryUtils.search(searchQuery).filter { country ->
                countriesList.any { it.countryCode == country.countryCode }
            }
        }
    }

    // Clickable row showing flag, dial code and dropdown icon
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(enabled = enabled) {
                showBottomSheet = true
            }
            .padding(start = 8.dp)
            .semantics {
                contentDescription = "Country selector"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = selectedCountry.flagEmoji,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = selectedCountry.dialCode,
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Select country",
            modifier = Modifier.padding(PaddingValues.Zero)
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                searchQuery = ""
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringProvider.countrySelectorModalTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringProvider.searchCountriesHint) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .testTag("CountrySelector LazyColumn")
                ) {
                    items(filteredCountries) { country ->
                        Button(
                            onClick = {
                                onCountrySelected(country)
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    searchQuery = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues.Zero
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = country.flagEmoji,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = country.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    text = country.dialCode,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
