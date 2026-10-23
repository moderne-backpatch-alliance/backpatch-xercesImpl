/* $Id$ */
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

package util;

import junit.framework.*;

import java.lang.reflect.*;

import org.apache.xerces.util.SymbolHash;
import org.apache.xerces.util.SymbolTable;

/**
 * The hash tables behind the parser's names: SymbolTable (every element and
 * attribute name), SymbolHash (schema components) and DTDGrammar's
 * QNameHashtable (DTD declarations). Each grows with its entries, and a
 * bucket that collects many keys is spread out again.
 */
public class HashCollisionTest extends TestCase {

    /** More keys than any table allows in one bucket. */
    private static final int KEYS = 60;

    public HashCollisionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(HashCollisionTest.class);
    }

    //
    // SymbolTable
    //

    static class SymbolTableProbe extends SymbolTable {
        int tableSize() {
            return fTableSize;
        }
        int longestBucket() {
            int max = 0;
            for (int i = 0; i < fBuckets.length; i++) {
                int n = 0;
                for (Entry e = fBuckets[i]; e != null; e = e.next) {
                    n++;
                }
                max = Math.max(max, n);
            }
            return max;
        }
    }

    public void testSymbolTableGrowsAndKeepsEverySymbol() {
        SymbolTableProbe table = new SymbolTableProbe();
        String[] added = new String[1000];
        for (int i = 0; i < added.length; i++) {
            added[i] = table.addSymbol("name" + i);
        }
        for (int i = 0; i < added.length; i++) {
            char[] ch = ("name" + i).toCharArray();
            assertSame(added[i], table.addSymbol(ch, 0, ch.length));
            assertSame(added[i], ("name" + i).intern());
            if (!table.containsSymbol("name" + i) || !table.containsSymbol(ch, 0, ch.length)) {
                fail("lost name" + i);
            }
        }
        if (table.tableSize() <= 101) {
            fail("1000 symbols in " + table.tableSize() + " buckets");
        }
    }

    public void testSymbolTableSpreadsABucketThatCollectsManySymbols() {
        SymbolTableProbe table = new SymbolTableProbe();
        String[] names = new String[KEYS];
        int target = table.hash("e0") % table.tableSize();
        for (int i = 0, n = 0; n < KEYS; i++) {
            String name = "e" + i;
            if (table.hash(name) % table.tableSize() == target) {
                names[n++] = name;
            }
        }
        for (int i = 0; i < KEYS; i++) {
            char[] ch = names[i].toCharArray();
            table.addSymbol(ch, 0, ch.length);
        }
        for (int i = 0; i < KEYS; i++) {
            if (!table.containsSymbol(names[i])) {
                fail("lost " + names[i]);
            }
        }
        if (table.longestBucket() >= KEYS) {
            fail(KEYS + " symbols in one bucket");
        }
    }

    //
    // SymbolHash
    //

    static class SymbolHashProbe extends SymbolHash {
        int tableSize() {
            return fTableSize;
        }
        int longestBucket() {
            int max = 0;
            for (int i = 0; i < fBuckets.length; i++) {
                int n = 0;
                for (Entry e = fBuckets[i]; e != null; e = e.next) {
                    n++;
                }
                max = Math.max(max, n);
            }
            return max;
        }
    }

    public void testSymbolHashGrowsAndKeepsEveryKey() {
        SymbolHashProbe hash = new SymbolHashProbe();
        for (int i = 0; i < 1000; i++) {
            hash.put("key" + i, new Integer(i));
        }
        assertEquals(1000, hash.getLength());
        for (int i = 0; i < 1000; i++) {
            assertEquals(new Integer(i), hash.get("key" + i));
        }
        if (hash.tableSize() <= 101) {
            fail("1000 keys in " + hash.tableSize() + " buckets");
        }
    }

    public void testSymbolHashSpreadsABucketThatCollectsManyKeys() {
        SymbolHashProbe hash = new SymbolHashProbe();
        String[] keys = new String[KEYS];
        int target = ("k0".hashCode() & 0x7FFFFFFF) % hash.tableSize();
        for (int i = 0, n = 0; n < KEYS; i++) {
            String key = "k" + i;
            if ((key.hashCode() & 0x7FFFFFFF) % hash.tableSize() == target) {
                keys[n++] = key;
            }
        }
        for (int i = 0; i < KEYS; i++) {
            hash.put(keys[i], new Integer(i));
        }
        for (int i = 0; i < KEYS; i++) {
            assertEquals(new Integer(i), hash.get(keys[i]));
        }
        if (hash.longestBucket() >= KEYS) {
            fail(KEYS + " keys in one bucket");
        }
        SymbolHash clone = hash.makeClone();
        for (int i = 0; i < KEYS; i++) {
            assertEquals(new Integer(i), clone.get(keys[i]));
        }
    }

    //
    // DTDGrammar.QNameHashtable: a protected nested class with private
    // fields, so it is reached by reflection.
    //

    static class QNameHashtableProbe {
        private final Object table;
        private final Method put;
        private final Method get;
        private final Method hash;
        private final Field buckets;

        QNameHashtableProbe() throws Exception {
            Class c = Class.forName("org.apache.xerces.impl.dtd.DTDGrammar$QNameHashtable");
            Constructor init = c.getDeclaredConstructor(new Class[0]);
            init.setAccessible(true);
            table = init.newInstance(new Object[0]);
            put = c.getDeclaredMethod("put", new Class[] { String.class, Integer.TYPE });
            get = c.getDeclaredMethod("get", new Class[] { String.class });
            hash = c.getDeclaredMethod("hash", new Class[] { String.class });
            buckets = c.getDeclaredField("fHashTable");
            put.setAccessible(true);
            get.setAccessible(true);
            hash.setAccessible(true);
            buckets.setAccessible(true);
        }
        void put(String key, int value) throws Exception {
            put.invoke(table, new Object[] { key, new Integer(value) });
        }
        int get(String key) throws Exception {
            return ((Integer) get.invoke(table, new Object[] { key })).intValue();
        }
        int hash(String key) throws Exception {
            return ((Integer) hash.invoke(table, new Object[] { key })).intValue();
        }
        int tableSize() throws Exception {
            return ((Object[][]) buckets.get(table)).length;
        }
        int longestBucket() throws Exception {
            Object[][] all = (Object[][]) buckets.get(table);
            int max = 0;
            for (int i = 0; i < all.length; i++) {
                if (all[i] != null) {
                    max = Math.max(max, ((int[]) all[i][0])[0]);
                }
            }
            return max;
        }
    }

    public void testDTDNameTableGrowsAndKeepsEveryName() throws Exception {
        QNameHashtableProbe table = new QNameHashtableProbe();
        for (int i = 0; i < 1000; i++) {
            table.put(("decl" + i).intern(), i);
        }
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, table.get(("decl" + i).intern()));
        }
        if (table.tableSize() <= 101) {
            fail("1000 names in " + table.tableSize() + " buckets");
        }
    }

    public void testDTDNameTableSpreadsABucketThatCollectsManyNames() throws Exception {
        QNameHashtableProbe table = new QNameHashtableProbe();
        String[] names = new String[KEYS];
        int size = table.tableSize();
        int target = table.hash("d0") % size;
        for (int i = 0, n = 0; n < KEYS; i++) {
            String name = ("d" + i).intern();
            if (table.hash(name) % size == target) {
                names[n++] = name;
            }
        }
        for (int i = 0; i < KEYS; i++) {
            table.put(names[i], i);
        }
        for (int i = 0; i < KEYS; i++) {
            assertEquals(i, table.get(names[i]));
        }
        if (table.longestBucket() >= KEYS) {
            fail(KEYS + " names in one bucket");
        }
    }
}
