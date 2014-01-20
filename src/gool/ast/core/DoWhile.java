package gool.ast.core;

import gool.generator.GoolGeneratorController;

public class DoWhile extends Statement{
	
	private Statement doStatement;
	private Expression condition;
	
	public DoWhile(Statement doStatement, Expression condition){
		this.condition = condition;
		this.doStatement = doStatement;
	}
	
	public Expression getCondition() {
		return condition;
	}

	public Statement getDoStatement() {
		return doStatement;
	}

	@Override
	public String callGetCode() {
		return GoolGeneratorController.generator().getCode(this);
	}

}
