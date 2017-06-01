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
package de.ovgu.featureide.fm.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.prop4j.And;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.SatSolver;
import org.sat4j.specs.TimeoutException;

import de.ovgu.featureide.fm.core.analysis.ConstraintProperties;
import de.ovgu.featureide.fm.core.analysis.FeatureModelProperties;
import de.ovgu.featureide.fm.core.analysis.FeatureProperties;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelElement;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.FeatureModelCNF;
import de.ovgu.featureide.fm.core.cnf.FeatureModelFormula;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.analysis.AbstractAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.AnalysisResult;
import de.ovgu.featureide.fm.core.cnf.analysis.AtomicSetAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.CoreDeadAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.DeterminedAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.HasSolutionAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.RedundancyAnalysis;
import de.ovgu.featureide.fm.core.cnf.solver.AdvancedSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver.SatResult;
import de.ovgu.featureide.fm.core.editing.NodeCreator;
import de.ovgu.featureide.fm.core.explanations.DeadFeatureExplanationCreator;
import de.ovgu.featureide.fm.core.explanations.Explanation;
import de.ovgu.featureide.fm.core.explanations.FalseOptionalFeatureExplanationCreator;
import de.ovgu.featureide.fm.core.explanations.RedundantConstraintExplanationCreator;
import de.ovgu.featureide.fm.core.filter.FeatureSetFilter;
import de.ovgu.featureide.fm.core.filter.HiddenFeatureFilter;
import de.ovgu.featureide.fm.core.filter.base.InverseFilter;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.ovgu.featureide.fm.core.functional.Functional.IFunction;
import de.ovgu.featureide.fm.core.io.manager.IFileManager;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.fm.core.job.monitor.NullMonitor;

/**
 * A collection of methods for working with {@link IFeatureModel} will replace
 * the corresponding methods in {@link IFeatureModel}
 * 
 * @author Soenke Holthusen
 * @author Florian Proksch
 * @author Stefan Krueger
 * @author Marcus Pinnecke (Feature Interface)
 */

@SuppressWarnings("deprecation")
public class FeatureModelAnalyzer {

	private static class AnalysisWrapper<R, A extends AbstractAnalysis<R>> {

		private final Class<A> analysis;

		private Object syncObject = new Object();
		private IMonitor monitor = new NullMonitor();
		private boolean enabled = true;

		private AnalysisResult<R> analysisResult;

		public AnalysisWrapper(Class<A> analysis) {
			this.analysis = analysis;
		}

		public R getCachedResult() {
			synchronized (this) {
				return analysisResult == null ? null : analysisResult.getResult();
			}
		}

		public A createNewAnalysis(CNF cnf) {
			try {
				return analysis.getConstructor(CNF.class).newInstance(cnf);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				throw new AssertionError();
			}
		}

		public R getResult(CNF cnf) {
			return getResult(createNewAnalysis(cnf));
		}

		public R getResult(A analysisInstance) {
			if (!enabled) {
				return null;
			}

			AnalysisResult<R> curAnalysisResult;
			IMonitor curMonitor;
			Object curSyncObject;
			synchronized (this) {
				curAnalysisResult = this.analysisResult;
				curMonitor = this.monitor;
				curSyncObject = this.syncObject;
			}

			synchronized (curSyncObject) {
				if (curAnalysisResult == null) {
					try {
						LongRunningWrapper.runMethod(analysisInstance, curMonitor);
						curAnalysisResult = analysisInstance.getResult();
					} catch (Exception e) {
						Logger.logError(e);
					}

					synchronized (this) {
						if (curSyncObject == this.syncObject) {
							this.analysisResult = curAnalysisResult;
							this.monitor = new NullMonitor();
						}
					}
				}
				return curAnalysisResult.getResult();
			}
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void reset() {
			synchronized (this) {
				analysisResult = null;
				monitor.cancel();
				monitor = new NullMonitor();
				syncObject = new Object();
			}
		}

	}

	public static class StringToFeature implements IFunction<String, IFeature> {
		private final IFeatureModel featureModel;

		public StringToFeature(IFeatureModel featureModel) {
			this.featureModel = featureModel;
		}

		@Override
		public IFeature invoke(String name) {
			return featureModel.getFeature(name);
		}
	};

	/**
	 * Defines whether features should be included into calculations.
	 * If features are not analyzed, then constraints a also NOT analyzed.
	 */
	public boolean calculateFeatures = true;
	/**
	 * Defines whether constraints should be included into calculations.
	 */
	public boolean calculateConstraints = true;
	/**
	 * Defines whether redundant constraints should be calculated.
	 */
	public boolean calculateRedundantConstraints = true;
	/**
	 * Defines whether constraints that are tautologies should be calculated.
	 */
	public boolean calculateTautologyConstraints = true;

	public boolean calculateFOConstraints = true;

	public boolean calculateDeadConstraints = true;
	/**
	 * Defines whether analysis should be performed automatically.
	 */
	public boolean runCalculationAutomatically = true;

	/**
	 * Remembers results for analyzed features.
	 */
	private final Map<IFeature, FeatureProperties> featurePropertiesMap = new WeakHashMap<>();

	/**
	 * Remembers results for analyzed constraints.
	 */
	private final Map<IConstraint, ConstraintProperties> constraintPropertiesMap = new WeakHashMap<>();

	/**
	 * Remembers results for analyzed feature model.
	 */
	private FeatureModelProperties featureModelProperties;

	/**
	 * Creates explanations for dead features.
	 * Stored for performance so the underlying CNF is not recreated for every explanation.
	 */
	private final DeadFeatureExplanationCreator deadFeatureExplanationCreator = new DeadFeatureExplanationCreator();
	/**
	 * Creates explanations for false-optional features.
	 * Stored for performance so the underlying CNF is not recreated for every explanation.
	 */
	private final FalseOptionalFeatureExplanationCreator falseOptionalFeatureExplanationCreator = new FalseOptionalFeatureExplanationCreator();
	/**
	 * Creates explanations for redundant constraints.
	 * Stored for performance so the underlying CNF is not recreated for every explanation.
	 */
	private final RedundantConstraintExplanationCreator redundantConstraintExplanationCreator = new RedundantConstraintExplanationCreator();

	private final FeatureModelFormula formula;
	private final IFileManager<IFeatureModel> fmManager;

	private AnalysisWrapper<Boolean, HasSolutionAnalysis> validAnalysis = new AnalysisWrapper<>(HasSolutionAnalysis.class);
	private AnalysisWrapper<List<LiteralSet>, AtomicSetAnalysis> atomicSetAnalysis = new AnalysisWrapper<>(AtomicSetAnalysis.class);
	private AnalysisWrapper<LiteralSet, CoreDeadAnalysis> coreDeadAnalysis = new AnalysisWrapper<>(CoreDeadAnalysis.class);
	
	private AnalysisWrapper<List<LiteralSet>, RedundancyAnalysis> redundancyAnalysis = new AnalysisWrapper<>(RedundancyAnalysis.class);
	private AnalysisWrapper<LiteralSet, DeterminedAnalysis> determinedAnalysis = new AnalysisWrapper<>(DeterminedAnalysis.class);

	public void reset() {
		validAnalysis.reset();
		atomicSetAnalysis.reset();
		coreDeadAnalysis.reset();
		redundancyAnalysis.reset();
		determinedAnalysis.reset();

		featurePropertiesMap.clear();
		constraintPropertiesMap.clear();
		featureModelProperties = null;
		deadFeatureExplanationCreator.setFeatureModel(fmManager.getObject());
		falseOptionalFeatureExplanationCreator.setFeatureModel(fmManager.getObject());
		redundantConstraintExplanationCreator.setFeatureModel(fmManager.getObject());
	}

	public FeatureModelAnalyzer(FeatureProject featureProject) {
		this.formula = featureProject.getFormula();
		this.fmManager = featureProject.getFeatureModelManager();
		reset();
	}

	public boolean isValid() {
		final Boolean result = validAnalysis.getResult(formula.getCNF());
		return result == null ? false : result;
	}

	public List<IFeature> getCoreFeatures() {
		final IFeatureModel featureModel = fmManager.getObject();
		final FeatureModelCNF cnf = formula.getCNF();
		final LiteralSet result = coreDeadAnalysis.getResult(cnf);
		if (result == null) {
			return Collections.emptyList();
		}
		return Functional.mapToList(cnf.getVariables().convertToString(result, true, false, false), new StringToFeature(featureModel));
	}

	public List<IFeature> getDeadFeatures() {
		final IFeatureModel featureModel = fmManager.getObject();
		final FeatureModelCNF cnf = formula.getCNF();
		final LiteralSet result = coreDeadAnalysis.getResult(cnf);
		if (result == null) {
			return Collections.emptyList();
		}
		return Functional.mapToList(cnf.getVariables().convertToString(result, false, true, false), new StringToFeature(featureModel));
	}

	/**
	 * Returns the list of features that occur in all variants, where one of the
	 * given features is selected. If the given list of features is empty, this
	 * method will calculate the features that are present in all variants
	 * specified by the feature model.
	 * 
	 * @return a list of features that is common to all variants
	 */
	public List<IFeature> commonFeatures() {
		final IFeatureModel featureModel = fmManager.getObject();
		final FeatureModelCNF cnf = formula.getCNF();
		final LiteralSet result = coreDeadAnalysis.getResult(cnf);
		if (result == null) {
			return Collections.emptyList();
		}
		final Set<IFeature> uncommonFeatures = Functional
				.toSet(Functional.map(cnf.getVariables().convertToString(result, true, true, false), new StringToFeature(featureModel)));
		return Functional.mapToList(featureModel.getFeatures(), new InverseFilter<>(new FeatureSetFilter(uncommonFeatures)),
				new Functional.IdentityFunction<IFeature>());
	}

	public List<List<IFeature>> getAtomicSets() {
		final IFeatureModel featureModel = fmManager.getObject();
		final FeatureModelCNF cnf = formula.getCNF();
		final List<LiteralSet> result = atomicSetAnalysis.getResult(cnf);
		if (result == null) {
			return Collections.emptyList();
		}

		final ArrayList<List<IFeature>> resultList = new ArrayList<>();
		for (LiteralSet literalList : result) {
			final List<IFeature> setList = new ArrayList<>();
			resultList.add(Functional.mapToList(cnf.getVariables().convertToString(literalList, true, true, false), new StringToFeature(featureModel)));

			for (int literal : literalList.getLiterals()) {
				final IFeature feature = featureModel.getFeature(cnf.getVariables().getName(literal));
				if (feature != null) {
					setList.add(feature);
				}
			}

		}
		return resultList;
	}

	/**
	 * Calculations for indeterminate hidden features
	 * 
	 * @param changedAttributes
	 */
	public List<IFeature> getIndeterminedHiddenFeatures() {
		final IFeatureModel featureModel = fmManager.getObject();
		final FeatureModelCNF cnf = formula.getCNF();
		LiteralSet result = determinedAnalysis.getCachedResult();
		if (result == null) {
			final DeterminedAnalysis analysis = determinedAnalysis.createNewAnalysis(cnf);
			analysis.setVariables(cnf.getVariables().convertToVariables(Functional.mapToList(featureModel.getFeatures(), new HiddenFeatureFilter(), FeatureUtils.GET_FEATURE_NAME)));
			result = determinedAnalysis.getResult(analysis);
			if (result == null) {
				return Collections.emptyList();
			}
		}
		return Functional.mapToList(cnf.getVariables().convertToString(result, false, true, false), new StringToFeature(featureModel));
	}

	public List<IFeature> getFalseOptionalFeatures() {
		final IFeatureModel featureModel = fmManager.getObject();
		final FeatureModelCNF cnf = formula.getCNF();
		List<LiteralSet> result = redundancyAnalysis.getCachedResult();
		if (result == null) {
			final RedundancyAnalysis analysis = redundancyAnalysis.createNewAnalysis(cnf);
			
			analysis.setClauseList(null);.setVariables(cnf.getVariables().convertToVariables(Functional.mapToList(featureModel.getFeatures(), new HiddenFeatureFilter(), FeatureUtils.GET_FEATURE_NAME)));
			result = redundancyAnalysis.getResult(analysis);
			if (result == null) {
				return Collections.emptyList();
			}
		}
		return Functional.mapToList(cnf.getVariables().convertToString(result, false, true, false), new StringToFeature(featureModel));
	}

	/**
	 * @param monitor
	 * @return Hashmap: key entry is Feature/Constraint, value usually
	 *         indicating the kind of attribute
	 */
	/*
	 * check all changes of this method and called methods with the related tests and
	 * benchmarks, see fm.core-test plug-in
	 * think about performance (no unnecessary or redundant calculations)
	 * 
	 * Hashing might be fast for locating features, but creating a HashSet is costly 
	 * So LinkedLists are much faster because the number of feature in the set is usually small (e.g. dead features)
	 */
	public HashMap<Object, Object> analyzeFeatureModel(IMonitor monitor) {
		final IFeatureModel featureModel = fmManager.getObject();
		final FeatureModelCNF cnf = formula.getCNF();
		for (AnalysisWrapper<?> analysisWrapper : analysisList) {
			// TODO !!!
			analysisWrapper.getResult(cnf);
		}
	}

	// TODO implement as analysis
	public int countConcreteFeatures() {
		int number = 0;
		for (IFeature feature : fmManager.getObject().getFeatures()) {
			if (feature.getStructure().isConcrete()) {
				number++;
			}
		}
		return number;
	}

	// TODO implement as analysis
	public int countHiddenFeatures() {
		int number = 0;
		for (IFeature feature : fmManager.getObject().getFeatures()) {
			final IFeatureStructure structure = feature.getStructure();
			if (structure.isHidden() || structure.hasHiddenParent()) {
				number++;
			}
		}
		return number;
	}

	// TODO implement as analysis
	public int countTerminalFeatures() {
		int number = 0;
		for (IFeature feature : fmManager.getObject().getFeatures()) {
			if (!feature.getStructure().hasChildren()) {
				number++;
			}
		}
		return number;
	}

	/**
	 * Sets the cancel status of analysis.<br>
	 * <code>true</code> if analysis should be stopped.
	 */
	public void cancel(boolean value) {
		//		cancel = value;
		reset();
	}

	/**
	 * Returns an explanation why the given feature model element is defect or null if it cannot be explained.
	 * 
	 * @param modelElement potentially defect feature model element
	 * @return an explanation why the given feature model element is defect or null if it cannot be explained
	 */
	public Explanation getExplanation(IFeatureModelElement modelElement) {
		Explanation explanation = null;
		if (modelElement instanceof IFeature) {
			final FeatureProperties featureProperties = featurePropertiesMap.get(modelElement);
			if (featureProperties != null) {
				final IFeature feature = (IFeature) modelElement;
				switch (featureProperties.getFeatureSelectionStatus()) {
				case DEAD:
					explanation = featureProperties.getDeadExplanation();
					if (explanation != null) {
						deadFeatureExplanationCreator.setDeadFeature(feature);
						explanation = deadFeatureExplanationCreator.getExplanation();
						featureProperties.setFalseOptionalExplanation(explanation);
					}
					break;
				default:
					break;
				}
				switch (featureProperties.getFeatureParentStatus()) {
				case FALSE_OPTIONAL:
					explanation = featureProperties.getFalseOptionalExplanation();
					if (explanation != null) {
						falseOptionalFeatureExplanationCreator.setFalseOptionalFeature(feature);
						explanation = falseOptionalFeatureExplanationCreator.getExplanation();
						featureProperties.setFalseOptionalExplanation(explanation);
					}
					break;
				default:
					break;
				}
			}
		} else if (modelElement instanceof IConstraint) {
			final ConstraintProperties constraintProperties = constraintPropertiesMap.get(modelElement);

			if (constraintProperties != null) {
				final IConstraint constraint = (IConstraint) modelElement;
				switch (constraintProperties.getConstraintRedundancyStatus()) {
				case REDUNDANT:
				case TAUTOLOGY:
				case IMPLICIT:
					explanation = constraintProperties.getRedundantExplanation();
					if (explanation != null) {
						redundantConstraintExplanationCreator.setRedundantConstraint(constraint);
						explanation = redundantConstraintExplanationCreator.getExplanation();
						constraintProperties.setRedundantExplanation(explanation);
					}
					break;
				default:
					break;
				}
			}
		}
		return explanation;
	}

	//	private FeatureProperties getFeatureProperties(final IFeature feature) {
	//		synchronized (featurePropertiesMap) {
	//			FeatureProperties featureProperties = featurePropertiesMap.get(feature);
	//		if (featureProperties == null) {
	//			featureProperties = new FeatureProperties(feature);
	//			
	//		}
	//		return featureProperties;
	//		}
	//	}

	//	/**
	//	 * Adds an explanation why the given feature model element is defect.
	//	 * Uses the default feature model stored in this instance.
	//	 * 
	//	 * @param modelElement potentially defect feature model element
	//	 */
	//	public void addExplanation(IFeatureModelElement modelElement) {
	//
	//		if (modelElement instanceof IFeature) {
	//			final IFeature feature = (IFeature) modelElement;
	//			switch (feature.getProperty().getFeatureStatus()) {
	//			case DEAD:
	//				addDeadFeatureExplanation(fm, feature);
	//				break;
	//			case FALSE_OPTIONAL:
	//				addFalseOptionalFeatureExplanation(fm, feature);
	//				break;
	//			default:
	//				break;
	//			}
	//		} else if (modelElement instanceof IConstraint) {
	//			final IConstraint constraint = (IConstraint) modelElement;
	//			switch (constraint.getConstraintAttribute()) {
	//			case REDUNDANT:
	//			case TAUTOLOGY:
	//			case IMPLICIT:
	//				addRedundantConstraintExplanation(fm, constraint);
	//				break;
	//			default:
	//				break;
	//			}
	//		}
	//	}
	//
	//	/**
	//	 * Returns an explanation why the given feature is dead or null if it cannot be explained.
	//	 * 
	//	 * @param feature potentially dead feature
	//	 * @return an explanation why the given feature is dead or null if it cannot be explained
	//	 */
	//	public Explanation getDeadFeatureExplanation(IFeature feature) {
	//		return deadFeatureExplanations.get(feature);
	//	}
	//
	//	/**
	//	 * Adds an explanation why the given feature is dead.
	//	 * Uses the default feature model stored in this instance.
	//	 * 
	//	 * @param feature potentially dead feature
	//	 */
	//	public void addDeadFeatureExplanation(IFeature feature) {
	//		addDeadFeatureExplanation(fm, feature);
	//	}
	//
	//	/**
	//	 * Adds an explanation why the given feature is dead.
	//	 * Uses the given feature model, which may differ from the default feature model stored in this instance.
	//	 * 
	//	 * @param fm feature model containing the feature
	//	 * @param feature potentially dead feature
	//	 */
	//	public void addDeadFeatureExplanation(IFeatureModel fm, IFeature feature) {
	//		final DeadFeatureExplanationCreator creator = fm == this.fm ? deadFeatureExplanationCreator : new DeadFeatureExplanationCreator(fm);
	//		creator.setDeadFeature(feature);
	//		deadFeatureExplanations.put(feature, creator.getExplanation());
	//	}
	//
	//	/**
	//	 * Returns an explanation why the given feature is false-optional or null if it cannot be explained.
	//	 * 
	//	 * @param feature potentially false-optional feature
	//	 * @return an explanation why the given feature is false-optional or null if it cannot be explained
	//	 */
	//	public Explanation getFalseOptionalFeatureExplanation(IFeature feature) {
	//		return falseOptionalFeatureExplanations.get(feature);
	//	}
	//
	//	/**
	//	 * Adds an explanation why the given feature is false-optional.
	//	 * Uses the default feature model stored in this instance.
	//	 * 
	//	 * @param feature potentially false-optional feature
	//	 */
	//	public void addFalseOptionalFeatureExplanation(IFeature feature) {
	//		addFalseOptionalFeatureExplanation(fm, feature);
	//	}
	//
	//	/**
	//	 * Adds an explanation why the given feature is false-optional.
	//	 * Uses the given feature model, which may differ from the default feature model stored in this instance.
	//	 * 
	//	 * @param fm feature model containing the feature
	//	 * @param feature potentially false-optional feature
	//	 */
	//	public void addFalseOptionalFeatureExplanation(IFeatureModel fm, IFeature feature) {
	//		final FalseOptionalFeatureExplanationCreator creator = fm == this.fm ? falseOptionalFeatureExplanationCreator
	//				: new FalseOptionalFeatureExplanationCreator(fm);
	//		creator.setFalseOptionalFeature(feature);
	//		falseOptionalFeatureExplanations.put(feature, creator.getExplanation());
	//	}
	//
	//	/**
	//	 * Returns an explanation why the given constraint is redundant or null if it cannot be explained.
	//	 * 
	//	 * @param constraint potentially redundant constraint
	//	 * @return an explanation why the given constraint is redundant or null if it cannot be explained
	//	 */
	//	public Explanation getRedundantConstraintExplanation(IConstraint constraint) {
	//		return redundantConstraintExplanations.get(constraint);
	//	}
	//
	//	/**
	//	 * Adds an explanation why the given constraint is redundant.
	//	 * Uses the default feature model stored in this instance.
	//	 * 
	//	 * @param constraint possibly redundant constraint
	//	 */
	//	public void addRedundantConstraintExplanation(IConstraint constraint) {
	//		addRedundantConstraintExplanation(fm, constraint);
	//	}
	//
	//	/**
	//	 * Adds an explanation why the given constraint is redundant.
	//	 * Uses the given feature model, which may differ from the default feature model stored in this instance.
	//	 * This is for example the case when explaining implicit constraints in subtree models.
	//	 * 
	//	 * @param fm feature model containing the constraint
	//	 * @param constraint potentially redundant constraint
	//	 */
	//	public void addRedundantConstraintExplanation(IFeatureModel fm, IConstraint constraint) {
	//		final RedundantConstraintExplanationCreator creator = fm == this.fm ? redundantConstraintExplanationCreator
	//				: new RedundantConstraintExplanationCreator(fm);
	//		creator.setRedundantConstraint(constraint);
	//		redundantConstraintExplanations.put(constraint, creator.getExplanation());
	//	}

	/**
	 * checks whether A implies B for the current feature model.
	 * 
	 * in detail the following condition should be checked whether
	 * 
	 * FM => ((A1 and A2 and ... and An) => (B1 or B2 or ... or Bn))
	 * 
	 * is true for all values
	 * 
	 * @param A
	 *            set of features that form a conjunction
	 * @param B
	 *            set of features that form a conjunction
	 * @return
	 * @throws TimeoutException
	 */
	public boolean checkImplies(Collection<IFeature> a, Collection<IFeature> b) throws TimeoutException {
		if (b.isEmpty()) {
			return true;
		}
		final FeatureModelCNF cnf = formula.getCNF();

		// (A1 and ... An) => (B1 or ... Bm)
		int[] literals = new int[a.size() + b.size()];
		int index = 0;
		for (IFeature feature : a) {
			literals[index++] = cnf.getVariables().getVariable(feature.getName());
		}
		for (IFeature feature : b) {
			literals[index++] = -cnf.getVariables().getVariable(feature.getName());
		}

		final AdvancedSatSolver advancedSatSolver = new AdvancedSatSolver(cnf);
		advancedSatSolver.assignmentPushAll(literals);

		return advancedSatSolver.hasSolution() == SatResult.FALSE;
	}

	public boolean checkIfFeatureCombinationNotPossible(IFeature a, Collection<IFeature> b) throws TimeoutException {
		if (b.isEmpty())
			return true;

		Node featureModel = NodeCreator.createNodes(fmManager.getObject().clone(null));
		boolean notValid = true;
		for (IFeature f : b) {
			Node node = new And(new And(featureModel, new Literal(NodeCreator.getVariable(f, fmManager.getObject().clone(null)))),
					new Literal(NodeCreator.getVariable(a, fmManager.getObject().clone(null))));
			notValid &= !new SatSolver(node, 1000).hasSolution();
		}
		return notValid;
	}

	/**
	 * checks some condition against the feature model. use only if you know
	 * what you are doing!
	 * 
	 * @return
	 * @throws TimeoutException
	 */
	public boolean checkCondition(Node condition) {
		final FeatureModelCNF cnf = formula.getCNF();
		return false;
	}

}
