package gool.recognizer.cpp;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import gool.ast.core.BinaryOperation;
import gool.ast.core.ClassDef;
import gool.ast.core.Constant;
import gool.ast.core.Expression;
import gool.ast.core.ExpressionUnknown;
import gool.ast.core.Field;
import gool.ast.core.GoolCall;
import gool.ast.core.MainMeth;
import gool.ast.core.Meth;
import gool.ast.core.MethCall;
import gool.ast.core.Modifier;
import gool.ast.core.Operator;
import gool.ast.core.Print;
import gool.ast.core.Return;
import gool.ast.core.Statement;

import gool.ast.system.SystemOutDependency;
import gool.ast.system.SystemOutPrintCall;
import gool.ast.type.IType;
import gool.ast.type.TypeBool;
import gool.ast.type.TypeChar;
import gool.ast.type.TypeDecimal;
import gool.ast.type.TypeInt;
import gool.ast.type.TypeString;
import gool.ast.type.TypeVoid;
import gool.parser.cpp.CPPParser;
import gool.parser.cpp.CPPParserTreeConstants;
import gool.parser.cpp.SimpleNode;
import gool.generator.GeneratorHelper;
import gool.generator.common.Platform;
import gool.generator.cpp.CppPlatform;
import gool.generator.java.JavaPlatform;
import gool.generator.python.PythonPlatform;

/**
 * Liste choses implémantées :
 * 	- Construction d'une classe principale à partir du nom du fichier
 *  - ajout de méthodes "externes" dont le main (différenciation)
 *  - gestion du return (<EXPRESSION>); 
 *  - declaration d'attributs sous la forme : int v1,v2=1,v3,v4=1+1; ne marche que sur java ...
 *
 */

public class CppRecognizer implements CPPParserTreeConstants {

	// AST produit par le parser C++
	private SimpleNode AST;

	// Collection des classes <=> AST GOOL
	private Collection<ClassDef> goolClasses = new ArrayList<ClassDef> ();

	// Langage output (fixé à JAVA pour les tests)
	private Platform defaultPlatform = JavaPlatform.getInstance();
	//private Platform defaultPlatform = CppPlatform.getInstance();
	//private Platform defaultPlatform = PythonPlatform.getInstance();

	// Getter sur L'AST GOOL
	public final Collection<ClassDef> getGoolClasses() {
		return goolClasses;
	}

	// Constructeur
	public CppRecognizer (){
		this.AST= CPPParser.getCppAST();
	}

	// Getter sur l'ast c++
	public SimpleNode getAST (){
		return AST;
	}

	// Pointeurs sur la "classe active",
	//ie la classe à laquelle les éléments qui sont en trian d'être visités appartiennent
	private ClassDef classActive;
	private Meth methActive;

	// Fonction Main qui parse un fichier test (fixe), construit un AST GOOL et affiche l'output en JAVA dans la console
	public static void main (String args[]){
		CppRecognizer cppr = new CppRecognizer();

		try{
			cppr.getAST().dump(""); // affiche l'ast c++
		}
		catch (NullPointerException e){ System.out.println("Parsing error !"); }

		cppr.constructGoolAst(cppr.getAST());
		try {
			GeneratorHelper.printClassDefs(cppr.getGoolClasses()); // println rajouté dans gool.generator.common.CodePrinter
		} catch (FileNotFoundException e) {}
	}

	// Fonction principale qui visite l'ast c++ : a developper
	public Object constructGoolAst (SimpleNode node){

		if (node == null)
			return null;

		switch(node.jjtGetId()){
		case JJTTRANSLATION_UNIT :
			ClassDef unitaryClass = new ClassDef(Modifier.PUBLIC, createClassNameFromFilename(node.jjtGetValue()), defaultPlatform);
			goolClasses.add(unitaryClass);
			classActive=(ClassDef) goolClasses.toArray()[0];

			for (int i=0; i<node.jjtGetNumChildren();i++)
				constructGoolAst((SimpleNode) node.jjtGetChild(i));
			break;
		case JJTFUNCTION_DEFINITION :
			Meth m;		
			if (getValueFromNode(node,JJTQUALIFIED_ID).compareTo("main") == 0 && ((SimpleNode) node.jjtGetParent()).jjtGetId() == JJTEXTERNAL_DECLARATION){
				m = new MainMeth();
			}
			else{
				m = new Meth(convertIType(getValueFromNode(node,JJTBUILTIN_TYPE_SPECIFIER)), Modifier.PUBLIC, getValueFromNode(node,JJTQUALIFIED_ID));
			}

			classActive.addMethod(m);
			methActive=m;

			constructGoolAst((SimpleNode) node.jjtGetChild(2)); // visite du contenu de la fonction			
			break;
		case JJTDECLARATION :			
			IType type = convertIType(getValueFromNode(node,JJTBUILTIN_TYPE_SPECIFIER));			
			SimpleNode decl_list_node = (SimpleNode) node.jjtGetChild(1); // <- INIT_DECLARATOR_LIST			
			for (int i=0;i<decl_list_node.jjtGetNumChildren();i++){
				String name = getValueFromNode((SimpleNode) decl_list_node.jjtGetChild(i),JJTQUALIFIED_ID);				
				Expression def = null;
				if (decl_list_node.jjtGetChild(i).jjtGetNumChildren() > 1)
					def = (Expression) constructGoolAst((SimpleNode) decl_list_node.jjtGetChild(i).jjtGetChild(1));
				classActive.addField(new Field(Modifier.PRIVATE, name, type, def));
			}
			break;
		case JJTSTATEMENT_LIST :			
			List<Statement> ls = new ArrayList<Statement>();
			for (int i=0;i<node.jjtGetNumChildren();i++){
				Statement s = (Statement) constructGoolAst ((SimpleNode) node.jjtGetChild(i));				
				if (s == null)
					s = new ExpressionUnknown(TypeString.INSTANCE, "");
				//if (!(s instanceof Return && methActive.isMainMethod())) // Pour Java, on doit supprimer le return ...
					ls.add(s);
			}
			methActive.addStatements(ls);
			break;
		case JJTJUMP_STATEMENT :
			return new Return ((Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0)));
		case JJTADDITIVE_EXPRESSION :
			return getBinaryExpression(node, 0, Operator.PLUS, "+"); // Traite le cas où on veut seulement descendre dans l'ast
		case JJTMULTIPLICATIVE_EXPRESSION :
			return getBinaryExpression(node, 0, Operator.MULT, "*"); // Traite le cas où on veut seulement descendre dans l'ast
		case JJTCONSTANT : 
			return new Constant(convertIType((String) node.jjtGetType()),node.jjtGetValue());
		case JJTPRIMARY_EXPRESSION :
			if (node.jjtGetNumChildren() == 0)
				return new Constant(TypeString.INSTANCE,((String) node.jjtGetValue()).subSequence(1, ((String) node.jjtGetValue()).length()-1));
		case JJTSHIFT_EXPRESSION :
			if (isFunctionPrint(node)){				
				classActive.addDependency(new SystemOutDependency());
				GoolCall gc = new SystemOutPrintCall();
				gc.addParameter(getExpressionPrint(node, 1));
				return gc;
			}
			// else pas de return : on va dans le default
		default :		
			if (node.jjtGetNumChildren() == 1)
				return constructGoolAst((SimpleNode) node.jjtGetChild(0));	
			for (int i=0;i<node.jjtGetNumChildren();i++)
				constructGoolAst((SimpleNode) node.jjtGetChild(i));				
		}

		return null;
	}

	// permet de trouver le nom de la classe principale (à partir du nom du fichier c++ en input)
	private String createClassNameFromFilename(Object o){
		String filename = (String) o;
		String className = filename.split("\\.")[0].toLowerCase();		
		return className.substring(0, 1).toUpperCase() + className.substring(1);
	}

	private String getValueFromNode (SimpleNode node, int nodeType){
		if (node.jjtGetId() == nodeType)
			return (String) node.jjtGetValue();
		else{
			for (int i=0;i<node.jjtGetNumChildren();i++){
				String valueChild = getValueFromNode((SimpleNode) node.jjtGetChild(i), nodeType);
				if (valueChild != null)
					return valueChild;
			}
		}
		return null;
	}

	// Fonction de convertion entre les type c++ et les type GOOL
	private IType convertIType (String type){
		if (type.compareTo("int") == 0){return TypeInt.INSTANCE;}
		else if (type.compareTo("void") == 0){return TypeVoid.INSTANCE;}
		else if (type.compareTo("char") == 0){return TypeChar.INSTANCE;}
		else if (type.compareTo("short") == 0){return TypeInt.INSTANCE; /* short -> int */ }
		else if (type.compareTo("long") == 0){return TypeInt.INSTANCE; /* long -> int */ }
		else if (type.compareTo("float") == 0){return TypeDecimal.INSTANCE; /* float -> decimal */ }
		else if (type.compareTo("double") == 0){return TypeDecimal.INSTANCE; /* double -> decimal */ }
		else if (type.compareTo("signed") == 0){return TypeInt.INSTANCE; /* signed -> int */ }
		else if (type.compareTo("unsigned") == 0){return TypeInt.INSTANCE; /*unsigned -> int */ }
		else if (type.compareTo("boolean") == 0){return TypeBool.INSTANCE;}
		else return null;
	}

	private Expression getBinaryExpression (SimpleNode node, int i, Operator ope, String symbol){
		if (i == node.jjtGetNumChildren()-1)
			return (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(i));
		else{
			Expression e1 = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(i));
			return new BinaryOperation(ope,e1,getBinaryExpression (node, i+1, ope, symbol),e1.getType(),symbol);
		}
	}

	private Expression getExpressionPrint (SimpleNode node, int i){
		if (i == node.jjtGetNumChildren()-2)
			return (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(i));
		else{
			Expression e1 = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(i));
			return new BinaryOperation(Operator.PLUS,e1,getExpressionPrint (node, i+1),e1.getType(),"+");
		}
	}

	// node est du type SHIFT_EXPRESSION
	private boolean isFunctionPrint (SimpleNode node){
		if (node.jjtGetNumChildren() < 3)
			return false;

		String cL = getValueFromNode((SimpleNode) node.jjtGetChild(0), JJTID_EXPRESSION); 
		String cR = getValueFromNode((SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1), JJTID_EXPRESSION); 

		return cL.compareTo("cout") == 0 && cR.compareTo("endl") == 0;
	}
}