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

package org.jamesframework.core.subset.algo;

import java.util.concurrent.TimeUnit;
import org.jamesframework.core.problems.objectives.evaluations.PenalizedEvaluation;
import org.jamesframework.core.search.SearchTestTemplate;
import org.jamesframework.core.subset.SubsetProblem;
import org.jamesframework.core.subset.SubsetSolution;
import org.jamesframework.core.util.SetUtilities;
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
 * Test LR subset search.
 * 
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public class LRSubsetSearchTest extends SearchTestTemplate {

    // LR subset search
    private LRSubsetSearch search;
    
    // default LR parameters
    private final int DEFAULT_L = 2;
    private final int DEFAULT_R = 1;
    
    // maximum runtime
    private final long SINGLE_RUN_RUNTIME = Long.MAX_VALUE;
    private final long MULTI_RUN_RUNTIME = Long.MAX_VALUE;
    private final TimeUnit MAX_RUNTIME_TIME_UNIT = TimeUnit.MILLISECONDS;
    
    // number of runs in multi run tests
    private final int NUM_RUNS = 5;
    
    /**
     * Print message when starting tests.
     */
    @BeforeClass
    public static void setUpClass() {
        System.out.println("# Testing LRSubsetSearch ...");
        SearchTestTemplate.setUpClass();
    }

    /**
     * Print message when tests are complete.
     */
    @AfterClass
    public static void tearDownClass() {
        System.out.println("# Done testing LRSubsetSearch!");
    }
    
    @Override
    @Before
    public void setUp(){
        // call super
        super.setUp();
        // create LR subset search 
        search = new LRSubsetSearch(problem, DEFAULT_L, DEFAULT_R);
        // set and log random seed
        setRandomSeed(search);
    }
    
    @After
    public void tearDown(){
        // dispose search
        search.dispose();
    }
    
    @Test
    public void testConstructor(){
        System.out.println(" - test constructor");
        
        boolean thrown;
        
        thrown = false;
        try {
            new LRSubsetSearch(problem, -1, 1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        
        thrown = false;
        try {
            new LRSubsetSearch(problem, 1, -1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        
        thrown = false;
        try {
            new LRSubsetSearch(problem, 1, 1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        
        thrown = false;
        try {
            new LRSubsetSearch(problem, 0, 0);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        
        for(int i=0; i<1000; i++){
            int l = RG.nextInt(500);
            int r = RG.nextInt(499);
            if(r >= l){
                r++;
            }
            LRSubsetSearch s = new LRSubsetSearch(problem, l, r);
            // verify
            assertEquals(l, s.getL());
            assertEquals(r, s.getR());
        }
        
    }

    /**
     * Test single run.
     */
    @Test
    public void testSingleRun() {
        System.out.println(" - test single run (L=" + DEFAULT_L + ", R=" + DEFAULT_R + ")");
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
    }
    
    /**
     * Test single run, decreasing (L=1, R=2).
     */
    @Test
    public void testSingleRunDecreasing() {
        System.out.println(" - test single run, decreasing (L=1, R=2)");
        // create LR search with L=1 and R=2
        search = new LRSubsetSearch(problem, 1, 2);
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
    }
    
    /**
     * Test single run, decreasing, custom initial solution (L=1, R=2).
     */
    @Test
    public void testSingleRunDecreasingCustomInitialSolution() {
        System.out.println(" - test single run, decreasing, custom initial solution (L=1, R=2)");
        // create LR search with L=1 and R=2
        search = new LRSubsetSearch(problem, 1, 2);
        // set custom initial solution: random subset of size 1.5 times the desired subset size
        SubsetSolution initialSolution = new SubsetSolution(data.getIDs(), SetUtilities.getRandomSubset(data.getIDs(), (int)(1.5*SUBSET_SIZE), RG));
        search.setCurrentSolution(initialSolution);
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
    }
    
    /**
     * Test single run, any subset size.
     */
    @Test
    public void testSingleRunAnySubsetSize() {
        System.out.println(" - test single run, any subset size (L=" + DEFAULT_L + ", R=" + DEFAULT_R + ")");
        // alter problem so that any subset size is accepted
        problem = new SubsetProblem<>(data, obj, 0, data.getIDs().size());
        // create LR search with default L and R
        search = new LRSubsetSearch(problem, DEFAULT_L, DEFAULT_R);
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
        System.out.println("   >>> num selected: " + search.getBestSolution().getNumSelectedIDs());
    }
    
    /**
     * Test single run, any subset size + large delta.
     */
    @Test
    public void testSingleRunAnySubsetSizeLargeDelta() {
        System.out.println(" - test single run, any subset size, large delta (L=20, R=5)");
        // alter problem so that any subset size is accepted
        problem = new SubsetProblem<>(data, obj, 0, data.getIDs().size());
        // create LR search with L=20 and R=5 (large delta)
        search = new LRSubsetSearch(problem, 20, 5);
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
        System.out.println("   >>> num selected: " + search.getBestSolution().getNumSelectedIDs());
    }
    
    /**
     * Test single run, huge delta (larger than full set size).
     */
    @Test
    public void testSingleRunHugeDelta() {
        System.out.println(" - test single run, huge delta (L=1000, R=300)");
        // create LR search with L=1000 and R=300 (huge delta)
        search = new LRSubsetSearch(problem, 1000, 300);
        // single run
        singleRunWithMaxRuntime(search, SINGLE_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT);
    }
    
    /**
     * Test single run with unsatisfiable constraint.
     */
    @Test
    public void testSingleRunWithUnsatisfiableConstraint() {
        System.out.println(" - test single run with unsatisfiable constraint (L=" + DEFAULT_L + ", R=" + DEFAULT_R + ")");
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
        System.out.println(" - test subsequent runs (maximizing) (L=" + DEFAULT_L + ", R=" + DEFAULT_R + ")");
        // perform multiple runs (maximizing objective)
        multiRunWithMaximumRuntime(search, MULTI_RUN_RUNTIME, MAX_RUNTIME_TIME_UNIT, NUM_RUNS, true, true);
    }
    
    /**
     * Test subsequent runs (minimizing).
     */
    @Test
    public void testSubsequentRunsMinimizing() {
        System.out.println(" - test subsequent runs (minimizing) (L=" + DEFAULT_L + ", R=" + DEFAULT_R + ")");
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
        System.out.println(" - test subsequent runs with unsatisfiable constraint (L=" + DEFAULT_L + ", R=" + DEFAULT_R + ")");
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
        System.out.println(" - test subsequent runs with penalizing constraint (L=" + DEFAULT_L + ", R=" + DEFAULT_R + ")");
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