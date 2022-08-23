/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian8.calc.impl;

import mondrian8.calc.Calc;
import mondrian8.calc.VoidCalc;
import mondrian8.olap.Evaluator;
import mondrian8.olap.Exp;

/**
 * Abstract implementation of the {@link mondrian8.calc.VoidCalc} interface.
 *
 * <p>The derived class must
 * implement the {@link #evaluateVoid(mondrian8.olap.Evaluator)} method,
 * and the {@link #evaluate(mondrian8.olap.Evaluator)} method will call it
 * and return <code>null</code>.
 *
 * @author jhyde
 * @since Sep 29, 2005
 */
public class AbstractVoidCalc extends GenericCalc implements VoidCalc {
    private final Calc[] calcs;

    protected AbstractVoidCalc(Exp exp, Calc[] calcs) {
        super(exp);
        this.calcs = calcs;
    }

    public Object evaluate(Evaluator evaluator) {
        evaluateVoid(evaluator);
        return null;
    }

    public Calc[] getCalcs() {
        return calcs;
    }
}

// End AbstractVoidCalc.java
