/* Generated By:JJTree: Do not edit this line. CPPAST_EXPRESSION_LIST.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=CPPAST_,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp.nodes;

import gool.parser.cpp.*;

public
class CPPAST_EXPRESSION_LIST extends SimpleNode {
  public CPPAST_EXPRESSION_LIST(int id) {
    super(id);
  }

  public CPPAST_EXPRESSION_LIST(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=7115502db0d429f51fb061716b9f70bd (do not edit this line) */