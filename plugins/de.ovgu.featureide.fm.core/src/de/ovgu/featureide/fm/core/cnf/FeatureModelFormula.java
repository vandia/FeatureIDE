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
package de.ovgu.featureide.fm.core.cnf;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent;
import de.ovgu.featureide.fm.core.base.event.IEventListener;
import de.ovgu.featureide.fm.core.cnf.manipulator.remove.CNFSlicer;
import de.ovgu.featureide.fm.core.editing.AdvancedNodeCreator;
import de.ovgu.featureide.fm.core.filter.AbstractFeatureFilter;
import de.ovgu.featureide.fm.core.filter.HiddenFeatureFilter;
import de.ovgu.featureide.fm.core.filter.base.IFilter;
import de.ovgu.featureide.fm.core.filter.base.OrFilter;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.ovgu.featureide.fm.core.io.manager.IFileManager;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;

/**
 * 
 * @author Sebastian Krieter
 */
public class FeatureModelFormula implements IEventListener {

	private static final int numberOfSlicedCNFs = 3;

	private final CNF[] slicedCNFs = new CNF[numberOfSlicedCNFs];
	private final Lock[] slicingLocks = new Lock[3];
	{
		for (int i = 0; i < slicingLocks.length; i++) {
			slicingLocks[i] = new ReentrantLock();
		}
	}

	private final Lock cnfLock = new ReentrantLock();
	private final IFileManager<IFeatureModel> fmManager;

	private FeatureModelCNF cnf;

	public FeatureModelFormula(IFileManager<IFeatureModel> fmManager) {
		this.fmManager = fmManager;
		fmManager.addListener(this);
	}

	public FeatureModelCNF getCNF() {
		cnfLock.lock();
		try {
			if (cnf == null) {
				final IFeatureModel fm = fmManager.getObject();
				cnf = new FeatureModelCNF(fm, false);
				cnf.addClauses(Nodes.convert(cnf.getVariables(), AdvancedNodeCreator.createRegularCNF(fm)));
			}
			return cnf;
		} finally {
			cnfLock.unlock();
		}
	}

	public CNF getSlicedCNF(int index) {
		final FeatureModelCNF cnf2 = getCNF();
		final Lock slicingLock = slicingLocks[index];
		slicingLock.lock();
		try {
			CNF slicedCNF = slicedCNFs[index];
			if (slicedCNF == null) {
				final IFeatureModel fm = fmManager.getObject();
				final IFilter<IFeature> filter;
				switch(index) {
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
					return cnf2;
				}
				final CNFSlicer slicer = new CNFSlicer(cnf2, Functional.map(Functional.filter(fm.getFeatures(), filter), FeatureUtils.GET_FEATURE_NAME));
				slicedCNF = LongRunningWrapper.runMethod(slicer);
				slicedCNFs[index] = slicedCNF;
			}
			return slicedCNF;
		} finally {
			slicingLock.unlock();
		}
	}

	private void resetCNF() {
		cnfLock.lock();
		try {
			cnf = null;
		} finally {
			cnfLock.unlock();
		}
		for (int i = 0; i < slicedCNFs.length; i++) {
			final Lock slicingLock = slicingLocks[i];
			slicingLock.lock();
			try {
				slicedCNFs[i] = null;
			} finally {
				slicingLock.unlock();
			}
		}
	}

	@Override
	public void propertyChange(FeatureIDEEvent event) {
		switch (event.getEventType()) {
		// TODO !!!
		case MODEL_DATA_LOADED:
			resetCNF();
			break;
		default:
			break;
		}
	}

	public CNF getClausesWithoutAbstract() {
		return getSlicedCNF(0);
	}

	public CNF getClausesWithoutHidden() {
		return getSlicedCNF(1);
	}

	public CNF getClausesWithoutAbstractAndHidden() {
		return getSlicedCNF(2);
	}

	//		public void load(IMonitor monitor) {
	//			if (clauses != null) {
	//				return null;
	//			}
	//			final IFeatureModel featureModel = configuration.getFeatureModel();
	//			final Collection<IFeature> features = FeatureUtils.getFeatures(featureModel);
	//
	//			final SatInstance2 orgSatInstance = new SimpleClauseCreator(featureModel).createNodes();
	//
	//			if (configuration.ignoreAbstractFeatures) {
	//				clauses = orgSatInstance;
	//			} else {
	//				filter1 = new OrFilter<>(Arrays.asList(new HiddenFeatureFilter(), new AbstractFeatureFilter()));
	//				filter2 = new AbstractFeatureFilter();
	//				nodeCreator1 = new AdvancedNodeCreator(featureModel, filter1);
	//				nodeCreator2 = new AdvancedNodeCreator(featureModel, filter2);
	//			}
	//			nodeCreator1.setCnfType(AdvancedNodeCreator.CNFType.Regular);
	//			nodeCreator2.setCnfType(AdvancedNodeCreator.CNFType.Regular);
	//			nodeCreator1.setIncludeBooleanValues(false);
	//			nodeCreator2.setIncludeBooleanValues(false);
	//
	//			final IRunner<Node> buildThread1 = LongRunningWrapper.getThread(nodeCreator1);
	//			final IRunner<Node> buildThread2 = LongRunningWrapper.getThread(nodeCreator2);
	//
	//			buildThread1.schedule();
	//			buildThread2.schedule();
	//
	//			try {
	//				buildThread2.join();
	//				buildThread1.join();
	//			} catch (InterruptedException e) {
	//				Logger.logError(e);
	//				final List<String> list = Functional
	//						.toList(Functional.map(Functional.filter(features, new AbstractFeatureFilter()), FeatureUtils.GET_FEATURE_NAME));
	//				clauses = LongRunningWrapper.runMethod(new SatSilcer(orgSatInstance, list));
	//	}
	//	final List<String> list = Functional.toList(Functional.map(Functional.filter(features, new HiddenFeatureFilter()), FeatureUtils.GET_FEATURE_NAME));
	//	clausesWithoutHidden = LongRunningWrapper.runMethod(new SatSilcer(clauses, list));
	//
	//	return null;
	//}
	//}

}
