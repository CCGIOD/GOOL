package gool.ast.core;

import gool.generator.GoolGeneratorController;

public class Case extends Statement {

	private Expression exp;
	
	private Statement statement;
	
	public Case (Expression e, Statement s){
		this.exp=e;
		this.statement=s;
	}
	
	public Expression getExp() {
		return exp;
	}

	public Statement getStatement() {
		return statement;
	}
	
	@Override
	public String callGetCode() {
		return GoolGeneratorController.generator().getCode(this);
	}	
}
