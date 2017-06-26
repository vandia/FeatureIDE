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
//package de.ovgu.featureide.fm.core;
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
//import java.util.List;
//import java.util.WeakHashMap;
//
//import javax.annotation.CheckForNull;
//
//import de.ovgu.featureide.fm.core.analysis.cnf.FeatureModelFormula;
//import de.ovgu.featureide.fm.core.base.IFeatureModel;
//import de.ovgu.featureide.fm.core.configuration.Configuration;
//import de.ovgu.featureide.fm.core.io.ProblemList;
//import de.ovgu.featureide.fm.core.io.manager.ConfigurationManager;
//import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
//import de.ovgu.featureide.fm.core.io.manager.IFileManager;
//import de.ovgu.featureide.fm.core.io.manager.VirtualFileManager;
//import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat;
//import de.ovgu.featureide.fm.core.job.LongRunningMethod;
//import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
//import de.ovgu.featureide.fm.core.job.util.JobSequence;
//
///**
// * 
// * @author Sebastian Krieter
// */
//public class ProjectManager {
//
//	protected static final WeakHashMap<Path, FeatureProjectData> pathFeatureProjectMap = new WeakHashMap<>();
//	protected static final WeakHashMap<IFeatureModel, FeatureProjectData> modelFeatureProjectMap = new WeakHashMap<>();
//	protected static final WeakHashMap<IFeatureModel, Path> modelPathMap = new WeakHashMap<>();
//
//	protected ProjectManager() {
//	}
//

//
//	public static void addProject(Path root, Path featureModelFile, Path configurations) {
//		synchronized (pathFeatureProjectMap) {
//			if (pathFeatureProjectMap.containsKey(root)) {
//				return;
//			}
//			final IFileManager<IFeatureModel> featureModelManager = FeatureModelManager.getInstance(featureModelFile);
//			final IFeatureModel featureModel = featureModelManager.getObject();
//			final FeatureProjectData data = new FeatureProjectData(featureModelManager);
//			data.addConfigurationManager(getConfigurationManager(configurations, featureModel));
//			pathFeatureProjectMap.put(root, data);
//			modelFeatureProjectMap.put(featureModel, data);
//			modelPathMap.put(featureModel, root);
//		}
//	}
//
//	public static FeatureProjectData addProject(Path root, Path featureModelFile) {
//		synchronized (pathFeatureProjectMap) {
//			FeatureProjectData featureProject = pathFeatureProjectMap.get(root);
//			if (featureProject == null) {
//				final IFileManager<IFeatureModel> featureModelManager = FeatureModelManager.getInstance(featureModelFile);
//				final IFeatureModel featureModel = featureModelManager.getObject();
//				featureProject = new FeatureProjectData(featureModelManager);
//				pathFeatureProjectMap.put(root, featureProject);
//				modelFeatureProjectMap.put(featureModel, featureProject);
//				modelPathMap.put(featureModel, root);
//			}
//			return featureProject;
//		}
//	}
//
//	public static ArrayList<IFileManager<Configuration>> getConfigurationManager(Path configurations, final IFeatureModel featureModel) {
//		final ArrayList<IFileManager<Configuration>> configurationManagerList = new ArrayList<>();
//		try {
//			Files.walkFileTree(configurations, new SimpleFileVisitor<Path>() {
//				@Override
//				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//					final Configuration c = new Configuration(featureModel);
//					ConfigurationManager configurationManager = ConfigurationManager.getInstance(file);
//					if (configurationManager != null) {
//						configurationManager.setObject(c);
//						configurationManager.read();
//					} else {
//						configurationManager = ConfigurationManager.getInstance(file, c);
//					}
//
//					final ProblemList lastProblems = configurationManager.getLastProblems();
//					if (lastProblems.containsError()) {
//						ConfigurationManager.removeInstance(file);
//					} else {
//						configurationManagerList.add(configurationManager);
//					}
//					return FileVisitResult.CONTINUE;
//				}
//			});
//		} catch (IOException e) {
//			Logger.logError(e);
//		}
//		return configurationManagerList;
//	}
//
//	public static void addProject(IFeatureModel featureModel) {
//		synchronized (pathFeatureProjectMap) {
//			if (modelFeatureProjectMap.containsKey(featureModel)) {
//				return;
//			}
//			modelFeatureProjectMap.put(featureModel, createVirtualFeatureProject(featureModel));
//		}
//	}
//
//	private static FeatureProjectData createVirtualFeatureProject(IFeatureModel featureModel) {
//		final FeatureProjectData data = new FeatureProjectData(new VirtualFileManager<>(featureModel, new XmlFeatureModelFormat()));
//		data.addConfigurationManager(new ArrayList<IFileManager<Configuration>>());
//		return data;
//	}
//
//	@CheckForNull
//	public static FeatureProjectData removeProject(Path root) {
//		synchronized (pathFeatureProjectMap) {
//			final FeatureProjectData project = pathFeatureProjectMap.remove(root);
//			if (project != null) {
//				final IFeatureModel fm = project.getFeatureModelManager().getObject();
//				modelPathMap.remove(fm);
//				modelFeatureProjectMap.remove(fm);
//			}
//			return project;
//		}
//	}
//
//	@CheckForNull
//	public static FeatureProjectData removeProject(IFeatureModel featureModel) {
//		synchronized (pathFeatureProjectMap) {
//			final FeatureProjectData project = modelFeatureProjectMap.remove(featureModel);
//			if (project != null) {
//				final Path path = modelPathMap.get(featureModel);
//				if (path != null) {
//					pathFeatureProjectMap.remove(path);
//				}
//			}
//			return project;
//		}
//	}
//
//	/**
//	 * returns an unmodifiable Collection of all ProjectData items, or <code>null</code> if plugin is not loaded
//	 * 
//	 * @return
//	 */
//	public static Collection<FeatureProjectData> getFeatureProjects() {
//		synchronized (pathFeatureProjectMap) {
//			return Collections.unmodifiableCollection(modelFeatureProjectMap.values());
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
//	@CheckForNull
//	public static FeatureProjectData getProject(Path path) {
//		synchronized (pathFeatureProjectMap) {
//			return pathFeatureProjectMap.get(path);
//		}
//	}
//
//	@CheckForNull
//	public static FeatureProjectData getProject(IFeatureModel model) {
//		synchronized (pathFeatureProjectMap) {
//			return modelFeatureProjectMap.get(model);
//		}
//	}
//
//	public static boolean hasProjectData(Path path) {
//		synchronized (pathFeatureProjectMap) {
//			return pathFeatureProjectMap.containsKey(path);
//		}
//	}
//
//	public static boolean hasProjectData(IFeatureModel model) {
//		synchronized (pathFeatureProjectMap) {
//			return pathFeatureProjectMap.containsKey(model);
//		}
//	}
//
//	public static FeatureModelAnalyzer getAnalyzer(IFeatureModel featureModel) {
//		final Path sourceFile = featureModel.getSourceFile();
//		if (sourceFile != null) {
//			final FeatureModelManager instance = FeatureModelManager.getInstance(sourceFile);
//			if (instance != null) {
//				return instance.getSnapshot().getAnalyzer();
//			}
//		}
//		return new FeatureModelAnalyzer(new FeatureModelFormula(featureModel));
//	}
//
//}
