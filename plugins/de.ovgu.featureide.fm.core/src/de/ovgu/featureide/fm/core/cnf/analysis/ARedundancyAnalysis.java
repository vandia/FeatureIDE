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

import java.util.List;

import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.SatUtils;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver.SatResult;
import de.ovgu.featureide.fm.core.cnf.solver.ModifiableSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.RuntimeContradictionException;

/**
 * Finds core and dead features.
 * 
 * @author Sebastian Krieter
 */
public abstract class ARedundancyAnalysis extends AClauseAnalysis<List<LiteralSet>> {

	public ARedundancyAnalysis(CNF satInstance) {
		super(satInstance);
	}

	public ARedundancyAnalysis(ISatSolver2 solver) {
		super(solver);
	}

	protected ISatSolver2 initSolver(CNF satInstance) {
		try {
			return new ModifiableSatSolver(satInstance);
		} catch (RuntimeContradictionException e) {
			return null;
		}
	}

	protected boolean isRedundant(ISimpleSatSolver solver, LiteralSet curClause) {
		final SatResult hasSolution = solver.hasSolution(SatUtils.negateSolution(curClause.getLiterals()));
		switch (hasSolution) {
		case FALSE:
			return true;
		case TIMEOUT:
		case TRUE:
			return false;
		default:
			throw new AssertionError(hasSolution);
		}
	}

}
