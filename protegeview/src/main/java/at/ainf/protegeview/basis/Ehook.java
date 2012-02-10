package at.ainf.protegeview.basis;

import at.ainf.owlcontroller.OwlControllerMngr;
import org.apache.log4j.Logger;
import org.protege.editor.core.editorkit.EditorKit;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLEditorKitHook;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: pfleiss
 * Date: 14.11.11
 * Time: 16:02
 * To change this template use File | Settings | File Templates.
 */
public class Ehook extends OWLEditorKitHook implements OWLModelManagerListener {

    private static Logger logger = Logger.getLogger(Ehook.class.getName());

    private OWLEditorKit owlEditorKit;

    public void initialise() throws Exception {

    }

    public void handleChange(OWLModelManagerChangeEvent event) {
         logger.info(event.getType());
        if (event.getType().equals(EventType.ACTIVE_ONTOLOGY_CHANGED)) {
            OWLReasonerFactory factory = owlEditorKit.getOWLModelManager().getOWLReasonerManager().getCurrentReasonerFactory().getReasonerFactory();
            OWLOntology ontology = owlEditorKit.getModelManager().getActiveOntology();
            OwlControllerMngr.getOWLController().updateActiveOntology (ontology,factory);

        }

    }

    public void setup(EditorKit editorKit) {
		this.owlEditorKit = (OWLEditorKit) editorKit;
        owlEditorKit.getModelManager().addListener(this);
	}

    public void dispose() throws Exception {
        owlEditorKit.getModelManager().removeListener(this);
    }

}