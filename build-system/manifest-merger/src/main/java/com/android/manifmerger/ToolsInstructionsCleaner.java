/*
 * Copyright (C) 2014 The Android Open Source Project
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
 */

package com.android.manifmerger;

import static com.android.manifmerger.MergingReport.Result.ERROR;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.concurrency.Immutable;
import com.android.utils.ILogger;
import com.android.utils.XmlUtils;
import com.google.common.base.Preconditions;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Removes all "tools:" statements from the resulting xml.
 *
 * All attributes belonging to the {@link com.android.SdkConstants#ANDROID_URI} namespace will be
 * removed. If an element contained a "tools:node=\"remove\"" attribute, the element will be
 * deleted.
 */
@Immutable
public class ToolsInstructionsCleaner {

    private static final String REMOVE_OPERATION_XML_MAME =
            NodeOperationType.REMOVE.toCamelCaseName();
    private static final String REMOVE_ALL_OPERATION_XML_MAME =
            NodeOperationType.REMOVE_ALL.toCamelCaseName();

    /**
     * Cleans all attributes belonging to the {@link com.android.SdkConstants#TOOLS_URI} namespace.
     *
     * @param document the xml document to clean
     * @param logger logger to use in case of errors and warnings.
     * @return the cleaned document or null if an error occurred.
     */
    @Nullable
    public static XmlDocument cleanToolsReferences(
            @NonNull XmlDocument document,
            @NonNull ILogger logger) {

        document = Preconditions.checkNotNull(document);
        logger = Preconditions.checkNotNull(logger);
        MergingReport.Result result = cleanToolsReferences(document.getRootNode().getXml(),
                logger);
        return result == MergingReport.Result.SUCCESS
            ? document.reparse()
            : null;
    }

    private static MergingReport.Result cleanToolsReferences(
            Element element,
            ILogger logger) {

        NamedNodeMap namedNodeMap = element.getAttributes();
        if (namedNodeMap != null) {
            for (int i = 0; i < namedNodeMap.getLength(); i++) {
                Node attribute = namedNodeMap.item(i);
                if (SdkConstants.TOOLS_URI.equals(attribute.getNamespaceURI())) {
                    // we need to special case when the element contained tools:node="remove"
                    // since it also needs to be deleted.
                    if (attribute.getLocalName().equals(NodeOperationType.NODE_LOCAL_NAME)
                            && (attribute.getNodeValue().equals(REMOVE_ALL_OPERATION_XML_MAME)
                                || attribute.getNodeValue().equals(REMOVE_OPERATION_XML_MAME))) {

                        if (element.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
                            logger.error(null /* Throwable */,
                                    String.format(
                                        "tools:node=\"%1$s\" not allowed on top level %2$s element",
                                        attribute.getNodeValue(),
                                        XmlNode.unwrapName(element)));
                            return ERROR;
                        } else {
                            element.getParentNode().removeChild(element);
                        }
                    } else {
                        // anything else, we just clean the attribute.
                        element.removeAttributeNS(
                                attribute.getNamespaceURI(), attribute.getLocalName());
                    }
                }
                // this could also be the xmlns:tools declaration.
                if (attribute.getNodeName().startsWith(SdkConstants.XMLNS_PREFIX)
                    && SdkConstants.TOOLS_URI.equals(attribute.getNodeValue())) {
                    element.removeAttribute(attribute.getNodeName());
                }
            }
        }
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (cleanToolsReferences((Element) node, logger) == ERROR) {
                    return ERROR;
                }
            }
        }
        return MergingReport.Result.SUCCESS;
    }

}
