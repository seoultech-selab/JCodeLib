package jcodelib.diffutil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Matcher;

import at.aau.softwaredynamics.classifier.AbstractJavaChangeClassifier;
import at.aau.softwaredynamics.classifier.JChangeClassifier;
import at.aau.softwaredynamics.classifier.NonClassifyingClassifier;
import at.aau.softwaredynamics.classifier.entities.FileChangeSummary;
import at.aau.softwaredynamics.gen.DocIgnoringTreeGenerator;
import at.aau.softwaredynamics.gen.OptimizedJdtTreeGenerator;
import at.aau.softwaredynamics.gen.SpoonTreeGenerator;
import at.aau.softwaredynamics.matchers.JavaMatchers;
import at.aau.softwaredynamics.runner.util.ClassifierFactory;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.Move;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import edu.fdu.se.cldiff.CLDiffLocal;
import file.FileIOManager;
import jcodelib.element.CDChange;
import jcodelib.element.IJMChange;
import jcodelib.util.CodeUtils;
import kr.ac.seoultech.selab.esscore.model.Script;
import kr.ac.seoultech.selab.esscore.util.IJMScriptConverter;
import kr.ac.seoultech.selab.esscore.util.LASScriptConverter;
import script.ScriptGenerator;
import script.model.EditScript;
import tree.Tree;
import tree.TreeBuilder;
import tree.TreeNode;

public class TreeDiff {

	public static List<SourceCodeChange> diffChangeDistiller(File left, File right){

		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		try {
			distiller.extractClassifiedSourceCodeChanges(left, right);
		} catch(Exception e) {
			/* An exception most likely indicates a bug in ChangeDistiller. Please file a
		       bug report at https://bitbucket.org/sealuzh/tools-changedistiller/issues and
		       attach the full stack trace along with the two files that you tried to distill. */
			System.err.println("Warning: error while change distilling. " + e.getMessage());
		}

		List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
		List<SourceCodeChange> filteredChanges = new ArrayList<>();
		if(changes != null) {
			for(SourceCodeChange change : changes) {
				if(!change.getChangedEntity().getType().isComment()){
					filteredChanges.add(change);
				}
			}
		}

		return filteredChanges;
	}

	public static List<CDChange> diffCDChanges(File left, File right) throws IOException {
		List<SourceCodeChange> changes = diffChangeDistiller(left, right);
		return convertSourceCodeChanges(changes);
	}

	public static List<CDChange> convertSourceCodeChanges(List<SourceCodeChange> changes) throws IOException {
		//Convert CDChange with new change type.
		List<CDChange> converted = new ArrayList<>();
		for(SourceCodeChange scc : changes) {
			SourceRange range = scc.getChangedEntity().getSourceRange();
			String changeType = scc.getChangeType().toString();
			if(scc instanceof Insert) {
				changeType = CDChange.INSERT;
			}else if(scc instanceof Delete) {
				changeType = CDChange.DELETE;
			}else if(scc instanceof Move) {
				changeType = CDChange.MOVE;
			}else if(scc instanceof Update) {
				changeType = CDChange.UPDATE;
			}
			converted.add(new CDChange(changeType, scc.getChangedEntity().getType().toString(),
					range.getStart(), range.getEnd()));
		}
		return converted;
	}

	public static void updateEntityTypes(File leftFile, File rightFile, List<CDChange> changes) throws IOException {
		//Create maps for start/end positions.
		Tree left = TreeBuilder.buildTreeFromFile(leftFile);
		Tree right = TreeBuilder.buildTreeFromFile(rightFile);
		Map<Integer, TreeMap<Integer, String>> leftTypeMap = createNodeTypeMap(left.getRoot());
		Map<Integer, TreeMap<Integer, String>> rightTypeMap = createNodeTypeMap(right.getRoot());

		//Search for corresponding TreeNode using start/end positions.
		for(CDChange c : changes) {
			//Check left or right based on change type. If missing, use Unknown# + original.
			String entityType = null;
			switch(c.getChangeType()) {
			case CDChange.INSERT:
				entityType = findClosestEntity(rightTypeMap, c.getStartPos(), c.getEndPos());
				break;
			case CDChange.DELETE:
			case CDChange.MOVE:
			case CDChange.UPDATE:
				entityType = findClosestEntity(leftTypeMap, c.getStartPos(), c.getEndPos());
			}
			c.setEntityType(entityType == null ? "Unknown#"+c.getEntityType() : entityType);
		}
	}

	private static String findClosestEntity(Map<Integer, TreeMap<Integer, String>> map, int start, int end) {
		//Find an entity which has the same start position and closest end position.
		if(!map.containsKey(start))
			return null;
		Integer closest = map.get(start).ceilingKey(end);
		return closest != null ? map.get(start).get(closest) : null;
	}

	private static Map<Integer, TreeMap<Integer, String>> createNodeTypeMap(TreeNode n) {
		Map<Integer, TreeMap<Integer, String>> map = new HashMap<>();
		createNodeTypeMap(map, n);
		return map;
	}

	private static void createNodeTypeMap(Map<Integer, TreeMap<Integer, String>> map, TreeNode n) {
		if(n != null && n.getASTNode() != null) {
			int start = n.getStartPosition();
			int end = n.getEndPosition();
			if(!map.containsKey(start))
				map.put(start, new TreeMap<>());
			map.get(start).put(end, CodeUtils.getTypeName(n.getType()));
		}
		for(TreeNode child : n.children) {
			createNodeTypeMap(map, child);
		}
	}

	public static EditScript diffLAS(File srcFile, File dstFile){
		try {
			Tree before = tree.TreeBuilder.buildTreeFromFile(srcFile);
			Tree after = tree.TreeBuilder.buildTreeFromFile(dstFile);
			EditScript script = ScriptGenerator.generateScript(before, after);
			return script;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static DiffResult diffLAS(String oldCode, String newCode) throws IOException {
		long startTime = System.currentTimeMillis();
		Tree before = tree.TreeBuilder.buildTreeFromSource(oldCode);
		Tree after = tree.TreeBuilder.buildTreeFromSource(newCode);
		EditScript script = ScriptGenerator.generateScript(before, after);
		long endTime = System.currentTimeMillis();
		Script converted = LASScriptConverter.convert(script);
		DiffResult result = new DiffResult(converted, endTime - startTime);

		return result;

	}

	public static void runCLDiff(String repo, String commitId, String outputDir) {
		CLDiffLocal CLDiffLocal = new CLDiffLocal();
		CLDiffLocal.run(commitId,repo,outputDir);
	}

	public static List<at.aau.softwaredynamics.classifier.entities.SourceCodeChange> diffIJMOriginal(File srcFile, File dstFile) {
		//Default options from the example: -c None -m IJM -w FS -g OTG
		return diffIJMOriginal(srcFile, dstFile, "None", "IJM", "OTG");
	}

	public static List<at.aau.softwaredynamics.classifier.entities.SourceCodeChange> diffIJMOriginal(File srcFile, File dstFile, String optClassifier, String optMatcher, String optGenerator) {
		Class<? extends AbstractJavaChangeClassifier> classifierType = getClassifierType(optClassifier);
		Class<? extends Matcher> matcher = getMatcherTypes(optMatcher);
		TreeGenerator generator = getTreeGenerator(optGenerator);

		ClassifierFactory factory = new ClassifierFactory(classifierType, matcher, generator);
		FileChangeSummary summary = new FileChangeSummary("", "", srcFile.getName(), dstFile.getName());

		try {
			String oldCode = FileIOManager.getContent(srcFile);
			String newCode = FileIOManager.getContent(dstFile);

			AbstractJavaChangeClassifier classifier = factory.createClassifier();
			try {
				classifier.classify(oldCode, newCode);
				summary.setChanges(classifier.getCodeChanges());
				summary.setMetrics(classifier.getMetrics());
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			List<at.aau.softwaredynamics.classifier.entities.SourceCodeChange> changes = classifier.getCodeChanges();

			return changes;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static DiffResult diffIJM(File srcFile, File dstFile) {
		//Default options from the example: -c None -m IJM -w FS -g OTG, with subtree edits grouped.
		return diffIJM(srcFile, dstFile, "None", "IJM", "OTG", true);
	}

	public static DiffResult diffIJM(File srcFile, File dstFile, boolean subtree) {
		//Default options from the example: -c None -m IJM -w FS -g OTG
		return diffIJM(srcFile, dstFile, "None", "IJM", "OTG", subtree);
	}

	public static DiffResult diffIJM(File srcFile, File dstFile, String optClassifier, String optMatcher, String optGenerator, boolean subtree) {
		Class<? extends AbstractJavaChangeClassifier> classifierType = getClassifierType(optClassifier);
		Class<? extends Matcher> matcher = getMatcherTypes(optMatcher);
		TreeGenerator generator = getTreeGenerator(optGenerator);

		ClassifierFactory factory = new ClassifierFactory(classifierType, matcher, generator);
		FileChangeSummary summary = new FileChangeSummary("", "", srcFile.getName(), dstFile.getName());

		try {
			String oldCode = FileIOManager.getContent(srcFile);
			String newCode = FileIOManager.getContent(dstFile);

			AbstractJavaChangeClassifier classifier = factory.createClassifier();
			try {
				classifier.classify(oldCode, newCode);
				summary.setChanges(classifier.getCodeChanges());
				summary.setMetrics(classifier.getMetrics());
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			List<at.aau.softwaredynamics.classifier.entities.SourceCodeChange> changes = classifier.getCodeChanges();
			Script script = IJMScriptConverter.convert(changes);
			DiffResult result = new DiffResult(script, summary.getMetrics().getTotalTime());

			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static List<IJMChange> getIJMChanges(List<at.aau.softwaredynamics.classifier.entities.SourceCodeChange> changes, boolean subtree) {
		//Group changes.
		List<IJMChange> grouped = new ArrayList<>();
		for(at.aau.softwaredynamics.classifier.entities.SourceCodeChange change : changes) {
			//No parent changes indicates this is the change subtree root.
			if(!subtree || change.getParentChanges().size() == 0) {
				grouped.add(convert(change));
			} else {
				//The change's direct parent node has different change type, also add this change as the subtree root.
				List<at.aau.softwaredynamics.classifier.entities.SourceCodeChange> nodeChanges
				= at.aau.softwaredynamics.classifier.entities.SourceCodeChange.treeIDChangeMap.get(change.getNode().getParent());
				for(at.aau.softwaredynamics.classifier.entities.SourceCodeChange c : nodeChanges) {
					if(!c.getAction().getName().equals(change.getAction().getName())) {
						grouped.add(convert(change));
						break;
					}
				}
			}
		}
		return grouped;
	}

	public static IJMChange convert(at.aau.softwaredynamics.classifier.entities.SourceCodeChange change) {
		IJMChange converted = new IJMChange(change);
		for(at.aau.softwaredynamics.classifier.entities.SourceCodeChange child : change.getChildrenChanges()) {
			converted.addChild(convert(child));
		}
		return converted;
	}

	private static Class<? extends Matcher> getMatcherTypes(String option) {
		switch(option) {
		case "GT":
			return CompositeMatchers.ClassicGumtree.class;
		case "IJM":
			return JavaMatchers.IterativeJavaMatcher_V2.class;
		case "IJM_Spoon":
			return JavaMatchers.IterativeJavaMatcher_Spoon.class;
		}

		return null;
	}

	private static Class<? extends AbstractJavaChangeClassifier> getClassifierType(String option) {
		switch (option) {
		case "Java": return JChangeClassifier.class;
		case "None": return NonClassifyingClassifier.class;
		default: return JChangeClassifier.class;
		}
	}

	private static TreeGenerator getTreeGenerator(String option) {
		switch (option)
		{
		case "OTG": return new OptimizedJdtTreeGenerator();
		case "JTG": return new JdtTreeGenerator();
		case "JTG1": return new DocIgnoringTreeGenerator();
		case "SPOON":
			return new SpoonTreeGenerator();
		}
		return null;
	}
}
