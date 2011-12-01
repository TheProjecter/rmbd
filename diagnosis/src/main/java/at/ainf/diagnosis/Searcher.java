/*
 * Copyright (c) 2009 Kostyantyn Shchekotykhin
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * http://www.gnu.org/licenses/gpl.txt
 */

/*
 * Created on 16.12.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package at.ainf.diagnosis;

import at.ainf.theory.Searchable;
import at.ainf.theory.model.SolverException;
import at.ainf.diagnosis.tree.exceptions.NoConflictException;

import java.util.Collection;
import java.util.Set;


/**
 * @param <F>
 * @author kostya
 *         <p/>
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public interface Searcher<F> {
    /**
     * Calculates a minimal conflict if any.
     *
     * @param searchable the theory that contains formulas of the background theory as well
     *                   as strategies, which must contain all formulas.
     * @param formulas   subset of formulas saved in the theory that might contain minimal conflicts.
     * @return a collection of formulas, which correspond to a minimal conflict.
     * @throws NoConflictException is thrown if formulas are consistent
     * @throws SolverException     is thrown if any solver related problems occur.
     */
    public Set<F> search(Searchable<F> searchable, final Collection<F> formulas)
            throws NoConflictException, SolverException;

    /**
     * Saves statistics of calculations.
     */
    //public void logStatistics();

}
