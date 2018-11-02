my $prog = shift @ARGV; 
die "usage: perl sample.pl <file>" unless defined $prog; 
my $found = 0; 
foreach my $dir (split /:/, $ENV{PATH}) { 
	if (-x "$dir/$prog") { 
		print "$dir/$progn"; 
		$found = 1; 
		last; 
	} 
} 
print "$prog not found in PATH" unless $found;