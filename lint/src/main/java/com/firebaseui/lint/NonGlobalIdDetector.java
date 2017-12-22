package com.firebaseui.lint;

import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;

/**
 * Lint detector to find layout attributes that reference "@id" where they should reference
 * "@+id" instead.
 */
public class NonGlobalIdDetector extends ResourceXmlDetector {

    private static final String NON_GLOBAL_ID_PREFIX = "@id";

    private static final String REPORT_MESSAGE = "Use of non-global @id in layout file, " +
            "consider using @+id instead for compatibility with aapt1.";

    public static final Issue NON_GLOBAL_ID = Issue.create(
            "NonGlobalIdInLayout",
            "Usage of non-global @id in layout file.",
            "To maintain compatibility with aapt1, it is safer to always use @+id in layout files.",
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            new Implementation(NonGlobalIdDetector.class, Scope.ALL_RESOURCES_SCOPE)
    );

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        // Only check layout XML files
        return ResourceFolderType.LAYOUT == folderType;
    }

    @Override
    public Collection<String> getApplicableElements() {
        // Returning null here and in getApplicableAttributes causes visitDocument to be called.
        return null;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        // Returning null here and in getApplicableElements causes visitDocument to be called.
        return null;
    }

    @Override
    public void visitDocument(XmlContext context, Document document) {
        // Visit every node in the document
        NodeList nodeList = document.getElementsByTagName("*");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                checkElement(context, element);
            }
        }
    }

    private void checkElement(XmlContext context, Element element) {
        NamedNodeMap namedNodeMap = element.getAttributes();
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            Node node = namedNodeMap.item(i);
            String value = node.getNodeValue();

            // Check if any attribute of the element starts with the @id prefix
            if (value.contains(NON_GLOBAL_ID_PREFIX)) {
                context.report(NON_GLOBAL_ID, element, context.getLocation(node), REPORT_MESSAGE);
            }
        }
    }
}
