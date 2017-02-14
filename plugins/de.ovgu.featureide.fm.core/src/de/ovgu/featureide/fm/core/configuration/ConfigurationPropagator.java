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
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.core.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.sat4j.specs.TimeoutException;

import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.util.Functional;
import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.CNFCreator;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.SatUtils;
import de.ovgu.featureide.fm.core.cnf.analysis.CoreDeadAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.CountSolutionsAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.HasSolutionAnalysis;
import de.ovgu.featureide.fm.core.cnf.generator.GetSolutionsAnalysis;
import de.ovgu.featureide.fm.core.cnf.generator.OneWiseConfigurationGenerator;
import de.ovgu.featureide.fm.core.cnf.manipulator.remove.CNFSilcer;
import de.ovgu.featureide.fm.core.cnf.solver.AdvancedSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2.SelectionStrategy;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver.SatResult;
import de.ovgu.featureide.fm.core.filter.AbstractFeatureFilter;
import de.ovgu.featureide.fm.core.filter.HiddenFeatureFilter;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Updates a configuration.
 * 
 * @author Sebastian Krieter
 */
public class ConfigurationPropagator implements IConfigurationPropagator {

	// TODO fix monitor values

	public class CanBeValidMethod implements LongRunningMethod<Boolean> {
		@Override
		public Boolean execute(IMonitor monitor) {
			if (clauses == null) {
				return false;
			}
			AdvancedSatSolver s = getSolverForCurrentConfiguration(false, true);
			return s.hasSolution() == SatResult.TRUE;
		}
	}

	public class Resolve implements LongRunningMethod<Void> {
		@Override
		public Void execute(IMonitor workMonitor) throws Exception {
			if (clauses == null) {
				return null;
			}

			// Reset all automatic values
			configuration.resetAutomaticValues();

			final List<SelectableFeature> manualSelected = getManualSelected();
			workMonitor.setRemainingWork(manualSelected.size() + configuration.features.size() + 1);

			final AdvancedSatSolver solver = new AdvancedSatSolver(clausesWithoutHidden);

			workMonitor.step();

			removeRedundantManual(workMonitor, manualSelected, solver);

			int[] relevantFeatures = new int[manualSelected.size()];
			int index = 0;
			for (SelectableFeature selectableFeature : manualSelected) {
				relevantFeatures[index++] = clausesWithoutHidden.getVariable(selectableFeature.getName());
			}

			final LiteralSet result = new CoreDeadAnalysis(solver, new LiteralSet(relevantFeatures)).analyze(workMonitor);
			for (int featureIndex : result.getLiterals()) {
				configuration.getSelectablefeature(clausesWithoutHidden.getName(featureIndex))
						.setAutomatic(featureIndex < 0 ? Selection.UNSELECTED : Selection.SELECTED);
			}

			return null;
		}

		private void removeRedundantManual(IMonitor workMonitor, final List<SelectableFeature> manualSelected, final AdvancedSatSolver solver)
				throws AssertionError {
			for (Iterator<SelectableFeature> iterator = manualSelected.iterator(); iterator.hasNext();) {
				final SelectableFeature next = iterator.next();
				solver.assignmentPush(clausesWithoutHidden.getVariable(next.getName(), next.getSelection() == Selection.SELECTED));

				final SatResult satResult = solver.hasSolution();
				switch (satResult) {
				case FALSE:
				case TIMEOUT:
					next.setManual(Selection.UNDEFINED);
					iterator.remove();
					solver.assignmentPop();
					break;
				case TRUE:
					break;
				default:
					throw new AssertionError(satResult);
				}
				workMonitor.worked();
			}
		}

	}

	private List<SelectableFeature> getManualSelected() {
		final List<SelectableFeature> manualSelected = new ArrayList<>();
		for (SelectableFeature feature : configuration.features) {
			if (feature.getManual() != Selection.UNDEFINED) {
				manualSelected.add(feature);
			}
		}
		return manualSelected;
	}

	private List<SelectableFeature> getUndefined() {
		final List<SelectableFeature> undefined = new ArrayList<>();
		for (SelectableFeature feature : configuration.features) {
			if (feature.getSelection() == Selection.UNDEFINED) {
				undefined.add(feature);
			}
		}
		return undefined;
	}

	public Resolve resolve() {
		return new Resolve();
	}

	public class CountSolutionsMethod implements LongRunningMethod<Long> {
		private final int timeout;

		public CountSolutionsMethod(int timeout) {
			this.timeout = timeout;
		}

		@Override
		public Long execute(IMonitor monitor) throws Exception {
			if (clauses == null) {
				return 0L;
			}
			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(false, false);
			solver.setTimeout(timeout);
			return new CountSolutionsAnalysis(solver).analyze(monitor);
		}

	}

	private AdvancedSatSolver getSolverForCurrentConfiguration(boolean deselectUndefinedFeatures, boolean considerHiddenFeatures) {
		final CNF satInstance = considerHiddenFeatures ? clauses : clausesWithoutHidden;
		final AdvancedSatSolver solver = new AdvancedSatSolver(satInstance);
		for (SelectableFeature feature : configuration.features) {
			if ((deselectUndefinedFeatures || feature.getSelection() != Selection.UNDEFINED)
					&& (configuration.ignoreAbstractFeatures || feature.getFeature().getStructure().isConcrete())
					&& (considerHiddenFeatures || !feature.getFeature().getStructure().hasHiddenParent())) {
				solver.assignmentPush(satInstance.getVariable(feature.getFeature().getName(), feature.getSelection() == Selection.SELECTED));
			}
		}
		return solver;
	}

	public class FindOpenClauses implements LongRunningMethod<List<LiteralSet>> {

		private List<SelectableFeature> featureList;

		public FindOpenClauses(List<SelectableFeature> featureList) {
			this.featureList = featureList;
		}

		public List<LiteralSet> execute(IMonitor workMonitor) {
			if (clauses == null) {
				return Collections.emptyList();
			}
			final boolean[] results = new boolean[clausesWithoutHidden.size() + 1];
			final List<LiteralSet> openClauses = new ArrayList<>();

			for (SelectableFeature selectableFeature : featureList) {
				selectableFeature.setRecommended(Selection.UNDEFINED);
				selectableFeature.clearOpenClauses();
			}

			workMonitor.setRemainingWork(clausesWithoutHidden.getClauses().size());

			loop: for (LiteralSet clause : clausesWithoutHidden.getClauses()) {
				workMonitor.step();
				final int[] orLiterals = clause.getLiterals();
				for (int j = 0; j < orLiterals.length; j++) {
					final int literal = orLiterals[j];
					final SelectableFeature feature = configuration.getSelectablefeature(clausesWithoutHidden.getName(literal));
					final Selection selection = feature.getSelection();
					switch (selection) {
					case SELECTED:
						if (literal > 0) {
							continue loop;
						}
						break;
					case UNDEFINED:
					case UNSELECTED:
						if (literal < 0) {
							continue loop;
						}
						break;
					default:
						throw new AssertionError(selection);
					}
				}

				boolean newLiterals = false;
				for (int j = 0; j < orLiterals.length; j++) {
					final int literal = orLiterals[j];
					if (!results[Math.abs(literal)]) {
						results[Math.abs(literal)] = true;
						newLiterals = true;

						final SelectableFeature feature = configuration.getSelectablefeature(clausesWithoutHidden.getName(literal));
						final Selection selection = feature.getSelection();
						switch (selection) {
						case SELECTED:
							feature.setRecommended(Selection.UNSELECTED);
							feature.addOpenClause(openClauses.size(), clause);
							feature.setSatMapping(clausesWithoutHidden);
							break;
						case UNDEFINED:
						case UNSELECTED:
							feature.setRecommended(Selection.SELECTED);
							feature.addOpenClause(openClauses.size(), clause);
							feature.setSatMapping(clausesWithoutHidden);
							break;
						default:
							throw new AssertionError(selection);
						}
						workMonitor.invoke(feature);
					}
				}

				if (newLiterals) {
					openClauses.add(clause);
				}
			}
			return openClauses;
		}
	}

	public class GetSolutionsMethod implements LongRunningMethod<List<List<String>>> {
		private final int max;

		public GetSolutionsMethod(int max) {
			this.max = max;
		}

		@Override
		public List<List<String>> execute(IMonitor monitor) throws Exception {
			if (clauses == null) {
				return null;
			}
			final ArrayList<List<String>> resultList = new ArrayList<>();

			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(false, false);
			final List<LiteralSet> result = LongRunningWrapper.runMethod(new GetSolutionsAnalysis(solver, max), monitor);
			for (LiteralSet is : result) {
				resultList.add(clausesWithoutHidden.convertToString(is));
			}

			return resultList;
		}
	}

	public class IsValidMethod implements LongRunningMethod<Boolean> {
		@Override
		public Boolean execute(IMonitor monitor) throws Exception {
			if (clauses == null) {
				return false;
			}
			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(true, true);

			return new HasSolutionAnalysis(solver).analyze(monitor) != null;
		}
	}

	/**
	 * Ignores hidden features.
	 * Use this, when propgate is disabled (hidden features are not updated).
	 */
	public class IsValidNoHiddenMethod implements LongRunningMethod<Boolean> {
		@Override
		public Boolean execute(IMonitor monitor) throws Exception {
			if (clauses == null) {
				return false;
			}
			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(true, false);

			return new HasSolutionAnalysis(solver).analyze(monitor) != null;
		}
	}

	public class LoadMethod implements LongRunningMethod<Void> {
		@Override
		public Void execute(IMonitor monitor) {
			if (clauses != null) {
				return null;
			}
			final IFeatureModel featureModel = configuration.getFeatureModel();
			final Collection<IFeature> features = FeatureUtils.getFeatures(featureModel);

			final CNF orgSatInstance = new CNFCreator(featureModel).createNodes();

			if (configuration.ignoreAbstractFeatures) {
				clauses = orgSatInstance;
			} else {
				final List<String> list = Functional
						.toList(Functional.map(Functional.filter(features, new AbstractFeatureFilter()), FeatureUtils.GET_FEATURE_NAME));
				clauses = LongRunningWrapper.runMethod(new CNFSilcer(orgSatInstance, list));
			}

			final List<String> list = Functional.toList(Functional.map(Functional.filter(features, new HiddenFeatureFilter()), FeatureUtils.GET_FEATURE_NAME));
			clausesWithoutHidden = LongRunningWrapper.runMethod(new CNFSilcer(clauses, list));

			return null;
		}
	}

	public class UpdateMethod implements LongRunningMethod<List<String>> {
		private final boolean redundantManual;
		private final Object startFeatureName;

		public UpdateMethod(boolean redundantManual, Object startFeatureName) {
			this.redundantManual = redundantManual;
			this.startFeatureName = startFeatureName;
		}

		@Override
		public List<String> execute(IMonitor workMonitor) throws Exception {
			if (clauses == null) {
				return null;
			}
			workMonitor.setRemainingWork(configuration.features.size() + 3);

			configuration.resetAutomaticValues();

			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(false, true);
			final List<SelectableFeature> undefined = getUndefined();
			workMonitor.step();

			final int initialAssignmentLength = solver.getAssignment().size();
			solver.setSelectionStrategy(SelectionStrategy.POSITIVE);
			int[] model1 = solver.findSolution();
			workMonitor.step();

			if (model1 != null) {
				solver.setSelectionStrategy(SelectionStrategy.NEGATIVE);
				int[] model2 = solver.findSolution();

				final int[] model3 = new int[model1.length];
				for (SelectableFeature feature : undefined) {
					final int index = clauses.getVariable(feature.getName()) - 1;
					model3[index] = model1[index];
				}
				model1 = model3;

				final int[] redundantManualArray;
				final int[] manualArray;
				if (redundantManual) {
					redundantManualArray = new int[model1.length];
					manualArray = Arrays.copyOf(solver.getAssignment().toArray(), initialAssignmentLength);
					for (int i = 0; i < initialAssignmentLength; i++) {
						final int j = Math.abs(solver.getAssignment().get(i)) - 1;
						model1[j] = 0;
						redundantManualArray[j] = i;
					}
				} else {
					redundantManualArray = null;
					manualArray = null;
					for (int i = 0; i < initialAssignmentLength; i++) {
						final int j = Math.abs(solver.getAssignment().get(i)) - 1;
						model1[j] = 0;
					}
				}

				SatUtils.updateSolution(model1, model2);
				solver.setSelectionStrategy(SelectionStrategy.POSITIVE);
				workMonitor.step();

				ListIterator<SelectableFeature> it = configuration.features.listIterator();
				int index = -1;
				if (startFeatureName != null) {
					while (it.hasNext()) {
						final SelectableFeature feature = it.next();

						if (startFeatureName.equals(feature.getFeature().getName())) {
							it.previous();
							index = it.nextIndex();
							break;
						}
					}
				}

				if (index > 0) {
					compute(workMonitor, solver, model1, redundantManualArray, manualArray, configuration.features.subList(index, model1.length).iterator());
					compute(workMonitor, solver, model1, redundantManualArray, manualArray, configuration.features.subList(0, index).iterator());
				} else {
					compute(workMonitor, solver, model1, redundantManualArray, manualArray, configuration.features.iterator());
				}

			}
			return null;
		}

		private void compute(IMonitor workMonitor, final AdvancedSatSolver solver, int[] model1, final int[] redundantManualArray, final int[] manualArray,
				final Iterator<SelectableFeature> it) throws AssertionError {
			while (it.hasNext()) {
				final SelectableFeature feature = it.next();
				final int i = clauses.getVariable(feature.getFeature().getName()) - 1;
				if (redundantManual && redundantManualArray[i] != 0) {
					final int j = redundantManualArray[i];
					final int varX = manualArray[j];
					manualArray[j] = -varX;
					final SatResult hasSolution = solver.hasSolution(manualArray);
					manualArray[j] = varX;
					switch (hasSolution) {
					case FALSE:
						feature.setAutomatic(varX < 0 ? Selection.UNSELECTED : Selection.SELECTED);
						break;
					case TIMEOUT:
					case TRUE:
						break;
					default:
						throw new AssertionError(hasSolution);
					}
				} else {
					final int varX = model1[i];
					if (varX != 0) {
						solver.assignmentPush(-varX);
						final SatResult hasSolution = solver.hasSolution();
						switch (hasSolution) {
						case FALSE:
							solver.assignmentReplaceLast(varX);
							feature.setAutomatic(varX < 0 ? Selection.UNSELECTED : Selection.SELECTED);
							break;
						case TIMEOUT:
							solver.assignmentPop();
							break;
						case TRUE:
							solver.assignmentPop();
							SatUtils.updateSolution(model1, solver.getSolution());
							solver.setOrderShuffle();
							break;
						default:
							throw new AssertionError(hasSolution);
						}
					}
				}
				workMonitor.step(feature);
			}
		}

	}

	public static int FEATURE_LIMIT_FOR_DEFAULT_COMPLETION = 150;

	//	private static final int TIMEOUT = 1000;

	private final Configuration configuration;

	//	private SatMapping satMapping = null;
	private CNF clauses = null, clausesWithoutHidden = null;

	/**
	 * This method creates a clone of the given {@link ConfigurationPropagator}
	 * 
	 * @param configuration The configuration to clone
	 */
	ConfigurationPropagator(Configuration configuration) {
		this.configuration = configuration;
	}

	ConfigurationPropagator(ConfigurationPropagator propagator) {
		this(propagator, propagator.configuration);
	}

	ConfigurationPropagator(ConfigurationPropagator propagator, Configuration configuration) {
		this.configuration = configuration;
		if (propagator.isLoaded()) {
			this.clauses = propagator.clauses.clone();
			this.clausesWithoutHidden = propagator.clausesWithoutHidden.clone();
		}
	}

	@Override
	public LongRunningMethod<Boolean> canBeValid() {
		return new CanBeValidMethod();
	}

	public class CoverFeatures implements LongRunningMethod<List<List<String>>> {
		private final Collection<String> features;
		private final boolean selection;

		public CoverFeatures(Collection<String> features, boolean selection) {
			this.features = features;
			this.selection = selection;
		}

		@Override
		public List<List<String>> execute(IMonitor workMonitor) throws Exception {
			if (clauses == null) {
				return null;
			}
			final OneWiseConfigurationGenerator oneWiseConfigurationGenerator = new OneWiseConfigurationGenerator(
					getSolverForCurrentConfiguration(false, false));
			oneWiseConfigurationGenerator.setCoverMode(selection ? 1 : 0);
			int[] featureArray = new int[features.size()];
			int index = 0;
			for (String feature : features) {
				featureArray[index++] = clausesWithoutHidden.getVariable(feature);
			}
			oneWiseConfigurationGenerator.setFeatures(featureArray);

			final List<LiteralSet> solutions = LongRunningWrapper.runMethod(oneWiseConfigurationGenerator, workMonitor);
			final List<List<String>> solutionList = new ArrayList<>();
			for (LiteralSet is : solutions) {
				solutionList.add(clausesWithoutHidden.convertToString(is, true, false));
			}

			return solutionList;
		}

	}

	/**
	 * Creates solutions to cover the given features.
	 * 
	 * @param features The features that should be covered.
	 * @param selection true is the features should be selected, false otherwise.
	 * @throws Exception
	 */
	public CoverFeatures coverFeatures(final Collection<String> features, final boolean selection) {
		return new CoverFeatures(features, selection);
	}

	public FindOpenClauses findOpenClauses(List<SelectableFeature> featureList) {
		return new FindOpenClauses(featureList);
	}

	@Override
	public GetSolutionsMethod getSolutions(int max) throws TimeoutException {
		return new GetSolutionsMethod(max);
	}

	public boolean isLoaded() {
		return clauses != null;
	}

	@Override
	public LongRunningMethod<Boolean> isValid() {
		return new IsValidMethod();
	}

	/**
	 * Ignores hidden features.
	 * Use this, when propgate is disabled (hidden features are not updated).
	 */
	public LongRunningMethod<Boolean> isValidNoHidden() {
		return new IsValidNoHiddenMethod();
	}

	public LoadMethod load() {
		return new LoadMethod();
	}

	/**
	 * Counts the number of possible solutions.
	 * 
	 * @return a positive value equal to the number of solutions (if the method terminated in time)</br>
	 *         or a negative value (if a timeout occurred) that indicates that there are more solutions than the absolute value
	 */
	@Override
	public CountSolutionsMethod number(int timeout) {
		return new CountSolutionsMethod(timeout);
	}

	@Override
	public UpdateMethod update(boolean redundantManual, String startFeatureName) {
		return new UpdateMethod(redundantManual, startFeatureName);
	}

	ConfigurationPropagator clone(Configuration configuration) {
		return new ConfigurationPropagator(this, configuration);
	}

}
