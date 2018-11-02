sub printparams { 
printf "First Parameter: $_[0]\n"; 
printf "Third Parameter: $_[2]\n"; 
printf "Fourth Parameter: $_[3]\n"; 
printf "Sixth Parameter: $_[5]\n"; 
} 
@array1 = ("This", "is", "text"); 
$num = 100; 
@array2 = ("Welcome", "here"); #calling subroutine 
&printparams(@array1, @array2, $num);
print"\n";

sub DisplayMyHash{ #copying passed hash to the hash 
my %hash1 = @_; 
for my $key (keys %hash2) { 
print "Key is: $key and value is: $hash1{$key}\n"; 
} 
} 
%hash2 = ('Item1', 'Orange', 'Item2', 'Apple', 'Item3', 'Banana'); # Function call with hash parameter 
&DisplayMyHash(%hash2);
print ("\n");

use warnings; 
use strict; 
print &say_hi , "\n"; 
sub say_hi{ 
	my $name = "Chythu";
	print "Hi $name \n"; 
	$name; 
}
print("\n");

use warnings; 
use strict; 
my @a = (); 
my $j = min(@a); 
if(defined $j){ 
	print("Min of @a is $j \n"); 
}else{ 
print("The array is empty.\n"); 
} 
my @b = (100,12,31); 
my $k = min(@b);
if(defined $k){
print("Min of @b is $k \n");
}else{
print("The array b is empty.\n");
}
sub min{
my $m = shift;
return undef unless defined $m;
for (@_){
$m = $_ if $m > $_;
}
return $m;
}
print "\n";

#next Statement
#Without Label:
print("next Statement\n");
print("Without Label:\n");
$a = 10;
while( $a < 20 ) {
if( $a == 15)
{
# skip the iteration.
$a = $a + 1;
next;
print("-------------it is 15--------------");
}
print "value of a: $a\n";
$a = $a + 1;
}
print "\n";

#With Label:
print("With Label:\n");
$a = 0;
OUTER: while( $a < 4 ) {
$b = 0;
print "value of a: $a\n";
INNER: while ( $b < 4) {
if( $a == 2) {
$a = $a + 1;
# jump to outer loop
next OUTER;
}
$b = $b + 1;
print "Value of b : $b\n";
}
print "\n";
$a = $a + 1;
}
print "\n";

#last Statement
#Without Label:
print("last Statement\n");
print("Without Label:\n");
$a = 10;
while( $a < 20 ) {
if( $a == 15)
{
# skip the iteration.
$a = $a + 1;
last;
}
print "value of a: $a\n";
$a = $a + 1;
}
print "\n";

#With Label:
print("With Label:\n");
$a = 0;
OUTER: while( $a < 4 ) {
$b = 0;
print "value of a: $a\n";
INNER: while ( $b < 4) {
if( $a == 2) {
$a = $a + 1;
# jump to outer loop
last OUTER;
}
$b = $b + 1;
print "Value of b : $b\n";
}
print "\n";
$a = $a + 1;
}
print "\n";


#continue Statement
print "continue Statement\n";
my @list = (1, 2, 3, 4, 5);
foreach $a (@list) {
print "Value of a = $a\n";
} continue {
last if $a == 4;
}
print "\n";


#redo Statement
print("redo Statement\n");
$a = 0;
while($a < 10) {
if( $a == 5 ) {
$a = $a + 1;
redo;
print("---------redo----------")
}
print "Value of a = $a\n";
} continue {
$a = $a + 1;
}