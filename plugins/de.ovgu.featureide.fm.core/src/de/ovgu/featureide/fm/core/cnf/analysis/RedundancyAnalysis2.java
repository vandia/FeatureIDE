///* FeatureIDE - A Framework for Feature-Oriented Software Development
// * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
// *
// * This file is part of FeatureIDE.
// * 
// * FeatureIDE is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Lesser General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// * 
// * FeatureIDE is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Lesser General Public License for more details.
// * 
// * You should have received a copy of the GNU Lesser General Public License
// * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
// *
// * See http://featureide.cs.ovgu.de/ for further information.
// */
//package de.ovgu.featureide.fm.core.cnf.analysis;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import de.ovgu.featureide.fm.core.cnf.CNF;
//import de.ovgu.featureide.fm.core.cnf.ClauseLengthComparatorDsc;
//import de.ovgu.featureide.fm.core.cnf.LiteralSet;
//import de.ovgu.featureide.fm.core.cnf.SatUtils;
//import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
//import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver;
//import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver.SatResult;
//import de.ovgu.featureide.fm.core.cnf.solver.ModifiableSatSolver;
//import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
//
///**
// * Finds core and dead features.
// * 
// * @author Sebastian Krieter
// */
//public class RedundancyAnalysis2 extends AbstractAnalysis<Map<LiteralSet, Boolean>> {
//
//	public static class RedundancyResult extends AnalysisResult<Map<LiteralSet, Boolean>> {
//
//		public RedundancyResult(LiteralSet assumptions) {
//			super(RedundancyAnalysis2.class.getName(), assumptions);
//		}
//
//	}
//
//	private List<LiteralSet> clauseList;
//
//	public RedundancyAnalysis2(CNF satInstance, List<LiteralSet> possiblyRedundantClauses) {
//		super(satInstance);
//		this.clauseList = possiblyRedundantClauses;
//	}
//
//	public RedundancyAnalysis2(ISatSolver2 solver, List<LiteralSet> possiblyRedundantClauses) {
//		super(solver);
//		this.clauseList = possiblyRedundantClauses;
//	}
//
//	public Map<LiteralSet, Boolean> analyze(IMonitor monitor) throws Exception {
//		final Map<LiteralSet, Boolean> resultList = new HashMap<>();
//
//		if (clauseList == null) {
//			return resultList;
//		}
//		monitor.setRemainingWork(clauseList.size() + 1);
//		Collections.sort(clauseList, new ClauseLengthComparatorDsc());
//		final ModifiableSatSolver fullSolver = new ModifiableSatSolver(solver.getSatInstance());
//		monitor.step();
//
//		for (int i = clauseList.size() - 1; i >= 0; --i) {
//			final LiteralSet clause = clauseList.get(i);
//			final boolean redundant = isRedundant(fullSolver, clause);
//			if (!redundant) {
//				fullSolver.addClause(clause);
//			}
//			resultList.put(clause, redundant);
//			monitor.step();
//		}
//
//		return resultList;
//	}
//
//	protected final boolean isRedundant(ISimpleSatSolver solver, LiteralSet curClause) {
//		final SatResult hasSolution = solver.hasSolution(SatUtils.negateSolution(curClause.getLiterals()));
//		switch (hasSolution) {
//		case FALSE:
//			return true;
//		case TIMEOUT:
//		case TRUE:
//			return false;
//		default:
//			throw new AssertionError(hasSolution);
//		}
//	}
//
//	@Override
//	protected RedundancyResult getResultObject(LiteralSet assumptions) {
//		return new RedundancyResult(assumptions);
//	}
//
//}
