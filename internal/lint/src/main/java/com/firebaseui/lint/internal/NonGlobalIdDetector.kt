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

package com.firebaseui.lint.internal

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LayoutDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScannerConstants
import org.w3c.dom.Element

/**
 * Lint detector to find layout attributes that reference "@id" where they should reference
 * "@+id" instead.
 */
class NonGlobalIdDetector : LayoutDetector() {
    override fun getApplicableElements(): List<String> = XmlScannerConstants.ALL

    override fun visitElement(context: XmlContext, element: Element) {
        (0 until element.attributes.length)
                .map { element.attributes.item(it) }
                .filter { it.nodeValue.contains(NON_GLOBAL_ID_PREFIX) }
                .forEach {
                    val quickFix = fix()
                            .replace()
                            .name("Fix id")
                            .text(it.nodeValue)
                            .with(it.nodeValue.replace(NON_GLOBAL_ID_PREFIX, GLOBAL_ID_PREFIX))
                            .build()

                    context.report(NON_GLOBAL_ID, it, context.getLocation(it), REPORT_MESSAGE, quickFix)
                }
    }

    companion object {
        private const val NON_GLOBAL_ID_PREFIX = "@id"
        private const val GLOBAL_ID_PREFIX = "@+id"

        private const val REPORT_MESSAGE = "Use of non-global @id in layout file, consider using @+id instead for compatibility with aapt1."

        val NON_GLOBAL_ID = Issue.create(
                "NonGlobalIdInLayout",
                "Usage of non-global @id in layout file.",
                "To maintain compatibility with aapt1, it is safer to always use @+id in layout files.",
                Category.CORRECTNESS,
                8,
                Severity.ERROR,
                Implementation(NonGlobalIdDetector::class.java, Scope.ALL_RESOURCES_SCOPE)
        )
    }
}
