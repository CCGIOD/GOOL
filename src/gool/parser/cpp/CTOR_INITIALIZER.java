/* Generated By:JJTree: Do not edit this line. CTOR_INITIALIZER.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp;

public
class CTOR_INITIALIZER extends SimpleNode {
  public CTOR_INITIALIZER(int id) {
    super(id);
  }

  public CTOR_INITIALIZER(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=bca41f658d46a2920fa04a29ad9082af (do not edit this line) */