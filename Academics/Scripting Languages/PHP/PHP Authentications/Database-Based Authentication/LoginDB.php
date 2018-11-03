<?php
$name=$_POST['uname'];
$pwd=$_POST['pwd'];
include_once 'db.php';
$result= mysqli_query($con, "select * from login where uname = '$name'");
$numrows= mysqli_num_rows($result);
if($numrows!=0)
{
while($row= mysqli_fetch_array($result))
{
$dbuname=$row['uname'];
$dbpwd=$row['password'];
}
if($name==$dbuname && $pwd==$dbpwd)
echo "<h1>Welcome $dbuname, You are successfully logged in!!!";
else
die("Passwords Mismatch");
}
else
echo "<h1>You are not a registered member.</h1>";
?>