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
package de.ovgu.featureide.fm.core.analysis.cnf;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.ovgu.featureide.fm.core.analysis.cnf.generator.ModalImplicationGraph;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.ModalImplicationGraphBuilder;
import de.ovgu.featureide.fm.core.analysis.cnf.manipulator.remove.CNFSlicer;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IModalImplicationGraph;
import de.ovgu.featureide.fm.core.editing.AdvancedNodeCreator;
import de.ovgu.featureide.fm.core.editing.AdvancedNodeCreator.CNFType;
import de.ovgu.featureide.fm.core.editing.AdvancedNodeCreator.ModelType;
import de.ovgu.featureide.fm.core.filter.AbstractFeatureFilter;
import de.ovgu.featureide.fm.core.filter.HiddenFeatureFilter;
import de.ovgu.featureide.fm.core.filter.base.IFilter;
import de.ovgu.featureide.fm.core.filter.base.OrFilter;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;

/**
 * 
 * @author Sebastian Krieter
 */
public class FeatureModelFormula {

	private static final int CNF_COMPLETE = 0;
	private static final int CNF_EMPTY = 1;
	private static final int CNF_NO_HIDDEN = 2;
	private static final int CNF_NO_ABSTRACT = 3;
	private static final int CNF_NO_HIDDEN_NO_ABSTRACT = 4;
	private static final int CNF_TREE_ONLY = 5;
	private static final int CNF_CONSTRAINTS_ONLY = 6;
	private static final int MIG = 7;

	private static final int numberOfCNFs = 7;

	private final CNF[] cnfs = new CNF[numberOfCNFs];
	private final Lock[] locks = new Lock[numberOfCNFs + 1];
	{
		for (int i = 0; i < locks.length; i++) {
			locks[i] = new ReentrantLock();
		}
	}

	private final IFeatureModel featureModel;

	private ModalImplicationGraph modalImplicationGraph;

	public FeatureModelFormula(IFeatureModel featureModel) {
		this.featureModel = featureModel.clone();
	}

	public IFeatureModel getFeatureModel() {
		return featureModel;
	}

	public Variables getVariables() {
		return new Variables(FeatureUtils.getFeatureNamesList(featureModel));
	}

	public CNF getCNF() {
		final Lock lock = locks[CNF_COMPLETE];
		lock.lock();
		try {
			CNF cnf = cnfs[CNF_COMPLETE];
			if (cnf == null) {
				cnf = new FeatureModelCNF(featureModel, false);
				cnf.addClauses(Nodes.convert(cnf.getVariables(), AdvancedNodeCreator.createRegularCNF(featureModel)));
				cnfs[CNF_COMPLETE] = cnf;
			}
			return cnf;
		} finally {
			lock.unlock();
		}
	}

	public CNF getSlicedCNF(int index) {
		final CNF completeCNF = getCNF();
		final Lock lock = locks[index];
		lock.lock();
		try {
			CNF slicedCNF = cnfs[index];
			if (slicedCNF == null) {
				final IFilter<IFeature> filter;
				switch (index) {
				case 0:
					filter = new AbstractFeatureFilter();
					break;
				case 1:
					filter = new HiddenFeatureFilter();
					break;
				case 2:
					filter = new OrFilter<IFeature>(Arrays.asList(new HiddenFeatureFilter(), new AbstractFeatureFilter()));
					break;
				default:
					return completeCNF;
				}
				final CNFSlicer slicer = new CNFSlicer(completeCNF, Functional.mapToList(featureModel.getFeatures(), filter, FeatureUtils.GET_FEATURE_NAME));
				slicedCNF = LongRunningWrapper.runMethod(slicer);
				cnfs[index] = slicedCNF;
			}
			return slicedCNF;
		} finally {
			lock.unlock();
		}
	}

	public void resetCNF() {
		for (int i = 0; i < cnfs.length; i++) {
			final Lock slicingLock = locks[i];
			slicingLock.lock();
			try {
				cnfs[i] = null;
			} finally {
				slicingLock.unlock();
			}
		}
	}

	//	// TODO use in FeatureProject
	//	@Override
	//	public void propertyChange(FeatureIDEEvent event) {
	//		switch (event.getEventType()) {
	//		// TODO !!!
	//		case MODEL_DATA_LOADED:
	//			resetCNF();
	//			break;
	//		default:
	//			break;
	//		}
	//	}

	public CNF getCNFWithoutAbstract() {
		return getSlicedCNF(CNF_NO_ABSTRACT);
	}

	public CNF getClausesWithoutHidden() {
		return getSlicedCNF(CNF_NO_HIDDEN);
	}

	public CNF getClausesWithoutAbstractAndHidden() {
		return getSlicedCNF(CNF_NO_HIDDEN_NO_ABSTRACT);
	}

	public CNF getFeatureTreeClauses() {
		final Lock lock = locks[CNF_TREE_ONLY];
		lock.lock();
		try {
			CNF cnf = cnfs[CNF_TREE_ONLY];
			if (cnf == null) {
				final AdvancedNodeCreator nodeCreator = new AdvancedNodeCreator(featureModel);
				nodeCreator.setModelType(ModelType.OnlyStructure);
				nodeCreator.setCnfType(CNFType.Regular);
				nodeCreator.setIncludeBooleanValues(false);
				cnf = new FeatureModelCNF(featureModel, false);
				cnf.addClauses(Nodes.convert(cnf.getVariables(), nodeCreator.createNodes()));
				cnfs[CNF_TREE_ONLY] = cnf;
			}
			return cnf;
		} finally {
			lock.unlock();
		}
	}

	public CNF getConstraintClauses() {
		final Lock lock = locks[CNF_CONSTRAINTS_ONLY];
		lock.lock();
		try {
			CNF cnf = cnfs[CNF_CONSTRAINTS_ONLY];
			if (cnf == null) {
				final AdvancedNodeCreator nodeCreator = new AdvancedNodeCreator(featureModel);
				nodeCreator.setModelType(ModelType.OnlyConstraints);
				nodeCreator.setCnfType(CNFType.Regular);
				nodeCreator.setIncludeBooleanValues(false);
				cnf = new FeatureModelCNF(featureModel, false);
				cnf.addClauses(Nodes.convert(cnf.getVariables(), nodeCreator.createNodes()));
				cnfs[CNF_CONSTRAINTS_ONLY] = cnf;
			}
			return cnf;
		} finally {
			lock.unlock();
		}
	}

	public CNF getEmptyCNF() {
		final Lock lock = locks[CNF_EMPTY];
		lock.lock();
		try {
			CNF cnf = cnfs[CNF_EMPTY];
			if (cnf == null) {
				cnf = new FeatureModelCNF(featureModel, false);
				cnfs[CNF_EMPTY] = cnf;
			}
			return cnf;
		} finally {
			lock.unlock();
		}
	}

	public IModalImplicationGraph getModalImplicationGraph() {
		final CNF completeCNF = getCNF();
		final Lock lock = locks[MIG];
		lock.lock();
		try {
			if (modalImplicationGraph == null) {
				modalImplicationGraph = LongRunningWrapper.runMethod(new ModalImplicationGraphBuilder(completeCNF, false));
			}
			return modalImplicationGraph;
		} finally {
			lock.unlock();
		}
	}

}
