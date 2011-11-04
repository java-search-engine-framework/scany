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
package org.jhlabs.scany.client;

import org.jhlabs.scany.entity.Schema;
import org.jhlabs.scany.index.AnyIndexer;
import org.jhlabs.scany.index.AnyIndexerImpl;
import org.jhlabs.scany.search.AnyExpertSearcher;
import org.jhlabs.scany.search.AnySearcher;
import org.jhlabs.scany.search.AnySmartSearcher;

/**
 * 
 * <p>Created: 2008. 01. 07 오후 7:33:28</p>
 * 
 * @author Gulendol
 *
 */
public class ScanyClientImpl extends ClientConfig implements ScanyClient {

	public ScanyClientImpl() {
		
	}

	public AnySearcher getSearcher(String schemaId) {
		Schema schema = getSchema(schemaId);
		
		if(schema == null)
			throw new ScanyClientException("등록된 스키마가 아닙니다. (Schema ID: " + schemaId + ")");
		
		return getSearcher(schema);
	}
	
	public AnySearcher getSearcher(Schema schema) {
		AnySearcher anySearcher = null;

		if(schema.isExpertQueryMode())
			anySearcher = getExpertSearcher(schema);
		else
			anySearcher = getAmateurSearcher(schema);
		
		return anySearcher;
	}

	public AnySearcher getExpertSearcher(Schema schema) {
		try {
			AnySearcher anySearcher = null;
			
			anySearcher = new AnyExpertSearcher(schema);
			
			return anySearcher;
		} catch(Exception e) {
			throw new ScanyClientException("Scany 검색기(AnyExpertSearcher) 생성에 실패했습니다.", e);
		}
	}

	public AnySearcher getAmateurSearcher(Schema schema) {
		try {
			AnySearcher anySearcher = null;
			
			anySearcher = new AnySmartSearcher(schema);
			
			return anySearcher;
		} catch(Exception e) {
			throw new ScanyClientException("Scany 검색기(AnyAmateurSearcher) 생성에 실패했습니다.", e);
		}
	}

	public AnyIndexer getIndexer(String schemaId) {
		Schema schema = getSchema(schemaId);
		
		if(schema == null)
			throw new ScanyClientException("등록된 스키마가 아닙니다. (Schema ID: " + schemaId + ")");
		
		return getIndexer(schema);
	}
	
	public AnyIndexer getIndexer(Schema schema) {
		try {
			AnyIndexer anyIndexer = null;
			
			anyIndexer = new AnyIndexerImpl(schema);
			
			return anyIndexer;
		} catch(Exception e) {
			throw new ScanyClientException("Scany 색인기(AnyIndexer) 생성에 실패했습니다.", e);
		}
	}
}