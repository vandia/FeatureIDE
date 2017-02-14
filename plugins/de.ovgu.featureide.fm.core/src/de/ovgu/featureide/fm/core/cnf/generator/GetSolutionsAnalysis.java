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
package de.ovgu.featureide.fm.core.cnf.generator;

import java.util.ArrayList;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.SatUtils;
import de.ovgu.featureide.fm.core.cnf.analysis.AbstractAnalysis;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Determines whether a sat instance is satisfiable and returns the found model.
 * 
 * @author Sebastian Krieter
 */
public class GetSolutionsAnalysis extends AbstractAnalysis<List<LiteralSet>> {

	private final int max;

	public GetSolutionsAnalysis(ISatSolver2 solver, int max) {
		super(solver);
		this.max = max;
	}

	public GetSolutionsAnalysis(CNF satInstance, int max) {
		super(satInstance);
		this.max = max;
	}

	@Override
	public List<LiteralSet> analyze(IMonitor monitor) throws Exception {
		final ArrayList<LiteralSet> solutionList = new ArrayList<>();

		final ISolver internalSolver = solver.getInternalSolver();
		int count = 0;
		while (++count <= max && internalSolver.isSatisfiable(true)) {
			final int[] nextSolution = internalSolver.model();
			solutionList.add(new LiteralSet(nextSolution));
			try {
				internalSolver.addClause(new VecInt(SatUtils.negateSolution(nextSolution)));
			} catch (ContradictionException e) {
				break;
			}
		}

		return solutionList;
	}

}
