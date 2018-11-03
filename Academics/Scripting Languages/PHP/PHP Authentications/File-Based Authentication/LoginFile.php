<?php
$uname = $_POST['uname'];
$pwd = $_POST['pwd'];
$pwd_file = 'pwds.txt';
if(!$fh = fopen($pwd_file, "r")){
die("<h1>Unable to open the password file");
}
$flag = 0;
while(!feof($fh)) {
$line = fgets($fh,4096);
$user_pass = explode(":", $line);
if($user_pass[0]==$uname && rtrim($user_pass[1])==$pwd){
$flag = 1;
break;
}
}
if($flag == 1)
echo "<center><h1>Login Successful</h1></center>";
else
echo "<center><h1>Login failed</h1></center>";
?>