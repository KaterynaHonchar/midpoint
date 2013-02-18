/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class UserBrowserDialog extends ModalWindow {

    private static final Trace LOGGER = TraceManager.getTrace(UserBrowserDialog.class);
    private IModel<UserBrowserDto> model;
    private boolean initialized;
    private PrismContext prismContext;

    public UserBrowserDialog(String id, PrismContext prismContext) {
        super(id);

        setTitle(createStringResource("userBrowserDialog.title"));
        this.prismContext = prismContext;
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(UserBrowserDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(900);
        setInitialHeight(500);
        setWidthUnit("px");

        model = new LoadableModel<UserBrowserDto>(false) {

            @Override
            protected UserBrowserDto load() {
                return new UserBrowserDto();
            }
        };

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (initialized) {
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()));
        initialized = true;
    }

    private void initLayout(WebMarkupContainer content) {
        Form mainForm = new Form("mainForm");
        content.add(mainForm);

        TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model, "searchText"));
        mainForm.add(search);

        CheckBox nameCheck = new CheckBox("nameCheck", new PropertyModel<Boolean>(model, "name"));
        mainForm.add(nameCheck);
        CheckBox fullNameCheck = new CheckBox("fullNameCheck", new PropertyModel<Boolean>(model, "fullName"));
        mainForm.add(fullNameCheck);
        CheckBox givenNameCheck = new CheckBox("givenNameCheck", new PropertyModel<Boolean>(model, "givenName"));
        mainForm.add(givenNameCheck);
        CheckBox familyNameCheck = new CheckBox("familyNameCheck", new PropertyModel<Boolean>(model, "familyName"));
        mainForm.add(familyNameCheck);


        List<IColumn<SelectableBean<UserType>, String>> columns = initColumns();
        TablePanel table = new TablePanel<SelectableBean<UserType>>("table",
                new ObjectDataProvider(getPageBase(), UserType.class), columns);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
                createStringResource("userBrowserDialog.button.searchButton")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getPageBase().getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }
        };
        mainForm.add(searchButton);

        AjaxLinkButton cancelButton = new AjaxLinkButton("cancelButton",
                createStringResource("userBrowserDialog.button.cancelButton")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancelButton);
    }

    private PageBase getPageBase() {
        return (PageBase) getPage();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private List<IColumn<SelectableBean<UserType>, String>> initColumns() {
        List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<IColumn<SelectableBean<UserType>, String>>();

        columns.add(new IconColumn<SelectableBean<UserType>>(createStringResource("userBrowserDialog.type")) {

            @Override
            protected IModel<ResourceReference> createIconModel(final IModel<SelectableBean<UserType>> rowModel) {
                return new AbstractReadOnlyModel<ResourceReference>() {

                    @Override
                    public ResourceReference getObject() {
                        UserType user = rowModel.getObject().getValue();
                        CredentialsType credentials = user.getCredentials();

                        if (credentials != null) {
                            Boolean allowedAdmin = credentials.isAllowedIdmAdminGuiAccess();
                            if (allowedAdmin != null && allowedAdmin) {
                                return new SharedResourceReference(ImgResources.class, "user_red.png");
                            }
                        }

                        return new SharedResourceReference(ImgResources.class, "user.png");
                    }
                };
            }
        });

        IColumn column = new LinkColumn<SelectableBean<UserType>>(createStringResource("userBrowserDialog.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                UserType user = rowModel.getObject().getValue();
                userDetailsPerformed(target, user);
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("userBrowserDialog.givenName"), "givenName", "value.givenName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("userBrowserDialog.familyName"), "familyName", "value.familyName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("userBrowserDialog.fullName"), "fullName", "value.fullName.orig");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<UserType>, String>(createStringResource("userBrowserDialog.email")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem, String componentId,
                                     IModel<SelectableBean<UserType>> rowModel) {

                String email = rowModel.getObject().getValue().getEmailAddress();
                cellItem.add(new Label(componentId, new Model<String>(email)));
            }
        };
        columns.add(column);

        return columns;
    }

    private void searchPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createQuery();
        target.add(getPageBase().getFeedbackPanel());

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        table.setCurrentPage(0);

        target.add(panel);
    }

    private TablePanel getTable() {
        return (TablePanel) getContent().get("mainForm:table");
    }

    private ObjectQuery createQuery() {
        UserBrowserDto dto = model.getObject();
        ObjectQuery query = null;
        if (StringUtils.isEmpty(dto.getSearchText())) {
            return null;
        }

        try {
			List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

			PolyStringNormalizer normalizer = prismContext.getDefaultPolyStringNormalizer();
			if (normalizer == null) {
				normalizer = new PrismDefaultPolyStringNormalizer();
			}

			String normalizedString = normalizer.normalize(dto.getSearchText());

			if (dto.isName()) {
				filters.add(SubstringFilter.createSubstring(UserType.class, prismContext, UserType.F_NAME,
						normalizedString));
			}

			if (dto.isFamilyName()) {
				filters.add(SubstringFilter.createSubstring(UserType.class, prismContext,
						UserType.F_FAMILY_NAME, normalizedString));
			}
			if (dto.isFullName()) {
				filters.add(SubstringFilter.createSubstring(UserType.class, prismContext,
						UserType.F_FULL_NAME, normalizedString));
			}
			if (dto.isGivenName()) {
				filters.add(SubstringFilter.createSubstring(UserType.class, prismContext,
						UserType.F_GIVEN_NAME, normalizedString));
			}

			if (!filters.isEmpty()) {
				query = new ObjectQuery().createObjectQuery(OrFilter.createOr(filters));
			}
		} catch (Exception ex) {
			error(getString("userBrowserDialog.message.queryError") + " " + ex.getMessage());
			LoggingUtils.logException(LOGGER, "Couldn't create query filter.", ex);
		}

        return query;
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }

    public void userDetailsPerformed(AjaxRequestTarget target, UserType user) {
        close(target);
    }
}
