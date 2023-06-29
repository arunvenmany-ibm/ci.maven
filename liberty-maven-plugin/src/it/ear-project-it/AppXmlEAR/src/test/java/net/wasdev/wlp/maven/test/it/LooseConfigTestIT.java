/*******************************************************************************
 * (c) Copyright IBM Corporation 2017, 2023.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.wasdev.wlp.maven.test.it;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

public class LooseConfigTestIT {
    
    public final String LOOSE_APP = "liberty/wlp/usr/servers/test/apps/AppXmlEAR.ear.xml";
        
    @Test
    public void testLooseApplicationFileExist() throws Exception {
        File f = new File(LOOSE_APP);
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testLooseApplicationFileContent() throws Exception {
        File f = new File(LOOSE_APP);
        FileInputStream input = new FileInputStream(f);
        
        // get input XML Document 
        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
        inputBuilderFactory.setIgnoringComments(true);
        inputBuilderFactory.setCoalescing(true);
        inputBuilderFactory.setIgnoringElementContentWhitespace(true);
        inputBuilderFactory.setValidating(false);
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
        Document inputDoc=inputBuilder.parse(input);
        
        // parse input XML Document
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/archive/file";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        assertEquals("Number of <file/> element ==>", 2, nodes.getLength());
        assertEquals("file targetInArchive attribute value", "/META-INF/application.xml", 
                nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue());
        File appXml = new File("../src/main/application/META-INF/application.xml");     
        assertEquals("file sourceOnDisk attribute value", appXml.getCanonicalPath(), 
                nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue());

        expression = "/archive/archive/archive";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        assertEquals("Number of <archive/> element ==>", 1, nodes.getLength());
        assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/SampleBundle-1.0-SNAPSHOT.jar", 
                nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue());

        expression = "/archive/archive/archive/dir";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        assertEquals("Number of <dir/> element ==>", 1, nodes.getLength());
        
        expression = "/archive/archive/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        assertEquals("Number of <dir/> element ==>", 1, nodes.getLength());

    }
}
