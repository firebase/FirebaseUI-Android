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

package com.firebase.ui.database.paging;

public enum LoadingState {
    /**
     * Loading initial data.
     */
    LOADING_INITIAL,

    /**
     * Loading a page other than the first page.
     */
    LOADING_MORE,

    /**
     * Not currently loading any pages, at least one page loaded.
     */
    LOADED,

    /**
     * The last page loaded had zero documents, and therefore no further pages will be loaded.
     */
    FINISHED,

    /**
     * The most recent load encountered an error.
     */
    ERROR
}
