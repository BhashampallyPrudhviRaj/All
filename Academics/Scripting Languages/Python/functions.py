def changeme( mylist ):
	"This changes a passed list into this function"
	mylist.append([1,2,3,4]);
	print("Values inside the function: ", mylist)
	return
mylist = [10,20,30];
changeme( mylist );
print("Values outside the function: ", mylist)
print("\n")

def changeme( mylist ):
	"This changes a passed list into this function"
	mylist=[1,2,3,4];
	print("Values inside the function: ", mylist)
	return
mylist = [10,20,30];
changeme( mylist );
print("Values outside the function: ", mylist)
print("\n")


def printinfo( name, age ):
	"This prints a passed info into this function"
	print("Name: ", name  ,"Age: ", age)
	return
printinfo( age=1, name="sample" )
print("\n")


def printinfo( name, age = 31 ):
	"This prints a passed info into this function"
	print("Name: ", name); print( "Age ", age)
	return
printinfo( age=30, name="sample" )
printinfo( name="sample" )
print("\n")


def printinfo( arg1, *vartuple ):
	"This prints a variable passed arguments"
	print("Output is: ")
	print(arg1)
	for var in vartuple:
		print(var)
	return;
printinfo( 10 )
printinfo( 70, 60, 50 )