package at.ainf.owlapi3.module.iterative.diag;

import at.ainf.diagnosis.Speed4JMeasurement;
import at.ainf.owlapi3.module.iterative.ModuleDiagSearcher;
import com.google.common.collect.Sets;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: pfleiss
 * Date: 06.03.13
 * Time: 09:54
 * To change this template use File | Settings | File Templates.
 */
public class IterativeModuleDiagnosis extends AbstractModuleDiagnosis {

    public static final int MAX_UNSAT_CLASSES = 50;
    private static Logger logger = LoggerFactory.getLogger(IterativeModuleDiagnosis.class.getName());

    private final boolean sortNodes;

    public IterativeModuleDiagnosis(Set<OWLLogicalAxiom> mappings, Set<OWLLogicalAxiom> ontoAxioms,
                                    OWLReasonerFactory factory, ModuleDiagSearcher moduleDiagSearcher,
                                    boolean sortNodes) {

        super(mappings, ontoAxioms, factory, moduleDiagSearcher);

        this.sortNodes = sortNodes;

    }

    protected boolean isSortNodes() {
        return sortNodes;
    }

    public Set<OWLLogicalAxiom> calculateTargetDiagnosis() {
        Set<OWLLogicalAxiom> targetDiagnosis = new HashSet<OWLLogicalAxiom>();
        Speed4JMeasurement.start("calculatetargetdiag");
        List<OWLClass> unsatClasses = getModuleCalculator().getInitialUnsatClasses(Collections.<OWLClass>emptySet(),
                MAX_UNSAT_CLASSES);
        if (isSortNodes())
            Collections.sort(unsatClasses, new ChildsComparator(unsatClasses, getMappings(), getOntoAxioms()));
        int toIndex = Collections.min(Arrays.asList(MAX_UNSAT_CLASSES, unsatClasses.size()));
        List<OWLClass> actualUnsatClasses = new LinkedList<OWLClass>(unsatClasses.subList(0, toIndex));

        while (!actualUnsatClasses.isEmpty()) {
            getModuleCalculator().calculateModules(actualUnsatClasses);


            Speed4JMeasurement.start("calculatemodule");
            Map<OWLClass, Set<OWLLogicalAxiom>> map = getModuleCalculator().getModuleMap();
            OWLClass actualUnsatClass;
            //if (isSortNodes())
            actualUnsatClass = Collections.min(actualUnsatClasses, new ModuleSizeComparator(map));
            //else
            //    actualUnsatClass = actualUnsatClasses.get(0);

            //Set<OWLLogicalAxiom> axioms = new LinkedHashSet<OWLLogicalAxiom>(map.get(actualUnsatClass));

            //Iterator<OWLClass> it = actualUnsatClasses.iterator();
            //OWLClass owlClass = it.next();
            Set<OWLLogicalAxiom> axioms = new HashSet<OWLLogicalAxiom>(map.get(actualUnsatClass));
            actualUnsatClasses.remove(actualUnsatClass);
            //Set<OWLLogicalAxiom> intersection = new HashSet<OWLLogicalAxiom>(map.get(owlClass));
            for (OWLClass unsatClass : actualUnsatClasses) {
                Set<OWLLogicalAxiom> module = map.get(unsatClass);
                if (module.size() < axioms.size())
                    axioms = new HashSet<OWLLogicalAxiom>(module);
                else {
                    Sets.SetView<OWLLogicalAxiom> intersection = Sets.intersection(axioms, module);
                    if (!intersection.isEmpty() && intersection.size() < axioms.size() &&
                            !getModuleCalculator().isConsistent(intersection))
                        axioms = new HashSet<OWLLogicalAxiom>(intersection);
                }
            }

            /*
            for (OWLClass unsatClass : actualUnsatClasses)
                axioms.addAll(map.get(unsatClass));
            */
            Set<OWLLogicalAxiom> background = new LinkedHashSet<OWLLogicalAxiom>(axioms);
            background.retainAll(getOntoAxioms());

            if (logger.isInfoEnabled())
                logger.info("Processing module with BK: " + background.size() + " from " + axioms.size());

            //Set<? extends Set<OWLLogicalAxiom>> diagnoses = searchDiagnoses(axioms, background);
            //Set<OWLLogicalAxiom> partDiag = diagnosisOracle.chooseDiagnosis(diagnoses);
            Speed4JMeasurement.start("calculatepartdiag");
            IterativeStatistics.avgCoherencyTime.createNewValueGroup();
            IterativeStatistics.avgConsistencyTime.createNewValueGroup();
            IterativeStatistics.avgConsistencyCheck.createNewValueGroup();
            IterativeStatistics.avgCoherencyCheck.createNewValueGroup();
            Set<OWLLogicalAxiom> partDiag = getDiagSearcher().calculateDiag(axioms, background);
            Speed4JMeasurement.stop();

            for (OWLLogicalAxiom axiom : partDiag)
                logger.info("part diag axiom: " + axiom);
            logger.info("---");

            long timeModule = Speed4JMeasurement.stop();
            IterativeStatistics.moduleTime.add(timeModule);
            getModuleCalculator().removeAxiomsFromOntologyAndModules(partDiag);
            getModuleCalculator().updatedLists(actualUnsatClasses, unsatClasses, MAX_UNSAT_CLASSES);
            targetDiagnosis.addAll(partDiag);
        }
        IterativeStatistics.logAndClear(logger, IterativeStatistics.avgCardCS, "average cardinality CS");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.cardHS, "cardinality HS");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.numberCS, "number CS");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.moduleSize, "module size");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.diagnosisTime, "diagnosis time");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.moduleTime, "module time");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.avgConsistencyTime, "consistency time");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.avgCoherencyTime, "coherency time");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.avgConsistencyCheck, "consistency checks");
        IterativeStatistics.logAndClear(logger, IterativeStatistics.avgCoherencyCheck, "coherency checks");

        Speed4JMeasurement.stop();
        return targetDiagnosis;
    }

    protected class ChildsComparator implements Comparator<OWLClass> {
        private Collection<OWLLogicalAxiom> mappings;
        private Collection<OWLLogicalAxiom> ontoAxioms;
        private final Map<OWLClass, Integer> map;

        public ChildsComparator(Collection<OWLClass> unsatClasses, Collection<OWLLogicalAxiom> mappings, Collection<OWLLogicalAxiom> ontoAxioms) {
            this.mappings = mappings;
            this.ontoAxioms = ontoAxioms;

            this.map = calculateNumberTransitiveUnsatChilds(unsatClasses);
        }

        protected Map<OWLClass, Integer> calculateNumberTransitiveUnsatChilds(Collection<OWLClass> unsatClasses) {
            Map<OWLClass, Integer> result = new LinkedHashMap<OWLClass, Integer>();
            Set<OWLLogicalAxiom> allAxioms = new HashSet<OWLLogicalAxiom>();
            allAxioms.addAll(mappings);
            allAxioms.addAll(ontoAxioms);
            StructuralReasoner reasoner = new StructuralReasoner(createOntology(allAxioms),
                    new SimpleConfiguration(), BufferingMode.NON_BUFFERING);
            for (OWLClass unsatClass : unsatClasses) {
                Set<OWLClass> childs = new HashSet<OWLClass>(reasoner.getSubClasses(unsatClass, false).getFlattened());
                childs.remove(BOT_CLASS);
                result.put(unsatClass, childs.size());
            }
            return result;
        }

        @Override
        public int compare(OWLClass o1, OWLClass o2) {
            return map.get(o1).compareTo(map.get(o2));
        }

    }

}
