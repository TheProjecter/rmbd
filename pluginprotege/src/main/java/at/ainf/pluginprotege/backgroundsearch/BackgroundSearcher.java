package at.ainf.pluginprotege.backgroundsearch;

import at.ainf.theory.storage.HittingSet;
import at.ainf.diagnosis.tree.TreeSearch;
import at.ainf.pluginprotege.debugmanager.DebugManager;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Created by IntelliJ IDEA.
 * User: pfleiss
 * Date: 28.07.11
 * Time: 10:15
 * To change this template use File | Settings | File Templates.
 */
public class BackgroundSearcher {

    private TreeSearch<? extends HittingSet<OWLLogicalAxiom>, Set<OWLLogicalAxiom>, OWLLogicalAxiom> trSearch;

    private Frame parent;

    public BackgroundSearcher(TreeSearch<? extends HittingSet<OWLLogicalAxiom>, Set<OWLLogicalAxiom>, OWLLogicalAxiom> search, Frame parent) {
        this.trSearch = search;
        this.parent = parent;
    }

    public BackgroundSearcherTask.Result doBackgroundSearch() {

        final BackgroundSearchDialog dialog = new BackgroundSearchDialog(parent);

        BackgroundSearcherTask task = new BackgroundSearcherTask(trSearch,
                dialog.getTextArea(),dialog.getProgressBar());
        trSearch.getStorage().addStorageItemListener(task);
        //BackgroundSearchManager.getInstance().addElementAddedToStorageListener(task);
        dialog.setWorker(task);
        task.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                            dialog.setVisible(false);
                            dialog.dispose();
                        }
                    }
                });
        task.execute();
        dialog.setVisible(true);

        try {
            BackgroundSearcherTask.Result res = task.get();
            trSearch.getStorage().removeStorageItemListener(task);
            //BackgroundSearchManager.getInstance().removeElementAddedToStorageListener(task);
            DebugManager.getInstance().setValidHittingSets(trSearch.getStorage().getValidHittingSets());
            DebugManager.getInstance().notifyHittingSetsChanged();
            DebugManager.getInstance().setConflictSets(trSearch.getStorage().getConflictSets());
            DebugManager.getInstance().notifyConflictSetsChanged();
            return res;
        } catch (InterruptedException e) {

            return null;
        } catch(CancellationException e) {
            DebugManager.getInstance().setValidHittingSets(trSearch.getStorage().getValidHittingSets());
            DebugManager.getInstance().notifyHittingSetsChanged();
            DebugManager.getInstance().setConflictSets(trSearch.getStorage().getConflictSets());
            DebugManager.getInstance().notifyConflictSetsChanged();
            return  BackgroundSearcherTask.Result.CANCELED;
        } catch (ExecutionException e) {
            return null;
        }

    }

}
