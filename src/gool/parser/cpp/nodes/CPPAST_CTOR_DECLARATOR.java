/* Generated By:JJTree: Do not edit this line. CPPAST_CTOR_DECLARATOR.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=CPPAST_,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp.nodes;

import gool.parser.cpp.*;

public
class CPPAST_CTOR_DECLARATOR extends SimpleNode {
  public CPPAST_CTOR_DECLARATOR(int id) {
    super(id);
  }

  public CPPAST_CTOR_DECLARATOR(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=a5cd071d40b3b27eb24b5391702c59a5 (do not edit this line) */
