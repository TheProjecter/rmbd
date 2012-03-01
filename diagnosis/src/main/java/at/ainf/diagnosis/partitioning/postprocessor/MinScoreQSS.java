package at.ainf.diagnosis.partitioning.postprocessor;

import at.ainf.theory.model.InconsistentTheoryException;
import at.ainf.theory.model.SolverException;
import at.ainf.theory.storage.AxiomSet;
import at.ainf.theory.storage.Partition;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: pfleiss
 * Date: 10.02.12
 * Time: 11:19
 * To change this template use File | Settings | File Templates.
 */
public class MinScoreQSS<T> extends AbstractQSS<T> {


    
    public Partition<T> run(List<Partition<T>> partitions, Partition<T> currentBest) throws SolverException, InconsistentTheoryException {
        //if (partitions!=null && partitions.size() > 0) {
        //    lastQuery = Collections.min(partitions,new ScoreComparator());
        //    return lastQuery;
        //}

        return currentBest;
    }


}
