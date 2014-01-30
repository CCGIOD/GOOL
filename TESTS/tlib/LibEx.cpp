
#include "GoolFileImpl.h"


int main()
{
  GoolFileImpl myfile1("mon_path1");
  GoolFileImpl myfile2("mon_path2");

  if( myfile1.exist() ){
    cout << "mon_path1 existe" << endl ;
  }
}
