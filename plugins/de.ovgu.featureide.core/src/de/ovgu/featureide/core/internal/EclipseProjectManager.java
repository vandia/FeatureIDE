///* FeatureIDE - A Framework for Feature-Oriented Software Development
// * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
// *
// * This file is part of FeatureIDE.
// * 
// * FeatureIDE is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Lesser General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// * 
// * FeatureIDE is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Lesser General Public License for more details.
// * 
// * You should have received a copy of the GNU Lesser General Public License
// * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
// *
// * See http://featureide.cs.ovgu.de/ for further information.
// */
//package de.ovgu.featureide.core.internal;
//
//import java.io.IOException;
//import java.nio.file.FileVisitResult;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.SimpleFileVisitor;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//
//import javax.annotation.CheckForNull;
//
//import de.ovgu.featureide.fm.core.JavaProjectManager;
//import de.ovgu.featureide.fm.core.base.IFeatureModel;
//import de.ovgu.featureide.fm.core.configuration.Configuration;
//import de.ovgu.featureide.fm.core.io.EclipseFileSystem;
//import de.ovgu.featureide.fm.core.io.InternalFeatureModelFormat;
//import de.ovgu.featureide.fm.core.io.ProblemList;
//import de.ovgu.featureide.fm.core.io.manager.ConfigurationManager;
//import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
//import de.ovgu.featureide.fm.core.io.manager.IFileManager;
//import de.ovgu.featureide.fm.core.io.manager.VirtualFileManager;
//
///**
// * 
// * @author Sebastian Krieter
// */
//public class EclipseProjectManager extends JavaProjectManager {
//
//	protected final HashMap<Path, FeatureProject> modelPathProjectMap = new HashMap<>();
//
//	protected EclipseProjectManager() {
//	}
//
//	@CheckForNull
//	public FeatureProject removeProject(final IFeatureModel model) {
//		final Path sourceFile = model.getSourceFile();
//		if (sourceFile != null) {
//			return getProject(sourceFile);
//		}
//		return null;
//	}
//
//	@CheckForNull
//	public FeatureProject removeProject(final Path sourceFile) {
//		synchronized (modelPathProjectMap) {
//			return modelPathProjectMap.remove(sourceFile);
//		}
//	}
//
//	/**
//	 * 
//	 * @return an unmodifiable Collection of all ProjectData items, or <code>null</code> if plugin is not loaded
//	 */
//	public Collection<FeatureProject> getAllProjects() {
//		synchronized (modelPathProjectMap) {
//			return Collections.unmodifiableCollection(modelPathProjectMap.values());
//		}
//	}
//
//	public FeatureProject getProject(final IFeatureModel model) {
//		final Path sourceFile = model.getSourceFile();
//		if (sourceFile != null) {
//			return getProject(sourceFile);
//		} else {
//			return new FeatureProject(new VirtualFileManager<IFeatureModel>(model, new InternalFeatureModelFormat()));
//		}
//	}
//
//	/**
//	 * returns the ProjectData object associated with the given resource
//	 * 
//	 * @param res
//	 * @return <code>null</code> if there is no associated project, no active
//	 *         instance of this plug-in or resource is the workspace root
//	 */
//	public FeatureProject getProject(final Path sourceFile) {
//		EclipseFileSystem.getResource(sourceFile).getProject();
//		synchronized (modelPathProjectMap) {
//			FeatureProject featureProject = modelPathProjectMap.get(sourceFile);
//			if (featureProject == null) {
//				featureProject = new FeatureProject(FeatureModelManager.getInstance(sourceFile));
//			}
//			return featureProject;
//		}
//	}
//
//}
