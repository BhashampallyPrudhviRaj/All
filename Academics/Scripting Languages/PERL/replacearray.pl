@num = qw\1 2 3 4 5 6 7 8 9 0\;
print "@num\t";
print "\n";
splice(@num, 3, 1,);
print "@num\t";
print "\n";	