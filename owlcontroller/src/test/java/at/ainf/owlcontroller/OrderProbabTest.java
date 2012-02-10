package at.ainf.owlcontroller;

import at.ainf.theory.model.InconsistentTheoryException;
import at.ainf.theory.model.SolverException;
import at.ainf.diagnosis.quickxplain.NewQuickXplain;
import at.ainf.theory.storage.AxiomSet;
import at.ainf.theory.storage.SimpleStorage;
import at.ainf.diagnosis.tree.UniformCostSearch;
import at.ainf.diagnosis.tree.exceptions.NoConflictException;
import at.ainf.owlapi3.model.OWLTheory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntax;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: pfleiss
 * Date: 24.06.11
 * Time: 10:28
 * To change this template use File | Settings | File Templates.
 */
public class OrderProbabTest {


    private static Logger logger = Logger.getLogger(SimpleQueryTest.class.getName());


    //private OWLDebugger debugger = new SimpleDebugger();
    private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    @BeforeClass
    public static void setUp() {
        String conf = ClassLoader.getSystemResource("owlcontroller-log4j.properties").getFile();
        PropertyConfigurator.configure(conf);
    }


    @Test
    public void probabTest() throws OWLException, InconsistentTheoryException, SolverException, NoConflictException {
        OWLOntology ontology =
                manager.loadOntologyFromOntologyDocument(ClassLoader.getSystemResourceAsStream("ecai2010.owl"));

        Random r = new Random();

        SimpleStorage<OWLLogicalAxiom> storage = new SimpleStorage<OWLLogicalAxiom>();
        HashMap<ManchesterOWLSyntax, Double> map = Utils.getProbabMap();

        Set<OWLLogicalAxiom> bax = new LinkedHashSet<OWLLogicalAxiom>();

        for (OWLIndividual ind : ontology.getIndividualsInSignature()) {
            bax.addAll(ontology.getClassAssertionAxioms(ind));
            bax.addAll(ontology.getObjectPropertyAssertionAxioms(ind));
        }

        OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
        OWLTheory theory = new OWLTheory(reasonerFactory, ontology, bax);

        for (int i = 0; i < 100 / 4; i++) {
            storage.resetStorage();
            map.put(ManchesterOWLSyntax.SOME, r.nextDouble() / 2);
            map.put(ManchesterOWLSyntax.ONLY, r.nextDouble() / 2);
            map.put(ManchesterOWLSyntax.NOT, r.nextDouble() / 2);
            map.put(ManchesterOWLSyntax.AND, r.nextDouble() / 2);
            map.put(ManchesterOWLSyntax.OR, r.nextDouble() / 2);
            map.put(ManchesterOWLSyntax.EQUIVALENT_TO, r.nextDouble() / 2);
            map.put(ManchesterOWLSyntax.SUBCLASS_OF, r.nextDouble() / 2);

            UniformCostSearch<OWLLogicalAxiom> search = new UniformCostSearch<OWLLogicalAxiom>(storage);
            search.setSearcher(new NewQuickXplain<OWLLogicalAxiom>());
            //search.setNormalize_keywords(false);
            //search.setNormalize_axioms (false);
            //search.setNormalize_diagnoses (true);
            search.setTheory(theory);
            search.setNodeCostsEstimator(new OWLAxiomNodeCostsEstimator(theory));

            ((OWLAxiomNodeCostsEstimator)search.getNodeCostsEstimator()).setKeywordProbabilities(map, null);


            Collection<? extends AxiomSet<OWLLogicalAxiom>> res = new TreeSet<AxiomSet<OWLLogicalAxiom>>(search.run(9));
            TreeSet<AxiomSet<OWLLogicalAxiom>> result = new TreeSet<AxiomSet<OWLLogicalAxiom>>();
            double measure = 0.0;
            for (AxiomSet<OWLLogicalAxiom> hs : res) {
                assertTrue(measure < ((AxiomSet<OWLLogicalAxiom>) hs).getMeasure());
                measure = ((AxiomSet<OWLLogicalAxiom>) hs).getMeasure();
                result.add((AxiomSet<OWLLogicalAxiom>) hs);
            }

            TreeSet<AxiomSet<OWLLogicalAxiom>> copyResult = new TreeSet<AxiomSet<OWLLogicalAxiom>>(result);

            map.put(ManchesterOWLSyntax.SOME, map.get(ManchesterOWLSyntax.SOME) + 0.01);
            map.put(ManchesterOWLSyntax.ONLY, map.get(ManchesterOWLSyntax.ONLY) + 0.01);
            map.put(ManchesterOWLSyntax.NOT, map.get(ManchesterOWLSyntax.NOT) + 0.01);
            map.put(ManchesterOWLSyntax.AND, map.get(ManchesterOWLSyntax.AND) + 0.01);
            map.put(ManchesterOWLSyntax.OR, map.get(ManchesterOWLSyntax.OR) + 0.01);
            map.put(ManchesterOWLSyntax.EQUIVALENT_TO, map.get(ManchesterOWLSyntax.EQUIVALENT_TO) + 0.01);
            map.put(ManchesterOWLSyntax.SUBCLASS_OF, map.get(ManchesterOWLSyntax.SUBCLASS_OF) + 0.01);
            ((OWLAxiomNodeCostsEstimator)search.getNodeCostsEstimator()).setKeywordProbabilities(map, result);
            result = sortDiagnoses(result);
            copyResult = sortDiagnoses(copyResult);

            Iterator<AxiomSet<OWLLogicalAxiom>> iterRes = result.iterator();
            Iterator<AxiomSet<OWLLogicalAxiom>> iterCopy = copyResult.iterator();
            while (iterRes.hasNext()) {
                AxiomSet<OWLLogicalAxiom> hsResult = iterRes.next();
                AxiomSet<OWLLogicalAxiom> hsResultCopy = iterCopy.next();

                assertTrue(hsResult.equals(hsResultCopy));
                double d = Math.abs(hsResult.getMeasure() - hsResultCopy.getMeasure());
                assertTrue(d < 0.00001);

            }

        }

    }

    private TreeSet<AxiomSet<OWLLogicalAxiom>> sortDiagnoses(TreeSet<AxiomSet<OWLLogicalAxiom>> axiomSets) {
        TreeSet<AxiomSet<OWLLogicalAxiom>> phs = new TreeSet<AxiomSet<OWLLogicalAxiom>>();
        for (AxiomSet<OWLLogicalAxiom> hs : axiomSets)
            phs.add(hs);
        return (phs);
    }


}