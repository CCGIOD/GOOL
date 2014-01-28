package gool.recognizer.cpp;

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
import gool.ast.core.CompoundAssign;
import gool.ast.core.Constant;
import gool.ast.core.Constructor;
import gool.ast.core.Dependency;
import gool.ast.core.DoWhile;
import gool.ast.core.Expression;
import gool.ast.core.Field;
import gool.ast.core.FieldAccess;
import gool.ast.core.For;
import gool.ast.core.GoolCall;
import gool.ast.core.Identifier;
import gool.ast.core.If;
import gool.ast.core.MainMeth;
import gool.ast.core.MemberSelect;
import gool.ast.core.Meth;
import gool.ast.core.MethCall;
import gool.ast.core.Modifier;
import gool.ast.core.NewInstance;
import gool.ast.core.Node;
import gool.ast.core.Operator;
import gool.ast.core.RecognizedDependency;
import gool.ast.core.Return;
import gool.ast.core.Statement;
import gool.ast.core.Switch;
import gool.ast.core.This;
import gool.ast.core.Try;
import gool.ast.core.UnaryOperation;
import gool.ast.core.UnrecognizedDependency;
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
import gool.ast.type.TypeGoolLibraryClass;
import gool.ast.type.TypeInt;
import gool.ast.type.TypeMethod;
import gool.ast.type.TypeString;
import gool.ast.type.TypeVar;
import gool.ast.type.TypeVoid;
import gool.generator.GeneratorHelper;
import gool.generator.common.Platform;
import gool.generator.cpp.CppPlatform;
import gool.generator.java.JavaPlatform;
import gool.generator.python.PythonPlatform;
import gool.parser.cpp.CPPParser;
import gool.parser.cpp.Token;
import gool.parser.cpp.nodes.*;
import gool.recognizer.common.GoolLibraryClassAstBuilder;
import gool.recognizer.common.RecognizerMatcher;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * The CppRecognizer does the work of converting abstract Cpp to abstract GOOL.
 */
@SuppressWarnings("unchecked")
public class CppRecognizer implements CPPParserVisitor, CPPParserTreeConstants {

	/**
	 * The option to print or not the Cpp AST in the console.
	 */
	public static final boolean OPTION_PRINT_AST = false;

	/**
	 * The list of AST (one per input file) produced by the Cpp parser.
	 */
	private List<SimpleNode> AST;

	/**
	 * The collection of GOOL classes (ie the result of the recognization).
	 */
	private Collection<ClassDef> goolClasses = new ArrayList<ClassDef> ();

	/**
	 * Getter of the collection.
	 */
	public final Collection<ClassDef> getGoolClasses() {
		return goolClasses;
	}

	/**
	 * The global variable used to print the type of an error when it's possible.
	 */
	private String errorType = null;

	/**
	 * Setter of the errorType.
	 */
	private void setErrorType (String str){
		this.errorType=str;
	}

	/**
	 * The list of cpp librairies that we don't check (ex: iostream).
	 */
	private List<String> uncheckedLib;

	/**
	 * The function used to init the list of unchecked librairies.
	 */
	private void initUncheckedLib (){
		this.uncheckedLib=new ArrayList<String>();
	}

	/**
	 * The default platform used to specify the Target Language, which will be
	 * annotated in the newly created classes.
	 */
	private Platform defaultPlatform = JavaPlatform.getInstance();
	//private Platform defaultPlatform = CppPlatform.getInstance();
	//private Platform defaultPlatform = PythonPlatform.getInstance();

	/**
	 * The stack of actives classes (used to know in which class insert a method or a field).
	 */
	private Stack<ClassDef> stackClassActives = new Stack<ClassDef>();

	/**
	 * The activ method (used to know in which method insert a statement).
	 */
	private Meth methActive = null;

	/**
	 * The default modifier.
	 */
	private Modifier accesModifierActive = Modifier.PUBLIC;

	/**
	 * The cache to know what is imported.
	 */
	private Collection<String> importCache = new ArrayList<String>();


	/**
	 * The constructor of CppRecognizer (call the parser).
	 */
	public CppRecognizer (){
		this.AST=CPPParser.getCppAST();
		initUncheckedLib();
	}

	/**
	 * Getter of the AST.
	 */
	public List<SimpleNode> getAST (){
		return AST;
	}


	/**
	 * Main method : translate from the input directory to the output directory.
	 */
	public static void main (String args[]){

		// Init the recognizer
		CppRecognizer cppr = new CppRecognizer();
		List<SimpleNode> ast = cppr.getAST();

		// Checking the parsing and print the ASTs if the option is set on true
		if (ast == null){return;}		
		if (OPTION_PRINT_AST)
			for (SimpleNode a : ast)
				a.dump("");

		// Init the recognizerMatcher (for librairies)
		RecognizerMatcher.init("cpp");

		// Start the visit from the root (TRANSLATION_UNIT)
		for (SimpleNode a : ast)
			cppr.visit(a, 0);

		// Print the collection of ClassDef
		try {GeneratorHelper.printClassDefs(cppr.getGoolClasses());}
		catch (FileNotFoundException e) {}
	}

	/**
	 * Print an error message from the first and the last token of a part of the input code.
	 * A prefix can be add in some case.
	 */
	private void getUnrecognizedPart(Token begin, Token end, String prefix){
		String toPrint="";
		while (!(begin.beginLine == end.beginLine && begin.beginColumn == end.beginColumn)){
			// Uncomment the following code to print the sequence with the original indent :
			/*toPrint+=begin.image;
			boolean sautDeLigne=false;
			for (int i=0;i<(begin.next.beginLine-begin.endLine);i++){
				toPrint+="\n";
				sautDeLigne=true;
			}
			if (sautDeLigne)
				for (int i=0;i<begin.next.beginColumn-1;i++)
					toPrint+=" ";
			else
				for (int i=0;i<(begin.next.beginColumn-begin.endColumn-1);i++)
					toPrint+=" ";*/
			toPrint+=begin.image+" ";
			begin=begin.next;
		}
		toPrint+=end.image;
		if (errorType == null)
			System.out.println("WARNING: \" "+prefix+toPrint+" \" in "+getLocationError()+" (line "+begin.beginLine+") was ignored because not recognized by GOOL!");
		else{ 
			System.out.println("WARNING: \" "+prefix+toPrint+" \" in "+getLocationError()+" (line "+begin.beginLine+") was ignored because not recognized ("+errorType+") by GOOL!");
			errorType=null;
		}
	}

	/**
	 * @see private void getUnrecognizedPart(Token begin, Token end, String prefix);
	 * No prefix.
	 */
	private void getUnrecognizedPart(Token begin, Token end){
		getUnrecognizedPart(begin, end, "");
	}

	/**
	 * @see private void getUnrecognizedPart(Token begin, Token end, String prefix);
	 * No first/last token but a string instead.
	 */
	private void setUnrecognizedPart(String toPrint, Token begin){
		System.out.println("WARNING: \" "+toPrint+" \" in "+getLocationError()+" (line "+begin.beginLine+") was ignored because not recognized by GOOL!");
	}

	/**
	 * Give the current method or class. Used when an error is printed.
	 */	
	private String getLocationError (){
		if (methActive != null)
			return "the function "+methActive.getName();
		else
			return "the class "+stackClassActives.peek().getName();
	}

	/**
	 * This method is used to call the visit on a specific children.
	 * If the children is not where he is suppose to be, the method return null.
	 * @param typeNode The type of node wanted
	 * @param pos The position of the node wanted
	 * @return
	 */
	private Object returnChild (int typeNode, SimpleNode node, int pos, Object data){
		if (node.jjtGetNumChildren() < pos+1)
			return null;
		else if (node.jjtGetChild(pos).jjtGetId() != typeNode)
			return null;
		else
			return visit((SimpleNode) node.jjtGetChild(pos), data);
	}

	/**
	 * This method give a class name like "Class" from a string like "class.cpp".
	 */
	private String createClassNameFromFilename(Object o){
		String filename = (String) o;
		String className = filename.split("\\.")[0];		
		return className.substring(0, 1).toUpperCase() + className.substring(1);
	}

	/**
	 * This method allow to check if a node have a specific child in his children.
	 * It's sometimes the only way of identifying the type of GOOL node we have to build.
	 */
	private boolean testChild (SimpleNode node, int n, String value){
		if (node.jjtGetId() == n){
			if (value == null)
				return true;
			else if (node.jjtGetValue() != null && node.jjtGetValue().toString().compareTo(value) == 0)
				return true;
		}
		boolean toReturn = false;
		for (int i=0;i<node.jjtGetNumChildren();i++)			
			toReturn |= testChild ((SimpleNode) node.jjtGetChild(i), n, value);
		return toReturn;
	}

	/**
	 * @see private boolean testChild (SimpleNode node, int n, String value);
	 * No verification of value.
	 */
	private boolean testChild (SimpleNode node, int n){
		return testChild (node,n, null);
	}

	/**
	 * Convertion function of type : string like "int" to GOOL type like TypeInt.INSTANCE.
	 */
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
		else return null;
	}

	/**
	 * Convertion function of modifier : string like "const" to GOOL type like Modifier.FINAL.
	 */
	private Modifier convertModToGoolMod (String mod){
		if (mod.compareTo("const") == 0){return Modifier.FINAL;}
		else if (mod.compareTo("volatile") == 0){return Modifier.VOLATILE;}
		else if (mod.compareTo("static") == 0){return Modifier.STATIC;}
		else if (mod.compareTo("public") == 0){return Modifier.PUBLIC;}
		else if (mod.compareTo("protected") == 0){return Modifier.PROTECTED;}
		else if (mod.compareTo("private") == 0){return Modifier.PRIVATE;}
		else if (mod.compareTo("virtual") == 0){
			// If abstract is detected, the class is automatically set to abstract
			stackClassActives.peek().addModifier(Modifier.ABSTRACT);
			return Modifier.ABSTRACT;
		}
		else return null;
	}

	/**
	 * This recursive function is used to build a binary expression (ex: 1+2-3*4).
	 * @param node The first node of the expression in the Cpp AST
	 * @param listOpe The list of operator (ex: in 1+2-3*4, it's [+,-,*])
	 */
	private Expression getBinaryExpression (SimpleNode node, int i, List<?> listOpe, Object data){
		if (listOpe == null)
			return (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
		else if (i == 0)
			return (Expression) visit((SimpleNode) node.jjtGetChild(i),data);
		else {
			String sym = (String) listOpe.get(i-1);
			Expression e1 = (Expression) visit((SimpleNode) node.jjtGetChild(i),data);
			if (e1 == null){return null;}
			return new BinaryOperation(convertSymToOpe(sym),getBinaryExpression (node, i-1, listOpe, data),e1,e1.getType(),sym);			
		}		
	}

	/**
	 * Convertion function of a symbol : string like "+" to GOOL type like Operator.PLUS.
	 * NB: the GOOL operator like Operator.DECIMALPLUS are not used.
	 */
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

	/**
	 * This function is used to check if an expression is a print call.
	 * The expression need to be like : cout (<< ...)+ << endl;
	 */
	private boolean isFunctionPrint (SimpleNode node, Object data){
		if (node.jjtGetNumChildren() < 3)
			return false;

		boolean testAdd=true;
		for (int i=0;i<node.jjtGetNumChildren();i++)
			if (node.jjtGetChild(i).jjtGetId() != JJTADDITIVE_EXPRESSION)
				testAdd=false;
		if (!testAdd){return false;}

		try {
			String cL = ((Identifier) visit((SimpleNode) node.jjtGetChild(0),data)).getName();
			String cR = ((Identifier) visit((SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1),data)).getName();
			return cL.compareTo("cout") == 0 && cR.compareTo("endl") == 0;
		} catch (Exception e){return false;}
	}

	/**
	 * This function build the expression of a print call.
	 * The first element (cout) and the last (endl) are ignored to do this.
	 */
	private Expression getExpressionPrint (SimpleNode node, int i, Object data){
		if (i == node.jjtGetNumChildren()-2)
			return (Expression) visit((SimpleNode) node.jjtGetChild(i),data);
		else{
			Expression e1 = (Expression) visit((SimpleNode) node.jjtGetChild(i),data);
			return new BinaryOperation(Operator.PLUS,e1,getExpressionPrint (node, i+1,data),e1.getType(),"+");
		}
	}

	/**
	 * This recursive function is used to build a boolean expression.
	 */	
	private Expression getBooleanExpression (SimpleNode node, Object data){
		if (node.jjtGetNumChildren() == 2){
			Operator o = convertSymToOpe((String) node.jjtGetValue());
			Expression left = (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			Expression right = (Expression) visit((SimpleNode) node.jjtGetChild(1),data);			
			if (left == null || right == null){return null;}			
			return new BinaryOperation(o, left, right, TypeBool.INSTANCE, (String) node.jjtGetValue());
		}
		else if (node.jjtGetNumChildren() == 1){
			return (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
		}
		else return null;
	}

	/**
	 * Check if we are in the case of a declaration of an array.
	 */
	private boolean testDeclarationArray (SimpleNode node){
		if (testChild(node, JJTDECLARATOR_SUFFIXES))
			return (!testChild(node, JJTDECLARATOR_SUFFIXES,"()"));
		return false;
	}

	/**
	 * This function give the list of the dimension of an array.
	 */
	private Expression getListDim (SimpleNode node, int i, Object data){
		if (i == 1){
			Expression id = (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			Expression index = (Expression) visit((SimpleNode) node.jjtGetChild(1),data);
			if (id == null || index == null){return null;}
			return new ArrayAccess(id, index);
		}
		else{
			Expression index = (Expression) visit((SimpleNode) node.jjtGetChild(i),data);
			if (index == null){return null;}
			return new ArrayAccess(getListDim(node, i-1, data), index);
		}
	}

	/**
	 * Setter of the active method. This function is used to check if the exist in the classes.
	 */
	private Object setMethActive (String className, String name){
		Iterator<ClassDef> it = goolClasses.iterator();
		while (it.hasNext()){
			ClassDef tmp = it.next();
			if (tmp.getName().compareTo(className) == 0){

				Meth m = tmp.getMethod(name);
				if (m == null){return null;}
				else
					methActive=m;

				return 0;
			}
		}
		return null;
	}

	/**
	 * This function is used to test if a class already exists.
	 */
	private ClassDef classExists (String className){
		Iterator<ClassDef> it = goolClasses.iterator();
		while (it.hasNext()){
			ClassDef tmp = it.next();
			if (tmp.getName().compareTo(className) == 0){
				return tmp;
			}
		}
		return null;
	}

	/**
	 * The following function are the implementation of the CPPParserVisitor
	 * interface provided by the Cpp parser.
	 */

	@Override
	public Object visit(SimpleNode node, Object data) {
		return node.jjtAccept(this, data);
	}

	@Override
	public Object visit(CPPAST_TRANSLATION_UNIT node, Object data) {
		ClassDef unitaryClass = classExists(createClassNameFromFilename(node.jjtGetValue()));

		// The class is added to goolClasses when it doesn't exist yet
		if (unitaryClass == null){
			unitaryClass = new ClassDef(Modifier.PUBLIC, createClassNameFromFilename(node.jjtGetValue()), defaultPlatform);
			goolClasses.add(unitaryClass);
		}
		stackClassActives.push(unitaryClass);
		node.childrenAccept(this, data);
		return null;
	}

	@Override
	public Object visit(CPPAST_EXTERNAL_DECLARATION node, Object data) {
		for (int i=0;i<node.jjtGetNumChildren();i++){
			if (node.jjtGetChild(i).jjtAccept(this, data) == null)
				// If an external declaration is not correctely recognized, an error is printed
				getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
		}		
		return null;
	}

	@Override
	public Object visit(CPPAST_FUNCTION_DEFINITION node, Object data) {
		String className = (String) returnChild(JJTFUNCTION_DECLARATOR, node, 1, "GET_SCOPE");

		// If the function definition is prefixed by <ClassName>:: , the active method is change
		if (className != null){
			String name = (String) returnChild(JJTFUNCTION_DECLARATOR, node, 1, "GET_NAME");
			if (name == null){return null;}
			if (setMethActive(className.replaceAll("::", ""),name) == null){return null;}
			visit((CPPAST_FUNC_DECL_DEF) node.jjtGetChild(2), data);
			methActive=null;
			return 0;
		}

		IType type = (IType) returnChild(JJTDECLARATION_SPECIFIERS, node, 0, "GET_TYPE");
		if (type == null){return null;}

		// Test of '*' (only char* is allowed)
		if (node.jjtGetChild(1).jjtGetChild(0).jjtGetId()==JJTPTR_OPERATOR && node.jjtGetChild(1).jjtGetChild(0).jjtGetValue() != null){
			if (type.equals(TypeChar.INSTANCE))
				type=TypeString.INSTANCE;
			else{
				setErrorType("pointer");
				return null;
			}
		}

		String name = (String) returnChild(JJTFUNCTION_DECLARATOR, node, 1, "GET_NAME");
		if (name == null){return null;}

		Collection <Modifier> cm = (Collection <Modifier>) visit((CPPAST_DECLARATION_SPECIFIERS) node.jjtGetChild(0), "GET_MODIFIERS");
		if (cm == null){setErrorType("modifiers"); return null;}

		Meth m;
		List<VarDeclaration> listVD=null;
		if (testChild(node, JJTPARAMETER_LIST)){
			listVD = (List<VarDeclaration>) returnChild(JJTFUNCTION_DECLARATOR, node, 1, "GET_PARAMS");
		}
		if (listVD == null){listVD=new ArrayList<VarDeclaration>();}

		// Checking if it's the main method or not
		if (name.compareTo("main") == 0 && type == TypeInt.INSTANCE && ((listVD.size() == 2 
				&& listVD.get(0).getType() == TypeInt.INSTANCE && listVD.get(1).getType() == TypeString.INSTANCE) || (listVD.size() == 0))){ 
			m = new MainMeth();
		}
		else{
			m = new Meth(type, name);
			for (VarDeclaration vd : listVD)
				m.addParameter(vd);

			// Throw are set in GOOL but not generated in java output
			if (testChild(node, JJTEXCEPTION_SPEC)){
				List<IType> lt = (List<IType>) returnChild(JJTFUNCTION_DECLARATOR, node, 1, "GET_EXCEP");
				if (lt != null)
					for (IType t : lt)
						m.addThrowStatement(t);
			}
		}

		m.setModifiers(cm);
		m.addModifier(accesModifierActive);
		stackClassActives.peek().addMethod(m);
		methActive=m;
		visit((CPPAST_FUNC_DECL_DEF) node.jjtGetChild(2), data);
		methActive=null;
		return 0;
	}

	@Override
	public Object visit(CPPAST_FUNC_DECL_DEF node, Object data) {
		if (node.jjtGetNumChildren() > 0){
			Block blockToAdd = (Block) visit((SimpleNode) node.jjtGetChild(0),data);

			if (blockToAdd != null)
				methActive.addStatement(blockToAdd);
		}
		return 0;			
	}

	@Override
	public Object visit(CPPAST_LINKAGE_SPECIFICATION node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_DECLARATION node, Object data) {
		// Test of unimplemented part of the parser
		if (testChild(node, JJTSTORAGE_CLASS_SPECIFIER,"UNKNOW"))
			return null;

		// Case of a class declaration
		if (testChild(node, JJTCLASS_SPECIFIER))	
			return returnChild(JJTDECLARATION_SPECIFIERS, node, 0, data);

		// Case of a varaible declaration
		else if (testChild(node, JJTDECLARATION_SPECIFIERS) && testChild(node, JJTINIT_DECLARATOR_LIST)){
			Block blockToReturn = new Block();
			boolean printErrorDef=false;

			IType type = (IType) returnChild(JJTDECLARATION_SPECIFIERS, node, 0, data);
			if (type == null){return null;}
			if (testChild((SimpleNode) node.jjtGetChild(1), JJTPTR_OPERATOR,"*")){
				if (type.equals(TypeChar.INSTANCE))
					type=TypeString.INSTANCE;
				else{
					setErrorType("pointer");
					return null;
				}
			}

			Collection <Modifier> cm = (Collection <Modifier>) visit((CPPAST_DECLARATION_SPECIFIERS) node.jjtGetChild(0), "GET_MODIFIERS");
			if (cm == null || cm.contains(Modifier.ABSTRACT)){cm = new ArrayList<Modifier>();}

			SimpleNode nodeDecList = (SimpleNode) node.jjtGetChild(1);
			for(int i=0;i<nodeDecList.jjtGetNumChildren();i++){
				String name = (String) returnChild(JJTINIT_DECLARATOR, nodeDecList, i, "GET_NAME");						
				if (name == null){return null;}

				// Looking for an initializer
				Expression def = null;
				if (nodeDecList.jjtGetChild(i).jjtGetNumChildren() > 1 && nodeDecList.jjtGetChild(i).jjtGetChild(1).jjtGetId() == JJTINITIALIZER){
					def = (Expression) visit((SimpleNode) nodeDecList.jjtGetChild(i).jjtGetChild(1), data);
					if (def == null)
						printErrorDef=true;
				}

				IType saveType = type;
				if (testDeclarationArray((SimpleNode) nodeDecList.jjtGetChild(i))){
					int dim = (Integer) visit((SimpleNode) nodeDecList.jjtGetChild(i),"GET_DIM");

					for (int j=0;j<dim;j++){
						type=new TypeArray(type);
					}					
					List<Expression> le = (List<Expression>) visit((SimpleNode) nodeDecList.jjtGetChild(i),"GET_DIM_VAL");
					if (le.size() > 0){
						if (dim > 1){setErrorType("init. with dimension > 1 impossible"); return null;} // Maximum un dimension sinon problème de génération de code ...
						def=new ArrayNew(saveType, le, le);				
					}					
				}				

				// Declaration of field
				if (node.jjtGetParent().jjtGetId() == JJTEXTERNAL_DECLARATION){
					cm.add(Modifier.PRIVATE);
					stackClassActives.peek().addField(new Field(cm, name, type, def));
				}	

				// Initialisation with a constructor with parameter(s) like MyClass mc(1);
				else if (nodeDecList.jjtGetChild(i).jjtGetNumChildren() > 1 && nodeDecList.jjtGetChild(i).jjtGetChild(1).jjtGetId() == JJTEXPRESSION_LIST){					
					if (testChild(nodeDecList, JJTINITIALIZER))
						return null;
					VarDeclaration vd = new VarDeclaration(type, name);
					vd.setModifiers(cm);
					NewInstance ni = new NewInstance(vd);
					List<Expression> le =  (List<Expression>) visit((SimpleNode) nodeDecList.jjtGetChild(i).jjtGetChild(1),data);
					if (le == null){return null;}
					for (Expression e : le)
						ni.addParameter(e);
					blockToReturn.addStatement(ni);
				}

				// Initialisation with a constructeur without parameter like Test t() or with a new call like Test t = new Test (...);
				else if (type instanceof TypeClass){
					if (testChild(nodeDecList, JJTINITIALIZER))
						return null;
					VarDeclaration vd = new VarDeclaration(type, name);
					vd.setModifiers(cm);
					NewInstance ni = new NewInstance(vd);
					if (nodeDecList.jjtGetChild(i).jjtGetNumChildren() > 1 && nodeDecList.jjtGetChild(i).jjtGetChild(1).jjtGetId() == JJTINITIALIZER && def != null)
						if (!(def instanceof ClassNew))
							return null;
						else
							ni.addParameters(((ClassNew) def).getParameters());
					blockToReturn.addStatement(ni);
				}

				// Simple initialisation like int i; ou int i=1;
				else{
					VarDeclaration vd = new VarDeclaration(type, name);
					vd.setInitialValue(def);					
					vd.setModifiers(cm);
					blockToReturn.addStatement(vd);
				}

				type=saveType;
				if (printErrorDef){
					getUnrecognizedPart(((SimpleNode) nodeDecList.jjtGetChild(i).jjtGetChild(1)).jjtGetFirstToken(), ((SimpleNode) nodeDecList.jjtGetChild(i).jjtGetChild(1)).jjtGetLastToken(),"= ");
					printErrorDef=false;
				}
			}

			return blockToReturn;
		}		
		return null;
	}

	@Override
	public Object visit(CPPAST_TYPE_MODIFIERS node, Object data) {
		if (node.jjtGetValue() != null){
			return convertModToGoolMod(node.jjtGetValue().toString());
		}
		else if (node.jjtGetNumChildren() != 0)
			return node.jjtGetChild(0).jjtAccept(this, data);
		else{
			getUnrecognizedPart(node.jjtGetFirstToken(), node.jjtGetLastToken());
			return null;
		}
	}

	@Override
	public Object visit(CPPAST_DECLARATION_SPECIFIERS node, Object data) {
		if (data.toString().compareTo("GET_MODIFIERS") == 0){
			Collection <Modifier> toReturn = new ArrayList<Modifier>();
			for (int i=0;node.jjtGetChild(i).jjtGetId() == JJTTYPE_MODIFIERS;i++){
				Modifier m = (Modifier) visit((SimpleNode) node.jjtGetChild(i), data);
				if (m == null)
					getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
				else
					toReturn.add(m);
			}
			return toReturn;			
		}
		else{
			int d=0;
			while (node.jjtGetChild(d).jjtGetId() == JJTTYPE_MODIFIERS){d++;}
			if (testChild(node, JJTCLASS_SPECIFIER))
				return returnChild(JJTCLASS_SPECIFIER, node, d, data);
			else if ((testChild(node, JJTBUILTIN_TYPE_SPECIFIER))){
				return returnChild(JJTBUILTIN_TYPE_SPECIFIER, node, d, data);
			}
			else if ((testChild(node, JJTQUALIFIED_TYPE))){
				return returnChild(JJTQUALIFIED_TYPE, node, d, data);
			}
		}
		return null;
	}

	@Override
	public Object visit(CPPAST_SIMPLE_TYPE_SPECIFIER node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_SCOPE_OVERRIDE_LOOKAHEAD node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_SCOPE_OVERRIDE node, Object data){
		if (node.jjtGetValue() == null || node.jjtGetNumChildren() > 0)
			return null;
		return node.jjtGetValue().toString();
	}

	@Override
	public Object visit(CPPAST_QUALIFIED_ID node, Object data) {
		if (data.toString().compareTo("GET_SCOPE") == 0)
			return returnChild(JJTSCOPE_OVERRIDE, node, 0, data);
		return node.jjtGetValue();
	}

	@Override
	public Object visit(CPPAST_PTR_TO_MEMBER node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_QUALIFIED_TYPE node, Object data) {
		String cppClass = node.jjtGetChild(0).jjtGetValue().toString();
		String goolClass = RecognizerMatcher.matchClass(cppClass);
		if (goolClass != null) {
			// If there is a librairy, a dependency is added
			if(!importCache.contains(goolClass)){
				stackClassActives.peek().addDependency(new RecognizedDependency(goolClass));
				importCache.add(goolClass);
			}
			return new TypeGoolLibraryClass(goolClass);
		}
		return new TypeClass(node.jjtGetChild(0).jjtGetValue().toString());
	}

	@Override
	public Object visit(CPPAST_TYPE_QUALIFIER node, Object data) {
		return convertModToGoolMod(node.jjtGetValue().toString());
	}

	@Override
	public Object visit(CPPAST_STORAGE_CLASS_SPECIFIER node, Object data) {
		if (node.jjtGetValue() == null){
			return null;
		}
		return convertModToGoolMod(node.jjtGetValue().toString());
	}

	@Override
	public Object visit(CPPAST_BUILTIN_TYPE_SPECIFIER node, Object data) {
		return convertIType(node.jjtGetValue().toString()); 
	}

	@Override
	public Object visit(CPPAST_INIT_DECLARATOR_LIST node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_INIT_DECLARATOR node, Object data) {
		if (data.toString().compareTo("GET_NAME") == 0 || data.toString().startsWith("GET_DIM")){
			return returnChild(JJTDECLARATOR, node, 0, data);
		}
		else if (data.toString().compareTo("GET_INIT") == 0){
			return returnChild(JJTINITIALIZER, node, 1, data);
		}
		return null;
	}

	@Override
	public Object visit(CPPAST_CLASS_HEAD node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_CLASS_SPECIFIER node, Object data) {
		if (node.jjtGetValue() == null){
			setErrorType("struct/union impossible"); 
			return null;
		}

		ClassDef cd = classExists(node.jjtGetValue().toString());

		if (cd == null){
			cd = new ClassDef(Modifier.PUBLIC, node.jjtGetValue().toString(), defaultPlatform);

			if (testChild((SimpleNode) node.jjtGetParent(), JJTSTORAGE_CLASS_SPECIFIER))
				cd.addModifier(Modifier.STATIC);

			stackClassActives.push(cd);
			if (testChild(node, JJTBASE_CLAUSE)){
				if (returnChild(JJTBASE_CLAUSE, node, 0, data) == null){setErrorType("inheritance"); return null;}
			}
			goolClasses.add(cd);
			node.childrenAccept(this, data);
			stackClassActives.pop();
			return 0;
		}
		else {
			stackClassActives.push(cd);
			node.childrenAccept(this, data);
			stackClassActives.pop();
			return 0;
		}
	}

	@Override
	public Object visit(CPPAST_BASE_CLAUSE node, Object data) {
		if (node.jjtGetNumChildren() == 1){
			String name = ((SimpleNode) node.jjtGetChild(0)).jjtGetValue().toString();
			stackClassActives.peek().setParentClass(new TypeClass(name));
			return 0;
		}
		else{
			return null;
		}
	}

	@Override
	public Object visit(CPPAST_BASE_SPECIFIER node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_ACCESS_SPECIFIER node, Object data) {
		return node.jjtGetValue().toString();
	}

	@Override
	public Object visit(CPPAST_MEMBER_DECLARATION node, Object data) {
		if (testChild(node, JJTACCESS_SPECIFIER)){
			accesModifierActive=convertModToGoolMod((String) returnChild(JJTACCESS_SPECIFIER, node, 0, data));
		}
		else if (testChild(node, JJTDTOR_DEFINITION)){
			if (visit((SimpleNode) node.jjtGetChild(0),data) == null){return null;}
		}
		else if (node.jjtGetNumChildren() > 0 && node.jjtGetChild(0).jjtGetId() == JJTDECLARATION_SPECIFIERS){

			Collection <Modifier> cm = (Collection <Modifier>) returnChild(JJTDECLARATION_SPECIFIERS,node,0,"GET_MODIFIERS");
			if (cm == null){cm = new ArrayList<Modifier>();}				
			IType type = (IType) returnChild(JJTDECLARATION_SPECIFIERS, node, 0, "GET_TYPE");
			if (type == null){return null;}
			if (testChild((SimpleNode) node.jjtGetChild(1), JJTPTR_OPERATOR,"*")){
				if (type.equals(TypeChar.INSTANCE))
					type=TypeString.INSTANCE;
				else{
					setErrorType("pointer");
					return null;
				}
			}

			SimpleNode nodeDecList = (SimpleNode) node.jjtGetChild(1);

			for (int i=0;i<nodeDecList.jjtGetNumChildren();i++){
				String name = (String) returnChild(JJTDECLARATOR, (SimpleNode) nodeDecList.jjtGetChild(i), 0, "GET_NAME");						
				if (name == null){return null;}

				cm.add(accesModifierActive);
				stackClassActives.peek().addField(new Field(cm, name, type, null));
			}
		}
		else if (testChild(node, JJTFUNCTION_DEFINITION)) {
			if (visit((SimpleNode) node.jjtGetChild(0),data) == null){return null;}
		}
		else {
			getUnrecognizedPart(((SimpleNode) node).jjtGetFirstToken(), ((SimpleNode) node).jjtGetLastToken());
			return null;
		}
		return 0;

	}

	@Override
	public Object visit(CPPAST_MEMBER_DECLARATOR_LIST node, Object data) {
		node.childrenAccept(this, data);
		return 0;
	}

	@Override
	public Object visit(CPPAST_MEMBER_DECLARATOR node, Object data) {
		return visit((SimpleNode) node.jjtGetChild(0),data);
	}

	@Override
	public Object visit(CPPAST_CONVERSION_FUNCTION_DECL_OR_DEF node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_ENUM_SPECIFIER node, Object data) {
		ClassDef cd = new ClassDef(Modifier.PUBLIC, node.jjtGetValue().toString(), defaultPlatform);
		cd.addMethod(new Constructor());
		cd.setIsEnum(true);
		stackClassActives.push(cd);
		goolClasses.add(cd);
		node.childrenAccept(this, node.jjtGetValue());
		stackClassActives.pop();
		return 0;
	}

	@Override
	public Object visit(CPPAST_ENUMERATOR_LIST node, Object data) {
		for (int i=0;i<node.jjtGetNumChildren();i++){
			if (visit((SimpleNode) node.jjtGetChild(i), data) == null)
				getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
		}
		return 0;
	}

	@Override
	public Object visit(CPPAST_ENUMERATOR node, Object data) {
		Field f = new Field(node.jjtGetValue().toString(),new TypeClass(data.toString()),new ClassNew(new TypeClass(data.toString())));
		f.addModifier(Modifier.STATIC);
		f.addModifier(Modifier.FINAL);
		stackClassActives.peek().addField(f);
		return 0;
	}

	@Override
	public Object visit(CPPAST_PTR_OPERATOR node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_CV_QUALIFIER_SEQ node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_DECLARATOR node, Object data) {
		if (node.jjtGetChild(0).jjtGetId() == JJTDIRECT_DECLARATOR){
			return returnChild(JJTDIRECT_DECLARATOR, node, 0, data);
		}
		else if (node.jjtGetChild(0).jjtGetId() == JJTPTR_OPERATOR && node.jjtGetChild(0).jjtGetValue() != null){
			return returnChild(JJTDECLARATOR, node, 1, data);
		}
		return null;
	}

	@Override
	public Object visit(CPPAST_DIRECT_DECLARATOR node, Object data) {
		if (data.toString().startsWith("GET_DIM")){
			return returnChild(JJTDECLARATOR_SUFFIXES, node, 1, data);
		}
		if (node.jjtGetValue() != null){
			return visit((CPPAST_QUALIFIED_ID) node.jjtGetChild(0), data);
		}
		return null;
	}

	@Override
	public Object visit(CPPAST_DECLARATOR_SUFFIXES node, Object data) {
		if (data.toString().compareTo("GET_DIM") == 0)
			return Integer.parseInt(node.jjtGetValue().toString());
		else{
			List<Expression> toReturn = new ArrayList<Expression>();
			for (int i=0;i<node.jjtGetNumChildren();i++){
				Expression e =(Expression) visit((SimpleNode) node.jjtGetChild(i), data);
				if (e == null){return null;}
				toReturn.add(e);
			}
			return toReturn;
		}
	}

	@Override
	public Object visit(CPPAST_FUNCTION_DECLARATOR_LOOKAHEAD node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_FUNCTION_DECLARATOR node, Object data) {
		if (node.jjtGetChild(0).jjtGetId() == JJTFUNCTION_DIRECT_DECLARATOR){
			return returnChild(JJTFUNCTION_DIRECT_DECLARATOR, node, 0, data);
		}
		else if (node.jjtGetChild(0).jjtGetId() == JJTPTR_OPERATOR && node.jjtGetChild(0).jjtGetValue() != null){
			return returnChild(JJTFUNCTION_DECLARATOR, node, 1, data);
		}
		return null;
	}

	@Override
	public Object visit(CPPAST_FUNCTION_DIRECT_DECLARATOR node, Object data) {
		if (data.toString().compareTo("GET_EXCEP") == 0)
			return returnChild(JJTEXCEPTION_SPEC, node, 1, data);
		else if (data.toString().compareTo("GET_NAME") == 0 || data.toString().compareTo("GET_SCOPE") == 0)
			return returnChild(JJTQUALIFIED_ID, node, 0, data);
		else if (data.toString().compareTo("GET_PARAMS") == 0)
			return returnChild(JJTPARAMETER_LIST, node, 1, data);
		return null;
	}

	@Override
	public Object visit(CPPAST_DTOR_CTOR_DECL_SPEC node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_DTOR_DEFINITION node, Object data) {
		Constructor c = new Constructor();
		if (testChild(node, JJTPARAMETER_LIST)){
			List<VarDeclaration> listVD = (List<VarDeclaration>) returnChild(JJTDTOR_DECLARATOR, node, 1, "GET_PARAMS");
			if (listVD == null){listVD=new ArrayList<VarDeclaration>();}
			for (VarDeclaration vd : listVD)
				c.addParameter(vd);
		}

		stackClassActives.peek().addMethod(c);
		methActive=c;
		visit((CPPAST_COMPOUND_STATEMENT) node.jjtGetChild(2), data);
		methActive=null;
		return 0;
	}

	@Override
	public Object visit(CPPAST_CTOR_DEFINITION node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_CTOR_DECLARATOR_LOOKAHEAD node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_CTOR_DECLARATOR node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_CTOR_INITIALIZER node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_SUPERCLASS_INIT node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_DTOR_DECLARATOR node, Object data) {
		if (node.jjtGetNumChildren() > 1)
			return null;
		return visit((SimpleNode) node.jjtGetChild(0), data);
	}

	@Override
	public Object visit(CPPAST_SIMPLE_DTOR_DECLARATOR node, Object data) {
		if (node.jjtGetNumChildren() > 1)
			return null;
		return visit((SimpleNode) node.jjtGetChild(0), data);
	}

	@Override
	public Object visit(CPPAST_PARAMETER_LIST node, Object data) {
		if (node.jjtGetNumChildren() == 0)
			getUnrecognizedPart(node.jjtGetFirstToken(), node.jjtGetLastToken());
		else {
			Object toReturn = returnChild(JJTPARAMETER_DECLARATION_LIST, node, 0, data);				
			if (node.jjtGetValue() != null)
				setUnrecognizedPart(", ...",node.jjtGetFirstToken());
			return toReturn;
		}
		return null;
	}

	@Override
	public Object visit(CPPAST_PARAMETER_DECLARATION_LIST node, Object data) {
		List<VarDeclaration> toReturn = new ArrayList<VarDeclaration>();
		for (int i=0;i<node.jjtGetNumChildren();i++){
			VarDeclaration vd = (VarDeclaration) visit((SimpleNode) node.jjtGetChild(i), data);
			if (vd != null)
				toReturn.add(vd);
		}
		return toReturn;
	}

	@Override
	public Object visit(CPPAST_PARAMETER_DECLARATION node, Object data) {
		IType type = (IType) returnChild(JJTDECLARATION_SPECIFIERS, node, 0, data);
		if (type == null){
			getUnrecognizedPart(node.jjtGetFirstToken(), node.jjtGetLastToken());
			return null;
		}
		if (testChild(node, JJTPTR_OPERATOR,"*")){
			if (type.equals(TypeChar.INSTANCE))
				type=TypeString.INSTANCE;
			else{
				setErrorType("pointer");
				getUnrecognizedPart(node.jjtGetFirstToken(), node.jjtGetLastToken());
				return null;
			}
		}
		String name = (String) returnChild(JJTDECLARATOR, node, 1, "GET_NAME");						
		if (name == null){
			getUnrecognizedPart(node.jjtGetFirstToken(), node.jjtGetLastToken());
			return null;
		}

		Collection <Modifier> cm = (Collection <Modifier>) visit((CPPAST_DECLARATION_SPECIFIERS) node.jjtGetChild(0), "GET_MODIFIERS");
		if (cm == null){
			setErrorType("modifiers");
			getUnrecognizedPart(node.jjtGetFirstToken(), node.jjtGetLastToken());
			return null;
		}

		VarDeclaration vd = new VarDeclaration(type, name);
		vd.setModifiers(cm);
		return vd;
	}

	@Override
	public Object visit(CPPAST_INITIALIZER node, Object data) {
		if (node.jjtGetValue() == null)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_TYPE_NAME node, Object data) {
		if(node.jjtGetChild(1) != null && node.jjtGetChild(1).jjtGetValue() != null && node.jjtGetChild(1).jjtGetValue().toString().compareTo("[]")==0){
			if (node.jjtGetChild(1).jjtGetNumChildren() > 0){
				IType type = (IType) returnChild(JJTDECLARATION_SPECIFIERS, node, 0, data);
				setErrorType("no init. in a array cast");
				getUnrecognizedPart(((SimpleNode)node.jjtGetChild(1).jjtGetChild(0)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(1).jjtGetChild(0)).jjtGetLastToken());
				return new TypeArray(type);
			}
			else{
				IType type = (IType) returnChild(JJTDECLARATION_SPECIFIERS, node, 0, data);
				return new TypeArray(type);
			}
		}
		else return visit((SimpleNode) node.jjtGetChild(0), data);
	}

	@Override
	public Object visit(CPPAST_ABSTRACT_DECLARATOR node, Object data) {
		return visit((SimpleNode) node.jjtGetChild(0), data);
	}

	@Override
	public Object visit(CPPAST_ABSTRACT_DECLARATOR_SUFFIX node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_TEMPLATE_HEAD node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_TEMPLATE_PARAMETER_LIST node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_TEMPLATE_PARAMETER node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_TEMPLATE_ID node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_TEMPLATE_ARGUMENT_LIST node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_TEMPLATE_ARGUMENT node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_STATEMENT_LIST node, Object data) {
		Block b = new Block();
		for (int i=0;i<node.jjtGetNumChildren();i++){
			Statement s = (Statement) visit((SimpleNode) node.jjtGetChild(i) , data);
			if (s == null)
				getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
			else
				b.addStatement(s);
		}
		return b;
	}

	@Override
	public Object visit(CPPAST_STATEMENT node, Object data) {
		if (node.jjtGetNumChildren() != 1)
			return null;
		return visit((SimpleNode) node.jjtGetChild(0), data);
	}

	@Override
	public Object visit(CPPAST_LABELED_STATEMENT node, Object data) {
		if (node.jjtGetValue() != null){
			Expression expCase = (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			Block stmtCase = (Block) visit((SimpleNode) node.jjtGetChild(1),data);
			if (expCase == null || stmtCase == null){return null;}
			return new Case(expCase,stmtCase);
		}
		return null;
	}

	@Override
	public Object visit(CPPAST_COMPOUND_STATEMENT node, Object data) {
		if (node.jjtGetNumChildren() > 0)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return new Block();
	}

	@Override
	public Object visit(CPPAST_SELECTION_STATEMENT node, Object data) {
		if (((String) node.jjtGetValue()).compareTo("if") == 0){
			Expression cond = (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			if (cond == null){return null;}
			Statement thenSt = (Statement) visit((SimpleNode) node.jjtGetChild(1),data);
			if (thenSt == null){thenSt=new Block();}

			Statement elseSt;
			if (node.jjtGetNumChildren() == 3){
				elseSt = (Statement) visit((SimpleNode) node.jjtGetChild(2),data);
				if (elseSt == null){elseSt=new Block();}
			}
			else
				elseSt=null;			
			return new If (cond,thenSt,elseSt);
		}
		// Cas du switch
		else if (((String) node.jjtGetValue()).compareTo("switch") == 0){
			List <Case> l=new ArrayList<Case>();
			Expression cond = (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			if (cond == null){return null;}

			Block listCase = (Block) returnChild(JJTSTATEMENT, node, 1, data);

			for (int i=0; i<listCase.getStatements().size();i++){
				l.add((Case) listCase.getStatements().get(i));
			}
			return new Switch(cond, l);
		}
		else if (node.jjtGetNumChildren() > 0)
			return visit((SimpleNode) node.jjtGetChild(0),data);
		return null;
	}

	@Override
	public Object visit(CPPAST_ITERATION_STATEMENT node, Object data) {
		if (((String) node.jjtGetValue()).compareTo("while")== 0){
			Expression condWhile = (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			if (condWhile == null){return null;}
			Statement stWhile = (Statement) visit((SimpleNode) node.jjtGetChild(1),data);
			if (stWhile == null){stWhile=new Block();}
			return new While (condWhile,stWhile);
		}

		else if (((String) node.jjtGetValue()).startsWith("for")){

			if (node.jjtGetValue().toString().split(" ").length == 1){
				Statement stFor = (Statement) visit((SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1),data);
				if (stFor == null)
					getUnrecognizedPart(((SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1)).jjtGetLastToken());
				return new For(null,null,null,stFor);
			}
			else {				
				String pattern = node.jjtGetValue().toString().split(" ")[1];
				Statement stFor=null;
				Statement initFor=null;
				Expression condFor=null;
				Statement updater=null;

				for (int i=0;i<node.jjtGetNumChildren()-1;i++){
					if (pattern.charAt(i) == '1'){
						initFor = (Statement) visit((SimpleNode) node.jjtGetChild(i),data);
						if (initFor instanceof Block){
							if (((Block) initFor).getStatements().size() > 1){
								setErrorType("no more than 1 declaration in a for");
								return null;
							}
							initFor=((Block) initFor).getStatements().get(0); 
						}
						if (initFor == null)
							getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
					}
					else if (pattern.charAt(i) == '2'){
						condFor = (Expression) visit((SimpleNode) node.jjtGetChild(i),data);
						if (condFor == null)
							getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
					}
					else{
						updater = (Statement) visit((SimpleNode) node.jjtGetChild(i),data);
						if (updater == null)
							getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
					}
				}
				stFor = (Statement) visit((SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1),data);
				if (stFor == null)
					getUnrecognizedPart(((SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1)).jjtGetLastToken());

				return new For(initFor,condFor,updater,stFor);
			}
		}
		else if (((String) node.jjtGetValue()).compareTo("dowhile")== 0){
			Expression condDo = (Expression) visit((SimpleNode) node.jjtGetChild(1),data);
			if (condDo == null){return null;}		
			Statement stDo = (Statement) visit((SimpleNode) node.jjtGetChild(0),data);
			if (stDo == null){return null;}				
			return new DoWhile(stDo,condDo);
		}
		else
			return null;
	}

	@Override
	public Object visit(CPPAST_JUMP_STATEMENT node, Object data) {
		if (node.jjtGetValue() != null){
			Expression tr = (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			if (tr == null){return null;}
			return new Return (tr);
		}
		return null;
	}

	@Override
	public Object visit(CPPAST_TRY_BLOCK node, Object data) {
		Block block = (Block) visit((SimpleNode) node.jjtGetChild(0),data);
		if (block == null){return null;}
		Block finallyBlock = new Block();
		List<Catch> catches = new ArrayList<Catch>();

		for (int i=1;i<node.jjtGetNumChildren();i++){
			if (((String) ((SimpleNode) node.jjtGetChild(i)).jjtGetValue()).compareTo("finally") == 0){
				finallyBlock = (Block) visit((SimpleNode) node.jjtGetChild(i),data);
				if (finallyBlock == null){return null;}
			}
			else{
				Catch c = (Catch) visit((SimpleNode) node.jjtGetChild(i),data);
				if (c == null){
					getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
				}
				else
					catches.add(c);
			}
		}			
		return new Try(catches, block, finallyBlock);
	}

	@Override
	public Object visit(CPPAST_HANDLER node, Object data) {
		if (((String) node.jjtGetValue()).compareTo("catch") == 0){
			List<VarDeclaration> listVD = (List<VarDeclaration>) visit((SimpleNode) node.jjtGetChild(0),data);
			if (listVD == null || listVD.size() != 1){return null;}
			Block block = (Block) visit((SimpleNode) node.jjtGetChild(1),data);
			if (block == null){return null;}
			return new Catch(listVD.get(0), block);
		}
		else{
			return (Block) visit((SimpleNode) node.jjtGetChild(0),data);
		}
	}

	@Override
	public Object visit(CPPAST_EXCEPTION_DECLARATION node, Object data) {
		if (node.jjtGetNumChildren() > 0)
			return visit((SimpleNode) node.jjtGetChild(0),data);
		return null;
	}

	@Override
	public Object visit(CPPAST_THROW_STATEMENT node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_ASSIGNMENT_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren()>1){
			Node varAss = (Node) visit((SimpleNode) node.jjtGetChild(0),data);
			Expression expAss = (Expression) visit((SimpleNode) node.jjtGetChild(1),data);
			if (((String) node.jjtGetValue()).compareTo("=")== 0){
				if (varAss == null || expAss == null){return null;}
				return new Assign(varAss, expAss);
			}
			else if (((String) node.jjtGetValue()).compareTo("+=")== 0){
				Operator operator = Operator.PLUS;
				String textualoperator = "+";
				if (varAss == null || expAss == null){return null;}
				return new CompoundAssign(varAss, expAss, operator, textualoperator, TypeInt.INSTANCE);
			}
			else if (((String) node.jjtGetValue()).compareTo("-=")== 0){

				Operator operator = Operator.MINUS;
				String textualoperator = "-";
				if (varAss == null || expAss == null){return null;}
				return new CompoundAssign(varAss, expAss, operator, textualoperator, TypeInt.INSTANCE);
			}
			else if (((String) node.jjtGetValue()).compareTo("*=")== 0){

				Operator operator = Operator.MULT;
				String textualoperator = "*";
				if (varAss == null || expAss == null){return null;}
				return new CompoundAssign(varAss, expAss, operator, textualoperator, TypeInt.INSTANCE);
			}
			else if (((String) node.jjtGetValue()).compareTo("/=")== 0){

				Operator operator = Operator.NOT;
				String textualoperator = "";
				if (varAss == null || expAss == null){return null;}
				return new CompoundAssign(varAss, expAss, operator, textualoperator, TypeInt.INSTANCE);
			}
			else {setErrorType("unrecognized symbol"); return null;}
		}
		else if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}


	@Override
	public Object visit(CPPAST_CONDITIONAL_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_CONSTANT_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_LOGICAL_OR_EXPRESSION node, Object data) {
		return getBooleanExpression(node,data);
	}

	@Override
	public Object visit(CPPAST_LOGICAL_AND_EXPRESSION node, Object data) {
		return getBooleanExpression(node,data);
	}

	@Override
	public Object visit(CPPAST_INCLUSIVE_OR_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_EXCLUSIVE_OR_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_AND_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_EQUALITY_EXPRESSION node, Object data) {
		return getBooleanExpression(node,data);
	}

	@Override
	public Object visit(CPPAST_RELATIONAL_EXPRESSION node, Object data) {
		return getBooleanExpression(node,data);
	}

	@Override
	public Object visit(CPPAST_SHIFT_EXPRESSION node, Object data) {
		if (isFunctionPrint(node,data)){							
			stackClassActives.peek().addDependency(new SystemOutDependency());
			GoolCall gc = new SystemOutPrintCall();
			gc.addParameter(getExpressionPrint(node, 1, data));
			return gc;
		}
		else if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_ADDITIVE_EXPRESSION node, Object data) {
		return getBinaryExpression (node, node.jjtGetNumChildren()-1, (List<?>) node.jjtGetValue(), data);
	}

	@Override
	public Object visit(CPPAST_MULTIPLICATIVE_EXPRESSION node, Object data) {
		return getBinaryExpression (node, node.jjtGetNumChildren()-1, (List<?>) node.jjtGetValue(), data);
	}

	@Override
	public Object visit(CPPAST_PM_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_CAST_EXPRESSION node, Object data) {
		if (node.jjtGetNumChildren() > 1){
			Expression exp = (Expression) visit((SimpleNode) node.jjtGetChild(1),data);
			if (exp == null){return null;}
			IType type = (IType) returnChild(JJTTYPE_NAME, node, 0, data);
			if (type == null){return null;}
			if (testChild(node, JJTPTR_OPERATOR,"*")){
				if (type.equals(TypeChar.INSTANCE))
					type=TypeString.INSTANCE;
				else{
					setErrorType("pointer");
					return null;
				}
			}				
			return new CastExpression(type, exp);
		}
		else if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_UNARY_EXPRESSION node, Object data) {
		if (node.jjtGetChild(0).jjtGetId() == JJTTYPE_NAME)
			return null;
		else if (!data.toString().startsWith("GET_") && Integer.parseInt(data.toString()) > 1){
			return null;
		}
		else if (node.jjtGetChild(0).jjtGetId() == JJTUNARY_OPERATOR && node.jjtGetChild(0).jjtGetValue() != null && node.jjtGetChild(0).jjtGetValue().toString().compareTo("!") == 0){
			Expression expr = (Expression) visit((SimpleNode) node.jjtGetChild(1),data);
			if (expr == null){return null;}
			return new UnaryOperation(convertSymToOpe("!"), expr, TypeBool.INSTANCE, "!");
		}
		else if (node.jjtGetChild(0).jjtGetId() == JJTUNARY_OPERATOR && node.jjtGetChild(0).jjtGetValue() != null && node.jjtGetChild(0).jjtGetValue().toString().compareTo("*") == 0){
			Expression expr = (Expression) visit((SimpleNode) node.jjtGetChild(1),data);
			if (expr == null){return null;}
			if (expr instanceof This)
				return expr;
			else
				return null;
		}
		else if (node.jjtGetValue()!=null && ((String) node.jjtGetValue()).compareTo("++")== 0){
			int nData = 0;
			if (data != null)
				nData+=Integer.parseInt(data.toString())+1;

			Operator operator = Operator.PREFIX_INCREMENT;
			Expression varPost= (Expression) visit((SimpleNode) node.jjtGetChild(0),nData);
			if (varPost == null){return null;}
			return new UnaryOperation(operator,varPost, TypeInt.INSTANCE, "++");

		} else if (node.jjtGetValue()!=null && ((String) node.jjtGetValue()).compareTo("--")== 0){
			int nData = 0;
			if (data != null)
				nData+=Integer.parseInt(data.toString())+1;

			Operator operator = Operator.PREFIX_DECREMENT;
			Expression varPost= (Expression) visit((SimpleNode) node.jjtGetChild(0),nData);
			if (varPost == null){return null;}
			return new UnaryOperation(operator,varPost, TypeInt.INSTANCE, "--");
		}
		else if (node.jjtGetNumChildren() == 1 && node.jjtGetValue() != null)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_NEW_EXPRESSION node, Object data) {
		IType type = (TypeClass) visit((SimpleNode) node.jjtGetChild(0).jjtGetChild(0), data);
		if (type == null){return null;}	

		if (testChild(node,JJTDIRECT_NEW_DECLARATOR)){
			List <Expression> listExpr = (List <Expression>) visit((SimpleNode) node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0),data);
			if (listExpr == null){return null;}
			else if (listExpr.size() > 1){setErrorType("dimension > 1 impossible"); return null;}
			return new ArrayNew(type,listExpr,listExpr);
		}

		ClassNew cn = new ClassNew(type);
		if (node.jjtGetNumChildren() > 1 && node.jjtGetChild(1).jjtGetNumChildren() > 0){
			List <Expression> listExpr = (List <Expression>) visit((SimpleNode) node.jjtGetChild(1).jjtGetChild(0),data);
			if (listExpr == null){return null;}
			cn.addParameters(listExpr);
		}
		return cn;
	}

	@Override
	public Object visit(CPPAST_NEW_TYPE_ID node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_NEW_DECLARATOR node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_DIRECT_NEW_DECLARATOR node, Object data) {
		List <Expression> listExp = new ArrayList<Expression>();
		for (int i=0;i<node.jjtGetNumChildren();i++){
			Expression e = (Expression) visit((SimpleNode) node.jjtGetChild(i),data);
			if (e == null)
				getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
			else
				listExp.add(e);
		}
		return listExp;
	}

	@Override
	public Object visit(CPPAST_NEW_INITIALIZER node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_DELETE_EXPRESSION node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_UNARY_OPERATOR node, Object data) {
		return null;
	}

	private Expression getLinkedTarget (SimpleNode node, int i, List<String> l, Object data){
		if (node.jjtGetChild(i).jjtGetId() == JJTEXPRESSION_LIST){
			return getLinkedTarget(node, i-1, l, data);
		}
		else if (i != 0){
			if (l.get(i).compareTo("()") == 0){
				Identifier id = (Identifier) returnChild(JJTID_EXPRESSION, node, i, data);
				Expression target = getLinkedTarget(node, i-1, l, data);
				if (id == null || target == null){return null;}
				MethCall m = new MethCall(new TypeMethod("typemeth"), new MemberSelect(target, new VarDeclaration(id.getType(), id.getName())));
				if (i+1 < node.jjtGetNumChildren() && node.jjtGetChild(i+1).jjtGetId() == JJTEXPRESSION_LIST){
					m.addParameters((List<Expression>) visit((SimpleNode) node.jjtGetChild(i+1),data));
				}
				return m;
			}
			else{
				Identifier id = (Identifier) returnChild(JJTID_EXPRESSION, node, i, data);
				Expression target = getLinkedTarget(node, i-1, l, data);
				if (id == null || target == null){return null;}
				return new MemberSelect(target, new VarDeclaration(id.getType(), id.getName()));
			}
		}
		else if (l.get(i).compareTo("()") == 0){
			Identifier id = (Identifier) returnChild(JJTPRIMARY_EXPRESSION, node, 0, data);
			if (id == null){return null;}
			MethCall m = new MethCall(new TypeMethod("typemeth"), id);
			if (i+1 < node.jjtGetNumChildren() && node.jjtGetChild(i+1).jjtGetId() == JJTEXPRESSION_LIST){
				m.addParameters((List<Expression>) visit((SimpleNode) node.jjtGetChild(i+1),data));
			}
			return m;
		}
		else if (l.get(i).compareTo("i") == 0){
			return (Identifier) returnChild(JJTPRIMARY_EXPRESSION, node, 0, data);
		}
		else
			return null;
	}

	@Override
	public Object visit(CPPAST_POSTFIX_EXPRESSION node, Object data) {
		if (node.jjtGetValue() instanceof List<?>){
			List<String> l = (List<String>) node.jjtGetValue();			
			return getLinkedTarget(node, node.jjtGetNumChildren()-1, l, data);
		}
		else if (node.jjtGetValue() != null && ((String) node.jjtGetValue()).compareTo("()") == 0){
			Identifier name = (Identifier) returnChild(JJTPRIMARY_EXPRESSION, node, 0, "GET_ID_FCT");						
			MethCall m = new MethCall(new TypeMethod("typemeth"), name);
			if (node.jjtGetNumChildren() > 1){
				m.addParameters((List<Expression>) visit((SimpleNode) node.jjtGetChild(1),data));
			}
			return m;
		}
		else if (node.jjtGetValue() != null && ((String) node.jjtGetValue()).compareTo("[]") == 0){
			return getListDim (node,node.jjtGetNumChildren()-1,data);
		}
		else if (node.jjtGetValue()!=null && ((String) node.jjtGetValue()).compareTo("++")== 0){
			Operator operator = Operator.POSTFIX_INCREMENT;
			Expression varPost= (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			if (varPost == null){return null;}
			return new UnaryOperation(operator,varPost, TypeInt.INSTANCE, (String) node.jjtGetValue());

		} else if (node.jjtGetValue()!=null && ((String) node.jjtGetValue()).compareTo("--")== 0){
			Operator operator = Operator.POSTFIX_DECREMENT;
			Expression varPost= (Expression) visit((SimpleNode) node.jjtGetChild(0),data);
			if (varPost == null){return null;}
			return new UnaryOperation(operator,varPost, TypeInt.INSTANCE, (String) node.jjtGetValue());
		}
		else if (node.jjtGetValue()!=null && ((String) node.jjtGetValue()).compareTo("ERROR")== 0)
			return null;
		else if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_ID_EXPRESSION node, Object data) {
		if (data.toString().compareTo("GET_ID_FCT") == 0 && node.jjtGetNumChildren() > 0){
			String id = (String) visit((SimpleNode) node.jjtGetChild(0),data);
			if (id == null){return null;}
			return new Identifier (new TypeVar("typevar"),id.replaceAll("::",".")+node.jjtGetValue().toString());
		}
		else if (node.jjtGetNumChildren() > 0){
			String id = (String) visit((SimpleNode) node.jjtGetChild(0),data);
			if (id == null){return null;}
			Identifier idf = new Identifier (new TypeVar("typevar"),id.replaceAll("::",".").substring(0, id.replaceAll("::",".").length()-1));
			return new FieldAccess(new TypeVar("typevar"), idf,node.jjtGetValue().toString());		}
		else
			return new Identifier (new TypeVar("typevar"),node.jjtGetValue().toString());
	}

	@Override
	public Object visit(CPPAST_PRIMARY_EXPRESSION node, Object data) {
		if (node.jjtGetValue() != null && node.jjtGetValue().toString().compareTo("this") == 0)
			return new This(stackClassActives.peek().getType());
		else if (node.jjtGetNumChildren() == 0)
			return new Constant(TypeString.INSTANCE,((String) node.jjtGetValue()).subSequence(1, ((String) node.jjtGetValue()).length()-1));
		else if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_EXPRESSION_LIST node, Object data) {
		List <Expression> listExp = new ArrayList<Expression>();
		for (int i=0;i<node.jjtGetNumChildren();i++){
			Expression e = (Expression) visit((SimpleNode) node.jjtGetChild(i),data);
			if (e == null)
				getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
			else
				listExp.add(e);
		}
		return listExp;
	}

	@Override
	public Object visit(CPPAST_CONSTANT node, Object data) {
		if (((String) node.jjtGetValue()).startsWith("'") || ((String) node.jjtGetValue()).endsWith("'"))
			return new Constant(convertIType((String) node.jjtGetType()),((String) node.jjtGetValue()).subSequence(1, ((String) node.jjtGetValue()).length()-1));
		else
			return new Constant(convertIType((String) node.jjtGetType()),node.jjtGetValue());
	}

	@Override
	public Object visit(CPPAST_OPTOR node, Object data) {
		return null;
	}

	@Override
	public Object visit(CPPAST_EXCEPTION_SPEC node, Object data) {
		if (node.jjtGetNumChildren() == 1)
			return visit((SimpleNode) node.jjtGetChild(0), data);
		return null;
	}

	@Override
	public Object visit(CPPAST_EXCEPTION_LIST node, Object data) {
		List<IType> listType = new ArrayList<IType>();
		for (int i=0;i<node.jjtGetNumChildren();i++){
			IType type = (IType) returnChild(JJTTYPE_NAME, node, 0, data);
			if (type == null){
				getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
			}
			else if (testChild(node, JJTPTR_OPERATOR,"*")){
				if (type.equals(TypeChar.INSTANCE))
					type=TypeString.INSTANCE;
				else{
					setErrorType("pointer");
					getUnrecognizedPart(((SimpleNode) node.jjtGetChild(i)).jjtGetFirstToken(), ((SimpleNode) node.jjtGetChild(i)).jjtGetLastToken());
				}
			}
			else
				listType.add(type);
		}
		return listType;
	}

	@Override
	public Object visit(CPPAST_INCLUDE_SPECIFER node, Object data) {
		for (String lib : uncheckedLib)
			if (((String)node.jjtGetValue()).substring(1,((String)node.jjtGetValue()).length()-1).compareTo(lib) == 0)
				return 0;	

		// The destination package is either null or that specified by the visited package
		Object toReturn = null ;
		List<Dependency> dependencies = new ArrayList<Dependency>();

		// GoolMatcher init call
		String dependencyString = ((String)node.jjtGetValue()).substring(1,((String)node.jjtGetValue()).length()-1);
		if (!RecognizerMatcher.matchImport(dependencyString )) {
			dependencies.add(new UnrecognizedDependency(dependencyString));
		}
		else{
			toReturn = 0 ;
		}

		//RecognizerMatcher.printMatchTables();
		stackClassActives.peek().addDependencies(dependencies);

		// Building in the tree
		for (ClassDef classDef : getGoolClasses()) {
			GoolLibraryClassAstBuilder.init(defaultPlatform);
			for (Dependency dep : classDef.getDependencies()) {
				if (dep instanceof RecognizedDependency) {
					GoolLibraryClassAstBuilder
					.buildGoolClass(((RecognizedDependency) dep)
							.getName());
				}
			}
		}

		return toReturn;
	}
}
