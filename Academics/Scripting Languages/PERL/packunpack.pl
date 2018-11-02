$bits = pack("c", 65);
# prints A, which is ASCII 65.
print "bits are $bits\n";
$bits = pack( "x" );
# $bits is now a null chracter.
print "bits are $bits\n";
$bits = pack( "sai", 255, "T", 30 );
# creates a seven character string on most computers'
print "bits are $bits\n";
@array = unpack( "sai", "$bits" );
#Array now contains three elements: 255, T and 30.
print "Array $array[0]\n";
print "Array $array[1]\n";
print "Array $array[2]\n";	