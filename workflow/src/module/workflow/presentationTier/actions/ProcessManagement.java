/*
 * @(#)ProcessManagement.java
 *
 * Copyright 2009 Instituto Superior Tecnico
 * Founding Authors: João Figueiredo, Luis Cruz, Paulo Abrantes, Susana Fernandes
 * 
 *      https://fenix-ashes.ist.utl.pt/
 * 
 *   This file is part of the MyOrg web application infrastructure.
 *
 *   MyOrg is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.*
 *
 *   MyOrg is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with MyOrg. If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package module.workflow.presentationTier.actions;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import module.workflow.activities.ActivityException;
import module.workflow.activities.ActivityInformation;
import module.workflow.activities.WorkflowActivity;
import module.workflow.domain.ProcessFile;
import module.workflow.domain.WorkflowProcess;
import module.workflow.domain.WorkflowProcessComment;
import module.workflow.util.WorkflowFileUploadBean;
import myorg.applicationTier.Authenticate.UserView;
import myorg.domain.exceptions.DomainException;
import myorg.presentationTier.Context;
import myorg.presentationTier.actions.ContextBaseAction;
import myorg.util.VariantBean;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixWebFramework.struts.annotations.Mapping;
import pt.ist.fenixWebFramework.util.DomainReference;
import pt.ist.fenixframework.pstm.AbstractDomainObject;

@Mapping(path = "/workflowProcessManagement")
public class ProcessManagement extends ContextBaseAction {

    public static Map<Class<? extends WorkflowProcess>, ProcessRequestHandler<? extends WorkflowProcess>> handlers = new HashMap<Class<? extends WorkflowProcess>, ProcessRequestHandler<? extends WorkflowProcess>>();

    public ActionForward viewProcess(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws ClassNotFoundException {

	WorkflowProcess process = getProcess(request);
	return viewProcess(process, request);
    }

    @SuppressWarnings("unchecked")
    public ActionForward viewProcess(WorkflowProcess process, final HttpServletRequest request) {

	request.setAttribute("process", process);
	ProcessRequestHandler<WorkflowProcess> handler = (ProcessRequestHandler<WorkflowProcess>) handlers
		.get(process.getClass());
	if (handler != null) {
	    handler.handleRequest(process, request);
	}
	return forward(request, "/workflow/viewProcess.jsp");
    }

    public ActionForward forwardToProcessPage(WorkflowProcess process, HttpServletRequest request) {

	ActionForward forward = new ActionForward();
	forward.setRedirect(true);
	String realPath = "/workflowProcessManagement.do?method=viewProcess&processId=" + process.getExternalId() + "&"
		+ CONTEXT_PATH + "=" + getContext(request).getPath();
	forward.setPath(realPath + "&" + GenericChecksumRewriter.CHECKSUM_ATTRIBUTE_NAME + "="
		+ GenericChecksumRewriter.calculateChecksum(request.getContextPath() + realPath));
	return forward;
    }

    public ActionForward process(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) {

	WorkflowProcess process = getProcess(request);
	WorkflowActivity<WorkflowProcess, ActivityInformation<WorkflowProcess>> activity = getActivity(process, request);
	ActivityInformation<WorkflowProcess> information = getRenderedObject("activityBean");
	return doLifeCycle(information, process, activity, request);
    }

    public ActionForward actionLink(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {

	WorkflowProcess process = getProcess(request);
	WorkflowActivity<WorkflowProcess, ActivityInformation<WorkflowProcess>> activity = getActivity(process, request);
	ActivityInformation<WorkflowProcess> information = populateInformation(process, activity, request);
	return doLifeCycle(information, process, activity, request);
    }

    private ActivityInformation<WorkflowProcess> populateInformation(WorkflowProcess process,
	    WorkflowActivity<WorkflowProcess, ActivityInformation<WorkflowProcess>> activity, HttpServletRequest request)
	    throws Exception {
	ActivityInformation<WorkflowProcess> activityInformation = activity.getActivityInformation(process);
	String parameters = request.getParameter("parameters");
	Class<? extends ActivityInformation> activityClass = activityInformation.getClass();
	if (!StringUtils.isEmpty(parameters)) {
	    for (String parameter : parameters.split(",")) {

		Field field = activityClass.getDeclaredField(parameter);
		Class<?> type = field.getType();
		Object convertedValue = convert(type, request.getParameter(parameter));
		Method declaredMethod = getMethod("set" + parameter.substring(0, 1).toUpperCase() + parameter.substring(1),
			activityClass, convertedValue.getClass());
		declaredMethod.invoke(activityInformation, convertedValue);
	    }
	}
	return activityInformation;
    }

    private Method getMethod(String methodName, Class<? extends ActivityInformation> activityClass,
	    Class<? extends Object> argumentClass) {
	Method method = null;
	try {
	    method = activityClass.getDeclaredMethod(methodName, argumentClass);
	} catch (NoSuchMethodException e) {
	    /*
	     * There's the chance that we just had a mismatch about the argument
	     * classes. For example. the method is defined for a super class and
	     * we were looking for a subclass. So in order to try to recover
	     * we'll try to look for a method with the name 'methodName'.
	     */
	    for (Method declaredMethod : activityClass.getDeclaredMethods()) {
		if (declaredMethod.getName().equals(methodName)) {
		    method = declaredMethod;
		    break;
		}
	    }
	}
	return method;
    }

    private Object convert(Class<?> type, String parameterValue) throws Exception {
	if (DomainReference.class == type) {
	    return AbstractDomainObject.fromExternalId(parameterValue);
	}
	if (type == Integer.class) {
	    return Integer.parseInt(parameterValue);
	}
	if (type == Double.class) {
	    return Double.parseDouble(parameterValue);
	}
	if (type == Float.class) {
	    return Float.parseFloat(parameterValue);
	}
	if (type == String.class) {
	    return parameterValue;
	}
	throw new IllegalArgumentException("Invalid type" + type.getName());
    }

    private ActionForward doLifeCycle(ActivityInformation<WorkflowProcess> information, WorkflowProcess process,
	    WorkflowActivity<WorkflowProcess, ActivityInformation<WorkflowProcess>> activity, HttpServletRequest request) {
	if (information == null) {
	    information = activity.getActivityInformation(process);
	} else {
	    information.markHasForwardedFromInput();
	}

	return executeActivity(process, request, activity, information);
    }

    public ActionForward executeActivity(WorkflowProcess process, HttpServletRequest request,
	    WorkflowActivity<WorkflowProcess, ActivityInformation<WorkflowProcess>> activity,
	    ActivityInformation<WorkflowProcess> information) {
	if (information.hasAllneededInfo()) {
	    try {
		activity.execute(information);
	    } catch (ActivityException e) {
		addMessage(request, e.getMessage());
	    } catch (DomainException e) {
		addMessage(request, e.getMessage());
		return forwardProcessForInput(activity, request, information);
	    }
	    return forwardToProcessPage(process, request);
	}

	return forwardProcessForInput(activity, request, information);
    }

    public static <T extends WorkflowProcess> ActionForward forwardProcessForInput(
	    WorkflowActivity<T, ActivityInformation<T>> activity, HttpServletRequest request, ActivityInformation<T> information) {
	request.setAttribute("information", information);
	if (activity.isDefaultInputInterfaceUsed()) {
	    return forward(request, "/workflow/activityInput.jsp");
	} else {
	    request.setAttribute("inputInterface", activity.getClass().getName().replace('.', '/') + ".jsp");
	    return forward(request, "/workflow/nonDefaultActivityInput.jsp");
	}
    }

    public ActionForward viewComments(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) {

	final WorkflowProcess process = getProcess(request);
	request.setAttribute("process", process);

	Set<WorkflowProcessComment> comments = new TreeSet<WorkflowProcessComment>(WorkflowProcessComment.COMPARATOR);
	comments.addAll(process.getComments());

	process.markCommentsAsReadForUser(UserView.getCurrentUser());
	request.setAttribute("comments", comments);
	request.setAttribute("bean", new VariantBean());

	return forward(request, "/workflow/viewComments.jsp");
    }

    public ActionForward addComment(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) {

	final WorkflowProcess process = getProcess(request);
	String comment = getRenderedObject("comment");
	process.createComment(UserView.getCurrentUser(), comment);

	RenderUtils.invalidateViewState("comment");
	return viewComments(mapping, form, request, response);
    }

    public ActionForward fileUpload(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) {

	final WorkflowProcess process = getProcess(request);
	WorkflowFileUploadBean bean = new WorkflowFileUploadBean(process);
	bean.setSelectedInstance(process.getAvailableFileTypes().get(0));

	request.setAttribute("bean", bean);
	request.setAttribute("process", process);

	return forward(request, "/workflow/fileUpload.jsp");
    }

    public ActionForward upload(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws IOException {
	WorkflowFileUploadBean bean = getRenderedObject("uploadFile");
	final WorkflowProcess process = getProcess(request);

	try {
	    process.addFile(bean.getSelectedInstance(), bean.getDisplayName(), bean.getFilename(), consumeInputStream(bean
		    .getInputStream()), bean);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	return viewProcess(process, request);

    }

    public ActionForward uploadPostBack(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws IOException {
	WorkflowFileUploadBean bean = getRenderedObject("uploadFile");
	final WorkflowProcess process = getProcess(request);

	request.setAttribute("bean", bean);
	request.setAttribute("process", process);

	return forward(request, "/workflow/fileUpload.jsp");
    }

    public ActionForward viewLogs(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) {

	final WorkflowProcess process = getProcess(request);

	request.setAttribute("operationLogs", process.getExecutionLogsSet());

	request.setAttribute("process", process);
	return forward(request, "/workflow/viewLogs.jsp");
    }

    public ActionForward downloadFile(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws IOException {

	final ProcessFile file = getDomainObject(request, "fileId");
	return download(response, file.getFilename(), file.getContent(), file.getContentType());
    }

    public ActionForward removeFile(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) {

	final ProcessFile file = getDomainObject(request, "fileId");
	final WorkflowProcess process = file.getProcess();
	process.removeFiles(file);

	return viewProcess(process, request);

    }

    public ActionForward viewRemovedFiles(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) {

	final WorkflowProcess process = getProcess(request);
	request.setAttribute("process", process);
	return forward(request, "/workflow/viewRemovedFiles.jsp");

    }

    private <T extends WorkflowProcess> WorkflowActivity<T, ActivityInformation<T>> getActivity(WorkflowProcess process,
	    HttpServletRequest request) {
	String activityName = request.getParameter("activity");
	return process.getActivity(activityName);
    }

    protected <T extends WorkflowProcess> T getProcess(HttpServletRequest request) {
	return (T) getDomainObject(request, "processId");
    }

    public static ActionForward forwardToProcess(final WorkflowProcess process) {
	return new ActionForward("/workflowProcessManagement.do?method=viewProcess&processId=" + process.getExternalId());
    }

    public static <T extends WorkflowProcess> void registerProcessRequestHandler(Class<T> workflowProcessClass,
	    ProcessRequestHandler<T> handler) {
	handlers.put(workflowProcessClass, handler);
    }

    public static interface ProcessRequestHandler<T extends WorkflowProcess> {
	public void handleRequest(T process, HttpServletRequest request);
    }

    @Override
    public Context createContext(String contextPathString, HttpServletRequest request) {
	WorkflowProcess process = getProcess(request);
	return process.getLayout();
    }
}
