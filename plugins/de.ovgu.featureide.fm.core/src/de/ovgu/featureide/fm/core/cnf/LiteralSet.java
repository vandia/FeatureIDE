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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

import javax.annotation.CheckForNull;

/**
 * Clause of a CNF.
 * 
 * @author Sebastian Krieter
 */
public class LiteralSet implements Cloneable, Serializable {

	private static final long serialVersionUID = 8948014814795787431L;

	protected final int[] literals;

	private final int hashCode;

	public LiteralSet(LiteralSet clause) {
		this.literals = Arrays.copyOf(clause.literals, clause.literals.length);
		hashCode = clause.hashCode;
	}

	public LiteralSet(int... literals) {
		this.literals = literals;
		Arrays.sort(this.literals);

		hashCode = 0; //Arrays.hashCode(literals);
	}

	public int[] getLiterals() {
		return literals;
	}

	public boolean containsLiteral(int literal) {
		for (int curLiteral : literals) {
			if (curLiteral == literal) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(int variable) {
		for (int curLiteral : literals) {
			if (Math.abs(curLiteral) == variable) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		return Arrays.equals(literals, ((LiteralSet) obj).literals);
	}

	@Override
	public String toString() {
		return "Clause [literals=" + Arrays.toString(literals) + "]";
	}

	@Override
	public LiteralSet clone() {
		return new LiteralSet(this);
	}

	public LiteralSet removeAll(LiteralSet variables) {
		final boolean[] removeMarker = new boolean[literals.length];
		final int count = getDuplicates(variables, removeMarker);

		final int[] newLiterals = new int[literals.length - count];
		int j = 0;
		for (int i = 0; i < literals.length; i++) {
			if (!removeMarker[i]) {
				newLiterals[j++] = literals[i];
			}
		}
		return new LiteralSet(newLiterals);
	}

	public LiteralSet retainAll(LiteralSet variables) {
		final boolean[] removeMarker = new boolean[literals.length];
		final int count = getDuplicates(variables, removeMarker);

		final int[] newLiterals = new int[count];
		int j = 0;
		for (int i = 0; i < literals.length; i++) {
			if (removeMarker[i]) {
				newLiterals[j++] = literals[i];
			}
		}
		return new LiteralSet(newLiterals);
	}

	private int getDuplicates(LiteralSet variables, final boolean[] removeMarker) {
		final int[] otherLiterals = variables.getLiterals();
		int count = 0;
		for (int i = 0; i < otherLiterals.length; i++) {
			final int otherLiteral = otherLiterals[i];
			for (int j = 0; j < literals.length; j++) {
				if (literals[j] == otherLiteral) {
					count++;
					removeMarker[j] = true;
				}
			}
		}
		return count;
	}

	public LiteralSet flip() {
		return new LiteralSet(SatUtils.negateSolution(literals));
	}

	public static int[] cleanLiteralArray(int[] newLiterals, byte[] helper) {
		int uniqueVarCount = newLiterals.length;
		for (int i = 0; i < newLiterals.length; i++) {
			final int literal = newLiterals[i];

			final int index = Math.abs(literal);
			final byte signum = (byte) Math.signum(literal);

			final int h = helper[index];
			switch (h) {
			case 0:
				helper[index] = signum;
				break;
			case 2:
				helper[index] = signum;
				break;
			case -1:
			case 1:
				if (h == signum) {
					newLiterals[i] = 0;
					uniqueVarCount--;
					break;
				} else {
					// reset
					for (int j = 0; j < i; j++) {
						helper[Math.abs(newLiterals[j])] = 0;
					}
					return null;
				}
			default:
				assert false;
				break;
			}
		}
		if (uniqueVarCount == newLiterals.length) {
			for (int i = 0; i < newLiterals.length; i++) {
				final int literal = newLiterals[i];
				helper[Math.abs(literal)] = 0;
			}
			return newLiterals;
		} else {
			final int[] uniqueVarArray = new int[uniqueVarCount];
			int k = 0;
			for (int i = 0; i < newLiterals.length; i++) {
				final int literal = newLiterals[i];
				helper[Math.abs(literal)] = 0;
				if (literal != 0) {
					uniqueVarArray[k++] = literal;
				}
			}
			return uniqueVarArray;
		}
	}

	/**
	 * Constructs a new array of literals that contains no duplicates and unwanted literals.
	 * Also checks whether the array contains a literal and its negation.
	 * 
	 * @param literalArray The initial literal array.
	 * @param unwantedVariables An array of variables that should be removed.
	 * @return A new literal array or {@code null}, if the initial set contained a literal and its negation.
	 * 
	 * @see #cleanLiteralSet(LiteralSet, int...)
	 */
	@CheckForNull
	public static int[] cleanLiteralArray(int[] literalArray, int... unwantedVariables) {
		final HashSet<Integer> newLiteralSet = new HashSet<>(literalArray.length << 1);

		outer: for (int literal : literalArray) {
			for (int i = 0; i < unwantedVariables.length; i++) {
				if (unwantedVariables[i] == Math.abs(literal)) {
					continue outer;
				}
			}
			if (newLiteralSet.contains(-literal)) {
				return null;
			} else {
				newLiteralSet.add(literal);
			}
		}

		int[] uniqueVarArray = new int[newLiteralSet.size()];
		int i = 0;
		for (int lit : newLiteralSet) {
			uniqueVarArray[i++] = lit;
		}
		return uniqueVarArray;
	}

	/**
	 * Constructs a new {@link LiteralSet} that contains no duplicates and unwanted literals.
	 * Also checks whether the set contains a literal and its negation.
	 * 
	 * @param literalSet The initial literal set.
	 * @param unwantedVariables An array of variables that should be removed.
	 * @return A new literal set or {@code null}, if the initial set contained a literal and its negation.
	 * 
	 * @see #cleanLiteralArray(int[], int...)
	 */
	@CheckForNull
	public static LiteralSet cleanLiteralSet(LiteralSet literalSet, int... unwantedVariables) {
		final HashSet<Integer> newLiteralSet = new HashSet<>(literalSet.getLiterals().length << 1);

		for (int literal : literalSet.getLiterals()) {
			if (newLiteralSet.contains(-literal)) {
				return null;
			} else {
				newLiteralSet.add(literal);
			}
		}
		
		for (int i = 0; i < unwantedVariables.length; i++) {
			final int unwantedVariable = unwantedVariables[i];
			newLiteralSet.remove(unwantedVariable);
			newLiteralSet.remove(-unwantedVariable);
		}

		int[] uniqueVarArray = new int[newLiteralSet.size()];
		int i = 0;
		for (int lit : newLiteralSet) {
			uniqueVarArray[i++] = lit;
		}
		return new LiteralSet(uniqueVarArray);
	}

}
