/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
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

/**
 * 
 * @author Sebastian Krieter
 */
public class DefaultInternalVariables implements IInternalVariables {

	public boolean checkClause(LiteralSet orgClause) {
		return true;
	}

	public LiteralSet convertToInternal(LiteralSet orgClause) {
		return orgClause;
	}

	public int[] convertToInternal(int[] orgLiterals) {
		return orgLiterals;
	}

	public int convertToInternal(int orgLiteral) {
		return orgLiteral;
	}

	public LiteralSet convertToOriginal(LiteralSet internalClause) {
		return internalClause;
	}

	public int[] convertToOriginal(int[] internalLiterals) {
		return internalLiterals;
	}

	public int convertToOriginal(int internalLiteral) {
		return internalLiteral;
	}

	@Override
	public int getNumberOfVariables() {
		return 0;
	}

}
