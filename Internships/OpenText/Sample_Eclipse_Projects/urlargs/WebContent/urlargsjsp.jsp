<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
<%
String q = request.getParameter("a");
String p = request.getParameter("b");
out.print(Integer.parseInt(q)+Integer.parseInt(p));
%>
</body>
</html>