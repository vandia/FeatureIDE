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

/**
 * Clause of a CNF.
 * 
 * @author Sebastian Krieter
 */
public class Clause implements Cloneable, Serializable {

	private static final long serialVersionUID = 8948014814795787431L;

	protected final int[] literals;

	private final int hashCode;

	public Clause(Clause clause) {
		this.literals = Arrays.copyOf(clause.literals, clause.literals.length);
		hashCode = clause.hashCode;
	}

	public Clause(int... literals) {
		this.literals = literals;
		Arrays.sort(this.literals);

		hashCode = Arrays.hashCode(literals);
	}

	public int[] getLiterals() {
		return literals;
	}

	public boolean contains(int literalID) {
		for (int curLiteralID : literals) {
			if (Math.abs(curLiteralID) == literalID) {
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
		return Arrays.equals(literals, ((Clause) obj).literals);
	}

	@Override
	public String toString() {
		return "Clause [literals=" + Arrays.toString(literals) + "]";
	}

	@Override
	public Clause clone() {
		return new Clause(this);
	}

	public Clause flip() {
		return new Clause(SatUtils.negateSolution(literals));
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

	public static int[] cleanLiteralArray(int[] newLiterals, int... unwantedLiterals) {
		final HashSet<Integer> literalSet = new HashSet<>(newLiterals.length << 1);

		outer: for (int literal : newLiterals) {
			for (int i = 0; i < unwantedLiterals.length; i++) {
				if (unwantedLiterals[i] == Math.abs(literal)) {
					continue outer;
				}
			}
			if (literalSet.contains(-literal)) {
				return null;
			} else {
				literalSet.add(literal);
			}
		}

		int[] uniqueVarArray = new int[literalSet.size()];
		int i = 0;
		for (int lit : literalSet) {
			uniqueVarArray[i++] = lit;
		}
		return uniqueVarArray;
	}

}
