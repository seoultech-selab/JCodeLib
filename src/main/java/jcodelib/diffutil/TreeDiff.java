package jcodelib.diffutil;

import at.aau.softwaredynamics.classifier.AbstractJavaChangeClassifier;
import at.aau.softwaredynamics.classifier.JChangeClassifier;
import at.aau.softwaredynamics.classifier.NonClassifyingClassifier;
import at.aau.softwaredynamics.classifier.entities.FileChangeSummary;
import at.aau.softwaredynamics.gen.DocIgnoringTreeGenerator;
import at.aau.softwaredynamics.gen.OptimizedJdtTreeGenerator;
import at.aau.softwaredynamics.gen.SpoonTreeGenerator;
import at.aau.softwaredynamics.matchers.JavaMatchers;
import at.aau.softwaredynamics.runner.util.ClassifierFactory;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Matcher;
import file.FileIOManager;
import jcodelib.element.IJMChange;
import kr.ac.seoultech.selab.esscore.model.Script;
import kr.ac.seoultech.selab.esscore.util.IJMScriptConverter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TreeDiff {

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
			IJMScriptConverter.computePosLineMap(oldCode, newCode);

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
//		case "JTG": return new JdtTreeGenerator();
		case "JTG1": return new DocIgnoringTreeGenerator();
		case "SPOON":
			return new SpoonTreeGenerator();
		}
		return null;
	}
}
