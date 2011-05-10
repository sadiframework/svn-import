<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="ca.wilkinsonlab.sadi.beans.ServiceBean" %>
<%@ page import="ca.wilkinsonlab.sadi.registry.*" %>
<%
	if (pageContext.getAttribute("service") == null) {
		String serviceURI = request.getParameter("serviceURI");
		if (serviceURI != null) {
			Logger log = Logger.getLogger("ca.wilkinsonlab.sadi.registry");
			Registry registry = null;
			try {
				registry = Registry.getRegistry();
				ServiceBean service = registry.getServiceBean(serviceURI);
				pageContext.setAttribute("service", service);
			} catch (Exception e) {
				log.error(String.format("error retrieving service definition for %s: %s", serviceURI, e));
			} finally {
				if (registry != null)
					registry.getModel().close();
			}
		}
	}
%>
<c:choose>
  <c:when test='${service != null}'>
<table class='service-detail'>
  <tr>
    <th>Name</th>
    <td>${service.name}</td>
  </tr>
  <tr>
    <th>Description</th>
    <td>${service.description}</td>
  </tr>
  <tr>
    <th>Properties attached</th>
    <td>
      <dl>
       <c:forEach var="restriction" items="${service.restrictionBeans}">
        <dt><a href='${restriction.onPropertyURI}'><c:out value="${restriction.onPropertyLabel}" default="${restriction.onPropertyURI}"/></a></dt>
         <c:choose>
	      <c:when test='${!empty restriction.valuesFromURI}'>
	      <dd>(with values from <a href='${restriction.valuesFromURI}'><c:out value="${restriction.valuesFromLabel}" default="${restriction.valuesFromURI}"/></a>)</dd>
	      </c:when>
	      <c:otherwise>
	      <dd>(with values from an unknown class)</dd>
	      </c:otherwise>
	     </c:choose>
       </c:forEach>
      </dl>
    </td>
  </tr>
</table>
  </c:when>
  <c:otherwise>
<p>No such service is registered.</p>
  </c:otherwise>
</c:choose>