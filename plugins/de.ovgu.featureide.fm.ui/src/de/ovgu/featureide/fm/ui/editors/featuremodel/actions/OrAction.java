/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2011  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.featuremodel.actions;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.ui.PlatformUI;

import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.featuremodel.operations.FeatureChangeGroupTypeOperation;

/**
 * Turns a group type into an Or-group.
 * 
 * @author Thomas Thuem
 */
public class OrAction extends SingleSelectionAction {

	public static String ID = "de.ovgu.featureide.or";

	private final FeatureModel featureModel;

	public OrAction(GraphicalViewerImpl viewer, FeatureModel featureModel) {
		super("Or", viewer);
		this.featureModel = featureModel;
	}

	@Override
	public void run() {
		FeatureChangeGroupTypeOperation op = new FeatureChangeGroupTypeOperation(
				FeatureChangeGroupTypeOperation.OR, feature, featureModel);
		op.addContext((IUndoContext) featureModel.getUndoContext());

		try {
			PlatformUI.getWorkbench().getOperationSupport()
					.getOperationHistory().execute(op, null, null);
		} catch (ExecutionException e) {
			FMUIPlugin.getDefault().logError(e);

		}
	}

	@Override
	protected void updateProperties() {
		boolean or = feature.isOr();
		// setEnabled(connectionSelected && !feature.isRoot() && !or);
		setEnabled(connectionSelected && !or);
		setChecked(connectionSelected && or);
	}

}
