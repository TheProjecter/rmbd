package at.ainf.protegeview.backgroundsearch;

import at.ainf.diagnosis.tree.OpenNodesListener;
import at.ainf.theory.model.InconsistentTheoryException;
import at.ainf.theory.model.SolverException;
import at.ainf.theory.storage.AxiomSet;
import at.ainf.theory.storage.StorageItemAddedEvent;
import at.ainf.theory.storage.StorageItemListener;
import at.ainf.diagnosis.tree.TreeSearch;
import at.ainf.diagnosis.tree.exceptions.NoConflictException;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: pfleiss
 * Date: 26.07.11
 * Time: 09:14
 * To change this template use File | Settings | File Templates.
 */
public class BackgroundSearcherTask extends SwingWorker<BackgroundSearcherTask.Result, BackgroundSearcherTask.BackgroundTaskDataExchange>
        implements StorageItemListener, OpenNodesListener {

    private JTextArea area;

    private JProgressBar progressBar;

    /* public void updateNumOpenNodes(int n) {
        int numOfHittingSets = search.getStorage().getValidHittingSets().size();
        int numOfConflictSets = search.getStorage().getConflictSets().size();
        int maxDiags = search.getMaxHittingSets();
        int numOfonodes=search.getOpenNodes().size();
        if (numOfonodes > maxOpeNodes) maxOpeNodes = numOfonodes;

        publish(new BackgroundTaskDataExchange(numOfHittingSets, numOfConflictSets,
                maxDiags,numOfonodes,maxOpeNodes));


    }*/

    int addedOnodes = 0;
    int removedOnodes = 0;

    public void updateOpenNodesAdded() {
        int numOfHittingSets = search.getStorage().getDiagnoses().size();
        int numOfConflictSets = search.getStorage().getConflicts().size();
        int maxDiags = search.getMaxHittingSets();
        addedOnodes++;

        publish(new BackgroundTaskDataExchange(numOfHittingSets, numOfConflictSets,
                maxDiags,removedOnodes,addedOnodes));
    }

    public void updateOpenNodesRemoved() {
        int numOfHittingSets = search.getStorage().getDiagnoses().size();
        int numOfConflictSets = search.getStorage().getConflicts().size();
        int maxDiags = search.getMaxHittingSets();
        removedOnodes++;

        publish(new BackgroundTaskDataExchange(numOfHittingSets, numOfConflictSets,
                maxDiags,removedOnodes,addedOnodes));
    }

    public enum Result {
        FINISHED,
        CANCELED,
        SOLVER_EXCEPTION,
        NO_CONFLICT_EXCEPTION,
        UNSAT_TESTS_EXCEPTION
    }

    public class BackgroundTaskDataExchange {

        public int hittingSetNum;

        public int conflictSetNum;

        public int m;

        private int removedOnodes;

        private int onode;

        public BackgroundTaskDataExchange(int hittingSets, int conflictSets, int maxDg, int removed, int added) {
            this.m = maxDg;
            this.hittingSetNum = hittingSets;
            this.conflictSetNum = conflictSets;
            this.removedOnodes = removed;
            this.onode =  added;


        }

    }

    TreeSearch<? extends AxiomSet<OWLLogicalAxiom>, OWLLogicalAxiom> search;

    BackgroundSearcherTask(TreeSearch<? extends AxiomSet<OWLLogicalAxiom>, OWLLogicalAxiom> search, JTextArea area, JProgressBar progressBar) {
        this.area = area;
        this.search = search;
        this.progressBar = progressBar;
    }

    int maxOpeNodes = 1;

    public void elementAdded(StorageItemAddedEvent e) {
        int numOfHittingSets = search.getStorage().getDiagnoses().size();
        int numOfConflictSets = search.getStorage().getConflicts().size();
        int maxDiags = search.getMaxHittingSets();

        publish(new BackgroundTaskDataExchange(numOfHittingSets, numOfConflictSets,
                maxDiags,removedOnodes,addedOnodes));
    }


    @Override
    public Result doInBackground() {                      // what to do if maxhittingset is 0
        try {
            search.run(search.getMaxHittingSets());
        } catch (SolverException e) {
            return Result.SOLVER_EXCEPTION;
        } catch (NoConflictException e) {
            return Result.NO_CONFLICT_EXCEPTION;
        } catch (InconsistentTheoryException e) {
            return Result.UNSAT_TESTS_EXCEPTION;
        }

        return Result.FINISHED;
    }

    @Override
    protected void process(List<BackgroundTaskDataExchange> chunks) {
        for (BackgroundTaskDataExchange backgroundDataExchange : chunks) {
            area.setText(" hitting sets: " + backgroundDataExchange.hittingSetNum + "\n conflict sets: "
                    + backgroundDataExchange.conflictSetNum + "\n"
                    + "open nodes: " + (backgroundDataExchange.onode-backgroundDataExchange.removedOnodes) + "\n");
            if (backgroundDataExchange.m > 0) {
                if (progressBar.isIndeterminate()) {

                    progressBar.setIndeterminate(false);
                    progressBar.setStringPainted(true);
                }
                progressBar.setValue(100 * backgroundDataExchange.hittingSetNum / backgroundDataExchange.m);
                progressBar.setString(backgroundDataExchange.hittingSetNum + "/" + backgroundDataExchange.m);
            }
            else {
                if (progressBar.isIndeterminate()) {

                    progressBar.setIndeterminate(false);
                    progressBar.setStringPainted(true);
                }
                progressBar.setValue(100 * backgroundDataExchange.removedOnodes / backgroundDataExchange.onode);
                progressBar.setString(backgroundDataExchange.removedOnodes + "/" + backgroundDataExchange.onode);

            }
        }
    }

}