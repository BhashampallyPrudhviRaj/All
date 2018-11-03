<html>
<head>
<title>My First PHP Program</title>
</head>
<body>
<?php
$accept_ip = "127.0.1.1"; //"192.168.43.1";
$accept_host = "localhost";
$IP = $_SERVER['REMOTE_ADDR'];
$HOST = $_SERVER['HTTP_HOST'];
if($IP == $accept_ip || $HOST == $accept_host){
echo "Access Granted";
}
else {
echo "Access Denied";
}
?>
</body>
</html>