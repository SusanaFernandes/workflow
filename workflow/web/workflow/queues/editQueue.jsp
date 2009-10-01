<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/fenix-renderers.tld" prefix="fr"%>

<%@page import="myorg.presentationTier.actions.ContextBaseAction"%>
<%@page import="module.workflow.presentationTier.WorkflowQueueLayoutContext"%>

<h2><bean:message key="title.queueEdit" bundle="WORKFLOW_RESOURCES"/></h2> 

<% 
 WorkflowQueueLayoutContext context = (WorkflowQueueLayoutContext) ContextBaseAction.getContext(request);
%>

<table class="table mtop15 mbottom15">
	<tr>
		<th><bean:message key="label.queue.name" bundle="WORKFLOW_RESOURCES"/></th>
		<th><bean:message key="label.queue.type" bundle="WORKFLOW_RESOURCES"/></th>
	</tr>
	<tr>
		<td><fr:view name="bean" property="queue.name"/></td>
		<td><fr:view name="bean" property="queue.class" layout="name-resolver"/></td>
	</tr>
</table>

<jsp:include page="<%= context.getEditQueue() %>"/>