package jcodelib.diffutil;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import jcodelib.element.IJMChange;

public class TreeDiffTest {

	@Test
	public void testDiffIJM() {
		File src = new File("resources/DiffTestBefore.java");
		File dst = new File("resources/DiffTestAfter.java");
		try {
			DiffResult res = TreeDiff.diffIJM(src, dst);
			List<IJMChange> changes = (List<IJMChange>)res.getScript();
			Map<String, Integer> count = new HashMap<>();
			for(IJMChange change : changes) {
				String key = change.getChangeType() + ":" + change.getEntityType();
				System.out.println(key);
				count.compute(key, (k, v) -> v==null ? 1 : v+1);
			}
			assertEquals(4, changes.size());
			assertEquals(2, (int)count.get("DEL:IfStatement"));
			assertEquals(1, (int)count.get("INS:ReturnStatement"));
			assertEquals(1, (int)count.get("INS:VariableDeclarationStatement"));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

}
