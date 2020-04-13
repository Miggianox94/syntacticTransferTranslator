package it.coluccia.syntactic.translator.main;

public class Dependency {
	
	private String dep;
	private int governor;
	private String governorGloss;
	private int dependent;
	private String dependentGloss;
	
	
	public Dependency(String dep, int governor, String governorGloss, int dependent, String dependentGloss) {
		super();
		this.dep = dep;
		this.governor = governor;
		this.governorGloss = governorGloss;
		this.dependent = dependent;
		this.dependentGloss = dependentGloss;
	}
	
	
	public String getDep() {
		return dep;
	}
	public void setDep(String dep) {
		this.dep = dep;
	}
	public int getGovernor() {
		return governor;
	}
	public void setGovernor(int governor) {
		this.governor = governor;
	}
	public String getGovernorGloss() {
		return governorGloss;
	}
	public void setGovernorGloss(String governorGloss) {
		this.governorGloss = governorGloss;
	}
	public int getDependent() {
		return dependent;
	}
	public void setDependent(int dependent) {
		this.dependent = dependent;
	}
	public String getDependentGloss() {
		return dependentGloss;
	}
	public void setDependentGloss(String dependentGloss) {
		this.dependentGloss = dependentGloss;
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Dependency [dep=");
		builder.append(dep);
		builder.append(", governor=");
		builder.append(governor);
		builder.append(", governorGloss=");
		builder.append(governorGloss);
		builder.append(", dependent=");
		builder.append(dependent);
		builder.append(", dependentGloss=");
		builder.append(dependentGloss);
		builder.append("]");
		return builder.toString();
	}
	
	
	
	
}
