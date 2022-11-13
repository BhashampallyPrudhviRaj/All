#include <iostream>
using namespace std;

long gcd(long m,long n){
    if(m==0)
        return n;
    return gcd(n%m,m);
}

int main() {
	// your code goes here
	int t,n;
	long l;
	std::cin >> t;
	while(t){
	    cin>>n;
	    long a[n];
	    for(int i=0; i<n; i++)
	        cin>>a[i];
	   
	    l=a[0];
	    for(int i=1; i<n; i++)
	        l = ((l*a[i])/gcd(l,a[i]));
	        
	    cout << l;
	    if(t!=1)
	        cout<<"\n";
	    t--;
	}
	
	return 0;
}
