/* Generated By:JJTree: Do not edit this line. MEMBER_DECLARATOR.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp;

public
class MEMBER_DECLARATOR extends SimpleNode {
  public MEMBER_DECLARATOR(int id) {
    super(id);
  }

  public MEMBER_DECLARATOR(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=daece1817406146e7700ea2df11e8bdc (do not edit this line) */