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

package org.jamesframework.core.search;

import org.jamesframework.core.search.status.SearchStatus;
import java.util.Collection;
import java.util.Iterator;
import org.jamesframework.core.exceptions.SearchException;
import org.jamesframework.core.problems.Problem;
import org.jamesframework.core.problems.Solution;
import org.jamesframework.core.search.cache.EvaluatedMoveCache;
import org.jamesframework.core.search.cache.SingleEvaluatedMoveCache;
import org.jamesframework.core.search.neigh.Move;
import org.jamesframework.core.util.JamesConstants;

/**
 * A neighbourhood search is a specific kind of local search in which the current solution is repeatedly modified by
 * applying moves, generated by one or more neighbourhoods, that transform this solution into a similar, neighbouring
 * solution. Generated moves can either be accepted, in which case the current solution is updated, or rejected, in
 * which case the current solution is retained. The number of accepted and rejected moves, during the current or last
 * run, can be accessed. This additional metadata applies to the current run only.
 * 
 * @param <SolutionType> solution type of the problems that may be solved using this search, required to extend {@link Solution}
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public abstract class NeighbourhoodSearch<SolutionType extends Solution> extends LocalSearch<SolutionType> {

    /******************/
    /* PRIVATE FIELDS */
    /******************/
    
    // number of accepted/rejected moves during current run
    private long numAcceptedMoves, numRejectedMoves;
    
    // evaluated move cache
    private EvaluatedMoveCache cache;
    
    /***************/
    /* CONSTRUCTOR */
    /***************/
    
    /**
     * Create a new neighbourhood search to solve the given problem, with default name "NeighbourhoodSearch".
     * 
     * @throws NullPointerException if <code>problem</code> is <code>null</code>
     * @param problem problem to solve
     */
    public NeighbourhoodSearch(Problem<SolutionType> problem){
        this(null, problem);
    }
    
    /**
     * Create a new neighbourhood search to solve the given problem, with a custom name. If <code>name</code> is
     * <code>null</code>, the default name "NeighbourhoodSearch" will be assigned.
     * 
     * @throws NullPointerException if <code>problem</code> is <code>null</code>
     * @param problem problem to solve
     * @param name custom search name
     */
    public NeighbourhoodSearch(String name, Problem<SolutionType> problem){
        super(name != null ? name : "NeighbourhoodSearch", problem);
        // initialize per run metadata
        numAcceptedMoves = JamesConstants.INVALID_MOVE_COUNT;
        numRejectedMoves = JamesConstants.INVALID_MOVE_COUNT;
        // set default (single) evaluated move cache
        cache = new SingleEvaluatedMoveCache();
    }
    
    /*********/
    /* CACHE */
    /*********/
    
    /**
     * Sets a custom evaluated move cache, which is used to avoid repeated evaluation or validation of the same move
     * from the same current solution. By default, a {@link SingleEvaluatedMoveCache} is used. Note that this method
     * may only be called when the search is idle. If the cache is set to <code>null</code>, no caching will be applied.
     * 
     * @param cache custom evaluated move cache
     * @throws SearchException if the search is not idle
     */
    public void setEvaluatedMoveCache(EvaluatedMoveCache cache){
        // acquire status lock
        synchronized(getStatusLock()){
            // assert idle
            assertIdle("Cannot set custom evaluated move cache in neighbourhood search.");
            // set cache
            this.cache = cache;
        }
    }
    
    /******************/
    /* INITIALIZATION */
    /******************/
    
    /**
     * When a neighbourhood search is started, the number of accepted and rejected moves is reset to zero.
     */
    @Override
    protected void searchStarted(){
        // call super
        super.searchStarted();
        // reset neighbourhood search specific, per run metadata
        numAcceptedMoves = 0;
        numRejectedMoves = 0;
    }
    
    /*****************************************/
    /* METADATA APPLYING TO CURRENT RUN ONLY */
    /*****************************************/
    
    /**
     * <p>
     * Get the number of moves accepted during the <i>current</i> (or last) run. The precise return value
     * depends on the status of the search:
     * </p>
     * <ul>
     *  <li>
     *   If the search is either RUNNING or TERMINATING, this method returns the number of moves accepted
     *   since the current run was started.
     *  </li>
     *  <li>
     *   If the search is IDLE, the total number of moves accepted during the last run is returned, if any.
     *   Before the first run, {@link JamesConstants#INVALID_MOVE_COUNT}.
     *  </li>
     *  <li>
     *   While INITIALIZING the current run, {@link JamesConstants#INVALID_MOVE_COUNT} is returned.
     *  </li>
     * </ul>
     * <p>
     * The return value is always positive, except in those cases when {@link JamesConstants#INVALID_MOVE_COUNT}
     * is returned.
     * </p>
     * 
     * @return number of moves accepted during the current (or last) run
     */
    public long getNumAcceptedMoves(){
        // depends on search status: synchronize with status updates
        synchronized(getStatusLock()){
            if(getStatus() == SearchStatus.INITIALIZING){
                // initializing
                return JamesConstants.INVALID_MOVE_COUNT;
            } else {
                // idle, running or terminating
                return numAcceptedMoves;
            }
        }
    }
    
    /**
     * <p>
     * Get the number of moves rejected during the <i>current</i> (or last) run. The precise return value
     * depends on the status of the search:
     * </p>
     * <ul>
     *  <li>
     *   If the search is either RUNNING or TERMINATING, this method returns the number of moves rejected
     *   since the current run was started.
     *  </li>
     *  <li>
     *   If the search is IDLE, the total number of moves rejected during the last run is returned, if any.
     *   Before the first run, {@link JamesConstants#INVALID_MOVE_COUNT}.
     *  </li>
     *  <li>
     *   While INITIALIZING the current run, {@link JamesConstants#INVALID_MOVE_COUNT} is returned.
     *  </li>
     * </ul>
     * <p>
     * The return value is always positive, except in those cases when {@link JamesConstants#INVALID_MOVE_COUNT}
     * is returned.
     * </p>
     * 
     * @return number of moves rejected during the current (or last) run
     */
    public long getNumRejectedMoves(){
        // depends on search status: synchronize with status updates
        synchronized(getStatusLock()){
            if(getStatus() == SearchStatus.INITIALIZING){
                // initializing
                return JamesConstants.INVALID_MOVE_COUNT;
            } else {
                // idle, running or terminating
                return numRejectedMoves;
            }
        }
    }
    
    /***********************/
    /* PROTECTED UTILITIES */
    /***********************/
    
    /**
     * When updating the current solution in a neighbourhood search, the evaluated move cache is cleared because
     * it is no longer valid for the new current solution.
     * 
     * @param solution new current solution
     * @param evaluation evaluation of new current solution
     */
    @Override
    protected void updateCurrentSolution(SolutionType solution, double evaluation){
        // call super
        super.updateCurrentSolution(solution, evaluation);
        // clear evaluated move cache
        if(cache != null){
            cache.clear();
        }
    }
    
    /**
     * Evaluates the neighbour obtained by applying the given move to the current solution. If this
     * move has been evaluated before and the computed value is still available in the cache, the
     * cached value will be returned. Else, the evaluation will be computed and offered to the cache.
     * 
     * @param move move applied to the current solution
     * @return evaluation of obtained neighbour, possibly retrieved from the evaluated move cache
     */
    protected double evaluateMove(Move<? super SolutionType> move){
        Double eval = null;
        // check cache
        if(cache != null){
            eval = cache.getCachedMoveEvaluation(move);
        }
        if(eval != null){
            // cache hit: return cached value
            return eval;
        } else {
            // cache miss: evaluate and cache
            move.apply(getCurrentSolution());                       // apply move
            eval = getProblem().evaluate(getCurrentSolution());     // evaluate neighbour
            if(cache != null){
                cache.cacheMoveEvaluation(move, eval);              // cache evaluation
            }
            move.undo(getCurrentSolution());                        // undo move
            return eval;                                            // return evaluation
        }
    }
    
    /**
     * Validates the neighbour obtained by applying the given move to the current solution. If this
     * move has been validated before and the result is still available in the cache, the cached result
     * will be returned. Else, the neighbour will be validated and the result is offered to the cache.
     * 
     * @param move move applied to the current solution
     * @return <code>true</code> if the obtained neighbour is <b>not</b> rejected,
     *         possibly retrieved from the evaluated move cache
     */
    protected boolean validateMove(Move<? super SolutionType> move){
        Boolean reject = null;
        // check cache
        if(cache != null){
            reject = cache.getCachedMoveRejection(move);
        }
        if(reject != null){
            // cache hit: return cached value
            return !reject;
        } else {
            // cache miss: validate and cache
            move.apply(getCurrentSolution());                               // apply move
            reject = getProblem().rejectSolution(getCurrentSolution());     // validate neighbour
            if(cache != null){
                cache.cacheMoveRejection(move, reject);                     // cache validity
            }
            move.undo(getCurrentSolution());                                // undo move
            return !reject;                                                 // return validity
        }
    }
    
    /**
     * Checks whether the given move leads to an improvement when being applied to the current solution.
     * An improvement is made if and only if the given move is <b>not</b> <code>null</code>, the neighbour
     * obtained by applying the move is <b>not</b> rejected (see {@link Problem#rejectSolution(Solution)})
     * and this neighbour has a better evaluation than the current solution (i.e. a positive delta is
     * observed, see {@link #computeDelta(double, double)}).
     * <p>
     * Note that computed values are cached to prevent multiple evaluations or validations of the same move.
     * 
     * @param move move to be applied to the current solution
     * @return <code>true</code> if applying this move yields an improvement
     */
    protected boolean isImprovement(Move<? super SolutionType> move){
        return move != null
                && validateMove(move)
                && computeDelta(evaluateMove(move), getCurrentSolutionEvaluation()) > 0;
    }
    
    /**
     * Given a collection of possible moves, get the move which yields the largest delta (see {@link #computeDelta(double, double)})
     * when applying it to the current solution, where only those moves leading to a valid neighbour are considered (those moves for
     * which {@link Problem#rejectSolution(Solution)} returns <code>false</code>). If <code>positiveDeltasOnly</code> is set to
     * <code>true</code>, only moves yielding a (strictly) positive delta, i.e. an improvement, are considered. May return
     * <code>null</code> if all moves lead to invalid solutions, or if no valid move with positive delta is found, in case
     * <code>positiveDeltasOnly</code> is set to <code>true</code>.
     * <p>
     * Note that all computed values are cached to prevent multiple evaluations or validations of the same move. Before returning
     * the selected "best" move, if any, its evaluation and validity are cached again to maximize the probability that these values
     * will remain available in the cache.
     * 
     * @param moves collection of possible moves
     * @param positiveDeltasOnly if set to <code>true</code>, only moves with <code>delta &gt; 0</code> are considered
     * @return valid move with largest delta, may be <code>null</code>
     */
    protected Move<? super SolutionType> getMoveWithLargestDelta(Collection<? extends Move<? super SolutionType>> moves, boolean positiveDeltasOnly){
        // track best move and corresponding delta
        Move<? super SolutionType> bestMove = null, curMove;
        double bestMoveDelta = -Double.MAX_VALUE, curMoveDelta, curMoveEval;
        Double bestMoveEval = null;
        // go through all moves
        for (Move<? super SolutionType> move : moves) {
            curMove = move;
            // validate move
            if (validateMove(curMove)) {
                // evaluate move
                curMoveEval = evaluateMove(curMove);
                // compute delta
                curMoveDelta = computeDelta(curMoveEval, getCurrentSolutionEvaluation());
                // compare with current best move
                if (curMoveDelta > bestMoveDelta                             // higher delta
                        && (!positiveDeltasOnly || curMoveDelta > 0)) {      // ensure positive delta, if required
                    bestMove = curMove;
                    bestMoveDelta = curMoveDelta;
                    bestMoveEval = curMoveEval;
                }
            }
        }
        // re-cache best move, if any
        if(bestMove != null && cache != null){
            cache.cacheMoveRejection(bestMove, false);              // best move is surely not rejected
            cache.cacheMoveEvaluation(bestMove, bestMoveEval);      // cache best move evaluation 
        }
        // return best move
        return bestMove;
    }
    
    /**
     * Accept the given move by applying it to the current solution. Updates the evaluation of the current solution and compares
     * it with the currently known best solution to check whether a new best solution has been found. Note that this method does
     * <b>not</b> verify whether the given move yields a valid neighbour, but assumes that this has already been checked <i>prior</i>
     * to deciding to accept the move. Therefore, it should <b>never</b> be called with a move that results in a solution for which
     * {@link Problem#rejectSolution(Solution)} returns <code>true</code>.
     * <p>
     * After updating the current solution, the evaluated move cache is cleared as this cache is no longer valid for the new current
     * solution. Furthermore, any local search listeners are informed and the number of accepted moves is updated.
     * 
     * @param move accepted move to be applied to the current solution
     */
    protected void acceptMove(Move<? super SolutionType> move){
        // compute new evaluation (likely to be present in cache)
        double newEval = evaluateMove(move); 
        // apply move to current solution (IMPORTANT: after evaluating the move!)
        move.apply(getCurrentSolution());
        // update current solution (same object, modified in place) and best solution (no validation)
        updateCurrentAndBestSolution(getCurrentSolution(), newEval, true);
        // increase accepted move counter
        numAcceptedMoves++;
    }
    
    /**
     * Increase the number of accepted moves with the given value.
     * 
     * @param inc value with which the number of accepted moves is increased
     */
    protected void incNumAcceptedMoves(long inc){
        numAcceptedMoves += inc;
    }
    
    /**
     * Indicate that a move was rejected. This method only updates the rejected move counter. If this method
     * is called for every rejected move, the number of rejected moves will be correctly reported.
     */
    protected void rejectMove(){
        incNumRejectedMoves(1);
    }
    
    /**
     * Increase the number of rejected moves with the given value.
     * 
     * @param inc value with which the number of rejected moves is increased
     */
    protected void incNumRejectedMoves(long inc){
        numRejectedMoves += inc;
    }
    
}
