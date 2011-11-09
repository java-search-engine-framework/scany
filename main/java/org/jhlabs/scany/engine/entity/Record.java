/*******************************************************************************
 * Copyright (c) 2008 Jeong Ju Ho.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Jeong Ju Ho - initial API and implementation
 ******************************************************************************/
package org.jhlabs.scany.engine.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 레코드 정보를 담고 있다.
 * 
 * @author Gulendol
 *
 */
public class Record {

	private RecordKey primaryKey;
	
	private Map values;

	public Record() {
		values = new HashMap();
	}
	
	/**
	 * @return the primaryKey
	 */
	public RecordKey getPrimaryKey() {
		return primaryKey;
	}

	/**
	 * @param primaryKey the primaryKey to set
	 * @throws ScanyEntityException
	 */
	public void setPrimaryKey(RecordKey primaryKey) throws RecordKeyException {
		this.primaryKey = primaryKey;
	}

	/**
	 * @param primaryKey the primaryKey to set
	 * @throws RecordKeyException 
	 */
	public void setPrimaryKey(String primaryKey, Relation schema) throws RecordKeyException {
		this.primaryKey = new RecordKey(primaryKey, schema);
	}

	/**
	 * 컬럼명에 해당하는 컬럼의 값을 반환한다.
	 * @param columnName 컬럼명
	 * @return
	 */
	public String getColumnValue(String columnName) {
		return (String)values.get(columnName);
	}
	
	/**
	 * 컬럼을 추가한다.
	 * @param columnName 컬럼명
	 * @param columnValue 컬럼의 값
	 */
	public void addColumnValue(String columnName, String columnValue) {
		values.put(columnName, columnValue);
	}
	
}
