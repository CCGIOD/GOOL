package gool.ast.core;

import gool.generator.GoolGeneratorController;

public class Case extends Statement {

	private Expression exp;
	
	private Block block;
	
	public Case (Expression e, Block s){
		this.exp=e;
		this.block=s;
	}
	
	public Expression getExp() {
		return exp;
	}

	public Statement getStatement() {
		return block;
	}
	
	@Override
	public String callGetCode() {
		return GoolGeneratorController.generator().getCode(this);
	}	
}
