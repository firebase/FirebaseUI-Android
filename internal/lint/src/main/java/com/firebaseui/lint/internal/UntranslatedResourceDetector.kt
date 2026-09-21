package com.firebaseui.lint.internal

import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.ATTR_TRANSLATABLE
import com.android.SdkConstants.TAG_STRING
import com.android.SdkConstants.VALUE_FALSE
import com.android.ide.common.resources.configuration.FolderConfiguration
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element

/**
 * Flags a string in a locale folder whose value is byte-identical to the base English one.
 *
 * `MissingTranslation` only fires when a string is *absent* from a locale, so a resource that
 * was copied over untranslated is invisible to it: it is present, it just holds English. A
 * whole class of shipped-in-English strings therefore never reaches a lint report; #2509
 * removed 269 such values, found by scanning rather than by any gate.
 *
 * Only `<string>` elements are checked. An English copy of a `<plurals>` or `<string-array>`
 * item is still invisible to this check as well as to `MissingTranslation`.
 *
 * English regional folders (`values-en-rGB` and friends) are skipped, as are strings marked
 * `translatable="false"` and the [ALLOWED] names below. Anything else that is legitimately the
 * same word in another language is suppressed at the site with `tools:ignore`, so the decision
 * sits next to the string.
 */
class UntranslatedResourceDetector : ResourceXmlDetector() {

    /** Base `values/strings.xml` text, keyed by resource name. */
    private val baseStrings = mutableMapOf<String, String>()

    /** Localized strings to judge once every resource file has been read. */
    private val localized = mutableListOf<LocalizedString>()

    private data class LocalizedString(
        val name: String,
        val folder: String,
        val text: String,
        val handle: Location.Handle
    )

    override fun appliesTo(folderType: ResourceFolderType): Boolean =
        folderType == ResourceFolderType.VALUES

    override fun getApplicableElements(): List<String> = listOf(TAG_STRING)

    override fun visitElement(context: XmlContext, element: Element) {
        val name = element.getAttribute(ATTR_NAME)
        if (name.isEmpty()) return
        if (element.getAttribute(ATTR_TRANSLATABLE) == VALUE_FALSE) return

        // Compare rendered text rather than markup: a value that differs from the base only in
        // its xliff placeholders is still untranslated copy.
        val text = element.textContent.trim()
        if (text.isEmpty()) return
        if (!hasTranslatableWords(text)) return

        val folderName = context.file.parentFile?.name ?: return

        if (folderName == BASE_VALUES_FOLDER) {
            // putIfAbsent rather than assignment: if a second source set ever contributes its
            // own values/ folder, first-wins keeps the map from depending on traversal order.
            baseStrings.putIfAbsent(name, text)
            return
        }

        // Only the base folder above seeds the comparison. Any other unqualified folder
        // (values-v26, values-sw360dp, a future values-night) is a configuration variant of the
        // English copy, not a translation, so it is neither a source nor a candidate.
        val locale = FolderConfiguration.getConfigForFolder(folderName)?.localeQualifier ?: return

        if (locale.language == LANGUAGE_ENGLISH) return
        if (name in ALLOWED) return

        localized += LocalizedString(
            name = name,
            folder = folderName,
            text = text,
            handle = context.createLocationHandle(element)
        )
    }

    override fun afterCheckRootProject(context: Context) {
        // Nothing to compare against when lint runs over a single file, which is the IDE's
        // incremental mode. Reporting there would flag every locale string in the file.
        if (baseStrings.isEmpty()) return

        for (string in localized) {
            if (baseStrings[string.name] != string.text) continue
            context.report(
                UNTRANSLATED_RESOURCE,
                string.handle.resolve(),
                "\"${string.name}\" is the base English string in ${string.folder}, so it " +
                    "ships untranslated. Translate it, or mark it `tools:ignore=" +
                    "\"$ISSUE_ID\"` if the translation is genuinely identical."
            )
        }

        baseStrings.clear()
        localized.clear()
    }

    /**
     * Whether [text] contains anything a translator could change.
     *
     * Strings made only of placeholders, punctuation and whitespace have no words to translate,
     * so they are necessarily identical in every locale folder that defines them. No base string
     * is placeholder-only today; the last one, `fui_tos_and_pp_footer`, went with the unreferenced
     * resources, so this guard is currently exercised only by its own test.
     *
     * Escape sequences are stripped first because they are spelled with letters. A non-breaking
     * space is written in these files as a backslash followed by u00A0, and the u and the A in
     * that sequence would otherwise read as translatable content.
     */
    private fun hasTranslatableWords(text: String): Boolean =
        text.replace(UNICODE_ESCAPE, "")
            .replace(FORMAT_SPECIFIER, "")
            .any(Char::isLetter)

    companion object {
        private const val ISSUE_ID = "UntranslatedResource"
        private const val LANGUAGE_ENGLISH = "en"
        private const val BASE_VALUES_FOLDER = "values"

        /** `%s`, `%d`, `%1$s` and friends. */
        private val FORMAT_SPECIFIER = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z%]""")

        /** Matches the escape sequence as written in the file, not the character it denotes. */
        private val UNICODE_ESCAPE = Regex("""\\u[0-9a-fA-F]{4}""")

        /**
         * Names exempted wholesale, because suppressing them per folder would mean roughly 375
         * `tools:ignore` attributes.
         *
         * The four `fui_idp_name_*` entries are brand names, which are not translated. Note this
         * does make a regression invisible: if a locale ever replaced one with a mistranslation,
         * nothing here would report it. `values-fil` and `values-tl` already carry `Fecebook`
         * for `fui_idp_name_facebook`, a pre-existing typo this exemption would hide.
         */
        private val ALLOWED = setOf(
            "fui_idp_name_facebook",
            "fui_idp_name_github",
            "fui_idp_name_google",
            "fui_idp_name_twitter"
        )

        val UNTRANSLATED_RESOURCE = Issue.create(
            ISSUE_ID,
            "Localized string still holds the base English text",
            "A string that is present in a locale folder but identical to the base English " +
                "value ships as English to users of that locale. `MissingTranslation` cannot " +
                "catch this, because it only reports strings that are absent.",
            Category.MESSAGES,
            6,
            Severity.ERROR,
            Implementation(UntranslatedResourceDetector::class.java, Scope.ALL_RESOURCES_SCOPE)
        )
    }
}
