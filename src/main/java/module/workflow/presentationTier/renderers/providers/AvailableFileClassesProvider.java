/*
 * @(#)AvailableFileClassesProvider.java
 *
 * Copyright 2009 Instituto Superior Tecnico
 * Founding Authors: Luis Cruz, Paulo Abrantes
 * 
 *      https://fenix-ashes.ist.utl.pt/
 * 
 *   This file is part of the Case Handleing Based Workflow Module.
 *
 *   The Case Handleing Based Workflow Module is free software: you can
 *   redistribute it and/or modify it under the terms of the GNU Lesser General
 *   Public License as published by the Free Software Foundation, either version 
 *   3 of the License, or (at your option) any later version.
 *
 *   The Workflow Module is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with the Workflow Module. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package module.workflow.presentationTier.renderers.providers;

import module.workflow.domain.WorkflowProcess;
import module.workflow.util.WorkflowFileUploadBean;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;

/**
 * 
 * @author Paulo Abrantes
 * 
 */
public class AvailableFileClassesProvider implements DataProvider {

    @Override
    public Converter getConverter() {
        return null;
    }

    @Override
    public Object provide(final Object source, final Object current) {
        final WorkflowFileUploadBean bean = (WorkflowFileUploadBean) source;
        final WorkflowProcess process = bean.getProcess();
        return process.getUploadableFileTypes();
    }

}
