/*
 * Copyright 2014 Ghent University, Bayer CropScience.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jamesframework.core.search.algo;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.jamesframework.core.problems.objectives.evaluations.PenalizedEvaluation;
import org.jamesframework.core.subset.SubsetSolution;
import org.jamesframework.core.search.SearchTestTemplate;
import org.jamesframework.core.search.neigh.Move;
import org.jamesframework.core.search.neigh.Neighbourhood;
import org.jamesframework.test.stubs.NeverSatisfiedConstraintStub;
import org.jamesframework.test.stubs.NeverSatisfiedPenalizingConstraintStub;
import org.jamesframework.test.util.TestConstants;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 * Test steepest descent algorithm.
 * 
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public class SteepestDescentTest extends SearchTestTemplate {

    // steepest descent algorithm
    private SteepestDescent<SubsetSolution> search;
    
    // maximum runtime
    private final long SINGLE_RUN_RUNTIME = 1000;
    private final long MULTI_RUN_RUNTIME = 50;
    private final TimeUnit MAX_RUNTIME_TIME_UNIT = TimeUnit.MILLISECONDS;
    
    // number of runs in multi run tests
    private final int NUM_RUNS = 5;
    
    /**
     * Print message when starting tests.
     */
    @BeforeClass
    public static void setUpClass() {
        System.out.println("# Testing SteepestDescent ...");
        SearchTestTemplate.setUpClass();
    }

    /**
     * Print message when tests are complete.
     */
    @AfterClass
    public static void tearDownClass() {
        System.out.println("# Done testing SteepestDescent!");
    }
    
    @Override
    @Before
    public void setUp(){
        // call super
        super.setUp();
        // create steepest descent search
        search = new SteepestDescent<>(problem, neigh);
        // set and log random seed
        setRandomSeed(search);
    }
    
    @After
    public void tearDown(){
        // dispose search
        search.dispose();
    }

    /**
     * Test single run.
     */
    @Test
    public void testSingleRun() {
        System.out.println(" - test single run");
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
    }
    
    @Test
    public void testEmptyNeighbourhood() {
        System.out.println(" - test with empty neighbourhood");
        // create empty neighbourhood
        Neighbourhood<SubsetSolution> emptyNeigh = new Neighbourhood<SubsetSolution>() {
            @Override
            public Move<? super SubsetSolution> getRandomMove(SubsetSolution solution, Random rnd) {
                return null;
            }
            @Override
            public List<? extends Move<? super SubsetSolution>> getAllMoves(SubsetSolution solution) {
                return Collections.emptyList();
            }
        };
        // set in search
        search.setNeighbourhood(emptyNeigh);
        // run search
        search.start();
        // verify: stopped after first step
        assertEquals(1, search.getSteps());
    }
    
    /**
     * Test single run with unsatisfiable constraint.
     */
    @Test
    public void testSingleRunWithUnsatisfiableConstraint() {
        System.out.println(" - test single run with unsatisfiable constraint");
        // add constraint
        problem.addMandatoryConstraint(new NeverSatisfiedConstraintStub());
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
        // verify
        assertNull(search.getBestSolution());
    }
    
    /**
     * Test single run with unsatisfiable penalizing constraint.
     */
    @Test
    public void testSingleRunWithUnsatisfiablePenalizingConstraint() {
        System.out.println(" - test single run with unsatisfiable penalizing constraint");
        // set constraint
        final double penalty = 7.8;
        problem.addPenalizingConstraint(new NeverSatisfiedPenalizingConstraintStub(penalty));
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
        // verify
        PenalizedEvaluation penEval = (PenalizedEvaluation) search.getBestSolutionEvaluation();
        assertEquals(penalty, penEval.getEvaluation().getValue() - penEval.getValue(), TestConstants.DOUBLE_COMPARISON_PRECISION);
    }
    
    /**
     * Test subsequent runs (maximizing).
     */
    @Test
    public void testSubsequentRuns() {
        System.out.println(" - test subsequent runs (maximizing)");
        // perform multiple runs (maximizing objective)
        multiRunWithMaximumRuntime(search, MULTI_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT, NUM_RUNS, true, true);
    }
    
    /**
     * Test subsequent runs (minimizing).
     */
    @Test
    public void testSubsequentRunsMinimizing() {
        System.out.println(" - test subsequent runs (minimizing)");
        // set minimizing
        obj.setMinimizing();
        // perform multiple runs (maximizing objective)
        multiRunWithMaximumRuntime(search, MULTI_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT, NUM_RUNS, false, true);
    }
    
    /**
     * Test subsequent runs with unsatisfiable constraint.
     */
    @Test
    public void testSubsequentRunsWithUnsatisfiableConstraint() {
        System.out.println(" - test subsequent runs with unsatisfiable constraint");
        // set constraint
        problem.addMandatoryConstraint(new NeverSatisfiedConstraintStub());
        // perform multiple runs (maximizing objective)
        multiRunWithMaximumRuntime(search, MULTI_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT, NUM_RUNS, true, true);
        // verify
        assertNull(search.getBestSolution());
    }
    
    /**
     * Test subsequent runs with unsatisfiable penalizing constraint.
     */
    @Test
    public void testSubsequentRunsWithUnsatisfiablePenalizingConstraint() {
        System.out.println(" - test subsequent runs with unsatisfiable penalizing constraint");
        // set constraint
        final double penalty = 7.8;
        problem.addPenalizingConstraint(new NeverSatisfiedPenalizingConstraintStub(penalty));
        // perform multiple runs (maximizing objective)
        multiRunWithMaximumRuntime(search, MULTI_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT, NUM_RUNS, true, true);
        // verify
        PenalizedEvaluation penEval = (PenalizedEvaluation) search.getBestSolutionEvaluation();
        assertEquals(penalty, penEval.getEvaluation().getValue() - penEval.getValue(), TestConstants.DOUBLE_COMPARISON_PRECISION);
    }
    
    /**
     * Test subsequent runs with penalizing constraint.
     */
    @Test
    public void testSubsequentRunsWithPenalizingConstraint() {
        System.out.println(" - test subsequent runs with penalizing constraint");
        // set constraint
        problem.addPenalizingConstraint(constraint);
        // perform 3 times as many runs as usual for this harder problem (maximizing objective)
        multiRunWithMaximumRuntime(search, MULTI_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT, 3*NUM_RUNS, true, false);
        System.out.println("   >>> best: " + search.getBestSolutionEvaluation());
        // constraint satisfied ?
        if(problem.getViolatedConstraints(search.getBestSolution()).isEmpty()){
            System.out.println("   >>> constraint satisfied!");
        } else {
            System.out.println("   >>> constraint not satisfied, penalty "
                    + constraint.validate(search.getBestSolution(), data).getPenalty());
        }
    }

}