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
package de.ovgu.featureide.fm.core.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.prop4j.Equals;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.Not;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;

import de.ovgu.featureide.fm.core.ConstraintAttribute;
import de.ovgu.featureide.fm.core.FeatureDependencies;
import de.ovgu.featureide.fm.core.FeatureStatus;
import de.ovgu.featureide.fm.core.Logger;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.base.util.Functional;
import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.CNFCreator;
import de.ovgu.featureide.fm.core.cnf.CNFCreator.ModelType;
import de.ovgu.featureide.fm.core.cnf.analysis.AnalysisResult;
import de.ovgu.featureide.fm.core.cnf.analysis.CoreDeadAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.HasSolutionAnalysis;
import de.ovgu.featureide.fm.core.cnf.analysis.RedundancyAnalysis;
import de.ovgu.featureide.fm.core.cnf.IVariables;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.Nodes;
import de.ovgu.featureide.fm.core.cnf.SatUtils;
import de.ovgu.featureide.fm.core.cnf.solver.AdvancedSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver.SatResult;
import de.ovgu.featureide.fm.core.cnf.solver.ModifiableSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.RuntimeContradictionException;
import de.ovgu.featureide.fm.core.explanations.DeadFeature;
import de.ovgu.featureide.fm.core.explanations.FalseOptionalFeature;
import de.ovgu.featureide.fm.core.explanations.RedundantConstraint;
import de.ovgu.featureide.fm.core.filter.HiddenFeatureFilter;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.fm.core.job.monitor.NullMonitor;

/**
 * A collection of methods for working with {@link IFeatureModel} will replace
 * the corresponding methods in {@link IFeatureModel}
 * 
 * @author Sebastian Krieter
 */
public class CoreDeadAnalysis implements LongRunningMethod<List<IFeature>> {
	

	private IFeatureModel fm;

	private void checkFeatureDead(final CNF cnf) {

	}

	@Override
	public List<IFeature> execute(IMonitor monitor) throws Exception {
		final LiteralSet solution2 = LongRunningWrapper.runMethod(new CoreDeadAnalysis(cnf), monitor.subTask(0));
		monitor.checkCancel();
		for (int var : solution2.getLiterals()) {
			
			monitor.checkCancel();
			final IFeature feature = fm.getFeature(cnf.getName(var));
			if (var < 0) {
				List<IFeature> deadFeatures;
				deadFeatures.add(feature);

//				if (calculateExplanations) {
//					// explain dead features and remember explanation in map
//					DeadFeature deadF = new DeadFeature();
//					List<String> expl = deadF.explain(fm, feature, false);
//					deadFeatureExpl.put(feature, expl);
//
//				} else {
					coreFeatures.add(feature);
//				}
			}
		}
	}


}
