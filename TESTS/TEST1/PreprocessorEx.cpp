

#include "PreprocessorEx.h"


#define MY_DEFINE_1 "I use the first solution"
#define MY_DEFINE_2 "I use the second solution"

void PreprocessorEx::usage(){
  cout << "usage()" << endl;
}

int main(void)
{
  usage();
#ifdef WANT_FIRST_SOLUTION
  cout << MY_DEFINE_1 << endl;
#else
  cout << MY_DEFINE_2 << endl;
#endif
}
