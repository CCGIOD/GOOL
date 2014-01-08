package gool.ast.core;

import java.util.List;

import gool.generator.GoolGeneratorController;

public class Switch extends Statement {

	private Expression var;

	private List <Case> listCases;

	public Switch(Expression var, List <Case> l) {
		this.var = var;
		this.listCases = l;
	}
	
	public Expression getVar() {
		return var;
	}

	public List<Case> getListCases() {
		return listCases;
	}
	
	@Override
	public String callGetCode() {
		return GoolGeneratorController.generator().getCode(this);
	}
}
