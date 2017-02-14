/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.core.cnf.solver;

import java.util.Arrays;
import java.util.Random;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.orders.NegativeLiteralSelectionStrategy;
import org.sat4j.minisat.orders.PositiveLiteralSelectionStrategy;
import org.sat4j.minisat.orders.RSATPhaseSelectionStrategy;
import org.sat4j.minisat.orders.RandomLiteralSelectionStrategy;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import de.ovgu.featureide.fm.core.base.util.RingList;
import de.ovgu.featureide.fm.core.cnf.CNF;

/**
 * Sat solver with advanced support.
 * 
 * @author Sebastian Krieter
 */
public class AdvancedSatSolver extends SimpleSatSolver implements ISatSolver2 {

	protected final VecInt assignment;
	protected final int[] order;

	protected RingList<int[]> solutionList = RingList.empytRingList();
	protected SelectionStrategy strategy = SelectionStrategy.ORG;

	public AdvancedSatSolver(CNF satInstance) {
		super(satInstance);
		this.strategy = SelectionStrategy.ORG;

		this.assignment = new VecInt(satInstance.size());
		this.order = new int[satInstance.size()];
		setOrderFix();
	}

	protected AdvancedSatSolver(AdvancedSatSolver oldSolver) {
		super(oldSolver);
		this.strategy = oldSolver.strategy;

		this.order = Arrays.copyOf(oldSolver.order, oldSolver.order.length);
		this.assignment = new VecInt(0);
		oldSolver.assignment.copyTo(this.assignment);

	}

	@Override
	public void assignmentClear(int size) {
		assignment.shrinkTo(size);
	}

	@Override
	public void assignmentPop() {
		assignment.pop();
	}

	@Override
	public void assignmentPush(int x) {
		assignment.push(x);
	}

	@Override
	public void assignmentReplaceLast(int x) {
		assignment.pop().unsafePush(x);
	}

	@Override
	public AdvancedSatSolver clone() {
		if (this.getClass() == AdvancedSatSolver.class) {
			return new AdvancedSatSolver(this);
		} else {
			throw new RuntimeException("Cloning not supported for " + this.getClass().toString());
		}
	}

	@Override
	public int[] findSolution() {
		return hasSolution() == SatResult.TRUE ? solver.model() : null;
	}

	@Override
	public VecInt getAssignment() {
		return assignment;
	}

	@Override
	public int[] getAssignmentArray() {
		return Arrays.copyOf(assignment.toArray(), assignment.size());
	}

	@Override
	public int[] getAssignmentArray(int from, int to) {
		return Arrays.copyOfRange(assignment.toArray(), from, to);
	}

	@Override
	public ISolver getInternalSolver() {
		return solver;
	}

	@Override
	public int[] getOrder() {
		return order;
	}

	@Override
	public SelectionStrategy getSelectionStrategy() {
		return strategy;
	}

	@Override
	public RingList<int[]> getSolutionList() {
		return solutionList;
	}

	@Override
	public SatResult hasSolution() {
		try {
			if (solver.isSatisfiable(assignment, false)) {
				solutionList.add(solver.model());
				return SatResult.TRUE;
			} else {
				return SatResult.FALSE;
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			return SatResult.TIMEOUT;
		}
	}

	/**
	 * {@inheritDoc}<br/>
	 * <br/>
	 * Does only consider the given {@code assignment} and <b>not</b> the global assignment variable of the solver.
	 */
	@Override
	public SatResult hasSolution(int... assignment) {
		final int[] unitClauses = new int[assignment.length];
		System.arraycopy(assignment, 0, unitClauses, 0, unitClauses.length);

		try {
			if (solver.isSatisfiable(new VecInt(unitClauses), false)) {
				solutionList.add(solver.model());
				return SatResult.TRUE;
			} else {
				return SatResult.FALSE;
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			return SatResult.TIMEOUT;
		}
	}

	@Override
	public void setOrder(int[] order) {
		assert order.length <= this.order.length;
		System.arraycopy(order, 0, this.order, 0, order.length);
	}

	@Override
	public void setOrderFix() {
		for (int i = 0; i < order.length; i++) {
			order[i] = i + 1;
		}
	}

	@Override
	public void setOrderShuffle() {
		final Random rnd = new Random();
		for (int i = order.length - 1; i >= 0; i--) {
			final int index = rnd.nextInt(i + 1);
			final int a = order[index];
			order[index] = order[i];
			order[i] = a;
		}
	}

	@Override
	public void setSelectionStrategy(SelectionStrategy strategy) {
		if (this.strategy != strategy) {
			this.strategy = strategy;
			switch (strategy) {
			case NEGATIVE:
				solver.setOrder(new VarOrderHeap2(new NegativeLiteralSelectionStrategy(), order));
				break;
			case ORG:
				solver.setOrder(new VarOrderHeap(new RSATPhaseSelectionStrategy()));
				break;
			case POSITIVE:
				solver.setOrder(new VarOrderHeap2(new PositiveLiteralSelectionStrategy(), order));
				break;
			case RANDOM:
				solver.setOrder(new VarOrderHeap2(new RandomLiteralSelectionStrategy(), order));
				break;
			default:
				assert false;
			}
		}
	}

	@Override
	public void useSolutionList(int size) {
		solutionList = new RingList<>(size);
	}

}