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
package de.ovgu.featureide.fm.core.cnf.analysis;

import java.util.HashMap;

import de.ovgu.featureide.fm.core.cnf.CNF;

/**
 * 
 * @author Sebastian Krieter
 */
public class CNFAnalysis {

	private CNF cnf;

	private HashMap<AnalysisResult<?>, AnalysisResult<?>> map;

	public CNF getCnf() {
		return cnf;
	}

	public void setCnf(CNF cnf) {
		this.cnf = cnf;
	}

	public AnalysisResult<?> getResult(AnalysisResult<?> result) {
		return map.get(result);
	}

	public boolean hasResult(AnalysisResult<?> result) {
		return map.containsKey(result);
	}

	public AnalysisResult<?> addResult(AnalysisResult<?> result) {
		final AnalysisResult<?> existingResult = map.get(result);
		if (existingResult == null) {
			map.put(result, result);
			return result;
		} else {
			return existingResult;
		}
	}
}
