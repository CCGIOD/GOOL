/* Generated By:JJTree: Do not edit this line. CPPAST_ACCESS_SPECIFIER.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=CPPAST_,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp.nodes;

import gool.parser.cpp.*;

public
class CPPAST_ACCESS_SPECIFIER extends SimpleNode {
  public CPPAST_ACCESS_SPECIFIER(int id) {
    super(id);
  }

  public CPPAST_ACCESS_SPECIFIER(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=3d1e65fd761eb2ffa8eca76623fd1bae (do not edit this line) */