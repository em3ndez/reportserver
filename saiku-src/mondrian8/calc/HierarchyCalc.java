/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian8.calc;

import mondrian8.olap.Evaluator;
import mondrian8.olap.Hierarchy;

/**
 * Expression which yields a {@link mondrian8.olap.Hierarchy}.
 *
 * <p>When implementing this interface, it is convenient to extend
 * {@link mondrian8.calc.impl.AbstractHierarchyCalc}, but it is not required.
 *
 * @author jhyde
 * @since Sep 26, 2005
 */
public interface HierarchyCalc extends Calc {
    /**
     * Evaluates this expression to yield a hierarchy.
     *
     * <p>Never returns null.
     *
     * @param evaluator Evaluation context
     * @return a hierarchy
     */
    Hierarchy evaluateHierarchy(Evaluator evaluator);
}

// End HierarchyCalc.java
