<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
  <title>BitMesh</title>
  <link rel="icon" href="https://www.bitmesh.network/favicon.png" type="image/png">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.min.css">
  <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>

  <style>
    td {
      padding: 5px;
    }
  </style>
</head>
<body>
<div style="text-align: center;"><h1>Transactions</h1></div>
<table>
  <tr style="text-align: center;">
    <td>Hash</td>
    <td>Lock Time</td>
    <td>Date Submitted</td>
    <td>Is Mainnet</td>
  </tr>
  <c:forEach items="${transactions}" var="tx">
    <tr>
      <c:choose>
      <c:when test="${tx.isSucceeded()}">
        <td><a href="https://www.blocktrail.com/tBTC/tx/${tx.txId}"><c:out value="${tx.txId}" /></a></td>
      </c:when>
      <c:otherwise>
        <td><c:out value="${tx.txId}" /></td>
      </c:otherwise>
      </c:choose>
      <td><c:out value="${tx.lockTime}" /></td>
      <td><c:out value="${tx.dateSubmitted}" /></td>
      <td><c:out value="${tx.isMainNet()}" /></td>
    </tr>
  </c:forEach>
  </tr>
</table>
</body>
</html>
