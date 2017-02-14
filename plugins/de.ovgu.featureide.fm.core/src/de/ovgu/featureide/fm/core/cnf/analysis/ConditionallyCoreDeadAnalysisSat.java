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

import org.sat4j.minisat.core.Solver;

import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.SatUtils;
import de.ovgu.featureide.fm.core.cnf.solver.FixedLiteralSelectionStrategy;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2.SelectionStrategy;
import de.ovgu.featureide.fm.core.cnf.solver.VarOrderHeap2;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Finds core and dead features.
 * 
 * @author Sebastian Krieter
 */
public class ConditionallyCoreDeadAnalysisSat extends AConditionallyCoreDeadAnalysis {

	public ConditionallyCoreDeadAnalysisSat(ISatSolver2 solver) {
		super(solver);
	}

	public ConditionallyCoreDeadAnalysisSat(CNF satInstance) {
		super(satInstance);
	}

	public LiteralSet analyze(IMonitor monitor) throws Exception {
//		satCount = 0;
//		solver.getAssignment().ensure(fixedVariables.length);
//		for (int i = 0; i < fixedVariables.length; i++) {
//			solver.assignmentPush(fixedVariables[i]);
//		}
		solver.setSelectionStrategy(SelectionStrategy.POSITIVE);
		int[] model1 = solver.findSolution();
//		satCount++;

		if (model1 != null) {
			solver.setSelectionStrategy(SelectionStrategy.NEGATIVE);
			int[] model2 = solver.findSolution();
//			satCount++;

			for (int i = 0; i < assumptions.getLiterals().length; i++) {
				model1[Math.abs(assumptions.getLiterals()[i]) - 1] = 0;
			}

			SatUtils.updateSolution(model1, model2);
			((Solver<?>) solver.getInternalSolver()).setOrder(new VarOrderHeap2(new FixedLiteralSelectionStrategy(model1, true), solver.getOrder()));
			for (int i = 0; i < model1.length; i++) {
				final int varX = model1[i];
				if (varX != 0) {
					solver.assignmentPush(-varX);
//					satCount++;
					switch (solver.hasSolution()) {
					case FALSE:
						solver.assignmentReplaceLast(varX);
						break;
					case TIMEOUT:
						solver.assignmentPop();
						break;
					case TRUE:
						solver.assignmentPop();
						SatUtils.updateSolution(model1, solver.getSolution());
						solver.setOrderShuffle();
						break;
					}
				}
			}
		}
		return new LiteralSet(solver.getAssignmentArray());
	}

}
