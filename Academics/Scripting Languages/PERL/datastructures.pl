#Data Structures
#Array of Arrays (Multi-dimensional Array)
print("-------Array of Arrays (Multi-dimensional Array)--------\n");
my @stuff = ( ['one', 'two', 'three', 'four'],
[7, 6, 5],
['apple', 'orange'],
[0.3, 'random', 'stuff', 'here', 5] );
print $stuff[0][2] , "\n"; # Prints three
print "\n";

#Advanced Data Structures
print("---------Advanced Data Structures----------");
$current = $current->{'R'} ; #move forward
$current = $current->{'L'} ; #move backward
$new = { L =>undef , R=>undef , C} ; #new element
#Insert new element after current element as
$new->{'R'}=$current->{'R'} ;
$current{'R'}->{'L'}= $new;
$current{'R'}=$new ;
$new->{'L'} = $current ;
#current element can be deleted as 
$current->{'L'}->{'R'} = $current->{'R'} ;
$current->{'R'}->{'L'} = $current->{'L'} ;