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
