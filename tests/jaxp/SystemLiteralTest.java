/*
 * $Id$
 *
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, Sun Microsystems, Inc., 
 * http://www.sun.com.  For more information on the Apache Software 
 * Foundation, please see <http://www.apache.org/>.
 */

package jaxp;

import junit.framework.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;

import java.io.*;

/**
 * System literals in a document type declaration: XML 1.0 production [11],
 * SystemLiteral ::= ('"' [^"]* '"') | ("'" [^']* "'"), where every character
 * must also match [2] Char.
 */
public class SystemLiteralTest extends TestCase {

    /** A parse that has not ended by then is reported as a failure. */
    private static final long LIMIT_MS = 30000;

    public SystemLiteralTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SystemLiteralTest.class);
    }

    public void testAsciiSystemLiteralIsReported() throws Exception {
        Result r = parse("<?xml version='1.0'?><!DOCTYPE root SYSTEM 'root.dtd'><root/>");
        assertNull("unexpected error: " + r.error, r.error);
        assertEquals("root.dtd", r.systemId);
    }

    public void testSupplementaryCharacterInSystemLiteralIsReported() throws Exception {
        Result r = parse("<?xml version='1.0'?><!DOCTYPE root SYSTEM 'a\uD800\uDC00.dtd'><root/>");
        assertNull("unexpected error: " + r.error, r.error);
        assertEquals("a\uD800\uDC00.dtd", r.systemId);
    }

    public void testInvalidCharacterInSystemLiteralIsAFatalError() throws Exception {
        Result r = parse("<?xml version='1.0'?><!DOCTYPE root SYSTEM 'a\u0001.dtd'><root/>");
        if (!(r.error instanceof SAXParseException)) {
            fail("expected a SAXParseException, got " + r.error);
        }
        if (r.error.getMessage().indexOf("system identifier") == -1) {
            fail(r.error.getMessage());
        }
    }

    static class Result {
        String systemId;
        Throwable error;
    }

    private Result parse(final String document) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        assertEquals("org.apache.xerces.jaxp.SAXParserFactoryImpl", spf.getClass().getName());
        final XMLReader reader = spf.newSAXParser().getXMLReader();
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        final Result result = new Result();
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", new LexicalHandler() {
            public void startDTD(String name, String publicId, String systemId) {
                result.systemId = systemId;
            }
            public void endDTD() {
            }
            public void startEntity(String name) {
            }
            public void endEntity(String name) {
            }
            public void startCDATA() {
            }
            public void endCDATA() {
            }
            public void comment(char[] ch, int start, int length) {
            }
        });
        Thread t = new Thread() {
            public void run() {
                try {
                    reader.parse(new InputSource(new StringReader(document)));
                }
                catch (Throwable e) {
                    result.error = e;
                }
            }
        };
        t.setDaemon(true);
        t.start();
        t.join(LIMIT_MS);
        if (t.isAlive()) {
            fail("the parse did not end within " + LIMIT_MS + " ms");
        }
        return result;
    }
}
