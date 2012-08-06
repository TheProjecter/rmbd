package at.ainf.owlapi3.utils.creation;

import at.ainf.diagnosis.Searcher;
import at.ainf.diagnosis.quickxplain.DirectDiagnosis;
import at.ainf.diagnosis.quickxplain.NewQuickXplain;
import at.ainf.diagnosis.storage.AxiomSet;
import at.ainf.diagnosis.tree.HsTreeSearch;
import at.ainf.diagnosis.tree.InvHsTreeSearch;
import at.ainf.diagnosis.tree.TreeSearch;
import at.ainf.diagnosis.tree.exceptions.NoConflictException;
import at.ainf.diagnosis.tree.searchstrategy.UniformCostSearchStrategy;
import at.ainf.owlapi3.model.DualTreeOWLTheory;
import at.ainf.owlapi3.model.OWLTheory;
import at.ainf.diagnosis.model.InconsistentTheoryException;
import at.ainf.diagnosis.model.SolverException;
import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntax;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: kostya
 * Date: 06.05.11
 * Time: 00:19
 * To change this template use File | Settings | File Templates.
 */
public class CommonUtils extends CreationUtils {

    private static ManchesterOWLSyntax[] keywords = {ManchesterOWLSyntax.SOME,
                ManchesterOWLSyntax.ONLY,
                ManchesterOWLSyntax.MIN,
                ManchesterOWLSyntax.MAX,
                ManchesterOWLSyntax.EXACTLY,
                ManchesterOWLSyntax.AND,
                ManchesterOWLSyntax.OR,
                ManchesterOWLSyntax.NOT,
                ManchesterOWLSyntax.VALUE,
                ManchesterOWLSyntax.INVERSE,
                ManchesterOWLSyntax.SUBCLASS_OF,
                ManchesterOWLSyntax.EQUIVALENT_TO,
                ManchesterOWLSyntax.DISJOINT_CLASSES,
                ManchesterOWLSyntax.DISJOINT_WITH,
                ManchesterOWLSyntax.FUNCTIONAL,
                ManchesterOWLSyntax.INVERSE_OF,
                ManchesterOWLSyntax.SUB_PROPERTY_OF,
                ManchesterOWLSyntax.SAME_AS,
                ManchesterOWLSyntax.DIFFERENT_FROM,
                ManchesterOWLSyntax.RANGE,
                ManchesterOWLSyntax.DOMAIN,
                ManchesterOWLSyntax.TYPE,
                ManchesterOWLSyntax.TRANSITIVE,
                ManchesterOWLSyntax.SYMMETRIC
        };

    public static HashMap<ManchesterOWLSyntax, BigDecimal> getProbabMap() {
        HashMap<ManchesterOWLSyntax, BigDecimal> map = new HashMap<ManchesterOWLSyntax, BigDecimal>();

        for (ManchesterOWLSyntax keyword : keywords) {
            map.put(keyword, BigDecimal.valueOf(0.01));
        }

        map.put(ManchesterOWLSyntax.SOME, BigDecimal.valueOf(0.05));
        map.put(ManchesterOWLSyntax.ONLY, BigDecimal.valueOf(0.05));
        map.put(ManchesterOWLSyntax.AND, BigDecimal.valueOf(0.001));
        map.put(ManchesterOWLSyntax.OR, BigDecimal.valueOf(0.001));
        map.put(ManchesterOWLSyntax.NOT, BigDecimal.valueOf(0.01));

        return map;
    }

    public static <E extends Set<OWLLogicalAxiom>> String logCollection(Logger logger, String name, Set<E> col) {
        StringBuilder buf = new StringBuilder();
        //TreeSet<E> col  = new TreeSet<E>();
        //col.addAll(col1);
        buf.append(name).append(" {");
        for (Iterator<? extends Set<OWLLogicalAxiom>> sub = col.iterator(); sub.hasNext(); ) {
            buf.append(" {");
            buf.append(logCollection(sub.next()));
            if (sub.hasNext())
                buf.append(",");

        }
        buf.append(" }");
        String message = buf.toString();
        logger.info(message);
        return message;
    }

    public static String logCollection(Set<OWLLogicalAxiom> sub) {
        //TreeSet<OWLLogicalAxiom> sub  = new TreeSet<OWLLogicalAxiom>();
        //sub.addAll(sub1);
        StringBuilder buf = new StringBuilder();
        for (Iterator<OWLLogicalAxiom> iter = sub.iterator(); iter.hasNext(); ) {
            OWLLogicalAxiom ax = iter.next();
            OWLClass cls;
            switch (ax.getAxiomType().getIndex()) {
                case 1:
                    OWLClass cl = ((OWLEquivalentClassesAxiom) ax).getNamedClasses().iterator().next();
                    buf.append(cl.asOWLClass().getIRI().getFragment());
                    break;
                case 2:
                    OWLClassExpression cle = ((OWLSubClassOfAxiom) ax).getSubClass();
                    buf.append(cle.asOWLClass().getIRI().getFragment());
                    break;
                case 3:
                    buf.append("D[ ");
                    Set<OWLClass> dja = ax.getClassesInSignature();
                    for (OWLClass ocl : dja)
                        buf.append(ocl.getIRI().getFragment()).append(" ");
                    buf.append("]");
                    break;
                case 5:
                    cls = ax.getClassesInSignature().iterator().next();
                    OWLIndividual ind = ((OWLClassAssertionAxiom) ax).getIndividual();
                    buf.append(cls.getIRI().getFragment()).append("(").append(ind.asOWLNamedIndividual()
                            .getIRI().getFragment()).append(")");
                    break;
                default:
                    buf.append(ax.getAxiomType());
                    for (Iterator<OWLEntity> iterator = ax.getSignature().iterator(); iterator.hasNext(); ) {
                        OWLEntity next = iterator.next();
                        buf.append(" [").append(next.getIRI().getFragment()).append("] ");
                    }
                    //throw new RuntimeException(ax.getAxiomType() + " has unknown index " + ax.getAxiomType().getIndex() + " !");
            }
            if (iter.hasNext())
                buf.append(",");
        }
        buf.append("}");
        return buf.toString();
    }

    public static String getStringTime(long millis) {
        long timeInHours = TimeUnit.MILLISECONDS.toHours(millis);
        long timeInMinutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long timeInSec = TimeUnit.MILLISECONDS.toSeconds(millis);
        long timeInMillisec = TimeUnit.MILLISECONDS.toMillis(millis);

        long hours = timeInHours;
        long minutes = timeInMinutes - TimeUnit.HOURS.toMinutes(timeInHours);
        long seconds = timeInSec - TimeUnit.MINUTES.toSeconds(timeInMinutes);
        long milliseconds = timeInMillisec - TimeUnit.SECONDS.toMillis(timeInSec);
        
        return String.format("%d , (%d h %d m %d s %d ms)", millis, hours, minutes, seconds, milliseconds);
    }
    
    /*public static String renderManyAxioms(Collection<OWLLogicalAxiom> axioms) {
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        String result = "";

        for (OWLLogicalAxiom axiom : axioms) {
            result += renderer.render(axiom) + "\n";
        }
        result = (String) result.subSequence(0,result.length()-2);

        return result; }*/

    public static String renderAxioms(Collection<OWLLogicalAxiom> axioms) {
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        String result = "";

        for (OWLLogicalAxiom axiom : axioms) {
            result += renderer.render(axiom) + ", ";
        }
        result = (String) result.subSequence(0,result.length()-2);

        return result;
    }

}