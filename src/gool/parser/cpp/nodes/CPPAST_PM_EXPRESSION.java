/* Generated By:JJTree: Do not edit this line. CPPAST_PM_EXPRESSION.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=CPPAST_,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp.nodes;

import gool.parser.cpp.*;

public
class CPPAST_PM_EXPRESSION extends SimpleNode {
  public CPPAST_PM_EXPRESSION(int id) {
    super(id);
  }

  public CPPAST_PM_EXPRESSION(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=04d65fceda54bd1f07cbcc466050aa6a (do not edit this line) */
