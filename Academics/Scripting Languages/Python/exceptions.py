try:
	fh = open("sample", "w")
	fh.write("This is my test file for exception handling!!")
except IOError:
	print("Error: can\'t find file or read data ")
else:
	print("Written content in the file successfully")
	fh.close()
print("\n")


try:
	fh = open("sample", "r")
	fh.write("This is my test file for exception handling!!")
except IOError:
	print("Error: can\'t find file or read data ")
else:
	print("Written content in the file successfully")
	fh.close()
print("\n")


try:
	fh = open("testfile", "w")
	try:
		fh.write("This is my test file for exception handling!!")
	finally:
		print("Going to close the file")
	fh.close()
except IOError:
	print("Error: can\'t find file or read data")