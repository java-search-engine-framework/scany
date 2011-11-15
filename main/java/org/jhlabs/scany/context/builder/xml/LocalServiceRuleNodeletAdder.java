/*
 *  Copyright (c) 2008 Jeong Ju Ho, All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.jhlabs.scany.context.builder.xml;

import java.util.Properties;

import org.jhlabs.scany.context.builder.ScanyContextBuilderAssistant;
import org.jhlabs.scany.context.rule.FileSpoolTransactionRule;
import org.jhlabs.scany.context.rule.LocalServiceRule;
import org.jhlabs.scany.context.rule.SpoolingRule;
import org.jhlabs.scany.context.type.SpoolingMode;
import org.jhlabs.scany.util.xml.Nodelet;
import org.jhlabs.scany.util.xml.NodeletAdder;
import org.jhlabs.scany.util.xml.NodeletParser;
import org.w3c.dom.Node;

/**
 * Translet Map Parser.
 * 
 * <p>Created: 2008. 06. 14 오전 4:39:24</p>
 */
public class LocalServiceRuleNodeletAdder implements NodeletAdder {
	
	protected ScanyContextBuilderAssistant assistant;
	
	private String prefixPath;

	/**
	 * Instantiates a new content nodelet adder.
	 * 
	 * @param parser the parser
	 * @param assistant the assistant for Context Builder
	 */
	public LocalServiceRuleNodeletAdder(ScanyContextBuilderAssistant assistant) {
		this.assistant = assistant;
	}

	/**
	 * Gets the prefix path.
	 * 
	 * @return the prefixPath
	 */
	public String getPrefixPath() {
		return prefixPath;
	}

	/**
	 * Sets the prefix path.
	 * 
	 * @param prefixPath the prefixPath to set
	 */
	public void setPrefixPath(String prefixPath) {
		this.prefixPath = prefixPath;
	}
	
	/**
	 * Process.
	 */
	public void process(String xpath, NodeletParser parser) {
		parser.addNodelet(xpath, "/local", new Nodelet() {
			public void process(Node node, Properties attributes, String text) throws Exception {
				LocalServiceRule lsr = new LocalServiceRule();
				assistant.pushObject(lsr);
			}
		});
		parser.addNodelet(xpath, "/local/schema", new Nodelet() {
			public void process(Node node, Properties attributes, String text) throws Exception {
				LocalServiceRule lsr = (LocalServiceRule)assistant.peekObject();
				lsr.setSchemaConfigLocation(text);
			}
		});
		parser.addNodelet(xpath, "/local/directory", new Nodelet() {
			public void process(Node node, Properties attributes, String text) throws Exception {
				LocalServiceRule lsr = (LocalServiceRule)assistant.peekObject();
				lsr.setDirectory(text);
			}
		});
		parser.addNodelet(xpath, "/local/characterEncoding", new Nodelet() {
			public void process(Node node, Properties attributes, String text) throws Exception {
				LocalServiceRule lsr = (LocalServiceRule)assistant.peekObject();
				lsr.setCharacterEncoding(text);
			}
		});
		parser.addNodelet(xpath, "/local/spooling", new Nodelet() {
			public void process(Node node, Properties attributes, String text) throws Exception {
				SpoolingRule sr = new SpoolingRule();
				assistant.pushObject(sr);
			}
		});
		parser.addNodelet(xpath, "/local/spooling/directory", new Nodelet() {
			public void process(Node node, Properties attributes, String text) throws Exception {
				SpoolingRule sr = (SpoolingRule)assistant.peekObject();
				sr.setSpoolingMode(SpoolingMode.FILE);
				
				FileSpoolTransactionRule fstr = new FileSpoolTransactionRule();
				fstr.setDirectory(text);
				
				sr.setSpoolTransactionRule(fstr);
			}
		});
		parser.addNodelet(xpath, "/local/spooling/end()", new Nodelet() {
			public void process(Node node, Properties attributes, String text) throws Exception {
				SpoolingRule sr = (SpoolingRule)assistant.popObject();
				LocalServiceRule lsr = (LocalServiceRule)assistant.peekObject();
				
				lsr.setSpoolingRule(sr);
			}
		});
		parser.addNodelet(xpath, "/local/end()", new Nodelet() {
			public void process(Node node, Properties attributes, String text) throws Exception {
				LocalServiceRule lsr = (LocalServiceRule)assistant.popObject();
				assistant.setLocalServiceRule(lsr);
			}
		});
	}

}