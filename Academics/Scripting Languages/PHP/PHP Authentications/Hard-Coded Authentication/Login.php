<?php
$uname = $_POST['uname'];
$pwd = $_POST['pwd'];
if($uname.equalsIgnoreCase("sample") && $pwd.equals("123"))
echo "Login Successful";
else
echo "Login Failed";
?>