package gool.recognizer.cpp;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.sun.tools.internal.xjc.generator.bean.field.NoExtendedContentField;

import gool.ast.core.ArrayAccess;
import gool.ast.core.ArrayNew;
import gool.ast.core.Assign;
import gool.ast.core.BinaryOperation;
import gool.ast.core.Block;
import gool.ast.core.Case;
import gool.ast.core.CastExpression;
import gool.ast.core.Catch;
import gool.ast.core.ClassDef;
import gool.ast.core.ClassNew;
import gool.ast.core.Constant;
import gool.ast.core.Expression;
import gool.ast.core.ExpressionUnknown;
import gool.ast.core.Field;
import gool.ast.core.FieldAccess;
import gool.ast.core.GoolCall;
import gool.ast.core.Identifier;
import gool.ast.core.If;
import gool.ast.core.MainMeth;
import gool.ast.core.Meth;
import gool.ast.core.MethCall;
import gool.ast.core.Modifier;
import gool.ast.core.NewInstance;
import gool.ast.core.Node;
import gool.ast.core.Operator;
import gool.ast.core.Print;
import gool.ast.core.Return;
import gool.ast.core.Statement;
import gool.ast.core.Switch;
import gool.ast.core.Try;
import gool.ast.core.UnaryOperation;
import gool.ast.core.VarDeclaration;
import gool.ast.core.While;

import gool.ast.system.SystemOutDependency;
import gool.ast.system.SystemOutPrintCall;
import gool.ast.type.IType;
import gool.ast.type.TypeArray;
import gool.ast.type.TypeBool;
import gool.ast.type.TypeChar;
import gool.ast.type.TypeClass;
import gool.ast.type.TypeDecimal;
import gool.ast.type.TypeInt;
import gool.ast.type.TypeMethod;
import gool.ast.type.TypeString;
import gool.ast.type.TypeUnknown;
import gool.ast.type.TypeVar;
import gool.ast.type.TypeVoid;
import gool.parser.cpp.CPPParser;
import gool.parser.cpp.CPPParserTreeConstants;
import gool.parser.cpp.SimpleNode;
import gool.generator.GeneratorHelper;
import gool.generator.common.Platform;
import gool.generator.cpp.CppPlatform;
import gool.generator.java.JavaPlatform;
import gool.generator.python.PythonPlatform;

@SuppressWarnings("unchecked")
public class CppRecognizer implements CPPParserTreeConstants {

	// AST produit par le parser C++
	private SimpleNode AST;

	// Collection des classes <=> AST GOOL
	private Collection<ClassDef> goolClasses = new ArrayList<ClassDef> ();

	// Langage output (fixé à JAVA pour les tests)
	//private Platform defaultPlatform = JavaPlatform.getInstance();
	//private Platform defaultPlatform = CppPlatform.getInstance();
	private Platform defaultPlatform = PythonPlatform.getInstance();

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
	//private ClassDef classActive;
	private Stack<ClassDef> stackClassActives = new Stack<ClassDef>();
	private Meth methActive;
	private Modifier accesModifierActive = Modifier.PUBLIC;

	// Fonction Main qui parse un fichier test (fixe), construit un AST GOOL et affiche l'output en JAVA dans la console
	public static void main (String args[]){
		CppRecognizer cppr = new CppRecognizer();

		try{
			cppr.getAST().dump(""); // affiche l'ast c++
		}
		catch (NullPointerException e){}

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
		case JJTTRANSLATION_UNIT : {
			ClassDef unitaryClass = new ClassDef(Modifier.PUBLIC, createClassNameFromFilename(node.jjtGetValue()), defaultPlatform);
			goolClasses.add(unitaryClass);
			stackClassActives.push(unitaryClass);

			for (int i=0; i<node.jjtGetNumChildren();i++)
				constructGoolAst((SimpleNode) node.jjtGetChild(i));
			break;
		}
		case JJTCLASS_SPECIFIER : {
			ClassDef cd = new ClassDef(Modifier.PUBLIC, node.jjtGetValue().toString(), defaultPlatform);
			goolClasses.add(cd);
			stackClassActives.push(cd);

			for (int i=0; i<node.jjtGetNumChildren();i++)
				constructGoolAst((SimpleNode) node.jjtGetChild(i));

			stackClassActives.pop();
			break;
		}
		case JJTBASE_CLAUSE : {
			if (node.jjtGetNumChildren() == 1){
				String name = ((SimpleNode) node.jjtGetChild(0)).jjtGetValue().toString();
				stackClassActives.peek().setParentClass(new TypeClass(name));
			}
			else{
				for (int i=0;i<node.jjtGetNumChildren();i++){
					String name = ((SimpleNode) node.jjtGetChild(i)).jjtGetValue().toString();
					setClassIsInterface(name);
					stackClassActives.peek().addInterface(new TypeClass(name));
				}
			}
			break;
		}
		case JJTFUNCTION_DEFINITION : {
			Meth m;
			if (getValueFromNode(node,JJTFUNCTION_DIRECT_DECLARATOR).compareTo("main") == 0){
				m = new MainMeth();
			}
			else{
				m = new Meth(convertIType(getValueFromNode(node,JJTDECLARATION_SPECIFIERS)), getValueFromNode(node,JJTFUNCTION_DIRECT_DECLARATOR));
				if (node.jjtGetChild(1).jjtGetChild(0).jjtGetNumChildren() > 1){
					SimpleNode nodeParam  = getNodeFromId(node, JJTPARAMETER_LIST);
					List<VarDeclaration> listVD = (List<VarDeclaration>) constructGoolAst(nodeParam);
					if (listVD != null)
						for (VarDeclaration vd : listVD)
							m.addParameter(vd);
				}

				if (testChild(node, JJTEXCEPTION_SPEC)){
					List<IType> lt = (List<IType>) constructGoolAst(getNodeFromId(node, JJTEXCEPTION_LIST));
					for (IType t : lt)
						m.addThrowStatement(t);
				}
			}

			m.setModifiers(getCollectionModifiers((SimpleNode) node.jjtGetChild(0)));
			m.addModifier(accesModifierActive);
			stackClassActives.peek().addMethod(m);
			methActive=m;

			constructGoolAst((SimpleNode) node.jjtGetChild(2)); // visite du contenu de la fonction			
			break;
		}
		case JJTEXCEPTION_LIST : {
			List<IType> listType = new ArrayList<IType>();
			for (int i=0;i<node.jjtGetNumChildren();i++)
				listType.add(convertIType(getValueFromNode((SimpleNode) node.jjtGetChild(i), JJTDECLARATION_SPECIFIERS)));
			return listType;
		}
		case JJTPARAMETER_DECLARATION_LIST : {
			List <VarDeclaration> listVD = new ArrayList<VarDeclaration>();
			for (int i=0;i<node.jjtGetNumChildren();i++)
				listVD.add((VarDeclaration) constructGoolAst((SimpleNode) node.jjtGetChild(i))); 
			return listVD;
		}
		case JJTPARAMETER_DECLARATION : {
			IType type = convertIType(getValueFromNode(node,JJTDECLARATION_SPECIFIERS));				
			String name = getValueFromNode(node,JJTDIRECT_DECLARATOR);
			VarDeclaration vd = new VarDeclaration(type, name);
			vd.setModifiers(getCollectionModifiers((SimpleNode) node.jjtGetChild(0)));
			return vd;
		}
		case JJTMEMBER_DECLARATION : {
			if (testChild(node, JJTACCESS_SPECIFIER)){
				accesModifierActive=convertModToGoolMod(getValueFromNode(node, JJTACCESS_SPECIFIER));
			}
			else if (!testChild(node, JJTFUNCTION_DEFINITION)){
				Collection <Modifier> cm = getCollectionModifiers((SimpleNode) node.jjtGetChild(0));
				IType type = convertIType(getValueFromNode(node,JJTDECLARATION_SPECIFIERS));				
				if (type.equals(TypeChar.INSTANCE) && testChild(node, JJTPTR_OPERATOR,"*"))
					type=TypeString.INSTANCE;

				SimpleNode decl_list_node = (SimpleNode) node.jjtGetChild(1); // <- MEMBER_DECLARATOR_LIST

				for (int i=0;i<decl_list_node.jjtGetNumChildren();i++){
					String name = getValueFromNode((SimpleNode) decl_list_node.jjtGetChild(i),JJTDIRECT_DECLARATOR);				

					Expression def = null;
					if (decl_list_node.jjtGetChild(i).jjtGetNumChildren() > 1 && 
							((SimpleNode) decl_list_node.jjtGetChild(i).jjtGetChild(1)).jjtGetId() == JJTINITIALIZER)
						def = (Expression) constructGoolAst((SimpleNode) decl_list_node.jjtGetChild(i).jjtGetChild(1));

					cm.add(accesModifierActive);
					stackClassActives.peek().addField(new Field(cm, name, type, def));
				}
			}
			else {
				constructGoolAst((SimpleNode) node.jjtGetChild(0));
			}
			break;
		}
		case JJTDECLARATION : {			
			// Cas d'une classe : on descend dans l'ast
			if (testChild(node, JJTCLASS_SPECIFIER)){
				constructGoolAst((SimpleNode) node.jjtGetChild(0));
				return null;
			}			

			Block blockToReturn = new Block();
			IType type = convertIType(getValueFromNode(node,JJTDECLARATION_SPECIFIERS));				
			Collection <Modifier> cm = getCollectionModifiers((SimpleNode) node.jjtGetChild(0));

			SimpleNode decl_list_node = (SimpleNode) node.jjtGetChild(1); // <- INIT_DECLARATOR_LIST			

			if (type.equals(TypeChar.INSTANCE) && testChild(node, JJTPTR_OPERATOR,"*"))
				type=TypeString.INSTANCE;

			for (int i=0;i<decl_list_node.jjtGetNumChildren();i++){
				String name = getValueFromNode((SimpleNode) decl_list_node.jjtGetChild(i),JJTDIRECT_DECLARATOR);				

				Expression def = null;
				if (decl_list_node.jjtGetChild(i).jjtGetNumChildren() > 1 && 
						((SimpleNode) decl_list_node.jjtGetChild(i).jjtGetChild(1)).jjtGetId() == JJTINITIALIZER)
					def = (Expression) constructGoolAst((SimpleNode) decl_list_node.jjtGetChild(i).jjtGetChild(1));

				// Déclaration des attributs de la classe
				if (((SimpleNode) node.jjtGetParent()).jjtGetId() == JJTEXTERNAL_DECLARATION){
					cm.add(Modifier.PRIVATE);
					stackClassActives.peek().addField(new Field(cm, name, type, def));
				}
				// Initialisation avec constructeur vide : Test t();
				else if (testChild((SimpleNode) decl_list_node.jjtGetChild(i), JJTDECLARATOR_SUFFIXES, "()")){
					VarDeclaration vd = new VarDeclaration(type, name);
					vd.setModifiers(cm);
					blockToReturn.addStatement(new NewInstance(vd));
				}
				// Cas d'un tableau (UNE DIMENSION)
				else if (testChild((SimpleNode) decl_list_node.jjtGetChild(i), JJTDECLARATOR_SUFFIXES, "[]")){
					VarDeclaration vd = new VarDeclaration(new TypeArray(type), name);
					vd.setModifiers(cm);

					SimpleNode dec_suf = getNodeFromId(node, JJTDECLARATOR_SUFFIXES);
					if (dec_suf.jjtGetNumChildren() > 0){
						Expression dim = (Expression) constructGoolAst((SimpleNode) dec_suf.jjtGetChild(0));
						List<Expression> le = new ArrayList<Expression>();
						le.add(dim);
						vd.setInitialValue(new ArrayNew(type, le, le));
					}
					blockToReturn.addStatement(vd);
				}
				// Initialisation avec constructeur non vide : Test t(1);
				else if (decl_list_node.jjtGetChild(i).jjtGetNumChildren() > 1 && 
						((SimpleNode) decl_list_node.jjtGetChild(i).jjtGetChild(1)).jjtGetId() == JJTEXPRESSION_LIST){					
					VarDeclaration vd = new VarDeclaration(type, name);
					vd.setModifiers(cm);
					NewInstance ni = new NewInstance(vd);
					List<Expression> le =  (List<Expression>) constructGoolAst((SimpleNode) decl_list_node.jjtGetChild(i).jjtGetChild(1));
					for (Expression e : le)
						ni.addParameter(e);
					blockToReturn.addStatement(ni);
				}
				// Initialisation simple : int i; ou int i=1;
				else{
					VarDeclaration vd = new VarDeclaration(type, name);
					vd.setInitialValue(def);					
					vd.setModifiers(cm);
					vd.addModifier(Modifier.PRIVATE);
					blockToReturn.addStatement(vd);
				}
			}
			return blockToReturn;
		}
		case JJTEXPRESSION_LIST : {
			List <Expression> listExp = new ArrayList<Expression>();
			for (int i=0;i<node.jjtGetNumChildren();i++)
				listExp.add((Expression) constructGoolAst((SimpleNode) node.jjtGetChild(i))); 
			return listExp;
		}
		case JJTFUNC_DECL_DEF : {
			if (node.jjtGetNumChildren() > 0){
				Block blockToAdd = (Block) constructGoolAst ((SimpleNode) node.jjtGetChild(0));

				if (blockToAdd != null)
					methActive.addStatement(blockToAdd);
			}
			break;
		}
		case JJTSTATEMENT_LIST : {			
			Block b = new Block();
			for (int i=0;i<node.jjtGetNumChildren();i++){
				Statement s = (Statement) constructGoolAst ((SimpleNode) node.jjtGetChild(i));				

				if (s == null){
					s = new ExpressionUnknown(TypeString.INSTANCE, "");
				}
				b.addStatement(s);
			}
			return b;
		}
		case JJTJUMP_STATEMENT :
			return new Return ((Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0)));
		case JJTADDITIVE_EXPRESSION :
		case JJTMULTIPLICATIVE_EXPRESSION :
			return getBinaryExpression (node, node.jjtGetNumChildren()-1, (List<?>) node.jjtGetValue());
		case JJTEQUALITY_EXPRESSION :
		case JJTRELATIONAL_EXPRESSION :
		case JJTLOGICAL_AND_EXPRESSION :
		case JJTLOGICAL_OR_EXPRESSION :
			return getBooleanExpression(node);
		case JJTUNARY_EXPRESSION : 
			if (node.jjtGetNumChildren() > 1){
				String sym = getValueFromNode(node, JJTUNARY_OPERATOR);
				Operator o = convertSymToOpe(sym);
				Expression expr = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(1));
				return new UnaryOperation(o, expr, TypeBool.INSTANCE, sym);
			}
			else
				return constructGoolAst((SimpleNode) node.jjtGetChild(0));
		case JJTCONSTANT :
			if (((String) node.jjtGetValue()).startsWith("'") || ((String) node.jjtGetValue()).endsWith("'"))
				return new Constant(convertIType((String) node.jjtGetType()),((String) node.jjtGetValue()).subSequence(1, ((String) node.jjtGetValue()).length()-1));
			else
				return new Constant(convertIType((String) node.jjtGetType()),node.jjtGetValue());
		case JJTID_EXPRESSION :
			if (node.jjtGetNumChildren() > 0){
				Identifier id = (Identifier) constructGoolAst((SimpleNode) node.jjtGetChild(0)); 
				return new FieldAccess(new TypeVar("typevar"), id, node.jjtGetValue().toString());
			}
			else
				return new Identifier (new TypeVar("typevar"),node.jjtGetValue().toString());
		case JJTSCOPE_OVERRIDE :
			return new Identifier (new TypeVar("typevar"),node.jjtGetValue().toString());
		case JJTPOSTFIX_EXPRESSION :
			if (node.jjtGetValue() != null && ((String) node.jjtGetValue()).compareTo("()") == 0){
				String name = getValueFromNode(node, JJTID_EXPRESSION);
				MethCall m = new MethCall(new TypeMethod("typemeth"), name);
				if (node.jjtGetNumChildren() > 1){
					m.addParameters((List<Expression>) constructGoolAst((SimpleNode) node.jjtGetChild(1)));
				}
				return m;
			}
			else if (node.jjtGetValue() != null && ((String) node.jjtGetValue()).compareTo("[]") == 0){
				return getListDim (node,node.jjtGetNumChildren()-1);
			}
			else
				return constructGoolAst((SimpleNode) node.jjtGetChild(0));
		case JJTPRIMARY_EXPRESSION :
			if (node.jjtGetNumChildren() == 0)
				return new Constant(TypeString.INSTANCE,((String) node.jjtGetValue()).subSequence(1, ((String) node.jjtGetValue()).length()-1));
		case JJTSHIFT_EXPRESSION :
			if (isFunctionPrint(node)){				
				stackClassActives.peek().addDependency(new SystemOutDependency());
				GoolCall gc = new SystemOutPrintCall();
				gc.addParameter(getExpressionPrint(node, 1));
				return gc;
			}
			return constructGoolAst((SimpleNode) node.jjtGetChild(0));
		case JJTCAST_EXPRESSION : {
			if (node.jjtGetNumChildren() > 1){
				Expression exp = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(1));
				IType type = convertIType(getValueFromNode(node,JJTDECLARATION_SPECIFIERS));				
				return new CastExpression(type, exp);
			}
			return constructGoolAst((SimpleNode) node.jjtGetChild(0));
		}
		case JJTNEW_EXPRESSION : {
			ClassNew cn = new ClassNew(convertIType(getValueFromNode(node,JJTDECLARATION_SPECIFIERS)));
			List <Expression> listExpr = (List <Expression>) constructGoolAst(getNodeFromId(node, JJTEXPRESSION_LIST));
			cn.addParameters(listExpr);
			return cn;
		}
		case JJTSELECTION_STATEMENT : {
			if (((String) node.jjtGetValue()).compareTo("if") == 0){
				Expression cond = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0));
				Statement thenSt = (Statement) constructGoolAst((SimpleNode) node.jjtGetChild(1));

				Statement elseSt;
				if (node.jjtGetNumChildren() == 3)
					elseSt = (Statement) constructGoolAst((SimpleNode) node.jjtGetChild(2));
				else
					elseSt=null;			
				return new If (cond,thenSt,elseSt);
			}
			// Cas du switch
			else if (((String) node.jjtGetValue()).compareTo("switch") == 0){
				List <Case> l=new ArrayList<Case>();
				Expression cond = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0));

				SimpleNode staListCase = getNodeFromId(node, JJTSTATEMENT_LIST);

				for (int j=0; j<staListCase.jjtGetNumChildren();j++){
					l.add((Case) constructGoolAst((SimpleNode) staListCase.jjtGetChild(j)));
				}
				return new Switch(cond, l);
			}
		}
		case JJTLABELED_STATEMENT : {
			Expression expCase = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0));
			Block stmtCase = (Block) constructGoolAst((SimpleNode) node.jjtGetChild(1));
			return new Case(expCase,stmtCase);
		}
		case JJTITERATION_STATEMENT : {
			Expression condWhile = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0));
			Statement stWhile = (Statement) constructGoolAst((SimpleNode) node.jjtGetChild(1));

			return new While (condWhile,stWhile);
		}
		case JJTASSIGNMENT_EXPRESSION :
			if (node.jjtGetNumChildren()>1){
				Node varAss = (Node) constructGoolAst((SimpleNode) node.jjtGetChild(0));
				Expression expAss = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(1));
				return new Assign(varAss, expAss);
			}
			else
				return constructGoolAst((SimpleNode) node.jjtGetChild(0));
		case JJTTRY_BLOCK : {
			Block block = (Block) constructGoolAst((SimpleNode) node.jjtGetChild(0)); 
			Block finallyBlock = new Block();
			List<Catch> catches = new ArrayList<Catch>();

			for (int i=1;i<node.jjtGetNumChildren();i++){
				if (((String) ((SimpleNode) node.jjtGetChild(i)).jjtGetValue()).compareTo("finally") == 0)
					finallyBlock = (Block) constructGoolAst((SimpleNode) node.jjtGetChild(i));
				else
					catches.add((Catch) constructGoolAst((SimpleNode) node.jjtGetChild(i)));
			}			
			return new Try(catches, block, finallyBlock);
		}
		case JJTHANDLER : {
			if (((String) node.jjtGetValue()).compareTo("catch") == 0){
				IType type = convertIType(getValueFromNode(node,JJTDECLARATION_SPECIFIERS));				
				String name = getValueFromNode((SimpleNode) node,JJTDIRECT_DECLARATOR);				
				Block block = (Block) constructGoolAst((SimpleNode) node.jjtGetChild(1));
				return new Catch(new VarDeclaration(type, name), block);
			}
			else{
				return (Block) constructGoolAst((SimpleNode) node.jjtGetChild(0));
			}
		}
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
		if (node.jjtGetId() == nodeType && nodeType == JJTDECLARATION_SPECIFIERS){
			String t1 = getValueFromNode (node, JJTBUILTIN_TYPE_SPECIFIER);
			String t2 = getValueFromNode (node, JJTQUALIFIED_ID);
			if (t1 != null) return t1; else return t2;
		}
		else if (node.jjtGetId() == nodeType && (nodeType == JJTDIRECT_DECLARATOR || nodeType == JJTFUNCTION_DIRECT_DECLARATOR)){
			return (String) ((SimpleNode) node.jjtGetChild(0)).jjtGetValue();
		}
		else if (node.jjtGetId() == nodeType)
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

	private SimpleNode getNodeFromId (SimpleNode node, int nodeType){
		if (node.jjtGetId() == nodeType)
			return node;
		else{
			for (int i=0;i<node.jjtGetNumChildren();i++){
				SimpleNode nodeTypeChild = getNodeFromId((SimpleNode) node.jjtGetChild(i), nodeType);
				if (nodeTypeChild != null)
					return nodeTypeChild;
			}
		}
		return null;
	}

	// Fonction de convertion entre les type c++ et les type GOOL
	private IType convertIType (String type){		
		if (type == null)
			return null;

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
		else return new TypeClass(type);
	}

	private Expression getBinaryExpression (SimpleNode node, int i, List<?> listOpe){
		if (listOpe == null)
			return (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0));
		else if (i == 0)
			return (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(i));
		else {
			String sym = (String) listOpe.get(i-1);
			Expression e1 = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(i));
			return new BinaryOperation(convertSymToOpe(sym),getBinaryExpression (node, i-1, listOpe),e1,e1.getType(),sym);			
		}		
	}

	private Expression getListDim (SimpleNode node, int i){
		if (i == 1){
			Expression id = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0));
			Expression index = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(1));
			return new ArrayAccess(id, index);
		}
		else{
			Expression index = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(i));
			return new ArrayAccess(getListDim(node, i-1), index);
		}
	}

	private Expression getBooleanExpression (SimpleNode node){
		if (node.jjtGetNumChildren() > 1){
			// 1 < 2 < 3 passe au parser mais se transforme en 1 < 2 danq gool
			Operator o = convertSymToOpe((String) node.jjtGetValue());
			Expression left = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0));
			Expression right = (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(1));			
			return new BinaryOperation(o, left, right, TypeBool.INSTANCE, (String) node.jjtGetValue());
		}
		else
			return (Expression) constructGoolAst((SimpleNode) node.jjtGetChild(0));
	}

	private Operator convertSymToOpe (String sym){
		if (sym.compareTo("+") == 0){return Operator.PLUS;}
		else if (sym.compareTo("-") == 0){return Operator.MINUS;}
		else if (sym.compareTo("*") == 0){return Operator.MULT;}
		else if (sym.compareTo("/") == 0){return Operator.DIV; }
		else if (sym.compareTo("%") == 0){return Operator.UNKNOWN;}
		else if (sym.compareTo("==") == 0){return Operator.EQUAL;}
		else if (sym.compareTo("!=") == 0){return Operator.NOT_EQUAL;}
		else if (sym.compareTo(">") == 0){return Operator.GT;}
		else if (sym.compareTo("<") == 0){return Operator.LT;}
		else if (sym.compareTo(">=") == 0){return Operator.GEQ;}
		else if (sym.compareTo("<=") == 0){return Operator.LEQ;}
		else if (sym.compareTo("&&") == 0){return Operator.AND;}
		else if (sym.compareTo("||") == 0){return Operator.OR;}
		else if (sym.compareTo("!") == 0){return Operator.NOT;}
		else return Operator.UNKNOWN;
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

	private boolean testChild (SimpleNode node, int n, String value){
		if (node.jjtGetId() == n){
			if (value == null)
				return true;
			else if (node.jjtGetValue() != null && ((String) node.jjtGetValue()).compareTo(value) == 0)
				return true;
		}
		boolean toReturn = false;
		for (int i=0;i<node.jjtGetNumChildren();i++)			
			toReturn |= testChild ((SimpleNode) node.jjtGetChild(i), n, value);
		return toReturn;
	}

	private boolean testChild (SimpleNode node, int n){
		return testChild (node,n, null);
	}

	private Modifier convertModToGoolMod (String mod){
		if (mod.compareTo("const") == 0){return Modifier.FINAL;}
		else if (mod.compareTo("volatile") == 0){return Modifier.VOLATILE;}
		else if (mod.compareTo("static") == 0){return Modifier.STATIC;}
		else if (mod.compareTo("public") == 0){return Modifier.PUBLIC;}
		else if (mod.compareTo("protected") == 0){return Modifier.PROTECTED;}
		else if (mod.compareTo("private") == 0){return Modifier.PRIVATE;}
		else if (mod.compareTo("virtual") == 0){
			stackClassActives.peek().addModifier(Modifier.ABSTRACT);
			return Modifier.ABSTRACT;
		}
		else return null;
	}

	// node de type DECLARATION_SPECIFIERS en paramètre
	private Collection<Modifier> getCollectionModifiers (SimpleNode node){
		Collection<Modifier> toReturn = new ArrayList<Modifier>();
		for (int i=0;((SimpleNode) node.jjtGetChild(i)).jjtGetId() == JJTTYPE_MODIFIERS;i++){
			if (((SimpleNode) node.jjtGetChild(i)).jjtGetValue() != null){
				Modifier m = convertModToGoolMod((String) ((SimpleNode) node.jjtGetChild(i)).jjtGetValue());
				if (m != null)
					toReturn.add(m);
			}
			else {			
				Modifier m = convertModToGoolMod((String) ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetValue());
				if (m != null)
					toReturn.add(m);
			}
		}
		return toReturn;
	}

	private void setClassIsInterface (String name){
		Iterator<ClassDef> itr = goolClasses.iterator();
		while(itr.hasNext()) {
			ClassDef element = (ClassDef) itr.next();
			if (element.getName().compareTo(name) == 0){
				element.setIsInterface(true);
				Collection<Modifier> c = element.getModifiers();
				c.remove(Modifier.ABSTRACT);
				element.setModifiers(c);
				return;
			}
		}
	}
}