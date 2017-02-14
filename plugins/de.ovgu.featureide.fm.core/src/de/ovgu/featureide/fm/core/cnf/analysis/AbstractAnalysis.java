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
package de.ovgu.featureide.fm.core.cnf.analysis;

import org.sat4j.core.VecInt;

import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.solver.AdvancedSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
import de.ovgu.featureide.fm.core.cnf.solver.RuntimeContradictionException;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Abstract analysis.
 * 
 * @author Sebastian Krieter
 */
public abstract class AbstractAnalysis<T> implements LongRunningMethod<T> {

	protected ISatSolver2 solver;

	protected LiteralSet assumptions = null;

	public AbstractAnalysis(CNF satInstance) {
		try {
			this.solver = new AdvancedSatSolver(satInstance);
		} catch (RuntimeContradictionException e) {
			this.solver = null;
		}
	}

	public AbstractAnalysis(ISatSolver2 solver) {
		this.solver = solver;
	}

	@Override
	public final T execute(IMonitor monitor) throws Exception {
		if (solver == null) {
			return null;
		}
		if (assumptions != null) {
			solver.getAssignment().pushAll(new VecInt(assumptions.getLiterals()));
		}
		assumptions = new LiteralSet(solver.getAssignmentArray());

		monitor.checkCancel();
		try {
			return analyze(monitor);
		} catch (Throwable e) {
			throw e;
		} finally {
			solver.assignmentClear(0);
		}
	}
	
	protected abstract T analyze(IMonitor monitor) throws Exception;

	public LiteralSet getAssumptions() {
		return assumptions;
	}

	public void setAssumptions(LiteralSet assumptions) {
		this.assumptions = assumptions;
	}

}
