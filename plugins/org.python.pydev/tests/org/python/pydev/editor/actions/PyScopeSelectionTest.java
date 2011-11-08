/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 */
public class PyScopeSelectionTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyScopeSelectionTest test = new PyScopeSelectionTest();
            test.setUp();
            test.testWithSelection2();
            test.tearDown();
            
            junit.textui.TestRunner.run(PyScopeSelectionTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void check(String string, int initialOffset, int initialLenOffset, int finalOffset, int finalLenOffset){
        PyScopeSelection scopeSelection = new PyScopeSelection();
        Document doc = new Document(string);
        ITextSelection selection = new TextSelection(initialOffset, initialLenOffset);
        
        ITextSelection newSelection = scopeSelection.getNewSelection(doc, selection);
        assertEquals("Expected offset to be: "+finalOffset+" actual offset: "+newSelection.getOffset()+" -- ", finalOffset, newSelection.getOffset());
        assertEquals(finalLenOffset, newSelection.getLength());
    }
    
    public void testSimple() {
        check("a.b", 0, 0, 0, 1);
        check("a.b", 1, 0, 0, 1);
        check("a.b", 2, 0, 2, 1);
        check("a.b", 3, 0, 2, 1);
        
        check("a.b()", 3, 0, 2, 1);
        check("a.b()", 4, 0, 3, 2);
    }
    
    public void testWithSelection() {
        check("aa.b", 0, 1, 0, 2);
        check("a.b", 0, 1, 0, 3);
        check("aaa.b", 0, 4, 0, 5);
        check("aaa.b", 4, 1, 0, 5);
        check("aaa.b()", 4, 1, 0, 7);
        check("aaa.b().o", 4, 1, 0, 9);
        check("a().o", 1, 2, 0, 5);
    }
    
    public void testWithSelection2() {
    }
}
