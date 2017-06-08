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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.ClauseLengthComparatorDsc;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.solver.AdvancedSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
import de.ovgu.featureide.fm.core.cnf.solver.ModifiableSatSolver;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Finds core and dead features.
 * 
 * @author Sebastian Krieter
 */
public class RedundancyCauseAnalysis extends AClauseAnalysis<List<List<LiteralSet>>> {

	public RedundancyCauseAnalysis(CNF satInstance) {
		super(satInstance);
	}

	public RedundancyCauseAnalysis(ISatSolver2 solver) {
		super(solver);
	}

	protected List<LiteralSet> redundantClauseList;

	public List<LiteralSet> getRedundantClauseList() {
		return redundantClauseList;
	}

	public void setRedundantClauseList(List<LiteralSet> redundantClauseList) {
		this.redundantClauseList = redundantClauseList;
	}

	public List<List<LiteralSet>> analyze(IMonitor monitor) throws Exception {
		if (clauseList == null) {
			return Collections.emptyList();
		}
		monitor.setRemainingWork(clauseList.size() + 1);

		final List<List<LiteralSet>> resultList = new ArrayList<>(clauseList.size());
		for (int i = 0; i < clauseList.size(); i++) {
			resultList.add(null);
		}
		List<LiteralSet> remainingClauses = new ArrayList<>(redundantClauseList);
		final Integer[] index = Functional.getSortedIndex(clauseList, new ClauseLengthComparatorDsc());
		final AdvancedSatSolver emptySolver = new ModifiableSatSolver(new CNF(solver.getSatInstance(), true));
		monitor.step();

		final ArrayList<LiteralSet> newClauseList = new ArrayList<>();
		for (int i = index.length - 1; i >= 0; --i) {
			emptySolver.addClause(clauseList.get(index[i]));

			final List<LiteralSet> newClauses = LongRunningWrapper.runMethod(new RedundancyAnalysis(solver, remainingClauses));
			newClauseList.clear();
			for (LiteralSet newClause : newClauses) {
				if (newClause != null) {
					newClauseList.add(newClause);
				}
			}
			if (!newClauseList.isEmpty()) {
				resultList.set(index[i], newClauseList);
				remainingClauses.removeAll(newClauseList);
			}
			monitor.step();
		}

		return resultList;
	}

}
