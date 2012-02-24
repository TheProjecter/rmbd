package at.ainf.owlcontroller.queryeval;

import at.ainf.diagnosis.partitioning.CKK;
import at.ainf.diagnosis.partitioning.EntropyScoringFunction;
import at.ainf.diagnosis.partitioning.Partitioning;
import at.ainf.diagnosis.partitioning.postprocessor.QSS;
import at.ainf.diagnosis.partitioning.postprocessor.QSSFactory;
import at.ainf.diagnosis.tree.UniformCostSearch;
import at.ainf.diagnosis.tree.exceptions.NoConflictException;
import at.ainf.owlapi3.model.OWLTheory;
import at.ainf.owlcontroller.Utils;
import at.ainf.owlcontroller.queryeval.result.TableList;
import at.ainf.owlcontroller.queryeval.result.Time;
import at.ainf.theory.model.ITheory;
import at.ainf.theory.model.InconsistentTheoryException;
import at.ainf.theory.model.SolverException;
import at.ainf.theory.storage.AxiomSet;
import at.ainf.theory.storage.Partition;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.util.*;

import static junit.framework.Assert.assertTrue;

public abstract class BasePerformanceTests {

    public static final int NUMBER_OF_HITTING_SETS = 9;
    protected static final double SIGMA = 85;
    protected static boolean userBrk = true;

    private static Logger logger = Logger.getLogger(BasePerformanceTests.class.getName());

    private int diagnosesCalc = 0;
    private int conflictsCalc = 0;
    private String daStr = "";

    enum QSSType {MINSCORE, SPLITINHALF, STATICRISK, DYNAMICRISK, PENALTY, NO_QSS}

    ;

    protected Random rnd = new Random();

    protected double avg(List<Double> nqueries) {
        double res = 0;
        for (Double qs : nqueries) {
            res += qs;
        }
        return res / nqueries.size();
    }

    protected Partitioning<OWLLogicalAxiom> createQueryGenerator(ITheory<OWLLogicalAxiom> theory, QSS<OWLLogicalAxiom> qss) {
        Partitioning<OWLLogicalAxiom> p = new CKK<OWLLogicalAxiom>(theory, new EntropyScoringFunction<OWLLogicalAxiom>());
        if (qss != null) p.setPostprocessor(qss);
        return p;
    }

    private <E extends OWLObject> void printc
            (Collection<? extends Collection<E>> c) {
        for (Collection<E> hs : c) {
            System.out.println("Test case:");
            print(hs);
        }
    }


    private <E extends OWLObject> void print
            (Collection<E> c) {
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        for (E el : c) {
            System.out.println(renderer.render(el));
        }
    }

    /* private void print(TreeSet<AxiomSet<OWLLogicalAxiom>> diagnoses) {
        for (AxiomSet<OWLLogicalAxiom> hs : diagnoses.descendingSet()) {
            System.out.println(hs.toString());
        }
    }*/

    private <E extends OWLObject> void prinths
            (Collection<AxiomSet<E>> c) {
        for (AxiomSet<E> hs : c) {
            logger.info(hs);
            print(hs);
        }
    }

    private boolean generateQueryAnswer
            (UniformCostSearch<OWLLogicalAxiom> search, Partition<OWLLogicalAxiom> actualQuery, AxiomSet<OWLLogicalAxiom> targetDiag) {
        boolean answer;
        ITheory<OWLLogicalAxiom> theory = search.getTheory();

        if (theory.diagnosisEntails(targetDiag, actualQuery.partition)) {
            answer = true;
            assertTrue(!actualQuery.dnx.contains(targetDiag));
        } else if (!theory.diagnosisConsistent(targetDiag, actualQuery.partition)) {
            answer = false;
            assertTrue(!actualQuery.dx.contains(targetDiag));
        } else {
            answer = rnd.nextBoolean();
        }

        return answer;

    }

    protected void simulateBruteForceOnl
            (UniformCostSearch<OWLLogicalAxiom> search, OWLTheory
                    theory, AxiomSet<OWLLogicalAxiom> targetDiag, TableList
                    entry, QSSType scoringFunc) {
        //DiagProvider diagProvider = new DiagProvider(search, false, 9);

        QSS<OWLLogicalAxiom> qss = createQSSWithDefaultParam(scoringFunc);

        Partition<OWLLogicalAxiom> actPa = null;

        Set<AxiomSet<OWLLogicalAxiom>> diagnoses = null;
        int num_of_queries = 0;

        boolean userBreak = false;
        boolean systemBreak = false;

        boolean querySessionEnd = false;
        long time = System.currentTimeMillis();
        Time queryTime = new Time();
        Time diagTime = new Time();
        int queryCardinality = 0;
        long reactionTime = 0;
        Partitioning<OWLLogicalAxiom> queryGenerator = createQueryGenerator(theory, qss);
        while (!querySessionEnd) {
            try {
                Collection<AxiomSet<OWLLogicalAxiom>> lastD = diagnoses;
                logger.trace("numOfQueries: " + num_of_queries + " search for diagnoses");

                userBreak = false;
                systemBreak = false;

                if (actPa != null && actPa.dx.size() == 1 && actPa.dz.size() == 1 && actPa.dnx.isEmpty()) {
                    logger.error("Help!");
                    printc(theory.getEntailedTests());
                    printc(theory.getNonentailedTests());
                    print(actPa.partition);
                    prinths(actPa.dx);
                    prinths(actPa.dz);
                }

                try {
                    long diag = System.currentTimeMillis();
                    search.run(NUMBER_OF_HITTING_SETS);

                    daStr += search.getStorage().getDiagnoses().size() + "/";
                    diagnosesCalc += search.getStorage().getDiagnoses().size();
                    conflictsCalc += search.getStorage().getConflicts().size();

                    diagnoses = search.getStorage().getDiagnoses();
                    diagTime.setTime(System.currentTimeMillis() - diag);
                } catch (SolverException e) {
                    diagnoses = new TreeSet<AxiomSet<OWLLogicalAxiom>>();

                } catch (NoConflictException e) {
                    diagnoses = new TreeSet<AxiomSet<OWLLogicalAxiom>>(search.getStorage().getDiagnoses());

                }

                if (diagnoses.isEmpty())
                    logger.error("No diagnoses found!");

                // cast should be corrected
                Iterator<AxiomSet<OWLLogicalAxiom>> descendSet = (new TreeSet<AxiomSet<OWLLogicalAxiom>>(diagnoses)).descendingIterator();
                AxiomSet<OWLLogicalAxiom> d = descendSet.next();
                AxiomSet<OWLLogicalAxiom> d1 = (descendSet.hasNext()) ? descendSet.next() : null;

                boolean isTargetDiagFirst = d.equals(targetDiag);
                double dp = d.getMeasure();
                if (logger.isInfoEnabled()) {
                    AxiomSet<OWLLogicalAxiom> o = containsItem(diagnoses, targetDiag);
                    double diagProbabilities = 0;
                    for (AxiomSet<OWLLogicalAxiom> tempd : diagnoses)
                        diagProbabilities += tempd.getMeasure();
                    logger.trace("diagnoses: " + diagnoses.size() +
                            " (" + diagProbabilities + ") first diagnosis: " + d +
                            " is target: " + isTargetDiagFirst + " is in window: " +
                            ((o == null) ? false : o.toString()));
                }

                if (d1 != null && scoringFunc != QSSType.SPLITINHALF) {
                    double d1p = d1.getMeasure();
                    double diff = 100 - (d1p * 100) / dp;
                    logger.trace("difference : " + (dp - d1p) + " - " + diff + " %");
                    if (userBrk && diff > SIGMA && isTargetDiagFirst && num_of_queries > 0) {
                        // user brake
                        querySessionEnd = true;
                        userBreak = true;
                        break;
                    }
                }

                if (diagnoses.equals(lastD) || diagnoses.size() < 2) {
                    // system brake
                    querySessionEnd = true;
                    systemBreak = true;
                    break;
                }
                Partition<OWLLogicalAxiom> last = actPa;

                logger.trace("numOfQueries: " + num_of_queries + " search for  query");

                long query = System.currentTimeMillis();
                //actPa = getBestQuery(search, diagnoses);

                actPa = queryGenerator.generatePartition(diagnoses);

                long querytime = System.currentTimeMillis() - query;
                queryTime.setTime(querytime);
                if (num_of_queries == 0)
                    reactionTime = querytime;

                if (actPa == null || actPa.partition == null || (last != null && actPa.partition.equals(last.partition))) {
                    // system brake
                    querySessionEnd = true;
                    break;
                }
                queryCardinality = actPa.partition.size();

                logger.trace("numOfQueries: " + num_of_queries + " generate answer");
                boolean answer = generateQueryAnswer(search, actPa, targetDiag);

                if (qss != null) qss.updateParameters(answer);

                num_of_queries++;
                // fine all dz diagnoses
                // TODO do we need this fine?
                for (AxiomSet<OWLLogicalAxiom> ph : actPa.dz) {
                    ph.setMeasure(0.5d * ph.getMeasure());
                }
                if (answer) {
                    try {
                        search.getTheory().addEntailedTest(new TreeSet<OWLLogicalAxiom>(actPa.partition));
                        if (actPa.dnx.isEmpty() && diagnoses.size() < NUMBER_OF_HITTING_SETS)
                            querySessionEnd = true;
                    } catch (InconsistentTheoryException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                } else {
                    try {
                        search.getTheory().addNonEntailedTest(new TreeSet<OWLLogicalAxiom>(actPa.partition));
                        if (actPa.dx.isEmpty() && diagnoses.size() < NUMBER_OF_HITTING_SETS)
                            querySessionEnd = true;
                    } catch (InconsistentTheoryException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            } catch (SolverException e) {
                querySessionEnd = true;
                logger.error(e);

            } catch (InconsistentTheoryException e) {
                querySessionEnd = true;
                logger.error(e);

            }
        }
        time = System.currentTimeMillis() - time;
        boolean targetDiagnosisIsInWind = false;
        boolean targetDiagnosisIsMostProbable = false;
        if (diagnoses != null) {
            //TreeSet<ProbabilisticHittingSet> diags = new TreeSet<ProbabilisticHittingSet>(diagnoses);
            for (AxiomSet<OWLLogicalAxiom> ps : diagnoses)
                if (ps.equals(targetDiag))
                    targetDiagnosisIsInWind = true;
            if (diagnoses.size() >= 1 && (new TreeSet<AxiomSet<OWLLogicalAxiom>>(diagnoses)).last().equals(targetDiag)) {
                targetDiagnosisIsMostProbable = true;
                targetDiagnosisIsInWind = true;
            }
        }
        int diagWinSize = 0;
        if (diagnoses != null)
            diagWinSize = diagnoses.size();

        int consistencyCount = theory.getConsistencyCount();
        logger.info("Iteration finished within " + time + " ms, required " + num_of_queries + " queries, most probable "
                + targetDiagnosisIsMostProbable + ", is in window " + targetDiagnosisIsInWind + ", size of window  " + diagWinSize
                + ", reaction " + reactionTime + ", consistency checks " + consistencyCount);

        entry.addEntr(num_of_queries, queryCardinality, targetDiagnosisIsInWind, targetDiagnosisIsMostProbable,
                diagWinSize, userBreak, systemBreak, time, queryTime, diagTime, reactionTime, consistencyCount);

    }

    protected <E extends AxiomSet<OWLLogicalAxiom>> E containsItem(Collection<E> col, E item) {
        for (E o : col) {
            if (o.equals(item)) {
                if (logger.isTraceEnabled())
                    logger.trace("Target dianosis " + o + "is in the window");
                return o;
            }
        }
        return null;
    }


    protected QSS<OWLLogicalAxiom> createQSSWithDefaultParam(QSSType type) {
        switch (type) {
            case MINSCORE:
                return QSSFactory.createMinScoreQSS();
            case SPLITINHALF:
                return QSSFactory.createSplitInHalfQSS();
            case STATICRISK:
                return QSSFactory.createStaticRiskQSS(0.3);
            case DYNAMICRISK:
                return QSSFactory.createDynamicRiskQSS(0, 0.5, 0.25);
            case PENALTY:
                return QSSFactory.createPenaltyQSS(10);
            default:
                return QSSFactory.createMinScoreQSS();
        }
    }

    protected long computeDual(UniformCostSearch<OWLLogicalAxiom> searchDual, OWLTheory theoryDual,
                               AxiomSet<OWLLogicalAxiom> diagnosis, List<Double> queries, QSSType type) {
        TableList entry2 = new TableList();
        long timeDual = System.currentTimeMillis();
        int diagnosesCalc = 0;
        String daStr = "";
        int conflictsCalc = 0;
        //QSS<OWLLogicalAxiom> qss = null;
        //if (type != null) qss = createQSSWithDefaultParam(type);
        simulateBruteForceOnl(searchDual, theoryDual, diagnosis, entry2, type);
        timeDual = System.currentTimeMillis() - timeDual;
        AxiomSet<OWLLogicalAxiom> diag2 = searchDual.getStorage().getDiagnoses().iterator().next();
        boolean foundCorrectD2 = diag2.equals(diagnosis);
        boolean hasNegativeTestcases = searchDual.getTheory().getNonentailedTests().size() > 0;
        theoryDual.clearTestCases();
        searchDual.clearSearch();
        logger.info("dual tree iteration finished: window size "
                + entry2.getMeanWin() + " num of query " + entry2.getMeanQuery() +
                " time " + Utils.getStringTime(timeDual) + " found correct diag " + foundCorrectD2 +
                " diagnoses: " + diagnosesCalc + " conflict  " + conflictsCalc +
                " has negative tests " + hasNegativeTestcases + " diagnoses in storage " + daStr +
                " cached subsets " + theoryDual.getCache().size()
        );
        Assert.assertTrue(foundCorrectD2);
        queries.add(entry2.getMeanQuery());
        return timeDual;
    }

    protected long computeHS(UniformCostSearch<OWLLogicalAxiom> searchNormal,
                             OWLTheory theoryNormal, AxiomSet<OWLLogicalAxiom> diagnoses,
                             List<Double> queries, PerformanceTests.QSSType type) {
        TableList entry = new TableList();
        long timeNormal = System.currentTimeMillis();
        int diagnosesCalc = 0;
        String daStr = "";
        int conflictsCalc = 0;
        //QSS<OWLLogicalAxiom> qss = null;
        //if (type != null) qss = createQSSWithDefaultParam(type);
        simulateBruteForceOnl(searchNormal, theoryNormal, diagnoses, entry, type);
        timeNormal = System.currentTimeMillis() - timeNormal;
        AxiomSet<OWLLogicalAxiom> diag = searchNormal.getStorage().getDiagnoses().iterator().next();
        boolean foundCorrectD = diag.equals(diagnoses);
        boolean hasNegativeTestcases = searchNormal.getTheory().getNonentailedTests().size() > 0;
        theoryNormal.clearTestCases();
        searchNormal.clearSearch();
        logger.info("hstree iteration finished: window size "
                + entry.getMeanWin() + " num of query " + entry.getMeanQuery() + " time " +
                Utils.getStringTime(timeNormal) + " found correct diag " + foundCorrectD +
                " diagnoses: " + diagnosesCalc + " conflict  " + conflictsCalc +
                " has negative testst " + hasNegativeTestcases + " diagnoses in storage " + daStr +
                " cached subsets " + theoryNormal.getCache().size()
        );
        Assert.assertTrue(foundCorrectD);
        queries.add(entry.getMeanQuery());
        return timeNormal;
    }
}