package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.Tuple3;

import junit.framework.TestCase;

public class PyImportsHandlingTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyImportsHandlingTest test = new PyImportsHandlingTest();
            test.setUp();
          test.testPyImportHandling2();
            test.tearDown();
            junit.textui.TestRunner.run(PyImportsHandlingTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testPyImportHandling() throws Exception {
        Document doc = new Document("from xxx import yyy");
        PyImportsHandling importsHandling = new PyImportsHandling(doc);
         Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();
        assertEquals("from xxx import yyy", next.importFound);
        assertEquals(0, next.startFoundLine);
        assertEquals(0, next.endFoundLine);
        assertFalse(it.hasNext());
    }        
        
    public void testPyImportHandling2() throws Exception {
        
        Document doc = new Document("from xxx import yyy\nfrom y import (a, \nb,\nc)");
        PyImportsHandling importsHandling = new PyImportsHandling(doc);
        Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();
        assertEquals("from xxx import yyy", next.importFound);
        
        assertEquals(0, next.startFoundLine);
        assertEquals(0, next.endFoundLine);
        assertTrue(it.hasNext());
        next = it.next();
        
        assertEquals("from y import (a, \nb,\nc)", next.importFound);
        assertEquals(1, next.startFoundLine);
        assertEquals(3, next.endFoundLine);
        
    }

}