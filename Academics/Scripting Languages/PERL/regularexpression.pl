my $s = 'Perl regular expression is powerful';
print "Match Found\n" if( $s =~ /ul/);
print"\n";

my @words= (
'Perl',
'regular expression',
'is',
'a very powerful',
'feature' );
foreach(@words){
print("$_ \n") if($_ !~ /er/);
}
print"\n";

my @html = ('<p>', 'html fragement', '</p>', '<br>', '<span>This is a span</span>' );
foreach(@html){
print("$_ \n") if($_ =~ m"/"); #print("$_ \n") if($_ =~ "\/");  
}
print"\n";

my $s = "Regular expression";
print "match" if $s =~ /Expression/i; #no o/p ==>  print "match" if $s =~ /Expra\na\na =~ /a$/mession/;