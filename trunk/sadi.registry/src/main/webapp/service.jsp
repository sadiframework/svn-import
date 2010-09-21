<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.apache.log4j.Logger" %>
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
       <c:forEach var="restriction" items="${service.restrictions}">
        <dt><a href='${restriction.onProperty}'>${restriction.onProperty}</a></dt>
        <dd>(with values from <c:out value="${restriction.valuesFrom}" default="an unknown class"/>)</dd>
       </c:forEach>
      </dl>
    </td>
  </tr>
</table>