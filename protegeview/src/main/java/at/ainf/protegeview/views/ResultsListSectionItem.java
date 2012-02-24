package at.ainf.protegeview.views;

import at.ainf.diagnosis.tree.NodeCostsEstimator;
import org.protege.editor.core.ui.list.MListItem;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;

public class ResultsListSectionItem implements MListItem {

    private OWLObject object;

    protected OWLLogicalAxiom axiom;

    private NodeCostsEstimator<OWLLogicalAxiom> estimator;

    public ResultsListSectionItem(OWLObject object, OWLLogicalAxiom axiom, NodeCostsEstimator<OWLLogicalAxiom> es) {
        this.object = object;
        this.estimator = es;
        this.axiom  = axiom;
    }


    public OWLObject getOWLObject() {
        return object;
    }
    
    public OWLLogicalAxiom getAxiom() {
		return axiom;
	}


    public String toString() {
        return object.toString();
    }


    public boolean isEditable() {
        return false;
    }


    public void handleEdit() {
    }


    public boolean isDeleteable() {
        return false;
    }


    public boolean handleDelete() {
        return false;
    }


    public String getTooltip() {
        if (estimator != null) {
            return "axiom error probability: " + estimator.getNodeCosts(axiom);
        } else {
            return null;
        }
    }
}
