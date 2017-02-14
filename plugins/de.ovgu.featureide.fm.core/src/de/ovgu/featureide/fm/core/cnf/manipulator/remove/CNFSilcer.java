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
package de.ovgu.featureide.fm.core.cnf.manipulator.remove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sat4j.specs.TimeoutException;

import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.ClauseLengthComparatorDsc;
import de.ovgu.featureide.fm.core.cnf.Variables;
import de.ovgu.featureide.fm.core.cnf.manipulator.AbstractManipulator;
import de.ovgu.featureide.fm.core.cnf.manipulator.remove.heuristic.AFeatureOrderHeuristic;
import de.ovgu.featureide.fm.core.cnf.manipulator.remove.heuristic.MinimumClauseHeuristic;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.SimpleSatSolver;
import de.ovgu.featureide.fm.core.editing.cnf.UnkownLiteralException;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Removes features from a model while retaining dependencies of all other feature.
 * 
 * @author Sebastian Krieter
 */
public class CNFSilcer extends AbstractManipulator {

	protected static final Comparator<LiteralSet> lengthComparator = new ClauseLengthComparatorDsc();

	protected final CNF cnfCopy;

	//	protected final boolean includeBooleanValues;
	//	protected final boolean regularCNF;

	protected final List<DeprecatedClause> newDirtyClauseList = new ArrayList<>();
	protected final List<DeprecatedClause> newCleanClauseList = new ArrayList<>();
	protected final List<DeprecatedClause> dirtyClauseList = new ArrayList<>();
	protected final ArrayList<LiteralSet> cleanClauseList = new ArrayList<>();
	protected final Set<DeprecatedClause> dirtyClauseSet = new HashSet<>();
	protected final Set<DeprecatedClause> cleanClauseSet = new HashSet<>();

	//	protected final Collection<String> cleanFeatures = new HashSet<>();
	protected final Iterable<String> dirtyFeatures;
	private int numberOfDirtyFeatures = 0;

	//	protected Map<Object, Integer> idMap;
	//	protected String[] featureNameArray;

	protected int[] helper;
	protected DeprecatedFeature[] map;
	protected AFeatureOrderHeuristic heuristic;
	private ISimpleSatSolver newSolver;

	private boolean first = false;

	protected int globalMixedClauseCount = 0;

	protected int dirtyListPosIndex = 0;
	protected int dirtyListNegIndex = 0;
	protected int newDirtyListDelIndex = 0;

	public CNFSilcer(CNF orgCNF, Iterable<String> dirtyFeatures) {
		super(orgCNF);
		this.dirtyFeatures = dirtyFeatures;
		this.cnfCopy = new CNF(orgCNF, false);
	}

	//	public FeatureRemover(SatInstance2 cnf, Collection<String> dirtyFeatures, boolean includeBooleanValues) {
	//		this(cnf, dirtyFeatures, includeBooleanValues, false);
	//	}

	//	public FeatureRemover(SatInstance2 cnf, Collection<String> dirtyFeatures, boolean includeBooleanValues, boolean regularCNF) {
	//		this.fmNode = cnf;
	//		this.dirtyFeatures = dirtyFeatures;
	//		this.includeBooleanValues = includeBooleanValues;
	//		this.regularCNF = regularCNF;
	//	}

	//	public final Node createNewClauseList(Collection<? extends Clause> clauses) {
	//		final int newClauseSize = clauses.size();
	//		final Node[] newClauses;
	//		if (includeBooleanValues) {
	//			newClauses = new Node[newClauseSize + 3];
	//
	//			// Create clause that contains all clean features
	//			final Node[] allLiterals = new Node[cleanFeatures.size() + 1];
	//			int i = 0;
	//			for (String featureName : cleanFeatures) {
	//				allLiterals[i++] = new Literal(featureName);
	//			}
	//			allLiterals[i] = new Literal(NodeCreator.varTrue);
	//
	//			newClauses[newClauseSize] = new Or(allLiterals);
	//			if (regularCNF) {
	//				newClauses[newClauseSize + 1] = new Or(new Literal(NodeCreator.varTrue, true));
	//				newClauses[newClauseSize + 2] = new Or(new Literal(NodeCreator.varFalse, false));
	//			} else {
	//				newClauses[newClauseSize + 1] = new Literal(NodeCreator.varTrue, true);
	//				newClauses[newClauseSize + 2] = new Literal(NodeCreator.varFalse, false);
	//			}
	//		} else {
	//			newClauses = new Node[newClauseSize];
	//		}
	//		int j = 0;
	//		for (Clause newClause : clauses) {
	//			final int[] newClauseLiterals = newClause.getLiterals();
	//			final Literal[] literals = new Literal[newClauseLiterals.length];
	//			int i = literals.length;
	//			for (int k = 0; k < literals.length; k++) {
	//				final int child = newClauseLiterals[k];
	//				literals[--i] = new Literal(featureNameArray[Math.abs(child)], child > 0);
	//			}
	//			newClauses[j++] = new Or(literals);
	//		}
	//		return new And(newClauses);
	//	}

	protected CNF manipulate(IMonitor workMonitor) throws TimeoutException, UnkownLiteralException {
		// Collect all features in the prop node and remove TRUE and FALSE
		init();

		final String[] variableObjects = Arrays.copyOf(orgCNF.getNames(), orgCNF.size());
		map = new DeprecatedFeature[orgCNF.size() + 1];
		numberOfDirtyFeatures = 0;
		for (String curFeature : dirtyFeatures) {
			final int id = orgCNF.getVariable(curFeature);
			if (id != 0) {
				map[id] = new DeprecatedFeature(curFeature, id);
				variableObjects[id] = null;
				numberOfDirtyFeatures++;
			}
		}
		helper = new int[map.length];

		final ArrayList<String> slicedFeatureList = new ArrayList<>(variableObjects.length - numberOfDirtyFeatures);
		for (String object : variableObjects) {
			if (object != null) {
				slicedFeatureList.add(object);
			}
		}
		final Variables mapping = new Variables(slicedFeatureList);

		// Initialize lists and sets
		createClauseLists();

		prepareHeuristics();

		while (heuristic.hasNext()) {
			workMonitor.checkCancel();
			final DeprecatedFeature nextFeature = heuristic.next();
			if (nextFeature == null) {
				break;
			}

			// Remove redundant dirty clauses
			firstRedundancyCheck(nextFeature);

			// Partition dirty list into clauses that contain the current variable and clauses that don't
			partitionDirtyList(nextFeature);

			// Remove variable & create transitive clauses
			resolution(nextFeature);

			// Remove redundant clauses
			detectRedundancy(nextFeature);

			// Merge new dirty list into the old list
			updateLists();

			// If ALL dirty clauses exclusively consists of dirty features, they can just be removed without applying resolution
			if (globalMixedClauseCount == 0) {
				break;
			}
		}

		addCleanClauses();

		release();

		for (LiteralSet clause : cleanClauseList) {
			final int[] literals = clause.getLiterals();
			for (int i = 0; i < literals.length; i++) {
				final int l = literals[i];
				final int variable = mapping.getVariable(orgCNF.getName(l));
				literals[i] = l > 0 ? variable : -variable;
			}
		}

		return new CNF(mapping, cleanClauseList);
	}

	private void addNewClause(final DeprecatedClause curClause) {
		if (curClause != null) {
			if (curClause.computeRelevance(map)) {
				globalMixedClauseCount++;
			}
			if (curClause.getRelevance() == 0) {
				if (cleanClauseSet.add(curClause)) {
					newCleanClauseList.add(curClause);
				} else {
					deleteClause(curClause);
				}
			} else {
				if (dirtyClauseSet.add(curClause)) {
					newDirtyClauseList.add(curClause);
				} else {
					deleteClause(curClause);
				}
			}
		}
	}

	//	private int[] convert(Literal[] newChildren) {
	//		final int[] literals = new int[newChildren.length];
	//		for (int j = 0; j < newChildren.length; j++) {
	//			final Literal child = newChildren[j];
	//			literals[j] = fmNode.getVariable(child.var.toString());
	//		}
	//		return literals;
	//	}

	private void createClauseLists() {
		for (LiteralSet clause : orgCNF.getClauses()) {
			addNewClause(new DeprecatedClause(clause.getLiterals()));
		}

		cleanClauseList.ensureCapacity(cleanClauseList.size() + newCleanClauseList.size());
		for (DeprecatedClause deprecatedClause : newCleanClauseList) {
			cleanClauseList.add(new LiteralSet(deprecatedClause));
		}
		dirtyClauseList.addAll(newDirtyClauseList);
		newDirtyClauseList.clear();
		newCleanClauseList.clear();

		dirtyListPosIndex = dirtyClauseList.size();
		dirtyListNegIndex = dirtyClauseList.size();
	}

	protected final void deleteClause(final DeprecatedClause curClause) {
		if (curClause.delete(map)) {
			globalMixedClauseCount--;
		}
	}

	protected final void deleteOldDirtyClauses() {
		if (dirtyListPosIndex < dirtyClauseList.size()) {
			final List<DeprecatedClause> subList = dirtyClauseList.subList(dirtyListPosIndex, dirtyClauseList.size());
			dirtyClauseSet.removeAll(subList);
			for (DeprecatedClause deprecatedClause : subList) {
				deleteClause(deprecatedClause);
			}
			subList.clear();
		}
	}

	protected final void deleteNewDirtyClauses() {
		if (newDirtyListDelIndex < newDirtyClauseList.size()) {
			final List<DeprecatedClause> subList = newDirtyClauseList.subList(newDirtyListDelIndex, newDirtyClauseList.size());
			dirtyClauseSet.removeAll(subList);
			for (DeprecatedClause deprecatedClause : subList) {
				deleteClause(deprecatedClause);
			}
		}
	}

	//	private DeprecatedClause getClause(Node andChild) {
	//		int absoluteValueCount = 0;
	//		boolean valid = true;
	//
	//		final Literal[] children = Arrays.copyOf(andChild.getChildren(), andChild.getChildren().length, Literal[].class);
	//		for (int j = 0; j < children.length; j++) {
	//			final Literal literal = children[j];
	//
	//			// sort out obvious tautologies
	//			if (literal.var.equals(NodeCreator.varTrue)) {
	//				if (literal.positive) {
	//					valid = false;
	//				} else {
	//					absoluteValueCount++;
	//					children[j] = null;
	//				}
	//			} else if (literal.var.equals(NodeCreator.varFalse)) {
	//				if (literal.positive) {
	//					absoluteValueCount++;
	//					children[j] = null;
	//				} else {
	//					valid = false;
	//				}
	//			}
	//		}
	//
	//		if (valid) {
	//			if (absoluteValueCount > 0) {
	//				if (children.length == absoluteValueCount) {
	//					throw new RuntimeException("Model is void!");
	//				}
	//				Literal[] newChildren = new Literal[children.length - absoluteValueCount];
	//				int k = 0;
	//				for (int j = 0; j < children.length; j++) {
	//					final Literal literal = children[j];
	//					if (literal != null) {
	//						newChildren[k++] = literal;
	//					}
	//				}
	//				return DeprecatedClause.createClause(convert(newChildren));
	//			} else {
	//				return DeprecatedClause.createClause(convert(children));
	//			}
	//		} else {
	//			return null;
	//		}
	//	}

	private void init() {
		release();
		cleanClauseList.clear();
	}

	private void resolution(DeprecatedFeature nextFeature) {
		final int curFeatureID = nextFeature.getId();
		for (int i = dirtyListPosIndex; i < dirtyListNegIndex; i++) {
			final int[] posOrChildren = dirtyClauseList.get(i).getLiterals();
			for (int j = dirtyListNegIndex; j < dirtyClauseList.size(); j++) {
				final int[] negOrChildren = dirtyClauseList.get(j).getLiterals();
				final int[] newChildren = new int[posOrChildren.length + negOrChildren.length];

				System.arraycopy(posOrChildren, 0, newChildren, 0, posOrChildren.length);
				System.arraycopy(negOrChildren, 0, newChildren, posOrChildren.length, negOrChildren.length);

				addNewClause(DeprecatedClause.createClause(newChildren, curFeatureID, helper));
			}
		}
		newDirtyListDelIndex = newDirtyClauseList.size();
	}

	private void partitionDirtyList(DeprecatedFeature nextFeature) {
		final int curFeatureID = nextFeature.getId();
		for (int i = 0; i < dirtyListNegIndex; i++) {
			final LiteralSet clause = dirtyClauseList.get(i);
			for (int literal : clause.getLiterals()) {
				if (literal == -curFeatureID) {
					Collections.swap(dirtyClauseList, i--, --dirtyListNegIndex);
					break;
				}
			}
		}
		dirtyListPosIndex = dirtyListNegIndex;
		for (int i = 0; i < dirtyListPosIndex; i++) {
			final LiteralSet clause = dirtyClauseList.get(i);
			for (int literal : clause.getLiterals()) {
				if (literal == curFeatureID) {
					Collections.swap(dirtyClauseList, i--, --dirtyListPosIndex);
					break;
				}
			}
		}
	}

	private void updateLists() {
		// delete old & redundant dirty clauses
		deleteOldDirtyClauses();

		// delete new & redundant dirty clauses
		deleteNewDirtyClauses();

		dirtyClauseList.addAll(newDirtyClauseList.subList(0, newDirtyListDelIndex));
		newDirtyClauseList.clear();

		dirtyListPosIndex = dirtyClauseList.size();
		dirtyListNegIndex = dirtyClauseList.size();
		newDirtyListDelIndex = 0;
	}

	protected final boolean isRedundant(ISimpleSatSolver solver, LiteralSet curClause) {
		final int[] literals = curClause.getLiterals();
		final int[] literals2 = new int[literals.length];
		for (int i = 0; i < literals.length; i++) {
			literals2[i] = -literals[i];
		}
		switch (solver.hasSolution(literals2)) {
		case FALSE:
			return true;
		case TIMEOUT:
		case TRUE:
			return false;
		default:
			assert false;
			return false;
		}
	}

	protected void detectRedundancy(DeprecatedFeature nextFeature) {
		if (nextFeature.getClauseCount() > 0) {
			addCleanClauses();

			final ISimpleSatSolver solver = new SimpleSatSolver(cnfCopy);
			solver.addClauses(cleanClauseList);
			solver.addClauses(dirtyClauseList.subList(0, dirtyListPosIndex));

			Collections.sort(newDirtyClauseList.subList(0, newDirtyListDelIndex), lengthComparator);
			for (int i = newDirtyListDelIndex - 1; i >= 0; --i) {
				final DeprecatedClause curClause = newDirtyClauseList.get(i);
				if (isRedundant(solver, curClause)) {
					Collections.swap(newDirtyClauseList, i, --newDirtyListDelIndex);
				} else {
					solver.addClause(curClause);
				}
			}
		}
	}

	protected void addCleanClauses() {
		Collections.sort(newCleanClauseList, lengthComparator);

		for (int i = newCleanClauseList.size() - 1; i >= 0; --i) {
			final DeprecatedClause clause = newCleanClauseList.get(i);
			if (isRedundant(newSolver, clause)) {
				deleteClause(clause);
			} else {
				newSolver.addClause(clause);
				cleanClauseList.add(new LiteralSet(clause));
			}
		}
		newCleanClauseList.clear();
	}

	protected void firstRedundancyCheck(DeprecatedFeature nextFeature) {
		if (first && nextFeature.getClauseCount() > 0) {
			first = false;
			Collections.sort(dirtyClauseList, lengthComparator);

			addCleanClauses();

			final ISimpleSatSolver solver = new SimpleSatSolver(cnfCopy);
			solver.addClauses(cleanClauseList);

			// SAT Relevant
			for (int i = dirtyListPosIndex - 1; i >= 0; --i) {
				final DeprecatedClause mainClause = dirtyClauseList.get(i);
				if (isRedundant(solver, mainClause)) {
					Collections.swap(dirtyClauseList, i, --dirtyListPosIndex);
				} else {
					solver.addClause(mainClause);
				}
			}
			deleteOldDirtyClauses();

			dirtyListPosIndex = dirtyClauseList.size();
			dirtyListNegIndex = dirtyClauseList.size();
		}
	}

	protected void prepareHeuristics() {
		heuristic = new MinimumClauseHeuristic(map, numberOfDirtyFeatures);
		first = true;
		newSolver = new SimpleSatSolver(cnfCopy);
		newSolver.addClauses(cleanClauseList);
	}

	protected void release() {
		newDirtyClauseList.clear();
		newCleanClauseList.clear();
		dirtyClauseSet.clear();
		cleanClauseSet.clear();
		dirtyClauseList.clear();

		if (newSolver != null) {
			newSolver.reset();
		}
	}

}
