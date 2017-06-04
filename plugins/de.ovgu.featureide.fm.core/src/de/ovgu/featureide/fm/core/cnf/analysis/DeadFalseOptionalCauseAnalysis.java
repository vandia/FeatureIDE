package de.ovgu.featureide.fm.core.cnf.analysis;
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
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedList;
//import java.util.List;
//
//import org.prop4j.Not;
//import org.sat4j.specs.ContradictionException;
//import org.sat4j.specs.IConstr;
//
//import de.ovgu.featureide.fm.core.ConstraintAttribute;
//import de.ovgu.featureide.fm.core.FeatureStatus;
//import de.ovgu.featureide.fm.core.base.FeatureUtils;
//import de.ovgu.featureide.fm.core.base.IConstraint;
//import de.ovgu.featureide.fm.core.base.IFeature;
//import de.ovgu.featureide.fm.core.base.IFeatureModel;
//import de.ovgu.featureide.fm.core.cnf.CNF;
//import de.ovgu.featureide.fm.core.cnf.LiteralSet;
//import de.ovgu.featureide.fm.core.cnf.Nodes;
//import de.ovgu.featureide.fm.core.cnf.SatUtils;
//import de.ovgu.featureide.fm.core.cnf.Variables;
//import de.ovgu.featureide.fm.core.cnf.CNFCreator.ModelType;
//import de.ovgu.featureide.fm.core.cnf.solver.AdvancedSatSolver;
//import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
//import de.ovgu.featureide.fm.core.cnf.solver.ModifiableSatSolver;
//import de.ovgu.featureide.fm.core.cnf.solver.RuntimeContradictionException;
//import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver.SatResult;
//import de.ovgu.featureide.fm.core.filter.HiddenFeatureFilter;
//import de.ovgu.featureide.fm.core.functional.Functional;
//import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
//import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
//
///**
// * Finds core and dead features.
// * 
// * @author Sebastian Krieter
// */
//public class RedundancyAnalysis3 extends ARedundancyAnalysis {
//
//	public RedundancyAnalysis3(CNF satInstance) {
//		super(satInstance);
//	}
//
//	public RedundancyAnalysis3(ISatSolver2 solver) {
//		super(solver);
//	}
//
//	public List<LiteralSet> analyze(IMonitor monitor) throws Exception {
//		if (clauseList == null) {
//			return Collections.emptyList();
//		}
//		monitor.setRemainingWork(clauseList.size() + 1);
//
//		final List<LiteralSet> resultList = new ArrayList<>(clauseList);
//		final AdvancedSatSolver emptySolver = new ModifiableSatSolver(new CNF(solver.getSatInstance(), false));
//		final List<IConstr> constraintMarkers = new ArrayList<>(emptySolver.addClauses(clauseList));
//		monitor.step();
//
//		int i = 0;
//		for (LiteralSet constraint : clauseList) {
//			boolean redundant = true;
//			final IConstr constr = constraintMarkers.get(i);
//			if (constr != null) {
//				emptySolver.removeClause(constr);
//				redundant = isRedundant(emptySolver, constraint);
//			}
//
//			if (!redundant) {
//				resultList.set(i, null);
//			}
//			i++;
//			monitor.step();
//		}
//		return resultList;
//	}
//	
//	private void checkConstraintDeadAndFalseOptional(final List<IConstraint> constraints) throws ContradictionException {
//		nodeCreator.setModelType(ModelType.OnlyStructure);
//		final CNF si = nodeCreator.createNodes();
//		final ISatSolver2 modSat = new AdvancedSatSolver(si);
//
//		final List<IFeature> deadList = new LinkedList<>(deadFeatures);
//		final List<IFeature> foList = new LinkedList<>(falseOptionalFeatures);
//		monitor.checkCancel();
//
//		for (IConstraint constraint : constraints) {
//			modSat.addClauses(Nodes.convert(si.getVariables(), constraint.getNode()));
//
//			if (constraint.getConstraintAttribute() == ConstraintAttribute.NORMAL) {
//				if (calculateDeadConstraints) {
//					final List<IFeature> newDeadFeature = checkFeatureDead2(modSat, deadList);
//					if (!newDeadFeature.isEmpty()) {
//						constraint.setDeadFeatures(newDeadFeature);
//						deadList.removeAll(newDeadFeature);
//						setConstraintAttribute(constraint, ConstraintAttribute.DEAD);
//					}
//				}
//
//				if (calculateFOConstraints) {
//					final List<IFeature> newFOFeature = checkFeatureFalseOptional2(modSat, foList);
//					if (!newFOFeature.isEmpty()) {
//						constraint.setFalseOptionalFeatures(newFOFeature);
//						foList.removeAll(newFOFeature);
//						if (constraint.getConstraintAttribute() == ConstraintAttribute.NORMAL) {
//							setConstraintAttribute(constraint, ConstraintAttribute.FALSE_OPTIONAL);
//						}
//					}
//				}
//			}
//			monitor.checkCancel();
//		}
//	}
//
//	private List<IFeature> checkFeatureDead2(final ISatSolver2 solver, List<IFeature> deadList) {
//		if (deadList.size() == 0) {
//			return Collections.emptyList();
//		}
//		final List<IFeature> result = new ArrayList<>();
//		int[] deadVars = new int[deadList.size()];
//		int j = 0;
//		for (IFeature deadFeature : deadList) {
//			deadVars[j++] = solver.getSatInstance().getVariables().getVariable(deadFeature.getName());
//		}
//		final LiteralSet solution2 = LongRunningWrapper.runMethod(new CoreDeadAnalysis(solver, new LiteralSet(deadVars)));
//		for (int i = 0; i < solution2.getLiterals().length; i++) {
//			final int var = solution2.getLiterals()[i];
//			if (var < 0) {
//				result.add(fm.getFeature(solver.getSatInstance().getVariables().getName(var)));
//			}
//		}
//		return result;
//	}
//
//	private List<IFeature> checkFeatureFalseOptional2(final ISatSolver2 solver, List<IFeature> foList) {
//		if (foList.size() == 0) {
//			return Collections.emptyList();
//		}
//		final List<IFeature> result = new ArrayList<>();
//		final List<LiteralSet> possibleFOFeatures = new ArrayList<>();
//		final CNF si = solver.getSatInstance();
//		for (IFeature feature : foList) {
//			final IFeature parent = FeatureUtils.getParent(feature);
//			if (parent != null && (!feature.getStructure().isMandatorySet() || !parent.getStructure().isAnd())) {
//				possibleFOFeatures.add(new LiteralSet(-si.getVariables().getVariable(parent.getName()), si.getVariables().getVariable(feature.getName())));
//			}
//		}
//		final List<LiteralSet> solution3 = LongRunningWrapper.runMethod(new RedundancyAnalysis(solver, possibleFOFeatures));
//		for (LiteralSet pair : solution3) {
//			result.add(fm.getFeature(si.getVariables().getName(pair.getLiterals()[1])));
//		}
//		return result;
//	}
//
//
//
//}
