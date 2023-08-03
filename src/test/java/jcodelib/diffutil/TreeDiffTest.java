package jcodelib.diffutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import jcodelib.element.CDChange;
import kr.ac.seoultech.selab.esscore.model.ESNodeEdit;
import kr.ac.seoultech.selab.esscore.model.Script;
import kr.ac.seoultech.selab.esscore.util.FileHandler;

public class TreeDiffTest {

	@Test
	public void testDiffCDChanges() {
		File left = new File("resources/DiffTestBefore.java");
		File right = new File("resources/DiffTestAfter.java");
		try {
			List<SourceCodeChange> changes = TreeDiff.diffChangeDistiller(left, right);
			List<CDChange> converted = TreeDiff.convertSourceCodeChanges(changes);
			assertEquals(changes.size(), converted.size());
			for(int i=0; i<changes.size(); i++) {
				assertFalse(changes.get(i).getChangeType().toString().equals(converted.get(i).getChangeType()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Ignore
	@Test
	public void testDiffCDChanges2() {
		File left = new File("resources/DiffTestBefore.java");
		File right = new File("resources/DiffTestAfter.java");
		try {
			List<CDChange> changes = new ArrayList<>();
			changes.add(new CDChange("INS", "xx", 186, 207));
			changes.add(new CDChange("INS", "xx", 214, 238));
			changes.add(new CDChange("DEL", "xx", 121, 152));
			changes.add(new CDChange("DEL", "xx", 256, 285));
			changes.add(new CDChange("MOV", "xx", 98, 197));
			changes.add(new CDChange("MOV", "xx", 166, 197));
			TreeDiff.updateEntityTypes(left, right, changes);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testDiffGumtree() {
		File left = new File("resources/DiffTestBefore.java");
		File right = new File("resources/DiffTestAfter.java");
		try {
			String oldCode = FileHandler.readFile(left);
			String newCode = FileHandler.readFile(right);
			DiffResult res = TreeDiff.diffGumTree(oldCode, newCode);
			Script script = res.getScript();
			assertEquals(32, script.editOps.size());
			assertTrue(script.editOps.stream().anyMatch(op -> op.type.equals(ESNodeEdit.OP_MOVE)));
			Map<Object, List<ESNodeEdit>> count = script.editOps.stream().collect(Collectors.groupingBy(op -> op.type));
			assertEquals(11, count.get(ESNodeEdit.OP_MOVE).size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
