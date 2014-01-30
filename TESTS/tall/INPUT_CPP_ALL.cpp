
#include "GoolFileImpl.h"

#define MY_PATH "azeerty1"
#define VAL_1 1
#define VAL_2 2

class A 
{
	public:
		~A(int i){int i;}
		void m(int i);
		virtual void f1 ();
		int main (){ new A (1,2); int i; i++; ++i; }

	private:
		const double d;
		int i,j,k;

};

static class B
{
};

int fct0(){
	GoolFileImpl f(MY_PATH);
}

enum Color
   {
      RED,
      BLUE,
      WHITE
   };

int v1=5,v2,v3=4+1;
char v4;
char* v5="toto";

bool bb;

ClassTest tt;

int main (){

	cout << 1 << endl;
	cout << "HELLO" << " " << "WORLD" << endl;

	if (true){}

	if (true){}
	else if (true){}
	else {}

	while (true){}
	do {} while (true);

	int i=1*2-10/2+5;
	int i2 = 1+2-3+4-5;
	int i3,i4,i5;
	int i6=1,i7,i8=5;
	ClassTest t(),t2(1,2),t3;

	int i9 = 5;
	float f0 = (float) 5;
 	int i10 = (double []) k;

	char* str;
	char* str2 = "CHEVAL";

	const int iii;	

	int b [5];

}

static const int i;

static int t () throw (int, double) {

	try{
     		int i;
   	}catch (int e) {
    		int j;
   	}
	catch (int e) {
    		int t;
   	}

	try{}
	finally { f (); }

	switch (i){
		case 1 : {
			int i;
			int k;
			return 0;
		}
		case 2 : {
			return 0;
		}
	}

	i=1*getI();

	
	bool b0 = true && true;
	bool b = 1 < 5;
	bool b2 = 1 > 5;
	bool b3 = 1 <= 5;
	bool b4 = 1 >= 5;
	bool b4b = 1 == 5;
	bool b4c = 1 != 5;
	bool b5 = (1 < 5) && (1 > 4);
	bool b6 = (1 < 5) || (1 > 4);
	bool b7 = !((1 < 5) && (1 > 4));

	int i = 1 + i;
	int i2 = 1 + i + f();
	int i3 = 1 + i + f() + f2(2) + f3(2,4) + f4(f(),f(1),f(2,3,4,5,6));
	
	int i4 = p[0][1][f(1)];
	int i5 = j + p[0] + getK() + getK2(1);

	int i7 = ClassTest::st;

	Test::f();

	for (int i=0;i<10;i++){}

	for (;;){}
	for (1;;){}
	for (;1;){}
	for (;;1){}
	for (1;1;){}
	for (;1;1){}
	for (1;;1){}
	for (1;1;1){}

	Test::Test::Test::f();

	f.m();
	f.i;
	f.i1.i2;

	f().g();
	f().g().h();

	i.j;
	i().j;
	i.j();
	i().j();
	i.j().i.j;

	i.j.h.k();
	i.j.h().k;
	i().j.h().k();
	
	i.j.h.k();
	i.j.h().k;
	i().j.h().k();
	i.j().k.l.m.n.o().p.q().r.s(1).t(u[0],1,2,3,4).a;

	return (VAL_1+VAL_2+10000);
} 

int test (int i, Test t, char c){
	return 0;
}

double f1 (){
	return 1.0+2.5*3.2;
}

int main (){}
double main ();
int main (Test tt);
int main (int argc, char* argv){}

