package com.evolveum.midpoint.report.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.base.JRBaseParameter;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.fill.JRFillParameter;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class MidPointLocalQueryExecutor extends MidPointQueryExecutor{
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointLocalQueryExecutor.class);
	private ObjectQuery query;
	private String script;
	private Class type;
	private ReportService reportService;
	
	
	public MidPointLocalQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap, ReportService reportService){
		super(jasperReportsContext, dataset, parametersMap);
	}
	
	protected MidPointLocalQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap) {
		super(jasperReportsContext, dataset, parametersMap);
		
		JRFillParameter fillparam = (JRFillParameter) parametersMap.get(JRParameter.REPORT_PARAMETERS_MAP);
		Map reportParams = (Map) fillparam.getValue();
		reportService = (ReportService) parametersMap.get(ReportService.PARAMETER_REPORT_SERVICE).getValue();

		parseQuery();
	}
	
	@Override
	protected Object getParsedQuery(String query, Map<QName, Object> expressionParameters) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		return reportService.parseQuery(query, expressionParameters);
	}
	
	@Override
	protected Collection searchObjects(Object query, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException{
		return reportService.searchObjects((ObjectQuery) query, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
	}
	
	@Override
	protected Collection evaluateScript(String script,
			Map<QName, Object> parameters) throws SchemaException, ObjectNotFoundException,
			SecurityViolationException, CommunicationException, ConfigurationException,
			ExpressionEvaluationException {
		return reportService.evaluateScript(script, getParameters());
	}

	@Override
	protected Collection<AuditEventRecord> searchAuditRecords(String script, Map<QName, Object> parameters) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		return reportService.evaluateAuditScript(script, parameters);
	}
	
	@Override
	protected JRDataSource createDataSource(Collection results) {
		return new MidPointDataSource(results);
	}
	
	
	public String getScript() {
		return script;
	}
	public ObjectQuery getQuery() {
		return query;
	}
	public Class getType() {
		return type;
	}
	
//	private Object getObjectQueryFromParameters(){
//		JRParameter[] params = dataset.getParameters();
//		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
//		for (JRParameter param : params){
//			if ("finalQuery".equals(param.getName())){
//				return getParameterValue(param.getName());
//			}
//		}
//		return null;
//	}
//	
//	private Map<QName, Object> getParameters(){
//		JRParameter[] params = dataset.getParameters();
//		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
//		for (JRParameter param : params){
//			LOGGER.trace(((JRBaseParameter)param).getName());
//			Object v = getParameterValue(param.getName());
//			try{ 
//			expressionParameters.put(new QName(param.getName()), new PrismPropertyValue(v));
//			} catch (Exception e){
//				//just skip properties that are not important for midpoint
//			}
//			
//			LOGGER.trace("p.val: {}", v);
//		}
//		return expressionParameters;
//	}
//	
//	private Map<QName, Object> getPromptingParameters(){
//		JRParameter[] params = dataset.getParameters();
//		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
//		for (JRParameter param : params){
//			if (param.isSystemDefined()){
//				continue;
//			}
//			if (!param.isForPrompting()){
//				continue;
//			}
//			LOGGER.trace(((JRBaseParameter)param).getName());
//			Object v = getParameterValue(param.getName());
//			try{ 
//			expressionParameters.put(new QName(param.getName()), new PrismPropertyValue(v));
//			} catch (Exception e){
//				//just skip properties that are not important for midpoint
//			}
//			
//			LOGGER.trace("p.val: {}", v);
//		}
//		return expressionParameters;
//	}
//	
//	@Override
//	protected void parseQuery() {
//		// TODO Auto-generated method stub
//		
//		
//		
//		String s = dataset.getQuery().getText();
//		
//		JRBaseParameter p = (JRBaseParameter) dataset.getParameters()[0];
//		
//		Map<QName, Object> expressionParameters = getParameters();
//		LOGGER.info("query: " + s);
//		if (StringUtils.isEmpty(s)){
//			query = null;
//		} else {
//			try {
//			if (s.startsWith("<filter")){
//			
//				Object queryParam = getObjectQueryFromParameters();
//				if (queryParam != null){
//					if (queryParam instanceof String){
//						s = (String) queryParam;
//					} else if (queryParam instanceof ObjectQuery){
//						query = (ObjectQuery) queryParam;
//					}
//				}
//				
//				if (query == null){
//					query = reportService.parseQuery(s, expressionParameters);
//				}
//			} else if (s.startsWith("<code")){
//				String normalized = s.replace("<code>", "");
//				script = normalized.replace("</code>", "");
//				
//			}
//			} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//	}
//
	//	@Override
//	public JRDataSource createDatasource() throws JRException {
//		Collection<PrismObject<? extends ObjectType>> results = new ArrayList<>();
//		
//		try {
//			if (query == null && script == null){
//				throw new JRException("Neither query, nor script defined in the report.");
//			}
//			
//			if (query != null){
//				results = reportService.searchObjects(query, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
//			} else {
//				if (script.contains("AuditEventRecord")){
//					Collection<AuditEventRecord> audtiEventRecords = reportService.evaluateAuditScript(script, getPromptingParameters());
//					return new JRBeanCollectionDataSource(audtiEventRecords);
//				} else {
//					results = reportService.evaluateScript(script, getParameters());
//				}
//			}
//		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
//				| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
//			// TODO Auto-generated catch block
//			throw new JRException(e);
//		}
//		
//		MidPointDataSource mds = new MidPointDataSource(results);
//		
//		return mds;
//	}
//	
//	
//	@Override
//	public void close() {
////		throw new UnsupportedOperationException("QueryExecutor.close() not supported");
//		//nothing to DO
//	}
//
//	@Override
//	public boolean cancelQuery() throws JRException {
//		 throw new UnsupportedOperationException("QueryExecutor.cancelQuery() not supported");
//	}
//
//	@Override
//	protected String getParameterReplacement(String parameterName) {
//		 throw new UnsupportedOperationException("QueryExecutor.getParameterReplacement() not supported");
//	}

	

	
	

}
