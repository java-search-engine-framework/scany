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
package org.jhlabs.scany.engine.index;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jhlabs.scany.engine.entity.Attribute;
import org.jhlabs.scany.engine.entity.AttributeMap;
import org.jhlabs.scany.engine.entity.Record;
import org.jhlabs.scany.engine.entity.RecordKey;
import org.jhlabs.scany.engine.entity.Relation;
import org.jhlabs.scany.util.StringUtils;

/**
 * 색인 추가(insert), 색인 갱신(update), 색인 삭제(delete) 기능을 담당한다.
 * 
 * @author gulendol
 */
public class LuceneIndexer implements AnyIndexer {

	private Relation relation;

	private Directory directory;

	private IndexWriter indexWriter;

	/**
	 * 생성자
	 * 
	 * @param schema Schema
	 * @throws MultipartRequestzException
	 */
	public LuceneIndexer(Relation schema) throws AnyIndexException {
		this.relation = schema;
		initialize(false);
	}

	public Relation getSchema() throws AnyIndexException {
		return relation;
	}

	/**
	 * 초기화
	 * 
	 * @param rebuild 색인DB 초기화 여부
	 * @throws AnyIndexException
	 */
	private void initialize(boolean rebuild) throws AnyIndexException {
		try {
			if(relation == null)
				throw new AnyIndexException("등록된 스키마가 아닙니다.(Schema is null)");
			
			boolean create = true;

			File repository = new File(relation.getDirectory());

			// 색인 디렉토리 생성되어 있지 않거나, 비어 있으면 새로 만든다.
			if(!rebuild && repository.exists()) {
				if(repository.list().length > 0)
					create = false;
			}

			directory = FSDirectory.getDirectory(repository);

			if(rebuild) {
				indexWriter.close();
				indexWriter = null;
			}

			indexWriter = new IndexWriter(directory, relation.getAnalyzer(), create);

			// Performance 설정
			indexWriter.setMergeFactor(relation.getMergeFactor());
			indexWriter.setMaxMergeDocs(relation.getMaxMergeDocs());

		} catch(IOException e) {
			throw new AnyIndexException("색인기(AnyIndexer)를 초기화할 수 없습니다.", e);
		}
	}

	/**
	 * 색인등록
	 * 
	 * @param record
	 * @throws AnyIndexException
	 */
	public void insert(Record record) throws AnyIndexException {
		if(exists(record.getRecordKey()))
			throw new AnyIndexException("동일한 Primary Key를 가진 레코드(Record)가 이미 존재합니다.");

		try {
			// Primary Key 생성
			RecordKey primaryKey = record.getRecordKey();

			if(primaryKey.hasWildcard())
				throw new IllegalArgumentException("PrimaryKey가 와일드카드 문자를 포함하고 있습니다.");

			Document document = recordToDocument(record); 
			
			indexWriter.addDocument(document);

		} catch(Exception e) {
			throw new AnyIndexException("색인 등록(insert)에 실패했습니다.", e);
		}
	}

	public void merge(Record record) throws AnyIndexException {
		if(exists(record.getRecordKey()))
			update(record);
		else
			insert(record);
	}
	
	/**
	 * 색인수정.
	 * primaryKey에 해당하는 레코드를 삭제하고, 새로운 record를 insert한다.
	 * 
	 * @param primaryKey 갱신 대상 레코드의 키
	 * @param record 수정 대상 레코드
	 * @throws AnyIndexException
	 */
	public void update(Record record) throws AnyIndexException {
		try {
			if(record.getRecordKey().hasWildcard())
				throw new IllegalArgumentException("PrimaryKey가 와일드카드 문자를 포함하고 있습니다.");

			Document document = recordToDocument(record); 
			Term term = new Term(RecordKey.RECORD_KEY, record.getRecordKey().getRecordKeyString());

			indexWriter.updateDocument(term, document);
		} catch(Exception e) {
			throw new AnyIndexException("색인 수정(update)에 실패했습니다.", e);
		}
	}

	/**
	 * 색인삭제. 와일드카드(*)를 사용한 하위 key 일괄 삭제 가능.
	 * 
	 * @param recordKey 삭제 대상 레코드의 키
	 * @param isAutoOptimizeOff Auto Optimize 기능을 강제로 끌지 여부.
	 * @throws AnyIndexException
	 */
	public void delete(RecordKey recordKey) throws AnyIndexException {
		Searcher searcher = null;

		try {
			String rkey = recordKey.getRecordKeyString();

			/**
			 * Term 선택 순서대로 속도에서 차이가 난다.
			 * WildcardTerm은 속도가 아주 느리다.
			 * --------------------------------
			 * 0: Term 1: PrefixQuery 2: WildcardQuery
			 */
			int whatQuery = 0;

			if(recordKey.hasWildcard()) {
				int wildcardCnt = 0;

				if(rkey.indexOf("?") != -1) {
					wildcardCnt = 99;
				} else {
					wildcardCnt = StringUtils.search(rkey, "*");
				}

				if(wildcardCnt == 1) {
					if(rkey.indexOf("*") == rkey.length() - 1) {
						whatQuery = 1;

						// PrefixTerm을 위해 "*" 제거
						rkey = rkey.substring(0, rkey.length() - 1);

					} else {
						whatQuery = 2;
					}
				} else {
					whatQuery = 2;
				}
			}

			Term term = new Term(RecordKey.RECORD_KEY, rkey);

			if(whatQuery == 0) {
				indexWriter.deleteDocuments(term);

			} else {
				synchronized(directory) {
					searcher = new IndexSearcher(directory);

					Query query = null;

					if(whatQuery == 1) {
						query = new PrefixQuery(term);
					} else if(whatQuery == 2) {
						query = new WildcardQuery(term);
					}

					Hits hits = searcher.search(query);

					Document doc = null;

					for(int i = 0; i < hits.length(); i++) {
						doc = hits.doc(i);
						term = new Term(RecordKey.RECORD_KEY, doc.get(RecordKey.RECORD_KEY));

						indexWriter.deleteDocuments(term);
					}

					searcher.close();
					searcher = null;
				}
			}

		} catch(Exception e) {
			throw new AnyIndexException("색인 삭제(delete)에 실패했습니다.", e);
		} finally {
			try {
				if(searcher != null)
					searcher.close();
			} catch(Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	/**
	 * 발생한 트랜잭션에 대해 Optimize를 수행한다.
	 */
	public void optimize() throws AnyIndexException {
		try {
			indexWriter.optimize();
		} catch(IOException e) {
			throw new AnyIndexException("색인 최적화(optimize) 작업에 실패했습니다.", e);
		}
	}

	/**
	 * 색인DB를 완전히 삭제한다.
	 * 
	 * @throws AnyIndexException
	 */
	public void destroy() throws AnyIndexException {
		initialize(true);
	}
	
	/**
	 * 색인 작업을 종료한다.
	 */
	public void close() throws AnyIndexException {
		try {
			indexWriter.close();
		} catch(IOException e) {
			throw new AnyIndexException("색인 작업 종료에 실패했습니다.", e);
		}
	}

	/**
	 * 해당 Key가 존재하는 여부를 반환.
	 * 와일드카드 문자가 포함된 primaryKey를 지정하면 안된다.
	 * 
	 * @param recordKey 레코드의 키
	 * @return
	 * @throws AnyIndexException
	 */
	public boolean exists(RecordKey recordKey) throws AnyIndexException {
		IndexSearcher searcher = null;

		try {
			searcher = new IndexSearcher(directory);

			Term term = new Term(RecordKey.RECORD_KEY, recordKey.getRecordKeyString());
			Query query = new TermQuery(term);

			Hits hits = searcher.search(query);

			int hitsLength = hits.length();

			searcher.close();
			searcher = null;

			return (hitsLength > 0);

		} catch(Exception e) {
			throw new AnyIndexException("색인 존재여부 확인에 실패했습니다.", e);
		} finally {
			try {
				if(searcher != null)
					searcher.close();
			} catch(Exception e2) {
				e2.printStackTrace();
			}
		}
	}
	
	/**
	 * 레코드(Record)를 도큐먼트(Document)로 전환 후 반환한다.
	 * @param record
	 * @return Record
	 * @throws AnyIndexException
	 */
	private Document recordToDocument(Record record) throws AnyIndexException {
		try {
			// 컬럼속성
			AttributeMap attributeMap = relation.getAttributeMap();

			if(attributeMap.size() == 0)
				throw new IllegalArgumentException("Column 속성이 정의되어 있지 않습니다.");

			Document document = new Document();
			Field field = null;
			Field.Index index = null;
			Field.Store store = null;

			// Primary Key 필드
			field = new Field(RecordKey.RECORD_KEY, record.getRecordKey().getRecordKeyString(), Field.Store.YES,
					Field.Index.UN_TOKENIZED);
			document.add(field);

			// 일반 필드 분석
			Iterator<Attribute> iter = attributeMap.values().iterator();

			while(iter.hasNext()) {
				Attribute attribute = iter.next();
				String value = record.getValue(attribute.getName());

				if(value == null)
					throw new IllegalArgumentException("[" + attribute.getName() + "] Column의 값이 지정되어 있지 않습니다.");

				// 긴 내용 또는 바이너리 데이터의 압축 여부
				if(attribute.isCompressable())
					store = Field.Store.COMPRESS;
				else
					store = attribute.isStorable() ? Field.Store.YES : Field.Store.NO;

				// 색인여부, 토큰분리 여부
				if(attribute.isTokenizable())
					index = Field.Index.TOKENIZED;
				else
					index = attribute.isIndexable() ? Field.Index.UN_TOKENIZED : Field.Index.NO;

				field = new Field(attribute.getName(), value, store, index);
				
				// boost factor
				field.setBoost(attribute.getBoost());

				document.add(field);
			}

			return document;
		} catch(Exception e) {
			throw new AnyIndexException("레코드(Record) 생성에 실패했습니다.", e);
		}
	}
}
