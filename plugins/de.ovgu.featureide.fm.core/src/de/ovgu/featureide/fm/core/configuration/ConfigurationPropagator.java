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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.prop4j.Literal;
import org.prop4j.solver.BasicSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import de.ovgu.featureide.fm.core.FeatureProject;
import de.ovgu.featureide.fm.core.Logger;
import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.FeatureModelFormula;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.analysis.CountSolutionsAnalysis;
import de.ovgu.featureide.fm.core.cnf.generator.GetSolutionsAnalysis;
import de.ovgu.featureide.fm.core.cnf.generator.OneWiseConfigurationGenerator;
import de.ovgu.featureide.fm.core.cnf.solver.AdvancedSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver.SatResult;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Updates a configuration.
 * 
 * @author Sebastian Krieter
 */
public class ConfigurationPropagator implements IConfigurationPropagator {

	public class IsValidMethod implements LongRunningMethod<Boolean> {
		private final boolean deselectUndefinedFeatures;
		private final boolean includeHiddenFeatures;

		public IsValidMethod(boolean includeUndefinedFeatures, boolean includeHiddenFeatures) {
			this.deselectUndefinedFeatures = includeUndefinedFeatures;
			this.includeHiddenFeatures = includeHiddenFeatures;
		}

		@Override
		public Boolean execute(IMonitor monitor) {
			if (formula == null) {
				return false;
			}
			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(deselectUndefinedFeatures, includeHiddenFeatures);

			final SatResult satResult = solver.hasSolution();
			switch (satResult) {
			case FALSE:
			case TIMEOUT:
				return false;
			case TRUE:
				return true;
			default:
				throw new AssertionError(satResult);
			}
		}
	}

	public class Resolve implements LongRunningMethod<Void> {
		@Override
		public Void execute(IMonitor workMonitor) throws Exception {
			if (formula == null) {
				return null;
			}

			// Reset all automatic values
			configuration.resetAutomaticValues();
			
			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(false, true);
			
			final SatResult satResult = solver.hasSolution();
			switch (satResult) {
			case FALSE:
			case TIMEOUT:
				final int[] contradictoryAssignment = solver.getContradictoryAssignment();
				for (int i : contradictoryAssignment) {
					configuration.setManual(solver.getSatInstance().getName(i), Selection.UNDEFINED);
				}
			case TRUE:
				return null;
			default:
				throw new AssertionError(satResult);
			}
		}
	}

	public class CountSolutionsMethod implements LongRunningMethod<Long> {
		private final int timeout;

		public CountSolutionsMethod(int timeout) {
			this.timeout = timeout;
		}

		@Override
		public Long execute(IMonitor monitor) throws Exception {
			if (formula == null) {
				return 0L;
			}
			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(false, false);
			solver.setTimeout(timeout);
			return new CountSolutionsAnalysis(solver).analyze(monitor);
		}

	}

	public class FindOpenClauses implements LongRunningMethod<List<LiteralSet>> {

		private List<SelectableFeature> featureList;

		public FindOpenClauses(List<SelectableFeature> featureList) {
			this.featureList = featureList;
		}

		public List<LiteralSet> execute(IMonitor workMonitor) {
			if (formula == null) {
				return Collections.emptyList();
			}
			final CNF clausesWithoutHidden = formula.getClausesWithoutHidden();
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
					final SelectableFeature feature = configuration.getSelectableFeature(clausesWithoutHidden.getName(literal));
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

						final SelectableFeature feature = configuration.getSelectableFeature(clausesWithoutHidden.getName(literal));
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
			if (formula == null) {
				return null;
			}
			final ArrayList<List<String>> resultList = new ArrayList<>();

			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(false, false);
			final List<LiteralSet> result = new GetSolutionsAnalysis(solver, max).analyze(monitor);
			for (LiteralSet is : result) {
				resultList.add(solver.getSatInstance().convertToString(is));
			}

			return resultList;
		}
	}

	/**
	 * Creates solutions to cover the given features.
	 * 
	 * @param features The features that should be covered.
	 * @param selection true is the features should be selected, false otherwise.
	 */
	public class CoverFeatures implements LongRunningMethod<List<List<String>>> {
		private final Collection<String> features;
		private final boolean selection;

		public CoverFeatures(Collection<String> features, boolean selection) {
			this.features = features;
			this.selection = selection;
		}

		@Override
		public List<List<String>> execute(IMonitor workMonitor) throws Exception {
			if (formula == null) {
				return null;
			}
			final CNF clausesWithoutHidden = formula.getClausesWithoutHidden();
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

	public class UpdateMethod implements LongRunningMethod<Void> {
		private final boolean redundantManual;
		private final Object startFeatureName;

		public UpdateMethod(boolean redundantManual, Object startFeatureName) {
			this.redundantManual = redundantManual;
			this.startFeatureName = startFeatureName;
		}

		@Override
		public Void execute(IMonitor workMonitor) {
			if (formula == null) {
				return null;
			}
			workMonitor.setRemainingWork(configuration.features.size() + 3);

			configuration.resetAutomaticValues();

			final ArrayList<Literal> manualLiterals = new ArrayList<>();
			for (SelectableFeature feature : featureOrder) {
				if (feature.getManual() != Selection.UNDEFINED && (includeAbstractFeatures || feature.getFeature().getStructure().isConcrete())) {
					manualLiterals.add(new Literal(feature.getFeature().getName(), feature.getManual() == Selection.SELECTED));
				}
			}
			final HashSet<Literal> manualLiteralSet = new HashSet<>(manualLiterals);
			for (SelectableFeature feature : configuration.features) {
				if (feature.getManual() != Selection.UNDEFINED && (includeAbstractFeatures || feature.getFeature().getStructure().isConcrete())) {
					final Literal l = new Literal(feature.getFeature().getName(), feature.getManual() == Selection.SELECTED);
					if (manualLiteralSet.add(l)) {
						manualLiterals.add(l);
					}
				}
			}
			//			final AdvancedSatSolver solver = getSolverForCurrentConfiguration(false, true);

			workMonitor.setRemainingWork(manualLiterals.size() + 1);
			Collections.reverse(manualLiterals);

			final ConditionallyCoreDeadAnalysis analysis = new ConditionallyCoreDeadAnalysis(rootNode);
			final int[] intLiterals = rootNode.convertToInt(manualLiterals);
			analysis.setAssumptions(intLiterals);
			final int[] impliedFeatures = LongRunningWrapper.runMethod(analysis, workMonitor.subTask(1));

			// if there is a contradiction within the configuration
			if (impliedFeatures == null) {
				return null;
			}

			for (int i : impliedFeatures) {
				final SelectableFeature feature = configuration.getSelectableFeature((String) formula.getCNF().getName(i));
				configuration.setAutomatic(feature, i > 0 ? Selection.SELECTED : Selection.UNSELECTED);
				workMonitor.invoke(feature);
				manualLiteralSet.add(new Literal(feature.getFeature().getName(), feature.getManual() == Selection.SELECTED));
			}
			// only for update of configuration editor
			for (SelectableFeature feature : configuration.features) {
				if (!manualLiteralSet.contains(new Literal(feature.getFeature().getName(), feature.getManual() == Selection.SELECTED))) {
					workMonitor.invoke(feature);
				}
			}
			if (redundantManual) {
				final BasicSolver solver;
				try {
					solver = new BasicSolver(rootNode);
				} catch (ContradictionException e) {
					Logger.logError(e);
					return null;
				}

				for (int feature : intLiterals) {
					solver.assignmentPush(feature);
				}

				int literalCount = intLiterals.length;
				final IVecInt assignment = solver.getAssignment();
				for (int i = 0; i < assignment.size(); i++) {
					final int oLiteral = intLiterals[i];
					final SelectableFeature feature = configuration.getSelectableFeature((String) rootNode.getVariableObject(oLiteral));
					assignment.set(i, -oLiteral);
					final SatResult satResult = solver.isSatisfiable();
					switch (satResult) {
					case FALSE:
						configuration.setAutomatic(feature, oLiteral > 0 ? Selection.SELECTED : Selection.UNSELECTED);
						workMonitor.invoke(feature);
						intLiterals[i] = intLiterals[--literalCount];
						assignment.delete(i--);
						break;
					case TIMEOUT:
					case TRUE:
						assignment.set(i, oLiteral);
						workMonitor.invoke(feature);
						break;
					default:
						throw new AssertionError(satResult);
					}
					workMonitor.worked();
				}
			}
			return null;
		}

	}

	// TODO fix monitor values
	private final FeatureModelFormula formula;
	private final Configuration configuration;

//	private CNF clauses = null, clausesWithoutHidden = null;

	private boolean includeAbstractFeatures;

	public ConfigurationPropagator(FeatureModelFormula formula, Configuration configuration) {
		this.formula = formula;
		this.configuration = configuration;
	}

	/**
	 * @deprecated Use {@link #ConfigurationPropagator(FeatureModelFormula, Configuration)} instead and receive a {@link FeatureModelFormula} instance from a {@link FeatureProject}.
	 * @param configuration
	 */
	@Deprecated
	public ConfigurationPropagator(Configuration configuration) {
		this.formula = new FeatureModelFormula(new configuration);
		this.configuration = configuration;
	}

	private AdvancedSatSolver getSolverForCurrentConfiguration(boolean deselectUndefinedFeatures, boolean includeHiddenFeatures) {
		final AdvancedSatSolver solver = getSolver(includeHiddenFeatures);
		for (SelectableFeature feature : configuration.features) {
			if ((deselectUndefinedFeatures || feature.getSelection() != Selection.UNDEFINED)
					&& (includeAbstractFeatures || feature.getFeature().getStructure().isConcrete())
					&& (includeHiddenFeatures || !feature.getFeature().getStructure().hasHiddenParent())) {
				solver.assignmentPush(solver.getSatInstance().getVariable(feature.getFeature().getName(), feature.getSelection() == Selection.SELECTED));
			}
		}
		return solver;
	}
	
	private AdvancedSatSolver getSolver(boolean includeHiddenFeatures) {
		final CNF satInstance;
		if (includeAbstractFeatures) {
			if (includeHiddenFeatures) {
				satInstance = formula.getCNF();
			} else {
				satInstance = formula.getClausesWithoutHidden();
			}
		} else {
			if (includeHiddenFeatures) {
				satInstance = formula.getClausesWithoutAbstract();
			} else {
				satInstance = formula.getClausesWithoutAbstractAndHidden();
			}
		}
		return new AdvancedSatSolver(satInstance);
	}

	@Override
	public LongRunningMethod<Boolean> canBeValid() {
		return new IsValidMethod(false, true);
	}

	@Override
	public Resolve resolve() {
		return new Resolve();
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

	@Override
	public LongRunningMethod<Boolean> isValid() {
		return new IsValidMethod(true, true);
	}

	/**
	 * Ignores hidden features.
	 * Use this, when propgate is disabled (hidden features are not updated).
	 */
	public IsValidMethod isValidNoHidden() {
		return new IsValidMethod(true, false);
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

}
