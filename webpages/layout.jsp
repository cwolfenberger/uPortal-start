<%@ page errorPage="error.jsp" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jasig.portal.UtilitiesBean" %>
<jsp:useBean id="layoutBean" type="org.jasig.portal.ILayoutBean" class="org.jasig.portal.LayoutBean" scope="session" />

<% String sUserName = (String) session.getAttribute ("userName"); %>
<% UtilitiesBean.preventPageCaching (response); %>

<html>
<head>
<title>Portal Framework</title>
<script language="JavaScript">
<!--hide

function openWin(url, title, width, height)
{
  var newWin = window.open(url, title, 'width=' + width + ',height=' + height +',resizable=yes,scrollbars=yes')
}

//stop hiding-->
</script>
</head>

<% layoutBean.writeBodyTag (request, response, out); %>

<!-- Header -->
<table border=0 cellpadding=0 cellspacing=1 width=100%>
  <tr>
    <td width=100><img src="images/MyIBS.gif" width=100 height=50 border=0></td>
    <td width=300><font face=Arial size=2 color=blue>Hello <%= session.getValue ("userName") == null ? "Guest" : session.getValue ("userName")%>, Welcome to MyIBS!</font><br>
        <font face=Arial size=1 color=#444444><%= UtilitiesBean.getDate (request) %></font></td>
    <td align=right><%= session.getValue ("userName") == null ? "&nbsp;" : "<a href=\"logout.jsp\">Logout</a>" %></td>
  </tr>
</table>

<%
layoutBean.writeTabs (request, response, out);
layoutBean.writeChannels (request, response, out);
%>

<jsp:include page="footer.jsp" flush="true" />

</body>
</html>
