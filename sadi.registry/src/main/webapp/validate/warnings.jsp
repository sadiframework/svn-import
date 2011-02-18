<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div id='validation-warnings'>
  <h3>Warning</h3>
  <p>There were warnings validating the service at <a href='${service.URI}'>${service.URI}</a>:</p>
  <blockquote>
    <dl>
     <c:forEach var="warning" items="${warnings}">
      <dt>${warning.message}</dt>
      <dd>${warning.details}</dd>
     </c:forEach>
    </dl>
  </blockquote>
</div>