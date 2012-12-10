/*
 * Copyright (c) 2009 Kostyantyn Shchekotykhin
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * http://www.gnu.org/licenses/gpl.txt
 */

package at.ainf.diagnosis.tree;

import   _dev.TimeLog;
import at.ainf.diagnosis.AbstractDebugger;
import at.ainf.diagnosis.Searchable;
import at.ainf.diagnosis.Searcher;
import at.ainf.diagnosis.model.InconsistentTheoryException;
import at.ainf.diagnosis.model.SolverException;
import at.ainf.diagnosis.storage.*;
import at.ainf.diagnosis.tree.exceptions.NoConflictException;
import at.ainf.diagnosis.tree.searchstrategy.SearchStrategy;
import at.ainf.logging.aop.ProfiledVar;
import org.perf4j.aop.Profiled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.math.BigDecimal;
import java.util.*;

import static _dev.TimeLog.stop;

/**
 * Created by IntelliJ IDEA.
 * User: student99
 * Date: 04.08.2009
 * Time: 08:04:41
 * To change this template use File | Settings | File Templates.
 */
public abstract class   AbstractTreeSearch<T extends FormulaSet<Id>, Id> extends AbstractDebugger<T, Id> implements TreeSearch<T, Id> {

    private int maxHittingSets = Integer.MAX_VALUE;

    private static Logger logger = LoggerFactory.getLogger(AbstractTreeSearch.class.getName());

    private static Logger loggerDual = LoggerFactory.getLogger("dualtreelogger");

    private Set<ChangeListener> searchListeners = new LinkedHashSet<ChangeListener>();

    private Node<Id> root = null;

    // ICONFLICTSEARCHER: is the start algorithm for conflicts (e.g. QuickXplain)
    private Searcher<Id> searcher;
    private int prunedHS;

    private SearchStrategy<Id> searchStrategy;

    public SearchStrategy<Id> getSearchStrategy() {
        return searchStrategy;
    }

    public void setSearchStrategy(SearchStrategy<Id> searchStrategy) {
        this.searchStrategy = searchStrategy;
    }




    /*public void setLogic(TreeLogic<T,Id> treeLog) {
        this.treeLogic = treeLog;
        treeLogic.setTreeSearch(this);}  */

    private CostsEstimator<Id> costsEstimator;

    public CostsEstimator<Id> getCostsEstimator() {
        return costsEstimator;
    }

    public void setCostsEstimator(CostsEstimator<Id> costsEstimator) {
        this.costsEstimator = costsEstimator;
    }

    //abstract public SimpleNode<Id> getNode();

    // public abstract Collection<SimpleNode<Id>> getOpenNodes();

    // public abstract SimpleNode<Id> popOpenNodes();

    // public abstract void pushOpenNode(SimpleNode<Id> node);

    //abstract public void addNodes(ArrayList<SimpleNode<Id>> nodeList);

    // public abstract void expand(SimpleNode<Id> node);

    //protected abstract T createConflictSet(SimpleNode<Id> node, Set<Id> quickConflict) throws SolverException;

    //protected abstract T createHittingSet(SimpleNode<Id> labels, boolean valid) throws SolverException;

    protected T createConflictSet(Node<Id> node, FormulaSet<Id> quickConflict) throws SolverException {
        Set<Id> entailments = calculateEntailmentsForConflictSet(quickConflict);
        BigDecimal measure =  getSearchStrategy().getConflictMeasure(quickConflict, getCostsEstimator());
        quickConflict.setEntailments(entailments);
        quickConflict.setMeasure(measure);
        quickConflict.setNode(node);



        return (T)quickConflict;
    }

    protected T createHittingSet(Node<Id> node, boolean valid) throws SolverException {
        Set<Id> labels = node.getPathLabels();
        Set<Id> entailments = calculateEntailmentsForHittingSet(labels, valid);
        BigDecimal measure = getSearchStrategy().getDiagnosisMeasure(node);
        T hs = (T) new FormulaSetImpl<Id>(measure, labels, entailments);
        hs.setNode(node);
        return hs;
    }

    protected Set<Id> calculateEntailmentsForConflictSet(FormulaSet<Id> quickConflict) throws SolverException {
        /*Set<Id> entailments = Collections.emptySet();
        if (getSearchable().supportEntailments() && getSearcher().isDual())
            entailments = getSearchable().getEntailments(quickConflict);
        if (entailments==null)
            entailments = Collections.emptySet();
        return entailments; */
        return Collections.emptySet();
    }

    protected Set<Id> calculateEntailmentsForHittingSet(Set<Id> labels, boolean valid) throws SolverException {
        /*Set<Id> entailments = Collections.emptySet();
        if (getSearchable().supportEntailments() && valid && !getSearcher().isDual())
            entailments = getSearchable().getEntailments(labels);
        return entailments;*/
        Set<Id> entailments = Collections.emptySet();
        if (getSearchable().supportEntailments() && valid)
            entailments = getSearchable().getEntailments(labels);
        return entailments;
    }

    protected List<OpenNodesListener> oNodesLsteners = new LinkedList<OpenNodesListener>();

    // protected abstract double getConflictMeasure(Set<Id> conflict, CostsEstimator<Id> costEst);

    // protected abstract double getDiagnosisMeasure(SimpleNode<Id> node);

    public void addOpenNodesListener(OpenNodesListener l) {
        oNodesLsteners.add(l);
    }

    public void removeOpenNodesListener(OpenNodesListener l) {
        oNodesLsteners.remove(l);
    }

    public List<OpenNodesListener> getOpenNodesListeners() {
        return oNodesLsteners;
    }

    public void setSearcher(Searcher<Id> searcher) {
        this.searcher = searcher;
    }

    public Searcher<Id> getSearcher() {
        return searcher;
    }

    // setter and getter:
    private Searchable<Id> theory = null;

    public void setSearchable(Searchable<Id> theory) {
        this.theory = theory;
    }

    public Searchable<Id> getSearchable() {
        return theory;
    }

    public Set<T> start() throws
            SolverException, NoConflictException, InconsistentTheoryException {
        // reset();
        return searchDiagnoses();
    }

    /*public Set<T> resume() throws
            SolverException, NoConflictException, InconsistentTheoryException {
        if (this.root == null)
            throw new RuntimeException("Nothing to resume!");
        return searchDiagnoses();
    }*/

    public void reset() {
        //setMaxDiagnosesNumber(-1);
        resetStorage();
        getSearchStrategy().getOpenNodes().clear();
        this.root = null;
    }

    public int getNumOfInvalidatedHS() {
        return numOfInvalidatedHS;
    }

    protected int numOfInvalidatedHS;

    @Profiled(tag = "time_calcdiagnoses")
    @ProfiledVar(tag = "calcdiagnoses", isCollection = true)
    protected Set<T> searchDiagnoses() throws SolverException, NoConflictException, InconsistentTheoryException {
        int numberOfDiags  = getMaxDiagnosesNumber();

        TimeLog.start("Overall runPostprocessor");
        TimeLog.start("Diagnosis", "diagnosis");
        try {
            theory.registerTestCases();
            // verify if background theory is consistent
            if (!theory.verifyRequirements())
                throw new SolverException("the background theory doesn't meet requirements");

            if (logger.isInfoEnabled())
                logger.info("runPostprocessor started");

            if (getRoot() != null) {
                // verify hitting sets and remove invalid
                List<T> invalidHittingSets = new LinkedList<T>();
                for (T hs : getDiagnoses()) {
                    if (!theory.testDiagnosis(hs)) {
                        invalidHittingSets.add(hs);
                    }
                }
                numOfInvalidatedHS = invalidHittingSets.size();
                for (T invHS : invalidHittingSets) {
                    invHS.setValid(false);

                }
                updateTree(invalidHittingSets);
            }
            if (getRoot() == null || getRoot().getAxiomSets() == null) {
                this.root = null;
                createRoot();
            }


            if (numberOfDiags == getDiagnoses().size()) {
                return getDiagnoses();
            }

            setMaxDiagnosesNumber(numberOfDiags);
            processOpenNodes();

        } finally {
              //NUR ZU TESTZWECKEN IF SPÄTER LÖSCHEN!!!
            if(theory!=null)
            theory.unregisterTestCases();
            getSearchStrategy().finalizeSearch((TreeSearch<FormulaSet<Id>, Id>) this);
            stop("diagnosis");
            stop();
        }

        return getDiagnoses();
    }

    protected abstract void updateTree(List<T> invalidHittingSets) throws SolverException, InconsistentTheoryException, NoConflictException;

    /*
    protected void expandLeafNodes(SimpleNode<Id> node) {
        if (node.getChildren().isEmpty() && !node.isClosed()) {
            ArrayList<SimpleNode<Id>> nodeList = node.expandNode();
            nodeList.removeAll(getOpenNodes());
            addNodes(nodeList);
            return;
        }
        for (SimpleNode<Id> idNode : node.getChildren()) {
            expandLeafNodes(idNode);
        }
    }*/
    //protected abstract void finalizeSearch(TreeSearch<T, Id> start);

    private void processOpenNodes() throws SolverException, NoConflictException, InconsistentTheoryException {
        if (getRoot() == null)
            throw new IllegalArgumentException("The tree is not initialized!");
        if (openNodesIsEmpty())
            throw new NoConflictException("There are no open nodes!");
        // while List of openNodes is not empty

        while (!openNodesIsEmpty() && (maxHittingSets <= 0 || (getDiagnoses().size() < getMaxDiagnosesNumber()))) {
            Node<Id> node = getSearchStrategy().getNode();
            if (formulaRenderer != null)
                logMessage(getDepth(node), " now processing node with uplink : ", node.getArcLabel());
            processNode(node);
        }
        if (logger.isInfoEnabled())
            logger.info("Finished start with " + getSizeOpenNodes() + " open nodes. Pruned " + this.prunedHS + " diagnoses on the last iteration.");
    }

    private void logMessage(int depth, String message, Set<Id> axioms) {
        String prefix = "";
        if (depth > 0)
            for (int i = 0; i < depth; i++)
                prefix += "    ";

        loggerDual.info(prefix + "o " + message + formulaRenderer.renderAxioms(axioms));
    }

    private void logMessage(int depth, String message, Id axioms) {
        String prefix = "";
        if (depth > 0)
            for (int i = 0; i < depth; i++)
                prefix += "    ";

        loggerDual.info(prefix + message + formulaRenderer.renderAxiom(axioms));
    }

    protected abstract boolean proveValidnessDiagnosis(Set<Id> diagnosis) throws SolverException;

    /*protected boolean proveValidnessDiagnosis(Set<Id> diagnosis) throws SolverException {
        if (!getSearcher().isDual()) {
            if (getSearchable().getKnowledgeBase().hasTests())
                return getSearchable().testDiagnosis(diagnosis);
        }
        return true; } */

    protected void processNode(Node<Id> node) throws SolverException, InconsistentTheoryException {
        boolean prune = pruneHittingSet(node);

        // if(axiomRenderer!=null) loggerDual.info("arc: " + axiomRenderer.renderAxiom(node.getArcLabel()));
        if (!prune) {
            try {
                if (!canReuseConflict(node) )
                    //EDITED von CalculateConflict
                    calculateNode(node);
                if (!node.isClosed() && isValidConflict(node.getAxiomSets())) {
                    getSearchStrategy().expand(node);
                   //If child isn't empty, add to open Nodes
                   /* for(SimpleNode<Id> child:node.getChildren()){
                        if(child.getAxiomSets()!=null)
                            getSearchStrategy().pushOpenNode(child);
                    } */
                }
            } catch (NoConflictException e) {
                // if(!getSearcher().isDual()) {
                node.setClosed();
                stop("diagnosis");
                if (logger.isInfoEnabled())
                    logger.info("Closing node. " + getSizeOpenNodes() + " more to process.");

                Set<Id> diagnosis = node.getPathLabels();

                boolean valid = proveValidnessDiagnosis(diagnosis);
                
                T hs = createHittingSet(node, valid);

                hs.setValid(valid);
                addHittingSet(hs);
                notifySearchListeners();
                TimeLog.start("Diagnosis", "diagnosis");
                if (logger.isInfoEnabled()) {
                    logger.info("Found conflicts: " + getConflicts().size() + " and diagnoses " + getDiagnoses().size());
                    logger.info("Pruned " + this.prunedHS + " diagnoses");
                    this.prunedHS = 0;
                }
                /*}
                else {
                    E conflictSet = createConflictSet(node.getPathLabels());
                    getStorage().addNodeLabel(conflictSet);
                    // verify if there is a conflict that is a subset of the new conflict
                    Set<E> invalidConflicts = new LinkedHashSet<E>();
                    for (E cs : getStorage().getConflicts()) {
                        if (cs.containsAll(conflictSet) && cs.size() > conflictSet.size())
                            invalidConflicts.add(cs);
                    }

                    if (!invalidConflicts.isEmpty()) {
                        for (E invalidConflict : invalidConflicts) {
                            loggerDual.info("now conflict invalid: " + invalidConflict);
                            getStorage().removeNodeLabel(invalidConflict);
                        }
                        updateTree(conflictSet);
                    }

                }*/
            }
        } else
            this.prunedHS++;
    }

    public void createRoot() throws NoConflictException,
            SolverException, InconsistentTheoryException {
        // if there is already a root
        if (getRoot() != null) return;
        Set<FormulaSet<Id>> conflict = calculateConflict(null);
        //NEW
        Node<Id> node = getSearchStrategy().createRootNode(conflict.iterator().next(), getCostsEstimator(), getSearchable().getKnowledgeBase().getFaultyFormulas());
        setRoot(node);
    }

    protected int getDepth(Node<Id> node) {
        if (node == null) return -1;
        if (node.getParent() == null)
            return 0;
        else
            return getDepth(node.getParent()) + 1;
    }

    //protected abstract SimpleNode<Id> createRootNode(Set<Id> conflict,CostsEstimator<Id> costsEstimator, Collection<Id> act);

    /*
    protected void proveValidnessConflict(T conflictSet) throws SolverException {
        if (getSearcher().isDual()) {
            boolean valid = true;
            if (getSearchable().hasTests()) {
//                getSearchable().addCheckedBackgroundFormulas(pathLabels);
                valid = getSearchable().testDiagnosis(conflictSet);
                //              getSearchable().removeBackgroundFormulas(pathLabels);
            }
            conflictSet.setValid(valid);

        }
    }
     */
    public Set<FormulaSet<Id>> calculateConflict(Node<Id> node) throws
            SolverException, NoConflictException, InconsistentTheoryException {
        // if conflict was already calculated
        //edited
       /* if(getRoot()!=null)
            System.out.println("Number of Nodes:"+((HSTreeNode)getRoot()).countNodes());
             */
        //System.out.println("Berechne neuen Konflikt");

        Set<FormulaSet<Id>> quickConflict;
        List<Id> list = new ArrayList<Id>(getSearchable().getKnowledgeBase().getFaultyFormulas());
        Collections.sort(list, new Comparator<Id>() {
            public int compare(Id o1, Id o2) {
                BigDecimal nodeCosts = getCostsEstimator().getFormulaCosts(o1);
                int value = -1 * nodeCosts.compareTo(getCostsEstimator().getFormulaCosts(o2));
                if (value == 0)
                    return ((Comparable)o1).compareTo(o2);
                return value;
            }
        });

        Set<Id> pathLabels = null;
        if (node != null) {
            if (logger.isDebugEnabled())
                logger.debug("Calculating a conflict for the node: " + node);
            if (node.getAxiomSets() != null) {
                if (logger.isDebugEnabled())
                    logger.debug("The conflict is already calculated: " + node.getAxiomSets());
                //EDITED
                return node.getAxiomSets();
            }
            pathLabels = node.getPathLabels();
        }

        quickConflict = getSearcher().search(getSearchable(), list, pathLabels);

        /*if(node instanceof BHSTreeNode){
            for(FormulaSet<Id> set: quickConflict){
                   ((BHSTreeNode) node).updateConflict(set);
            }
        } */

        //if(!searcher.isDual()) {
        if (logger.isInfoEnabled())
            logger.info("Found conflict: " + quickConflict);

        T conflictSet = createConflictSet(node, quickConflict.iterator().next());

        proveValidnessConflict(conflictSet);

        if (formulaRenderer != null)
            logMessage(getDepth(node), "created conflict set: ", conflictSet);
        if (formulaRenderer != null)
            logMessage(getDepth(node), "pathlabels: ", pathLabels);


        pruneConflictSets(node, conflictSet);

        addNodeLabel(conflictSet);
        notifySearchListeners();

        // current node should get a conflict only if a path from
        // this node to root does not include closed nodes
        if (node != null && !hasClosedParent(node.getParent())){

            node.setAxiomSet(new LinkedHashSet<FormulaSet<Id>>(quickConflict));

        }

        if(this instanceof BinaryTreeSearch && node!=null){

         for(FormulaSet<Id> conflict: quickConflict) {
            for(HSTreeNode<Id> leave: (Set<HSTreeNode>)((HSTreeNode)getRoot()).getLeaves()) {
                if(!leave.isClosed() && !intersectsWith(conflict,leave.getPathLabels())) {
                    ((BHSTreeNode<Id>)leave).addNewConflict(conflict);
                    //SEHR UNSCHÖN später ausbessern
                    if(leave.getConflicts()!=null)
                    leave.getConflicts().add(((BHSTreeNode)leave).updateConflict(conflict));
                    else leave.setAxiomSet((FormulaSet<Id>)((BHSTreeNode)leave).updateConflict(conflict));
                }
            }
        }

        }
        return quickConflict;
        /*}
        else {
                Set<Id> diagnosis = quickConflict;
                boolean valid = true;
                if (getSearchable().hasTests())
                    valid = getSearchable().testDiagnosis(diagnosis);
                T hs = createHittingSet(diagnosis, valid);
                hs.setValid(valid);
                getStorage().addHittingSet(hs);
                if (node != null && !hasClosedParent(node.getParent()))
                node.setAxiomSet(quickConflict);

            return hs;

        }*/
    }

    protected abstract Set<FormulaSet<Id>> calculateNode(Node<Id> node) throws NoConflictException,SolverException, InconsistentTheoryException;

    /*public Set<T> getConflicts() {
        return getStorage().getConflicts();
    }

    public Set<T> getDiagnoses() {
        return getStorage().getDiagnoses();
    } */

    protected Set<T> getValidAxiomSets(Set<T> set) {
        Set<T> valid = new LinkedHashSet<T>();

        for (T s : set) {
            if (s.isValid())
                valid.add(s);
        }
        return Collections.unmodifiableSet(valid);

    }

    protected Set<T> copy(Set<T> set) {
        Set<T> hs = new LinkedHashSet<T>();
        for (T hset : set)
            hs.add(hset);
        return hs;
    }

    protected Set<Set<T>> copy2(Set<Set<T>> set) {
        Set<Set<T>> hs = new LinkedHashSet<Set<T>>();
        for (Set<T> hset : set)
            hs.add(copy(hset));
        return hs;
    }

    protected abstract void pruneConflictSets(Node<Id> node, T conflictSet) throws SolverException, InconsistentTheoryException;

    protected abstract void proveValidnessConflict(T conflictSet) throws SolverException;

    /*
    protected void pruneConflictSets(SimpleNode<Id> node, T conflictSet) throws SolverException, InconsistentTheoryException {
        if (!getSearcher().isDual()) {
        // DAG: verify if there is a conflict that is a subset of the new conflict
        Set<T> invalidConflicts = new LinkedHashSet<T>();
        for (T e : getStorage().getConflicts()) {
            if (e.containsAll(conflictSet) && e.size() > conflictSet.size())
                invalidConflicts.add(e);
        }

        if (!invalidConflicts.isEmpty()) {
            for (T invalidConflict : invalidConflicts) {
                if (axiomRenderer != null) logMessage(getDepth(node), "now conflict invalid: ", invalidConflict);
                getStorage().removeNodeLabel(invalidConflict);
            }
            updateTree(conflictSet);
        }
        }
    }

*/
    protected boolean hasClosedParent(Node<Id> node) {
        if (node.isRoot())
            return node.isClosed();
        return node.getParent()==null || node.isClosed() || hasClosedParent(node.getParent());
    }

  /*

 */

    public boolean canReuseConflict(Node<Id> node) {
        // check if this is a root
        //EDITED MultiNodes don't reuse Conflicts
        if (node.isRoot() || node.getAxiomSets() != null || (node instanceof BHSTreeNode)) return false;
        Collection<Id> pathLabels = node.getPathLabels();
        for (FormulaSet<Id> localConflict : getConflicts()) {
            if (localConflict.isValid() && !intersectsWith(pathLabels, localConflict)) {
                node.setAxiomSet(localConflict);
                if (logger.isDebugEnabled())
                    logger.debug("Reusing conflict: " + localConflict);
                if (formulaRenderer != null) logMessage(getDepth(node), "reusing conflict ", localConflict);
                return true;
            }
        }
        return false;
    }

    private boolean intersectsWith(Collection<Id> pathLabels, Collection<Id> localConflict) {
        for (Id label : pathLabels) {
            //if (localConflict.contains(label))
            //    return true;
            for (Id axiom : localConflict) {
                if (axiom.equals(label))
                    return true;
            }
        }
        return false;
    }

    public boolean pruneHittingSet(Node<Id> node) {
        if (node.isRoot()) return false;
        Collection<Id> pathLabels = node.getPathLabels();
        for (T diagnosis : getHittingSets()) {
            if (pathLabels.containsAll(diagnosis)) {
                return true;
            }
        }
        return false;
    }

    public void setMaxDiagnosesNumber(int maxDiagnoses) {
        this.maxHittingSets = maxDiagnoses;
    }

    public int getMaxDiagnosesNumber() {
        return this.maxHittingSets;
    }

    public void setRoot(Node<Id> rootNode) {
        root = rootNode;
        clearOpenNodes();
        getSearchStrategy().pushOpenNode(root);
    }

    public Node<Id> getRoot() {
        return root;
    }

    public boolean openNodesIsEmpty() {
        return getSearchStrategy().getOpenNodes().isEmpty();
    }


    public void addLastOpenNodes(Node<Id> node) {
        getSearchStrategy().getOpenNodes().add(node);
    }

    public int getSizeOpenNodes() {
        return getSearchStrategy().getOpenNodes().size();
    }

    public void clearOpenNodes() {
        getSearchStrategy().getOpenNodes().clear();
    }

    public void setFormulaRenderer(FormulaRenderer<Id> idFormulaRenderer) {
        this.formulaRenderer = idFormulaRenderer;

    }

    //

    protected Set<T> nodeLabels = new LinkedHashSet<T>();
    protected Set<T> hittingSets = new LinkedHashSet<T>();

    private StorageListener<T, Id> hittingSetListener = new StorageListener<T, Id>() {
        public boolean remove(T oldObject) {
            boolean remValid = oldObject.isValid();
            if (!hittingSets.remove(oldObject)) {
                // perhaps treeset order is not correct
                hittingSets = copy(hittingSets);
                if (hittingSets.remove(oldObject))
                    logger.error("treeset ordering is not correct - updates of probabilities? ");
                else
                    throw new IllegalStateException("Existing hitting set was not removed!");
            }

            return remValid;
        }

        public void add(T newObject, boolean addValid) {
            hittingSets.add(newObject);
        }
    };

       //Leere Attribute
    public void resetStorage() {
        for (T hs : this.getHittingSets())
            hs.setListener(null);
        hittingSets.clear();
        nodeLabels.clear();
    }


    public boolean addNodeLabel(T nodeLabel) {
        return nodeLabels.add(nodeLabel);
    }

    public boolean removeNodeLabel(T nodeLabel) {
        return this.nodeLabels.remove(nodeLabel);
    }

    public Set<T> getNodeLabels() {
        return Collections.unmodifiableSet(nodeLabels);
    }




    public boolean addHittingSet(final T hittingSet) {
        hittingSet.setListener(this.hittingSetListener);

        Set<T> del = new HashSet<T>();

        //Prüfe ob ein HS Superset des neuen HS ist
        for (T set : hittingSets) {
            if (set.containsAll(hittingSet))
                del.add(set);
        }

        //Wenn ja, entferne es
        if (!del.isEmpty())
            for (T ids : del) {
                removeHittingSet(ids);
            }



      //Füge neues HS hinzu
        return hittingSets.add(hittingSet);
    }

    public void addSearchListener(ChangeListener listener) {
        searchListeners.add(listener);
    }

    public void removeSearchListener(ChangeListener listener) {
        searchListeners.remove(listener);
    }

    private void notifySearchListeners() {
        for (ChangeListener searchListener : searchListeners)
            searchListener.stateChanged(new ChangeEvent(this));
    }

    public boolean removeHittingSet(final T diagnosis) {
        return hittingSets.remove(diagnosis);
    }

    public Set<T> getHittingSets() {
        return Collections.unmodifiableSet(hittingSets);
    }

    private boolean isValidConflict(Set<FormulaSet<Id>> set){

        if(set==null || set.isEmpty()) return false;

        for(Set<Id>c:set){
            if(c==null||c.isEmpty()) return false;
        }

        return true;
    }

}
