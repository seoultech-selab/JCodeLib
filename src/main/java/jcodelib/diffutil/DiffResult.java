package jcodelib.diffutil;

import kr.ac.seoultech.selab.esscore.model.Script;

public class DiffResult {

	private Object script;
	private long runtime;

	public DiffResult() {
		this(new Script(), 0);
	}

	public DiffResult(Object script) {
		this(script, 0);
	}

	public DiffResult(Object script, long runtime) {
		super();
		this.script = script;
		this.runtime = runtime;
	}

	public Object getScript() {
		return script;
	}

	public void setScript(Object script) {
		this.script = script;
	}

	public long getRuntime() {
		return runtime;
	}

	public void setRuntime(long runtime) {
		this.runtime = runtime;
	}

}
