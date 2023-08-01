package jcodelib.diffutil;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import at.aau.softwaredynamics.classifier.entities.SourceCodeChange;
import jcodelib.element.IJMChange;
import kr.ac.seoultech.selab.esscore.util.CodeHandler;

public class TreeDiffTest {

	@Test
	public void testDiffIJM() {
		File src = new File("resources/DiffTestBefore.java");
		File dst = new File("resources/DiffTestAfter.java");
		try {
			DiffResult res = TreeDiff.diffIJM(src, dst);
			List<IJMChange> changes = (List<IJMChange>)res.getScript();
			Map<String, Integer> count = updateCount(changes);
			assertEquals(41, changes.size());
			assertEquals(2, (int)count.get("DEL:IfStatement"));
			assertEquals(2, (int)count.get("INS:ReturnStatement"));
			assertEquals(1, (int)count.get("INS:VariableDeclarationStatement"));
			assertEquals(2, (int)count.get("MOV:InfixExpression"));

			res = TreeDiff.diffIJM(src, dst, true);
			changes = (List<IJMChange>)res.getScript();
			count = updateCount(changes);
			assertEquals(13, changes.size());

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	private Map<String, Integer> updateCount(List<IJMChange> changes) {
		Map<String, Integer> count = new HashMap<>();
		for(IJMChange change : changes) {
			String key = change.getChangeType() + ":" + change.getEntityType();
			System.out.println(key);
			count.compute(key, (k, v) -> v==null ? 1 : v+1);
		}
		return count;
	}

	@Test
	public void testDiffIJMOriginal() {
		File src = new File("resources/DiffTestBefore.java");
		File dst = new File("resources/DiffTestAfter.java");
		try {
			List<SourceCodeChange> changes = TreeDiff.diffIJMOriginal(src, dst);
			assertEquals(41, changes.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public String getInfo(SourceCodeChange c) {
		return c.getAction().getName() + ":" + CodeHandler.getTypeName(c.getNodeType())
		+ "(" + c.getSrcInfo().getStartLineNumber() + "," + c.getDstInfo().getStartLineNumber() + ")";
	}

}
