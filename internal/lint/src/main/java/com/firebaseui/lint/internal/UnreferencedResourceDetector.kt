package com.firebaseui.lint.internal

import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.TAG_PLURALS
import com.android.SdkConstants.TAG_STRING
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
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element
import java.util.EnumSet

/**
 * Flags a `fui_*` string or plurals resource that nothing in this module references.
 *
 * Android Lint's own `UnusedResources` does not report these, because for a library module it
 * cannot see the consumers that might use them. That blind spot is how 34 dead strings reached
 * `:auth`, carrying 2,603 translations across 84 locale folders, without the build ever saying
 * so: the module runs under `checkAllWarnings` with `warningsAsErrors` and still reported
 * nothing.
 *
 * The policy this encodes is deliberately narrower than `UnusedResources`, and is only correct
 * because of how this library is meant to be customised. `auth/README.md` documents these names
 * as ones a consuming app *overrides*, by declaring the same name in its own `strings.xml`. It
 * does not invite an app to read them through `R.string.`, and an override of a name the library
 * no longer declares is simply an app-owned string, so deleting one can neither break a consumer
 * build nor change behaviour the library never drove.
 *
 * Note that `:auth` ships no `res/values/public.xml`, so as far as AGP is concerned every one of
 * these resources is public API and this check is stricter than the build itself declares.
 * Declaring the intended surface in a `public.xml` would make the policy real rather than
 * conventional; until then this check is the only thing expressing it.
 *
 * Only names beginning `fui_` are considered. That is not much of a narrowing in practice, since
 * `auth/build.gradle.kts` sets `resourcePrefix("fui_")` and the built-in `ResourceName` check
 * then requires the prefix on everything the module declares. A resource that genuinely must be
 * exempt needs `tools:ignore="UnreferencedResource"` at its declaration, the same way `app_name`
 * carries `tools:ignore="ResourceName"`.
 *
 * ### What counts as a reference
 *
 * `R.string.name` and `R.plurals.name` in Kotlin and Java, and `@string/name` or `@plurals/name`
 * in any XML under `res/` or in `AndroidManifest.xml`, including an alias such as
 * `<string name="a">@string/b</string>`. Attributes in the `tools:` namespace are ignored, since
 * `tools:text="@string/x"` is design-time only and does not keep a resource alive at runtime.
 *
 * ### Known limits
 *
 * **Reports a resource used only by tests.** Test sources are not scanned, so a name referenced
 * from `src/test` or `src/androidTest` but nowhere in `src/main` is reported. That is deliberate:
 * a string no production code path reads is dead copy regardless of what a test does with it, and
 * the test should go with it. The message says "main sources" for that reason.
 *
 * **Cannot see other modules.** Lint runs this against `:auth` alone, so a `fui_*` resource that
 * only `:app` or a future sibling module referenced would be reported. Nothing outside `:auth`
 * declares or references a `fui_*` name today. If that changes, the reference needs
 * `tools:ignore` at the declaration, because a library module's lint genuinely cannot see it.
 *
 * The remaining limits all fail towards *not* reporting, so they cannot break a build over a
 * resource that is really in use. Source and manifest files are matched textually, so a reference
 * written through an aliased import (`import ...R as Res`, then `Res.string.name`) is not seen,
 * and a name appearing only in a comment counts as a reference; neither occurs in this repository
 * today. A resource reached only by [android.content.res.Resources.getIdentifier] is invisible
 * here, as it is to `UnusedResources`; there is no such call in the module. And a dead resource
 * that aliases another dead one keeps the second alive, so a chain like that is reported one link
 * per run rather than all at once.
 */
class UnreferencedResourceDetector : ResourceXmlDetector(), SourceCodeScanner {

    /** Declarations, keyed by resource name. Locale folders translate, they do not declare. */
    private val declarations = mutableMapOf<String, Location.Handle>()

    /** Every `fui_*` name referenced from source, the manifest, or XML anywhere in the module. */
    private val referenced = mutableSetOf<String>()

    // Resources are referenced from every kind of folder, not just values/, so unlike
    // UntranslatedResourceDetector this one reads them all.
    override fun appliesTo(folderType: ResourceFolderType): Boolean = true

    // Declarations only. References are read textually in beforeCheckFile, which covers every
    // attribute and text node of the file in one pass. Visiting every element to read its
    // textContent instead would rescan each element's whole subtree, so the root <resources>
    // node alone would re-read the entire file.
    override fun getApplicableElements(): List<String> = listOf(TAG_STRING, TAG_PLURALS)

    override fun visitElement(context: XmlContext, element: Element) {
        val folder = context.file.parentFile?.name ?: return
        if (!declaresResources(folder)) return

        val name = element.getAttribute(ATTR_NAME)
        if (!name.startsWith(RESOURCE_PREFIX)) return

        // First-wins, so the map does not depend on traversal order when the same name is
        // declared in both values/ and a configuration variant such as values-v26/.
        declarations.putIfAbsent(name, context.createLocationHandle(element))
    }

    override fun beforeCheckFile(context: Context) {
        // Every reference is found here, from one read per file: source, resource XML, and the
        // manifest, which is not under res/ and so is never dispatched to the XML scanner at all.
        // Missing it would make android:label="@string/fui_x" look like a dead resource.
        val name = context.file.name
        val scannable = name.endsWith(".kt") || name.endsWith(".java") || name.endsWith(".xml")
        if (!scannable) return
        collectReferences(context.getContents()?.toString() ?: return)
    }

    override fun afterCheckRootProject(context: Context) {
        // Nothing is collected when lint runs over a single file, which is the IDE's incremental
        // mode, nor when it runs over a source set that declares no resources of its own, which
        // is how the unit-test and androidTest analysis passes behave. Reporting in either case
        // would call every resource dead.
        if (declarations.isNotEmpty()) {
            for ((name, handle) in declarations) {
                if (name in referenced) continue
                context.report(
                    UNREFERENCED_RESOURCE,
                    handle.resolve(),
                    "\"$name\" is not referenced anywhere in this module's main sources, by " +
                        "`R.string.`, `R.plurals.` or `@string/`. Delete it along with its " +
                        "translations in every `values-*` folder, or mark it " +
                        "`tools:ignore=\"$ISSUE_ID\"` if something reaches it in a way this " +
                        "check cannot see."
                )
            }
        }

        declarations.clear()
        referenced.clear()
    }

    /**
     * Whether a `values` folder declares resources rather than translating them.
     *
     * Every locale folder repeats the base names, so treating one as a declaration site would
     * report a translation rather than the resource. A configuration variant such as `values-v26`
     * or `values-night` is not a translation, though, and a resource declared only there is just
     * as capable of being dead, so those do count.
     */
    private fun declaresResources(folder: String): Boolean {
        if (folder == BASE_VALUES_FOLDER) return true
        if (!folder.startsWith("$BASE_VALUES_FOLDER-")) return false
        return FolderConfiguration.getConfigForFolder(folder)?.localeQualifier == null
    }

    private fun collectReferences(rawText: String) {
        if (!rawText.contains(RESOURCE_PREFIX)) return
        // Strip design-time attributes before matching: tools:text="@string/x" is preview only
        // and does not keep a resource alive at runtime.
        val text = rawText.replace(TOOLS_ATTRIBUTE, "")
        for (pattern in REFERENCE_PATTERNS) {
            for (match in pattern.findAll(text)) {
                referenced += match.groupValues[1]
            }
        }
    }

    companion object {
        private const val ISSUE_ID = "UnreferencedResource"
        private const val BASE_VALUES_FOLDER = "values"
        private const val RESOURCE_PREFIX = "fui_"

        /** `R.string.fui_x` and `R.plurals.fui_x`, including a package-qualified `R`. */
        private val CODE_REFERENCE = Regex("""\bR\.(?:string|plurals)\.(fui_[A-Za-z0-9_]+)""")

        /** A whole `tools:` attribute, name and quoted value, as written in the file. */
        private val TOOLS_ATTRIBUTE = Regex("""\btools:[\w.:-]+\s*=\s*"[^"]*"""")

        /** `@string/fui_x` and `@plurals/fui_x`, in any XML attribute or text node. */
        private val XML_REFERENCE = Regex("""@(?:string|plurals)/(fui_[A-Za-z0-9_]+)""")

        private val REFERENCE_PATTERNS = listOf(CODE_REFERENCE, XML_REFERENCE)

        val UNREFERENCED_RESOURCE = Issue.create(
            ISSUE_ID,
            "Library string resource nothing references",
            "A `fui_*` string or plurals resource that this module never reads is dead copy. " +
                "It still ships, and it still reaches translators for every locale folder that " +
                "carries it. `UnusedResources` cannot report it, because for a library module " +
                "lint cannot see the consumers that might use the resource.",
            Category.PERFORMANCE,
            5,
            Severity.ERROR,
            Implementation(
                UnreferencedResourceDetector::class.java,
                EnumSet.of(Scope.ALL_RESOURCE_FILES, Scope.JAVA_FILE, Scope.MANIFEST)
            )
        )
    }
}
