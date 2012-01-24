/*
 * Copyright (c) 2009 Kostyantyn Shchekotykhin
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * http://www.gnu.org/licenses/gpl.txt
 */

package at.ainf.diagnosis.tree;

import at.ainf.theory.model.SolverException;
import at.ainf.theory.model.InconsistentTheoryException;
import at.ainf.theory.storage.AxiomSet;
import at.ainf.theory.storage.Storage;
import at.ainf.diagnosis.tree.exceptions.NoConflictException;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: student99
 * Date: 04.08.2009
 * Time: 11:49:34
 * To change this template use File | Settings | File Templates.
 */
public class DepthLimitedSearch<Id> extends DepthFirstSearch<Id> {

    private int limit;
    private boolean expandable = false;

    public DepthLimitedSearch(Storage<AxiomSet<Id>, Set<Id>, Id> storage) {
        super(storage);
        this.limit = Integer.MAX_VALUE;
    }

    public Set<AxiomSet<Id>> run() throws NoConflictException, SolverException, InconsistentTheoryException {
        this.expandable = false;
        return super.run();
    }

    @Override
    public void expand(Node<Id> node) {
        int level = node.getLevel();
        if (level < this.limit) {
            addNodes(node.expandNode());
        } else if (level == this.limit) {
            this.expandable = true;
        }
    }

    public DepthLimitedSearch(Storage<AxiomSet<Id>, Set<Id>, Id> storage, int limit) {
        super(storage);
        this.limit = limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isExpandable() {
        return expandable;
    }
}
