open(READ1, "<slice.pl");
while(<READ1>) {
print "$_";
}
print("\n\n");

open($fh, "<", "slice.pl");
print readline($fh);

open(WRITE, ">", "file.pl");

open(WRITE, ">", "file.txt") or die "Couldn't open the file";
print WRITE "I am ROBOT\n";
print("\n");


my $filename = 'slice.pl';
open(my $fh, '<', $filename)
or die "Could not open file '$filename' $!";
my $row = <$fh>;
print "$row\n";
print "done\n";
print "\n";


open (FILEHANDLE, "<", "slice.pl") or die ("Cannot open file.txt");
while (defined($char = getc FILEHANDLE)){
print $char;
}
close FILEHANDLE;
