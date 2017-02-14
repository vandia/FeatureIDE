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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.TimeoutException;

import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.CNF;

/**
 * Light version of a sat solver with reduced functionality.
 * 
 * @author Sebastian Krieter
 */
public class SimpleSatSolver implements ISimpleSatSolver {

	protected final CNF satInstance;
	protected final Solver<?> solver;

	public SimpleSatSolver(CNF satInstance) {
		this.satInstance = satInstance;
		this.solver = newSolver();
	}

	protected SimpleSatSolver(SimpleSatSolver oldSolver) {
		this.satInstance = oldSolver.satInstance;
		this.solver = newSolver();
	}

	@Override
	public IConstr addClause(LiteralSet mainClause) {
		return addClauseInternal(solver, mainClause);
	}

	protected IConstr addClauseInternal(Solver<?> solver, LiteralSet mainClause) {
		try {
			final int[] literals = mainClause.getLiterals();
			assert checkClauseValidity(literals);
			return solver.addClause(new VecInt(Arrays.copyOf(literals, literals.length)));
		} catch (ContradictionException e) {
			throw new RuntimeContradictionException(e);
		}
	}

	@Override
	public List<IConstr> addClauses(Iterable<? extends LiteralSet> clauses) {
		return addClauses(solver, clauses);
	}

	protected List<IConstr> addClauses(Solver<?> solver, Iterable<? extends LiteralSet> clauses) {
		final ArrayList<IConstr> constrList = new ArrayList<>();
		for (LiteralSet clause : clauses) {
			constrList.add(addClauseInternal(solver, clause));
		}
		return constrList;
	}

	@Override
	public SimpleSatSolver clone() {
		if (this.getClass() == SimpleSatSolver.class) {
			return new SimpleSatSolver(this);
		} else {
			throw new RuntimeException("Cloning not supported for " + this.getClass().toString());
		}
	}

	@Override
	public CNF getSatInstance() {
		return satInstance;
	}

	@Override
	public int[] getSolution() {
		return solver.model();
	}

	@Override
	public SatResult hasSolution() {
		try {
			if (solver.isSatisfiable(false)) {
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
	public SatResult hasSolution(int... assignment) {
		final int[] unitClauses = new int[assignment.length];
		System.arraycopy(assignment, 0, unitClauses, 0, unitClauses.length);

		try {
			if (solver.isSatisfiable(new VecInt(unitClauses), false)) {
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
	public void removeClause(IConstr constr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeLastClause() {
		removeLastClauses(1);
	}

	@Override
	public void removeLastClauses(int numberOfClauses) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void reset() {
		solver.reset();
	}

	private boolean checkClauseValidity(final int[] literals) {
		for (int i = 0; i < literals.length; i++) {
			final int l = literals[i];
			if (l == 0 || Math.abs(l) > satInstance.size()) {
				return false;
			}
		}
		return true;
	}

	protected final Solver<?> newSolver() {
		final Solver<?> solver = createSolver();
		configureSolver(solver);
		initSolver(solver);
		return solver;
	}

	/**
	 * Creates the Sat4J solver instance.
	 */
	protected Solver<?> createSolver() {
		return (Solver<?>) SolverFactory.newDefault();
	}

	/**
	 * Set several options for the Sat4J solver instance.
	 */
	protected void configureSolver(Solver<?> solver) {
		solver.setTimeoutMs(1000);
		solver.newVar(satInstance.size());
		solver.setDBSimplificationAllowed(true);
		solver.setVerbose(false);
	}

	/**
	 * Add clauses to the solver.
	 * Initializes the order instance.
	 */
	protected void initSolver(Solver<?> solver) {
		final List<LiteralSet> clauses = satInstance.getClauses();
		if (!clauses.isEmpty()) {
			solver.setExpectedNumberOfClauses(clauses.size());
			addClauses(solver, clauses);
		}
		solver.getOrder().init();
	}

	public void setTimeout(int timeout) {
		solver.setTimeout(timeout);
	}

}